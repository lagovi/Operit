# Building an Android app on GitHub runners

This guide is for an agent or a developer who has to build an Android project
without a local Android SDK. The build happens on GitHub Actions, the APK comes
back as a workflow artifact, and the only local tools are `adb` for the phone
and `python3` for scripting. It is written so that it can be applied to another
repository with small edits; the examples use a generic project and note where
the operit fork does something specific.

## What the environment must provide

- A GitHub repository with Actions enabled for the branch you push to.
- A Gradle wrapper (`./gradlew`) and a JDK version the project declares.
- A phone reachable over `adb` if you intend to install and check the result.

Locally you need almost nothing: `adb` (platform-tools), `gh` logged in, and a
JDK only if you want to read a stack trace with `retrace`. Do not try to
install the Android SDK locally just to compile: it downloads gigabytes, needs
licences accepted, and duplicates what the runner already does.

## Workflow skeleton

The smallest workflow that produces an installable APK. Save it as
`.github/workflows/android-build.yml`.

```yaml
name: Android Build

on:
  workflow_dispatch:
    inputs:
      gradle_task:
        description: Gradle task to run
        required: true
        default: assembleDebug
        type: choice
        options:
          - assembleDebug
          - bundleRelease

concurrency:
  group: android-build-${{ github.event_name }}-${{ github.ref }}
  cancel-in-progress: true

permissions:
  contents: read

jobs:
  build:
    runs-on: ubuntu-24.04
    timeout-minutes: 120
    steps:
      - uses: actions/checkout@v5
        with:
          fetch-depth: 1
          submodules: false
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: '21'
          cache: gradle
      - uses: android-actions/setup-android@v4
        with:
          packages: platform-tools
      - name: Install SDK packages
        run: |
          sdkmanager --install "platforms;android-36" "build-tools;35.0.0"
      - name: Configure local.properties
        run: echo "sdk.dir=$ANDROID_HOME" > local.properties
      - name: Build
        run: |
          chmod +x ./gradlew
          ./gradlew "${{ inputs.gradle_task }}" --stacktrace --no-daemon
      - uses: actions/upload-artifact@v4
        if: success()
        with:
          name: app-${{ github.run_number }}
          path: app/build/outputs/apk/**/*.apk
          if-no-files-found: error
          retention-days: 14
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: reports-${{ github.run_number }}
          path: |
            app/build/reports/**
            build/reports/**
          if-no-files-found: warn
          retention-days: 14
```

Three details that cost time when forgotten:

- `actions/setup-android` installs `tools platform-tools` by default, but the
  `tools` package was removed from the SDK repository and the step fails before
  the job starts. Pin `packages: platform-tools` and install the rest by hand
  through `sdkmanager`.
- `local.properties` with `sdk.dir` is not committed; write it in the workflow.
- `--no-daemon` avoids a daemon that outlives the step and hides the exit code.

A large project adds native builds, Node, Rust or Python steps before Gradle.
Keep the ordering: system tools, caches, code generation, Gradle, artifacts.

## Triggers and cost

Prefer `workflow_dispatch` alone. A push trigger with `paths:` filters is fine
for a busy repository, but an agent does not need a build on every push and
will burn the monthly minutes. Dispatch with an input so one workflow serves
debug, release and instrumentation builds:

```
gh workflow run android-build.yml --ref <branch> -f gradle_task=assembleDebug
```

`concurrency` with `cancel-in-progress: true` stops a stale run of the same
branch when a newer commit arrives, which is what you want while iterating.

## Caches that matter

- `actions/setup-java` with `cache: gradle` restores `~/.gradle` when the
  dependencies do not change. It is the single largest win.
- `actions/cache` on downloaded prebuilt native archives, generated assets or
  a Rust/meson build directory, keyed by `hashFiles()` of the scripts and build
  files that produce them.
- Do not cache `app/build/` wholesale: stale outputs produce confusing
  incremental failures and the upload may pick up an APK from a previous run.

The first run after a cache-key change is slow, later ones are minutes faster.
Note the wall clock of a green run and use it as the baseline for judging a
regression.

## Signing

Debug builds are signed with a key generated per runner, so the signature
changes on every run. Consequences to design around:

- `adb install` over an existing copy fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
  Always `adb uninstall <package>` before installing a CI APK.
