# JCR Node.getUUID Call Site Inventory

Per **T036** of Spec 987, this document inventories `getUUID()` call sites across `system/services`, `modules/utils`, and `projects/sitemanage`.

## Findings Summary
- Over 450 `getUUID()` occurrences exist across the codebase.
- **Type Analysis**: The vast majority of `.getUUID()` calls target Percussion GUID abstractions (e.g. `IPSGuid.getUUID()`, `PSGuid`, `PSDesignGuid`, `PSLegacyGuid`), which return `int` or `long` integer identifier values. These are **not** JCR API calls.
- **JCR Node `getUUID()`**: The product's primary JCR `Node` implementation (`PSContentNode`) already implements `getIdentifier()` (returning the item GUID string), which replaces deprecated JCR 1.0 `getUUID()`.
- **Finder & Query Layer**: `PSJcrNodeFinder` issues JCR SQL queries and works directly with `IPSNode` representations and `Value` objects; it does not call deprecated `Node.getUUID()`.
- **Result**: Zero critical editor or publisher paths are using deprecated `javax.jcr.Node.getUUID()`.
