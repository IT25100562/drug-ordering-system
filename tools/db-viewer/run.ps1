# Starts the MediSys live database viewer on http://localhost:8765
# Run from the project folder:   .\tools\db-viewer\run.ps1
# Needs: Java 17+ and the SQL Server driver (Maven downloads it on the first `mvn package`).

$ErrorActionPreference = "Stop"
$root = Resolve-Path "$PSScriptRoot\..\.."

# 1. Find Java 17+: JAVA_HOME first, then IntelliJ's bundled JDK, then java on the PATH.
$candidates = @()
if ($env:JAVA_HOME) { $candidates += "$env:JAVA_HOME\bin\java.exe" }
$candidates += Get-ChildItem "C:\Program Files\JetBrains\*\jbr\bin\java.exe" -ErrorAction SilentlyContinue |
    Sort-Object FullName -Descending | ForEach-Object { $_.FullName }
$onPath = Get-Command java -ErrorAction SilentlyContinue
if ($onPath) { $candidates += $onPath.Source }

$java = $null
foreach ($c in $candidates) {
    if (-not (Test-Path $c)) { continue }
    $v = (& cmd /c "`"$c`" -version 2>&1" | Select-Object -First 1)
    if ($v -match '"(\d+)' -and [int]$Matches[1] -ge 17) { $java = $c; break }
}
if (-not $java) { throw "Java 17 or newer not found. Set JAVA_HOME to a JDK 17+ folder." }

# 2. Find the SQL Server JDBC driver in the local Maven repository.
$jar = Get-ChildItem "$HOME\.m2\repository\com\microsoft\sqlserver\mssql-jdbc" -Recurse -Filter "mssql-jdbc-*.jar" -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch "sources|javadoc" } | Sort-Object FullName -Descending | Select-Object -First 1
if (-not $jar) { throw "SQL Server driver not found. Run 'mvn package' once so Maven downloads it." }

# 3. Run the single-file program (no build step needed).
Set-Location $root
& $java -cp $jar.FullName "$PSScriptRoot\DbViewer.java" "src\main\resources\db.properties"
