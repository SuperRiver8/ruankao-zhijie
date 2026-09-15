$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$deployRoot = $PSScriptRoot
$repoRoot = Split-Path $deployRoot -Parent
function Invoke-BuildCommand {
    param([string]$Command, [string[]]$Arguments)
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) { throw "构建失败：$Command，退出码 $LASTEXITCODE" }
}
foreach ($command in @('java', 'mvn.cmd', 'node', 'npm.cmd')) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) { throw "未找到 $command，请安装 Java 21、Maven 3.9 和 Node.js 22。" }
}
$javaInfo = (& cmd.exe /c 'java -version 2>&1') -join "`n"
if ($javaInfo -notmatch 'version "21\.') { throw '请使用 Java 21。' }
$nodeInfo = & node --version
if ([int]($nodeInfo.TrimStart('v').Split('.')[0]) -lt 22) { throw '请使用 Node.js 22 或更高版本。' }
Push-Location $repoRoot
try {
    Invoke-BuildCommand 'mvn.cmd' @('-B', '-f', 'backend/pom.xml', '-DskipTests', 'clean', 'package')
    foreach ($app in @('customer', 'admin')) {
        Invoke-BuildCommand 'npm.cmd' @('--prefix', "frontend/$app", 'ci', '--no-audit', '--no-fund')
        Invoke-BuildCommand 'npm.cmd' @('--prefix', "frontend/$app", 'run', 'build')
    }
    # 所有编译成功后才准备替换部署产物。
    $stage = Join-Path $deployRoot ('.package-' + [guid]::NewGuid().ToString('N'))
    $stageFrontend = Join-Path $stage 'frontend'
    New-Item -ItemType Directory -Path $stageFrontend -Force | Out-Null
    Copy-Item -LiteralPath 'backend/target/zhijie-api-0.1.0.jar' -Destination (Join-Path $stage 'app.jar')
    foreach ($app in @('customer', 'admin')) {
        if (-not (Test-Path "frontend/$app/dist/index.html")) { throw "缺少 $app 构建产物。" }
        Copy-Item -LiteralPath "frontend/$app/dist" -Destination (Join-Path $stageFrontend $app) -Recurse
    }
    $destination = [IO.Path]::GetFullPath((Join-Path $deployRoot 'frontend'))
    if ((Split-Path $destination -Parent) -ne [IO.Path]::GetFullPath($deployRoot)) { throw '产物路径不在 deploy 目录内。' }
    if (Test-Path -LiteralPath $destination) { Remove-Item -LiteralPath $destination -Recurse -Force }
    Move-Item -LiteralPath $stageFrontend -Destination $destination
    Move-Item -LiteralPath (Join-Path $stage 'app.jar') -Destination (Join-Path $deployRoot 'app.jar') -Force
    Remove-Item -LiteralPath $stage
    Write-Host '打包完成。配置 deploy/.env 后，将整个 deploy 目录上传服务器。'
} finally {
    Pop-Location
}
