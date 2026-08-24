# Tasks

- [x] Modify Data Layer for Multiple Contributors
    - [x] Update `Dtos.kt` to support new API fields
    - [x] Update `Models.kt` to include contributor lists in `Song`
    - [x] Update `Daos.kt` to fetch artists/composers via Junctions
    - [x] Update `RepositoryImpls.kt` mapping logic
- [x] Update UI Components
    - [x] Update `SongListContent.kt` for "Various" labeling in lists
    - [x] Update `SongDetailScreen.kt` for "Various" chips and Contributor Popup
    - [x] Ensure Copyright Owner name is displayed at the bottom of `SongDetailScreen.kt`
- [/] Verification & Robustness
    - [x] Build and verify successful compilation
    - [x] Add `copyright_owner_name` fallback to `SongEntity` (DB Version 10)
    - [ ] Manual verification of UI changes
