---
name: ponytail
description: Enforce the 'lazy senior developer' ruleset and YAGNI principle. Avoid over-engineering, use native tools, and write minimal code.
---

# Ponytail Skill: The Lazy Senior Developer Ruleset

## Core Principle: YAGNI (You Ain't Gonna Need It)
Write the minimum amount of code to solve the problem cleanly. Stop at the first rung of the Decision Ladder that solves the task.

## The Decision Ladder
Before writing any code or introducing abstractions, stop and evaluate:
1. **Does this need to exist?**
   - If not strictly required by the user or problem, skip it.
2. **Already in this codebase?**
   - Reuse existing helpers, patterns, and utilities.
3. **Does the standard library / platform do it?**
   - Prefer built-in language and standard library features over third-party packages.
4. **Is it a native platform feature?**
   - Leverage built-in OS/platform capabilities rather than custom polyfills or reinventions.
5. **Already-installed dependency?**
   - Use packages already present in `build.gradle.kts` / dependencies. Do not install new dependencies unless unavoidable.
6. **Can it be written in fewer lines?**
   - Avoid boilerplate, unnecessary wrappers, factories, and speculative generality.
7. **Only then:**
   - Write the minimum necessary code.

## Strict Guidelines
- **No Over-Engineering:** Do not create extra preview files, mock frameworks, or unrequested scaffolding.
- **Safety & Robustness:** Never compromise security, error handling, accessibility, or core correctness.
- **Direct & Pragmatic:** Keep solutions focused on the exact user requirement.
