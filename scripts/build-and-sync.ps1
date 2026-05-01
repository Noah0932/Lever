param(
    [string]$Target = "1.21.1",
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo

$propsPath = Join-Path $repo "versions/$Target.properties"
if (-not (Test-Path -LiteralPath $propsPath)) {
    throw "Unknown target '$Target'. Run: powershell -File scripts/package-target.ps1 -List"
}

$props = @{}
Get-Content -LiteralPath $propsPath | ForEach-Object {
    if ($_ -and -not $_.StartsWith('#') -and $_.Contains('=')) {
        $parts = $_.Split('=', 2)
        $props[$parts[0]] = $parts[1]
    }
}

$java21 = "C:\Program Files\Microsoft\jdk-21.0.6.7-hotspot"
if ([int]$props.java_release -ge 25) {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-25"
} else {
    $env:JAVA_HOME = $java21
}
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:GRADLE_USER_HOME = (Resolve-Path ".gradle-build-home").Path
$tmp = (Resolve-Path ".gradle-tmp").Path
$env:TEMP = $tmp
$env:TMP = $tmp
$env:GRADLE_OPTS = "-Dorg.gradle.native=false -Djava.io.tmpdir=$tmp"

$gradleArgs = @("clean", "build", "--no-daemon", "-Dorg.gradle.project.mcTarget=$Target")
if ($SkipTests) {
    $gradleArgs += "-x"
    $gradleArgs += "test"
}

Write-Host "=== Building Minecraft AI Agent for $Target ===" -ForegroundColor Cyan

& ".\.gradle-local\gradle-8.12\bin\gradle.bat" @gradleArgs
if ($LASTEXITCODE -ne 0) {
    Write-Host "BUILD FAILED" -ForegroundColor Red
    exit $LASTEXITCODE
}

$modProps = @{}
Get-Content -LiteralPath "gradle.properties" | ForEach-Object {
    if ($_ -and -not $_.StartsWith('#') -and $_.Contains('=')) {
        $parts = $_.Split('=', 2)
        $modProps[$parts[0]] = $parts[1]
    }
}
$modVersion = $modProps.mod_version

$sourceJar = "build/libs/minecraft-agent-$Target-$modVersion.jar"
$destDir = "dist"
$destJar = "$destDir/Minecraft-AI-Agent-v$modVersion-mc$Target-fabric.jar"

New-Item -ItemType Directory -Force -Path $destDir | Out-Null
Copy-Item -LiteralPath $sourceJar -Destination $destJar -Force
$jarSize = (Get-Item $destJar).Length
Write-Host "JAR: $destJar ($([math]::Round($jarSize/1KB,1)) KB)" -ForegroundColor Green

$githubDir = "github-release"
$syncItems = @("src", "scripts", "versions", "docs", "build.gradle", "settings.gradle", "gradle.properties", ".gitignore", "README.md", "COMPATIBILITY.md", "LICENSE")
foreach ($item in $syncItems) {
    $srcPath = Join-Path $repo $item
    $destPath = Join-Path $repo "$githubDir\$item"
    if (Test-Path -LiteralPath $srcPath) {
        Remove-Item -LiteralPath $destPath -Recurse -Force -ErrorAction SilentlyContinue
        Copy-Item -LiteralPath $srcPath -Destination $destPath -Recurse -Force
    }
}
$fileCount = (Get-ChildItem -LiteralPath "$repo\$githubDir" -Recurse -File).Count
Write-Host "SYNC: $githubDir ($fileCount files)" -ForegroundColor Green
Write-Host "=== Done ===" -ForegroundColor Cyan
