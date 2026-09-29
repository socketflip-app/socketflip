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
- [x] Self-check screen: every permission and setting SocketFlip needs, with a
      button to fix each one and a "copy report" button for bug reports (1.6)
- [x] Ask to be exempted from battery optimisation on phones known to close
      background apps (Samsung, Xiaomi, OnePlus and others) (1.6)
- [x] Settings page (1.7)
- [x] Adjustable cooldown between taps (1.7)
- [x] Vibration and hint messages can be turned off (1.7)
- [x] Button shows whether the tunnel is up, with a ring counting down the cooldown (1.7)
- [x] Full colour picker for the button, the tunnel-up colour, the cooldown ring
      and the icon (1.8)
- [x] Opacity sliders (resting and just tapped) and a size slider (1.8)
- [x] Live preview, presets, and reset to default (1.8)
- [x] Snap the button to the screen edge (1.8)
- [x] Only show the button while a target app is on screen (optional, needs
      Usage Access) (1.9)
- [x] Take the tunnel down when you leave the target apps, so the VPN key does not
      linger (1.9)

## Later

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
- [ ] Home screen shortcut that shows the button and opens your app in one tap
- [ ] An easier first run: numbered steps that carry on by themselves, and a plain
      explanation before Android's VPN warning
- [ ] App picker with icons, search, and games listed first
- [ ] Errors leave a notification that opens the setup check, which can open a
      prefilled bug report
- [ ] Adaptive launcher icon with a themed (monochrome) version
- [ ] Send SocketFlip to a friend: the app itself, phone to phone, or the link
- [ ] Your numbers: reconnects in total and this week, and restarts used, with a
      Share button (counted on the phone only)
- [ ] A note in the app when your copy is more than 30 days old
- [ ] A quicker emergency restart question: two big buttons, Restart and Cancel
- [ ] Clear "not yet" feedback for a tap the cooldown ignores
- [ ] Settings kept in your phone's own backup, so a reinstall or a new phone keeps
      your setup
- [ ] Setup check row for the Xiaomi, Redmi and POCO permission emergency restart needs
- [ ] Accessibility: TalkBack names and values for every setting and for the
      button's state, and text that is easier to read in light mode
- [ ] Settings lead with the cooldown; resetting the look can be undone
- [ ] A 48 dp touch area around small buttons, and layouts that fit large text,
      tablets and landscape
- [ ] Ready for translators: every piece of text in one file, and setup check
      reports that stay in English
- [ ] Built with current Android tools, targets Android 16
- [ ] Translations, first German, Brazilian Portuguese, Spanish, French and
      Russian (help welcome: the text is all in one strings file)
- [ ] Reproducible builds, so anyone can check the APK matches the source

## Not planned

- **An internet permission.** SocketFlip has none and never checks online by
  itself. That stays.
- **Analytics or telemetry**, opt-in or otherwise.
- **Accessibility-service triggers.** Too much access for what they would add.
- **Resetting UDP traffic** (calls, voice chat, QUIC). Android does not close UDP
  sockets when a VPN changes, and there is no clean way to do it.
