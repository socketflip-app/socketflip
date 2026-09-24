# SocketFlip

A floating button for Android that gives one chosen app a clean, instant reconnect.

Tap the button and Android closes the target app's open network connections. The
app notices straight away and reconnects over your normal network, usually within
a second or two. Nothing else on the phone is touched, and no traffic ever passes
through SocketFlip.

> **Worried by the install warnings?** Read **[Is SocketFlip safe? The warnings,
> explained](docs/SAFETY.md)**: why Google makes it awkward, what each permission
> is for, and why it uses a VPN when it is not a VPN.

## What it is for

- **Unsticking an app** that sits on "Connecting..." or a stale feed after a
  Wi-Fi hand-off, a dodgy hotspot or a network change, without toggling
  Wi-Fi or airplane mode for the whole phone.
- **Testing reconnect handling** in your own app: one tap reproduces a clean
  server disconnect on a real device, as often as you like.
- **Online games that settle a round on the server**: a quick reconnect drops
  you straight into the next phase instead of waiting out a long animation.

It works with any app you pick.

## What it can and cannot reset

SocketFlip resets **TCP** connections only. When a VPN starts or stops, Android
closes the covered app's open TCP sockets; it does not do this for **UDP**.

- **Reset:** anything that talks to its server over TCP, which is most app
  APIs, chat and notification links, game servers and ordinary web traffic.
- **Not reset:** voice and video calls (WhatsApp, Signal, Meet and similar),
  most online game voice chat, and traffic using QUIC / HTTP/3, all of which
  run over UDP. A call in progress simply carries on.

If you tap and nothing seems to happen, the app you chose is probably talking
UDP, or reconnected so quickly you did not notice.

## How it works

SocketFlip uses Android's VPN API, but not to carry traffic:

- The tunnel covers only the target app. It has the address `10.111.222.1` and
  routes exactly one other address, `10.111.222.2`, which nothing uses. All of
  the app's real traffic still goes out over Wi-Fi or mobile data as normal.
  There is no server at the other end and nothing leaves the phone.
- Whenever a VPN covering an app starts or stops, Android closes that app's open
  sockets. That is the clean disconnect.
- Each tap makes exactly **one** state change (tunnel up, or tunnel down), so each
  tap is exactly one disconnect. Bringing a tunnel up and straight back down hits
  the app twice, and a reconnect that lands between the two can get stuck.

Things that do **not** work, for the record:

- Airplane mode takes the whole Wi-Fi link down, which is slow to come back.
- A per-app firewall block only stalls the connection silently. A short stall is
  never noticed, so nothing reconnects.

Taps less than 10 seconds apart are ignored: a second disconnect while the app is
still reconnecting can leave it stuck on its reconnect screen.

While the tunnel is up the status bar shows a VPN key. That is expected; the next
tap takes it down again (and is also a reconnect).

## Using it

Full walkthrough, including Play Protect and permission prompts: **[docs/GUIDE.md](docs/GUIDE.md)**.
Problems and questions: **[docs/FAQ.md](docs/FAQ.md)**.

The short version:


1. Install the APK and open SocketFlip.
2. Tap **Target app** and choose the app.
3. Tap **Show floating button** and grant what it asks for: display over other
   apps, notifications, and the VPN connection request.
4. Drag the button wherever it is out of the way. Tap it to reconnect.

There is also a **SocketFlip** Quick Settings tile and a **Flip now** action on the
notification, which do the same thing. Only one VPN can run on Android at a time,
so SocketFlip will replace another VPN app's tunnel while its own is up.

## Automatic updates

SocketFlip is not on the Play Store, so it will not update itself. The easiest way
to stay up to date is **[Obtainium](https://github.com/ImranR98/Obtainium)**, a
free, open-source app that installs and updates apps straight from their GitHub
releases:

1. Install Obtainium.
2. Tap **Add App**, paste `https://github.com/socketflip-app/socketflip`, and tap
   **Add**.

Obtainium then tells you when a new release is out. Every SocketFlip release is
signed with the same key, so updates install over the top and keep your settings.

## Permissions

| Permission | Why |
|---|---|
| VPN (user consent) | To start and stop the per-app tunnel |
| Display over other apps | To draw the floating button |
| Foreground service, notifications | To keep the button alive over the other app |
| Access network state | To hand the target app your current DNS servers |

SocketFlip has no `INTERNET` permission, collects nothing and sends nothing.

## Building

Requires JDK 17 and the Android SDK (platform 35).

```sh
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Disclaimer

SocketFlip is not affiliated with or endorsed by any game or app developer. Forcing
reconnects in an online game may be against that game's terms of service; use it
at your own risk.

## Support

SocketFlip is free and always will be. If it saves you time, you can leave a tip at
<https://buy.stripe.com/cNidR84Lg4pT36l35jfnO00> (any amount, card, Apple Pay or
Google Pay). Thank you.

## License

MIT, see [LICENSE](LICENSE).
