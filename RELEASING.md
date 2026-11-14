# Releasing memory

The current release is **1.0.0**, with Android `versionCode` **6**.
The application ID is `dev.collinsthomas.memhogs.light`, matching the local
memory preview. The app label is `memory`; the Kotlin namespace is unchanged.

## GitHub Actions

- **Build** (`.github/workflows/ci.yml`) runs on pushes to `main`, pull requests,
  and manual dispatch. It checks formatting, lint and unit tests, then uploads
  a debug APK and check reports to the workflow's **Artifacts** section.
  Debug artifacts use the runner's own development key; use Release APKs when
  updating the installed memory app.
- **Release** (`.github/workflows/release.yml`) runs when you push a `vX.Y.Z`
  tag. You can also use **Run workflow** with an existing version tag. It
  checks the tag against `versionName`, requires the versionCode changelog,
  runs the checks, builds an optimized signed APK, verifies its signature,
  existing signing identity and version, then publishes a GitHub Release
  containing `memory-X.Y.Z.apk` and `SHA256SUMS`.

The workflows use Java 21, Android platform `android-37.0`, and build tools
`37.0.0`. They use the current repository context, so renaming the GitHub
repository does not require editing workflow URLs.

## Retain the current signing key

Every installable APK needs a signature. The signature identifies the
publisher; it does not bind the app to a particular phone. Future updates
must keep the same application ID and signing key, and increase versionCode.

This release deliberately reuses the exact key from the currently installed
memory preview, so **1.0.0 can update it without uninstalling or clearing data**.
It is an optimized, non-debuggable release build signed with that existing key.
Do not generate a replacement keystore, and do not use a CI runner's automatically
generated debug key for releases. The public certificate fingerprint is recorded
in `signing-certificate.sha256`; Release refuses to publish if it does not match.

The verified key and credentials are backed up locally in
`$env:USERPROFILE\.android\memory-release`, outside the repository:

- `memory-release.jks`: a copy of the existing private signing key
- `credentials.json`: its signing credentials
- `MEMORY_SIGNING.json`: the single value ready to paste into GitHub Secrets

Keep a private backup of this whole directory. Never upload these files to
Git or a Release. The public `signing-certificate.sha256` contains no private key
and should be committed.

You can recreate the combined JSON secret from the private backup with:

```powershell
.\scripts\export-signing-secrets.ps1
```

The script exports the existing key and credentials into one JSON file. It
removes the four obsolete TXT exports and never generates or replaces a key.

In your GitHub repository, open **Settings → Secrets and variables → Actions →
New repository secret** and add just one entry:

- **Name:** `MEMORY_SIGNING`
- **Secret:** the entire contents of the private `MEMORY_SIGNING.json` file

The JSON contains the existing keystore (Base64), store password, key alias
and key password. Paste the file contents without editing them. This JSON is
private signing material: keep it outside Git and Release assets.

The old four `MEMORY_KEYSTORE_BASE64`, `MEMORY_KEYSTORE_PASSWORD`,
`MEMORY_KEY_ALIAS` and `MEMORY_KEY_PASSWORD` repository secrets are no longer
read by the workflow and can be deleted from GitHub after this change is pushed.

No personal access token is required. Release uses the workflow's automatic
`GITHUB_TOKEN` with `contents: write`; ordinary builds have read access only.
The combined secret is validated and its individual values are masked before
building. The key is decoded into a private temporary directory during signing,
then removed even if the build fails. Passwords are passed directly to the Gradle
child process as environment variables and are never written into a repository
properties file or included in uploaded artifacts.

## Publish version 1.0.0

After configuring `MEMORY_SIGNING`, commit and push your changes to your own
repository. Review the changes first; signing files stay outside the repo.
Run the following yourself from the repository directory:

```powershell
git status
git add .
git commit -m "Release memory 1.0.0"
git push origin main
git tag v1.0.0
git push origin v1.0.0
```

Watch **Actions → Release**. Once it succeeds, the repository's **Releases**
page contains the APK and checksum file. If the repository is a fork and
Actions is disabled, enable workflows on the Actions page before pushing.
If a tag was already pushed before enabling Actions, manually run Release
with that existing tag. The workflow will not create or move tags, and will
not overwrite an existing published release.

## Later versions

1. Increase `versionCode` by one and update `versionName` in
   `app/build.gradle.kts`.
2. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
   Its contents become the GitHub Release notes.
3. Run `./gradlew ktlintFormat`, then
   `./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug`.
4. Commit and push, then create and push the matching `vX.Y.Z` tag yourself.
   Keep the original signing key and `MEMORY_SIGNING` secret.

## Local release builds

GitHub needs only the `MEMORY_SIGNING` secret. The following four variables
are internal Gradle inputs for local signed builds, not extra GitHub Secrets:

- `MEMORY_KEYSTORE_PATH`: absolute path to the `.jks` file
- `MEMORY_KEYSTORE_PASSWORD`
- `MEMORY_KEY_ALIAS`
- `MEMORY_KEY_PASSWORD`

Then run `./gradlew assembleRelease`. Alternatively, an ignored
`keystore.properties` file supports `storeFile`, `storePassword`, `keyAlias`
and `keyPassword`; environment variables take precedence. Incomplete signing
configuration fails with a configuration error. Without any signing settings,
local release builds remain unsigned and cannot be installed. Debug builds use
the normal development key and need no secrets.

The `fdroid/` directory contains the original upstream submission template;
it is not an active publishing configuration for this fork.

References: [Android app signing](https://developer.android.com/studio/publish/app-signing),
[GitHub Actions secrets](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets),
[GitHub CLI release creation](https://cli.github.com/manual/gh_release_create).
