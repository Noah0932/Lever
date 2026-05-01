param(
    [string]$Target = "1.21.1",
    [switch]$List,
    [switch]$AllStable,
    [switch]$AllowExperimental
)

$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo

function Read-Props($path) {
    $props = @{}
    Get-Content -LiteralPath $path | ForEach-Object {
        if ($_ -and -not $_.StartsWith('#') -and $_.Contains('=')) {
            $parts = $_.Split('=', 2)
            $props[$parts[0]] = $parts[1]
        }
    }
    return $props
}

function Get-Targets() {
    Get-ChildItem -LiteralPath "versions" -Filter "*.properties" | Sort-Object Name | ForEach-Object {
        $props = Read-Props $_.FullName
        [PSCustomObject]@{
            Target = $_.BaseName
            Minecraft = $props.minecraft_version
            Java = $props.java_release
            FabricApi = $props.fabric_api_version
            State = $props.support_state
            File = $_.FullName
        }
    }
}

if ($List) {
    Get-Targets | Format-Table Target, Minecraft, Java, FabricApi, State -AutoSize
    exit 0
}

if ($AllStable) {
    $stableTargets = Get-Targets | Where-Object { $_.State -eq 'stable' } | Select-Object -ExpandProperty Target
    foreach ($stableTarget in $stableTargets) {
        & $PSCommandPath -Target $stableTarget
    }
    exit 0
}

$propsPath = Join-Path $repo "versions/$Target.properties"
if (-not (Test-Path -LiteralPath $propsPath)) {
    throw "Unknown target '$Target'. Run: powershell -File scripts/package-target.ps1 -List"
}

$props = Read-Props $propsPath
if ($props.support_state -ne 'stable' -and -not $AllowExperimental) {
    throw "Target $Target is registered as '$($props.support_state)' and is not buildable yet. Use -List to see states, or pass -AllowExperimental only after adding a source adapter for that target."
}

$java21 = "C:\Program Files\Microsoft\jdk-21.0.6.7-hotspot"
if ([int]$props.java_release -ge 25) {
    $java25 = "C:\Program Files\Java\jdk-25"
    if (-not (Test-Path -LiteralPath $java25)) {
        throw "Target $Target requires Java $($props.java_release). Install JDK 25 before building this target."
    }
    $env:JAVA_HOME = $java25
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
if ($AllowExperimental) {
    $gradleArgs += "-PallowExperimentalTarget"
    if ($props.requires_mojang_port -eq 'true') {
        $gradleArgs += "-PallowExperimental26"
    }
}

& ".\.gradle-local\gradle-8.12\bin\gradle.bat" @gradleArgs

$modVersion = (Read-Props "gradle.properties").mod_version
$sourceJar = "build/libs/minecraft-agent-$Target-$modVersion.jar"
$destJar = "dist/Minecraft-AI-Agent-v$modVersion-mc$Target-fabric.jar"
New-Item -ItemType Directory -Force -Path dist | Out-Null
Copy-Item -LiteralPath $sourceJar -Destination $destJar -Force
Write-Host "Packaged $destJar"
