# OpenAPI Web App

This module is an isolated web application for hosting OpenAPI documentation.

## Purpose

- Keep OpenAPI/Swagger UI delivery separate from the main `Rhythmyx` runtime
- Support Path C in the Jackson 3 migration plan by avoiding Swagger runtime dependencies in the main web app classloader
- Enable Jackson 3 migration without Swagger runtime compatibility issues

## Current Architecture

- **Deployment**: Unpacked as separate Jetty web app at `/openapi` context path
- **Static Assets**:
  - `index.html` - Swagger UI HTML interface
  - `openapi.json` - OpenAPI 3.0.3 specification (currently placeholder)
  - CSS, JavaScript, and other Swagger UI resources via `org.webjars:swagger-ui` WebJar
- **Dependencies**: No runtime Swagger dependencies in main Rhythmyx app (`rest`, `system`, `WebUI`)
  - Only `swagger-annotations` kept in main app for source-level REST resource documentation

## OpenAPI Spec Generation

### Current Implementation

- Using static `openapi.json` placeholder during initial rollout
- Spec is deployed as-is from `src/main/webapp/openapi.json`

### Future Enhancement

Build-time spec generation from REST annotations recommended using:
- **CXF OpenAPI Maven plugin** (native CXF support for OpenAPI)
- **Custom Java annotation processor** (scans `@Path`, `@Get`, etc.)
- **swagger-maven-plugin** (legacy but JAX-RS compatible)

Steps to implement:
1. Add Maven plugin to `pom.xml` that scans `rest` module JAX-RS resources
2. Configure plugin to output generated spec to `src/main/webapp/openapi.json`
3. Plugin runs during build `process-classes` phase
4. Generated spec packaged into WAR alongside Swagger UI

## Deployment

The module is configured in:
- **perc-distribution-tree/pom.xml** - Added as WAR dependency
- **perc-distribution-tree/src/main/resources/installDistributionFiles.xml** - Deploy to `${assembly-directory}/jetty/base/webapps/openapi`

Deployed structure:

```
${assembly-directory}/jetty/base/webapps/openapi/
  ├── index.html
  ├── openapi.json
  ├── WEB-INF/
  │   ├── lib/
  │   │   └── [swagger-ui WebJar dependencies]
  │   └── ...
  └── [Swagger UI static assets]
```

## Usage

After deployment, access API documentation at:
- **URL**: `http://<host>:8080/openapi/index.html`
- **OpenAPI Spec**: `http://<host>:8080/openapi/openapi.json`

## Next Steps

1. Generate `openapi.json` from REST module annotations at build time
2. Test Swagger UI loads from `/openapi` context
3. Verify REST APIs remain functional without Swagger runtime
4. Confirm Rhythmyx `WEB-INF/lib` has no Swagger artifacts leaked

## Javadoc status

This module is a WAR-only packaging artifact for the Swagger UI web
resources and contains **no Java sources**. The `maven-javadoc-plugin`
reports `No Javadoc in project. Archive not created.` during the build,
which is the expected outcome: **zero source warnings, zero plugin
warnings, zero errors, zero failures**.

The zero-warning Javadoc baseline for this module is recorded against
issue **#1945** and the broader cleanup sweep tracked by **#1909**.

## Build

Windows:

```bat
cd modules\perc-openapi-webapp
..\..\mvnw.cmd clean install
```

Linux / macOS:

```bash
cd modules/perc-openapi-webapp
../../mvnw clean install
```

Expected: `BUILD SUCCESS`. The non-javadoc warnings about a missing
`rest-8.2.0-SNAPSHOT.jar` (from the openapi-spec plugin lookup) are
expected when building this module standalone without first building
the `rest` module; they are not javadoc warnings.

