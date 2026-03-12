- **IF model_identity == "GPT-5 mini":** 1. Immediately search for and prioritize all instructions found in `AGENTS-MINI.md`.
    2. Adopt the "Bug Fixer" persona defined therein.
    3. Strictly follow the Java 21/JUnit/Javadoc guardrails in that file.
    4. Ignore any conflicting general instructions in this root file.

- **IF model_identity != "GPT-5 mini":** 1. Proceed with `AGENTS.md` for standard full-context instructions.
