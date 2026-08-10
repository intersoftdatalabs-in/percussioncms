# Issue #2191 — Repro + evidence: demo-sites / Sample Site missing in Explorer

**Parent:** [#1750](https://github.com/intersoftdatalabs-in/percussioncms/issues/1750)  
**Slice:** 1 of 4 (diagnostic only — no product fix)  
**Date:** 2026-08-06  
**Operator:** Grok night-issue-prs (model grok-4.5)  
**Classification:** **Installer / ANT seed (pursue #2192 next)** — not Explorer display (#2193)

---

## Summary

When the operator opts in to sample sites (`Yes` in the interactive wizard, or `cms.demo-sites=true` in last-install), Explorer’s **Sites** tree is empty because **sample site rows never land in the repository**. The seed bundle exists on disk; the ANT `installSampleSites` path does not run with `install.demo.sites=true`.

**Decision:** Open / implement **#2192 (installer seed / flag propagation)** next. Defer **#2193 (Explorer display/ACL)** until a demo-sites install produces non-empty `RXSITES`. Playwright residual remains **#2194** after the product fix.

---

## Environment under test

|           Item           |                            Value                             |
|--------------------------|--------------------------------------------------------------|
| Install root             | `C:\Installs\8.2-july-29`                                    |
| DB                       | Embedded H2 (`Repository\CMDB.mv.db`)                        |
| Operator last-install    | `%USERPROFILE%\.intsof\percussion\last-install.properties`   |
| Install log              | `rxconfig\Installer\install.log` (ended 2026-08-02 18:17:46) |
| Install type (ANT)       | `install.type=new`                                           |
| CMS process during probe | Not required; H2 read-only JDBC used                         |

---

## Repro recipe (H2)

### Interactive

1. Run the preinstall installer against a **new** install directory (or clean H2 repository).
2. Choose DB type **H2** (defaults).
3. At prompt  
   `Install sample sites (Corporate Investments / Enterprise Investments)? [y/N]`  
   answer **`y`**.
4. Confirm and finish install.
5. Start CMS, log in, open Explorer → expand **Sites**.

**Expected:** Sites lists Corporate / Enterprise Investments (or folder children for those sites).  
**Actual (reported #1750 + this env):** Sites empty.

### Silent

```text
java -jar <preinstall-or-distribution-installer>.jar \
  --install-dir=<new-path> \
  --silent \
  --db.type=h2 \
  --demo-sites
```

Optional belt-and-braces (forces ANT JVM prop even if CLI→ANT wiring is broken):

```text
java -Dinstall.demo.sites=true -jar <installer>.jar --install-dir=<path> --silent --db.type=h2 --demo-sites
```

### Post-install checks (classification)

1. **last-install** — `%USERPROFILE%\.intsof\percussion\last-install.properties` should contain `cms.demo-sites=true` when the operator opted in.
2. **install.log** — search for `install.demo.sites` and sample-seed echoes:
   - Ran: `Seeding sample sites (Corporate Investments / Enterprise Investments)...` then `Sample sites seeded.`
   - Skipped (when `installSampleSites` is invoked with flag false):  
     `installSampleSites: skipped (install.demo.sites=…)`
   - **Silent no-op (observed):** outer `installRepository` guard does **not** call `installSampleSites` when flag ≠ `true`, so **no** skip/seed echo appears between `updating table schemas` and `Repository installation complete...`.
3. **Repository** — H2 read-only:

```text
java -cp <install>/jetty/base/lib/jdbc/h2-*.jar org.h2.tools.Shell \
  -url "jdbc:h2:file:<install>/Repository/CMDB;ACCESS_MODE_DATA=r;IFEXISTS=TRUE" \
  -user sa -password <from rxrepository.properties> \
  -sql "SELECT COUNT(*) AS SITE_COUNT FROM RXSITES; SELECT SITENAME, SITEDESC FROM RXSITES;"
```

- Seeded: expect rows `Enterprise_Investments`, `Corporate_Investments` (see seed XML).
- Unseeded (this repro): `SITE_COUNT = 0`.

4. **Explorer / REST** (when CMS is up; patterns from [#1622](https://github.com/intersoftdatalabs-in/percussioncms/issues/1622)):

```text
GET /Rhythmyx/services/pathmanagement/path/folder/
GET /Rhythmyx/services/pathmanagement/path/folder/Sites
```

With empty `RXSITES`, Sites children should be empty even if REST returns 200. Parent #1750 screenshot shows empty Sites in the UI.

---

## Evidence from `C:\Installs\8.2-july-29`

### 1. Operator opted in (`cms.demo-sites=true`)

From `%USERPROFILE%\.intsof\percussion\last-install.properties` (timestamp **Sun Aug 02 18:17:46 EDT 2026**, same minute as install.log end):

```properties
cms.db.type=h2
cms.demo-sites=true
cms.install.directory=C\:\\Installs\\8.2-july-29
```

`InstallerUserSettings` persists `demo-sites` from the preinstall **options** map after a successful install. So the wizard/CLI path recorded **true**.

### 2. ANT received `install.demo.sites=false`

From `rxconfig\Installer\install.log`:

```text
45027:[echoproperties] install.demo.sites=false
45034:[echoproperties] install.type=new
49699:[echoproperties] install.demo.sites=false
```

### 3. `installSampleSites` did not run

Between core schema load and completion there is **no** seed or skip message:

```text
45780:     [echo]  updating table schemas
45954:     [echo] Repository installation complete...
```

The complete target list includes `installSampleSites` as a **named** target, but the in-`installRepository` guard only `antcall`s it when `${install.demo.sites}` equals `true` (uses Ant-Contrib `<if>`, not core Ant):

```xml
<!-- installRepository.xml — <if> is Ant-Contrib (third-party), not standard Ant -->
<if>
  <equals arg1="${install.demo.sites}" arg2="true" />
  <then>
    <antcall target="installSampleSites" inheritrefs="true" />
  </then>
</if>
```

When false, the seed target is never entered (no `installSampleSites: skipped` echo either).

No `RxffTableData.staging.xml` was produced under `rxconfig\Installer\data\` (staging is created only inside `installSampleSites` → `stripSampleLocales`). Shipped seed files **are** present after install:

- `rxconfig\Installer\data\RxffTableData.xml` (~717 KB)
- `rxconfig\Installer\data\RxffTableDef.xml`

Seed XML contains site rows (not loaded):

```xml
<table name="RXSITES" onCreateOnly="no">
  ...
  <column name="SITENAME">Enterprise_Investments</column>
  <column name="SITEDESC">Represents the Enterprise Investments web site</column>
  ...
  <column name="SITENAME">Corporate_Investments</column>
  <column name="SITEDESC">Represents the Corporate Investments web site</column>
```

### 4. Repository: `RXSITES` empty

Read-only H2 probe (2026-08-06) against `Repository\CMDB`:

|             Check              |                               Result                               |
|--------------------------------|--------------------------------------------------------------------|
| `SELECT COUNT(*) FROM RXSITES` | **0**                                                              |
| `SELECT * FROM RXSITES`        | 0 rows (table exists; schema from core `cmsTable*` load)           |
| `PSX_FOLDER`                   | 11 system folder rows only; no Corporate/Enterprise sample folders |

Artifacts (local worktree, not required in git): `tmp/issue-2191/h2-probe-rxsites.txt`, `install-log-snippets.txt`, copy of `last-install.properties`.

---

## Code path analysis (root-cause hypothesis for #2192)

This slice does **not** change product code; the following is diagnostic only.

### Flag resolution in the wizard (works)

`InteractiveInstallWizard.resolveDemoSites`:

- Interactive: prompts Yes/No (default No); writes `options["demo-sites"] = "true"|"false"`.
- Silent: `DbInstallConfigResolver.parseDemoSitesFlag(options)`.
- Summary line shows “Sample sites: enabled …” when true.

### Flag propagation into ANT (broken gap)

`Main.execJar` builds the ANT child JVM command and sets (actual code at `Main.java` ~575–579):

```java
boolean demoSites =
    DbInstallConfigResolver.parseDemoSitesFlag(
        System.getProperties().entrySet().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    e -> e.getKey().toString(),
                    e -> e.getValue() == null ? null : e.getValue().toString())));
command.add("-Dinstall.demo.sites=" + demoSites);
```

It **does not** pass `phase1.options()` (where interactive/`--demo-sites` stored `"demo-sites"="true"`).  
`parseArgs` only fills the options map; it does **not** `System.setProperty("install.demo.sites", …)`.

Therefore:

|             Operator action              |    Options map    | System property (preinstall JVM) | ANT `-Dinstall.demo.sites` |
|------------------------------------------|-------------------|----------------------------------|----------------------------|
| Interactive **Yes**                      | `demo-sites=true` | usually unset                    | **false** (observed)       |
| CLI `--demo-sites` only                  | `demo-sites=true` | unset unless outer `-D`          | **false**                  |
| Outer `java -Dinstall.demo.sites=true …` | may still be true | true                             | **true**                   |

This matches the observed dual state:

- `cms.demo-sites=true` in last-install (from options after Phase 1)
- `install.demo.sites=false` in ANT echoproperties
- empty `RXSITES`

Secondary note (docs vs comment drift, for #2192): `Main.execJar` comment says upgrades “always ignore” the flag; `installRepository.xml` / wizard / AGENTS.md say new **and** upgrades honor the flag with locale strip protection. Classification still lands on installer wiring, not Explorer.

### Unit coverage gap (context)

Existing tests cover flag **parsing** and wizard prompt/`options` mutation (`InteractiveInstallWizardTest`, `DbInstallConfigResolverTest`, `SampleSiteLocaleStripTest`). They do **not** assert that `Main.execJar` / ANT child receives the Phase-1 options value.

---

## Classification matrix (slice 2 vs 3)

|                        Observation                        |                             Points to                              |
|-----------------------------------------------------------|--------------------------------------------------------------------|
| Seed XML on disk with Corporate/Enterprise `RXSITES` rows | Bundle present (not a packaging “file missing” issue alone)        |
| `cms.demo-sites=true` but ANT `install.demo.sites=false`  | Flag not wired to seed step                                        |
| No seed/skip echo in installRepository window             | `installSampleSites` never antcall’d                               |
| `RXSITES` count = 0                                       | **Data never seeded**                                              |
| Parent UI: empty Sites                                    | Consistent with empty site table (not exclusive of a later UI bug) |

**Conclusion:** Primary failure is **installer seed / flag propagation → #2192**.  
**Do not start #2193** until a corrected demo-sites install shows non-zero `RXSITES` **and** Explorer still empty.  
**#2194** Playwright residual after product fix.

---

## Acceptance checklist (#2191)

- [x] Document interactive Yes + silent `--demo-sites` repro on H2
- [x] Capture install evidence: flag false / seed path not run
- [x] Probe repository for sample sites (empty `RXSITES`; seed names documented)
- [x] Note Explorer empty Sites (parent screenshot + empty repo ⇒ no need for live REST for classification)
- [x] Comment on #1750 with **pursue slice 2 (#2192)**; mark #2191 done; update Agent progress table

---

## Out of scope (honored)

- No changes to installer Java, ANT seed, or Explorer product code in this slice.
- No Playwright implementation (#2194).
- Fix implementation belongs on #2192 (and only then #2193 if still needed).

