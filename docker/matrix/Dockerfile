# Matrix install-smoke image: JRE 21 + curl + Rhythmyx HEALTHCHECK (#2481).
# Installer jars are bind-mounted at runtime (not baked in).
#
# Build context is docker/ (see matrix-install-smoke.py build_matrix_image):
#   docker build -t percussion-matrix-cell:local -f docker/matrix/Dockerfile docker/
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

# Cell entrypoint + shared readiness helpers (HTTP + ApplicationContext markers).
COPY matrix/cell-entrypoint.py /usr/local/bin/cell-entrypoint.py
COPY scripts/rhythmyx_ready.py /usr/local/lib/perc/rhythmyx_ready.py
COPY scripts/rhythmyx_healthcheck.py /usr/local/bin/rhythmyx_healthcheck.py
RUN chmod +x /usr/local/bin/cell-entrypoint.py /usr/local/bin/rhythmyx_healthcheck.py

# In-image health: fail when /Rhythmyx/login is not ready OR Jetty logs show a
# dead Rhythmyx ApplicationContext (same markers as docker/scripts/rhythmyx_ready.py).
# start_period covers silent install + first Jetty start inside matrix cells.
# Operators: docker inspect -f "{{.State.Health.Status}}" perc-matrix-cms-h2
HEALTHCHECK --interval=30s --timeout=15s --start-period=600s --retries=5 \
    CMD ["/usr/bin/python3", "/usr/local/bin/rhythmyx_healthcheck.py"]

# Default: run one matrix cell (install + start + wait for signal).
ENTRYPOINT ["/usr/bin/python3", "/usr/local/bin/cell-entrypoint.py"]
