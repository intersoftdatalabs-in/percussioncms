== Git Workflow
* **NEVER commit directly** to the development-8.1.x branch
* **NEVER commit without explicit permission** 
* **NEVER push to remote** without explicit user permission
* Before creating any feature branch:
1. Always pull latest changes on the base branch first
2. Always prompt to use an existing GitHub issue or to create a new issue first in order to document the bug/feature
3. Always include the issue number in the branch name (e.g., `bugfix/123-fix-logging`)
* Only push commits after user has reviewed and approved changes
* All changes must be tested locally before pushing

== Branch Information
* Branch Name: development-8.1.x
* This branch is intended to maintain compatibility with Java 8 (JDK 1.8.0) while providing security updates and bug fixes.
* All code changes in this branch must be compatible with JDK 1.8.0
* All changes must preserve existing functionality and not introduce any features that require a higher Java version.
* Maintaining backward compatibility with existing functionality is the highest priority behind security and accessibility defects.
== Java Version
* Ensure all code is compatible with JDK 1.8.0
* Build and test the project using JDK 1.8.0
* **ALWAYS set JAVA_HOME to a Java 1.8 JRE before running any build or shell commands**
* Example: `export JAVA_HOME=/usr/lib/jvm/java-1.8.0-amazon-corretto` before running `mvn` commands
== Dependencies
* Upgrade dependencies to their latest versions that are compatible with JDK 1.8.0
* Dependency versions are managed in the parent pom.xml file
* axis
** axis:axis dependencies are manaaged in static lib folder and not an external repository
* Cactus test framework is retired, remove any cactus dependencies and relocate any cactus tests to the CMLight-Main-cactus-tests module wich is currently excluded from the build.
* Any23 is retired. Remove any Any23 dependencies. Refactor code that uses Any23 to not use it.
* Add missing perc-i18n dependency where needed.
* prefer the javax namespace, do not migrate to the jakarta namespace on this branch.