- The uninstall wipes app data. Anything the app downloads on first launch has
  to be downloaded again; automate that or accept the manual step.
- A debug APK cannot upgrade a release APK and vice versa.

For release builds, keep the keystore in a repository secret as base64 and
write it to a file inside the job:

```
echo "$KEYSTORE_BASE64" | base64 -d > keystore.jks
```

Pass `storeFile`, `storePassword`, `keyAlias` and `keyPassword` through
`ORG_GRADLE_PROJECT_*` environment variables or `~/.gradle/gradle.properties`
generated in the job, never on the command line where they land in the log.

If the app is minified, publish the R8 `mapping.txt` as a separate artifact.
Without it a production stack trace is unreadable, and retracing after the
artifact expires is impossible.

## Watching a run

```
gh run list --workflow android-build.yml --limit 5
gh run watch <run-id> --exit-status
gh run view <run-id> --log-failed
```

`--log-failed` prints only the failed step, which is usually enough. For a
compile error, search the full log for the Kotlin or Java file name:

```
gh run view <run-id> --log | grep -n -E "error:|e: file:///"
```

A build in a sandbox takes 10-40 minutes depending on native code and caches.
Poll the status instead of blocking on a fixed sleep, and do useful work
meanwhile.

## Getting the APK back

Artifacts are not part of the repository and expire. Download them with `gh`:

```
gh api repos/<owner>/<repo>/actions/runs/<run-id>/artifacts \
  --jq '.artifacts[] | select(.name|test("^app-[0-9]+$")) | .id'
gh api repos/<owner>/<repo>/actions/artifacts/<artifact-id>/zip > app.zip
unzip -tq app.zip
unzip -o app.zip -d apk
```

Two habits save time:

- Filter by name. A workflow usually uploads several artifacts, and pulling the
  reports archive instead of the APK is a common mistake.
- Retry the download and check the zip with `unzip -tq`. Artifact downloads
  occasionally return a truncated body, and a broken zip looks like a Gradle
  problem later.

Keep `retention-days` short for reports and moderate for APKs. A 100 MB APK
times dozens of runs fills a small disk quickly, and a full disk breaks both the
build tools and your local workspace.

## Installing and checking on a device

```
adb connect <ip>:5555
adb devices
adb uninstall <package>
adb install -r apk/app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p <package> -c android.intent.category.LAUNCHER 1
adb exec-out screencap -p > screen.png
```

For UI automation, `adb shell uiautomator dump /dev/tty` prints the view tree
with bounds; parse it to find buttons by text instead of hard-coding pixels.
Dumps and screenshots sometimes return empty over Wi-Fi; retry a few times and
reconnect with `adb connect` on a timeout.

A CI APK is the only build that reflects the real compiler. If the local
machine cannot run Gradle, treat "is it green on CI" as the first question and
"does it work on the phone" as the second, never the reverse.

## Failure catalogue

| symptom | cause | fix |
| --- | --- | --- |
| `Failed to find package tools` | setup-android default package removed from the SDK repo | pin `packages: platform-tools`, install the rest via `sdkmanager` |
| `Unsupported class file major version` | JDK mismatch between Gradle and the project | align `java-version` with the toolchain the project declares |
| `Could not resolve ...` first run only | cache miss or a private repository without credentials | add a token secret, check the dependency URL |
| Java heap space | default Gradle daemon memory too small | `org.gradle.jvmargs=-Xmx4g` in `gradle.properties` |
| Kotlin errors that never appear locally | the local build never ran, or a different JDK | fix from the CI log, do not guess locally |
| `HTTP 422: Actions has been disabled` | a fork inherited schedules and GitHub switched workflows off | re-enable Actions in the repository web UI, do not debug permissions |
| artifact download returns a broken zip | truncated transfer | retry, verify with `unzip -tq` |
| test APK cannot talk to the app APK | two separate runs signed with different debug keys | build both APKs in one run (`assembleDebug assembleDebugAndroidTest`) |

## Checklist before you call a build done

- The run is green and the APK artifact exists.
- The APK is downloaded, `unzip -tq` passes, and the file size looks plausible.
- The APK is installed on the phone after an uninstall and the app launches.
- The behaviour that motivated the change is visible on the device, with a
  screenshot kept outside the repository.
- The workflow file changes are committed with the code they build, because the
  next agent needs the same trigger, task and artifact names.
