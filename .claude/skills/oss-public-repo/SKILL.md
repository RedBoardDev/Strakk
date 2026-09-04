---
name: oss-public-repo
description: "Templates and conventions for GitHub community files: LICENSE, SECURITY.md, CONTRIBUTING, CODE_OF_CONDUCT, issue/PR templates, dependabot."
---

# Public OSS Repo — Community Files

Used by `oss-publisher` (mode: oss).

## File checklist + canonical locations

| File | Location | Required | Source |
|------|----------|----------|--------|
| LICENSE | `LICENSE` (root) | Yes | SPDX text, user-chosen |
| README | `README.md` (root) | Yes | `docs-curator` writes |
| SECURITY | `SECURITY.md` or `.github/SECURITY.md` | Yes | template below |
| CONTRIBUTING | `CONTRIBUTING.md` | Yes | template below |
| Code of Conduct | `CODE_OF_CONDUCT.md` | Recommended | Contributor Covenant 2.1 verbatim |
| Issue templates | `.github/ISSUE_TEMPLATE/{bug_report,feature_request,config}.yml` | Recommended | templates below |
| PR template | `.github/PULL_REQUEST_TEMPLATE.md` | Recommended | template below |
| Dependabot | `.github/dependabot.yml` | Recommended | template below |
| Funding | `.github/FUNDING.yml` | Optional | ask user |
| Discussions | enable in repo settings | Optional | ask user |

## SECURITY.md template

```markdown
# Security Policy

## Supported versions

We support the latest minor release of the latest major version. Older versions
receive security fixes only at the maintainers' discretion.

| Version | Supported |
|---------|-----------|
| latest  | ✅        |
| < latest| ❌        |

## Reporting a vulnerability

**Please do not open a public issue for security vulnerabilities.**

Report privately via either:

1. **GitHub Private Vulnerability Reporting** — preferred. Open the repo's
   Security tab → "Report a vulnerability".
2. **Email** — security@<your-domain> (PGP key available on request).

Include:
- A description of the vulnerability
- Steps to reproduce
- The affected versions
- Any known mitigations

## Response time

- We will acknowledge within **3 business days**.
- A patch or workaround will be provided within **30 days** for high/critical
  severity, sooner if actively exploited.

## Disclosure policy

Coordinated disclosure. We will:
- Confirm the issue and develop a fix.
- Credit the reporter (or maintain confidentiality if requested).
- Publish a GitHub Security Advisory after the fix is released.
```

## CONTRIBUTING.md template

```markdown
# Contributing

Thank you for considering a contribution.

## Code of Conduct

This project adopts the [Contributor Covenant](CODE_OF_CONDUCT.md). By
participating you agree to uphold this code.

## Development setup

See the [README](README.md) for the full setup. In short:

1. Clone and configure `local.properties` (see `docs/ENVIRONMENTS.md`).
2. `./gradlew :shared:build :androidApp:assembleDebug`
3. `cd iosApp && xcodegen generate && open Strakk.xcodeproj`

## Branch & PR workflow

- Branch from `main`. Use `feat/<scope>-<short-name>`, `fix/<short-name>`, etc.
- One concern per PR. Smaller PRs ship faster.
- Use [conventional commits](https://www.conventionalcommits.org/): `type(scope): summary`.
- Squash merges. PR title becomes the squashed commit message.

## Code style

- Run `make lint` before pushing.
- Detekt is strict — no warnings.
- SwiftLint runs locally (we have no macOS CI runner).

## Tests

- New shared logic requires a unit test in `shared/src/commonTest/`.
- `./gradlew :shared:allTests` must pass.

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). Key rule: domain has zero
dependencies; data is `internal`; presentation calls UseCases only.

## Filing issues

Use the templates under `.github/ISSUE_TEMPLATE/`.

## License

By contributing you agree your contribution is licensed under the same terms as
the project (see [LICENSE](LICENSE)).
```

## .github/ISSUE_TEMPLATE/config.yml

