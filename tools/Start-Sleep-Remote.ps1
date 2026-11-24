$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot -Parent
$backend=Join-Path $project 'backend'
$data=Join-Path $backend 'data'
$logs=Join-Path $data 'logs'
$bin=Join-Path $data 'tools'
New-Item -ItemType Directory -Force -Path $logs,$bin | Out-Null
& (Join-Path $PSScriptRoot 'Start-Sleep-Server.ps1')
$node=(Get-Command node -ErrorAction Stop).Source
$access=Join-Path $data 'remote-access.json'
$statePath=Join-Path $data 'remote-tunnel.json'
$cloudflared=Join-Path $bin 'cloudflared.exe'
function Health($url){try{return Invoke-RestMethod -Uri $url -TimeoutSec 5}catch{return $null}}
if(!(Test-Path -LiteralPath $cloudflared)){
    $release=Invoke-RestMethod 'https://api.github.com/repos/cloudflare/cloudflared/releases/latest'
    $asset=$release.assets | Where-Object name -eq 'cloudflared-windows-amd64.exe' | Select-Object -First 1
    if(!$asset -or !$asset.digest.StartsWith('sha256:')){throw 'Official cloudflared download checksum unavailable.'}
    $download=Join-Path $bin 'cloudflared.download.exe'
    Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $download
    if((Get-FileHash -LiteralPath $download -Algorithm SHA256).Hash.ToLowerInvariant() -ne $asset.digest.Substring(7)){throw 'cloudflared checksum mismatch.'}
    Move-Item -LiteralPath $download -Destination $cloudflared -Force
}
if((Health 'http://127.0.0.1:8081/health').service -ne 'Sleep Remote Gateway'){
    Start-Process -FilePath $node -ArgumentList '--env-file-if-exists=.env','src/remote-server.js' -WorkingDirectory $backend -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logs 'remote.stdout.log') -RedirectStandardError (Join-Path $logs 'remote.stderr.log') | Out-Null
    for($i=0;$i -lt 15;$i++){if((Health 'http://127.0.0.1:8081/health').ok){break};Start-Sleep -Seconds 1}
}
if((Health 'http://127.0.0.1:8081/health').service -ne 'Sleep Remote Gateway'){throw 'Remote gateway failed to start.'}
$url=$null
if(Test-Path -LiteralPath $statePath){
    $state=Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    $existing=Get-Process -Id $state.processId -ErrorAction SilentlyContinue
    if($existing -and $existing.Path -eq $cloudflared){
        if(!$state.url){
            $pendingLog=Join-Path $logs 'cloudflared.stderr.log'
            $pendingText=''+(Get-Content -LiteralPath $pendingLog -Raw -ErrorAction SilentlyContinue)
            $pendingMatch=[regex]::Match($pendingText,'https://[a-z0-9-]+\.trycloudflare\.com')
            if($pendingMatch.Success){$state.url=$pendingMatch.Value;$state | ConvertTo-Json | Set-Content -LiteralPath $statePath -Encoding utf8}
            else{throw 'Tunnel is still starting. Try the launcher again shortly; the existing process is being kept.'}
        }
        if((Health ($state.url+'/health')).service -eq 'Sleep Remote Gateway'){$url=$state.url}
        else{
            $recentLog=[string]::Join("`n",(Get-Content -LiteralPath (Join-Path $logs 'cloudflared.stderr.log') -Tail 40 -ErrorAction SilentlyContinue))
            if($recentLog -match 'Unauthorized: Tunnel not found'){
                # This exact local executable owns an expired temporary tunnel; it cannot reconnect.
                Stop-Process -Id $existing.Id
                $existing.WaitForExit(5000) | Out-Null
                Write-Host 'The temporary tunnel expired. Creating a new address; reconnect the phone and XIAO with the new details.'
            }else{throw 'The tunnel process is still running but not reachable yet. Keep it running and try this launcher again; do not create a second tunnel.'}
        }
    }
}
if(!$url){
    $err=Join-Path $logs 'cloudflared.stderr.log'
    $process=Start-Process -FilePath $cloudflared -ArgumentList 'tunnel','--no-autoupdate','--url','http://127.0.0.1:8081','--protocol','http2' -WorkingDirectory $data -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logs 'cloudflared.stdout.log') -RedirectStandardError $err -PassThru
    @{url='';processId=$process.Id;createdAt=(Get-Date).ToUniversalTime().ToString('o')} | ConvertTo-Json | Set-Content -LiteralPath $statePath -Encoding utf8
    for($i=0;$i -lt 60;$i++){
        if(Test-Path -LiteralPath $err){$logText=''+(Get-Content -LiteralPath $err -Raw);$m=[regex]::Match($logText,'https://[a-z0-9-]+\.trycloudflare\.com');if($m.Success){$url=$m.Value;break}}
        if($process.HasExited){throw 'Cloudflare tunnel stopped. Check backend/data/logs/cloudflared.stderr.log.'}
        Start-Sleep -Seconds 1
    }
    if(!$url){throw 'Tunnel is starting but has not returned an address. Check the tunnel log.'}
    @{url=$url;processId=$process.Id;createdAt=(Get-Date).ToUniversalTime().ToString('o')} | ConvertTo-Json | Set-Content -LiteralPath $statePath -Encoding utf8
}
$credentials=Get-Content -LiteralPath $access -Raw | ConvertFrom-Json
$credentials | Add-Member -NotePropertyName url -NotePropertyValue $url -Force
$credentials | ConvertTo-Json | Set-Content -LiteralPath $access -Encoding utf8
& python (Join-Path $PSScriptRoot 'Write-Remote-Access.py')
Write-Host ('Remote HTTPS: '+$url)
Write-Host 'Keep this PC awake and online. The free address changes when a new tunnel is created.'
Write-Host 'Private connection details: backend/data/remote-access.json (do not share publicly).'
