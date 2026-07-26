# Matrix install-smoke image: JRE 21 + curl for health probes.
# Installer jars are bind-mounted at runtime (not baked in).
FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        curl \
        ca-certificates \
        bash \
        python3 \
        postgresql-client \
        default-mysql-client \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/matrix

# Build context is docker/matrix/ (see matrix-install-smoke.py docker build -f/-path).
COPY cell-entrypoint.py /usr/local/bin/cell-entrypoint.py
RUN chmod +x /usr/local/bin/cell-entrypoint.py

# Default: run one matrix cell (install + start + wait for signal).
ENTRYPOINT ["/usr/bin/python3", "/usr/local/bin/cell-entrypoint.py"]
