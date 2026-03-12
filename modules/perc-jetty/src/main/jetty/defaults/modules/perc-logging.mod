#
# Percussion Jetty Logging Module
#   Output Managed by Log4j2
#

[depend]
perc-config
resources

[tags]
logging

[provides]
logging-log4j2|default

[lib]
lib/perc-logging/**.jar

[files]
logs/
basehome:modules/perc-logging


[ini]

[exec]
-Dorg.eclipse.jetty.util.log.class?=org.apache.logging.log4j.appserver.jetty.Log4j2Logger


