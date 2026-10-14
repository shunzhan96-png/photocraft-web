# PhotoCraft Android MVP

This is a companion Android shell for the already-deployed official PhotoCraft Web release. It does not modify or fork the upstream PhotoCraft project.

## MVP scope

- An Editor tab loads the existing GitHub Pages web app.
- Android's document picker handles PhotoCraft's file-open action.
- Blob-based exports are bridged to Android's Downloads/PhotoCraft folder; ordinary download URLs use DownloadManager.
- An AI Director tab lets the user choose a reference photo, describe the goal, and share both with an installed AI app such as ChatGPT.
- The AI must propose directions first; the user confirms a direction before following short editing steps.
- A Chrome fallback is included because WebView's GPU capabilities can differ from the phone's full browser.

## Build

GitHub Actions builds a debug APK for testing. Open the repository's Actions tab, choose **Build PhotoCraft Android APK**, run the workflow, and download the \`photocraft-android-debug-apk\` artifact. The APK is unsigned for production distribution and is intended for personal testing.

The app requires Android 10 (API 29) or newer. It requires internet access to load the hosted PhotoCraft web app and to share a photo to an external AI app.

## Important limitations

This is not yet an AI agent that directly controls PhotoCraft's canvas. The current official web build has no public in-page command interface. The upstream project documents CLI/MCP/control-protocol automation for its native desktop app, but that control channel is loopback-only and is not exposed by the browser build. Do not expose its bearer token over the network.

The current AI flow uses Android's share sheet, so the user chooses the AI app and confirms sending the photo. After AI returns a plan, the user switches back to PhotoCraft and edits. Directly applying AI-approved operations would require a separately designed, permission-limited command bridge and verification of which editing commands the web build can safely expose. That is a later phase, not something this wrapper should fake with unreliable coordinate taps.

## Privacy and safety

- The app does not include an AI API key or backend.
- The app does not upload working files to this repository or to a PhotoCraft server.
- If the user shares a photo to ChatGPT or another AI app, that app receives the photo according to the user's share action and its own privacy terms.
- The Android bridge is intended only for the fixed trusted PhotoCraft GitHub Pages origin. Do not add arbitrary third-party pages to the WebView allowlist while the JavaScript download bridge is enabled.