```yaml
blank_issues_enabled: false
contact_links:
  - name: Question / discussion
    url: https://github.com/RedBoardDev/Strakk/discussions
    about: Use Discussions for questions and ideas.
  - name: Security report
    url: https://github.com/RedBoardDev/Strakk/security/advisories/new
    about: Report security issues privately.
```

## .github/ISSUE_TEMPLATE/bug_report.yml

```yaml
name: Bug report
description: Report a reproducible bug.
labels: [bug]
body:
  - type: markdown
    attributes:
      value: |
        Thanks for taking the time to file a bug.
  - type: textarea
    id: description
    attributes:
      label: Description
      description: What happened?
    validations:
      required: true
  - type: textarea
    id: repro
    attributes:
      label: Steps to reproduce
      placeholder: |
        1.
        2.
        3.
    validations:
      required: true
  - type: textarea
    id: expected
    attributes:
      label: Expected behavior
    validations:
      required: true
  - type: input
    id: platform
    attributes:
      label: Platform
      placeholder: "iOS 17.4 / Android 14 / both"
    validations:
      required: true
  - type: input
    id: version
    attributes:
      label: App version / commit
    validations:
      required: true
  - type: textarea
    id: logs
    attributes:
      label: Logs / screenshots
      render: shell
```

## .github/ISSUE_TEMPLATE/feature_request.yml

```yaml
name: Feature request
description: Suggest a new feature or improvement.
labels: [enhancement]
body:
  - type: textarea
    id: problem
    attributes:
      label: Problem
      description: What user need is unmet today?
    validations:
      required: true
  - type: textarea
    id: proposal
    attributes:
      label: Proposal
    validations:
      required: true
  - type: textarea
    id: alternatives
    attributes:
      label: Alternatives considered
```

## .github/PULL_REQUEST_TEMPLATE.md

```markdown
## Summary
<!-- 1–3 bullet points -->

## Related issues
<!-- Closes #N / Refs #N -->

## Test plan
- [ ] `make lint` passes
- [ ] `./gradlew :shared:allTests` passes
- [ ] Manually tested on iOS
- [ ] Manually tested on Android

## Breaking changes
<!-- API change? Migration? Yes/No + details -->

## Screenshots
<!-- For UI changes -->
```

## .github/dependabot.yml

```yaml
version: 2
updates:
  - package-ecosystem: gradle
    directory: /
    schedule:
      interval: weekly
    open-pull-requests-limit: 5
    groups:
      kotlin:
        patterns: ["org.jetbrains.kotlin*", "org.jetbrains.kotlinx*"]
      compose:
        patterns: ["androidx.compose*", "org.jetbrains.compose*"]
      ktor:
        patterns: ["io.ktor*"]

  - package-ecosystem: github-actions
    directory: /
    schedule:
      interval: weekly

  - package-ecosystem: docker
    directory: /infra/nutrition-api
    schedule:
      interval: weekly

  - package-ecosystem: swift
    directory: /iosApp
    schedule:
      interval: weekly
```

## License selection guide

| Choice | Use when |
|--------|----------|
| **MIT** | Simplest. Permissive. Good default for personal projects. |
| **Apache-2.0** | Like MIT but with explicit patent grant. Good for projects expecting commercial users. |
| **GPL-3.0** | Copyleft. Derivative works must be GPL too. Use if you want all forks to remain open. |
| **AGPL-3.0** | GPL + extends to network use (SaaS forks must publish source). |
| **MPL-2.0** | File-level copyleft. Middle ground. |

**Default for Strakk: MIT** (mobile app, personal project, broad reach).

## GitHub repo settings (manual user actions)

After files land:
- [ ] Repo description (1 line, ≤350 chars)
- [ ] Repo topics: `kotlin-multiplatform`, `compose-multiplatform`, `swiftui`, `supabase`, `nutrition`, `kmp`
- [ ] Homepage URL (privacy policy or landing page)
- [ ] Enable "Issues" and "Discussions"
- [ ] Enable Private Vulnerability Reporting
- [ ] Enable Dependabot alerts and security updates
- [ ] Enable secret scanning + push protection
- [ ] Branch protection on `main` and `release` (require PR review + status checks)
