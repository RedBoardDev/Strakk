FROM denoland/deno:2.3.3

# unzip + curl needed by the import pipeline (FDC CSV bundle download).
# ca-certificates is required for HTTPS download via curl.
USER root
RUN apt-get update && apt-get install -y --no-install-recommends \
        curl unzip ca-certificates \
    && update-ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Cache dependencies
COPY deno.json .
RUN deno install

# Copy source and import scripts
COPY src/ src/
COPY import/ import/

# Pre-create the import data directory and give the deno user ownership of /app
# so that the import pipeline can download/extract the FDC bundle into the
# named volume mounted at /app/import/data.
RUN mkdir -p /app/import/data && chown -R deno:deno /app

USER deno

EXPOSE 3000

CMD ["deno", "run", "--allow-net", "--allow-env", "src/main.ts"]
