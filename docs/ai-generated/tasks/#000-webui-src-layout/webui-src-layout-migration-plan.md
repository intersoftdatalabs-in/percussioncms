# WebUI Ideal `src` Layout and Migration Map

## Goal

Normalize WebUI so:

- Web assets are managed under `src/` as source-of-truth.
- Third-party assets are isolated in `vendor` folders.
- Percussion-owned code is isolated in `app` folders.
- Existing runtime URLs (for example `/cm/jslib/...`, `/cm/jslibMin/...`, `/cm/cssMin/...`) keep working during migration.

## Current Constraints (Verified)

- `WebUI` is a WAR module and currently packages from `war/` as source.
- Many JSP/HTML files hardcode paths under `/cm/jslib/...`, `/cm/jslibMin/...`, and `/cm/cssMin/...`.
- React/Vite build exists and currently outputs into `war/modern`.
- Build configuration files (`package.json`, `package-lock.json`, `vite.config.ts`, `vite.legacy.config.ts`) are at module root.
- Legacy bundle manifest configs in `src/main/resources/minify/`.

## Git Ignore Configuration

Add the following to `.gitignore` **before migration starts**:

```gitignore
# Generated frontend outputs (do not commit)
WebUI/target/generated-webui/
WebUI/war/modern/
WebUI/war/jslibMin/
WebUI/war/cssMin/
WebUI/src/main/webapp/cm/modern/
WebUI/src/main/webapp/cm/jslibMin/
WebUI/src/main/webapp/cm/cssMin/

# Node/npm artifacts
WebUI/src/main/frontend/node_modules/
WebUI/src/main/frontend/node/
WebUI/src/main/frontend/.vite/
WebUI/src/main/frontend/dist/

# Build logs
WebUI/build.log
WebUI/rxBuild.log
```

**Rationale**: Generated folders must never be committed; only source files (vendor/, app/, pages/) belong in source control.

## Ideal Source Layout

```text
WebUI/
  src/
    main/
      java/
      resources/
      frontend/                              # Node/React/Vite source (build authoring)
        package.json                         # MOVED from WebUI/package.json
        package-lock.json                    # MOVED from WebUI/package-lock.json
        tsconfig.json                        # MOVED from WebUI/tsconfig.json
        vite.config.ts                       # MOVED from WebUI/vite.config.ts
        vite.legacy.config.ts                # MOVED from WebUI/vite.legacy.config.ts
        scripts/                             # MOVED from WebUI/scripts/
        node_modules/                        # installed by frontend-maven-plugin (NOT checked in)
        modern/                              # React app source
          src/
          public/
        legacy-bundles/                      # legacy JS/CSS bundle entry files
          perc_*.bundle.js                   # Entry definition files (reference jslib sources)

      webapp/                                # canonical web source root
        cm/
          vendor/
            js/
              npm/                           # npm-provided browser libs (jquery, bootstrap, etc.)
              legacy/                        # non-npm vendor artifacts kept for compatibility
            css/
              npm/
              legacy/
            fonts/

          app/
            js/
              legacy/                        # controllers/models/plugins/services/views/classes
              bootstrap/                     # small page boot scripts, route mounting
            css/
              legacy/
              modern/
            images/
            templates/

          pages/                             # JSP and page-level includes
            app/                             # JSP pages and includes (admin, dashboard, etc.)
            includes/                        # JSP includes (common headers, footers)
            popups/                          # Popup pages
            cui/                             # CUI - separate single-page app
              index.html
              require.js

          widgets/
          api/
          themes/
          skin-win8/
          WEB-INF/
          META-INF/

          (no generated files - see target/generated-webui/ for all generated outputs)
```

## Runtime Path Strategy

### Phase 1 Approach: Update JSP references immediately when moving source files

Do NOT keep old paths as compatibility aliases. Instead:

1. When moving `war/controllers/` → `src/main/webapp/cm/app/js/legacy/controllers/`, update all JSP files that reference `/cm/jslib/controllers/` to `/cm/app/js/legacy/controllers/`.
2. When moving `war/css/` → `src/main/webapp/cm/app/css/legacy/`, update JSP `<link>` tags from `/cm/css/` to `/cm/app/css/legacy/`.
3. By end of Phase 1, NO references to old paths remain; all JSPs point to new locations.

### Phase 2 Approach: Generated bundles stay at `/cm/jslibMin/`, `/cm/cssMin/`, `/cm/modern/`

Generated outputs (legacy bundles, React build) are emitted by Maven to `target/generated-webui/cm/` and overlaid into the WAR at package time. Runtime references these paths:

