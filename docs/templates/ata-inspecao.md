# Template — ata de inspeção (revisão técnica formal)

Seção 6 do enunciado: analisar requisitos, arquitetura, código, casos de teste e documentação; registrar cada problema encontrado.

```markdown
# Inspeção — <artefato>

Data: DD/MM/AAAA. Duração: hh:mm. Artefato e versão: `caminho` @ `commit`.

## Papéis

| Papel | Integrante | Responsabilidade |
| --- | --- | --- |
| Moderador | | Conduz a reunião, controla o tempo e decide o resultado. |
| Autor | | Esclarece dúvidas; não defende o artefato durante a leitura. |
| Leitor | | Lê/parafraseia o artefato trecho a trecho. |
| Registrador | | Anota cada achado na tabela abaixo. |

## Preparação

- Material distribuído em DD/MM, com antecedência mínima de 24 h.
- Tempo de preparação individual (min): ...

## Checklist aplicado

| # | Pergunta | Resultado |
| --- | --- | --- |
| 1 | Toda entrada externa é validada antes de alterar estado? | |
| 2 | Regras de negócio (status, valor, tipo) estão no serviço e não só no DTO/front? | |
| 3 | Operações que leem e depois escrevem estão protegidas contra concorrência? | |
| 4 | Exceções têm tipo e status HTTP coerentes com o contrato OpenAPI? | |
| 5 | Dinheiro usa `BigDecimal`/`Money` com 2 casas e HALF_UP? | |
| 6 | Conversão de texto usa `Locale.ROOT`? | |
| 7 | Há código duplicado ou método com complexidade ciclomática > 10? | |
| 8 | Cada regra tem teste e o teste falharia sem a regra? | |
| 9 | Requisitos/critérios citados existem e estão coerentes com o código? | |
| 10 | Documentação e comentários batem com o comportamento? | |

## Registro de achados (mínimo do enunciado)

| ID | Artefato | Descrição | Severidade | Prioridade | Status |
| --- | --- | --- | --- | --- | --- |

## Resultado

Aceito / Aceito com correções / Nova inspeção. Achados que viram defeito recebem ID `Dxxx` em `docs/defeitos.md`.

## Métricas da inspeção

Tamanho inspecionado (linhas), achados por severidade, tempo total (pessoa-hora), achados por hora.
```