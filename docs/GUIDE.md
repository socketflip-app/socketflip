# SocketFlip: install and first run

This walks through installing SocketFlip from GitHub and setting it up. It takes
about two minutes. SocketFlip needs Android 10 or newer.

Phone makers word their screens slightly differently. Where a step says
"something like", look for the nearest match.

## 1. Download

1. On your phone, open <https://github.com/socketflip-app/socketflip/releases/latest>.
2. Under **Assets**, tap the `.apk` file (for example `socketflip-1.0.apk`).
3. If your browser warns that "this type of file can harm your device", tap
   **Download anyway**. Every APK download gets this warning.

## 2. Allow your browser to install apps

Android blocks installs from anywhere other than the Play Store until you allow
it once, per app.

1. Open the downloaded file (from the download notification, or your Files app).
2. Android says something like **"For your security, your phone is not allowed
   to install unknown apps from this source"**. Tap **Settings**.
3. Turn on **Allow from this source**, then go back.
4. Tap **Install**.

You can turn that switch off again afterwards; SocketFlip stays installed.

## 3. Google Play Protect

On most phones Google Play Protect checks apps installed from outside the Play
Store. SocketFlip is new and is not on the Play Store, so Play Protect has never seen
it and may say one of these:

- **"Send app for security check?"** Either choice is fine. Tapping **Send**
  helps Google learn the app.
- **"App scan recommended"**: tap **Scan app**. It should come back clean.
- **"Unsafe app blocked"**: tap **More details**, then **Install anyway**.

SocketFlip has no internet permission and sends nothing anywhere, and the whole
source is published in this repository. If you want to be sure the file is the
genuine one, see [How do I check the APK is genuine?](FAQ.md#how-do-i-check-the-apk-is-genuine).

## 4. First run

1. Open **SocketFlip**.
2. Tap **Choose target app** and pick the app you want to reconnect.
3. Tap **Show floating button**. SocketFlip now asks for three things, one at a
   time. After each one, come back to SocketFlip and tap **Show floating button**
   again until the button appears.

   - **Display over other apps**: Android opens a settings page. Turn SocketFlip
     **on**, then go back.
     If the switch is greyed out or says it is controlled by a restricted
     setting, see [The "Display over other apps" switch is greyed
     out](FAQ.md#the-display-over-other-apps-switch-is-greyed-out).
   - **Notifications**: tap **Allow**. The notification keeps the button alive
     and gives you **Flip now** and **Stop** buttons. If you refuse, the button
     still works, you just do not see the notification.
   - **Connection request** ("SocketFlip wants to set up a VPN connection"): tap
     **OK**. See [Why does it need a VPN?](FAQ.md#why-does-it-need-a-vpn) for
     what this does and does not mean.

4. A round blue button appears on the left of the screen. Drag it anywhere.

## 5. Using it

- Open the app you chose, and when you want a clean reconnect, **tap the blue
  button once**. It flashes orange. The app should drop its connection and
  reconnect within a second or two.
- Taps less than 10 seconds apart are ignored, on purpose: a second disconnect
  while the app is still reconnecting can leave it stuck.
- A **key icon** appears in the status bar after the first tap and disappears
  after the next. That is normal: every tap switches SocketFlip's tunnel on or off,
  and both directions cause the reconnect.

### Other ways to trigger it

- **Quick Settings tile**: pull down the notification shade twice, tap the
  pencil (edit) icon, and drag the **SocketFlip** tile into your tiles.
- **Notification**: tap **Flip now** on SocketFlip's notification.

### Turning it off

- Tap **Stop** on the notification, or open SocketFlip and tap **Hide floating
  button**. Either one also removes the tunnel, so the phone is exactly as it was.

## 6. Updating

Download the new `.apk` from the releases page and install it over the top, the
same way as step 2. Your settings are kept. Every release is signed with the same
key, so Android accepts it as an update. If Android ever says the app "conflicts
with an existing package", the file is **not** a genuine release; do not force it.

## 7. Uninstalling

Long-press the SocketFlip icon, tap **App info**, then **Uninstall**. Nothing is left
behind.
