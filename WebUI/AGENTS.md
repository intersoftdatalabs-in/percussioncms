# WebUI Module Agent Guidelines

Read the root [@AGENTS.md](../AGENTS.md) for general guidelines. This file contains WebUI-specific rules that supersede general rules for this module.

## Module Overview

**WebUI** is a **hybrid J2EE WAR module** undergoing a multi-phase, multi-track modernization strategy. The application contains **8 distinct UI layers**, each with different technologies, features, and communication protocols. The module is transitioning from legacy frameworks (Dojo, jQuery, Knockout) to a unified React-based UI.

**Three Parallel Tracks:**
- **Track A:** Remove Dojo 0.4.3 security risk by migrating 5 Active Assembly screens to jQuery (next release)
- **Track B:** Build consolidated React UI to replace all legacy JS layers (long-term strategic)
- **Maintenance:** Continue supporting jQuery (~20 screens), Knockout/CUI (~8 screens), and JSF Admin/Publishing

**Current Status:**
- ✅ Phase 0: Vite build pipeline established
- ✅ Phase 1: React Dashboard complete (24 widget components)
- ✅ Phase 2: Build output separation to `target/generated-webui/`
- ✅ Phase 3: Full Maven integration validated
- ✅ Track B Home + Widget Builder: React shells (`HomeShell`, `WidgetBuilderApp`) via `PercModernUI`; classic CUI/WB clients removed on feature `989-react-cui-widget-builder`
- 🔄 Track A: Dojo→jQuery migration planned
- 🚀 Track B: Incremental React component migration ongoing

---

## UI Layers Inventory

The WebUI shares the Percussion CMS application with 7 other UI layers:

|        Layer         |                                       Technology                                       |   Screens   |       Protocol        |                                               Track                                                |
|----------------------|----------------------------------------------------------------------------------------|-------------|-----------------------|----------------------------------------------------------------------------------------------------|
| Desktop Explorer     | Java Swing + JavaFX WebView                                                            | ~10         | JAX-WS SOAP           | Legacy                                                                                             |
| Rhythmyx Admin       | JSF pages (MyFaces/Trinidad removed)                                                   | 12          | REST / pending React  | Legacy / retiring                                                                                  |
| Rhythmyx Publishing  | **React PublishingShell** (`publishModern.jsp`); classic Minuet + JSF entries redirect | Publishing  | REST/JSON (Fetch)     | **Track B (feature 990)** — exclusive Minuet views removed; JSF deep pages residual packaging only |
| Package Manager      | GWT + SmartGWT                                                                         | 3+          | GWT-RPC               | Legacy                                                                                             |
| **WebUI Legacy**     | **jQuery 3.6 + jQuery UI + Backbone**                                                  | **~20**     | **REST/JSON**         | **Maintaining**                                                                                    |
| Contributor UI (CUI) | RequireJS + Knockout.js                                                                | ~8          | REST/JSON             | Maintaining                                                                                        |
| Dojo Screens (AA/CB) | **Dojo 0.4.3**                                                                         | **5**       | **REST/JSON**         | **→Track A (jQuery)**                                                                              |
| **React Modern**     | **React 19 + TypeScript + Vite**                                                       | **Growing** | **REST/JSON (Fetch)** | **→Track B (Strategic)**                                                                           |

