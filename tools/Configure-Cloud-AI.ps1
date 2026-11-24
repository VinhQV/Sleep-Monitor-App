$ErrorActionPreference='Stop'
$backend=Join-Path (Split-Path $PSScriptRoot -Parent) 'backend'
Write-Host 'Sleep AI | Gemini Cloud'
Write-Host '1. Create a key at https://aistudio.google.com/apikey in a Free Tier project (billing off).'
Write-Host '2. Paste the key below. Input is hidden. Press Enter to test and save.'
$secret=Read-Host -AsSecureString -Prompt 'Gemini API key'
if ($secret.Length -eq 0) { Write-Host 'Cancelled.'; exit 1 }
$keyPointer=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
Push-Location $backend
try {
    [Runtime.InteropServices.Marshal]::PtrToStringBSTR($keyPointer) | node --env-file-if-exists=.env tools/configure-gemini.mjs
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($keyPointer)
    $secret.Dispose()
    Pop-Location
}
& (Join-Path $PSScriptRoot 'Start-Sleep-Server.ps1')
Write-Host 'Open Sleep AI and tap Thu ket noi lai.'
