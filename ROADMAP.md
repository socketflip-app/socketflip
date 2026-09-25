# Roadmap

What is planned for SocketFlip, roughly in order. Items are ticked off as they
ship. Ideas and requests are welcome in the
[issues](https://github.com/socketflip-app/socketflip/issues).

## Done

- [x] Floating button, Quick Settings tile and notification action (1.0)
- [x] Refuses to be set as the Always-on VPN (1.1)
- [x] Donate button, check for updates (1.2)
- [x] GPL-3.0-or-later, source code link in the app (1.3)
- [x] Clear messages when a flip fails; the tile shows whether the tunnel is up (1.4)
- [x] Keep name lookups working when the phone changes network while the tunnel
      is up (for example leaving Wi-Fi for mobile data) (1.5)
- [x] Warn before SocketFlip's tunnel replaces another VPN app (1.5)

## Next: reliability

- [ ] Self-check screen: every permission and setting SocketFlip needs, with a
      button to fix each one and a "copy report" button for bug reports
- [ ] Ask to be exempted from battery optimisation on phones known to close
      background apps (Samsung, Xiaomi, OnePlus and others)

## Then: settings and looks

- [ ] Settings page
- [ ] Adjustable cooldown between taps
- [ ] Vibration and hint messages can be turned off
- [ ] Button shows whether the tunnel is up, with a ring counting down the cooldown
- [ ] Full colour picker for the button, the tunnel-up colour, the cooldown ring
      and the icon
- [ ] Opacity sliders (resting and just tapped) and a size slider
- [ ] Live preview, presets, and reset to default
- [ ] Snap the button to the screen edge

## Later

- [ ] Only show the button while a target app is on screen (optional, needs
      Usage Access)
- [ ] Take the tunnel down when you leave the target apps, so the VPN key does not
      linger
- [ ] More than one target app: tick as many as you like and each tap reconnects
      all of them
- [ ] Each target app can have its own cooldown, with Cooldown and Remove buttons
      right in the list
- [ ] Let automation apps (Tasker, MacroDroid, Key Mapper) trigger a flip, off by
      default
- [ ] Home screen shortcuts
- [ ] A cleaner look: the main screen and Settings laid out in cards
- [ ] Skins: a row of ready-made looks for the button, one tap to use
- [ ] Option to mirror the arrow so it turns the other way
- [ ] Optional emergency restart: a red ! during the cooldown restarts a stuck app,
      after asking
- [ ] Translations (help welcome: the text is all in one strings file)
- [ ] Export and import settings, so a reinstall or a new phone keeps your setup
- [ ] Reproducible builds, so anyone can check the APK matches the source

## Not planned

- **An internet permission.** SocketFlip has none and never checks online by
  itself. That stays.
- **Analytics or telemetry**, opt-in or otherwise.
- **Accessibility-service triggers.** Too much access for what they would add.
- **Resetting UDP traffic** (calls, voice chat, QUIC). Android does not close UDP
  sockets when a VPN changes, and there is no clean way to do it.
