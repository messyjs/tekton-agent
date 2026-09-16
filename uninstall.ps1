#Requires -Version 7
# ⚔ Tekton Agent uninstaller — Windows. Keeps your data by default.
# Usage: ./uninstall.ps1 [-Yes] [-Purge]
param([switch]$Yes, [switch]$Purge)
$ErrorActionPreference = "Continue"
$TektonHome = if ($env:TEKTON_HOME) { $env:TEKTON_HOME } else { "$env:USERPROFILE\.tekton" }
function Ask($q) { if ($Yes) { return $true }; $a = Read-Host "$q [y/N]"; return $a -match "^[Yy]" }

Write-Host "⚔ Tekton Agent uninstaller" -ForegroundColor Yellow
$skills = "caveman","caveman-commit","caveman-compress","caveman-help","caveman-review","checkpoint","compress","startup","health","sorting-hat","update-models","add-account","project-registry","project-status","treasure-map","dictionary","find-skills","engine-forge","subagent-forge","provider-add","house-style","hermes-trading","markov-hedge-fund-method","tv-fast-ops","orca-cli","orchestration","skillception","computer-use","skeleton-key","dreams","hunger-games","quantum-consciousness","pause","play","product-enhance","1up"
Write-Host "▸ removing skills from $TektonHome\skills" -ForegroundColor Cyan
foreach ($s in $skills) { if (Test-Path "$TektonHome\skills\$s") { Remove-Item "$TektonHome\skills\$s" -Recurse -Force -ErrorAction SilentlyContinue } }
if (Test-Path "$TektonHome\extensions\openviking") { Remove-Item "$TektonHome\extensions\openviking" -Recurse -Force; Write-Host "  removed openviking extension" }
if (Test-Path "$TektonHome\engines.yaml") { Remove-Item "$TektonHome\engines.yaml" -Force }
if (Test-Path "$TektonHome\subagents") { Remove-Item "$TektonHome\subagents" -Recurse -Force }

Write-Host "▸ removing npm components" -ForegroundColor Cyan
if (Get-Command cavemem -ErrorAction SilentlyContinue) { npm uninstall -g cavemem 2>$null; Write-Host "  removed cavemem" }
if ((Get-Command tekton -ErrorAction SilentlyContinue) -and (Test-Path "$env:USERPROFILE\.tekton-src\tekton")) {
  Push-Location "$env:USERPROFILE\.tekton-src\tekton"; try { npm unlink -g 2>$null } catch {}; Pop-Location; Write-Host "  unlinked tekton CLI" }
if (Get-Command pi -ErrorAction SilentlyContinue) { npm uninstall -g @earendil-works/pi-coding-agent 2>$null; Write-Host "  removed pi runtime" }
if (Test-Path "$env:USERPROFILE\.tekton-src") { Remove-Item "$env:USERPROFILE\.tekton-src" -Recurse -Force }

if ($Purge -and (Test-Path $TektonHome)) {
  if (Ask "DELETE $TektonHome entirely (sessions, memory, projects)?") {
    $bk = "$TektonHome-backup-$(Get-Date -Format yyyyMMdd-HHmmss).zip"
    Compress-Archive -Path $TektonHome -DestinationPath $bk -Force; Write-Host "  backed up → $bk"
    Remove-Item $TektonHome -Recurse -Force; Write-Host "  removed (backup kept)"
  }
} else {
  Write-Host "▸ kept (your data): $TektonHome — sessions, memory, projects, models, config" -ForegroundColor DarkGray
  Write-Host "  remove entirely with: ./uninstall.ps1 -Purge" -ForegroundColor DarkGray
}
Write-Host "▸ untouched: OpenViking server data (~\.openviking) — manage separately" -ForegroundColor DarkGray
Write-Host "Done. Only the worthy may draw again." -ForegroundColor Yellow