- `/cm/jslibMin/perc_*.packed.js` ← from `target/generated-webui/cm/jslibMin/`
- `/cm/cssMin/perc_*.packed.css` ← from `target/generated-webui/cm/cssMin/`
- `/cm/modern/assets/**` ← from `target/generated-webui/cm/modern/`

Why this is safe:

- Generated paths are NOT user-modifiable (they're built by Maven).
- Source paths are logical (`/cm/app/js/`, `/cm/vendor/js/`) and never change once JSPs are updated.
- No versioning issues; build artifacts are ephemeral.

## Folder Mapping (Current `war/` -> Target `src/main/webapp/cm/`)

### Vendor-centric sources (MOVED in Phase 1)

- `war/jslib/**` → `src/main/webapp/cm/vendor/js/legacy/**`
- `war/themes/**` → `src/main/webapp/cm/vendor/css/legacy/themes/**`
- `war/skin-win8/**` → `src/main/webapp/cm/vendor/css/legacy/skin-win8/**`
- `war/images/icons/**` (third-party) → `src/main/webapp/cm/vendor/images/icons/**`

### App-owned sources (MOVED in Phase 1)

- `war/controllers/**` → `src/main/webapp/cm/app/js/legacy/controllers/**`
- `war/models/**` → `src/main/webapp/cm/app/js/legacy/models/**`
- `war/services/**` → `src/main/webapp/cm/app/js/legacy/services/**`
- `war/plugins/**` → `src/main/webapp/cm/app/js/legacy/plugins/**`
- `war/views/**` → `src/main/webapp/cm/app/js/legacy/views/**`
- `war/classes/**` → `src/main/webapp/cm/app/js/legacy/classes/**`
- `war/css/**` → `src/main/webapp/cm/app/css/legacy/**`
- `war/images/**` (non-vendor) → `src/main/webapp/cm/app/images/**`
- `war/app/**` → `src/main/webapp/cm/pages/app/**` (JSP pages)
- `war/app/includes/**` → `src/main/webapp/cm/pages/includes/**` (JSP includes)
- `war/app/popups/**` → `src/main/webapp/cm/pages/popups/**` (popup JSPs)
- `war/widgetbuilder/**` → `src/main/webapp/cm/app/widgetbuilder/**`
- `war/widgets/**` → `src/main/webapp/cm/widgets/**`
- `war/cui/**` → `src/main/webapp/cm/pages/cui/**` (separate SPA, not under app/)
- `war/mock/**` → `src/main/webapp/cm/pages/mock/**` (test/mock pages)
- `war/testing/**` → `src/main/webapp/cm/pages/testing/**` (test harness pages)

### Generated outputs (CREATED in Phase 2)

**These are NOT source files; they are generated by Maven during build:**

- `war/jslibMin/**` → `target/generated-webui/cm/jslibMin/**` (legacy bundle output)
- `war/cssMin/**` → `target/generated-webui/cm/cssMin/**` (legacy bundle output)
- `war/modern/**` → `target/generated-webui/cm/modern/**` (React/Vite output)
- `target/generated-webui/` → overlayed into WAR at package time by maven-war-plugin

**Runtime compatibility:** WAR packaging combines `src/main/webapp/cm/` + `target/generated-webui/cm/` so both source assets and generated bundles are available at runtime.

## Build Wiring (Maven Configuration)

### WebUI/pom.xml Changes for Phase 1 & Phase 2

#### 1. frontend-maven-plugin Configuration (Phase 1)

```xml
<plugin>
  <groupId>com.github.eirslett</groupId>
  <artifactId>frontend-maven-plugin</artifactId>
  <version>1.15.1</version>
  <configuration>
    <nodeVersion>v22.22.0</nodeVersion>
    <!-- CHANGE: working directory from module root to src/main/frontend -->
    <workingDirectory>${project.basedir}/src/main/frontend</workingDirectory>
    <installDirectory>${user.home}/.m2/frontend</installDirectory>
  </configuration>
  <executions>
    <execution>
      <id>install-node-and-npm</id>
      <goals><goal>install-node-and-npm</goal></goals>
      <phase>generate-resources</phase>
    </execution>
    <execution>
      <id>npm-install</id>
      <goals><goal>npm</goal></goals>
      <phase>generate-resources</phase>
      <configuration>
        <arguments>ci</arguments>
      </configuration>
    </execution>
    <execution>
      <id>npm-build</id>
      <goals><goal>npm</goal></goals>
      <phase>generate-resources</phase>
      <configuration>
        <arguments>run build</arguments>
      </configuration>
    </execution>
  </executions>
</plugin>
```

#### 2. Vite Configuration Changes (Phase 1)

**vite.config.ts:**

```typescript
export default defineConfig({
  // ...
  build: {
    // CHANGE: output from war/modern to target/generated-webui/cm/modern
    outDir: "${project.build.directory}/generated-webui/cm/modern",
    emptyOutDir: true,
    // ...
  },
  // ...
});
```

**vite.legacy.config.ts:**

```typescript
export default defineConfig({
  // ...
  build: {
    // CHANGE: output from war to target/generated-webui/cm
    outDir: "${project.build.directory}/generated-webui/cm",
    emptyOutDir: false, // Don't wipe; other builds already in dir
    // ...
  },
  // ...
});
```

#### 3. maven-war-plugin Configuration (Phase 2)

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-war-plugin</artifactId>
  <configuration>
    <!-- Primary source: src/main/webapp root -->
    <webResources>
      <resource>
        <directory>src/main/webapp</directory>
        <targetPath>.</targetPath>
        <!-- ... existing includes/excludes ... -->
      </resource>

      <!-- NEW: Overlay generated frontend outputs -->
      <resource>
        <directory>${project.build.directory}/generated-webui</directory>
        <targetPath>.</targetPath>
        <filtering>false</filtering>
      </resource>

      <!-- ... rest of web resources ... -->
    </webResources>
  </configuration>
</plugin>
```

### build-legacy-bundles.js Configuration (Phase 2)

Update `scripts/build-legacy-bundles.js` to output to `target/generated-webui/cm/` instead of `war/`:

```javascript
const OUTPUT_DIR = path.join(WEBUI_DIR, "target", "generated-webui", "cm");
```

Alternatively, reference `${project.build.directory}` from pom.xml via environment variable.

### .gitignore Additions (Phase 1)

See Git Ignore Configuration section above.

## 4-Phase Migration Breakdown

### Phase 1: Structural Move + JSP Updates + Config Changes

**Duration:** 2-4 hours | **Risk:** Medium (requires coordination) | **Validation:** Local build, pages still load

**Scope:**

1. Create directory structure under `src/main/webapp/cm/`:
   - `vendor/{js/legacy, css/legacy, fonts}`
   - `app/{js/legacy, css/legacy, images, widgetbuilder}`
   - `pages/{app, includes, popups, cui, mock, testing}`
   - `widgets`, `api`, `themes`, `WEB-INF`, `META-INF`
2. **Move source files:**
   - `war/jslib/**` → `src/main/webapp/cm/vendor/js/legacy/**`
   - `war/css/**` → `src/main/webapp/cm/app/css/legacy/**`
   - `war/controllers/**` → `src/main/webapp/cm/app/js/legacy/controllers/**`
   - `war/models/**` → `src/main/webapp/cm/app/js/legacy/models/**`
   - `war/services/**` → `src/main/webapp/cm/app/js/legacy/services/**`
   - `war/plugins/**` → `src/main/webapp/cm/app/js/legacy/plugins/**`
   - `war/views/**` → `src/main/webapp/cm/app/js/legacy/views/**`
   - `war/classes/**` → `src/main/webapp/cm/app/js/legacy/classes/**`
   - `war/app/**` → `src/main/webapp/cm/pages/app/**`
   - `war/cui/**` → `src/main/webapp/cm/pages/cui/**` (separate SPA)
   - `war/widgets/**`, `war/widgetbuilder/**`, `war/themes/**`, `war/skin-win8/**` → `src/main/webapp/cm/{widgets,app/widgetbuilder,vendor/css/legacy/themes,vendor/css/legacy/skin-win8}/`
   - All other static files (images, API docs, mock data) → `src/main/webapp/cm/pages/` or `app/` as appropriate
3. **Update all JSP file references** (70+ files):
   - Replace `src="/cm/jslib/` with `src="/cm/app/js/legacy/` (or `/cm/vendor/js/legacy/` for third-party)
   - Replace `href="/cm/css/` with `href="/cm/app/css/legacy/`
   - Replace `href="/cm/themes/` with `href="/cm/vendor/css/legacy/themes/`
   - Replace `src="/cm/controllers/` with `src="/cm/app/js/legacy/controllers/`
   - Update all relative requires in legacy JS (if any)
4. **Move build configuration files:**
   - Move `WebUI/package.json` → `WebUI/src/main/frontend/package.json`
   - Move `WebUI/package-lock.json` → `WebUI/src/main/frontend/package-lock.json`
   - Move `WebUI/tsconfig.json` → `WebUI/src/main/frontend/tsconfig.json`
   - Move `WebUI/vite.config.ts` → `WebUI/src/main/frontend/vite.config.ts`
   - Move `WebUI/vite.legacy.config.ts` → `WebUI/src/main/frontend/vite.legacy.config.ts`
   - Move `WebUI/scripts/` → `WebUI/src/main/frontend/scripts/`
5. **Update WebUI/pom.xml:**
   - Update `frontend-maven-plugin` `<workingDirectory>` to `${project.basedir}/src/main/frontend`
   - Update `maven-war-plugin` to use `src/main/webapp` as source (instead of `war/`)
   - Add .gitignore entry for generated folders
6. **Validation:**

   ```bash
   ./mvnw -f WebUI/pom.xml clean compile
   # Check: no compilation errors
   # Check: JSP files resolve to new paths

   # Local browser test: one JSP page should load and all JS/CSS should be found
   # (Even though some files are still building, old paths should work)
   ```

---

### Phase 2: Build Output Separation

**Duration:** 1-2 hours | **Risk:** Low (config only) | **Validation:** Maven build succeeds, outputs in target/

**Scope:**

1. **Update Vite output paths** in `src/main/frontend/vite.config.ts` and `vite.legacy.config.ts`:
   - Change `outDir: "war/modern"` → `outDir: "../../../../../target/generated-webui/cm/modern"`
   - Change `outDir: "war"` → `outDir: "../../../../../target/generated-webui/cm"`
   - (Or use environment variable injected from Maven)
2. **Update legacy bundle script** (`src/main/frontend/scripts/build-legacy-bundles.js`):
   - Change `OUTPUT_DIR = path.join(WEBUI_DIR, "war")` → `OUTPUT_DIR = path.join(WEBUI_DIR, "../../../../../target/generated-webui/cm")`
   - This ensures `jslibMin/` and `cssMin/` go to target, not source
3. **Update maven-war-plugin** in `WebUI/pom.xml`:

   Add webResource overlay for `target/generated-webui/cm`:

   ```xml
   <resource>
     <directory>${project.build.directory}/generated-webui</directory>
     <targetPath>.</targetPath>
   </resource>
   ```
4. **Update .gitignore** to ensure generated folders are never committed:

   ```plaintext
   WebUI/target/generated-webui/
   WebUI/war/modern/
   WebUI/war/jslibMin/
   WebUI/war/cssMin/
   ```
5. **Clean up old output folders** from source:
   - Delete `WebUI/war/modern/` (will be regenerated in target/)
   - Delete `WebUI/war/jslibMin/` (will be regenerated in target/)
   - Delete `WebUI/war/cssMin/` (will be regenerated in target/)
6. **Validation:**

   ```bash
   ./mvnw -f WebUI/pom.xml clean package
   # Check: WebUI-*.war is created
   # Check: target/generated-webui/cm/modern/ exists (React build)
   # Check: target/generated-webui/cm/jslibMin/ exists (legacy bundles)
   # Check: No generated files in src/main/webapp/
   ```

---

### Phase 3: Full Integration Test

**Duration:** 30 min | **Risk:** Low | **Validation:** WAR deploys and pages load

1. Run full integration test:

   ```bash
   ./mvnw clean install
   ```
2. Extract and inspect WAR:

   ```bash
   unzip -l target/WebUI-*.war | grep -E "cm/(app|vendor|pages)/"
   # Verify: source files in app/, vendor/, pages/ are present
   # Verify: generated files from target/generated-webui/ are present
   ```
3. Deploy to test container and verify:
   - All JSPs load without 404s
   - CSS/JS are found at new paths
   - React bundle loads at `/cm/modern/`
   - Legacy bundles load at `/cm/jslibMin/`

---

### Phase 4: (Optional) Further Optimization

**Future:** Progressive modernization

1. Migrate legacy JS modules to ES modules (optional)
2. Replace RequireJS with modern bundler (optional)
3. Remove unused vendor libraries (cleanup)
4. Standardize on Vite for all builds (optional)

---

## Validation Checklist by Phase

### Before Phase 1

- [ ] Backup current `war/` folder
- [ ] Create feature branch: `git checkout -b feature/webui-src-layout-refactor`
- [ ] Update .gitignore in repo root and commit

### After Phase 1

- [ ] All 70+ JSP files updated to new paths
- [ ] `mvn clean compile` succeeds
- [ ] No 404s for JS/CSS in browser console
- [ ] At least one JSP page loads in browser
- [ ] Git diff shows only file moves + path updates (no logic changes)

### After Phase 2

- [ ] `mvn clean package` succeeds
- [ ] `target/generated-webui/cm/` populated with modern/, jslibMin/, cssMin/
- [ ] `target/*.war` contains files from both src/main/webapp/ and target/generated-webui/
- [ ] `src/main/webapp/` contains NO modern/, jslibMin/, or cssMin/ folders

### After Phase 3

- [ ] Full integration test passes
- [ ] WAR deploys successfully
- [ ] All pages load with correct CSS/JS
- [ ] React bundle functional if accessed
- [ ] Console shows no 404 errors

