SET JAVA_OPTS=%JAVA_OPTS% -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager @JAVA_OPTS@
SET CLASSPATH=%CATALINA_BASE%/log4j2/lib/*;%CATALINA_BASE%/log4j2/conf;%CLASSPATH%

REM Suppress noisy JUL bridge warnings during early Tomcat startup.
SET JAVA_OPTS=%JAVA_OPTS% -Dorg.apache.logging.log4j.jul.Log4jLogger.level=OFF

REM CVE-2026-114938 (GH-6664): defense-in-depth for unpatched JDK 21 (<21.0.13.1).
SET CATALINA_OPTS=-XX:-UseBASE64Intrinsics %CATALINA_OPTS%


