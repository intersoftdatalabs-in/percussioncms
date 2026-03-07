# perc-jetty AI Agent Notes

## Required reading

- Read modules/perc-jetty/README.md before making changes in this module.

## Jetty version

- Current Jetty version: 12.0.25
- Source of truth: root pom.xml property "jetty.version"

### How to update Jetty

1. Update the root pom.xml property:
   - <jetty.version>NEW_VERSION</jetty.version>
2. Rebuild perc-jetty to refresh the assembled distribution:
   - ./mvn-env.sh clean install -pl modules/perc-jetty -DskipTests
3. Validate module names and dependencies under:
   - modules/perc-jetty/src/main/jetty/defaults/modules/

## Example: adding a new Jetty module

1. Create a module descriptor:
   - modules/perc-jetty/src/main/jetty/defaults/modules/example.mod
2. Enable by default (optional):
   - modules/perc-jetty/src/main/jetty/defaults/start.d/example.ini
   - Include: --module=example
3. Add related configuration files if needed:
   - modules/perc-jetty/src/main/jetty/defaults/etc/
4. If the perc module depends on it, add to:
   - modules/perc-jetty/src/main/jetty/defaults/modules/perc.mod
5. Build to verify:
   - ./mvn-env.sh clean install -pl modules/perc-jetty -DskipTests

## AI-Generated Worklogs and Change Logs

AI-generated worklogs, change notes, and implementation details should be placed in:

- Location: `modules/perc-jetty/src/site/markdown/worklog/`
- Format: Markdown (.md files)
- Naming: Use descriptive names (e.g., `jetty-12-compatibility-fixes.md`)

### Adding worklog entries to the site menu

1. Create the markdown file in `src/site/markdown/worklog/`
2. Update `modules/perc-jetty/src/site/site.xml`:
   - Add item under the "Work / Change Log" menu
   - Reference the file with `.html` extension (Maven converts .md to .html)
   - Example:

   ```xml
   <menu name="Work / Change Log">
       <item name="Jetty 12 Migration" href="./worklog/jetty-12-compatibility-fixes.html"/>
   </menu>
   ```

## Module Documentation

Module-specific documentation (README, guides, architecture, etc.) should be placed in:

- Location: `modules/perc-jetty/src/site/markdown/`
- Format: Markdown (.md files)
- Purpose: Overview, building, configuration, and usage guides

### Adding documentation to the site Overview menu

1. Create the markdown file in `src/site/markdown/`
2. Update `modules/perc-jetty/src/site/site.xml`:
   - Add item under the "Overview" menu
   - Example:

   ```xml
   <menu name="Overview">
       <item name="Overview" href="./index.html"/>
       <item name="Architecture" href="./architecture.html"/>
   </menu>
   ```
