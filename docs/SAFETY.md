# Is SocketFlip safe? The warnings, explained

Installing SocketFlip shows you several warnings, and one of them says the app
could "monitor network traffic". That sounds alarming, so this page explains, in
plain language:

1. [Why Google makes installing it awkward](#why-google-makes-this-awkward)
2. [Every warning you will see, and what to tap](#every-warning-in-order)
3. [Why SocketFlip needs each permission](#why-each-permission)
4. [Why it uses a VPN when it is not a VPN](#why-a-vpn)
5. [How to check all of this for yourself](#check-it-yourself)

**The short version:** the warnings appear because SocketFlip is installed from
outside the Play Store and uses powerful Android features. They are general
warnings Android shows for *any* app that does these things, not a verdict on
SocketFlip. SocketFlip has no permission to use the internet, so it cannot send
anything anywhere, and all of its code is public.

## Why Google makes this awkward

Most Android malware reaches people as an app file sent in a message or offered
on a dodgy website: "install this to track your parcel", "update your banking
app here". To protect people from that, Google puts several speed bumps between
you and any app that does not come from the Play Store:

- Your browser warns that the file "might be harmful".
- Android will not install it until you allow that browser to install apps.
- Google Play Protect checks it, and is suspicious of anything it has not seen
  before.
- Some powerful settings are locked for apps installed this way until you unlock
  them yourself.

These are sensible defaults for most people, and they apply to every app from
outside the Play Store, good or bad. They slow you down; they do not mean
SocketFlip has been found to be harmful.

**Why not just put it on the Play Store?** Google only lets an app use Android's
VPN feature if the app *is* a VPN service or one of a short list of approved
types. SocketFlip uses the VPN feature for something else (explained
[below](#why-a-vpn)), so it would be turned down. Publishing it here, with the
full source, is the honest alternative.

## Every warning, in order

| When | What you see | What it means | What to tap |
|---|---|---|---|
| Downloading | "This type of file can harm your device" | Your browser says this for every app file. | **Download anyway** |
| Opening the file | "Your phone is not allowed to install unknown apps from this source" | Android's gate for apps from outside the Play Store. You allow it once, for that browser. | **Settings**, turn on **Allow from this source**, go back, **Install** |
| Installing | Play Protect: "Send app for security check?", "App scan recommended" or "Unsafe app blocked" | Google has not seen SocketFlip before, because it is not on the Play Store. | **Scan app**, or **More details** then **Install anyway** |
| First run | "Display over other apps" settings page | SocketFlip asks to draw its floating button. See [below](#display-over-other-apps). | Turn SocketFlip **on**, go back |
| First run, some phones | That switch is greyed out ("restricted setting") | Android locks this for apps installed from a browser. | App info, **three-dot menu**, **Allow restricted settings**, then turn it on |
| First run | "Allow SocketFlip to send you notifications?" | Keeps the button alive and gives you Flip now and Stop buttons. | **Allow** |
| First run | "Connection request: SocketFlip wants to set up a VPN connection that allows it to monitor network traffic. Only accept if you trust the source." | The scary one. Android shows these exact words for **every** VPN app. See [Why a VPN](#why-a-vpn). | **OK** |
| After a tap | A key icon in the status bar | SocketFlip's (empty) tunnel is on. It goes away on the next tap. | Nothing |

## Why each permission

SocketFlip asks for five things. Here is every one, and nothing else:

### Display over other apps
To draw the round button on top of the app you are using. Android treats this as
a powerful permission because a malicious app could draw a fake login screen over
your banking app. SocketFlip only ever draws one small round button, and you can
see exactly that in the code (`OverlayService.kt`).

### Notifications
Android only lets an app keep running in the background if it shows a
notification, so you always know it is there. The notification is also where the
**Flip now** and **Stop** buttons live. If you refuse, the floating button still
works; you just do not see the notification.

### Run in the foreground ("special use")
The technical name for "keep the floating button on screen while another app is
in front". It is paired with the notification above. It does not show a prompt.

### View network connections
Lets SocketFlip read which DNS servers your Wi-Fi or mobile network uses, so it
can pass them on to the app you picked (otherwise that app could not look up
addresses while the tunnel is on). It can only *read* this, not change it, and it
does not show a prompt.

### VPN (the connection request)
Explained in full in the next section.

### What SocketFlip does **not** have
- **No internet permission.** Android will not let SocketFlip open a single
  connection of its own. It cannot upload, report or "phone home", even if it
  wanted to.
- No access to your contacts, messages, photos, files, location, microphone,
  camera or accounts.
- No accessibility access, which is what apps need to read or control what is
  on your screen.

## Why a VPN

SocketFlip needs a way to make one app drop its connection and reconnect,
instantly, without touching the rest of your phone. Android has no "reconnect
this app" button, and ordinary apps are not allowed to interfere with other apps'
connections.

There is one thing Android always does, though: **whenever a VPN covering an app
starts or stops, Android closes that app's open connections**, so they can be
reopened through the new route. SocketFlip uses exactly that, and nothing more:

- Its "VPN" covers **only the one app you choose**. Every other app is untouched.
- It routes **one private address that nothing uses** (`10.111.222.2`). None of
  your traffic, and none of the chosen app's real traffic, goes into it. Everything
  still goes out over your Wi-Fi or mobile data as normal.
- There is **no server** at the other end. Nothing leaves your phone.
- Each tap switches it on or off. Either change makes Android close the app's
  connections once, and the app reconnects about a second later.

So why does Android say it "allows it to monitor network traffic"? Because the
VPN feature *could* be used that way, so Android shows the same wording for every
VPN app, whether it is a big-name VPN service or SocketFlip. The warning describes
what the feature is capable of, not what the app does. SocketFlip never reads
from its tunnel, and with no internet permission it would have nowhere to send
anything.

Android allows only one VPN at a time, so if you use a privacy VPN, SocketFlip's
tap will switch it off. See the [FAQ](FAQ.md#i-use-another-vpn-app).

## Check it yourself

You do not have to take this page's word for any of it:

- **The code is public.** Everything the app does is in
  [app/src/main/java/app/socketflip](../app/src/main/java/app/socketflip), five
  short files. The permissions are listed in
  [AndroidManifest.xml](../app/src/main/AndroidManifest.xml).
- **See its permissions on your phone.** Settings, Apps, SocketFlip,
  Permissions (and "All permissions" or "App details" where your phone offers
  it). You will not find internet access. On GrapheneOS there is not even a
  Network switch to turn off, because it never asked for one.
- **Check the file is genuine.** The release page lists the file's SHA-256 and
  the signing certificate; see the
  [FAQ](FAQ.md#how-do-i-check-the-apk-is-genuine).
- **Build it yourself.** The [README](../README.md#building) shows how to build
  the exact same app from the source.

If something here does not match what you see, please
[open an issue](https://github.com/socketflip-app/socketflip/issues).
