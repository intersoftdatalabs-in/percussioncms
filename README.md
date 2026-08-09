# Percussion CMS

**Actively maintained by [Intersoft Data Labs](https://www.intsof.com)** · Apache 2.0 · Formerly Percussion CM1 / Rhythmyx / CM System

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Latest Release](https://img.shields.io/github/v/release/intersoftdatalabs-in/percussioncms?label=Latest%20Release)](https://github.com/intersoftdatalabs-in/percussioncms/releases)

<p align="center">
  <img src="docs/images/login-screen.png" alt="Percussion CMS Login Screen" width="720">
</p>

*The refreshed login experience in the current development line (heading toward 8.2).*

---

## What is Percussion CMS?

Percussion CMS is a mature, enterprise-grade, decoupled / headless-capable content management system with roots going back to 1999. It unifies the content production strengths of the original CM1 with the deep development and integration capabilities of Rhythmyx.

It was designed from the beginning for multi-channel delivery — websites, static sites, XML/JSON feeds, databases, and custom channels — with strong workflow, permissions, and extensibility for both marketers and developers.

**Smart architecture. Smart APIs. Smart UI.**

Intersoft Data Labs assumed full responsibility for support, maintenance, and ongoing development of the Percussion CMS product line in July 2023 after Percussion Software ended commercial support. This repository is the official open-source home of the product under the Apache 2.0 license.

---

## Latest Status (August 2026)

| Version     | Status                          | Notes |
|-------------|----------------------------------|-------|
| **8.1.7**   | Current stable release           | June 2026 – security hardening, WCAG 2.1 AA accessibility improvements, Google Analytics 4 support, REST API fixes, and dozens of quality-of-life updates |
| **8.1.6**   | Previous stable release          | January 2026 – security and dependency updates while maintaining Java 8 compatibility |
| **8.2**     | Active development / modernization | Performance, headless capabilities, UI modernization (including the new login experience shown above), and platform updates |

The 8.1.x line remains fully supported (managed in the `percussioncms-java8` repository). Work on the 8.2 release is underway on the `development` branch with a focus on platform modernization, improved developer experience, and continued accessibility/security investment.

**Upgrading to the latest 8.1.x release is strongly recommended** — recent releases contain important security patches.

---

## What can you do with it?

- Create and manage one or more websites (small sites to large multi-site deployments)
- Re-purpose content to databases, XML/JSON channels, or other delivery endpoints
- Generate static websites
- Enforce editorial control through robust workflows and fine-grained permissions
- Extend the platform with custom applications, templates, and integrations
- Run fully decoupled / headless or hybrid delivery models

---

## How do I get it?

**Binaries and installers** are published on the [Releases page](https://github.com/intersoftdatalabs-in/percussioncms/releases).

The latest stable release is always featured there.

### Commercial Support & Services

**Intersoft Data Labs** is the exclusive commercial support and maintenance provider for Percussion CMS (all versions of CMS and Rhythmyx) since July 2023.

- Production support and SLAs
- Upgrade and migration assistance
- Custom development and integrations
- Hosting / managed services options

Contact: [inquire@intsof.com](mailto:inquire@intsof.com) · [intsof.com](https://www.intsof.com) · [Support Portal](https://percussionsupport.intsof.com)

Documentation lives at [percussioncmshelp.intsof.com](https://percussioncmshelp.intsof.com).

---

## Building from Source

**Requirements:** JDK 21 (`JAVA_HOME` must point to a JDK 21 installation).

```bash
# Linux / macOS
./mvnw clean install

# Windows
mvnw.cmd clean install
```

Detailed build instructions, environment setup, CodeQL configuration, and development guidelines are in the [Contributor Guide](https://github.com/intersoftdatalabs-in/percussioncms/blob/development/CONTRIBUTING.md).

---

## Contributing

We welcome contributions. Please see the [Contributor Guide](https://github.com/intersoftdatalabs-in/percussioncms/blob/development/CONTRIBUTING.md) for coding standards, pull-request process, and development workflow.

This project follows the [Universal Code v1.0.0](docs/policies/UC-v1.0.0.md) (vendored; [upstream](https://github.com/monkeyking-hq/universal-code)).

---

## License

Apache License 2.0 — see [LICENSE](LICENSE) for details.

---

**Maintained with care by Intersoft Data Labs**  
Questions? Open an issue or reach out via the support portal.
