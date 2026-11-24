$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot -Parent
$archive=Join-Path (Split-Path $project -Parent) 'SleepMonitor-System.zip'
Add-Type -AssemblyName System.IO.Compression
$stream=[IO.File]::Open($archive,[IO.FileMode]::Create)
$zip=[IO.Compression.ZipArchive]::new($stream,[IO.Compression.ZipArchiveMode]::Create)
try {
  Get-ChildItem -LiteralPath $project -Recurse -File -Force | ForEach-Object {
    $relative=$_.FullName.Substring($project.TrimEnd('\','/').Length+1).Replace('\','/')
    if ($_.Name -like '.env*' -and $_.Name -ne '.env.example') { return }
    if ($relative -match '(^|/)(node_modules|build|\.gradle|\.kotlin|\.idea|\.pio|\.git)(/|$)' -or $relative -match '^backend/data/' -or $relative -match '(^|/)(local\.properties|\.env|secrets\.h)$') { return }
    $entry=$zip.CreateEntry('SleepMonitor/'+$relative,[IO.Compression.CompressionLevel]::Optimal)
    $destination=$entry.Open()
    $source=[IO.File]::OpenRead($_.FullName)
    try { $source.CopyTo($destination) } finally { $source.Dispose();$destination.Dispose() }
  }
} finally { $zip.Dispose();$stream.Dispose() }
Get-Item -LiteralPath $archive | Select-Object FullName,Length
