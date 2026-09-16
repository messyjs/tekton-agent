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

# ── 0. preflight — detect everything, prompt for optionals ────────
$CoreMiss = @(); $OptMiss = @()
try { $v = (node -v); if ([int]($v.Substring(1).Split(".")[0]) -lt 20) { $CoreMiss += "node20+" } } catch { $CoreMiss += "node20+" }
if (-not (Get-Command git -ErrorAction SilentlyContinue))  { $CoreMiss += "git" }
if ($OsApp -and -not (Get-Command cargo -ErrorAction SilentlyContinue))     { $OptMiss += "rust/cargo   [recommended - builds the Agent OS App]" }
if ($MobileChat -and -not (Get-Command cloudflared -ErrorAction SilentlyContinue)) { $OptMiss += "cloudflared  (remote mobile chat tunnel)" }
Write-Host "▸ preflight" -ForegroundColor Cyan
foreach ($m in $CoreMiss) { Write-Host "  ● missing (required)  $m  → auto-install" -ForegroundColor Yellow }
foreach ($m in $OptMiss)  { Write-Host "  ○ missing (optional)  $m" -ForegroundColor DarkGray }
$OptInstall = $false
if ($OptMiss.Count -gt 0) {
  if ($Yes) { $OptInstall = $true; Write-Host "  -Yes: installing optional packages automatically" -ForegroundColor DarkGray }
  elseif ([Environment]::UserInteractive) {
    $ans = Read-Host "  Install the missing optional packages now? [Y/n]"
    if ($ans -notmatch "^[Nn]") { $OptInstall = $true }
  } else { Write-Host "  (non-interactive: skipping optional installs — re-run with -Yes)" -ForegroundColor DarkGray }
}

