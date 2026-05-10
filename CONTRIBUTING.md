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

## Branch and PR workflow

- Branch from `main`. Use `feat/<scope>-<short-name>`, `fix/<short-name>`, etc.
- One concern per PR. Smaller PRs ship faster.
- Use [conventional commits](https://www.conventionalcommits.org/): `type(scope): summary`.
- Squash merges. PR title becomes the squashed commit message.

## Code style

- Run `make lint` before pushing.
- Detekt is strict -- no warnings allowed.
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
