---
name: cosign-sign
description: Signs an AI resource by generating a SHA-256 hash and a detached Sigstore signature.
version: 1.0

---
# Cosign Sign Skill

**Purpose**: Authenticate an AI resource.

## Skill Execution

```bash
#!/bin/bash
REPO_ROOT=$(git rev-parse --show-toplevel)
"$REPO_ROOT/mvn-env.sh" -pl modules/ai-shared-develop exec:java \
    -Dexec.mainClass="com.percussion.ai.signing.ResourceSigner" \
    -Dexec.args="$1"
```
