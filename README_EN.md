# Customize Android TV Box Remote Shortcut Keys (No Root)

> **Türkçe:** [README.md](README.md) | Teknik doküman: [TEKNIK.md](TEKNIK.md) (TR) / [TECHNICAL.md](TECHNICAL.md) (EN)

Remap the hard-coded **Netflix, YouTube, Disney+, Prime Video** shortcut keys on Android TV / Google TV box remotes to any app or action you want — **no root, no PC required at runtime**.

What this project does:

- **Netflix key** → launches any app you choose (example: Televizo)
- **Prime Video key** → **rewind** in YouTube (N consecutive presses = N×10s back)
- **Disney+ key** → **fast-forward** in YouTube (N consecutive presses = N×10s forward)

Developed and tested on a Skyworth UHD Google TV STB (Android 14); it should also work on other boxes that use the same `SdtGlobalKey` mechanism.

## How does it work? (in short)

On these boxes, the shortcut keys are intercepted by the vendor's `SdtGlobalKeyHandler` system app, which looks up the `custom_app_hotkey_package_1..4` settings. We write **our invisible mini-apps** into those settings; when the key is pressed, the app launches, injects `input keyevent` via [Shizuku](https://shizuku.rikka.app/) and exits immediately. The seek reaches YouTube as a real D-pad press, so no media session is required. After setup, no PC is needed — everything runs on the box.

Flow summary:

1. A branded remote key arrives as a vendor broadcast.
2. The invisible mini-app registered in the slot setting starts and counts consecutive presses within a **1-second window**.
3. If the foreground app is not YouTube, the action is cancelled — the keys are **only active inside YouTube**.
4. Shizuku injects `input keyevent` with shell privileges; YouTube treats it as a real remote press and seeks.

System diagrams (mermaid): [TECHNICAL.md](TECHNICAL.md)

## Requirements

- Android TV box (capable of ADB/network debugging)
- A computer on the same network (setup only)
- `adb` installed (`sudo apt install adb`)

## Installation — step by step

### 1. Enable USB debugging on the box

Press OK 7 times on `Settings → System → About → Android TV OS build` → developer mode is enabled. Then turn on `Developer options → USB debugging`.

### 2. Find the box's IP and connect

```bash
# Find the device on the network (the box is usually the only Android device)
nmap -p 8008,8443 --open 192.168.1.0/24

# Connect — check "always allow" on the permission prompt shown on the TV
adb connect 192.168.1.101:5555
adb devices   # you should see "device"
```

### 3. Install and start Shizuku

```bash
# Install the Shizuku APK (apk/shizuku-v13.6.0.apk or from the official site)
adb install apk/shizuku-v13.6.0.apk

# Launch the app once
adb shell monkey -p moe.shizuku.privileged.api 1
```

Open the Shizuku app → **"Start via connecting to a computer" → "View command"** — copy the command it shows and run it (it looks like):

```bash
adb shell /data/app/~~XXXX==/moe.shizuku.privileged.api---XXXX==/lib/arm/libshizuku.so
```

`shizuku_server` starts running. **This command must be run once after every box reboot.**

### 4. Install the mini-apps

```bash
adb install apk/seekback.apk   # Prime key → rewind
adb install apk/seekfwd.apk    # Disney key → fast-forward
```

### 5. Assign the keys

```bash
# Prime Video key (keycode 138, slot 3) → rewind
adb shell settings put global custom_app_hotkey_package_3 dev.nuri.seekback

# Disney+ key (keycode 136, slot 4) → fast-forward
adb shell settings put global custom_app_hotkey_package_4 dev.nuri.seekfwd

# Netflix key (keycode 265, slot 2) → any app you want (e.g. Televizo)
adb shell settings put global custom_app_hotkey_package_2 com.ottplay.ottplay
```

> **The slot mapping may differ on your box model.** To discover your own keys, run `adb logcat | grep SdtGlobalKey` and press the remote buttons — you'll see which keycode launches which app. Details: [TECHNICAL.md](TECHNICAL.md)

### 6. Approve the Shizuku permission (one time)

Press the Prime and Disney keys once each — a dialog appears asking **"Allow … to access Shizuku?"**, choose **"Always allow"**. It appears once per app.

**Setup is complete.** The keys now work even with the PC turned off.

## Usage

| Key                                  | Behavior                                       |
| ------------------------------------ | ---------------------------------------------- |
| Netflix                              | Opens Televizo (or whichever app you assigned) |
| Prime Video                          | 10s rewind in YouTube                          |
| Disney+                              | 10s fast-forward in YouTube                    |
| Prime/Disney, N consecutive presses  | **N × 10s** back/forward                       |

The seek keys **only work while YouTube is open** — they do nothing inside other apps. After each press the app **waits 1 second**; if another press arrives within that window the counter increases and the wait resets, otherwise it seeks press-count × 10s in one burst and keeps playback going with `MEDIA_PLAY`.

## Troubleshooting

- **Key does nothing:** the Shizuku server may not be running (if the box was rebooted, repeat the command from step 3). Check with `adb shell ps -ef | grep shizuku_server`.
- **Prime/Disney app opens instead:** the slot assignment may be wrong — check with `adb shell settings get global custom_app_hotkey_package_3`.
- **Permission dialog keeps reappearing:** "Always allow" was not selected; press once more and tick it.
- **To restore the original behavior:**

```bash
adb shell settings delete global custom_app_hotkey_package_3
adb shell settings delete global custom_app_hotkey_package_4
```

## Files

| Path           | Contents                                                        |
| -------------- | --------------------------------------------------------------- |
| `apk/`         | Ready-to-install APKs (seekback, seekfwd, shizuku)              |
| `src/`         | Java source code + manifests of the mini-apps                   |
| `scripts/`     | Setup helpers and the PC-daemon alternative                     |
| `TEKNIK.md`    | Technical documentation (Turkish)                               |
| `TECHNICAL.md` | Technical documentation (English)                               |

For technical details (vendor broadcast mechanism, keycode discovery, Shizuku integration, building the APKs from source): **[TECHNICAL.md](./TECHNICAL.md)**

## Security notes

- Shizuku is a bridge that runs with shell privileges; apps you authorize can execute system-level commands. Only select "Always allow" for apps you trust.
- `input keyevent` injection is just key simulation; it collects no data.

## License

The code is MIT licensed. Shizuku is a RikkaApps project and is subject to its own license.
