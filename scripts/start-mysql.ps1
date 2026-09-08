param([switch]$Background, [switch]$NoBrowser, [switch]$Stop)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
$baseUrl = 'http://127.0.0.1:8081'
$configPath = Join-Path $projectRoot '.local/mysql.json'
$passwordPath = Join-Path $projectRoot '.local/mysql-password.txt'
$processPath = Join-Path $projectRoot '.local/mysql-process.json'

try {
    if ($Stop) {
        if (-not (Test-Path -LiteralPath $processPath)) { Write-Host 'No recorded MySQL app process.'; exit 0 }
        $savedProcess = Get-Content -LiteralPath $processPath -Raw | ConvertFrom-Json
        $running = Get-Process -Id $savedProcess.id -ErrorAction SilentlyContinue
        if ($running -and $running.ProcessName -eq 'java' -and $running.StartTime.ToUniversalTime().Ticks.ToString() -eq $savedProcess.started) {
            Stop-Process -Id $running.Id
            Write-Host 'Taskboard MySQL stopped.'
        } else { Write-Host 'The recorded Taskboard process is no longer running.' }
        exit 0
    }
    if (-not (Test-Path -LiteralPath $configPath) -or -not (Test-Path -LiteralPath $passwordPath)) {
        throw 'MySQL configuration is missing. Set up the local connection first.'
    }
    $config = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
    $existing = $null
    try { $existing = Invoke-RestMethod "$baseUrl/api/tasks" -TimeoutSec 2 } catch {}
    if ($null -ne $existing -and $null -ne $existing.revision -and $null -ne $existing.tasks) {
        Write-Host "Taskboard is already running: $baseUrl"
        if (-not $NoBrowser) { Start-Process $baseUrl }
        exit 0
    }

    $java = 'java'
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath "$env:JAVA_HOME/bin/java.exe")) {
        $java = "$env:JAVA_HOME/bin/java.exe"
    }
    if (-not (Get-Command $java -ErrorAction SilentlyContinue)) { throw 'Java 17 or later is required.' }
    $jar = Join-Path $projectRoot 'target/todo-list-java-1.0.0.jar'
    if (-not (Test-Path -LiteralPath $jar)) { throw 'Build the app first with: mvn clean verify' }

    $secure = (Get-Content -LiteralPath $passwordPath -Raw).Trim() | ConvertTo-SecureString
    $credential = New-Object System.Management.Automation.PSCredential($config.user, $secure)
    $env:DB_URL = $config.url
    $env:DB_USER = $config.user
    $env:DB_PASS = $credential.GetNetworkCredential().Password
    # Explicit default profile prevents accidental use of a demo profile from the shell.
    $arguments = @('-jar', 'target/todo-list-java-1.0.0.jar', '--spring.profiles.active=default', '--server.port=8081')
    $log = Join-Path $projectRoot '.local/mysql-output.log'
    $errLog = Join-Path $projectRoot '.local/mysql-error.log'
    $app = Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput $log -RedirectStandardError $errLog -PassThru
    Remove-Item Env:DB_PASS -ErrorAction SilentlyContinue
    @{ id = $app.Id; started = $app.StartTime.ToUniversalTime().Ticks.ToString() } | ConvertTo-Json | Set-Content -LiteralPath $processPath

    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        $app.Refresh()
        if ($app.HasExited) { throw "Startup failed. See $log and $errLog" }
        try {
            $state = Invoke-RestMethod "$baseUrl/api/tasks" -TimeoutSec 1
            if ($null -ne $state.revision -and $null -ne $state.tasks) { $ready = $true; break }
        } catch {}
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw "Startup timed out. See $log" }
    Write-Host "Taskboard MySQL is ready: $baseUrl"
    if (-not $NoBrowser) { Start-Process $baseUrl }
    if (-not $Background) {
        Write-Host 'Press Enter here to stop Taskboard.'
        [void](Read-Host)
        if (-not $app.HasExited) { Stop-Process -Id $app.Id }
    }
} catch {
    if ($app -and -not $app.HasExited) { Stop-Process -Id $app.Id -ErrorAction SilentlyContinue }
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
} finally {
    Remove-Item Env:DB_PASS -ErrorAction SilentlyContinue
}
