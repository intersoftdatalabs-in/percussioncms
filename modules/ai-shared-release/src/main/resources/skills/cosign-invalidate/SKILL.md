---
name: cosign-invalidate
description: Manually invalidates a signature by renaming sidecar files.
version: 1.0

---

# Cosign Invalidate Skill

**Purpose**: Force immediate verification failure for a resource.

## Skill Execution

```bash
#!/bin/bash
if [ -f "$1.sha256" ]; then
    mv "$1.sha256" "$1.sha256.invalid"
    echo "Invalidated integrity sidecar for $1"
fi
if [ -f "$1.sha256.sig" ]; then
    mv "$1.sha256.sig" "$1.sha256.sig.invalid"
    echo "Invalidated authenticity sidecar for $1"
fi
```

