# Extension Lookup Regression — Root Cause and Fix (March 2026)

**Date:** 2026-03-10
**Affected versions:** All builds since commit `6a1fbb402`
**Fixed in:** `development` branch, `perc-system` module

## Symptom

The following extensions (and others) produced `PSNotFoundException` during
server startup:

```
Java/global/percussion/filter/sys_DefaultPasswordFilter
Java/global/percussion/system/sys_emptyDoc
Java/global/percussion/cx/sys_CreateTranslations
Java/global/percussion/generic/sys_ToLowerCase
Java/global/percussion/generic/sys_MakeAbsLink
```

The server logged errors of the form:

```
ERROR Extension Java/global/percussion/filter/sys_DefaultPasswordFilter not found.
```

despite all extensions being correctly present in:

- `extensions-main-<version>.jar` (this module)
- `Extensions/Handlers/Java/30/Extensions.xml` (the installed registry)

## Root cause

Commit `6a1fbb402` ("Refactor/webui build structure #662") added `m_category`
to `PSExtensionRef.equals()` and `hashCode()`:

```java
// After 6a1fbb402 — category included in equality
return new EqualsBuilder()
    .append(m_handlerName, second.m_handlerName)
    .append(m_context,     second.m_context)
    .append(m_extName,     second.m_extName)
    .append(m_category,    second.m_category)   // <-- added
    .isEquals();
```

At the same time, `PSExtensionHandlerConfiguration` used a
`ConcurrentHashMap<PSExtensionRef, IPSExtensionDef>` as the inner registry
map. Extensions were stored with the `categorystring` value from
`Extensions.xml` (e.g., `"filter"`, `"generic"`, `"cx"`). But all runtime
lookup callers construct `PSExtensionRef` via:

```java
new PSExtensionRef(handlerName, context, name)   // category = ""
```

or parse a full-name string (also produces empty category). Because
`"filter" != ""`, `Map.get(ref)` always returned `null` — causing
`PSNotFoundException` for every extension that had a non-empty
`categorystring`.

Extensions with an empty `categorystring` (about 6 entries) continued to
work, which is why the server partially started.

## Why category belongs in equals

`PSExtensionRef` is the correct place for category equality: two refs pointing
to the same handler/context/name but with different categories are logically
different objects. Removing category from equality would hide the category
mismatch between stored and looked-up refs rather than fixing it.

## Fix applied

Changed the inner map key in `PSExtensionHandlerConfiguration` from
`PSExtensionRef` (object equality, includes category) to `String` (the FQN
via `ref.getFQN()` = `handler/context/name`, no category):

```java
// Before: inner map keyed by PSExtensionRef (broken)
private volatile Map<String, Map<PSExtensionRef, IPSExtensionDef>> m_extensionContexts;

// After: inner map keyed by FQN string (correct)
private volatile Map<String, Map<String, IPSExtensionDef>> m_extensionContexts;
```

All four operations updated to use `ref.getFQN()` as the key:

```java
// Store
extensionDefs.put(ref.getFQN(), extensionDef);

// Lookup
return extensionDefs.get(ref.getFQN());

// Remove
extensionDefs.remove(ref.getFQN());

// Enumerate (refs returned from def.getRef(), not from String keys)
return extensionDefs.values().stream()
    .map(IPSExtensionDef::getRef)
    .collect(Collectors.toList())
    .iterator();
```

`PSExtensionRef.equals()` and `hashCode()` are unchanged — they correctly
include category for full object equality.

## Also fixed: silent exception swallowing

A `catch (PSExtensionException pse)` block in
`PSExtensionHandlerConfiguration.load()` was completely empty (just a
`// TODO` comment). Parse failures during startup were silently discarded,
making the root cause invisible in logs. That catch block now logs:

```
ERROR [PSExtensionHandlerConfiguration] Failed to load extension
      context=global/percussion/filter/ name=sys_DefaultPasswordFilter: <message>
```

## Verification

After the fix, `server.log` shows:

```
INFO  [com.percussion.extension.PSExtensionManager] Initializing extension manager.
INFO  [com.percussion.extension.PSExtensionManager] Initialization successful.
```

with no extension-not-found errors.

## Files changed

|                                         File                                         |                                                        Change                                                        |
|--------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| `system/src/main/java/com/percussion/extension/PSExtensionHandlerConfiguration.java` | Inner map key changed from `PSExtensionRef` to `String` (FQN); `load()` catch block now logs `ERROR`                 |
| `system/src/main/java/com/percussion/extension/PSExtensionRef.java`                  | No net change — category restored to `equals()`/`hashCode()` after an intermediate incorrect workaround was reverted |

