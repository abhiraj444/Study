# How to build the StudyFlow APK for free using GitHub Actions

You don't need a laptop. All you need is:
- A web browser (on your tablet, phone, or any PC)
- A free GitHub account
- About 15 minutes

GitHub gives every account **2,000 free build minutes per month** — this project uses ~5–10 minutes per build, so you can build it 200+ times monthly for free.

---

## Step 1 — Create a free GitHub account (skip if you already have one)

Go to https://github.com/signup and create an account.

---

## Step 2 — Create a new GitHub repository for the project

1. Tap the **+** icon at the top-right → **New repository**.
2. **Repository name:** `studyflow`
3. Set visibility to **Private** (only you can see it).
4. **Do NOT** check "Add a README" or ".gitignore" — we want an empty repo.
5. Tap **Create repository**.

GitHub will show you a page with commands for "…or push an existing repository from the command line". Leave this tab open — you'll need the URL in Step 4.

---

## Step 3 — Upload the project files to your new repo

The easiest way on a tablet is **GitHub's web upload feature**:

1. Unzip `StudyFlow.zip` somewhere you can find it (Files app → Downloads → tap the zip → "Extract").
2. Go back to your empty GitHub repo page.
3. Tap **Add file → Upload files**.
4. **Drag the entire contents of the StudyFlow folder** into the upload area (or tap "choose your files" and multi-select). Make sure to upload:
   - `.github/` (hidden folder — enable showing hidden files in your file manager)
   - `app/`
   - `gradle/`
   - `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradlew`, `.gitignore`
   - `README.md`
5. Scroll down, type a commit message like "Initial commit", tap **Commit changes**.

> **Important:** The `.github` folder is hidden by default on most file managers. On Android's Files app, tap the three dots → Settings → enable "Show hidden files". If you skip this folder, the workflow won't run.

---

## Step 4 — Trigger the build

The workflow runs automatically every time you push to `main`. Since you just uploaded files, it should already be running!

1. Tap the **Actions** tab at the top of your repo.
2. You'll see a workflow run named **"Build APK"** with a yellow dot (in progress) or green check (done).
3. Tap on it to see the live build log. First build takes ~10–15 min as GitHub downloads the Android SDK and all Gradle dependencies.

---

## Step 5 — Download the APK

1. Once the build is green, scroll to the **Artifacts** section at the bottom of the run page.
2. Tap **studyflow-debug-apk** to download. You'll get a file called `studyflow-debug-apk.zip`.
3. Unzip it — inside is `app-debug.apk`. That's your app!

---

## Step 6 — Install on your tablet

1. Open **Files** (or any file manager).
2. Find `app-debug.apk` in your Downloads folder and tap it.
3. Android will warn "For your security, your phone is not allowed to install unknown apps from this source." Tap **Settings** on that prompt → enable **Allow from this source** → tap back → tap **Install**.
4. Once installed, open **StudyFlow** from your app drawer.

---

## Step 7 — Test the voice integration

You have three options (also covered in the main README):

### Option A — Test locally with ADB-style intents (no Google account needed)

Open any terminal app (Termux on your tablet, or use `adb` over USB from a borrowed PC):

```bash
# Start a study session
adb shell am start -n com.studyflow/.ui.SessionActivity \
    --es subject "Math" --es chapter "Trigonometry" --es mode "theory"

# Stop the session
adb shell am start -n com.studyflow/.ui.SessionActivity --es action "STOP"
```

### Option B — Use Android Studio's App Actions Test Tool

(skip — requires Android Studio on a computer)

### Option C — Real "Hey Google" voice activation

1. Go to https://play.google.com/console with your Google account.
2. Create a new app, set package name to `com.studyflow`, upload your APK to the **Internal Testing** track.
3. Under **Setup → Advanced Settings → App Actions testers**, add your own Google account email.
4. Within ~30 minutes, you can say **"Hey Google, start studying Math"** on your tablet and it will work.

---

## Troubleshooting

### "Build failed" — what to check

1. Open the failed run → tap the **build** job → scroll to the failing step.
2. The most common cause is a missing file or typo. Compare your repo file tree against the `StudyFlow/` folder in the zip.
3. If you see "Could not find gradle-wrapper.jar" — make sure you uploaded `gradle/wrapper/gradle-wrapper.properties`. The workflow auto-generates the `gradle-wrapper.jar` on the runner.
4. The **build-reports** artifact (also downloadable from the run page) has detailed error info.

### Re-running the build

Open the Actions tab → tap any run → top-right **Re-run all jobs**.

### Building after you edit a file

Edit any file on GitHub (tap the pencil icon on a file) → commit. The workflow runs automatically. Download the new APK from the new run.

### Want to change the app icon or name?

- **App name:** edit `app/src/main/res/values/strings.xml` → change `<string name="app_name">StudyFlow</string>` to whatever you want.
- **App icon:** replace the launcher icon drawables in `app/src/main/res/drawable/` and the `mipmap-*/ic_launcher*.xml` files. Commit, and the new APK is built automatically.

### The build is slow (10+ minutes) — can I speed it up?

After the first build, GitHub caches the Gradle dependencies (about 700 MB), so subsequent builds are ~3–5 min. You can't make it faster than that on the free tier.

---

## Want a release (signed) APK instead?

For installing on your own devices, a debug APK is fine. But if you want to publish to Play Store, you need a signed release build. Tell me and I'll add a second workflow that produces a signed AAB.

---

## Need to talk to me about changes?

Just describe what you want changed in chat — I'll edit the files and you re-upload. For big changes I'll generate a new zip.
