# Continuação — contrato e regressão monetária

Data local: 03/10/2026, America/Fortaleza. Base de código: `ca79f02`, branch `conformidade-main`. Mudanças desta rodada ainda locais.

## Contexto confirmado

O trabalho anterior publicou documentos, wrapper, Compose, CI, contratos e correção do preço unitário em `ca79f02`. A última execução registrada tinha dois testes Java aprovados. Esse resultado histórico não foi reutilizado como aprovação da suíte ampliada.

## Implementação

- Nova revisão do alvo com schemas fechados por projeção e papéis documentados, sem sobrescrever os dois contratos anteriores.
- Auditoria de referências, perfis, segurança declarada, parâmetros e rastreabilidade em Node.js. Sete testes da auditoria.
- Diagnóstico com Ajv 6.15.0: positivo sintético e negativos de campos extras, mais tentativa explícita de metaschema 2020-12.
- CT-MONEY-001: novo teste unitário de R$ 4.500 + R$ 1.200 = R$ 5.700.
- Cobertura complementar do preço histórico em memória e dois testes PostgreSQL: recarga após alteração de catálogo e persistência de cliente/endereço/pagamento/total.
- `OrderFixtures`: massa sintética compartilhada, construída por teste, sem seed automático no banco de desenvolvimento.
- Perfil `integration` com banco exclusivo `order_management_test`, porta 15432, `create-drop` e transações com rollback. PostgreSQL descartável em Compose próprio.
- JUnit `@Tag("integration")`: testes unitários são o padrão sem banco; `-Pintegration verify` inclui também contexto e persistência.
- Runner PowerShell, CI atualizado para banco isolado e preservação dos relatórios Surefire.
- Wrapper corrigido para diretórios cujo atributo `Target` é nulo; a falha original era indexação de matriz nula antes de iniciar o Maven.

## Validação executada

| Verificação | Resultado real |
| --- | --- |
| `node --test scripts/audit-contract.test.mjs` | 7 aprovados, 0 falhas. |
| Auditoria do alvo inicial | 12 achados estruturais/política; mantido intacto. |
| Auditoria do alvo v2 | 10 paths, 11 operações, 0 achados nas regras implementadas. |
| Exemplo positivo no diagnóstico Ajv legado | Aceito em ambos os alvos. |
| Campos extras no alvo inicial | Aceitos, inclusive margem dentro de usuário. |
| Campos extras no alvo v2 | Rejeitados, inclusive campo aninhado. |
| Compatibilidade 2020-12 do Ajv legado | Rejeitada; ferramenta não adotada. |
| XML do POM e parser PowerShell | Leitura/parser sem erros. |
| `git diff --check` | Sem erros de whitespace. |
| Maven/testes Java nesta sessão | Não executados com sucesso: `AccessDeniedException` em `java.security`. |
| Docker/PostgreSQL | Docker fora do PATH, diretório do aplicativo sem acesso e porta 5432 sem serviço. Integração não executada. |
| Instalação de validador novo | npm bloqueado por `EACCES` ao acessar registry.npmjs.org. |

Não há medição nova de cobertura JVM, aprovação de CT-MONEY-001/003 em PostgreSQL, execução remota do CI ou aprovação humana dos cards.

## Reproduzir em ambiente com JDK/Docker disponíveis

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test.ps1 -Mode Integration
node --test scripts/audit-contract.test.mjs
node scripts/audit-contract.mjs
```

O modo integração usa apenas `docker-compose.test.yml` e o projeto Compose `a3-tests`. Não usa nem apaga o container `postgres-order`. O banco de teste é descartável; não apontar o perfil para um banco com dados que devam ser preservados.

## Próximo trabalho

### Atualização após execução no terminal do usuário

Na retomada, os relatórios locais Surefire atualizados em 03/10/2026 às 23:59:02 foram conferidos: `OrderServiceTest` tem 3 testes, `OrderPersistenceTest` tem 2 e `OrderManagementSpringApplicationTests` tem 1; todos com zero falhas, erros ou testes ignorados. Os XML registram Java 25.0.4.1 e o contexto registra PostgreSQL em `jdbc:postgresql://localhost:15432/order_management_test`.

Essa evidência supera o bloqueio de execução Java/PostgreSQL relatado acima para esta rodada no terminal do usuário. Não comprova dois runs independentes de integração, execução remota do CI nem conclusão do spike HTTP/validador 2020-12. Java e Docker ainda não estão disponíveis no PATH desta sessão do agente.

### Segunda execução de integração conferida em 04/10/2026

Os relatórios Surefire foram atualizados novamente em 04/10/2026 às 00:06:23. As mesmas três classes somam 6 testes aprovados, zero falhas, erros ou testes ignorados. O contexto registra novamente PostgreSQL em `jdbc:postgresql://localhost:15432/order_management_test`. Esta execução, comparada à leitura anterior das 23:59:02, confirma duas rodadas aprovadas da suíte ampliada. A evidência da segunda rodada foi copiada para `reports/execucoes/integration-2026-10-04-000623/` para não depender de arquivos sobrescritos em `target`.

1. Testes Java ampliados e duas rodadas de integração: conferidos; preservar evidências na revisão.
2. Instalar validador 2020-12, completar o spike HTTP e revisar contrato/perfis.
3. Fechar ARQ-01/API-01 antes das rotas dependentes de autenticação, metas e estoque.

Trello recebe detalhes por card, com implementação, ferramentas, evidências e os bloqueios reais. Cards novos de implementação não são marcados concluídos sem teste/revisão.

## Sincronização Trello conferida

14 cards receberam descritivos detalhados e foram relidos pela API: spike, contrato, ambiente/seed, README, QA-04, setup, CI, QA-02, requisitos, validação, critérios, PL-01, DOM-01 e plano de testes. Cinco cards iniciados foram movidos para Em andamento; os que já aguardavam revisão permaneceram Em revisão. Não houve conclusão automática nem mudança de responsável ou prazo.

O Trello normalizou referências com arroba como menções em três descrições; esses trechos foram reescritos em texto técnico claro e relidos. Registro: `reports/contrato/spike-2026-10-03/trello-verificacao.json`.
