# ChannelOnly build status

The repository currently contains only the initial README and Gradle settings file. The complete Android source project is not yet present on the `main` branch, so an APK build cannot succeed until the project files are uploaded.

The complete source bundle was prepared separately as `ChannelOnly_Android_Project.zip`. Extract it and upload the contents of the top-level `ChannelOnly/` directory into the repository root (so that `app/`, `build.gradle.kts`, `settings.gradle.kts`, and `.github/workflows/` are at the repository root).

After the upload, open **Actions → Android build and emulator smoke test** and run the workflow. The workflow should run unit tests, assemble a debug APK, install and launch it in an Android emulator, and upload the APK artifact if all steps pass.

Do not commit `local.properties`, Firebase secrets, OAuth tokens, or passwords. Firebase configuration can be supplied locally after downloading the project. A successful CI build is not the same as completing the Firebase and YouTube OAuth setup required for end-to-end account testing.
