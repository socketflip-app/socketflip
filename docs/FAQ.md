# SocketFlip FAQ

- [Installing](#installing)
- [It is not working](#it-is-not-working)
- [How it works, privacy and safety](#how-it-works-privacy-and-safety)
- [Other questions](#other-questions)

## Installing

### Why is it not on the Play Store?

Google Play only allows apps to use Android's VPN feature when the app is a VPN
service or one of a short list of approved kinds of app. SocketFlip uses the VPN
feature for a side effect and never carries traffic, so it would probably be
turned down. It is published here instead, with the full source.

### Play Protect says "Unsafe app blocked" or "App scan recommended"

That is Play Protect's normal reaction to an app it has not seen before. Tap
**More details** then **Install anyway**, or **Scan app**. See
[the guide, step 3](GUIDE.md#3-google-play-protect).

### How do I check the APK is genuine?

Each release lists two fingerprints:

- the **SHA-256 of the APK file**, which you can check on a computer with
  `sha256sum socketflip-<version>.apk` (Linux, macOS) or
  `Get-FileHash socketflip-<version>.apk` (Windows PowerShell);
- the **signing certificate SHA-256**, which stays the same for every release:
  `23c4cf27a70b2b804f58726183bd771e6fd1844853e9362b4c43a4e92263b179`.
  On a computer: `apksigner verify --print-certs socketflip-<version>.apk`. On the phone,
  the open-source app **AppVerifier** can check it before you install.

### "App not installed" or "conflicts with an existing package"

The file is signed with a different key from the SocketFlip already on your phone.
Genuine releases never do this. Only uninstall the old one if you are sure the new
file came from this repository's releases page.

### "There was a problem parsing the package"

The download was cut short, or your Android is older than 10. Download it again.

### Will Android stop me installing apps from outside the Play Store?

Not yet. Google's 2026 developer registration rules only apply to a few
participating app stores, so nothing changes in 2026 for SocketFlip installed from
this page. Google plans to extend them to all certified Android phones in 2027,
and apps from unregistered developers would then need extra steps to install.
If your phone refuses to install SocketFlip with a message about an unverified
developer, please open an issue here and say which country you are in.

### Does it work on iPhone?

No, and it cannot be made to. iOS does not allow floating buttons over other
apps, or a per-app VPN outside company-managed devices. On iPhone, switching to
another app and back reportedly already gives some games a reconnect.

## It is not working

### The "Display over other apps" or "Usage access" switch is greyed out

On Android 13 and newer, some settings are locked for apps installed from a
browser or file manager ("restricted settings"). Android 15 added Usage access to
that list, which the settings that follow the target app need. To unlock them:

1. Open **Settings > Apps > SocketFlip** (or long-press the icon, **App info**).
2. Tap the **three-dot menu** in the top corner.
3. Tap **Allow restricted settings** and confirm with your PIN or fingerprint.
4. Go back and turn on **Display over other apps** (or **Usage access**).

If there is no three-dot menu, the setting is not restricted on your phone; try
the switch again.

### My whole phone lost its internet connection

SocketFlip has been set as the **Always-on VPN** with **Block connections without
VPN**. Its tunnel deliberately carries no traffic, so that setting blocks
everything. Open **Settings > Network & internet > VPN**, tap the gear next to
SocketFlip, and turn both switches off. From version 1.1 Android greys these
switches out for SocketFlip, so update if you can.

### I tap the button and nothing happens

Open SocketFlip and tap **Check setup** first: it shows every permission and
setting SocketFlip needs, with a **Fix** button next to anything wrong. If that is
all green, work down this list:

1. **Wrong target app.** Open SocketFlip and check the **Target app** line.
2. **Tapped too soon.** Taps before the cooldown ring has run out are ignored. A
   short message says "Still reconnecting" when this happens (unless hint
   messages are turned off in Settings).
3. **The app uses UDP, not TCP.** SocketFlip only resets TCP connections. Voice and
   video calls, most game voice chat and anything using QUIC / HTTP/3 are UDP and
   carry on untouched. See [What can SocketFlip reset?](#what-can-socketflip-reset)
4. **It did work, very quickly.** Many apps reconnect so fast there is nothing to
   see. Watch for the key icon appearing or disappearing in the status bar: if it
   changed, SocketFlip did its part.
5. **Another VPN app is set as Always-on.** See
   [SocketFlip says another VPN app is set as Always-on](#socketflip-says-another-vpn-app-is-set-as-always-on).

### I use another VPN app

Android allows only one VPN at a time.

- Just opening SocketFlip leaves your other VPN alone.
- If your other VPN is running, turning the floating button on (or the first tap)
  **disconnects it** and SocketFlip's tunnel takes its place. SocketFlip warns you
  once before that happens, and shows a short message whenever a tap replaces
  another VPN. Reconnect your VPN afterwards. SocketFlip is not meant to be used
  alongside a privacy VPN.
- If your other VPN app is set as **Always-on VPN**, Android will not let SocketFlip
  start at all. See the next question.

### SocketFlip says another VPN app is set as Always-on

Android will not let any app replace an Always-on VPN, so it closes SocketFlip's
VPN request before you even see it. Ad blockers and privacy apps that work as a
VPN (AdGuard, Blokada, RethinkDNS, NetGuard, Proton, Mullvad and others) often ask
to be Always-on, and work phones may set it too. To use SocketFlip, open
**Settings > Network & internet > VPN**, tap the gear next to that app and turn
**Always-on VPN** off. You can turn it back on when you are done.

### The app got stuck on its reconnecting screen

Usually a second disconnect arrived while the app was still reconnecting. Tap the
app's own reconnect button, or wait for it to retry. The cooldown exists to
prevent this. If you have shortened it in **Settings**, set it back to 10 seconds
or more. If it keeps happening with one particular app, please open an issue
saying what kind of app it is (game, chat,
browser...).

### The floating button disappeared

- After a **restart** of the phone: SocketFlip does not start itself. Open SocketFlip and
  tap **Show floating button**.
- If a **battery saver** closed it: open SocketFlip, tap **Check setup**, and tap
  **Fix** next to Battery optimisation. Or in **Settings > Apps > SocketFlip >
  Battery**, choose **Unrestricted**. Samsung, Xiaomi, OnePlus and some other
  makers have extra settings of their own; [dontkillmyapp.com](https://dontkillmyapp.com)
  has step-by-step instructions for each brand.

### The button is in the way

Drag it to any edge. It remembers where you left it. In **Settings > Appearance**
you can also make it smaller or more see-through while it is resting, and turn
on **Snap to the screen edge** so it tucks itself against the side when you let go.

### I made the button too faint or the wrong colour

Open SocketFlip, tap **Settings**, and tap **Reset appearance to default** at the
bottom. Opacity never goes below 15%, so the button can always be found again.

### A key icon stays in the status bar

SocketFlip's tunnel is up. That does nothing to your traffic; the next tap removes
it, and so does **Stop** on the notification. To have it go away by itself, turn on
**Settings > Take the tunnel down when I leave the target app**.

### "SocketFlip could not reconnect: ..."

Something unexpected went wrong. Please open an issue with the exact message.
**Check setup > Copy report** puts your phone model, Android version and every
setting SocketFlip depends on onto the clipboard, ready to paste into the issue.
It does not include the name of the app you chose, and there is no need to name
it in the issue.

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

### Which DNS server does the app use while the tunnel is up?

Your own network's. While the tunnel is up Android asks it which DNS servers the
target app should use, so SocketFlip passes on the ones your Wi-Fi or mobile network
already gave you. If the phone changes network while the tunnel is up (leaving
home Wi-Fi for mobile data, for example), SocketFlip switches to the new network's
servers straight away.

The exception: if the network offers none that SocketFlip can use, it falls back
to Cloudflare (`1.1.1.1`) and Quad9 (`9.9.9.9`). That includes mobile networks that
only hand out IPv6 DNS servers. Nothing changes while the tunnel is down.

### Does it drain the battery or slow my connection?

Not noticeably. Between taps it only keeps the button on screen, unless you turn
on one of the settings that follow the target app, which check which app is in
front about once a second while the screen is on. The tunnel carries no traffic,
so it cannot slow anything down.

### Is it different from turning on airplane mode?

Very. Airplane mode drops Wi-Fi and mobile data for the whole phone, and getting
them back takes several seconds. SocketFlip touches only the one app and it
reconnects at once. A per-app firewall block does not work either: it only stalls
the connection, and a short stall is never noticed.

### Is it safe to use in online games?

SocketFlip is not affiliated with any game or app developer. Many online games'
terms forbid tools that give a player an advantage, and a game's developer can see
every reconnect on its servers. We know of no one penalised for reconnecting, but
that can change without notice. Use it at your own risk, on an account you would
not mind losing, and never in tournaments.

### Will SocketFlip ever reconnect automatically?

No, by design. Every reconnect is one tap by you, one per use. SocketFlip has no
timer or "every round" mode and will not get one.

## Other questions

### Is it free?

Yes, and it always will be. If it saves you time, tap **Donate** at the
bottom of SocketFlip's main screen, or go to
<https://buy.stripe.com/cNidR84Lg4pT36l35jfnO00>. The button just opens that page
in your browser; SocketFlip itself still has no internet permission. After your
25th flip SocketFlip shows a one-time thank-you card; **Not now** hides it for
good.

### How do I report a problem or suggest something?

Open an issue at <https://github.com/socketflip-app/socketflip/issues> with your phone
model, Android version, what kind of app you use it with (game, chat, browser...)
and what happened. **Check setup > Copy report** gives you most of that in one
paste. There is no need to name the app.
