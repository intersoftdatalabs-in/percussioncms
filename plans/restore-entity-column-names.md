# Plan: Restore Incorrect @Column Names on JPA Entity Classes

**Issue:** A previous GitHub Copilot "Java 11 refactoring" (commit `7f7eef023`) incorrectly renamed `@Column(name = "...")` values in several JPA entity classes on the `development` branch. These column names must match the physical database schema exactly. The schema DDL (in `cmsTableDef.xml`) was **NOT changed** — only the Java entity annotations were broken.

**Source of truth:** The `development-8.1.x` branch has the correct `@Column` names. The DDL in `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/data/cmsTableDef.xml` also confirms the correct names.

**Date:** 2026-03-12
**Target Branch:** `development` (JDK 21)
**Reference Branch:** `origin/development-8.1.x` (READ ONLY — do NOT modify)

---

## CRITICAL GUARDRAILS — READ BEFORE DOING ANYTHING

1. **NEVER modify any file on the `development-8.1.x` branch.** That branch is the source of truth. Only modify files on the `development` branch.
2. **NEVER change the database schema files** (`cmsTableDef.xml`, `cmsTableData.xml`, etc.). Schema is correct and must not be touched.
3. **ONLY change the `name = "..."` value inside `@Column(...)` annotations.** Do NOT change:
   - Java field names
   - Java field types
   - Other annotation parameters (e.g., `nullable`, `unique`, `length`)
   - `@Table` annotations (except where explicitly specified in this plan)
   - `@JoinColumn` annotations (except where explicitly specified in this plan)
   - Any import statements
   - Any method bodies or logic
   - Any file that is NOT listed in this plan
4. **Do NOT reformat code** outside the specific annotation being changed. No whitespace changes, no import reordering, no line-wrap changes.
5. **Do NOT add or remove fields.** Only fix existing `@Column(name = "...")` values.
6. **Do NOT add comments** like `// FIXED` or `// RESTORED` to the code.
7. **After each file is modified, compile the `system` module** to verify no errors:

   ```bash
   ./mvnw -pl system -am compile -DskipTests -q
   ```

   If compilation fails, **STOP and investigate** — do NOT proceed to the next file.

8. **All column names are UPPERCASE.** The naming strategy (`UpperCaseNamingStrategy`) converts identifiers to uppercase. The `@Column(name = "...")` values must match the DDL column names exactly — they USE underscores where the DDL uses them.

---

## HOW TO VERIFY A COLUMN NAME IS CORRECT

For any column name fix in this plan, you can verify it against the DDL:

```bash
# Search for the column name in the schema definition
grep -i 'COLUMN_NAME_HERE' modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/data/cmsTableDef.xml
```

You can also verify against the `development-8.1.x` branch:

```bash
# View the correct version of a file from development-8.1.x
git show origin/development-8.1.x:path/to/File.java | grep '@Column'
```

**If a column name in this plan does NOT appear in `cmsTableDef.xml`, STOP and report the discrepancy.** Do NOT blindly apply the change.

---

## AFFECTED FILES AND EXACT CHANGES

There are **3 entity files** that need changes, plus **1 entity file** that has a structural issue requiring careful attention.

---

### FILE 1: PSSite.java (25 column name fixes)

**ALREADY FIXED IN WORKING TREE** — This file was already corrected in a previous session. The fixes are in the working tree but may not be committed yet. **Verify the current state before making any changes.** If the corrections are already applied, skip this file.

**Path:** `system/services/src/com/percussion/services/sitemgr/data/PSSite.java`
**Table:** `RXSITES`

**Verification command:**

```bash
grep '@Column' system/services/src/com/percussion/services/sitemgr/data/PSSite.java
```

Compare the output against this list of **CORRECT** column names. If ANY of the "Wrong" values appear, fix them:

