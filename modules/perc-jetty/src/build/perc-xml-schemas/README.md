# Percussion CMS — DigesterFactory XML schema resources (GH-1487)

These W3C XML Schema infrastructure files are placed on the Jetty server
classpath under org/apache/tomcat/util/descriptor/ so Apache Tomcat
DigesterFactory (used by Jetty ee11-apache-jsp) can resolve:

- XMLSchema.dtd
- datatypes.dtd
- xml.xsd

Without them, DigesterFactory logs WARN at startup and falls back to
non-validating mode. CMS already sets
-Dorg.eclipse.jetty.xml.XmlParser.Validating=false; shipping the files
removes the noise and enables validation if it is turned on later.

Source: redistributed from org.apache.tomcat:tomcat-servlet-api (jakarta.servlet.resources),
which packages the same W3C resources. Copyright W3C; used under the W3C
document license as redistributed by Apache Tomcat.
