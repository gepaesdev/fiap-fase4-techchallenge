# Plataforma de Feedback — FIAP Pós Tech · Tech Challenge Fase 4

Plataforma serverless em que estudantes avaliam as aulas e administradores recebem alertas de avaliações
críticas e um relatório semanal. Construída com **Java 21 + Spring Cloud Function no Azure Functions**.

> 🚧 Em desenvolvimento — as seções abaixo são preenchidas conforme cada fase é entregue.

## Sumário
1. [Modelo de nuvem e componentes](#modelo-de-nuvem-e-componentes)
2. [Arquitetura da solução](#arquitetura-da-solução)
3. [Funções serverless](#funções-serverless)
4. [API](#api)
5. [Segurança e governança](#segurança-e-governança)
6. [Monitoramento](#monitoramento)
7. [Execução local](#execução-local)
8. [Deploy](#deploy)
9. [Estrutura do projeto](#estrutura-do-projeto)

## Modelo de nuvem e componentes
_A fazer (fase 8)._

## Arquitetura da solução
```mermaid
flowchart LR
    Student([Estudante]) -- "POST /api/avaliacao" --> Submit[submit-feedback<br/>gatilho HTTP]
    Submit --> Cosmos[(Cosmos DB<br/>feedbacks)]
    Cosmos -- change feed --> Notifier[critical-feedback-notifier]
    Notifier -- somente CRITICAL --> Email[Communication Services<br/>Email]
    Timer([Agendamento: segundas 08:00 BRT]) --> Report[weekly-report]
    Report --> Cosmos
    Report --> Reports[(Cosmos DB<br/>weekly-reports)]
    Report --> Email
    Email --> Admins([Administradores])
```

## Funções serverless
| Função | Gatilho | Responsabilidade única |
|---|---|---|
| `submit-feedback-function` | HTTP `POST /api/avaliacao` | Validar, classificar a urgência e gravar a avaliação |
| `critical-feedback-notifier-function` | Change feed do Cosmos DB | Enviar e-mail aos administradores sobre avaliações críticas |
| `weekly-report-function` | Agendamento (semanal) | Gerar o relatório semanal, salvá-lo e enviá-lo por e-mail |

### critical-feedback-notifier-function
| Item | Detalhe |
|---|---|
| Gatilho | Change feed do container `feedbacks` (Cosmos DB), com container de controle `leases` |
| Entrada | Lote de avaliações recém-gravadas |
| O que faz | Ignora as que não são `CRITICAL` e envia um e-mail por avaliação crítica |
| Conteúdo do e-mail | Descrição, urgência e data de envio (exigidos pelo enunciado), além da nota e do id |
| Falhas | Política de *retry* exponencial (5 tentativas, de 5 s a 5 min); documentos malformados são ignorados e registrados em log |
| Configurações | `CosmosDbConnection`, `COSMOS_DATABASE`, `COSMOS_CONTAINER`, `EMAIL_PROVIDER`, `ACS_ENDPOINT`, `EMAIL_SENDER_ADDRESS`, `ADMIN_EMAILS` |

A função de envio **não sabe** que a de notificação existe: elas se comunicam apenas pelo banco (change feed),
o que mantém a responsabilidade única de cada uma.

### weekly-report-function
| Item | Detalhe |
|---|---|
| Gatilho | Agendamento (*timer*) toda **segunda-feira às 08:00 (Brasília)** — expressão `0 0 11 * * 1`, em UTC |
| Período | Segunda a domingo, no horário de Brasília. `REPORT_WEEK_OFFSET=1` (padrão) = semana anterior completa; `0` = semana atual até o momento (testes e demonstração) |
| O que faz | Lê as avaliações da semana, calcula os indicadores, salva o relatório no Cosmos DB e envia por e-mail |
| Conteúdo | Descrição, urgência e data de envio de cada avaliação; quantidade por dia; quantidade por urgência; média das notas |
| Histórico | Container `weekly-reports`, um documento por semana com `id` = data de início (ex.: `2026-10-19`). Reexecutar a mesma semana substitui o relatório (sem duplicatas) |
| Leitura eficiente | Uma consulta por dia, cada uma em uma única partição (`createdDate`) — nunca varre o banco inteiro |
| Disparo manual | `POST /admin/functions/weekly-report` (chave mestra na Azure) — requisição 4.1 da collection do Postman |
| Configurações | `COSMOS_*`, `COSMOS_REPORTS_CONTAINER`, `EMAIL_PROVIDER`, `ACS_ENDPOINT`, `EMAIL_SENDER_ADDRESS`, `ADMIN_EMAILS`, `REPORT_WEEK_OFFSET` |

O enunciado não define o formato do relatório; optamos por e-mail aos administradores **e** histórico no banco, para
que qualquer semana passada possa ser consultada pela data.

## API
```http
POST /api/avaliacao
Content-Type: application/json

{ "descricao": "string", "nota": 0..10 }
```

| Situação | Resposta |
|---|---|
| Avaliação válida | `201 Created` com `{ "id", "urgency", "createdAt" }` |
| `nota` fora de 0–10, decimal (ex.: `7.5`) ou ausente | `400 Bad Request` com a lista de problemas |
| `descricao` vazia ou com mais de 2000 caracteres | `400 Bad Request` |
| Corpo vazio ou JSON inválido | `400 Bad Request` |

**Urgência** — o enunciado exige que a urgência apareça no e-mail e no relatório, mas não define como calculá-la.
Adotamos três níveis derivados da nota:

| Nota | Urgência |
|---|---|
| 0 a 3 | `CRITICAL` (dispara o e-mail de alerta) |
| 4 a 6 | `MEDIUM` |
| 7 a 10 | `LOW` |

Observações sobre o contrato:
- `nota` aceita apenas **inteiros** de 0 a 10, conforme o enunciado (`int`); valores decimais são rejeitados em vez
  de truncados silenciosamente.
- A rota é `/avaliacao` (sem o acento do enunciado) para manter a URL segura em ASCII.
- Os nomes dos campos de entrada seguem o enunciado em português; o código-fonte é escrito em inglês.

## Segurança e governança
_A fazer (fases 6 e 8)._

## Monitoramento
_A fazer (fases 6 e 8)._

## Execução local
Tudo roda em emuladores locais — não é necessária uma assinatura da Azure.

**Pré-requisitos:** JDK 21+, Maven 3.9+, Docker Desktop e
[Azure Functions Core Tools v4](https://learn.microsoft.com/azure/azure-functions/functions-run-local).
No macOS, instale o Core Tools pelo terminal:
```bash
brew tap azure/functions
brew trust azure/functions
brew install azure-functions-core-tools@4
```

Com o **Docker Desktop aberto**, execute no terminal, na raiz do projeto:
```bash
# 1. Compilar e rodar todos os testes
mvn clean verify

# 2. Subir os emuladores (Cosmos DB vNext + Azurite)
docker compose up -d

# 3. Configuração local (ignorada pelo Git). Contém apenas a chave pública e padrão do emulador.
cp submit-feedback-function/local.settings.example.json submit-feedback-function/local.settings.json

# 4. Rodar a função — http://localhost:7071/api/avaliacao
mvn -pl submit-feedback-function -am install -DskipTests
mvn -pl submit-feedback-function azure-functions:run
```

Para rodar também as outras funções, cada uma em **outro terminal**:
```bash
# Função 2 — notificação crítica (porta 7072)
cp critical-feedback-notifier-function/local.settings.example.json critical-feedback-notifier-function/local.settings.json
mvn -pl critical-feedback-notifier-function -am install -DskipTests
mvn -pl critical-feedback-notifier-function azure-functions:run -DfuncPort=7072

# Função 3 — relatório semanal (porta 7073)
cp weekly-report-function/local.settings.example.json weekly-report-function/local.settings.json
mvn -pl weekly-report-function -am install -DskipTests
mvn -pl weekly-report-function azure-functions:run -DfuncPort=7073
```
Para gerar o relatório sem esperar a segunda-feira:
```bash
curl -i -X POST http://localhost:7073/admin/functions/weekly-report \
  -H "Content-Type: application/json" -d '{"input": ""}'
```
Localmente não existe emulador de e-mail: com `EMAIL_PROVIDER=log`, o e-mail de alerta é **escrito no log** da
função em vez de enviado. Para receber no seu endereço, ajuste `ADMIN_EMAILS` no `local.settings.json`.

Teste rápido, em outro terminal:
```bash
curl -i -X POST http://localhost:7071/api/avaliacao \
  -H "Content-Type: application/json" \
  -d '{"descricao": "Áudio inaudível durante a aula", "nota": 2}'
```
Os documentos gravados podem ser consultados no Data Explorer do emulador: http://localhost:1234.
Com as duas funções rodando, uma avaliação com nota 0 a 3 gera, segundos depois, a linha `[LOCAL E-MAIL]` no log
da função de notificação; notas 4 a 10 não geram e-mail.

**Postman:** importe `docs/postman/feedback-platform.postman_collection.json` e o environment
`local.postman_environment.json`, depois rode a collection na ordem das pastas (Collection Runner).
Localmente as chaves não são exigidas, então `functionKey` e `masterKey` ficam vazios.

| Pasta | O que valida | Requisito do enunciado |
|---|---|---|
| 1. Envio de avaliação | `201` para notas LOW, MEDIUM e CRITICAL; a crítica dispara o e-mail de urgência | Endpoint `POST /avaliacao`; notificação de itens críticos |
| 2. Limites da urgência | Notas 0, 3, 4, 6, 7 e 10 | Classificação de urgência |
| 3. Validações | `400` para entradas inválidas | Segurança dos dados |
| 4. Relatório semanal | Disparo manual do relatório (`202`) | Relatório semanal com média |

## Deploy
_A fazer (fases 6 e 7)._

## Estrutura do projeto
```
feedback-core/                         domínio compartilhado, sem dependências de nuvem
feedback-cosmos-adapter/               implementação das portas com Cosmos DB, usada por todas as funções
feedback-email-adapter/                envio de e-mail aos administradores (Azure Communication Services ou log local)
submit-feedback-function/              função 1 — recebimento de avaliações
critical-feedback-notifier-function/   função 2 — notificação de avaliações críticas
weekly-report-function/                função 3 — relatório semanal
infra/                                 templates Bicep (infraestrutura como código)
docs/                                  diagramas, roteiro do vídeo, collection do Postman
.github/workflows/                     pipelines de CI e deploy
```
