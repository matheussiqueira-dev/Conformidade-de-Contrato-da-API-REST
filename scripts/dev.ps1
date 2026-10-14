# Ambiente de desenvolvimento local em partes, usado pelas tarefas do VS Code ("SBI: iniciar tudo").
#   Db   -> PostgreSQL 18 descartavel (tmpfs) em 127.0.0.1:5434, container sbi-dev-db
#   Api  -> API no perfil demo (contas e massa do seed) em http://localhost:8080
#   Web  -> frontend Next.js em http://localhost:3000
#   Stop -> remove o banco de desenvolvimento
# As senhas ficam em .env.local (fora do git), gerado na primeira execucao.
param([Parameter(Mandatory)][ValidateSet('Db', 'Api', 'Web', 'Stop')][string]$Part)
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $projectDirectory '.env.local'
$dbContainer = 'sbi-dev-db'
$dbPort = 5434

function Import-LocalEnv {
    if (-not (Test-Path $envFile)) {
        $demoPassword = 'sbi-' + [guid]::NewGuid().ToString('N').Substring(0, 12)
        @(
            '# Senhas locais do ambiente de desenvolvimento (scripts/dev.ps1). Nao versionar.'
            "SBI_DB_PASSWORD=$([guid]::NewGuid().ToString('N'))"
            "SBI_DEMO_PASSWORD=$demoPassword"
        ) | Set-Content -Path $envFile -Encoding ascii
        Write-Host "Criado .env.local com a senha das contas demo: $demoPassword" -ForegroundColor Yellow
    }
    foreach ($line in Get-Content $envFile) {
        if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') {
            Set-Item -Path "env:$($Matches[1])" -Value $Matches[2]
        }
    }
}

# O stack de demonstracao (docker-compose.demo.yml) usa a porta 3000 e responde com outra senha; ele e descartavel.
function Stop-DemoStack {
    $demoContainers = & docker ps -q --filter 'label=com.docker.compose.project=sbi-demo'
    if ($demoContainers) {
        Write-Host 'Parando o stack de demonstracao sbi-demo, que ocupa a porta 3000...' -ForegroundColor Yellow
        & docker rm -f $demoContainers 2>$null | Out-Null
        Start-Sleep -Seconds 1
    }
}

function Assert-PortFree([int]$port, [string]$service) {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
        throw "Porta $port ocupada; o $service nao pode subir. Se for o stack de demonstracao, pare-o com: docker compose -f docker-compose.demo.yml -p sbi-demo down"
    }
}

Push-Location $projectDirectory
try {
    Import-LocalEnv
    switch ($Part) {
        'Db' {
            Stop-DemoStack
            & docker rm -f $dbContainer 2>$null | Out-Null
            & docker run -d --name $dbContainer `
                -e POSTGRES_DB=sbi_dev -e POSTGRES_USER=sbi -e "POSTGRES_PASSWORD=$env:SBI_DB_PASSWORD" `
                -p "127.0.0.1:${dbPort}:5432" --tmpfs /var/lib/postgresql `
                --health-cmd 'pg_isready -U sbi -d sbi_dev' --health-interval 2s --health-retries 30 `
                postgres:18 | Out-Null
            if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel iniciar o PostgreSQL (o Docker Desktop esta aberto?).' }
            # Banco novo a cada inicio: o perfil demo precisa estar ativo desde a primeira migracao.
            for ($i = 0; $i -lt 60; $i++) {
                if ((& docker inspect -f '{{.State.Health.Status}}' $dbContainer) -eq 'healthy') {
                    Write-Host "PostgreSQL pronto em 127.0.0.1:$dbPort (banco sbi_dev, usuario sbi)."
                    return
                }
                Start-Sleep -Seconds 1
            }
            throw 'PostgreSQL nao ficou pronto em 60 s.'
        }
        'Api' {
            Assert-PortFree 8080 'API'
            $env:SPRING_PROFILES_ACTIVE = 'demo'
            $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://127.0.0.1:$dbPort/sbi_dev"
            $env:SPRING_DATASOURCE_USERNAME = 'sbi'
            $env:SPRING_DATASOURCE_PASSWORD = $env:SBI_DB_PASSWORD
            $env:SESSION_COOKIE_SECURE = 'false'
            & .\mvnw.cmd spring-boot:run
        }
        'Web' {
            Stop-DemoStack
            Assert-PortFree 3000 'frontend'
            Set-Location frontend
            if (-not (Test-Path node_modules)) { & npm ci; if ($LASTEXITCODE -ne 0) { throw 'npm ci falhou.' } }
            & npm run dev
        }
        'Stop' {
            & docker rm -f $dbContainer | Out-Null
            Write-Host 'Banco de desenvolvimento removido.'
        }
    }
}
finally {
    Pop-Location
}
