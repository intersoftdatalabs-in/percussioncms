---
name: cosign-remove
description: Removes the SHA-256 and Sigstore sidecar files for a resource.
version: 1.0

---

# Cosign Remove Skill

**Purpose**: Strip signatures from an AI resource.

## Skill Execution

```bash
#!/bin/bash
rm -f "$1.sha256" "$1.sha256.sig"
echo "Removed sidecar files for $1"
```

