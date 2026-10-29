# Changelog

Every released version of the MaraLyrics Android app (`com.maralyrics.laitei`), newest first, with its full commit log. Versions follow [semantic versioning](https://semver.org) and each one is a git tag (`vX.Y.Z`) on the commit it names; the website has its own versions at [laiteichozah/maralyrics](https://github.com/laiteichozah/maralyrics/releases).
This file is generated: edit `scripts/releases.json` and run `node scripts/changelog.mjs`. A version's log lists every commit since the previous version; merge commits are counted, not listed.

## v1.3.0 — Artists, accounts and sync (2026-10-29)

Sign in with your MaraLyrics account (or Google) so favorites, theme and language follow you across devices, an Artists & Composers directory, badges and the Green mark on profiles, event reminders, and database upgrades that can no longer wipe a reader's data.

**Highlights**
- Accounts: Settings > Account signs in to the same account as maralyrics.com with email or username and password, or with Continue with Google through Android's own account chooser. Creating an account and resetting a password open the website's account page inside the app (only maralyrics.com pages load there)
- Sync: signed in, favorites, theme and language follow the reader to every device and to maralyrics.com through the website's own sync (newest change wins, removals reach other devices, offline changes are kept and sent later; signing in adds the favorites already on the phone instead of replacing them)
- The session token is encrypted with a key held in the Android Keystore, kept out of Android backups, redacted from network logs and sent only to the account endpoints; a server that stops accepting it signs that device out, being offline never does
- Artist and composer profiles show recognition badges, the Green tick and formatted bios, saved for offline once seen. Buying a Green mark is deliberately not offered in the app
- A new Artists & Composers directory (Home > Artists): search that ignores case and accents, an All / Artists / Composers filter and sorting by name, number of songs or badges; it works offline
- The Credits screen also lists the website's sponsors, partners and editors, saved for offline
- Opt-in event reminders (Settings > Event reminders, off by default): one notification at about 8:00 on days with a community event, on its own notification channel
- Safe database upgrades: opening the database can no longer fall back to wiping favorites, recently viewed songs and queued feedback; every version from 1 to 10 has an explicit, tested migration, and the 1 to 2 step that failed on any database with two or more songs is fixed
- Burmese: Song of the Day, Today's Pick, All and the two name sorts, copied from the website's own translations. The other new screens still show in English until they are translated
- Song screen: the position pill moves beside Previous / Next so a long title or a large text size can no longer run over it

**Deploy notes:** versionCode 8; the Room schema stays at version 10 (migrations 1 to 10 are covered by tests). Build with `./gradlew bundleRelease` (needs `keystore.properties`, which is not in git). Before relying on it for readers: (1) register the app, `com.maralyrics.laitei` with the upload key's and the Play App Signing SHA-1, as Android OAuth clients in the same Google Cloud project as the website's web client, or Continue with Google answers that the app is not set up and readers use email and password; (2) add the Play App Signing SHA-256 to the website's `assetlinks.json` so song links open in the app; (3) update the Play Console Data safety form (email address and synced favorites when signed in, Google sign-in) and make the in-app privacy text match the website's policy.

13 commits (0 merge commits not listed) · 1 author · 83 files changed, 6810 insertions(+), 571 deletions(-)

<details><summary>Full log — 13 commits</summary>

- `db8c9e4` 2026-10-28 Make database upgrades safe: no silent wipe, full migration tests — Laitei40
- `15e6f8d` 2026-10-28 Show badges, the Green mark and rich bios on artist and composer profiles — Laitei40
- `eed7961` 2026-10-28 Add an Artists & Composers directory — Laitei40
- `8b76cd4` 2026-10-28 Show sponsors, partners and editors on the Credits screen — Laitei40
- `414525a` 2026-10-28 Add opt-in "event today" reminders for the community calendar — Laitei40
- `0c31e90` 2026-10-28 Test the event reminder notification on a device — Laitei40
- `3dd36ac` 2026-10-28 Version 1.3.0 and the translations the website already has — Laitei40
- `044bc5f` 2026-10-28 Polish the directory and move the song position pill — Laitei40
- `4b2043a` 2026-10-29 Add reader accounts: sign in with your MaraLyrics account — Laitei40
- `a857ec4` 2026-10-29 Sync favorites, theme and language across a reader's devices — Laitei40
- `4dd6e71` 2026-10-29 Make the website's sign-in page usable inside the app — Laitei40
- `69546d4` 2026-10-29 Add "Continue with Google" to the sign-in screen — Laitei40
- `d6feee9` 2026-10-29 Tell the reader when Google turns the app down, instead of staying silent — Laitei40

</details>

## v1.2.0 — Articles, Mara Idol and a calendar (2026-10-27)

The website's articles, Mara Idol and community calendar in the app, a Song of the Day, new reading controls on the song screen and song links that open in the app.

**Highlights**
- Home gains a Song of the Day card, with shortcuts to Articles, Mara Idol and the Community Calendar
- Articles & News with article pages; Mara Idol seasons, contestants and videos; a Community Calendar with year navigation and countdowns. They read from the same API as maralyrics.com and are saved on the phone, so they open offline once seen (network first, saved copy as fallback)
- Song screen: tap the lyrics for floating Previous / Next (following the list the song was opened from), rotate, fullscreen and favorite; zoom in / out / reset buttons and pinch-to-zoom, kept from song to song
- Share sends only the song's maralyrics.com link
- maralyrics.com/song/{slug} opens that song in the app (an Android App Link), with a notice when the song is not in the library yet
- Sharing the app and songs with nearby phones was removed on purpose, together with its Settings and Setup screens
- Build: keystore.properties, *.jks and *.keystore are ignored, signed release outputs (app/release/) are no longer tracked, and R8 resource shrinking is on

**Deploy notes:** R8 resource shrinking is on, so build a signed release (`./gradlew bundleRelease`, needs `keystore.properties`, which is not in git) and try it on a phone before uploading it to Google Play. Opening maralyrics.com/song links automatically in the Play-installed app needs the Play App Signing SHA-256 from Play Console in the website's `/.well-known/assetlinks.json`.

3 commits (0 merge commits not listed) · 1 author · 83 files changed, 5536 insertions(+), 1741 deletions(-)

<details><summary>Full log — 3 commits</summary>

- `e948e2f` 2026-10-27 Remove nearby sharing and other earlier changes — Laitei40
- `5d61d92` 2026-10-27 Add articles, Mara Idol, calendar, Song of the Day, reader controls and song links — Laitei40
- `4709d32` 2026-10-27 Stop tracking release build outputs — Laitei40

</details>

## v1.1.0 — Widgets and background sync (2026-09-05)

Two home screen widgets, new-song notifications, quiet background updates and a refreshed tutorial.

**Highlights**
- Two home screen widgets: Search Mara Lyrics instantly, or a Featured Lyrics card with a random song from your library (tap to shuffle or open it)
- A notification when new songs are added and ready to read offline
- Small lyric corrections and new songs sync quietly in the background, with no more prompts asking to download
- Sharing the app and downloaded songs with nearby phones without internet (removed again in 1.2.0)
- A refreshed onboarding tutorial with clearer screens
- Fixed: search finds exact title and number matches; the database size display, offline error messages, small-screen layouts and update prompts

2 commits (0 merge commits not listed) · 1 author · 44 files changed, 1758 insertions(+), 581 deletions(-)

<details><summary>Full log — 2 commits</summary>

- `3f00888` 2026-08-25 Added offline sharing features. — Laitei40
- `5f3c463` 2026-09-05 ✨ New: Two home screen widgets — Search Mara Lyrics instantly, or see a Featured Lyrics card with a random song from your library (tap to shuffle or open it). 🔔 New: Get notified when new songs are added and ready to read offline. 📡 Small lyric corrections and new songs now sync quietly in the background — no more interruptions asking to download. 📴 New: Share the app and your downloaded songs directly with nearby phones — no internet needed on either device. 🎨 Refreshed onboarding tutorial with clearer, more polished screens. 🔍 Fixed: search now reliably finds exact title/number matches. 🐛 Various fixes: database size display, offline error messages, small-screen layout issues, and smoother update prompts. — Laitei40

</details>

## v1.0.3 — Search and language fixes (2026-08-24)

Search and language switching fixed, a refreshed look and compatibility updates. Version control goes straight from 1.0.0 to 1.0.3, so 1.0.1 and 1.0.2 have no tags of their own.

**Highlights**
- Search reliably finds matching songs by title, lyrics or number
- Language switching fixed: Mara and Burmese display correctly everywhere, including on first launch
- The app's visual design refreshed for a cleaner, more consistent look, plus various wording improvements
- Under-the-hood updates for compatibility with the latest Android versions

3 commits (0 merge commits not listed) · 1 author · 126 files changed, 15127 insertions(+), 10338 deletions(-)

<details><summary>Full log — 3 commits</summary>

- `2afb717` 2026-08-24 Improved features. — Laitei40
- `58757db` 2026-08-24 Improved features. — Laitei40
- `629bf4f` 2026-08-24 • Fixed search so it reliably finds matching songs by title, lyrics, or number • Fixed language switching — Mara and Burmese now display correctly everywhere, including on first launch • Refreshed the app's visual design for a cleaner, more consistent look • Various text and wording improvements • Under-the-hood updates for compatibility with the latest Android versions — Laitei40

</details>

## v1.0.0 — First release (2026-06-24)

The first MaraLyrics Android app: browse and read Mara lyrics, keep favorites and see who is behind each song, in English, Mara and Burmese.

**Highlights**
- Home, category, favorites and song screens for reading lyrics, with artist and composer profiles
- Onboarding and setup screens for a first start
- Mara and Burmese translations next to English, with Transifex set up for translators, and a fix so Burmese can be chosen
- A dynamic, localized Credits screen, an FAQ link in Settings and the About text in all three languages

16 commits (0 merge commits not listed) · 1 author · 141 files changed, 14547 insertions(+)

<details><summary>Full log — 16 commits</summary>

- `6d93025` 2026-06-17 Created app. — Laitei40
- `a4d964f` 2026-06-17 Updated Home Screen. — Laitei40
- `ed935f9` 2026-06-20 Improved Features. — Laitei40
- `8c42188` 2026-06-20 Improved Features. — Laitei40
- `100d3a2` 2026-06-22 Update Credits screen to be dynamic and localized — Laitei40
- `449405d` 2026-06-23 Update FAQ URL in Settings — Laitei40
- `f868559` 2026-06-23 Fix ReportBottomSheet localization by explicitly providing LocalContext — Laitei40
- `674067a` 2026-06-24 Update Credits screen to be dynamic and localized — Laitei40
- `d3a21c3` 2026-06-24 Fix Burmese language selection by adding 'my' to resConfigs and simplifying locale logic — Laitei40
- `bba45ba` 2026-06-24 Update transifex.yml with correct filter configuration — Laitei40
- `034770d` 2026-06-24 Fix duplicate and incorrect string resources in values-my/strings.xml — Laitei40
- `7ff76e3` 2026-06-24 Translate 'Mara Lyrics' to 'မရာ သီချင်းစာသား' in Burmese strings — Laitei40
- `abb503c` 2026-06-24 Replace 'မာရာ' with 'မရာ' in Burmese translations — Laitei40
- `a57e117` 2026-06-24 Complete missing Burmese translations — Laitei40
- `1efb24b` 2026-06-24 Update all missing Burmese translations — Laitei40
- `ab506a4` 2026-06-24 Update About App description in English, Mara, and Burmese — Laitei40

</details>
