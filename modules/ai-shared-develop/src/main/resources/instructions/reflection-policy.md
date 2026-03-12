# Reflection Policy

**Purpose:** Short, practical rules to avoid unsafe or brittle reflection workarounds in the codebase.

## Summary

- **Avoid using reflection as a workaround** for overload disambiguation, type mismatches, or to access private fields/methods. Such uses are brittle, bypass static checks, and create maintenance and security risks.
- Acceptable uses (require justification):
  - Class loading for pluggable components or providers (use documented constructors / factory methods where possible).
  - Invocation of framework-required methods when no alternative exists (provide clear comments & tests).

## Rules

1. Do not use reflection to call another method solely to avoid a compile-time change. Add a typed helper method or overload instead. Example: add `deleteDependents(PSLocator, PSDependentSet)` rather than using Method.invoke.
2. Do not use `setAccessible(true)` or reflectively access private fields unless there is a documented, reviewed reason and an associated issue/PR describing the justification.
3. Replace deprecated `Class.newInstance()` with `getDeclaredConstructor().newInstance()` when dynamic instantiation is necessary.
4. When reflection is used, add a clear comment explaining why, add a unit test that demonstrates the expected behavior, and reference a GitHub issue that approves the exception.

## Enforcement

- Code reviewers must flag any reflection usage that looks like a workaround and require either:
  - an explicit helper method / overload, or
  - a documented and reviewed justification with an associated issue/PR.

## Examples

**Bad:**

```java
// avoids changing API and uses reflection to resolve ambiguity
Method m = processor.getClass().getMethod("delete", PSLocator.class, PSDependentSet.class);
m.invoke(processor, owner, dependents);
```

**Good:**

```java
// explicit, typed helper added to the processor
processor.deleteDependents(owner, dependents);
```

