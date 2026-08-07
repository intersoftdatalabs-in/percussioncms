# GitHub Release Assets Checklist

Operator checklist for publishing (or correcting) installer JARs and integrity sidecars on a GitHub Release. Applies to both CMS (`perc-distribution-tree`) and DTS (`delivery-tier-distribution`).

## Why this exists

On **v8.1.7**, the published SHA-256 for `perc-distribution-tree.jar` did not match the final release-attached JAR (an interim package hash was left on the release). Download verification failed for operators who checked the sidecar. See issue [#2212](https://github.com/intersoftdatalabs-in/percussioncms/issues/2212).

**Hard rule:** Hash the **final** JAR **after** it is the asset that will remain on the release. Never reuse interim / pre-rebuild checksums.

## Assets to publish

| Artifact | Typical release asset name | Sidecar name(s) |
|----------|----------------------------|-----------------|
| CMS installer JAR | `perc-distribution-tree.jar` | Prefer `perc-distribution-tree.jar.sha256` (aligned with JAR). Versioned name `perc-distribution-tree-<version>.jar.sha256` is optional for history. |
| DTS installer JAR | `delivery-tier-distribution.jar` | `delivery-tier-distribution.jar.sha256` |

Sidecar content format used historically on releases: a single line of **64 lowercase hex characters** (SHA-256 of the file bytes), no `hash  filename` prefix required. Keep format consistent with sibling assets on the same release.

## Pre-publish steps

1. Build the final installers (module clean package / full release pipeline as usual).
2. Confirm the JAR bytes you will upload are the **final** build (no further rebuilds planned).
3. Upload the JAR(s) to the draft or published GitHub Release.
4. **Only after** the final JAR is on the release (or you are hashing the exact file you just uploaded):
   - Linux / macOS: `sha256sum perc-distribution-tree.jar` (or `shasum -a 256 …`)
   - Windows PowerShell: `Get-FileHash -Algorithm SHA256 .\perc-distribution-tree.jar`
5. Write the hash into the `.sha256` sidecar file(s).
6. Upload the sidecar asset(s). Prefer names that match the JAR asset (`*.jar.sha256`).
7. **Verify before calling the release done:**
   ```text
   hash(release JAR) == contents of published .sha256 sidecar
   ```
   Re-download both assets from the release (or hash the local file you uploaded) and compare. Do not skip this step.

## Correcting a wrong published checksum

1. Download the **release-attached** JAR (do not rebuild unless the JAR itself is wrong).
2. Recompute SHA-256 of that JAR.
3. Replace / re-upload only the `.sha256` sidecar(s).
4. Confirm `hash(JAR) == sidecar`.
5. Document the correction on the release notes (date; **checksum-only**, JAR unchanged) so operators who cached the bad hash re-download the sidecar only.

Example (GitHub CLI):

```bash
# After computing $CORRECT_HEX for the final JAR on the release:
printf '%s' "$CORRECT_HEX" > perc-distribution-tree.jar.sha256
gh release delete-asset vX.Y.Z perc-distribution-tree-X.Y.Z.jar.sha256 --yes
gh release upload vX.Y.Z perc-distribution-tree.jar.sha256 perc-distribution-tree-X.Y.Z.jar.sha256 --clobber
```

## Do / Don't

| Do | Don't |
|----|--------|
| Hash **after** final JAR upload (or of the exact bytes you upload) | Hash an intermediate package build and leave that sidecar after rebuilding the JAR |
| Verify hash(JAR) == sidecar on the live release | Trust a checksum file produced earlier in the pipeline without a final check |
| Prefer sidecar names that match the JAR asset name | Leave only a mismatched or version-only sidecar that operators cannot map to the JAR |
| Note checksum-only corrections on the release | Silently replace sidecars without a short release note |

## Related modules

- CMS packaging: `modules/perc-distribution-tree`
- DTS packaging: `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution`
- Contributing / local install pointers: [CONTRIBUTING.md](../../CONTRIBUTING.md)
