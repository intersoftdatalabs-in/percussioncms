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
# Keep webapps from loading server Log4j/SLF4J implementation packages.
jetty.webapp.addHiddenClasses+=,org.apache.logging.log4j.
jetty.webapp.addHiddenClasses+=,org.slf4j.
