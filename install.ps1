#Requires -Version 7
# ⚔ Tekton Agent installer — Windows (PowerShell 7+)
# Usage: ./install.ps1 [-Profile minimal|recommended|full] [-OsApp] [-MobileChat]
#                      [-Model <name>] [-ProviderUrl <url>] [-OvServer <url>] [-User <name>]
param(
  [ValidateSet("minimal","recommended","full")][string]$Profile = "recommended",
  [switch]$OsApp, [switch]$MobileChat,
  [string]$Model = "glm-5.1:cloud", [string]$ProviderUrl = "http://localhost:11434",
  [string]$OvServer = "", [string]$User = "", [switch]$SkipRuntime,
  [string]$Engines = "", [switch]$AllEngines
)
$ErrorActionPreference = "Stop"
$Repo    = $PSScriptRoot
$TektonHome = if ($env:TEKTON_HOME) { $env:TEKTON_HOME } else { "$env:USERPROFILE\.tekton" }
$TektonSrc  = "$env:USERPROFILE\.tekton-src\tekton"

Get-Content "$Repo\assets\splash.txt" -ErrorAction SilentlyContinue

# ── 1. Node.js 20+ ──────────────────────────────────────────────────
$needNode = $true
try { $v = (node -v); if ([int]($v.Substring(1).Split(".")[0]) -ge 20) { $needNode = $false } } catch {}
if ($needNode) {
  Write-Host "▸ installing Node.js 20+ (winget)" -ForegroundColor Cyan
  if (Get-Command winget -ErrorAction SilentlyContinue) { winget install -e --id OpenJS.NodeJS.LTS --accept-source-agreements --accept-package-agreements }
  elseif (Get-Command choco -ErrorAction SilentlyContinue) { choco install nodejs-lts -y }
  else { throw "Install Node.js 20+ from https://nodejs.org then re-run." }
  $env:Path = [Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [Environment]::GetEnvironmentVariable("Path","User")
}

# ── 2. runtime + CLI ────────────────────────────────────────────────
if (-not $SkipRuntime) {
  Write-Host "▸ installing pi runtime" -ForegroundColor Cyan
  npm install -g @earendil-works/pi-coding-agent 2>$null
}
if (-not (Get-Command tekton -ErrorAction SilentlyContinue)) {
  Write-Host "▸ building Tekton CLI from messyjs/tekton" -ForegroundColor Cyan
  if (-not (Test-Path "$TektonSrc")) { git clone https://github.com/messyjs/tekton "$TektonSrc" }
  Push-Location "$TektonSrc"
  try { npm install; npm run build -w @tekton/cli; npm link } catch { Write-Host "  build failed — request repo access" -ForegroundColor DarkGray }
  Pop-Location
} else { Write-Host "▸ tekton CLI present: $((Get-Command tekton).Source)" -ForegroundColor DarkGray }

# ── 3. ~/.tekton home ───────────────────────────────────────────────
Write-Host "▸ wiring $TektonHome (context home, tekton.md)" -ForegroundColor Cyan
foreach ($d in "skills","extensions","sessions","checkpoints") { New-Item -ItemType Directory -Force -Path "$TektonHome\$d" | Out-Null }
if (-not (Test-Path "$TektonHome\tekton.md")) {
  (Get-Content "$Repo\config\tekton.md" -Raw).
    Replace("{{USER_NAME}}", $(if ($User) { $User } else { "Knight" })).
    Replace("{{USER_STYLE}}", "direct, hands-on builder") | Set-Content "$TektonHome\tekton.md" -Encoding utf8
}
if (-not (Test-Path "$TektonHome\config.yaml")) {
  (Get-Content "$Repo\config\config.example.yaml" -Raw).
    Replace("{{MODEL_FAST}}", $Model).Replace("{{MODEL_DEEP}}", $Model).
    Replace("{{PROVIDER_URL}}", $ProviderUrl) | Set-Content "$TektonHome\config.yaml" -Encoding utf8
}
if (-not (Test-Path "$TektonHome\models.json"))    { Copy-Item "$Repo\config\models.example.json"    "$TektonHome\models.json" }
if (-not (Test-Path "$TektonHome\settings.json"))  { Copy-Item "$Repo\config\settings.example.json"  "$TektonHome\settings.json" }
if ((Test-Path "$env:USERPROFILE\.pi\agent\auth.json") -and -not (Test-Path "$TektonHome\auth.json")) {
  Copy-Item "$env:USERPROFILE\.pi\agent\auth.json" "$TektonHome\auth.json"
}

# ── 4. skills per profile ───────────────────────────────────────────
$kits = @{
  caveman  = "caveman","caveman-commit","caveman-compress","caveman-help","caveman-review"
  session  = "checkpoint","compress","startup","health"
  routing  = "sorting-hat","update-models","add-account"
  project  = "project-registry","project-status","treasure-map","dictionary","find-skills"
  engines  = "engine-forge"
  trading  = "hermes-trading","markov-hedge-fund-method","tv-fast-ops","orca-cli"
  power    = "orchestration","skillception","computer-use","skeleton-key","dreams","hunger-games","quantum-consciousness"
  media    = "pause","play","product-enhance","1up"
}
$chosen = @()
if ($Profile -in "recommended","full") { $chosen += $kits.caveman + $kits.session + $kits.routing + $kits.project + $kits.engines }
if ($Profile -eq "full")               { $chosen += $kits.trading + $kits.power + $kits.media }
Write-Host "▸ installing skills ($Profile)" -ForegroundColor Cyan
Copy-Item "$Repo\skills\house-style" "$TektonHome\skills\" -Recurse -Force
foreach ($s in $chosen) {
  if (Test-Path "$Repo\skills\$s") { Copy-Item "$Repo\skills\$s" "$TektonHome\skills\" -Recurse -Force }
}
if ($Profile -in "recommended","full" -and -not (Get-Command cavemem -ErrorAction SilentlyContinue)) {
  npm install -g cavemem 2>$null
}

# ── 5. OpenViking recall ────────────────────────────────────────────
if ($Profile -ne "minimal") {
  Write-Host "▸ installing openviking recall extension" -ForegroundColor Cyan
  if (-not (Test-Path "$TektonHome\extensions\openviking")) { Copy-Item "$Repo\extensions\openviking" "$TektonHome\extensions\" -Recurse }
  if ($OvServer -and -not (Select-String -Path "$TektonHome\config.yaml" -Pattern "^openviking:" -Quiet)) {
    Add-Content "$TektonHome\config.yaml" "`nopenviking:`n  enabled: true`n  server: $OvServer"
  }
}

# ── 5b. engines (capability servers for sub-agents) ─────────────────
if ($AllEngines -or $Engines) {
  Write-Host "▸ registering engines" -ForegroundColor Cyan
  Copy-Item "$Repo\config\engines.yaml" "$TektonHome\engines.yaml" -Force
  if ($AllEngines -and (Test-Path "$TektonSrc")) {
    Push-Location "$TektonSrc"
    try { npm run build -w @tekton/forge -w @tekton/voice -w @tekton/docling-service -w @tekton/browser-use-service -w @tekton/ml-ops } catch { Write-Host "  engine build skipped" -ForegroundColor DarkGray }
    Pop-Location
  }
  Write-Host "  registry: $TektonHome\engines.yaml — start engines listed there;" -ForegroundColor DarkGray
  Write-Host "  sub-agents (orchestration skill) call engines via gateway/MCP adapters." -ForegroundColor DarkGray
}

# ── 6. add-ons ──────────────────────────────────────────────────────
if ($MobileChat) {
  Write-Host "▸ mobile remote chat:" -ForegroundColor Yellow
  Write-Host "    1) run:      tekton --gateway        (serves chat on :8080)"
  Write-Host "    2) expose:   cloudflared tunnel --url http://localhost:8080"
  Write-Host "    3) phone:    open the tunnel URL in any browser, or point the"
  Write-Host "                 Agent OS App at it. Chat with Tekton from anywhere."
  if (-not (Get-Command cloudflared -ErrorAction SilentlyContinue)) {
    Write-Host "    (install cloudflared: winget install Cloudflare.cloudflared)" -ForegroundColor DarkGray
  }
}
if ($OsApp) {
  Write-Host "▸ Agent OS App (Tauri 2: Windows/macOS/Android)" -ForegroundColor Cyan
  if (Get-Command cargo -ErrorAction SilentlyContinue) {
    Push-Location "$Repo\app"
    try { npm install; npx tauri build; Write-Host "  built → app\src-tauri\target\release\bundle\" -ForegroundColor Yellow }
    catch { Write-Host "  build failed — see docs\AGENT-OS.md" -ForegroundColor DarkGray }
    Pop-Location
  } else { Write-Host "  Rust not found. Install rustup: https://rustup.rs then re-run -OsApp" -ForegroundColor DarkGray }
}

# ── 7. done ─────────────────────────────────────────────────────────
Write-Host ""
Write-Host "⚔ Tekton Agent installed ($Profile profile)" -ForegroundColor Yellow
Write-Host "  home      : $TektonHome        context: $TektonHome\tekton.md"
Write-Host "  skills    : $((Get-ChildItem "$TektonHome\skills" -Directory -ErrorAction SilentlyContinue).Count) installed"
Write-Host "  next      : run " -NoNewline; Write-Host "tekton" -ForegroundColor White -NoNewline; Write-Host " — pull the sword from the stone."