| #  |       Java Field Name        |  Wrong Value (development)   | Correct Value (8.1.x / DDL) |
|----|------------------------------|------------------------------|-----------------------------|
| 1  | `description`                | `DESCRIPTION`                | `SITEDESC`                  |
| 2  | `previousName`               | `PREVIOUSNAME`               | `PREVSITENAME`              |
| 3  | `privateKey`                 | `PRIVATEKEY`                 | `PRIVATE_KEY`               |
| 4  | `navTheme`                   | `NAVTHEME`                   | `NAV_THEME`                 |
| 5  | `folderRoot`                 | `FOLDERROOT`                 | `FOLDER_ROOT`               |
| 6  | `is_secure`                  | `ISSECURE`                   | `IS_SECURE`                 |
| 7  | `defaultPubServer`           | `DEFAULTPUBSERVER`           | `DEFAULT_PUBSERVERID`       |
| 8  | `defaultFileExtention`       | `DEFAULTFILEEXTENTION`       | `DEFAULT_FILE_EXT`          |
| 9  | `is_canonical`               | `ISCANONICAL`                | `IS_CANONICAL`              |
| 10 | `siteProtocol`               | `SITEPROTOCOL`               | `SITE_PROTOCOL`             |
| 11 | `defaultDocument`            | `DEFAULTDOCUMENT`            | `DEFAULT_DOCUMENT`          |
| 12 | `canonicalDist`              | `CANONICALDIST`              | `CANONICAL_DIST`            |
| 13 | `is_canonical_replace`       | `ISCANONICALREPLACE`         | `IS_CANONICAL_REPLACE`      |
| 14 | `siteAdditionalHeadContent`  | `SITE_ADDITIONAL_HEAD`       | `ADDL_HEAD_CONTENT`         |
| 15 | `siteBeforeBodyCloseContent` | `SITE_BEFORE_BODY_CLOSE`     | `BEFORE_BODY_CLOSE`         |
| 16 | `siteAfterBodyOpenContent`   | `SITE_AFTER_BODY_OPEN`       | `AFTER_BODY_START`          |
| 17 | `loginPage`                  | `LOGINPAGE`                  | `LOGIN_PAGE`                |
| 18 | `registrationPage`           | `REGISTRATIONPAGE`           | `REGISTRATION_PAGE`         |
| 19 | `generateSiteMap`            | `GENERATESITEMAP`            | `GENERATE_SITEMAP`          |
| 20 | `generateSiteMapOptions`     | `GENERATESITEMAPOPTIONS`     | `GENERATE_SITEMAP_OPTIONS`  |
| 21 | `mobilePreviewEnabled`       | `MOBILEPREVIEWENABLED`       | `ENABLE_MOBILE_PREVIEW`     |
| 22 | `overrideSystemJQuery`       | `OVERRIDE_SYSTEM_JQUERY`     | `OVERRIDE_JQUERY`           |
| 23 | `overrideSystemFoundation`   | `OVERRIDE_SYSTEM_FOUNDATION` | `OVERRIDE_FOUNDATION`       |
| 24 | `overrideSystemJQueryUI`     | `OVERRIDE_SYSTEM_JQUERYUI`   | `OVERRIDE_JQUERYUI`         |
| 25 | `unpublishFlags`             | `UNPUBLISHFLAGS`             | `UNPUBLISH_FLAGS`           |

**Example fix (one annotation):**

```java
// BEFORE (wrong):
@Column(name = "CANONICALDIST")

// AFTER (correct):
@Column(name = "CANONICAL_DIST")
```

**IMPORTANT:** The field `allowedNamespaces` with `@Column(name = "ALLOWED_NAMESPACES")` is a **legitimate new addition** on the `development` branch. It exists in the DDL on both branches. Do NOT remove it.

**DDL verification for RXSITES table:**

```bash
grep -A3 'column name="CANONICAL_DIST"\|column name="SITEDESC"\|column name="PREVSITENAME"\|column name="PRIVATE_KEY"' \
  modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/data/cmsTableDef.xml
```

---

### FILE 2: PSLocationScheme.java (5 column name fixes + 1 @Table fix + 1 @JoinColumn fix)

**Path:** `system/services/src/com/percussion/services/sitemgr/data/PSLocationScheme.java`
**DDL Table:** `RXLOCATIONSCHEME`
**Current (wrong) @Table:** `PSX_LOCATIONSCHEME`
**Correct @Table:** `RXLOCATIONSCHEME`

This file was heavily rewritten during the Java 11 refactoring. It has 3 types of issues:

#### 2a. Fix the @Table name

Find this line:

```java
@Table(name = "PSX_LOCATIONSCHEME")
```

Change it to:

```java
@Table(name = "RXLOCATIONSCHEME")
```

**DDL verification:**

```bash
grep 'table name="RXLOCATIONSCHEME"' modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/data/cmsTableDef.xml
```

#### 2b. Fix @Column names (5 changes)

