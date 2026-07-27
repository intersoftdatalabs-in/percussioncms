#
# Percussion Jetty Logging Module
#   Output Managed by Log4j2
#
# GH-1484: Provides Jetty capability "logging|default" so the stock logging-jetty
# module (jetty-slf4j-impl) is not also selected. Dual SLF4J providers caused
# "not a subtype" failures and NOP logger fallback at startup.
#
# GH-1485: No [exec] section. Module-level JVM args force start.jar to fork a
# second JVM; required system properties live in start.d/jvm.ini instead.
#

[depend]
perc-config
resources

[tags]
logging

[provides]
logging|default
logging-log4j2

[lib]
lib/perc-logging/**.jar

[files]
logs/
basehome:modules/perc-logging

[ini]
# Server-owned Log4j2 must be *visible* to webapps as protected (parent-first)
# classes. WEB-INF packaging excludes log4j-* jars (see WebUI packagingExcludes);
# application code such as PSConsole uses org.apache.logging.log4j.io.IoBuilder
# from log4j-iostreams on the server classpath (lib/perc-logging).
#
# GH-1484 originally used addHiddenClasses for log4j, which hid those packages
# from the webapp classloader and caused:
#   java.lang.NoClassDefFoundError: org/apache/logging/log4j/io/IoBuilder
# Use addProtectedClasses so webapps share the server Log4j2 stack and cannot
# override it with a conflicting WEB-INF copy.
jetty.webapp.addProtectedClasses+=,org.apache.logging.log4j.
# Keep server SLF4J packages hidden so WEB-INF slf4j-api (and jcl-over-slf4j)
# win for Artemis/Spring JMS (see WebUI packagingExcludes comments).
jetty.webapp.addHiddenClasses+=,org.slf4j.
