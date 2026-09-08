$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$localDir = Join-Path $projectRoot '.local'
$defaultUrl = 'jdbc:mysql://localhost:3306/tododb?serverTimezone=Asia/Tokyo'
$url = Read-Host "JDBC URL (Enter for $defaultUrl)"
if ([string]::IsNullOrWhiteSpace($url)) { $url = $defaultUrl }
if (-not $url.StartsWith('jdbc:mysql:')) { throw 'A MySQL JDBC URL is required.' }
$user = Read-Host 'MySQL user'
if ([string]::IsNullOrWhiteSpace($user)) { throw 'MySQL user is required.' }
$password = Read-Host 'MySQL password' -AsSecureString
New-Item -ItemType Directory -Path $localDir -Force | Out-Null
@{ url = $url; user = $user } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $localDir 'mysql.json') -Encoding UTF8
$password | ConvertFrom-SecureString | Set-Content -LiteralPath (Join-Path $localDir 'mysql-password.txt') -Encoding ASCII
Write-Host 'Connection settings saved. Run start-mysql.bat after building the app.'