| # | Java Field Name | Wrong Value (development) | Correct Value (8.1.x / DDL) |
|---|-----------------|---------------------------|-----------------------------|
| 1 | `schemeId`      | `LOCATIONSCHEME_ID`       | `SCHEMEID`                  |
| 2 | `name`          | `NAME`                    | `SCHEMENAME`                |
| 3 | `templateId`    | `TEMPLATE_ID`             | `VARIANTID`                 |
| 4 | `contentTypeId` | `CONTENT_TYPE_ID`         | `CONTENTTYPEID`             |
| 5 | `contextId`     | `CONTEXT_ID`              | `CONTEXTID`                 |

**Example fix:**

```java
// BEFORE (wrong):
@Id
@Column(name = "LOCATIONSCHEME_ID")
private long schemeId = -1;

// AFTER (correct):
@Id
@Column(name = "SCHEMEID")
private long schemeId = -1;
```

**DDL verification for RXLOCATIONSCHEME columns:**

```bash
grep -A2 'column name="SCHEMEID"\|column name="SCHEMENAME"\|column name="VARIANTID"\|column name="CONTENTTYPEID"\|column name="CONTEXTID"' \
  modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/data/cmsTableDef.xml
```

#### 2c. Fix the @JoinColumn for the parameters collection

The `@JoinColumn` on the `parameters` field references the primary key column of the RXLOCATIONSCHEME table, which is now wrong:

Find this:

```java
@JoinColumn(name = "LOCATIONSCHEME_ID")
```

Change it to:

```java
@JoinColumn(name = "SCHEMEID", nullable = false)
```

**Why:** The child table `RXLOCATIONSCHEMEPARAMS` has a foreign key column named `SCHEMEID` that references `RXLOCATIONSCHEME.SCHEMEID`. The DDL confirms this:

```bash
grep -A5 'RXLOCATIONSCHEMEPARAMS' modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/data/cmsTableDef.xml | grep SCHEMEID
```

**NOTE:** The `PSLocationSchemeParameter.java` file has a `@JoinColumn(name = "SCHEMEID", ...)` which is already correct and does NOT need to be changed.

---

### FILE 3: PSAssemblyTemplate.java (1 removed field + 1 type change)

**Path:** `system/services/src/com/percussion/services/assembly/data/PSAssemblyTemplate.java`
**Table:** `PSX_TEMPLATE`

This file has two structural issues from the Java 11 refactoring:

#### 3a. Restore the `globalTemplate` field (GLOBAL_TEMPLATE column)

The Java 11 refactoring **removed** the persisted `globalTemplate` field and replaced it with a `@Transient` `globalTemplateGuid` field. The DDL column `GLOBAL_TEMPLATE` (BIGINT) still exists in `PSX_TEMPLATE`.

**Current (wrong) state on development:**

```java
// Backing field for the global template GUID. Transient for now to keep
// changes minimal and behavior-preserving; persisted mapping can be added
// later if required.
@jakarta.persistence.Transient
private com.percussion.utils.guid.IPSGuid globalTemplateGuid;
```

**Correct state (from development-8.1.x):**

```java
@Basic
@Column(name = "GLOBAL_TEMPLATE")
private Long globalTemplate;
```

**Action:** Replace the `@Transient globalTemplateGuid` field with the original `@Basic @Column(name = "GLOBAL_TEMPLATE") private Long globalTemplate;` field.

**IMPORTANT:** After restoring this field, you MUST also check that the getter and setter methods (`getGlobalTemplate()`, `setGlobalTemplate()`) work correctly with the `Long globalTemplate` field instead of the `IPSGuid globalTemplateGuid` field. Compare the method implementations against `development-8.1.x`:

```bash
git show origin/development-8.1.x:system/services/src/com/percussion/services/assembly/data/PSAssemblyTemplate.java | grep -A10 'getGlobalTemplate\|setGlobalTemplate'
```

The original getter/setter pattern on 8.1.x is:

```java
public IPSGuid getGlobalTemplate() {
    if (globalTemplate != null)
        return new PSGuid(PSTypeEnum.TEMPLATE, globalTemplate);
    else
        return null;
}

public void setGlobalTemplate(IPSGuid guid) {
    if (guid != null)
        globalTemplate = guid.longValue();
    else
        globalTemplate = null;
}
```

Compare this carefully against the current dev branch implementations. If they use `globalTemplateGuid` instead of `globalTemplate`, update them to use the restored `Long globalTemplate` field.

#### 3b. Fix the `globalTemplateUsage` field type

**Current (wrong) state on development:**

```java
@Basic
@Column(name = "GLOBAL_TEMPLATE_USAGE")
private String globalTemplateUsage;
```

