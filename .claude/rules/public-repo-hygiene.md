# Rule — Public repo hygiene

This repo is **public on GitHub**. Anything committed is visible, indexed, mirrored, and forked. Treat every commit as a permanent publication.

## Nothing internal-only goes in the public repo

The following do not belong on a public branch:
- Internal product strategy or marketing notes (`MARKETING.md`, `IDEA.md`, etc.)
- Customer or user names, vendor pricing, financials
- Audit reports with sensitive specifics (security review findings before they're patched)
- Roadmaps with unreleased feature names
- Personal notes / scratch files

If you need such files locally, keep them in `.private/` (gitignored) or in a separate private repo.

## What public-friendly content looks like

- Documentation framed for an outside contributor, not the team
- Issue-tracker style "what" and "why", not "what we agreed in last week's meeting"
- Generic placeholders for environment-specific values (`example.com`, not real hostnames)
- No screenshots that include PII or production data

## Pre-commit mental checklist

Before staging any new doc or note for commit, ask:
1. Would I be comfortable if this was on the front page of HackerNews tomorrow?
2. Does this reveal customer identities, vendor pricing, or unreleased feature names?
3. Does this make sense to someone who has never met the team?

If any answer is no, keep it out of the public repo.

## Force-push policy (for the new repo, after the initial push)

Once the fresh public repo is live:
- Branch protection on `main` should disallow force-push.
- Branch protection on `release` should disallow force-push.
- Require linear history (squash or rebase merges only).

The initial push to the new repo replaces no existing history (the repo starts empty), so force-push concerns don't apply at creation time.

## Discoverability is a feature, not a bug

A clean public repo with proper README, LICENSE, SECURITY.md, and CONTRIBUTING attracts contributors. A messy public repo with internal docs and TODO scribbles attracts ridicule. The rule above protects both reputation and security.
