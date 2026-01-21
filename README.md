# Percussion CMS

> **Note:** This branch (`development-8.1.x`) is the maintenance branch for Java 8 (JDK 1.8). All code must remain compatible with Java 8.

This is the main project for Percussion CMS - the next version of Percussion CM1 and Rhythmyx.

[![Maven Build](https://github.com/intersoftdatalabs-in/percussioncms/actions/workflows/dependency-submission.yml/badge.svg?branch=development-8.1.x)](https://github.com/intersoftdatalabs-in/percussioncms/actions/workflows/dependency-submission.yml)

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

Yes.  The latest release will be featured in the [Releases page](https://github.com/intersoftdatalabs-in/percussioncms/releases) on this GitHub project.  For Technical Support please see the [Intersoft support portal](https://percussionsupport.intsof.com). The documentation can be found on https://percussioncmshelp.intsof.com and the old Percussion community has beene re-homed to https://percussioncmshelp.intsof.com.

## Development Setup

### Prerequisites

* **Java Development Kit (JDK) 8**: This project requires Java 8. We recommend [Amazon Corretto 8](https://aws.amazon.com/corretto/) or [Eclipse Adoptium (Temurin) 8](https://adoptium.net/).
* **Git**: To clone the repository.

### Setting up the Toolchain

#### Windows

1. Download and install JDK 8.
2. Set the `JAVA_HOME` environment variable to your JDK 8 installation directory.
3. Add `%JAVA_HOME%\bin` to your `PATH`.
4. Verify installation by running `java -version` in a command prompt. It should output version `1.8.x`.

#### Linux

1. Install JDK 8 using your package manager or download a tarball.
   * Ubuntu/Debian: `sudo apt-get install openjdk-8-jdk`
   * RHEL/CentOS: `sudo yum install java-1.8.0-openjdk-devel`
2. Set `JAVA_HOME`. You can add this to your `.bashrc` or `.zshrc`:

   ```bash
   export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 # Example path, adjust as needed
   export PATH=$JAVA_HOME/bin:$PATH
   ```
3. Verify with `java -version`.

#### macOS

1. Install JDK 8. You can use Homebrew:

   ```bash
   brew tap homebrew/cask-versions
   brew install --cask temurin8
   ```

   Or download from a provider.

2. Set `JAVA_HOME`:

   ```bash
   export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
   ```
3. Verify with `java -version`.

### Environment Setup Scripts

To ensure Maven uses JDK 8, use the provided environment setup scripts instead of running `mvnw` directly. These scripts set `JAVA_HOME` to `JAVA_HOME_8` and run Maven.

#### Linux/macOS

1. Set `JAVA_HOME_8` in your shell profile (e.g., `~/.bashrc` or `~/.zshrc`):

   ```bash
   export JAVA_HOME_8=/path/to/jdk-8
   ```

   - Linux example: `export JAVA_HOME_8=/usr/lib/jvm/java-1.8.0-amazon-corretto`
   - macOS example: `export JAVA_HOME_8=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home`
2. Run Maven commands using the script:

   ```bash
   ./mvn-env.sh clean install
   ```

#### Windows

1. Set `JAVA_HOME_8` as an environment variable:
   - Open System Properties > Environment Variables.
   - Add a new user variable `JAVA_HOME_8` with value `C:\path\to\jdk-8` (e.g., `C:\Program Files\Java\jdk-8`).
2. Run Maven commands using the script:

   ```batch
   mvn-env.bat clean install
   ```

If `JAVA_HOME_8` is not set or invalid, the script will display an error message with setup instructions.

### Building the Project

This project uses the Maven Wrapper (`mvnw`), which ensures the correct Maven version is used. To ensure JDK 8 is used, use the environment setup scripts.

1. Clone the repository:

   ```bash
   git clone https://github.com/intersoftdatalabs-in/percussioncms.git
   cd percussioncms
   ```
2. Build the project:
   * **Linux/macOS**:

     ```bash
     ./mvn-env.sh clean install
     ```
   * **Windows**:

     ```batch
     mvn-env.bat clean install
     ```

   To skip tests (for a faster build):

   ```bash
   ./mvn-env.sh clean install -DskipTests
   ```

### Installing the Application

After building the project, you can install it using the distribution tool.

#### Installing the Main CMS Application

The main CMS application can be installed using the `perc-distribution-tree.jar`:

```bash
java -jar ./modules/perc-distribution-tree/target/perc-distribution-tree.jar <installation-directory>
```

**Example:**

```bash
java -jar ./modules/perc-distribution-tree/target/perc-distribution-tree.jar /opt/Percussion
```

This will deploy the application to the specified installation directory with Jetty as the application server.

#### Installing the Delivery Tier

The Delivery Tier services can be installed separately using the `delivery-tier-distribution.jar`:

```bash
java -jar ./deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/target/delivery-tier-distribution-<version>.jar <installation-directory>
```

**Example:**

```bash
java -jar ./deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/target/delivery-tier-distribution.jar /opt/PercussionDTS
```

This installs the Delivery Tier services including:
- Metadata Services
- Comments Services
- Form Processor
- Polls Services
- Membership Services
- Feeds Services

### Running the Application

After installation:

1. Navigate to your installation directory
2. Start the Jetty server:
   * **Linux/macOS**:

     ```bash
     cd <installation-directory>/jetty/base
     ./start.sh
     ```
   * **Windows**:

     ```cmd
     cd <installation-directory>\jetty\base
     start.bat
     ```
3. Access the application at `http://localhost:9992/` (default port)

To stop the server, use `stop.sh` (Linux/macOS) or `stop.bat` (Windows) in the same directory.

## Interested in Contributing?

Check out our [Contributor Page](https://github.com/intersoftdatalabs-in/percussioncms/blob/development/CONTRIBUTING.md) for more information.

