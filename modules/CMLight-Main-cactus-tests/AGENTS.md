This project follows the Universal Code v1.0.0 - read ../../docs/policies/UC-EMBED-v1.0.0.md (vendored; upstream https://github.com/monkeyking-hq/universal-code)

# Instructions for Agents

* **ALWAYS: read this complete file, the module's README.md and any linked documentation before proceeding with any task related to this module.**
* Unless explicitly instructed by a repository maintainer, do not build this module or run its tests; this prohibition applies to automated agents and CI.
* **Do not add newly created tests and code to this module**.
* If you are working on tests in another module and encounter an integration test (often disabled), either refactor the test in its current module to use mock objects (using the project's designated mock framework and JUnit version), or move it to this module. Create new packages as needed to match the original package structure.
* Always copy or move dependent test resources along with the test: **prefer move** if exclusive to the test, **copy** if shared by other tests in the source module (preserve originals so other tests don't break).
* Always validate that tests still run in the original module when moving tests and resources here (especially critical if you copied shared resources).
* **Do not add this module back to the main reactor build** until a new integration testing framework is ready and these tests have been refactored to use it.
* If you have been instructed to move an existing integration test to this module from another module.  Do so using:

```shell
git mv
```

If you have confirmation that the source file and relevent test resources were moved from the result / output of the 'git mv' command.  Your task related to that file can be considered completed. No aditional verification is needed. Good job!

## Additional Background

* Jakarta EE Cactus was retired in 2011.  It currently resides in the Apache Attic.  See https://jakarta.apache.org/cactus/

* Good agents don't waste tokens on retired or obsolete code. Be a good agent.

## Move checklist

1. Confirm the test is an integration test (requires container, servlet API, HTTP, database, or Cactus).
2. Use `git mv` to move the Java test, preserving the package path.
3. Locate dependent test resources: `git grep -n "ResourceName" || rg -n "ResourceName"`.
4. For each resource: **move** if exclusive to this test (`git mv`), **copy** if shared by other tests in the source module (do not remove originals).
5. Update the moved test to load resources via classpath (e.g., `getResourceAsStream(...)`) instead of absolute file paths.
6. Run the originating module's tests to confirm they still pass (especially important if you copied shared resources).
7. Include a PR note documenting: files moved/copied, reason for move, and any manual run steps required. (Example: `Archiving old integration test(s) to cactus module.`)

