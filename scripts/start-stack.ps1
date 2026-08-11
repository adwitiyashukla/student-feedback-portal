[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [switch]$Down,
    [switch]$Monitoring
)

$ErrorActionPreference = 'Continue'

$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $RepoRoot

$script:StageNumber = 0

function Write-Stage {
    param([string]$Message)
    $script:StageNumber++
    Write-Host ''
    Write-Host ('=' * 70) -ForegroundColor Cyan
    Write-Host ("  STAGE $script:StageNumber : $Message") -ForegroundColor Cyan
    Write-Host ('=' * 70) -ForegroundColor Cyan
}

function Write-Info { param([string]$m) Write-Host "  $m" -ForegroundColor Gray }

function Clear-StatusLine {
    Write-Host ("`r" + (' ' * 78) + "`r") -NoNewline
}
function Write-Ok   { param([string]$m) Write-Host "  OK   $m" -ForegroundColor Green }
function Write-Warn { param([string]$m) Write-Host "  WARN $m" -ForegroundColor Yellow }
function Write-Err  { param([string]$m) Write-Host "  FAIL $m" -ForegroundColor Red }

function Stop-WithError {
    param([string]$Message, [string]$Hint)
    Write-Host ''
    Write-Err $Message
    if ($Hint) {
        Write-Host ''
        Write-Host "  What to try:" -ForegroundColor Yellow
        foreach ($line in $Hint -split "`n") { Write-Host "    $line" -ForegroundColor Yellow }
    }
    Write-Host ''
    exit 1
}

function Test-DockerReady {
    Write-Info 'Checking that the Docker daemon is responding...'
    docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Stop-WithError `
            'The Docker daemon is not responding.' `
            "Start Docker Desktop and wait for the whale icon to stop animating.`nIf it is already running, quit it fully and start it again."
    }
    Write-Ok 'Docker is responding.'
}

function Show-DockerMemory {
    $bytes = (docker info --format '{{.MemTotal}}' 2>$null)
    if ($LASTEXITCODE -eq 0 -and $bytes -match '^\d+$') {
        $gb = [math]::Round([double]$bytes / 1GB, 1)
        if ($gb -lt 3.5) {
            Write-Warn "Docker has only $gb GB of memory. The Java build may thrash."
            Write-Warn 'Raise it: Docker Desktop > Settings > Resources > Memory > 6 GB > Apply & Restart.'
            Write-Host ''
            $answer = Read-Host '  Continue anyway? (y/N)'
            if ($answer -ne 'y') { Write-Info 'Stopped. Adjust the setting and re-run.'; exit 0 }
        } else {
            Write-Ok "Docker memory: $gb GB."
        }
    }
}

function Test-EnvFile {
    if (-not (Test-Path '.env')) {
        Stop-WithError `
            'No .env file found in the repo root.' `
            "Run this once:`n    Copy-Item .env.example .env`nthen open .env and fill in the required values."
    }
    Write-Ok '.env present.'
}

function Wait-ForHealthy {

    param(
        [Parameter(Mandatory)][string]$ContainerName,
        [int]$TimeoutSeconds = 180
    )

    Write-Info "Waiting for '$ContainerName' to report healthy (up to $TimeoutSeconds s)..."
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $spinner  = '|/-\'
    $i = 0

    while ((Get-Date) -lt $deadline) {
        $state = docker inspect --format '{{.State.Health.Status}}' $ContainerName 2>$null

        if ($LASTEXITCODE -ne 0) {
            Start-Sleep -Seconds 2
            continue
        }

        switch ($state) {
            'healthy' {
                Clear-StatusLine
                Write-Ok "'$ContainerName' is healthy."
                return
            }
            'unhealthy' {
                Clear-StatusLine
                Write-Err "'$ContainerName' reported unhealthy. Last 40 log lines:"
                docker logs --tail 40 $ContainerName
                Stop-WithError `
                    "'$ContainerName' failed its healthcheck." `
                    "Read the log lines above - they usually name the problem directly."
            }
        }

        $running = docker inspect --format '{{.State.Running}}' $ContainerName 2>$null
        if ($running -eq 'false') {
            Clear-StatusLine
            Write-Err "'$ContainerName' stopped unexpectedly. Last 40 log lines:"
            docker logs --tail 40 $ContainerName
            Stop-WithError "'$ContainerName' exited before becoming healthy." ''
        }

        $char = $spinner[$i % 4]
        $left = [int]($deadline - (Get-Date)).TotalSeconds
        Write-Host "`r  $char still starting... ${left}s remaining " -NoNewline -ForegroundColor Gray
        $i++
        Start-Sleep -Seconds 3
    }

    Clear-StatusLine
    Stop-WithError `
        "'$ContainerName' did not become healthy within $TimeoutSeconds seconds." `
        "Check its logs:`n    docker logs $ContainerName"
}

