#!/bin/bash

CLASSPATH=$CATALINA_BASE/log4j2/lib/*:$CATALINA_BASE/log4j2/conf
LOGGING_MANAGER="-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager"

# Suppress noisy "Ignoring call to j.u.l.Logger.setUseParentHandlers(true)" warnings
# from the Log4j JUL bridge during early Tomcat startup.
JAVA_OPTS="$JAVA_OPTS -Dorg.apache.logging.log4j.jul.Log4jLogger.level=OFF @JAVA_OPTS@"
