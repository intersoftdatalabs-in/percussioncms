---
name: cosign-validate
description: Validates an AI resource using SHA-256 integrity and Sigstore authenticity sidecars.
version: 1.0

---
# Cosign Validate Skill

**Purpose**: Verify the status of an AI resource.

## Skill Execution

```bash
#!/bin/bash
REPO_ROOT=$(git rev-parse --show-toplevel)
"$REPO_ROOT/mvn-env.sh" -pl modules/ai-shared-develop exec:java \
    -Dexec.mainClass="com.percussion.ai.signing.ResourceVerifier" \
    -Dexec.args="$1"
```