if ($Down) {
    Write-Stage 'Stopping the stack'
    docker compose down
    Write-Ok 'Stack stopped. Data volumes were kept.'
    Write-Info 'To delete the database too: docker compose down -v'
    exit 0
}

$started = Get-Date

Write-Host ''
Write-Host '  Student Feedback Portal - staged startup' -ForegroundColor White
Write-Host '  One stage at a time, so the laptop is never doing two heavy things at once.' -ForegroundColor DarkGray

Write-Stage 'Pre-flight checks'
Test-DockerReady
Show-DockerMemory
Test-EnvFile

if (-not $SkipBuild) {

    Write-Stage 'Build the backend image (nothing else runs during this)'
    Write-Info 'This is the heavy one: Maven downloads dependencies, then compiles 119 files.'
    Write-Info 'The "Compiling ... source files" line prints nothing while it works. That is normal.'
    Write-Info 'Expect 3-6 minutes on a cold cache. Do not press Ctrl+C.'
    Write-Host ''

    docker compose build backend
    if ($LASTEXITCODE -ne 0) {
        Stop-WithError `
            'The backend image failed to build.' `
            "Scroll up to the first line containing ERROR - that is the real cause.`nEverything after it is usually noise."
    }
    Write-Ok 'Backend image built.'

    Write-Stage 'Build the analytics image'
    Write-Info 'Installs scikit-learn and trains the classifier during the build. 2-4 minutes.'
    Write-Host ''

    docker compose build analytics
    if ($LASTEXITCODE -ne 0) {
        Stop-WithError 'The analytics image failed to build.' 'Scroll up to the first ERROR line.'
    }
    Write-Ok 'Analytics image built.'

} else {
    Write-Stage 'Skipping builds (-SkipBuild)'
    Write-Info 'Using whatever images already exist.'
}

Write-Stage 'Start the datastores (MySQL + Redis)'
Write-Info 'MySQL initialises its data directory on first run, which takes a while.'
Write-Host ''

docker compose up -d mysql redis
if ($LASTEXITCODE -ne 0) { Stop-WithError 'Could not start MySQL/Redis.' '' }

Wait-ForHealthy -ContainerName 'sfp-mysql' -TimeoutSeconds 240
Wait-ForHealthy -ContainerName 'sfp-redis' -TimeoutSeconds 60

Write-Stage 'Start the analytics service'
docker compose up -d analytics
if ($LASTEXITCODE -ne 0) { Stop-WithError 'Could not start analytics.' '' }
Wait-ForHealthy -ContainerName 'sfp-analytics' -TimeoutSeconds 120

Write-Stage 'Start the backend'
Write-Info 'Spring Boot runs Flyway migrations on first boot, so give it a minute.'
docker compose up -d backend
if ($LASTEXITCODE -ne 0) { Stop-WithError 'Could not start the backend.' '' }
Wait-ForHealthy -ContainerName 'sfp-backend' -TimeoutSeconds 240

if ($Monitoring) {
    Write-Stage 'Start monitoring (Prometheus + Grafana)'
    docker compose up -d prometheus grafana
    Write-Ok 'Monitoring started.'
}

$elapsed = [math]::Round(((Get-Date) - $started).TotalMinutes, 1)

Write-Host ''
Write-Host ('=' * 70) -ForegroundColor Green
Write-Host '  STACK IS UP' -ForegroundColor Green
Write-Host ('=' * 70) -ForegroundColor Green
Write-Host ''
Write-Host "  Took $elapsed minutes." -ForegroundColor Gray
Write-Host ''
Write-Host '  App          http://localhost:8080'          -ForegroundColor White
Write-Host '  Health       http://localhost:8080/api/v1/health' -ForegroundColor White
Write-Host '  API docs     http://localhost:8080/swagger-ui.html' -ForegroundColor White
Write-Host '  Analytics    http://localhost:8000/api/v1/health'   -ForegroundColor White
Write-Host '  Mail catcher http://localhost:8025'          -ForegroundColor White
if ($Monitoring) {
    Write-Host '  Prometheus   http://localhost:9090'      -ForegroundColor White
    Write-Host '  Grafana      http://localhost:3000'      -ForegroundColor White
}
Write-Host ''
Write-Host '  Follow the backend log:  docker compose logs -f backend' -ForegroundColor DarkGray
Write-Host '  Stop everything:         .\scripts\start-stack.ps1 -Down' -ForegroundColor DarkGray
Write-Host ''
