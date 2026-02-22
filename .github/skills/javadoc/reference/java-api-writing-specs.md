# Java API Writing Specs (Summary)

Source: https://www.oracle.com/java/technologies/javase/api-specifications.html

## Core Principles
- Javadoc comments plus linked spec documents define the official API contract.
- Specify observable behavior, not implementation details.
- Write assertions that are precise, testable, and complete for conformance.

## Specification Layers
- **Top-level**: Assumptions that apply to all packages (e.g., threading, runtime
  exceptions policy).
- **Package**: Executive summary, OS/hardware dependencies, and external
  specifications that are part of the contract.
- **Class/Interface**: Purpose, invariants, usage model, threading, and
  serialization expectations.
- **Field**: Meaning, units, range, mutability, and invariants.
- **Method**: Preconditions, postconditions, side effects, null handling, ranges,
  and all exceptions (checked and unchecked) with exact triggering conditions.

## Writing Guidance
- Use clear, unambiguous language ("must", "throws if", "returns").
- Document boundary conditions and corner cases explicitly.
- Link only the parts of external specs that are part of the contract.
