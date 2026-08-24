# Implementation Plan - Multiple Contributors and UI Refinements

This plan addresses the missing Composer and Copyright Owner data following an API upgrade and implements the "Various Artists/Composers" feature with a selection popup.

## User Review Required

> [!IMPORTANT]
> The app requires a data refresh (Clear Data/Sync) after these changes to correctly map the new API structure to the local database.

## Proposed Changes

### Data Layer

#### [Dtos.kt](file:///C:/Users/Laitei/AndroidStudioProjects/MaraLyrics/app/src/main/java/com/maralyrics/laitei/data/remote/dto/Dtos.kt)
- Update `SongDto` to include singular `artist`, `composer`, and `copyright_owner` objects to support the upgraded API.

#### [Models.kt](file:///C:/Users/Laitei/AndroidStudioProjects/MaraLyrics/app/src/main/java/com/maralyrics/laitei/domain/model/Models.kt)
- Add `artists` and `composers` lists (as `SongContributor` objects) to the `Song` domain model.

#### [Daos.kt](file:///C:/Users/Laitei/AndroidStudioProjects/MaraLyrics/app/src/main/java/com/maralyrics/laitei/data/local/dao/Daos.kt)
- Update `SongWithArtistAndComposer` to use `@Relation` with `Junction` to fetch all linked artists and composers.

#### [RepositoryImpls.kt](file:///C:/Users/Laitei/AndroidStudioProjects/MaraLyrics/app/src/main/java/com/maralyrics/laitei/data/repository/RepositoryImpls.kt)
- Update `toEntity()` to extract IDs and names from the new singular objects.
- Update `toDomain()` to populate the new list fields in the `Song` model.

---

### Presentation Layer

#### [SongListContent.kt](file:///C:/Users/Laitei/AndroidStudioProjects/MaraLyrics/app/src/main/java/com/maralyrics/laitei/presentation/common/components/SongListContent.kt)
- Update `SongRow` subtitle logic to show "Various Artists" or "Various Composers" when multiple contributors exist.

#### [SongDetailScreen.kt](file:///C:/Users/Laitei/AndroidStudioProjects/MaraLyrics/app/src/main/java/com/maralyrics/laitei/presentation/song_detail/SongDetailScreen.kt)
- Implement `ModalBottomSheet` popup to show the list of contributors when a "Various" chip is clicked.
- Ensure the Copyright Owner name is displayed at the bottom of the view.

## Verification Plan

### Manual Verification
- Deploy to a device.
- Perform a "Clear Data" and "Sync" to fetch latest data.
- Verify "Various Artists" appears in song lists for relevant songs.
- Verify clicking a "Various" chip in Song Detail opens the bottom sheet with clickable names.
- Verify Copyright Owner is visible at the bottom of the lyrics section.
