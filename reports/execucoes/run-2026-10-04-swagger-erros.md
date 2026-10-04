# Documentação Springdoc dos erros — 04/10/2026

## Implementação

`ProductController` declara 200/400 no POST de produtos e 200/404 na consulta por ID, com content/schema explícitos. `ErrorResponse` foi anotado como `ApiErrorResponse`, com timestamp local sem offset, campos obrigatórios timestamp/status/error/message e details como array de strings ou null. As alterações são de documentação e não mudam o tratamento/serialização dos erros.

O runner HTTP ganhou um sexto caso para validar o documento OpenAPI ao vivo, as declarações 400/404 e os campos obrigatórios/formato do erro. Os casos negativos validam o corpo tanto contra a revisão de contrato quanto contra a documentação gerada. O resultado novo será `reports/contrato/spike-2026-10-04/http-swagger-errors-v1.json`; a exportação será preservada em `openapi-runtime-errors.json` após passar o caso documental. Os dois resultados HTTP anteriores e o baseline original são preservados.

Referências oficiais das anotações: https://docs.swagger.io/swagger-core/v2.2.34/apidocs/io/swagger/v3/oas/annotations/media/Schema.html e https://docs.swagger.io/swagger-core/v2.2.34/apidocs/io/swagger/v3/oas/annotations/responses/ApiResponse.html.

## Verificação e limite

Os 18 testes Node passaram; sintaxe do script Node, parser PowerShell e whitespace foram conferidos. A tentativa de build nesta sessão falhou antes da compilação por `AccessDeniedException` em `java.security` do JDK portátil. Java/Docker da sessão do usuário continuam sendo necessários para verificar o build e os seis casos HTTP. Não reutilizar a aprovação dos cinco casos anteriores como aprovação da nova documentação Java.

## Executar

### Resultado confirmado após correção

O build gerou novo jar em 04/10/2026 às 00:42:13. O relatório `http-swagger-errors-v1.json`, atualizado às 00:42:24 (America/Fortaleza), confirma os seis casos aprovados, incluindo validação do documento OpenAPI ao vivo e dos corpos 400/404 contra a exportação e a revisão estática. A exportação `openapi-runtime-errors.json` foi conferida: `ApiErrorResponse.details` aceita array/null com itens string. A documentação das duas respostas está verificada no ambiente isolado; as limitações de build/reexecução abaixo representam o histórico anterior. Permanecem revisão/publicação, CI remoto e decisões ARQ-01/DOM-01.

### Primeira execução e correção de null

Na leitura do relatório atualizado em 04/10 às 00:40:21, cinco casos passaram e a consulta inexistente falhou: o corpo retornou `details: null`, mas a anotação de array produziu apenas `type: array` na exportação Springdoc. Os relatórios anteriores à correção foram preservados em `http-swagger-before-null-fix.json` e `openapi-runtime-before-null-fix.json`.

Foi adicionado `OpenApiConfig.errorDetailsSchema`, um customizer que define somente `ApiErrorResponse.details` como JsonSchema 3.1 com tipos array/null e itens string. Mantém o payload da API e as demais declarações de documentação. O efeito do customizer ainda precisa de build/reexecução HTTP no terminal do usuário; não declarar seis casos aprovados antes de conferir o novo relatório.

Na pasta `order-management-api`, com Docker Desktop ativo:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-contract.ps1
```

Conferir o novo relatório HTTP e a exportação runtime antes de declarar a documentação Springdoc alinhada. O schema compartilhado limita status a 400–599; cada caso HTTP verifica seu status e a revisão estática também exige o status específico. Não há declaração nova de erros para outras rotas nem implementação de autenticação.
