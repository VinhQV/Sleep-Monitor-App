$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot -Parent
$sdk=Join-Path $env:LOCALAPPDATA 'Android\Sdk'
if(!(Test-Path (Join-Path $sdk 'platform-tools\adb.exe'))){$sdk=Join-Path $env:USERPROFILE 'AppData\Local\Android\Sdk'}
$adb=Join-Path $sdk 'platform-tools\adb.exe'
$emulator=Join-Path $sdk 'emulator\emulator.exe'
if(!(Test-Path $adb) -or !(Test-Path $emulator)){throw 'Android SDK / emulator not found.'}
$env:HOST='0.0.0.0'
if(Test-Path -LiteralPath (Join-Path $project 'backend/data/remote-access.json')){
    & (Join-Path $PSScriptRoot 'Start-Sleep-Remote.ps1')
}else{& (Join-Path $PSScriptRoot 'Start-Sleep-Server.ps1')}
$serial='emulator-5554'
$devices=& $adb devices
if(!($devices -match '^emulator-5554\s+device')){
    if($devices -match '^emulator-') {throw 'An emulator is already starting. Wait for it before opening another.'}
    $logDir=Join-Path $project 'android\build'
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
    Start-Process -FilePath $emulator -ArgumentList '-avd','Pixel_3','-port','5554','-memory','2048','-cores','4','-no-boot-anim','-no-snapshot','-camera-back','none','-camera-front','none','-no-audio','-gpu','host','-feature','-Vulkan' -WindowStyle Normal -RedirectStandardOutput (Join-Path $logDir 'emulator-light.stdout.log') -RedirectStandardError (Join-Path $logDir 'emulator-light.stderr.log') | Out-Null
}
$ready=$false
for($attempt=0;$attempt -lt 90;$attempt++){
    $boot=& $adb -s $serial shell getprop sys.boot_completed 2>$null
    if($boot -match '^1'){$ready=$true;break}
    Start-Sleep -Seconds 2
}
if(!$ready){throw 'Emulator did not finish booting. Check android/build/emulator-light.stderr.log.'}
# Render fewer pixels while preserving the Pixel 3 aspect ratio and UI sizing.
& $adb -s $serial shell wm size 720x1440 | Out-Null
& $adb -s $serial shell wm density 293 | Out-Null
$installed=& $adb -s $serial shell pm path com.example.sleepmonitor
if(!$installed){
    & $adb -s $serial install (Join-Path $project 'release\SleepMonitor-debug.apk')
    if($LASTEXITCODE -ne 0){throw 'APK installation failed.'}
}
& $adb -s $serial shell am start -n com.example.sleepmonitor/.MainActivity
Write-Host 'Android ready: 2048 MB guest RAM, 4 CPU cores, 720x1440 display.'
Write-Host 'Only the emulator and Sleep server are started; no Android Studio or local AI model.'
