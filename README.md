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

Teste rápido, em outro terminal:
```bash
curl -i -X POST http://localhost:7071/api/avaliacao \
  -H "Content-Type: application/json" \
  -d '{"descricao": "Áudio inaudível durante a aula", "nota": 2}'
```
Os documentos gravados podem ser consultados no Data Explorer do emulador: http://localhost:1234.

**Postman:** importe `docs/postman/feedback-platform.postman_collection.json` e o environment
`local.postman_environment.json`, depois rode a collection na ordem das pastas (Collection Runner).
Localmente as chaves de função não são exigidas, então `functionKey` fica vazio.

## Deploy
_A fazer (fases 6 e 7)._

## Estrutura do projeto
```
feedback-core/                         domínio compartilhado, sem dependências de nuvem
feedback-cosmos-adapter/               implementação das portas com Cosmos DB, usada por todas as funções
submit-feedback-function/              função 1 — recebimento de avaliações
critical-feedback-notifier-function/   função 2 — notificação de avaliações críticas
weekly-report-function/                função 3 — relatório semanal
infra/                                 templates Bicep (infraestrutura como código)
docs/                                  diagramas, roteiro do vídeo, collection do Postman
.github/workflows/                     pipelines de CI e deploy
```
