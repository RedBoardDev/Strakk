# nutrition-api

Self-hosted Deno API for AI-powered meal scanning. Handles vision analysis, food embedding generation, and vector similarity search.

## Architecture

```
src/
├── main.ts          # Hono HTTP server entrypoint
├── config/          # Environment config, constants
├── domain/          # Types, interfaces
└── infra/           # External service clients (OpenAI, Qdrant, Supabase)
```

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/scan-meal` | Analyze a meal photo and return matched food items |

## Setup

```bash
# 1. Copy environment template
cp .env.example .env
# Fill in required values (see below)

# 2. Run locally
deno task dev

# 3. Run with Docker
docker compose up
```

## Environment variables

| Variable | Description | Required |
|----------|-------------|----------|
| `OPENAI_API_KEY` | OpenAI API key for vision and embeddings | Yes |
| `QDRANT_URL` | Qdrant vector database URL | Yes |
| `QDRANT_API_KEY` | Qdrant API key | Yes |
| `SUPABASE_URL` | Supabase project URL | Yes |
| `SUPABASE_SERVICE_ROLE_KEY` | Supabase service role key | Yes |

See `.env.example` for the full template.

## Deployment

```bash
# Deploy to VPS via SSH
./deploy.sh <ssh-alias>

# Force redeploy with re-import
./deploy.sh <ssh-alias> --force
```

The deploy script copies source files, builds the Docker image on the server, and restarts the container.

## Data import

```bash
# Import food embeddings into Qdrant
deno task import
```

Import scripts live in `import/` and populate the vector database from food catalog data.

## Requirements

- Deno 2.x
- Docker and Docker Compose (for deployment)
- Qdrant instance
- OpenAI API access
