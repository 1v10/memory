[CmdletBinding()]
param(
    [string]$SigningDirectory = (Join-Path $env:USERPROFILE '.android\memory-release')
)

$ErrorActionPreference = 'Stop'
$privateDirectory = [System.IO.Path]::GetFullPath($SigningDirectory)
$repositoryDirectory = [System.IO.Path]::GetFullPath((Split-Path $PSScriptRoot -Parent))
$repositoryPrefix = $repositoryDirectory.TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar
if ($privateDirectory.Equals($repositoryDirectory, [System.StringComparison]::OrdinalIgnoreCase) -or
    $privateDirectory.StartsWith($repositoryPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'Signing files must be stored outside the repository.'
}

$keyFile = Join-Path $privateDirectory 'memory-release.jks'
$credentialsFile = Join-Path $privateDirectory 'credentials.json'
if (-not (Test-Path -LiteralPath $keyFile) -or -not (Test-Path -LiteralPath $credentialsFile)) {
    throw 'Restore the original signing backup (memory-release.jks and credentials.json) to this private directory.'
}
$credentials = Get-Content -LiteralPath $credentialsFile -Raw | ConvertFrom-Json
if (-not $credentials.storePassword -or -not $credentials.keyAlias -or -not $credentials.keyPassword) {
    throw 'credentials.json is incomplete. Restore its original contents.'
}
$signing = [ordered]@{
    keystoreBase64 = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($keyFile))
    storePassword = $credentials.storePassword
    keyAlias = $credentials.keyAlias
    keyPassword = $credentials.keyPassword
}
$secretFile = Join-Path $privateDirectory 'MEMORY_SIGNING.json'
[System.IO.File]::WriteAllText($secretFile, ($signing | ConvertTo-Json -Compress))
$legacyFiles = @('MEMORY_KEYSTORE_BASE64.txt', 'MEMORY_KEYSTORE_PASSWORD.txt', 'MEMORY_KEY_ALIAS.txt', 'MEMORY_KEY_PASSWORD.txt')
foreach ($legacyName in $legacyFiles) {
    $legacyFile = Join-Path $privateDirectory $legacyName
    if (Test-Path -LiteralPath $legacyFile) { Remove-Item -LiteralPath $legacyFile }
}
Write-Output "Single secret file is ready: $secretFile"
Write-Output 'Create one GitHub repository secret named MEMORY_SIGNING and paste the entire JSON file contents.'
