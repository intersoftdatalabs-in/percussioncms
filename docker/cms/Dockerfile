# Dev cms-dts image: JRE 21 + curl + Rhythmyx HEALTHCHECK (#2481).
# Host install is bind-mounted; entrypoint only starts services.
#
# Build context is the repo root (see docker-compose.yml cms-dts service).
FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates bash python3 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/Percussion

# CVE-2026-114938 (GH-6664): defense-in-depth if cached base image precedes Temurin 21.0.13.1+2.
ENV JDK_JAVA_OPTIONS="-XX:-UseBASE64Intrinsics"

COPY docker/entrypoint/install-update.py /usr/local/bin/install-update.py
COPY docker/scripts/rhythmyx_ready.py /usr/local/lib/perc/rhythmyx_ready.py
COPY docker/scripts/rhythmyx_healthcheck.py /usr/local/bin/rhythmyx_healthcheck.py
RUN chmod +x /usr/local/bin/install-update.py /usr/local/bin/rhythmyx_healthcheck.py

# Prefer image HEALTHCHECK; compose may also set healthcheck (same script).
# Shorter start_period: host install is already present (service-only mode).
HEALTHCHECK --interval=20s --timeout=10s --start-period=60s --retries=15 \
    CMD ["/usr/bin/python3", "/usr/local/bin/rhythmyx_healthcheck.py"]

ENTRYPOINT ["/usr/bin/python3", "/usr/local/bin/install-update.py"]
