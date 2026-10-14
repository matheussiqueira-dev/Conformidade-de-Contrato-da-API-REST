# Massa de demonstracao Olist (equivalente PowerShell de docs/bd/pacote-sql/run_demo_olist.sh).
# Sobe a stack demo, espera a API saudavel (Flyway aplicado), carrega 07_carga_olist.sql por psql dentro do
# conteiner do banco e imprime as contagens. Com -Check: carga repetida (idempotencia), testes 09 (CT-DEMO-01 a 04)
# e verificacao pela API (CT-DEMO-05).
#   $env:SBI_DEMO_PASSWORD = '<senha das contas>'; $env:SBI_DB_PASSWORD = [guid]::NewGuid().ToString('N')
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\demo-olist.ps1 [-Check]
# Banco acessivel depois: jdbc:postgresql://127.0.0.1:5433/sbi_demo (usuario sbi, senha de SBI_DB_PASSWORD).
param([switch]$Check)
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path $PSScriptRoot -Parent
Push-Location $projectDirectory
try {
    if (-not $env:SBI_DEMO_PASSWORD) { throw 'Defina SBI_DEMO_PASSWORD (senha das contas de demonstracao).' }
    if (-not $env:SBI_DB_PASSWORD) { throw 'Defina SBI_DB_PASSWORD (senha interna do banco de demonstracao).' }
    $compose = @('compose', '-f', 'docker-compose.demo.yml', '-p', 'sbi-demo')
    & docker @compose up --build -d --wait
    if ($LASTEXITCODE -ne 0) { throw 'Stack de demonstracao nao subiu.' }
    # Banco recriado sem a API (ex.: compose alterado): a API e recriada para o Flyway rodar de novo.
    $schema = (& docker @compose exec -T db psql -X -A -t -U sbi -d sbi_demo -c "SELECT to_regclass('public.app_users')") -join ''
    if (-not $schema.Trim()) {
        & docker @compose up -d --force-recreate --wait api
        if ($LASTEXITCODE -ne 0) { throw 'API nao voltou depois de recriada.' }
    }
    & docker @compose exec -T db rm -rf /tmp/pacote-sql
    & docker @compose cp docs/bd/pacote-sql db:/tmp/pacote-sql
    if ($LASTEXITCODE -ne 0) { throw 'Copia do pacote SQL para o banco falhou.' }
    $psql = @('exec', '-T', '-w', '/tmp/pacote-sql', 'db', 'psql', '-X', '-q', '-U', 'sbi', '-d', 'sbi_demo')
    & docker @compose @psql -v ON_ERROR_STOP=1 -f 07_carga_olist.sql
    if ($LASTEXITCODE -ne 0) { throw 'Carga Olist falhou (nada foi gravado: a carga e uma transacao).' }
    if ($Check) {
        & docker @compose @psql -v ON_ERROR_STOP=1 -f 07_carga_olist.sql
        if ($LASTEXITCODE -ne 0) { throw 'Segunda carga Olist falhou.' }
        # Os PASS/FAIL saem como NOTICE no stderr do psql; o PowerShell 5.1 trataria isso como erro com 'Stop'.
        $ErrorActionPreference = 'Continue'
        $tests = & docker @compose @psql -f 09_testes_olist.sql 2>&1 | Out-String -Width 400
        $ErrorActionPreference = 'Stop'
        $lines = @($tests -split "`r?`n" | Where-Object { $_ -cmatch 'NOTICE:  (PASS|FAIL) |ERROR:' } | ForEach-Object { $_ -replace '^.*NOTICE:  ', '' })
        $lines | ForEach-Object { Write-Host $_ }
        & node scripts/demo-olist-api.mjs 'http://127.0.0.1:3000/api/backend'
        $apiExit = $LASTEXITCODE
        if ($apiExit -ne 0 -or $lines.Count -eq 0 -or ($lines -cmatch '^FAIL |ERROR:')) { throw 'Verificacao da massa Olist falhou.' }
    }
    Write-Host 'Banco: jdbc:postgresql://127.0.0.1:5433/sbi_demo (usuario sbi). Roteiro SQL: docs/bd/pacote-sql/08_crud_clientes_olist.sql'
}
finally {
    Pop-Location
}
