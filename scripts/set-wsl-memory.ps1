[CmdletBinding()]
param(
    [int]$MemoryGB = 0,
    [int]$Processors = 0
)

$ErrorActionPreference = 'Stop'

function Write-Ok   { param($m) Write-Host "  OK   $m" -ForegroundColor Green }
function Write-Info { param($m) Write-Host "  $m"      -ForegroundColor Gray  }
function Write-Warn { param($m) Write-Host "  WARN $m" -ForegroundColor Yellow }

Write-Host ''
Write-Host '  Configuring WSL 2 resources for Docker' -ForegroundColor White
Write-Host ''

$totalBytes = (Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory
$totalGB    = [math]::Round($totalBytes / 1GB, 1)
$cpuCount   = (Get-CimInstance Win32_ComputerSystem).NumberOfLogicalProcessors

Write-Info "This PC has $totalGB GB of RAM and $cpuCount logical processors."

if ($MemoryGB -eq 0) {
    if     ($totalGB -le 8.5)  { $MemoryGB = 4 }
    elseif ($totalGB -le 17)   { $MemoryGB = 6 }
    else                       { $MemoryGB = 8 }
}

if ($Processors -eq 0) {
    $Processors = [math]::Max(2, [math]::Min(6, [int][math]::Floor($cpuCount / 2)))
}

Write-Info "Will give Docker $MemoryGB GB of memory and $Processors processors."

if ($MemoryGB -ge ($totalGB - 2)) {
    Write-Warn 'That leaves Windows very little. Consider a smaller -MemoryGB value.'
}

$configPath = Join-Path $env:USERPROFILE '.wslconfig'

if (Test-Path -LiteralPath $configPath) {
    $backup = "$configPath.backup-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
    Copy-Item -LiteralPath $configPath -Destination $backup
    Write-Warn "An existing .wslconfig was found."
    Write-Info "Backed it up to: $backup"
    Write-Host ''
    Write-Info 'Its current contents:'
    Get-Content -LiteralPath $configPath | ForEach-Object { Write-Host "      $_" -ForegroundColor DarkGray }
    Write-Host ''
    $answer = Read-Host '  Overwrite it with the new settings? (y/N)'
    if ($answer -ne 'y') {
        Write-Info 'Nothing changed. Exiting.'
        exit 0
    }
}

$content = @"

[wsl2]

memory=${MemoryGB}GB

processors=$Processors

swap=2GB

autoMemoryReclaim=gradual

sparseVhd=true
"@

Set-Content -LiteralPath $configPath -Value $content -Encoding ASCII

Write-Ok "Wrote $configPath"
Write-Host ''
Get-Content -LiteralPath $configPath | ForEach-Object { Write-Host "      $_" -ForegroundColor DarkGray }

Write-Host ''
Write-Host '  ------------------------------------------------------------' -ForegroundColor Cyan
Write-Host '   NEXT: the new settings need a WSL restart to take effect.' -ForegroundColor Cyan
Write-Host '  ------------------------------------------------------------' -ForegroundColor Cyan
Write-Host ''
Write-Info '1. Quit Docker Desktop completely'
Write-Info '   (right-click the whale in the system tray > Quit Docker Desktop)'
Write-Info ''
Write-Info '2. Then run:    wsl --shutdown'
Write-Info ''
Write-Info '3. Then start Docker Desktop again and wait for "Engine running".'
Write-Host ''
