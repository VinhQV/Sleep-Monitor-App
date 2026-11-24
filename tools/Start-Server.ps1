param([switch]$Lan)
$ErrorActionPreference='Stop'
Push-Location (Join-Path $PSScriptRoot '..\backend')
try {
  if ($Lan) { $env:HOST='0.0.0.0' } else { $env:HOST='127.0.0.1' }
  if (!(Test-Path 'node_modules')) { npm ci; if ($LASTEXITCODE -ne 0) { throw 'npm ci failed' } }
  node --env-file-if-exists=.env src/server.js
} finally { Pop-Location }
