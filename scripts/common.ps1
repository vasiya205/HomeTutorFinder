# Shared helpers for the PowerShell scripts (works in Windows PowerShell 5.1 and PowerShell 7).
$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$MavenVersion = '3.9.9'

function Write-Step($message) { Write-Host "==> $message" -ForegroundColor Cyan }

# 'java -version' writes to stderr; in Windows PowerShell 5.1 that must not be treated as an error.
function Get-JavaVersion {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { return (& java -version 2>&1 | ForEach-Object { "$_" } | Out-String).Trim() }
    finally { $ErrorActionPreference = $previous }
}

function Assert-Java {
    $java = Get-Command java -ErrorAction SilentlyContinue
    if (-not $java) {
        throw "Java was not found. Install JDK 21 (e.g. 'winget install EclipseAdoptium.Temurin.21.JDK'), then reopen VS Code."
    }
    $versionText = Get-JavaVersion
    if ($versionText -match 'version "(\d+)') {
        if ([int]$Matches[1] -lt 21) { throw "Java 21 or newer is required, found: $($Matches[0]). Install JDK 21 and reopen VS Code." }
    }
}

# Uses Maven from PATH, otherwise downloads a private copy into .tools/ (one time, ~10 MB).
function Get-Maven {
    $onPath = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if (-not $onPath) { $onPath = Get-Command mvn -ErrorAction SilentlyContinue }
    if ($onPath) { return $onPath.Source }

    $toolsDir = Join-Path $ProjectRoot '.tools'
    $mavenHome = Join-Path $toolsDir "apache-maven-$MavenVersion"
    $mavenCmd = Join-Path $mavenHome 'bin\mvn.cmd'
    if (Test-Path $mavenCmd) { return $mavenCmd }

    Write-Step "Maven not found on PATH - downloading Apache Maven $MavenVersion into .tools (one time only)"
    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
    $zip = Join-Path $toolsDir "maven.zip"
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $url = "https://archive.apache.org/dist/maven/maven-3/$MavenVersion/binaries/apache-maven-$MavenVersion-bin.zip"
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
    Expand-Archive -Path $zip -DestinationPath $toolsDir -Force
    Remove-Item $zip
    return $mavenCmd
}

function Get-Npm {
    $npm = Get-Command npm.cmd -ErrorAction SilentlyContinue
    if (-not $npm) { throw "Node.js was not found. Install Node.js 20 LTS or newer from https://nodejs.org, then reopen VS Code." }
    return $npm.Source
}
