# SocketFlip FAQ

- [Installing](#installing)
- [It is not working](#it-is-not-working)
- [How it works, privacy and safety](#how-it-works-privacy-and-safety)
- [Other questions](#other-questions)

## Installing

### Why is it not on the Play Store?

Google Play only allows apps to use Android's VPN feature when the app is a VPN
service or one of a short list of approved kinds of app. SocketFlip uses the VPN
feature for a side effect and never carries traffic, so it would almost certainly
be rejected. It is published here instead, with the full source.

### Play Protect says "Unsafe app blocked" or "App scan recommended"

That is Play Protect's normal reaction to an app it has not seen before. Tap
**More details** then **Install anyway**, or **Scan app**. See
[the guide, step 3](GUIDE.md#3-google-play-protect).

### How do I check the APK is genuine?

Each release lists two fingerprints:

- the **SHA-256 of the APK file**, which you can check on a computer with
  `sha256sum socketflip-1.0.apk` (Linux, macOS) or
  `Get-FileHash socketflip-1.0.apk` (Windows PowerShell);
- the **signing certificate SHA-256**, which stays the same for every release:
  `23c4cf27a70b2b804f58726183bd771e6fd1844853e9362b4c43a4e92263b179`.
  On a computer: `apksigner verify --print-certs socketflip-1.0.apk`. On the phone,
  the open-source app **AppVerifier** can check it before you install.

### "App not installed" or "conflicts with an existing package"

The file is signed with a different key from the SocketFlip already on your phone.
Genuine releases never do this. Only uninstall the old one if you are sure the new
file came from this repository's releases page.

### "There was a problem parsing the package"

The download was cut short, or your Android is older than 10. Download it again.

### Will Android stop me installing apps from outside the Play Store?

Google has announced that certified Android phones will gradually only install
apps from developers registered with Google, starting in a few countries in 2026
and spreading after that. If your phone refuses to install SocketFlip with a message
about an unverified developer, please open an issue here and say which country
you are in.

### Does it work on iPhone?

No. iOS does not allow floating buttons over other apps, or a per-app VPN
outside company-managed devices.

## It is not working

### The "Display over other apps" switch is greyed out

On Android 13 and newer, some settings are locked for apps installed from a
browser or file manager ("restricted settings"). To unlock them:

1. Open **Settings > Apps > SocketFlip** (or long-press the icon, **App info**).
2. Tap the **three-dot menu** in the top corner.
3. Tap **Allow restricted settings** and confirm with your PIN or fingerprint.
4. Go back and turn on **Display over other apps**.

If there is no three-dot menu, the setting is not restricted on your phone; try
the switch again.

### My whole phone lost its internet connection

SocketFlip has been set as the **Always-on VPN** with **Block connections without
VPN**. Its tunnel deliberately carries no traffic, so that setting blocks
everything. Open **Settings > Network & internet > VPN**, tap the gear next to
SocketFlip, and turn both switches off. From version 1.1 Android greys these
switches out for SocketFlip, so update if you can.

### I tap the button and nothing happens

Work down this list:

1. **Wrong target app.** Open SocketFlip and check the **Target app** line.
2. **Tapped too soon.** Taps less than 10 seconds apart are ignored. A short
   message says "Still reconnecting" when this happens.
3. **The app uses UDP, not TCP.** SocketFlip only resets TCP connections. Voice and
   video calls, most game voice chat and anything using QUIC / HTTP/3 are UDP and
   carry on untouched. See [What can SocketFlip reset?](#what-can-socketflip-reset)
4. **It did work, very quickly.** Many apps reconnect so fast there is nothing to
   see. Watch for the key icon appearing or disappearing in the status bar: if it
   changed, SocketFlip did its part.
5. **Another VPN is locked on.** See the next question.

### I use another VPN app

Android allows only one VPN at a time.

- If your other VPN is running, the first tap on SocketFlip **disconnects it** and
  SocketFlip's tunnel takes its place. Reconnect your VPN afterwards. SocketFlip is not
  meant to be used alongside a privacy VPN.
- If your other VPN is set to **Always-on VPN** with **Block connections without
  VPN**, Android will not let SocketFlip start at all. You will see "SocketFlip needs VPN
  permission". Turn that setting off in **Settings > Network > VPN** if you want
  to use SocketFlip.

### The app got stuck on its reconnecting screen

Usually a second disconnect arrived while the app was still reconnecting. Tap the
app's own reconnect button, or wait for it to retry. The 10-second cooldown exists
to prevent this; if it keeps happening with one particular app, please open an
issue saying which app.

### The floating button disappeared

- After a **restart** of the phone: SocketFlip does not start itself. Open SocketFlip and
  tap **Show floating button**.
- If a **battery saver** closed it: in **Settings > Apps > SocketFlip > Battery**,
  choose **Unrestricted**.

### The button is in the way

Drag it to any edge. It remembers where you left it.

### A key icon stays in the status bar

SocketFlip's tunnel is up. That does nothing to your traffic; the next tap removes
it, and so does **Stop** on the notification.

### "SocketFlip could not reconnect: ..."

Something unexpected went wrong. Please open an issue with the exact message and
your phone model and Android version.

## How it works, privacy and safety

### Why does it need a VPN?

SocketFlip uses Android's VPN feature as a switch, not as a VPN. When a VPN covering
an app starts or stops, Android closes that app's open connections. That is the
reconnect. SocketFlip's tunnel covers only the app you picked, claims one private
address that nothing uses (`10.111.222.2`), and routes nothing else. All of your
traffic, including the target app's, goes out over Wi-Fi or mobile data as
normal. There is no server at the other end.

### Can SocketFlip see my traffic?

No. No traffic enters the tunnel, SocketFlip never reads from it, and SocketFlip does not
even have Android's internet permission, so it could not send anything anywhere.
It collects nothing.

### What can SocketFlip reset?

TCP connections, which is what most apps use to talk to their servers: app APIs,
chat and notification links, game servers and ordinary web traffic. It cannot
reset UDP traffic: voice and video calls, most game voice chat, and QUIC / HTTP/3.
A call in progress simply carries on.

### Does it drain the battery or slow my connection?

No. Between taps it does nothing except keep the button on screen. The tunnel
carries no traffic, so it cannot slow anything down.

### Is it different from turning on airplane mode?

Very. Airplane mode drops Wi-Fi and mobile data for the whole phone, and getting
them back takes several seconds. SocketFlip touches only the one app and it
reconnects at once. A per-app firewall block does not work either: it only stalls
the connection, and a short stall is never noticed.

### Is it safe to use in online games?

SocketFlip is not affiliated with any game or app developer. Forcing reconnects may be
against a game's terms of service, and what a developer does about that is up to
them. Use it at your own risk.

## Other questions

### Is it free?

Yes, and it always will be. If it saves you time, tap **Leave a tip** at the
bottom of SocketFlip's main screen, or go to
<https://buy.stripe.com/cNidR84Lg4pT36l35jfnO00>. The button just opens that page
in your browser; SocketFlip itself still has no internet permission. After your
25th flip SocketFlip shows a one-time thank-you card; **Not now** hides it for
good.

### How do I report a problem or suggest something?

Open an issue at <https://github.com/socketflip-app/socketflip/issues> with your phone
model, Android version, the target app and what happened.
