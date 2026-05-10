# scripts/

Developer utilities. Not part of the app build.

## Contents

| Script | Language | Description |
|--------|----------|-------------|
| [`backtest/`](backtest/) | Python | Backtest meal scanning accuracy against recipe datasets |
| [`import-embeddings/`](import-embeddings/) | TypeScript (Deno) | Import food catalog embeddings into Qdrant vector database |
| [`ciqual-seed/`](ciqual-seed/) | Python | Generate SQL seed data from the French CIQUAL food composition database |
| [`test-meal-scan.sh`](test-meal-scan.sh) | Bash | Quick smoke test for the meal scanning endpoint |

## Setup

Each script directory has its own `.env` file (gitignored) and dependencies. See the README in each subdirectory for specific setup instructions.

## Notes

- These scripts are for local development and data preparation only
- They are not deployed or run in CI
- Each has its own `.gitignore` to exclude generated output
