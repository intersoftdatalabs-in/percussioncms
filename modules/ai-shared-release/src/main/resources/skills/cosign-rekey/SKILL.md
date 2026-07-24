---
name: cosign-rekey
description: Rotates keys or re-signs all AI resources in the repository.
version: 1.0

---

# Cosign Rekey Skill

**Purpose**: Update all signatures for the repository's AI resources.

## Skill Execution

```bash
#!/bin/bash
REPO_ROOT=$(git rev-parse --show-toplevel)
# This simply runs the signing script which re-signs everything
"$REPO_ROOT/modules/ai-shared-develop/scripts/sign-ai-resources.py"
```

