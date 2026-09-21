# Erlang review — #4637 Explorer create folder

Independent pass on `feat/issue-4637-explorer-create-folder` vs `main`.

## Verdict

**PASS** (no hard-gate bugs found). Behavioral tests cover REST 400/403/404/409, adaptor name validation (NIO `Path.of` + separator check), SPA client validation, and Playwright surface helper/spec for `POST /rest/folders/create`. Paths for CMS folders remain `/`-separated finder/JCR strings; filesystem joins use `Path.of` / `concatPath`, not OS-concatenated literals.

## Checks

- New `IFolderAdaptor.createFolder` implemented in `FolderAdaptor` and `FolderTestAdaptor`.
- Invalid names (`/`, `\`, `.`, `..`, absolute `Path`) are HTTP 400 before `addFolder`.
- Duplicate name and non-folder parent are HTTP 409.
- Missing parent is `FolderNotFoundException` (404).
- Non-admin is `NotAuthorizedException` (403), matching sibling folder mutations.

> Co-Authored by Grok Build 1.0.40 using grok-4.6 with agent night-issue-prs.
