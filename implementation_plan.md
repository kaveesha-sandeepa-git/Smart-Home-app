# Firebase Authentication Integration Plan

This plan details how we will migrate from the current simple local authentication (using `SharedPreferences`) to a real, secure **Firebase Authentication** flow using Email and Password. 

## User Review Required
> [!IMPORTANT]
> - **Dependency Additions**: This will require adding the `firebase-auth` dependency to your Gradle configuration and version catalog. 
> - **Current Accounts**: Since we're moving to Firebase, the local "mock" accounts will no longer work. You will need to either create users in your Firebase Console or allow users to register through the app.

## Open Questions
> [!WARNING]
> 1. Currently, the `LoginActivity` only has a "Login" button. Should we add a **"Register/Sign Up"** button so new users can create an account directly in the app, or will you manage all user creation via the Firebase Console?
> 2. Do you want to show a loading spinner while Firebase is processing the sign-in request?

## Proposed Changes

### 1. Build Dependencies
- **[MODIFY]** `gradle/libs.versions.toml`: 
  - Add `firebaseAuth` version.
  - Add `firebase-auth` library definition.
- **[MODIFY]** `app/build.gradle.kts`:
  - Add `implementation(libs.firebase.auth)` dependency.

### 2. Login Activity Updates
- **[MODIFY]** `app/src/main/res/layout/activity_login.xml`:
  - (Optional based on your feedback) Add a "Register" button below the "Login" button.
- **[MODIFY]** `app/src/main/java/com/example/smart_home/activities/LoginActivity.kt`:
  - Remove `SharedPreferences` logic.
  - Initialize `FirebaseAuth.getInstance()`.
  - On create, check `auth.currentUser != null` for auto-login.
  - Update the Login button click listener to call `auth.signInWithEmailAndPassword(email, password)`.
  - Handle success (navigate to `MainActivity`) and failure (show error Toast).

### 3. Dashboard / Main Activity Updates
- **[MODIFY]** `app/src/main/java/com/example/smart_home/activities/MainActivity.kt`:
  - Update the `showLogoutDialog` method.
  - Remove `SharedPreferences` logout logic.
  - Call `FirebaseAuth.getInstance().signOut()`.
  - Navigate back to `LoginActivity`.

## Verification Plan

### Manual Verification
- Run a Gradle sync to ensure the `firebase-auth` dependency is resolved.
- Build and run the application.
- Attempt to log in with an invalid account to verify error handling.
- Attempt to log in with a valid Firebase account (or register if we add that feature) and ensure redirection to the Dashboard.
- Click the profile icon on the Dashboard and sign out.
- Verify that restarting the app persists the login state appropriately.
