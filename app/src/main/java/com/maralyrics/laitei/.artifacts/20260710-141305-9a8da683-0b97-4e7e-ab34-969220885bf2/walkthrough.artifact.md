# Walkthrough - Multiple Contributors & UI Refinements

I have implemented support for "Various Artists" and "Various Composers" across the app, including a new interaction model for songs with multiple contributors. I also fixed data mapping issues for the upgraded API.

## 1. Multi-Contributor Support
**Feature:** Songs with multiple artists or composers now display as "Various Artists" or "Various Composers" in lists and on the song detail page.
- **Popup Selection:** Clicking a "Various" chip on the [SongDetailScreen.kt](file:///C:/Users/Laitei/AndroidStudioProjects/MaraLyrics/app/src/main/java/com/maralyrics/laitei/presentation/song_detail/SongDetailScreen.kt) opens a bottom sheet listing all contributors. Each name is clickable and navigates to the respective profile page.
- **Data Layer:** Updated [Daos.kt](file:///C:/Users/Laitei/AndroidStudioProjects/MaraLyrics/app/src/main/java/com/maralyrics/laitei/data/local/dao/Daos.kt) to fetch multiple contributors using Room's junction tables.

## 2. API Upgrade Compatibility
**Fix:** Updated the data mapping to correctly handle the new singular object structure (`artist`, `composer`, `copyright_owner`) returned by the upgraded API.
- Updated [Dtos.kt](file:///C:/Users/Laitei/AndroidStudioProjects/MaraLyrics/app/src/main/java/com/maralyrics/laitei/data/remote/dto/Dtos.kt) and [RepositoryImpls.kt](file:///C:/Users/Laitei/AndroidStudioProjects/MaraLyrics/app/src/main/java/com/maralyrics/laitei/data/repository/RepositoryImpls.kt) to ensure IDs and metadata are correctly extracted.
- **Copyright Display:** Confirmed that the Copyright Owner's name is now correctly displayed at the bottom of the lyrics section.

## 3. UI Enhancements
- **Song List:** The subtitle in [SongRow](file:///C:/Users/Laitei/AndroidStudioProjects/MaraLyrics/app/src/main/java/com/maralyrics/laitei/presentation/common/components/SongListContent.kt) now dynamically builds its text to include "Various" labels and the Copyright Owner when available.
- **Navigation:** Seamless navigation from the contributor popup to artist and composer profile pages.

## Verification Summary
- **Compilation:** Project builds successfully.
- **Data Integrity:** Mapping logic covers legacy fields, new singular objects, and plural relationship lists.
- **UI Logic:** Verified the conditional "Various" labeling and the `ModalBottomSheet` logic.

> [!IMPORTANT]
> Please perform a **Sync Now** or **Clear Data** in the app settings to repopulate your local database with the updated API data structure.
