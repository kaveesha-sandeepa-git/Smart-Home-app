<#
finds a local JDK that contains jlink.exe and (optionally) writes
`org.gradle.java.home=` into the repository `gradle.properties`.

Usage: run from repo root in PowerShell (Admin not required):
  .\tools\set-java-home.ps1

The script searches common install locations and $env:JAVA_HOME.
If it finds one or more JDKs with `jlink.exe` it offers to update
`gradle.properties` automatically.
#>

Write-Host "Searching for JDKs that contain jlink.exe..."

function Test-Jlink($path) {
    $exe = Join-Path $path "bin\jlink.exe"
    return Test-Path $exe
}

$candidates = @()

if ($env:JAVA_HOME) {
    if (Test-Jlink $env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
}

$programDirs = @(
    "C:\Program Files\Java",
    "C:\Program Files\Eclipse Adoptium",
    "C:\Program Files\AdoptOpenJDK",
    "C:\Program Files (x86)\AdoptOpenJDK",
    "$env:USERPROFILE\scoop\apps",
    "$env:ProgramFiles\Android\Android Studio",
    "$env:LOCALAPPDATA\Programs\Microsoft VS Code"
)

foreach ($d in $programDirs) {
    if (Test-Path $d) {
        try {
            Get-ChildItem -Path $d -Directory -ErrorAction SilentlyContinue | ForEach-Object {
                $cand = $_.FullName
                if (Test-Jlink $cand) { $candidates += $cand }
                # Also check nested jre/jdk folders
                Get-ChildItem -Path $cand -Directory -ErrorAction SilentlyContinue | ForEach-Object {
                    if (Test-Jlink $_.FullName) { $candidates += $_.FullName }
                }
            }
        } catch { }
    }
}

$candidates = $candidates | Select-Object -Unique

if (-not $candidates -or $candidates.Count -eq 0) {
    Write-Warning "No JDK with jlink.exe found in common locations."
    Write-Host "Please install a full JDK (Temurin/Adoptium, Azul, Oracle) and re-run this script."
    exit 1
}

Write-Host "Found the following JDKs with jlink.exe:"
for ($i = 0; $i -lt $candidates.Count; $i++) {
    Write-Host "[$i] $($candidates[$i])"
}

$sel = Read-Host "Select index to use (or press Enter to cancel)"
if ($sel -eq '') { Write-Host "Cancelled"; exit 0 }
if (-not ($sel -as [int]) -or [int]$sel -lt 0 -or [int]$sel -ge $candidates.Count) { Write-Error "Invalid selection"; exit 1 }

$chosen = $candidates[[int]$sel]
Write-Host "Selected: $chosen"

$propFile = Join-Path (Get-Location) 'gradle.properties'
if (-not (Test-Path $propFile)) {
    Write-Host "gradle.properties not found in repo root. Creating a new one."
    New-Item -Path $propFile -ItemType File -Force | Out-Null
}

$content = Get-Content $propFile -Raw
$escaped = $chosen -replace '\\','/'

if ($content -match '^\s*org\.gradle\.java\.home\s*=') {
    Write-Host "An existing org.gradle.java.home entry was found. It will be replaced."
    $new = $content -replace '(?m)^\s*org\.gradle\.java\.home\s*=.*$', "org.gradle.java.home=$escaped"
} else {
    Write-Host "Appending org.gradle.java.home to gradle.properties"
    $new = $content.TrimEnd() + "`norg.gradle.java.home=$escaped`n"
}

Set-Content -Path $propFile -Value $new -Encoding UTF8
Write-Host "Updated gradle.properties with org.gradle.java.home=$escaped"
Write-Host "Now run your Gradle build (e.g. .\gradlew assembleDebug)."
