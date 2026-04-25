---
name: pr-summary
description: Drafts a concise PR summary from the current branch diff and recent commits.
argument-hint: "[optional base branch]"
context: fork
agent: build-verify
disable-model-invocation: true
allowed-tools:
  - Bash(git status *)
  - Bash(git diff *)
  - Bash(git log *)
---
# PR Summary

Draft a PR title and body from the current branch.

Use `$ARGUMENTS` as the base branch if provided. If no base is provided, infer the likely default branch from git.

## Required Output

```markdown
## Summary
- ...

## Test Plan
- ...

## Risks / Review Focus
- ...
```

Keep it value-first. Do not dump a file-by-file changelog unless it clarifies risk.
