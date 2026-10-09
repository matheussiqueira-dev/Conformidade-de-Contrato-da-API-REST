param([switch]$HttpOnly, [switch]$Frontend, [switch]$Preview)
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path $PSScriptRoot -Parent
$apiProcess = $null
$webProcess = $null
if ($Preview) { $Frontend = $true }
$composeStarted = $false
$previousIsolatedFlag = $env:A3_CONTRACT_ISOLATED
$previousTestPassword = $env:A3_TEST_PASSWORD
$previousBackendOrigin = $env:BACKEND_ORIGIN
$previousDbPassword = $env:TEST_DB_PASSWORD
Push-Location $projectDirectory
try {
    if (-not (Get-Command node -ErrorAction SilentlyContinue)) { throw 'Instale Node.js antes de executar.' }
    if (-not (Test-Path 'node_modules/ajv/dist/2020.js')) {
        & npm.cmd ci --ignore-scripts --no-audit --no-fund --fetch-retries=1 --fetch-timeout=20000 --fetch-retry-mintimeout=1000 --fetch-retry-maxtimeout=2000
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
    # Install before starting disposable services: a registry failure must not
    # look like an API startup failure or leave the user waiting through retries.
    if ($Frontend) {
        $webListener = Get-NetTCPConnection -LocalPort 3000 -State Listen -ErrorAction SilentlyContinue
        if ($webListener) { throw 'Porta 3000 ocupada. Encerre o frontend existente antes do runner isolado.' }
        Push-Location frontend
        try {
            if (Test-Path package-lock.json) {
                & npm.cmd ci --no-audit --no-fund --fetch-retries=1 --fetch-timeout=20000 --fetch-retry-mintimeout=1000 --fetch-retry-maxtimeout=2000
            }
            else {
                & npm.cmd install --no-audit --no-fund --fetch-retries=1 --fetch-timeout=20000 --fetch-retry-mintimeout=1000 --fetch-retry-maxtimeout=2000
            }
            if ($LASTEXITCODE -ne 0) { throw 'Instalacao do frontend falhou antes de iniciar API/banco. Para ECONNRESET ou timeout, verifique acesso HTTPS a registry.npmjs.org e o log npm informado acima.' }
        }
        finally { Pop-Location }
    }
    $listener = Get-NetTCPConnection -LocalPort 18080 -State Listen -ErrorAction SilentlyContinue
    if ($listener) { throw 'Porta 18080 ocupada. Encerre o processo existente antes do teste isolado.' }
    & .\mvnw.cmd -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw 'Build Java falhou.' }
    # Banco descartavel em tmpfs: uma senha nova por execucao basta.
    if (-not $env:TEST_DB_PASSWORD) { $env:TEST_DB_PASSWORD = [guid]::NewGuid().ToString('N') }
    $composeStarted = $true
    & docker compose -f docker-compose.test.yml -p a3-tests up -d --wait
    if ($LASTEXITCODE -ne 0) { throw 'Banco descartavel nao iniciou.' }
    $jar = Join-Path $projectDirectory 'target/order-management-spring-0.0.1-SNAPSHOT.jar'
    $outputDirectory = Join-Path $projectDirectory 'reports/contrato/spike-2026-10-04'
    New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
    # Explicit disposable datasource; application-integration.properties is a test resource,
    # so it is not bundled in the application jar.
    $env:A3_CONTRACT_ISOLATED = '1'
    $env:A3_TEST_PASSWORD = [guid]::NewGuid().ToString('N')
    $arguments = @('-jar', ('"' + $jar + '"'), '--spring.profiles.active=contract', '--server.address=127.0.0.1', '--server.port=18080',
        '--spring.datasource.url=jdbc:postgresql://localhost:15432/order_management_test',
        '--spring.datasource.username=postgres_test', ('--spring.datasource.password=' + $env:TEST_DB_PASSWORD),
        '--spring.jpa.hibernate.ddl-auto=create-drop', '--spring.jpa.show-sql=false')
    $apiProcess = Start-Process -FilePath (Join-Path $env:JAVA_HOME 'bin/java.exe') -ArgumentList $arguments -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $outputDirectory 'api-stdout.log') -RedirectStandardError (Join-Path $outputDirectory 'api-stderr.log')
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        if ($apiProcess.HasExited) { throw 'API encerrou antes de iniciar. Consulte api-stderr.log.' }
        try {
            $response = Invoke-WebRequest -UseBasicParsing 'http://127.0.0.1:18080/v3/api-docs' -TimeoutSec 1
            if ($response.StatusCode -eq 200) { $ready = $true; break }
        }
        catch { Start-Sleep -Milliseconds 500 }
    }
    if (-not $ready) { throw 'Timeout aguardando API isolada.' }
    $env:A3_CONTRACT_ISOLATED = '1'
    & node scripts/http-contract.mjs
    if ($LASTEXITCODE -ne 0) { throw 'Teste HTTP falhou. Consulte reports/contrato/spike-2026-10-04/http-swagger-errors-v1.json.' }
    & node scripts/http-session.mjs
    if ($LASTEXITCODE -ne 0) { throw 'Teste de sessao falhou. Consulte reports/contrato/session-2026-10-04/http-session.json.' }
    if ($Frontend) {
        Push-Location frontend
        try {
            $env:BACKEND_ORIGIN = 'http://127.0.0.1:18080'
            & npm.cmd test
            if ($LASTEXITCODE -ne 0) { throw 'Teste do cliente de sessao falhou.' }
            & npm.cmd run build
            if ($LASTEXITCODE -ne 0) { throw 'Build Next.js falhou.' }
            & npx.cmd playwright install chromium
            if ($LASTEXITCODE -ne 0) { throw 'Instalacao do navegador de teste falhou.' }
            & npm.cmd run test:e2e
            if ($LASTEXITCODE -ne 0) { throw 'E2E de sessao/proxy falhou.' }
            if ($Preview) {
                $nodeExecutable = (Get-Command node.exe -ErrorAction Stop).Source
                $nextExecutable = Join-Path $PWD 'node_modules/next/dist/bin/next'
                $webProcess = Start-Process -FilePath $nodeExecutable -ArgumentList @(('"' + $nextExecutable + '"'), 'start', '--hostname', '127.0.0.1') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $outputDirectory 'web-stdout.log') -RedirectStandardError (Join-Path $outputDirectory 'web-stderr.log')
                Write-Host 'Preview: http://127.0.0.1:3000'
                Write-Host 'Contas sinteticas: manager@example.test / seller@example.test'
                Write-Host ('Senha temporaria de ambas as contas: ' + $env:A3_TEST_PASSWORD)
                Read-Host 'Pressione Enter para encerrar o preview e o banco descartavel' | Out-Null
            }
        }
        finally { Pop-Location }
    }
}
finally {
    $env:A3_CONTRACT_ISOLATED = $previousIsolatedFlag
    $env:A3_TEST_PASSWORD = $previousTestPassword
    $env:BACKEND_ORIGIN = $previousBackendOrigin
    if ($webProcess -and -not $webProcess.HasExited) { Stop-Process -Id $webProcess.Id -Force }
    
    if ($apiProcess -and -not $apiProcess.HasExited) { Stop-Process -Id $apiProcess.Id -Force }
    if ($composeStarted) {
        & docker compose -f docker-compose.test.yml -p a3-tests down
        if ($LASTEXITCODE -ne 0) { Write-Warning 'Nao foi possivel encerrar o banco descartavel.' }
    }
    # Restaurar so depois do down: o Compose precisa da variavel para interpolar o arquivo.
    $env:TEST_DB_PASSWORD = $previousDbPassword
    Pop-Location
}
