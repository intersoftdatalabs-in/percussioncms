#
# Percussion core module
#

[optional]
https
ext

[depend]
ee11-deploy
http
gzip
ee11-plus
ee11-jstl
jaas
fcgi
resources
rewrite
ee11-servlets
ee11-annotations
ee11-cdi
statistics
perc-config
perc-ds
perc-logging
perc-mq
jvm

[xml]
etc/installation.properties
etc/perc-ssl.xml

[lib]
# Only Percussion-owned libs under jetty.base / defaults.
# Do NOT use lib/**.jar — that also matches ${jetty.home}/lib/** and pulls every
# EE8/EE9/EE10/EE11 Jetty jar onto the server classpath. ServiceLoader then loads
# multiple org.apache.juli.logging.Log providers (e.g. ee10 JuliLog) and JSP
# init fails with Provider org.eclipse.jetty.ee10.apache.jsp.JuliLog not found.
lib/perc/**.jar
lib/jdbc/**.jar
lib/extra/**.jar

[files]
lib/
lib/extra/
lib/perc/
lib/jdbc/
etc/
basehome:etc/login.conf|etc/login.conf
basehome:etc/installation.properties|etc/installation.properties

[ini]
jetty.deploy.monitoredPath=${jetty.base}/webapps
jetty.deploy.defaultsDescriptionPath=${jetty_perc_defaults}/perc-webdefault.xml
jetty_perc_defaults?=${jetty.base}/../defaults
jetty.server.stopTimeout=10000
jetty.server.dumpBeforeStart=true
jetty.webapp.addProtectedClasses+=,org.xml.sax.,org.w3c.,org.apache.xmlcommons.Version,org.apache.html.,org.apache.wml.,org.apache.xerces.,org.apache.xml.
[exec]
-Djava.library.path=../../bin
-Djavax.xml.parsers.SAXParserFactory=com.percussion.xml.PSSaxParserFactoryImpl
-Dorg.apache.commons.logging.LogFactory=org.apache.commons.logging.impl.LogFactoryImpl
-Dderby.system.home=../../Repository
-Dderby.drda.startNetworkServer=true
-Djava.net.preferIPv4Stack=true
-Djava.net.preferIPv4Addresses=true


