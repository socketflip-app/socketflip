# SocketFlip: install and first run

This walks through installing SocketFlip from GitHub and setting it up. It takes
about two minutes. SocketFlip needs Android 10 or newer.

You will see some warnings along the way, including one about "monitoring
network traffic". They are normal for any app installed this way; **[the
warnings, explained](SAFETY.md)** says why each one appears.

Phone makers word their screens slightly differently. Where a step says
"something like", look for the nearest match.

## 1. Download

1. On your phone, open <https://github.com/socketflip-app/socketflip/releases/latest>.
2. Under **Assets**, tap the `.apk` file (named `socketflip-<version>.apk`, for example `socketflip-1.6.apk`).
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
2. Tap **+ Add app** and pick the app you want to reconnect. Type in the search box
   to find it quickly; apps that Android knows are games are listed first.
3. Tap the big **Show floating button** button. SocketFlip now asks for three
   things, one at a time (four on Samsung, Xiaomi, OnePlus and similar phones, and
   one more note if another VPN is on). After each one, come back to SocketFlip: it
   carries on by itself and says what comes next, until the button appears.

   - **Display over other apps**: Android opens a settings page. Turn SocketFlip
     **on**, then go back.
     If the switch is greyed out or says it is controlled by a restricted
     setting, see [The "Display over other apps" switch is greyed
     out](FAQ.md#the-display-over-other-apps-or-usage-access-switch-is-greyed-out).
   - **Notifications**: tap **Allow**. The notification keeps the button alive
     and gives you **Flip now** and **Stop** buttons. If you refuse, the button
     still works, you just do not see the notification.
   - **Connection request** ("SocketFlip wants to set up a VPN connection"):
     SocketFlip first explains what is coming; tap **Continue**, then **OK** on
     Android's request. See [Why does it need a VPN?](FAQ.md#why-does-it-need-a-vpn) for
     what this does and does not mean. If SocketFlip instead says another VPN app
     is set as Always-on, see
     [that FAQ entry](FAQ.md#socketflip-says-another-vpn-app-is-set-as-always-on).
   - **Only on Samsung, Xiaomi, OnePlus and similar phones**: "Keep the button on
     screen?", then Android's own "Let app always run in background?". Tap
     **Allow** both times. These phones close background apps to save battery,
     which would make the button disappear.
   - **Only if another VPN is on**: a note that Android runs one VPN at a time, so
     SocketFlip's tunnel will switch the other one off. Tap **Got it** to carry on.

4. A round blue button appears on the left of the screen. Drag it anywhere.

## 5. Using it

- Open the app you chose, and when you want a clean reconnect, **tap the
  button once**. The app should drop its connection and reconnect within a
  second or two.
- The button is **blue** while SocketFlip's tunnel is down and **teal** while it
  is up. Both are fine: every tap switches it over, and either way is a reconnect.
- An **amber ring** around the button counts down the cooldown. Taps before it
  runs out are ignored, on purpose: a second disconnect while the app is still
  reconnecting can leave it stuck. The cooldown is 10 seconds; you can change it
  in **Settings**.
- A **key icon** appears in the status bar after the first tap and disappears
  after the next. That is normal: every tap switches SocketFlip's tunnel on or off,
  and both directions cause the reconnect.

### Emergency restart (optional)

Very occasionally an app stays stuck on its reconnecting screen and needs a
restart. Turn on **Settings > Emergency restart**: while the cooldown runs, the
button turns into a red **!**, and tapping it asks whether to restart the app. It
always asks first, so a stray tap never restarts anything.

### Following the target app (optional)

Two settings follow which app is on screen. Both need **Usage access**, which
SocketFlip asks for when you turn one on:

- **Only show the button while a ticked app is on screen** hides the button
  everywhere else. The notification stays, so you can still stop it.
- **Take the tunnel down when I leave the ticked apps** removes the VPN key as soon
  as you switch away, so it does not linger or keep another VPN off.

### Making it your own

Open **Settings**. At the top, a preview shows the button in every state as you
change things.

- **Skins** is a row of ready-made looks. Swipe along it and tap one to use it.
- **Customise** has a colour picker for the button (tunnel down and tunnel up),
  the cooldown ring and the icon, a switch to mirror the arrow so it turns the
  other way, and sliders for size and for opacity, both while resting and just
  after a tap.
- **Reset appearance to default** undoes everything.

### Other ways to trigger it

- **Quick Settings tile**: pull down the notification shade twice, tap the
  pencil (edit) icon, and drag the **SocketFlip** tile into your tiles.
- **Notification**: tap **Flip now** on SocketFlip's notification.
- **Home screen**: long-press the SocketFlip icon for **Flip now**, **Show
  button** and **Hide button**. Drag one out to make it an icon of its own.
- **Automation apps** (Tasker, MacroDroid, Key Mapper and similar): turn on
  **Settings > Let other apps trigger a flip**, tap **Copy code** under it, then
  have the app send a broadcast with action `app.socketflip.FLIP` (or
  `app.socketflip.STOP` to take the tunnel down) to package `app.socketflip`, with a
  string extra named `token` set to that code. Broadcasts without the right code
  are ignored, so other apps cannot trigger it. The floating button must be
  switched on. Key Mapper can use this to flip on a volume button press.

### One tap to start

Under **Target apps**, tap **Home screen shortcut** (choose the app if you have
more than one). Android asks to add it to your home screen. The icon is the app's
own at full size (your home screen may add a small SocketFlip mark to it); tapping
it shows the floating button and opens the app in one go. If something still needs
allowing, SocketFlip opens instead, says so, and walks you through it; after that
the shortcut works in one tap.

### More than one app

Add as many apps as you like with **+ Add app**. Every **ticked** app is
reconnected by each tap, all at once; untick one to leave it alone without
removing it. Each app has a **Cooldown** button, to give it its own cooldown, and
a **Remove** button. With several apps ticked, the longest of their cooldowns
applies.

### Turning it off

- Tap **Stop** on the notification, or open SocketFlip and tap **Hide floating
  button**. Either one also removes the tunnel, so the phone is exactly as it was.

## 6. Updating

Download the new `.apk` from the releases page and install it over the top, the
same way as step 2. Your settings are kept. Every release is signed with the same
key, so Android accepts it as an update. If Android ever says the app "conflicts
with an existing package", the file is **not** a genuine release; do not force it.

Tip: to be told about new releases automatically, add SocketFlip to
[Obtainium](https://github.com/ImranR98/Obtainium) (see the
[README](../README.md#automatic-updates)).

## 7. Uninstalling

Long-press the SocketFlip icon, tap **App info**, then **Uninstall**. Nothing is left
behind.
