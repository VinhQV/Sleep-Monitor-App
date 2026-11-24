$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot -Parent
$vswhere=Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio\Installer\vswhere.exe'
if (!(Test-Path $vswhere)) { throw 'Install Visual Studio C++ Build Tools, or compile firmware/test/native/rules_test.cpp with g++ and both include directories.' }
$vsRoot=& $vswhere -latest -products '*' -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
$vcvars=Join-Path $vsRoot 'VC\Auxiliary\Build\vcvars64.bat'
$output=Join-Path $project 'firmware\.pio\native-tests'
New-Item -ItemType Directory -Force -Path $output | Out-Null
$batch=Join-Path $output 'run.cmd'
@"
@echo off
call "$vcvars" >nul
cl /nologo /EHsc /I"$project\firmware\include" /I"$project\firmware\test\native" "$project\firmware\test\native\rules_test.cpp" /Fe:"$output\rules-test.exe" /Fo:"$output\rules-test.obj"
if errorlevel 1 exit /b 1
"$output\rules-test.exe"
"@ | Set-Content -LiteralPath $batch -Encoding ascii
& $env:ComSpec /d /c $batch
if ($LASTEXITCODE -ne 0) { throw 'Firmware rule tests failed' }
