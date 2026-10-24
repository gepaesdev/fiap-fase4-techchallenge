# Feedback Platform — FIAP Pós Tech · Phase 4 Tech Challenge

Serverless platform where students rate classes and administrators receive critical-feedback alerts and a
weekly report. Built with **Java 21 + Spring Cloud Function on Azure Functions**.

> 🚧 Work in progress — sections below are filled in as each phase is delivered.

## Table of contents
1. [Cloud model and components](#cloud-model-and-components)
2. [Solution architecture](#solution-architecture)
3. [Serverless functions](#serverless-functions)
4. [API](#api)
5. [Security and governance](#security-and-governance)
6. [Monitoring](#monitoring)
7. [Running locally](#running-locally)
8. [Deploy](#deploy)
9. [Project structure](#project-structure)

## Cloud model and components
_TODO (phase 8)._

## Solution architecture
```mermaid
flowchart LR
    Student([Student]) -- "POST /api/avaliacao" --> Submit[submit-feedback<br/>HTTP trigger]
    Submit --> Cosmos[(Cosmos DB<br/>feedbacks)]
    Cosmos -- change feed --> Notifier[critical-feedback-notifier]
    Notifier -- CRITICAL only --> Email[Communication Services<br/>Email]
    Timer([Timer: Mondays 08:00 BRT]) --> Report[weekly-report]
    Report --> Cosmos
    Report --> Blob[(Blob Storage<br/>reports)]
    Report --> Email
    Email --> Admins([Administrators])
```

## Serverless functions
| Function | Trigger | Single responsibility |
|---|---|---|
| `submit-feedback-function` | HTTP `POST /api/avaliacao` | Validate, classify urgency and persist a feedback |
| `critical-feedback-notifier-function` | Cosmos DB change feed | E-mail administrators about critical feedback |
| `weekly-report-function` | Timer (weekly) | Build the weekly report, store it and e-mail it |

## API
```http
POST /api/avaliacao
Content-Type: application/json

{ "descricao": "string", "nota": 0..10 }
```
Urgency: `0–3 → CRITICAL`, `4–6 → MEDIUM`, `7–10 → LOW`.
The route is `/avaliacao` (without the accent from the challenge statement) to keep the URL ASCII-safe.

## Security and governance
_TODO (phase 6/8)._

## Monitoring
_TODO (phase 6/8)._

## Running locally
Requirements: JDK 21+, Maven 3.9+, Azure Functions Core Tools v4, Docker (Cosmos DB Emulator).
```bash
mvn clean verify
```

## Deploy
_TODO (phase 6/7)._

## Project structure
```
feedback-core/                         shared domain, no cloud dependencies
submit-feedback-function/              function 1
critical-feedback-notifier-function/   function 2
weekly-report-function/                function 3
infra/                                 Bicep templates
docs/                                  diagrams, video script, Postman collection
.github/workflows/                     CI and deploy pipelines
```
