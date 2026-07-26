# Contracts

There are **no runtime contracts** introduced by this feature. The content explorer
module is a Swing/JavaFX desktop client invoked by operators; it does not expose a
public REST/SOAP/CLI surface that this feature would change.

The only "contract" surface this feature touches is the **build-time contract** between
the module's source files and the JDK 21 javadoc tool. That contract is implicit and
inherited from the parent POM (`pom.xml:2636-2653`, `maven-javadoc-plugin` 3.12.0,
`doclint=all`).

### Inherited build-time contract

|           Aspect            |             Value (from parent POM)             |       Source        |
|-----------------------------|-------------------------------------------------|---------------------|
| Plugin                      | `org.apache.maven.plugins:maven-javadoc-plugin` | `pom.xml:2636-2653` |
| Version                     | `3.12.0`                                        | `pom.xml:2639`      |
| `failOnError`               | `false`                                         | `pom.xml:2642`      |
| `failOnWarnings`            | `false`                                         | `pom.xml:2641`      |
| `doclint`                   | `all`                                           | `pom.xml:2643`      |
| Goal bound to package phase | `jar` (execution id `attach-javadocs`)          | `pom.xml:2646-2652` |

### Forbidden changes (Constitution IV: Contract & Integration Integrity)

Because this build contract is shared by every module in the mono-repo, the cleanup
**must not** introduce per-module overrides. Any change to the parent's javadoc plugin
configuration is out of scope and would be a separate ADR.

If a future cleanup wants `failOnWarnings=true`, that is a project-wide policy change
and belongs in its own spec.
