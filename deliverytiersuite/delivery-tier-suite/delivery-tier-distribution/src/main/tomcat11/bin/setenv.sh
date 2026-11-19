#!/bin/bash

CLASSPATH=$CATALINA_BASE/log4j2/lib/*:$CATALINA_BASE/log4j2/conf
LOGGING_MANAGER="-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager"

# Suppress noisy "Ignoring call to j.u.l.Logger.setUseParentHandlers(true)" warnings
# from the Log4j JUL bridge during early Tomcat startup.
JAVA_OPTS="$JAVA_OPTS -Dorg.apache.logging.log4j.jul.Log4jLogger.level=OFF @JAVA_OPTS@"

# CVE-2026-114938 (GH-6664): defense-in-depth for unpatched JDK 21 (<21.0.13.1).
case " ${CATALINA_OPTS:-} " in
  *"UseBASE64Intrinsics"*) ;;
  *) CATALINA_OPTS="-XX:-UseBASE64Intrinsics${CATALINA_OPTS:+ ${CATALINA_OPTS}}" ;;
esac
export CATALINA_OPTS
