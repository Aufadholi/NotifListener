# Fix ContactRepository and Consolidate into MainActivity

The goal is to fix the compilation error in `ContactRepository.kt` (specifically the `deleteContact` return type) and consolidate the repository logic into `MainActivity.kt` as requested ("only use in mainactivity").

## User Review Required

> [!IMPORTANT]
> This plan moves `IContactRepository`, `ContactRepository`, `AddResult`, and `UpdateResult` into `MainActivity.kt`. This will consolidate the code into one file, making it easier to manage if it's only intended for use within the `MainActivity` scope.

## Proposed Changes

### Repository & UI

#### [MODIFY] [MainActivity.kt](file:///D:/Project/Android Native/app/src/main/java/com/example/wanotification/uiux/MainActivity.kt)
- Append `IContactRepository` and `ContactRepository` implementation.
- Append `AddResult` and `UpdateResult` enums.
- Fix the `deleteContact` return type to `Unit`.

#### [MODIFY] [ContactViewModel.kt](file:///D:/Project/Android Native/app/src/main/java/com/example/wanotification/viewmodel/ContactViewModel.kt)
- Update imports to use the new location of `AddResult`, `UpdateResult`, and `IContactRepository`.

#### [MODIFY] [AppContainer.kt](file:///D:/Project/Android Native/app/src/main/java/com/example/wanotification/di/AppContainer.kt)
- Update imports to use the new location of `IContactRepository` and `ContactRepository`.

#### [DELETE] [ContactRepository.kt](file:///D:/Project/Android Native/app/src/main/java/com/example/wanotification/repository/ContactRepository.kt)
- Remove the separate file as its contents are now in `MainActivity.kt`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to ensure the project still compiles after the move and fix.

### Manual Verification
- Deploy the app and verify that adding, updating, and deleting contacts still works as expected in the UI.
