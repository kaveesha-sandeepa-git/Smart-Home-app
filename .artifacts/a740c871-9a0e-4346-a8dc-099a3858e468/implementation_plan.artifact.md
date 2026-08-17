# Implementation Plan: Fix Firebase Data Sync and Mapping Issues

The user reported that Firebase data does not show correctly in the app. My investigation revealed several issues in the data synchronization and mapping layer:

1.  **Data Loss during Deserialization**: The `FirebaseSyncService` uses the base `Device` class to fetch all devices. Since specialized devices (Light, Iron, etc.) have fields not present in `Device`, this data is lost during deserialization.
2.  **Missing ID Mapping**: For Floors and Usage Reports, the service doesn't populate the ID field from the Firebase snapshot key if it's missing in the payload. This leads to empty primary keys in the local Room database.
3.  **Inconsistent Database Sync**: While devices are correctly "replaced" in the local database during sync, floors and reports are only added, meaning deletions on Firebase are not reflected locally.
4.  **Redundant Table Structure**: The use of separate tables for each device type (lights, irons, etc.) while querying from a common `devices` table leads to data fragmentation.

## Proposed Changes

### 1. Data Models [Core]

#### [MODIFY] [Device.kt](file:///C:/Users/MSI-G/AndroidStudioProjects/Smart-Home-app/app/src/main/java/com/example/smart_home/models/Device.kt)
- Add all specific fields from subclasses (`Light`, `Iron`, `Outlet`, `SecurityCamera`, `MultiSwitch`) to the base `Device` class.
- This ensures that `snapshot.getValue(Device::class.java)` captures all data from Firebase.

### 2. Database Layer [Persistence]

#### [MODIFY] [FloorDao.kt](file:///C:/Users/MSI-G/AndroidStudioProjects/Smart-Home-app/app/src/main/java/com/example/smart_home/database/FloorDao.kt)
- Add `replaceAll(floors: List<Floor>)` to handle authoritative sync.

#### [MODIFY] [DeviceUsageReportDao.kt](file:///C:/Users/MSI-G/AndroidStudioProjects/Smart-Home-app/app/src/main/java/com/example/smart_home/database/DeviceUsageReportDao.kt)
- Add `replaceAll(reports: List<DeviceUsageReport>)` to handle authoritative sync.

### 3. Services [Sync Logic]

#### [MODIFY] [FirebaseSyncService.kt](file:///C:/Users/MSI-G/AndroidStudioProjects/Smart-Home-app/app/src/main/java/com/example/smart_home/services/FirebaseSyncService.kt)
- Update `syncFloorsFromFirebase` and `syncReportsFromFirebase` to populate IDs from snapshot keys.
- Update `readDevices` to ensure all fields are correctly trimmed/normalized.
- Use `replaceAll` for floors and reports to ensure deleted items are removed locally.

## Verification Plan

### Automated Tests
- Build the project to ensure `Device.kt` changes don't break subclasses.
- (Manual) Verify logs show successful sync and ID mapping.

### Manual Verification
1.  Check the Dashboard to see if all devices from Firebase are listed.
2.  Open a Light device to see if its brightness and other details are correctly populated (after adding fields to `Device`).
3.  Add/Delete a floor on Firebase and verify it reflects in the app's floor spinner.
