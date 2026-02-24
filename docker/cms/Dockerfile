FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates bash \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/Percussion

COPY docker/entrypoint/install-update.sh /usr/local/bin/install-update.sh
RUN chmod +x /usr/local/bin/install-update.sh

ENTRYPOINT ["/usr/local/bin/install-update.sh"]