**Correct state (from development-8.1.x):**

```java
@Basic
@Column(name = "GLOBAL_TEMPLATE_USAGE")
private Integer globalTemplateUsage = GlobalTemplateUsage.None.ordinal();
```

The DDL defines `GLOBAL_TEMPLATE_USAGE` as `INTEGER`. The field type must be `Integer`, not `String`.

**Action:** Change the field type from `String` to `Integer` and restore the initializer. Then check all methods that read/write this field and update them to work with `Integer`:

```bash
git show origin/development-8.1.x:system/services/src/com/percussion/services/assembly/data/PSAssemblyTemplate.java | grep -B2 -A10 'globalTemplateUsage'
```

**WARNING:** This file requires extra care because the refactoring changed method implementations too. After making the field changes:
1. Compare EVERY method that references `globalTemplateUsage` or `globalTemplate` against the 8.1.x version
2. Restore the original method logic where it was changed
3. Update imports if needed (e.g., `jakarta.persistence.Basic` should already be imported)

**DDL verification:**

```bash
grep -A3 'column name="GLOBAL_TEMPLATE_USAGE"\|column name="GLOBAL_TEMPLATE"' \
  modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/data/cmsTableDef.xml
```

---

### FILE 4: PSTemplateBinding.java (NO CHANGES NEEDED)

**Path:** `system/services/src/com/percussion/services/assembly/data/PSTemplateBinding.java`

The `EXECUTION_ORDER` column was already commented out (`//@Column`) and marked `@Transient` on the `development-8.1.x` branch. The `development` branch removed the comments but kept `@Transient`. This is functionally identical. **No action required.**

---

## EXECUTION ORDER

Execute in this exact order:

### Step 1: Verify PSSite.java (already fixed)

```bash
# Check if fixes are already applied
grep -n '@Column' system/services/src/com/percussion/services/sitemgr/data/PSSite.java | grep -E 'CANONICALDIST|DEFAULTDOCUMENT|DEFAULTFILEEXTENTION|DEFAULTPUBSERVER|DESCRIPTION"|FOLDERROOT|GENERATESITEMAP"|GENERATESITEMAPOPTIONS|ISCANONICAL"|ISCANONICALREPLACE|ISSECURE|LOGINPAGE|MOBILEPREVIEWENABLED|NAVTHEME|OVERRIDE_SYSTEM_|PREVIOUSNAME|PRIVATEKEY|REGISTRATIONPAGE|SITEPROTOCOL|SITE_ADDITIONAL_HEAD|SITE_AFTER_BODY_OPEN|SITE_BEFORE_BODY_CLOSE|UNPUBLISHFLAGS'
```

If the above command produces NO output, PSSite is already fixed. If it produces output, apply the 25 fixes from the table in FILE 1.

### Step 2: Fix PSLocationScheme.java

Apply changes 2a, 2b, and 2c from FILE 2 above.

### Step 3: Compile and verify after PSLocationScheme fix

```bash
./mvnw -pl system -am compile -DskipTests -q
```

**If compilation fails, STOP.** Check error messages, fix, and retry compilation before proceeding.

### Step 4: Fix PSAssemblyTemplate.java

Apply changes 3a and 3b from FILE 3 above. This requires comparing method implementations against 8.1.x.

### Step 5: Compile and verify after PSAssemblyTemplate fix

```bash
./mvnw -pl system -am compile -DskipTests -q
```

**If compilation fails, STOP.** This file is the most complex — any method change errors must be resolved.

### Step 6: Run unit tests

```bash
./mvnw -pl system test -DfailIfNoTests=false -q 2>&1 | tail -20
```

Check for any test failures related to the changed entities.

### Step 7: Final verification — diff against 8.1.x @Column names

Run this command to confirm no remaining @Column discrepancies:

```bash
# For each fixed file, verify all @Column names match 8.1.x
for file in \
  system/services/src/com/percussion/services/sitemgr/data/PSSite.java \
  system/services/src/com/percussion/services/sitemgr/data/PSLocationScheme.java \
  system/services/src/com/percussion/services/assembly/data/PSAssemblyTemplate.java; do
  echo "=== $file ==="
  diff <(grep '@Column' "$file" | sed 's/.*name = "//;s/".*//' | sort) \
       <(git show origin/development-8.1.x:"$file" | grep '@Column' | sed 's/.*name = "//;s/".*//' | sort)
done
```

