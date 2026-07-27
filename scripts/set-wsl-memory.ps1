<#
    Give Docker (WSL 2) more memory - and cap its CPU use so the laptop
    stays cooler.

    Docker Desktop on the WSL 2 backend has no memory slider. The limits are
    set by Windows in C:\Users\<you>\.wslconfig. This script writes that file
    for you, choosing values that fit the amount of RAM the machine has.

    Usage
    -----
        powershell -ExecutionPolicy Bypass -File .\scripts\set-wsl-memory.ps1
#>

[CmdletBinding()]
param(
    [int]$MemoryGB = 0,       # 0 = choose automatically
    [int]$Processors = 0      # 0 = choose automatically
)

$ErrorActionPreference = 'Stop'

function Write-Ok   { param($m) Write-Host "  OK   $m" -ForegroundColor Green }
function Write-Info { param($m) Write-Host "  $m"      -ForegroundColor Gray  }
function Write-Warn { param($m) Write-Host "  WARN $m" -ForegroundColor Yellow }

Write-Host ''
Write-Host '  Configuring WSL 2 resources for Docker' -ForegroundColor White
Write-Host ''

# ---------------------------------------------------------------------------
#  Work out what this machine actually has
# ---------------------------------------------------------------------------

$totalBytes = (Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory
$totalGB    = [math]::Round($totalBytes / 1GB, 1)
$cpuCount   = (Get-CimInstance Win32_ComputerSystem).NumberOfLogicalProcessors

Write-Info "This PC has $totalGB GB of RAM and $cpuCount logical processors."

# Leave Windows enough room to breathe. Windows 11 itself wants ~3 GB.
if ($MemoryGB -eq 0) {
    if     ($totalGB -le 8.5)  { $MemoryGB = 4 }
    elseif ($totalGB -le 17)   { $MemoryGB = 6 }
    else                       { $MemoryGB = 8 }
}

# Capping processors is what keeps the fan quiet. Maven will happily use every
# core it is given; it does not need to.
if ($Processors -eq 0) {
    $Processors = [math]::Max(2, [math]::Min(6, [int][math]::Floor($cpuCount / 2)))
}

Write-Info "Will give Docker $MemoryGB GB of memory and $Processors processors."

if ($MemoryGB -ge ($totalGB - 2)) {
    Write-Warn 'That leaves Windows very little. Consider a smaller -MemoryGB value.'
}

# ---------------------------------------------------------------------------
#  Write .wslconfig, preserving anything already there
# ---------------------------------------------------------------------------

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
# Written by scripts/set-wsl-memory.ps1
# Controls the resources Windows gives to WSL 2, which is what Docker
# Desktop runs inside. Restart with:  wsl --shutdown

[wsl2]

# Memory ceiling for the whole WSL 2 VM.
memory=${MemoryGB}GB

# Number of logical processors. Capping this is the single most effective
# way to stop a big build from overheating a laptop.
processors=$Processors

# A little swap so a memory spike fails slowly instead of being killed.
swap=2GB

# Return freed memory to Windows instead of holding on to it.
autoMemoryReclaim=gradual

# Let WSL release disk space back to Windows when files are deleted.
sparseVhd=true
"@

Set-Content -LiteralPath $configPath -Value $content -Encoding ASCII

Write-Ok "Wrote $configPath"
Write-Host ''
Get-Content -LiteralPath $configPath | ForEach-Object { Write-Host "      $_" -ForegroundColor DarkGray }

# ---------------------------------------------------------------------------
#  What to do next
# ---------------------------------------------------------------------------

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