# ── 1. Node.js 20+ ──────────────────────────────────────────────────────────────────────────────────────────────────
$needNode = $true
try { $v = (node -v); if ([int]($v.Substring(1).Split(".")[0]) -ge 20) { $needNode = $false } } catch {}
if ($needNode) {
  Write-Host "▸ installing Node.js 20+ (winget)" -ForegroundColor Cyan
  if (Get-Command winget -ErrorAction SilentlyContinue) { winget install -e --id OpenJS.NodeJS.LTS --accept-source-agreements --accept-package-agreements }
  elseif (Get-Command choco -ErrorAction SilentlyContinue) { choco install nodejs-lts -y }
  else { throw "Install Node.js 20+ from https://nodejs.org then re-run." }
  $env:Path = [Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [Environment]::GetEnvironmentVariable("Path","User")
}

if ($CoreMiss -contains "git") {
  Write-Host "▸ installing git (winget)" -ForegroundColor Cyan
  if (Get-Command winget -ErrorAction SilentlyContinue) { winget install -e --id Git.Git --accept-source-agreements --accept-package-agreements }
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
  engines  = "engine-forge","subagent-forge","provider-add","os-control"
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

# ── 5c. app toolchains (-Toolchains "web,windows,macos,linux,android,ios,vst,games") ──
if ($Toolchains) {
  Write-Host "▸ toolchains: $Toolchains" -ForegroundColor Cyan
  foreach ($t in $Toolchains.Split(",")) {
    switch ($t.Trim()) {
      "web"      { Write-Host "  web/html: node (installed with core) - nothing extra" }
      "windows"  { Write-Host "  windows apps: + MSVC Build Tools (winget install Microsoft.VisualStudio.2022.BuildTools --override '--add Microsoft.VisualStudio.Workload.VCTools --passive') + WebView2" }
      "macos"    { Write-Host "  macos apps: run 'xcode-select --install' (Xcode CLT) if not present" }
      "linux"    { Write-Host "  linux apps: + build-essential libwebkit2gtk-4.1-dev libgtk-3-dev libayatana-appindicator3-dev librsvg2-dev libssl-dev patchelf (apt)" }
      "android"  { Write-Host "  android apps: + rustup android targets + Android Studio (SDK+NDK), set ANDROID_HOME + NDK_HOME - then 'tauri android build'" }
      "ios"      { Write-Host "  ios apps: needs full Xcode from the App Store (macOS only) - then 'tauri ios init'" }
      "vst"      { Write-Host "  vst/audio plugins: C++ toolchain + CMake + JUCE - Windows: MSVC Build Tools; mac: xcode-select --install; linux: build-essential cmake" }
      "games"    { Write-Host "  games: Unity Hub / Unreal / Godot per engine; html5 games need only node" }
      default    { Write-Host "  unknown toolchain: $t" }
    }
  }
}

# ── 6. add-ons ──────────────────────────────────────────────────────
if ($MobileChat) {
  Write-Host "▸ mobile remote chat:" -ForegroundColor Yellow
  Write-Host "    1) run:      tekton --gateway        (serves chat on :8080)"
  Write-Host "    2) expose:   cloudflared tunnel --url http://localhost:8080"
  Write-Host "    3) phone:    open the tunnel URL in any browser, or point the"
  Write-Host "                 Agent OS App at it. Chat with Tekton from anywhere."
  if (-not (Get-Command cloudflared -ErrorAction SilentlyContinue)) {
    if ($OptInstall) { Write-Host "  installing cloudflared (winget)" -ForegroundColor Cyan; winget install -e --id Cloudflare.cloudflared --accept-source-agreements --accept-package-agreements }
    else { Write-Host "    (install cloudflared: winget install Cloudflare.cloudflared | re-run with -Yes)" -ForegroundColor DarkGray }
  }
}
if ($OsApp) {
  Write-Host "▸ Agent OS App (Tauri 2: Windows/macOS/Android)" -ForegroundColor Cyan
  if (Get-Command cargo -ErrorAction SilentlyContinue) {
    Push-Location "$Repo\app"
    try { npm install; npx tauri build; Write-Host "  built → app\src-tauri\target\release\bundle\" -ForegroundColor Yellow }
    catch { Write-Host "  build failed — see docs\AGENT-OS.md" -ForegroundColor DarkGray }
    Pop-Location
  } elseif ($OptInstall) {
    Write-Host "  installing rustup (winget)" -ForegroundColor Cyan
    winget install -e --id Rustlang.Rustup --accept-source-agreements --accept-package-agreements
    $env:Path = [Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [Environment]::GetEnvironmentVariable("Path","User")
    Push-Location "$Repopp"
    try { npm install; npx tauri build; Write-Host "  built → app\src-tauri	arget
eleaseundle\" -ForegroundColor Yellow } catch { Write-Host "  build failed — see docs\AGENT-OS.md" -ForegroundColor DarkGray }
    Pop-Location
  } else { Write-Host "  Rust not found. Re-run with -Yes to auto-install, or: https://rustup.rs" -ForegroundColor DarkGray }
}

# ── 6b. verify ──────────────────────────────────────────────────────
Write-Host "▸ verifying install" -ForegroundColor Cyan
try { Write-Host "  ✓ node $((node -v))" -ForegroundColor Yellow } catch { Write-Host "  ✗ node missing" -ForegroundColor Red }
if (Get-Command tekton -ErrorAction SilentlyContinue) { Write-Host "  ✓ tekton CLI" -ForegroundColor Yellow } else { Write-Host "  ✗ tekton CLI not linked" -ForegroundColor Red }
if (Test-Path "$TektonHome	ekton.md") { Write-Host "  ✓ context: $TektonHome	ekton.md" -ForegroundColor Yellow } else { Write-Host "  ✗ tekton.md missing" -ForegroundColor Red }
if (Get-Command cloudflared -ErrorAction SilentlyContinue) { Write-Host "  ✓ cloudflared" -ForegroundColor Yellow }

# ── 7. done ─────────────────────────────────────────────────────────
Write-Host ""
Write-Host "⚔ Tekton Agent installed ($Profile profile)" -ForegroundColor Yellow
Write-Host "  home      : $TektonHome        context: $TektonHome\tekton.md"
Write-Host "  skills    : $((Get-ChildItem "$TektonHome\skills" -Directory -ErrorAction SilentlyContinue).Count) installed"
Write-Host "  next      : run " -NoNewline; Write-Host "tekton" -ForegroundColor White -NoNewline; Write-Host " — pull the sword from the stone."
