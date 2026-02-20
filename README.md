# Percussion CMS

This is the main project for Percussion CMS - the next version of Percussion CM1 and Rhythmyx.

## What is Percussion CMS?

Percussion CMS is the next generation of Percussion Software's proprietary Rhythmyx and CM1 content management products.  The original headless CMS (1999), Percussion CMS has a long history of de-coupled deployments wth easily extensible integration points for delivering content in different formats to different channels.

Our goal is to empower the developer and the marketer. Smart architecture, smart API's, smart UI.

## What can I do with it?

* Create and manage one or more web sites - small or large.
* Re-purpose Website content to database or XML channels.
* Generate static Web site
* Control content editorial through Workflows and Permissions.

## How do I get it?

Download links are available on the project [Releases page](https://github.com/intersoftdatalabs-in/percussioncms/releases)

### Commercial Providers

* [Intersoft Data Labs](https://www.intsof.com) will offer commercial support for Percussion CMS since July 2023.

[Percussion](https://www.percussion.com) ended support for Percussion CMS products in June 2023.

## I was a Percussion customer, is this where I download updates?

Yes.  The latest release will be featured in the [Releases page](https://github.com/intersoftdatalabs-in/percussioncms/releases) on this GitHub project.  For Technical Support please see the [Intersoft support portal](https://percussionsupport.intsof.com). The documentation can be found on https://percussioncmshelp.intsof.com and the old Percussion community has been re-homed to https://percussioncmshelp.intsof.com.

## Building from Source

This project requires JDK 21 for building and running. The build uses Maven toolchains to ensure compatibility.

### Prerequisites

- JDK 21 installed on your system.
- Set the `JAVA_HOME_21` environment variable to the path of your JDK 21 installation.

### Environment Setup Scripts

To ensure Maven uses JDK 21, use the provided environment setup scripts instead of running `mvn` directly. These scripts set `JAVA_HOME` to `JAVA_HOME_21` and run Maven.

#### Linux/macOS

1. Set `JAVA_HOME_21` in your shell profile (e.g., `~/.bashrc` or `~/.zshrc`):

   ```bash
   export JAVA_HOME_21=/path/to/jdk-21
   ```

   - Linux example: `export JAVA_HOME_21=/usr/lib/jvm/java-21-openjdk-amd64`
   - macOS example: `export JAVA_HOME_21=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home`
2. Run Maven commands using the script:

   ```bash
   ./mvn-env.sh clean install
   ```

#### Windows

1. Set `JAVA_HOME_21` as an environment variable:
   - Open System Properties > Environment Variables.
   - Add a new user variable `JAVA_HOME_21` with value `C:\path\to\jdk-21` (e.g., `C:\Program Files\Java\jdk-21`).
2. Run Maven commands using the script:

   ```batch
   mvn-env.bat clean install
   ```

If `JAVA_HOME_21` is not set or invalid, the script will display an error message with setup instructions.

Editor / IDE tip: To avoid your IDE (for example, VS Code) writing build outputs into Maven's `target/` directory and causing conflicts with command-line builds, add the following to your local `.vscode/settings.json` (the repository ignores `.vscode/`):

```json
{
  "java.autobuild.enabled": false,
  "files.watcherExclude": {
    "**/target/**": true,
    "**/.git/**": true
  },
  "files.exclude": {
    "**/target": true,
    "**/.vscode/out": true
  },
  "search.exclude": {
    "**/target": true
  },
  "java.project.outputPath": ".vscode/out"
}
```

These settings disable automatic Java builds, exclude `target/` from file watchers and search, and optionally direct VS Code output to `.vscode/out` to avoid collisions with Maven's `target/`.

## Interested in Contributing?

Check out our [Contributor Page](https://github.com/intersoftdatalabs-in/percussioncms/blob/development/CONTRIBUTING.md) for more information.
