# J2ME Notes — Nokia C2-00

A small keypad-friendly Notes MIDlet for Java ME phones. Target: MIDP 2.0 / CLDC 1.1.

## Features
- Create, edit and delete notes
- Persistent local storage using Record Management System (RMS)
- Works offline
- Simple screen-based UI suitable for a feature phone

## Build with GitHub Actions
1. Create a new GitHub repository.
2. Upload all files in this project (keep the `.github/workflows/build.yml` path).
3. Open **Actions** and enable workflows if GitHub asks.
4. Push a commit to `main`/`master`, or choose **Build J2ME Notes → Run workflow**.
5. Open the successful workflow run, scroll to **Artifacts**, and download `j2me-notes-nokia-c2-00-preverified`.
6. Extract the ZIP; it contains `Notes.jar` and `Notes.jad`.

## Install on Nokia C2-00
Transfer `Notes.jar` to the phone via Bluetooth or memory card, then open it to install.
If installing through a file manager, use the JAR. JAD is useful for OTA installation.

## Important compatibility notes
- The project is compiled against MicroEmulator's CLDC 1.1 / MIDP 2.0 API stubs.
- The workflow compiles for Java 1.1 class-file compatibility and runs ProGuard 7.6.1 with `-microedition` to generate CLDC StackMap attributes (Java ME preverification). Shrinking, optimization and obfuscation are disabled.
- The JAR is packaged after preverification, and the JAD size is calculated from the final JAR. The phone's firmware still determines final compatibility; test the new build on the handset.
- Nokia C2-00 is a Series 40 Java phone; it commonly supports MIDP 2.0 and CLDC 1.1.
- The app uses standard MIDP UI and RMS only; no Android APIs, networking, or smartphone-only features.
- The UI uses standard phone fonts and softkey commands, not actual iOS graphics.

## Project structure
```text
src/com/example/notes/NotesMIDlet.java
build.xml
.github/workflows/build.yml
```

## Troubleshooting
- If Actions fails at `Install Ant and download tools`, inspect whether the Maven artifact URLs are still available.
- If the phone reports an invalid application, verify the downloaded JAR is intact and try transferring the JAR directly.
- If the phone reports unsupported application, its Java runtime/profile may differ from the expected MIDP 2.0 / CLDC 1.1.
