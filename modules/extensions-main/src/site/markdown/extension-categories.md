# Extension Categories

Each entry in `Java/Extensions.xml` carries a `categorystring` attribute that
groups extensions by functional area. The category is informational only — it
does **not** affect runtime lookup. The FQN (`handler/context/name`) is the
authoritative identity.

## Java extension categories

| Category | Count | Description |
|---|---|---|
| `generic` | 72 | General-purpose UDFs: string manipulation, date formatting, link building, parameter handling |
| `contentassembler` | 25 | Assembly location generators and content assembler exits |
| `jexl` | 20 | JEXL-expression extensions called from Velocity templates |
| `translation` | 18 | Field-value translators: date formatting, form encode/decode, value mapping |
| `psxsystem` | 15 | Internal system exits |
| `xmldom` | 14 | XML DOM manipulation: transform, clean up, move, copy, and convert DOM nodes |
| `relationship` | 13 | Relationship effects and translation constraints |
| `editiontask` | 11 | Edition task exits that fire on pre/post-publish events |
| `contenteditor` | 11 | Content-editor field validators and input transformers |
| `cx` | 9 | Content-explorer (CX) exits |
| `assembly` | 8 | Assembler exits and slot content finders |
| `encoding` | 7 | Input-sanitization transformers: HTML, JS, CSS, XML, URI encoding |
| `contentlist` | 7 | Content-list generators and template expanders |
| `workflow` | 6 | Workflow action exits |
| `validation` | 6 | Field-value validators |
| `uicontext` | 6 | UI context menus |
| `security` | 6 | Input-validation security extensions (allow only boolean, integer, GUID, etc.) |
| `search` | 6 | Search results processors |
| `itemfilter` | 6 | Item filter rule exits for visibility and publish eligibility |
| `cms` | 6 | Additional CMS exits (inline links, compare revision, site definitions) |
| `SlotContentFinder` | 5 | Slot content-finder exits |
| `scheduledTask` | 5 | Scheduled task exits |
| `i18n` | 5 | Internationalization date and text exits |
| `publisher` | 4 | Publishing exits |
| `communities` | 4 | Community management exits (authenticate user, ACL flags) |
| `clone` | 3 | Content-clone exits |
| `components` | 2 | Component management exits |
| `filter` | 1 | Password filter (`sys_DefaultPasswordFilter`) |
| `usersearch` | 1 | Server user search cataloger |
| `rule` | 1 | Field editability rule |
| `report` | 1 | Publish-time statistics report exit |
| `filetracker` | 1 | File info and file-size exits |
| `fastforward` | 1 | FastForward site extensions |
| `exit` | 1 | Miscellaneous exit |
| `ca` | 1 | Content archive exit |

## JavaScript extension categories

The `Javascript/Extensions.xml` registry contains 11 extensions that run as
server-side JavaScript within the Rhythmyx script engine. These are listed in
the registry with their own context paths and do not use the Java classloader.

## Context paths by interface

When registering an extension the `context` attribute in `Extensions.xml`
determines where the extension appears in the registry hierarchy. The
established conventions are:

| Interface | Conventional context |
|---|---|
| `IPSUdfProcessor` | `global/percussion/generic/` |
| `IPSResultDocumentProcessor` | `global/percussion/exit/` |
| `IPSRequestPreProcessor` | `global/percussion/exit/` |
| `IPSPasswordFilter` | `global/percussion/filter/` |
| `IPSFieldInputTransformer` | `global/percussion/content/` |
| `IPSWorkflowAction` | `global/percussion/workflow/` |
| `IPSEffect` | `global/percussion/relationship/` |
| `IPSEditionTask` | `global/percussion/task/` |
| `IPSAssembler` | `global/percussion/assembly/` |
| `IPSItemFilterRule` | `global/percussion/filter/` |
| `IPSSlotContentFinder` | `global/percussion/SlotContentFinder/` |