**Reference:** [UI Layer Inventory & Technical Research](../docs/ai-generated/tasks/#000-unified-ui-plan/ui-layer-inventory.md)

---

## Track A: Dojo → jQuery (Next Release)

**Status:** Planned for next release
**Purpose:** Remove security scan alerts from Dojo 0.4.3 (pre-1.0, from 2006)
**Scope:** ~43 custom `ps.*` modules (~10K lines) + 1,477 Dojo vendor files

**Screens Affected:**
- Active Assembly editor
- Content Browser
- Relationship Editor
- Field editing UI
- Search interface

**Key Work Items:**
1. Rewrite `ps.io.Actions` (1,179 lines) — `dojo.io.bind()` → `$.ajax()`
2. Rewrite `ps.aa.controller` (2,475 lines) — Dojo events → jQuery, Dojo classes → ES6
3. Replace Dojo widgets with jQuery UI equivalents
4. Update server-side HTML generation (PSPageTree.java, PSActionBar.java)
5. Update XSL, JSP, and HTML entry points
6. Delete Dojo vendor directory

**Reference:** [Unified UI Plan — Track A Detail](../docs/ai-generated/tasks/#000-unified-ui-plan/unified-ui-plan.md)

**DO NOT:**
- Add new Dojo code (violates Track A roadmap)
- Extend existing Dojo functionality without approval
- Create new `ps.*` modules using Dojo patterns

---

## Track B: React Unified UI (Long-Term Strategic)

**Status:** In progress (Phase 3 complete)
**Goal:** Consolidate all legacy JS layers (jQuery, Knockout, Dojo) into a single React-based UI
**Timeline:** Multi-release roadmap

**Completed Milestones:**
- ✅ Phase 0 (Build): Vite pipeline, React bridge (`PercModernUI.mount()`)
- ✅ Phase 1 (Dashboard): 24 widget components replacing Shindig gadgets
- ✅ Phase 2 (Build Structure): Output separation to `target/generated-webui/cm/`
- ✅ Phase 3 (Maven Integration): Full WAR generation tested

**Next Steps:**
- Incrementally migrate high-value screens from jQuery/Knockout
- Establish patterns for React component library
- Plan screen-by-screen migration roadmap

**Reference:** [Unified UI Plan — Track B](../docs/ai-generated/tasks/#000-unified-ui-plan/unified-ui-plan.md) | [Source Layout Migration](../docs/ai-generated/tasks/#000-webui-src-layout/webui-src-layout-migration-plan.md)

---

## Directory Structure

```
WebUI/
├── src/
│   ├── main/
│   │   ├── java/                        # Java servlets, REST controllers
│   │   ├── resources/                   # Shared config files
│   │   ├── webapp/                      # War root; JSP pages, legacy JS/CSS
│   │   │   ├── cm/                      # Deployed application root
│   │   │   │   ├── app/                 # JSP application pages
│   │   │   │   ├── jslib/               # Legacy JavaScript source (~43 ps.* modules)
│   │   │   │   ├── jslibMin/            # Generated minified JavaScript
│   │   │   │   ├── cssMin/              # Generated minified CSS
│   │   │   │   ├── modern/              # Generated React/Vite bundles
│   │   │   │   ├── vendor/              # Vendor libs (jQuery, Bootstrap, etc.)
│   │   │   │   ├── widgets/             # jQuery UI widgets
│   │   │   │   └── themes/              # CSS themes
│   │   │   ├── dce/                     # Desktop Content Explorer pages
│   │   │   ├── *.jsp                    # War root JSP files
│   │   │   └── WEB-INF/                 # Deployment configuration
│   │   └── frontend/                    # ← Node.js/React/Vite source (NEW LOCATION)
│   │       ├── package.json
│   │       ├── tsconfig.json
│   │       ├── vite.config.ts           # Modern ES2020+ build
│   │       ├── vite.legacy.config.ts    # Legacy ES5 bundles
│   │       ├── scripts/
│   │       │   └── build-legacy-bundles.js
│   │       └── src/main/ts/
│   │           ├── components/          # React components
│   │           ├── api/                 # API client code
│   │           ├── dashboard/           # Dashboard (fully migrated)
│   │           ├── hooks/               # Custom React hooks
│   │           └── index.ts             # Entry point
│   └── test/
│       ├── java/                        # JUnit 5 tests
│       └── ts/                          # Vitest tests
├── pom.xml
└── target/
    └── generated-webui/                 # Build outputs (NOT in git)
        └── cm/
            ├── modern/
            ├── jslibMin/
            └── cssMin/
```

---

## Build Pipeline

### Maven Build (Full Application)

```bash
./mvn-env.sh -pl WebUI clean install         # Build everything
./mvn-env.sh -pl WebUI clean package         # Skip test phase
./mvn-env.sh -pl WebUI clean install -DskipTests
```

**What happens:**
1. Maven invokes `frontend-maven-plugin` to:
- Install Node.js if needed
- Run `npm install`
- Run `npm run build` (TypeScript compilation via Vite)
2. Legacy bundle builder generates minified JS/CSS bundles
3. Maven packages single WAR containing:
- Compiled Java classes (src/main/java)
- JSP pages and assets (src/main/webapp)
- Generated React bundles (target/generated-webui/cm/modern)
- Generated legacy bundles (target/generated-webui/cm/jslibMin, cssMin, shared-common*, shared-finder.js)

### Node.js/npm Commands (Frontend Development)

```bash
cd WebUI/src/main/frontend

npm run dev              # Vite dev server with HMR (React)
npm run build            # Build modern + legacy bundles
npm run build:modern     # Modern ES2020+ only
npm run build:legacy     # Legacy ES5 JavaScript/CSS bundles
npm run preview          # Preview production build
npm run test             # Run Vitest tests once
npm run test:watch       # Watch mode for tests
npm run lint             # Check ESLint errors
```

### Quick Type Check & Linting

```bash
cd WebUI/src/main/frontend
npx tsc --noEmit         # Verify TypeScript types
npm run lint             # Check code style
```

---

## Coding Standards

### Java (Servlets, REST Controllers)

Follow root [@AGENTS.md](../AGENTS.md) Java standards:
- Use dependency injection
- Keep servlets thin; move logic to service classes
- Provide RESTful endpoints consumable by both legacy and modern UIs
- Use JUnit 5 + Mockito for testing
- Document API contracts clearly

### React + TypeScript (Track B, Growing)

**Component Development:**
- Functional components with React hooks only
- Strict TypeScript (no `any` types)
- One file per component: `ComponentName.tsx`
- Props interface in `ComponentName.types.ts` if complex

**Styling:**
- Bootstrap 5 classes for new components
- CSS modules: `ComponentName.module.css` for local scoping
- Avoid inline styles
- Be aware of global CSS conflicts from jQuery/legacy

**Testing (Vitest):**

```bash
npm run test             # Run once
npm run test:watch     # Development watch mode
```

**Example Component:**

```typescript
// src/main/ts/components/MyWidget.tsx
import styles from './MyWidget.module.css';

interface MyWidgetProps {
  title: string;
  count: number;
}

export function MyWidget({ title, count }: MyWidgetProps) {
  return <div className={styles.widget}>{title}: {count}</div>;
}
```

### jQuery + jQuery UI (Legacy, Under Maintenance)

**Rules:**
- Do NOT add new jQuery code
- Evaluate React migration for features you modify
- Keep jQuery isolated from React DOM

### Knockout.js (Contributor UI, Under Maintenance)

**Rules:**
- Do NOT extend Knockout bindings
- Evaluate React migration for pages you modify
- Use bridge pattern if React/Knockout coexist

### Dojo 0.4.3 (Being Removed — Track A)

**Rules:**
- ❌ Do NOT write new Dodo code
- ❌ Do NOT extend existing Dojo modules
- Contact team lead before touching Dojo files
- All Dojo code targets jQuery migration in Track A

---

## Testing Guidelines

### React/TypeScript (Vitest)

**Rules:**
- Use Vitest (not Jest)
- Filename: `ComponentName.test.tsx` or `utilityName.test.ts`
- Mock API calls; don't call real endpoints
- Test user-facing behavior, not implementation
- Target >80% coverage for new components

**Example:**

```typescript
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MyWidget } from './MyWidget';

describe('MyWidget', () => {
  it('should render title', () => {
    render(<MyWidget title="Test" count={5} />);
    expect(screen.getByText('Test: 5')).toBeInTheDocument();
  });
});
```

### Java (JUnit 5)

**Rules:**
- Use JUnit 5 (refactor any JUnit 3/4)
- Filename: `ServiceNameTest.java`
- Mock external dependencies with Mockito
- Test servlets, REST endpoints, business logic

---

## Backend Integration

### REST API Contract

Create clear, typed endpoints:

```java
@RestController
@RequestMapping("/api/items")
public class ItemController {
  @GetMapping("/{id}")
  public ItemDTO getItem(@PathVariable String id) {
    return itemService.findById(id);
  }
}
```

Frontend calls it:

```typescript
// src/main/ts/api/itemApi.ts
export interface ItemDTO {
  id: string;
  name: string;
  created: string;
}

export async function fetchItem(id: string): Promise<ItemDTO> {
  const res = await fetch(`/api/items/${id}`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}
```

### Authentication & Sessions

- Own from `/api/auth` endpoint
- Use HTTP-only cookies where possible
- Include `Authorization` header when needed
- Handle logout for both legacy and React UIs

### Shared Data Between Frameworks

Instead of coupling jQuery + React directly:

```
jQuery code → /api/shared-data ← React code
```

Both frameworks call the same REST endpoint, get consistent JSON.

---

## Migration Strategy

### When to Migrate to React

**Priority:**
1. Security-sensitive features
2. High-traffic screens
3. Features needing rich interactivity
4. Components that benefit from type safety

### Migration Approach

1. **Analyze current implementation** — Document JSP/jQuery/Knockout code
2. **Design REST API** — Create clean endpoints if not present
3. **Build React component** — New component in `src/main/ts/components/`
4. **Create JSP entry point** — Embed React with `PercModernUI.mount()`
5. **Test thoroughly** — Component behavior + coexistence with legacy
6. **Deploy & monitor** — Full Maven build + production verification
7. **Clean up** — Remove old code only after verification

### Best Practices

**DO:**
- ✅ Use REST APIs for inter-framework communication
- ✅ Isolate React in separate DOM subtrees
- ✅ Test coexistence before cleanup
- ✅ Document API contracts
- ✅ Maintain backward compatibility
- ✅ Write thorough tests

**DON'T:**
- ❌ Mix React and jQuery on same element
- ❌ Mix React and Knockout on same element
- ❌ Share global variables between frameworks
- ❌ Create new Dojo code
- ❌ Add new jQuery UI code
- ❌ Directly manipulate DOM from multiple frameworks

### Bridge Pattern Example

**Legacy jQuery notifying React:**

```javascript
document.dispatchEvent(new CustomEvent('itemUpdated', { detail: itemData }));
```

**React component listening:**

```typescript
useEffect(() => {
  const handleUpdate = (e: Event) => {
    const customEvent = e as CustomEvent;
    console.log('Item updated:', customEvent.detail);
  };
  document.addEventListener('itemUpdated', handleUpdate);
  return () => document.removeEventListener('itemUpdated', handleUpdate);
}, []);
```

---

## Build Outputs & WAR Packaging

### Source vs. Generated Files

**Committed to Git:**
- `src/main/webapp/cm/` — JSP pages, static assets
- `src/main/frontend/src/main/ts/` — React/TypeScript source

**Generated (NOT committed):**
- `target/generated-webui/cm/modern/` — React bundles
- `target/generated-webui/cm/jslibMin/` — Minified legacy JS
- `target/generated-webui/cm/cssMin/` — Minified legacy CSS
- `target/generated-webui/cm/shared-common.js` (and `-minuet`, `.css`, `shared-finder.js`) — intermediate concatenations from `src/main/resources/minify/*-bundles.json` via `src/main/frontend/scripts/build-legacy-bundles.js`

Do **not** check intermediate bundles into `src/main/webapp/cm/`. Maven overlays `target/generated-webui` into the WAR. Committed copies drift from source and create duplicate CodeQL false positives.

**.gitignore includes:**

```gitignore
WebUI/target/generated-webui/
WebUI/war/modern/
WebUI/war/jslibMin/
WebUI/war/cssMin/
WebUI/src/main/webapp/cm/modern/
WebUI/src/main/webapp/cm/jslibMin/
WebUI/src/main/webapp/cm/cssMin/
WebUI/src/main/webapp/cm/shared-common.js
WebUI/src/main/webapp/cm/shared-common-minuet.js
WebUI/src/main/webapp/cm/shared-finder.js
WebUI/src/main/webapp/cm/shared-common.css
WebUI/src/main/webapp/cm/shared-common-minuet.css
```

### WAR Contents

**Final `target/perc-web-ui-8.2.0-SNAPSHOT.war`:**

```
cm/
├── app/                 # JSP pages (from source)
├── jslib/               # Legacy JS source (from source)
├── jslibMin/            # Minified bundles (generated)
├── cssMin/              # Minified CSS (generated)
├── modern/              # React bundles (generated)
├── shared-common*.js/css, shared-finder.js  # intermediate concatenations (generated)
├── vendor/              # jQuery, Bootstrap, etc. (from source)
├── widgets/, themes/, api/
└── [other static assets]

WEB-INF/
├── web.xml
├── classes/             # Compiled Java
└── lib/                 # JAR dependencies
```

---

## Version Requirements

|       Branch        | Node | JDK |
|---------------------|------|-----|
| `development`       | 22   | 21  |
| `development-8.1.x` | 18   | 8   |

```bash
# development branch
export JAVA_HOME=/path/to/jdk-21
nvm use 22
./mvn-env.sh clean install

# development-8.1.x branch
export JAVA_HOME=/path/to/jdk-8
nvm use 18
./mvn-env.sh clean install
```

---

## Hot Deployment

```bash
./scripts/hot-deploy-local.py \
  --install-dir /path/to/cms-install \
  --modules webui \
  --restart
```

Rebuilds WebUI and redeploys to local installation.

### Fast iteration against the docker dev CMS (no container restart)

When the dev CMS is running via the docker compose stack at `localhost:9992` (see `docker-compose.yml` + `docker/scripts/perc-devctl.py`), **JS/TS/JSP changes do NOT require a container restart** — just rebuild and copy the artifact:

```bash
# 1. Rebuild only the modern bundle (fast: ~3s)
cd WebUI/src/main/frontend
npm run build:modern

# 2. Copy the new bundle to the runtime webapp
cp WebUI/target/generated-webui/cm/modern/assets/perc-modern-ui.js \
   /opt/Percussion/jetty/base/webapps/Rhythmyx/cm/modern/assets/perc-modern-ui.js
cp WebUI/target/generated-webui/cm/modern/assets/perc-modern-ui.js.map \
   /opt/Percussion/jetty/base/webapps/Rhythmyx/cm/modern/assets/perc-modern-ui.js.map

# 3. Copy new/modified JSPs (Jetty serves them fresh on the next request)
cp WebUI/src/main/webapp/cm/app/explorerModern.jsp \
   /opt/Percussion/jetty/base/webapps/Rhythmyx/cm/app/explorerModern.jsp
# (and the cm/pages/app/ mirror if the page is reached through the legacy Track A path)

# 4. Bust the browser cache for the new bundle
# Add a cache-buster to the JSP <script> tag, e.g.:
#   <script type="module" src="/cm/modern/assets/perc-modern-ui.js?cb=<%= System.currentTimeMillis() %>"></script>
# or hit the page with a unique querystring (Playwright tests can do this).
```

**Java code** (when you change `.java` files, e.g. `PSPathService`) **does** need a full container restart because Jetty's WebAppClassLoader doesn't hot-reload classes. The fastest path:

```bash
./mvn-env.sh -pl <module> -am install   # rebuild jar
docker compose --env-file .env.compose -f docker-compose.yml restart cms-dts
./docker/scripts/perc-devctl.py verify   # confirm health
```

**When in doubt, Jetty's `<servlet>` config can be set to `<init-param><param-name>development</param-name><param-value>true</param-value></init-param>`** so JSPs hot-reload on the next request. Default is single-recompile per deploy; flip to `true` in `web.xml` to avoid the `docker compose restart` cycle for JSP-only changes.

### Iteration cost reference

|             Change             |                        Build                        |              Restart              |   Total   |
|--------------------------------|-----------------------------------------------------|-----------------------------------|-----------|
| TS/TSX (component or path API) | `npm run build:modern` (~3 s)                       | copy + cache-buster (0 s)         | ~3 s      |
| JSP                            | copy file (0 s)                                     | none (Jetty re-serves on request) | ~1 s      |
| SCSS / styles                  | `npm run build:modern` (~3 s)                       | copy (~0 s)                       | ~3 s      |
| Java class                     | `./mvn-env.sh -pl <m> -am install` (~30–60 s)       | `docker compose restart` (~30 s)  | ~1–2 min  |
| Maven / pom                    | `./mvn-env.sh clean install` (~5–10 min first time) | `docker compose restart`          | ~5–10 min |

When iterating, prefer the cheap paths first (TS + JSP); only escalate to Java / pom when the change actually requires it.

---

## Common Tasks Cheatsheet

|        Task        |                                    Command                                    |
|--------------------|-------------------------------------------------------------------------------|
| Full build         | `./mvn-env.sh -pl WebUI clean install`                                        |
| Build (skip tests) | `./mvn-env.sh -pl WebUI clean install -DskipTests`                            |
| React dev server   | `cd WebUI/src/main/frontend && npm run dev`                                   |
| Build React only   | `cd WebUI/src/main/frontend && npm run build`                                 |
| Run tests          | `cd WebUI/src/main/frontend && npm run test:watch`                            |
| Check types        | `cd WebUI/src/main/frontend && npx tsc --noEmit`                              |
| Lint check         | `cd WebUI/src/main/frontend && npm run lint`                                  |
| Format code        | `./mvn-env.sh spotless:apply`                                                 |
| Hot deploy         | `./scripts/hot-deploy-local.py --install-dir /path --modules webui --restart` |

---

## Documentation & References

- [Unified UI Plan](../docs/ai-generated/tasks/#000-unified-ui-plan/unified-ui-plan.md) — Tracks A & B roadmap
- [UI Layer Inventory](../docs/ai-generated/tasks/#000-unified-ui-plan/ui-layer-inventory.md) — 8 UI layers analysis
- [WebUI Source Layout Migration](../docs/ai-generated/tasks/#000-webui-src-layout/webui-src-layout-migration-plan.md) — Build structure design
- [Phase 2: Build Output Separation](../docs/ai-generated/tasks/#000-webui-src-layout/PHASE-2-BUILD-OUTPUT-SEPARATION.md) — Current setup details
- [Phase 3: Full Integration Test](../docs/ai-generated/tasks/#000-webui-src-layout/PHASE-3-FULL-INTEGRATION-TEST.md) — Maven integration validation

