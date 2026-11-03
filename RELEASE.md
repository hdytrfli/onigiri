# Releasing

The app version lives in [`gradle/libs.versions.toml`](gradle/libs.versions.toml):

```toml
app-name = "1.0.0-beta.2"
app-code = "1000002"
```

`app-name` is the human-readable version. `app-code` must increase with every
published APK, including betas.

There is no local release tooling dependency. Version bumps, commits, tags, and
pushes are explicit manual actions.

## Beta / staging release

Edit both values in `gradle/libs.versions.toml`:

```toml
app-name = "1.0.0-beta.2"
app-code = "1000002"
```

Then commit and tag:

```bash
git add gradle/libs.versions.toml
git commit -m "chore: release v1.0.0-beta.2"
git tag v1.0.0-beta.2
```

Review, then push when ready:

```bash
git push origin HEAD --follow-tags
```

A **beta** tag builds a staging APK and creates a GitHub prerelease. It is not
marked as the latest release. In CI it uses the stable release key; local builds
fall back to the debug key if the release key is unavailable.

## Stable release

Edit both values in `gradle/libs.versions.toml`:

```toml
app-name = "1.0.0"
app-code = "1000100"
```

Then commit and tag:

```bash
git add gradle/libs.versions.toml
git commit -m "chore: release v1.0.0"
git tag v1.0.0
git push origin HEAD --follow-tags
```

A stable tag builds a signed release APK and creates a normal GitHub Release.

## Changelog

GitHub release notes are generated in CI by `git-cliff` from Conventional
Commits. Nothing is committed to the repository; the notes live on the GitHub
Release.

Use commit messages such as:

```text
feat: add ...
fix: correct ...
perf: improve ...
refactor: ...
```

`cliff.toml` controls the grouping and formatting.

## Version rules

```text
1.0.0        stable release
1.0.0-beta.2 staging / GitHub prerelease
```

The tag must exactly match `v` plus `app-name`; the release workflow rejects
mismatches. `app-code` is not part of the tag, but it must always increase.

## Required GitHub secrets

The local keystore can live at `keystore/onigiri-release.jks`; the whole
`keystore/` directory is gitignored.

Stable releases require:

```text
RELEASE_KEYSTORE_BASE64
RELEASE_KEYSTORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

Staging builds use the same stable key when the secrets are available, so beta
users can install updates. Local staging builds without release credentials fall
back to the debug keystore.

## CI behavior

- GitHub Actions runs only for version tags.
- No build or format workflow runs on every push or pull request.
- `v*-beta*` tags run the Staging workflow.
- Other `v*` tags run the Release workflow.
- Formatting is checked as part of the build workflow.
