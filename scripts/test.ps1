param([ValidateSet('Unit', 'Integration')][string]$Mode = 'Unit')
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path $PSScriptRoot -Parent
$previousDbPassword = $env:TEST_DB_PASSWORD
Push-Location $projectDirectory
try {
    if (-not $env:JAVA_HOME) {
        $portableJdk = Join-Path (Split-Path $projectDirectory -Parent) 'tmp/tools/jdk-25/jdk-25.0.4.1+1'
        if (Test-Path (Join-Path $portableJdk 'bin/java.exe')) { $env:JAVA_HOME = $portableJdk }
    }
    if (-not $env:JAVA_HOME) { throw 'Configure JAVA_HOME para um JDK 25.' }
    if ($Mode -eq 'Integration') {
        if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Docker CLI nao encontrado no PATH.' }
        # Banco descartavel em tmpfs: uma senha nova por execucao basta.
        if (-not $env:TEST_DB_PASSWORD) { $env:TEST_DB_PASSWORD = [guid]::NewGuid().ToString('N') }
        & docker compose -f docker-compose.test.yml -p a3-tests up -d --wait
        if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL de teste nao iniciou.' }
        try {
            & .\mvnw.cmd -Pintegration verify
            if ($LASTEXITCODE -ne 0) { throw 'Suite de integracao falhou. Consulte target/surefire-reports.' }
        } finally {
            & docker compose -f docker-compose.test.yml -p a3-tests down
            if ($LASTEXITCODE -ne 0) { Write-Warning 'Nao foi possivel encerrar o Compose de teste.' }
        }
    } else {
        & .\mvnw.cmd test
        if ($LASTEXITCODE -ne 0) { throw 'Suite unitaria falhou. Consulte target/surefire-reports.' }
    }
} finally {
    $env:TEST_DB_PASSWORD = $previousDbPassword
    Pop-Location
}
