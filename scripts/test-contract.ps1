param([switch]$HttpOnly)
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path $PSScriptRoot -Parent
$apiProcess = $null
$composeStarted = $false
$previousIsolatedFlag = $env:A3_CONTRACT_ISOLATED
Push-Location $projectDirectory
try {
    if (-not (Get-Command node -ErrorAction SilentlyContinue)) { throw 'Instale Node.js antes de executar.' }
    if (-not (Test-Path 'node_modules/ajv/dist/2020.js')) {
        & npm.cmd ci --ignore-scripts --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) { throw 'Instalacao dos validadores falhou.' }
    }
    & npm.cmd test
    if ($LASTEXITCODE -ne 0) { throw 'Testes dos validadores falharam.' }
    if (-not $HttpOnly) {
        & node scripts/validate-contract.mjs
        if ($LASTEXITCODE -ne 0) { throw 'Validacao OpenAPI falhou ou download oficial indisponivel. Veja reports/contrato/spike-2026-10-04/validation.json. Para executar HTTP separadamente use -HttpOnly.' }
    }
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Inicie Docker Desktop e disponibilize docker no PATH.' }
    if (-not $env:JAVA_HOME) {
        $portableJdk = Join-Path (Split-Path $projectDirectory -Parent) 'tmp/tools/jdk-25/jdk-25.0.4.1+1'
        if (Test-Path (Join-Path $portableJdk 'bin/java.exe')) { $env:JAVA_HOME = $portableJdk }
    }
    if (-not $env:JAVA_HOME) { throw 'Configure JAVA_HOME para JDK 25.' }
    $listener = Get-NetTCPConnection -LocalPort 18080 -State Listen -ErrorAction SilentlyContinue
    if ($listener) { throw 'Porta 18080 ocupada. Encerre o processo existente antes do teste isolado.' }
    & .\mvnw.cmd -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw 'Build Java falhou.' }
    $composeStarted = $true
    & docker compose -f docker-compose.test.yml -p a3-tests up -d --wait
    if ($LASTEXITCODE -ne 0) { throw 'Banco descartavel nao iniciou.' }
    $jar = Join-Path $projectDirectory 'target/order-management-spring-0.0.1-SNAPSHOT.jar'
    $outputDirectory = Join-Path $projectDirectory 'reports/contrato/spike-2026-10-04'
    New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
    # Explicit disposable datasource; application-integration.properties is a test resource,
    # so it is not bundled in the application jar.
    $arguments = @('-jar', ('"' + $jar + '"'), '--server.address=127.0.0.1', '--server.port=18080',
        '--spring.datasource.url=jdbc:postgresql://localhost:15432/order_management_test',
        '--spring.datasource.username=postgres_test', '--spring.datasource.password=local_test_only',
        '--spring.jpa.hibernate.ddl-auto=create-drop', '--spring.jpa.show-sql=false')
    $apiProcess = Start-Process -FilePath (Join-Path $env:JAVA_HOME 'bin/java.exe') -ArgumentList $arguments -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $outputDirectory 'api-stdout.log') -RedirectStandardError (Join-Path $outputDirectory 'api-stderr.log')
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        if ($apiProcess.HasExited) { throw 'API encerrou antes de iniciar. Consulte api-stderr.log.' }
        try {
            $response = Invoke-WebRequest -UseBasicParsing 'http://127.0.0.1:18080/v3/api-docs' -TimeoutSec 1
            if ($response.StatusCode -eq 200) { $ready = $true; break }
        } catch { Start-Sleep -Milliseconds 500 }
    }
    if (-not $ready) { throw 'Timeout aguardando API isolada.' }
    $env:A3_CONTRACT_ISOLATED = '1'
    & node scripts/http-contract.mjs
    if ($LASTEXITCODE -ne 0) { throw 'Teste HTTP falhou. Consulte reports/contrato/spike-2026-10-04/http-swagger-errors-v1.json.' }
} finally {
    $env:A3_CONTRACT_ISOLATED = $previousIsolatedFlag
    if ($apiProcess -and -not $apiProcess.HasExited) { Stop-Process -Id $apiProcess.Id -Force }
    if ($composeStarted) {
        & docker compose -f docker-compose.test.yml -p a3-tests down
        if ($LASTEXITCODE -ne 0) { Write-Warning 'Nao foi possivel encerrar o banco descartavel.' }
    }
    Pop-Location
}
