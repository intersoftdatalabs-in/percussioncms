FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates bash python3 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/Percussion

COPY docker/entrypoint/install-update.py /usr/local/bin/install-update.py
RUN chmod +x /usr/local/bin/install-update.py

ENTRYPOINT ["/usr/local/bin/python3", "/usr/local/bin/install-update.py"]
