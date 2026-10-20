# SentinelX

A simulated fintech engineering platform demonstrating payment processing, fraud detection, threat simulation, and collaborative engineering workflows.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      Next.js Frontend (3000)                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              Spring Boot Modular Monolith (8080)                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐   │
│  │ Auth     │ │ Payments │ │ Risk     │ │ ThreatLab        │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐   │
│  │Community │ │ DryRun   │ │ Headlines│ │ OpsSentinel      │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                         │
│  │ AI       │ │ Shared   │ │ Events   │                         │
│  └──────────┘ └──────────┘ └──────────┘                         │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
   PostgreSQL 16         Redis 7              Kafka 3.7
   (Primary DB)         (Cache/Idempotency)  (Async Events)
```

## Core Modules

### 1. SentinelPay + Risk Engine
- **Simulated payment processing** with full transaction lifecycle (CREATED → PROCESSING → RISK_REVIEW → AUTHORIZED → COMPLETED)
- **Idempotency** via Redis + DB unique constraints
- **Deterministic risk rules** with explainable scoring (ALLOW/REVIEW/BLOCK)
- **Audit trail** for every state transition

### 2. Threat Lab + Dry Run
- **Controlled simulations** for fraud patterns, velocity attacks, account takeover, API abuse
- **Reproduction engine** to replay reported issues
- **Dry-run framework** to test proposed rule changes against historical transactions
- **Before/after comparison** with false positive/negative delta analysis

### 3. Community Intelligence + Headlines
- **Three post types**: Flaw Reports, Coder Blocks, Feature Proposals
- **Tag-based clustering** with Jaccard similarity for relationship detection
- **Automatic headline generation** when 3+ related reports detected
- **Evidence-based** - always shows underlying reports and simulations

### 4. OpsSentinel
- **Internal monitoring** of API latency, error rates, payment processing
- **Automated incident detection** for error spikes, latency spikes
- **Incident lifecycle**: DETECTED → INVESTIGATING → RESOLVED
- **AI-assisted root cause analysis** (when enabled)

## Technology Stack

| Layer | Technology |
|-------|------------|
| Frontend | Next.js 14, TypeScript, Tailwind CSS |
| Backend | Spring Boot 3.3, Java 21, Kotlin |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| Events | Apache Kafka 3.7 (KRaft) |
| Auth | JWT with Spring Security |
| Observability | Micrometer, Prometheus, Grafana |
| Infrastructure | Docker, Terraform, AWS ECS/Fargate |
| CI/CD | GitHub Actions |

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 21 (for local development)
- Node.js 20 (for frontend development)

### Local Development

1. **Start infrastructure**
```bash
cd sentinelx
docker-compose up -d
```

2. **Run backend**
```bash
cd backend
./gradlew :sentinelx-app:bootRun
```

3. **Run frontend**
```bash
cd frontend
npm install
npm run dev
```

4. **Access applications**
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Mailhog: http://localhost:8025

### Default Users (created on first run)
| Email | Password | Role |
|-------|----------|------|
| admin@sentinelx.local | admin123 | ADMIN |
| analyst@sentinelx.local | analyst123 | ANALYST |
| dev@sentinelx.local | dev123 | DEVELOPER |

## API Documentation

### Authentication
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123","fullName":"John Doe"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
```

### Payments
```bash
# Create payment
curl -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "idem_123",
    "amountCents": 10000,
    "merchantId": "MERCHANT_001",
    "deviceId": "device_abc",
    "locationCountry": "US"
  }'

# List payments
curl -X GET http://localhost:8080/api/payments \
  -H "Authorization: Bearer <token>"
```

### Risk Engine
```bash
# Evaluate risk
curl -X POST http://localhost:8080/api/risk/evaluate \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"transactionId": "<uuid>"}'

# List rules
curl -X GET http://localhost:8080/api/risk/rules \
  -H "Authorization: Bearer <token>"
```

### Threat Lab
```bash
# List scenarios
curl -X GET http://localhost:8080/api/threatlab/scenarios

# Run simulation
curl -X POST http://localhost:8080/api/threatlab/simulations/run \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"scenarioId": "<uuid>"}'

# Reproduce simulation
curl -X POST http://localhost:8080/api/threatlab/simulations/reproduce \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"simulationId": "<uuid>"}'
```

### Community
```bash
# Create report
curl -X POST http://localhost:8080/api/community/reports \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "FLAW_REPORT",
    "title": "Duplicate payment on retry",
    "description": "When a payment times out and is retried...",
    "severity": "HIGH",
    "component": "Payment API",
    "tags": ["idempotency", "retry", "duplicate"]
  }'

# List reports
curl -X GET http://localhost:8080/api/community/reports
```

### Dry Run
```bash
# Create dry run
curl -X POST http://localhost:8080/api/dryrun \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Stricter velocity rule",
    "baselineRuleVersion": 1,
    "proposedRuleJson": "{...}"
  }'

# Execute dry run
curl -X POST http://localhost:8080/api/dryrun/<uuid>/execute \
  -H "Authorization: Bearer <token>"
```

## Deployment

### AWS Infrastructure (Terraform)

```bash
cd infrastructure/terraform/environments/dev
terraform init
terraform plan -var="db_password=xxx" -var="redis_password=xxx" -var="jwt_secret=xxx" -var="app_image=ghcr.io/..."
terraform apply -var="db_password=xxx" -var="redis_password=xxx" -var="jwt_secret=xxx" -var="app_image=ghcr.io/..."
```

### Required Secrets (GitHub Actions)
- `AWS_ACCOUNT_ID`
- `ECR_REGISTRY`
- `DB_PASSWORD`
- `REDIS_PASSWORD`
- `JWT_SECRET`
- `KAFKA_BOOTSTRAP_SERVERS`

## Project Structure

```
sentinelx/
├── backend/
│   ├── sentinelx-shared/      # Shared kernel, security, events
│   ├── sentinelx-auth/        # Authentication & authorization
│   ├── sentinelx-payments/    # Payment processing
│   ├── sentinelx-risk/        # Risk engine & rules
│   ├── sentinelx-threatlab/   # Threat simulations
│   ├── sentinelx-community/   # Reports, blocks, proposals
│   ├── sentinelx-dryrun/      # Dry-run framework
│   ├── sentinelx-ops/         # Monitoring & incidents
│   ├── sentinelx-ai/          # AI features
│   └── sentinelx-app/         # Main application entry point
├── frontend/
│   ├── src/app/               # Next.js App Router pages
│   ├── src/components/        # React components
│   ├── src/lib/               # Utilities, API client, auth
│   └── src/hooks/             # Custom React hooks
├── infrastructure/
│   ├── db/init/               # Flyway migrations
│   └── terraform/             # AWS infrastructure
├── .github/workflows/         # CI/CD pipelines
└── docker-compose.yml         # Local development stack
```

## Development Guidelines

### Backend
- **Modular monolith**: Each module is a Gradle subproject
- **Domain-driven design**: Entities, repositories, services per module
- **Event-driven**: Domain events published via Spring Events + Kafka
- **Security**: JWT auth, RBAC, method-level security
- **Testing**: Unit tests + Testcontainers integration tests

### Frontend
- **App Router**: Next.js 14 with Server Components
- **State management**: Zustand for client state, SWR for server state
- **Forms**: React Hook Form + Zod validation
- **UI**: Tailwind CSS + custom component library
- **Type safety**: Full TypeScript with strict mode

## License

MIT License - see LICENSE file for details.