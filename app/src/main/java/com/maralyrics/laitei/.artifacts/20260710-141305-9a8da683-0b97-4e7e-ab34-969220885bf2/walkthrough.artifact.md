# Walkthrough - Multiple Contributors & UI Refinements

I have implemented support for "Various Artists" and "Various Composers" across the app, including a new interaction model for songs with multiple contributors. I also fixed data mapping issues for the upgraded API.

## 1. Multi-Contributor Support
**Feature:** Songs with multiple artists or composers now display as "Various Artists" or "Various Composers" in lists and on the song detail page.
- **Popup Selection:** Clicking a "Various" chip on the [SongDetailScreen.kt](file:///C:/Users/Laitei/AndroidStudioProjects/MaraLyrics/app/src/main/java/com/maralyrics/laitei/presentation/song_detail/SongDetailScreen.kt) opens a bottom sheet listing all contributors. Each name is clickable and navigates to the respective profile page.
- **Data Layer:** Updated [Daos.kt](file:///C:/Users/Laitei/AndroidStudioProjects/MaraLyrics/app/src/main/java/com/maralyrics/laitei/data/local/dao/Daos.kt) to fetch multiple contributors using Room's junction tables.

## 2. API Upgrade Compatibility & Copyright Display
**Fix:** Updated the data mapping to correctly handle the new singular object structure (`artist`, `composer`, `copyright_owner`) returned by the upgraded API.
- **Robustness:** Added a `copyright_owner_name` column to the `songs` table (Database Version 10) to ensure the copyright notice displays even before a full table join is possible.
- **Display:** The Copyright notice is now prominently displayed at the bottom of the lyrics section using the `Copyright © Owner Name` format (localized).
- **Views Count:** Added a dedicated "Views" chip to the song detail metadata section.

## 3. UI Enhancements
- **Song List:** The subtitle in [SongRow](file:///C:/Users/Laitei/AndroidStudioProjects/MaraLyrics/app/src/main/java/com/maralyrics/laitei/presentation/common/components/SongListContent.kt) now includes the Copyright Owner name alongside artists and composers.
- **Popup UI:** Cleaned up the `ModalBottomSheet` implementation for better selection of individual contributors from "Various" groups.

## Verification Summary
- **Database Migration:** Successfully added MIGRATION_9_10 to handle the new copyright column.
- **Compilation:** Project builds successfully.
- **UI Logic:** Verified the conditional "Various" labeling and the popup selection flow.

> [!IMPORTANT]
> Please perform a **Sync Now** or **Clear Data** in the app settings to repopulate your local database with the updated API data structure.
