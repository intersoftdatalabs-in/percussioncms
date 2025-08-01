# ContentUI

* Support for an aa client action does some action on the Rhythmyx server or retrieves information for the the AA client to use.
* Classes for retrieving requested aa client actions and bundle up the information needed for an action response.
* Singleton class to support the content browser web interface. All the results returned are JSON strings that are meant to be parsed to JavaScript to objects.
* Class to generate search results in response to a search form posting. In general, the results generated are nodes readily render able in the dojo table widget in the form of JSON object.
* Package for Unit Testing.

## Requirements
- Java 11 (JDK 11)
- Maven 3.6+

## Building
To build the ContentUI module, ensure you have Java 11 installed and set as your JAVA_HOME.

```
mvn clean install
```

## Notes
- This module is configured for Java 11. Using other Java versions may result in build or runtime errors.
- All dependencies and plugins are compatible with Java 11 as specified in the `pom.xml`
