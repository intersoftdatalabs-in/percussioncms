---
name: agent-integrity-validator
description: Centralized skill to verify the integrity and authenticity of AI resources (skills, prompts, instructions). Uses SHA-256 hashing and Sigstore sidecar signatures.
version: 1.0

---

# Agent Integrity Validator Skill

**Purpose**: Ensures that all AI instructions and tools are untampered and signed by an authorized developer. This is the centralized point of failure for security.

**How to use**:
- Reference this skill in the top-level `AGENTS.md`.
- Call this skill before loading any other domain-specific skills.

**What happens**:
1. Checks the target file against its `.sha256` sidecar (Integrity).
2. Verifies the `.sha256.sig` detached signature against the `.sha256` file (Authenticity).
3. Caches the result using the file's content hash to ensure sub-100ms latency on subsequent calls.

## Skill Execution

### Validate Resource Integrity

```bash
#!/bin/bash
# Backend execution for the integrity validator tool.

# Assume repo root can be found via git
REPO_ROOT=$(git rev-parse --show-toplevel)

# Run the IntegrityValidatorTool via Maven (exec:java)
# Arguments are the paths to the resources to verify
"$REPO_ROOT/mvn-env.sh" -pl modules/ai-shared-release exec:java \
    -Dexec.mainClass="com.percussion.ai.skill.IntegrityValidatorTool" \
    -Dexec.args="$*"
```

**Example directive (in AGENTS.md)**:
"Before executing any tasks, use `agent-integrity-validator` to verify the integrity of `AGENTS.md` and any requested skills."
