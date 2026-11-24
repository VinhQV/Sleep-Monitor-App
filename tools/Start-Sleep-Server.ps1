$ErrorActionPreference='Stop'
$backend=Join-Path (Split-Path $PSScriptRoot -Parent) 'backend'
$node=(Get-Command node -ErrorAction Stop).Source
Push-Location $backend
try {
    $publicConfig=& $node --env-file-if-exists=.env --input-type=module -e "import {config} from './src/config.js'; console.log(JSON.stringify({port:config.port,provider:config.ai.provider}));"
    if ($LASTEXITCODE -ne 0) { throw 'Cannot read backend configuration.' }
    $settings=$publicConfig | ConvertFrom-Json
    $base='http://127.0.0.1:'+$settings.port
    function Read-SleepHealth {
        try { return Invoke-RestMethod -Uri ($base+'/health') -TimeoutSec 3 } catch { return $null }
    }
    $health=Read-SleepHealth
    if ($null -eq $health) {
        $logDir=Join-Path $backend 'data\logs'
        New-Item -ItemType Directory -Force -Path $logDir | Out-Null
        Start-Process -FilePath $node -ArgumentList '--env-file-if-exists=.env','src/server.js' -WorkingDirectory $backend -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logDir 'server.stdout.log') -RedirectStandardError (Join-Path $logDir 'server.stderr.log') | Out-Null
        for ($attempt=0;$attempt -lt 10 -and $null -eq $health;$attempt++) { Start-Sleep -Seconds 1; $health=Read-SleepHealth }
    }
    if ($null -eq $health -or $health.service -ne 'Sleep Monitor') { throw 'Sleep backend is unavailable. Check port and backend/data/logs/server.stderr.log.' }
    Write-Host ('Sleep backend: '+$base)
    Write-Host ('Configured AI provider: '+$settings.provider)
    Write-Host 'This launcher does not start Ollama or load a local AI model.'
} finally { Pop-Location }