The diff should show:
- **PSSite.java:** Only `ALLOWED_NAMESPACES` as extra (legitimate new field)
- **PSLocationScheme.java:** Empty diff (all columns match)
- **PSAssemblyTemplate.java:** Empty diff (all columns match)

---

## ADDITIONAL ISSUES FOUND (Comprehensive Audit)

A comprehensive schema audit of all 115 @Entity files against the DDL revealed **65 total mismatches**. In addition to the 3 files in the main plan above, the following were identified:

### Additional File 5: PSRelationshipData.java (Core System)

**Path:** `system/services/src/com/percussion/services/relationship/data/PSRelationshipData.java`

**Issue:** Multiple @Column annotations declare columns with correct names, but the entity may be mapped to the wrong table during audit validation.

**Status:** Requires investigation. Cross-reference against the actual correct @Table mapping.

### Additional File 6: PSRelationshipPropertyData.java

**Path:** `system/src/main/java/com/percussion/design/objectstore/PSRelationshipPropertyData.java`

**Issue:** 3 @Column annotations may not match the mapped table.

**Status:** Requires investigation. Verify @Table name is correct.

### Delivery Tier Suite (18 Entities)

These entities use separate database tables (PERC_* namespace) not defined in core cmsTableDef.xml. This is by design:

- PSComment, PSCommentTag, PSDefaultModerationState (comments module)
- PSLikes (likes module)
- PSConnectionInfo, PSFeedDescriptor (feeds module)
- PSFormData (forms module)
- PSGenericKey, PSMembership (membership module)
- PSDbBlogPostVisit, PSDbCookieConsent, PSDbMetadataEntry, PSDbMetadataProperty (metadata module)
- PSPoll, PSPollAnswer (polls module)
- PSCategoryEntity (sitemanage/category)
- PSWorkflowCommunity (workflow)

**Status:** These are correct — separate schema from core, no action needed.

### Other Files (104 Entities)

All remaining 104 entity files have correctly matching @Column names between development and development-8.1.x branches. Other changes (javax→jakarta imports, field renames, whitespace) are non-critical.

---

## SUMMARY OF ALL CHANGES

|          File           |       Change Type        |   Count    |        Risk        |
|-------------------------|--------------------------|------------|--------------------|
| PSSite.java             | @Column name fixes       | 25         | Low (already done) |
| PSLocationScheme.java   | @Table name fix          | 1          | Medium             |
| PSLocationScheme.java   | @Column name fixes       | 5          | Medium             |
| PSLocationScheme.java   | @JoinColumn name fix     | 1          | Medium             |
| PSAssemblyTemplate.java | Restore field + fix type | 2          | **HIGH**           |
| PSAssemblyTemplate.java | Method logic restoration | ~4 methods | **HIGH**           |
| **TOTAL**               |                          | **34+**    |                    |

---

## ROLLBACK PROCEDURE

If anything goes wrong, you can restore any file to its committed state:

```bash
# Restore a single file from HEAD (last commit)
git checkout HEAD -- path/to/File.java

# Or restore from development-8.1.x to see the "correct" version
git show origin/development-8.1.x:path/to/File.java > /tmp/correct_version.java
# Then manually compare and apply ONLY the @Column name changes
```

---

## WHAT NOT TO DO — COMMON MISTAKES

1. **Do NOT rename Java fields to match column names.** The field `description` maps to column `SITEDESC` — that is correct. Only the `@Column(name=...)` value should be `SITEDESC`.
2. **Do NOT change `@Column` annotations that aren't in the tables above.** Many columns ARE correct (e.g., `SITENAME`, `BASEURL`, `ROOT`, `PORT`).
3. **Do NOT change the `UpperCaseNamingStrategy` configuration.** It's correct.
4. **Do NOT apply changes to `deliverytiersuite/` entities** — they are correct already.
5. **Do NOT change schema files (cmsTableDef.xml, cmsTableData.xml).** Ever.
6. **Do NOT switch branches.** Stay on `development`.
7. **Do NOT touch any `@Id`, `@Version`, `@Basic`, `@Entity`, or `@OneToMany` annotations** unless explicitly instructed in this plan.
8. **Do NOT change the `DESCRIPTION` column on PSLocationScheme.java** — it is correct on both branches.
9. **Do NOT change the `GENERATOR` column on PSLocationScheme.java** — it is correct on both branches.
10. **Do NOT change the `VERSION` column on PSLocationScheme.java** — it is correct on both branches.

