# Quickstart validation: URL allow/block lists

**Branch**: `986-url-allowlist-config` | **Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

## Prerequisites

- JDK 21 via `./mvnw`
- Checkout feature branch `986-url-allowlist-config`
- Module `modules/perc-security-utils` builds

## 1. Unit tests (primary gate)

```bash
./mvnw -pl modules/perc-security-utils -am test \
  -Dtest=URLValidationTest,URLGlobMatcherTest,URLListFileLoaderTest
```

### Expected outcomes

|                 Scenario                  |                      Expected                       |
|-------------------------------------------|-----------------------------------------------------|
| `https://example.com/x`                   | Permit (baseline)                                   |
| `http://10.0.0.5/api` without allow       | Deny                                                |
| Same private URL with allow pattern match | Permit                                              |
| Metadata `http://169.254.169.254/...`     | Deny (hard and/or block list)                       |
| Allow `*` alone                           | Ignored; arbitrary URL still denied if not baseline |
| Block matches even if allow matches       | Deny                                                |
| Seed missing files in temp dir            | Files created with correct default character        |
| Seed when files exist                     | Content unchanged                                   |

## 2. Manual / integration smoke (optional)

1. Point a test CMS install root at a temp directory with `rxconfig/Server/`.
2. Ensure `rxdeploydir` system property (or server start) resolves that root.
3. Omit allow/block files → first validation load seeds both.
4. Add active line to allow file for an internal host; restart process; confirm validation permits that URL.
5. Confirm existing allow file is not replaced on re-run of seed/upgrade copy.

## 3. Consumer regression (smoke)

```bash
./mvnw -pl modules/extensions-main -Dtest=PSProxyQueryResourceTest test
./mvnw -pl system -Dtest=PSDocumentUtilsSsrfTest,PSDtdTreeSsrfTest test
```

Existing SSRF denials must remain green.

## 4. Documentation check

- Release notes / admin help describe: paths, additive allow, block precedence, full-URL globs, create-if-absent, no system properties.

## References

- File contract: [contracts/url-list-files.md](./contracts/url-list-files.md)
- Decision contract: [contracts/url-validation-decision.md](./contracts/url-validation-decision.md)
- Data model: [data-model.md](./data-model.md)

