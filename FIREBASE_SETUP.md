# MHOP Songs — Firebase Phase 2 Integration & First-Administrator Setup

This project currently runs in **Phase 1 (Local Offline Architecture)** using Room, DataStore, and a Firebase-ready repository layer (`SongRepository`, `PreferencesRepository`, `AuthRepository`).

When you are ready to enable **Phase 2 (Shared Church Cloud Sync & Google Sign-In Administration)**, follow these steps:

## 1. Provision Firebase in the Project
1. Create or select a Firebase project in the Firebase Console (or via AI Studio Firebase provisioning).
2. Enable **Authentication** -> **Sign-in method** -> **Google**.
3. Create a **Cloud Firestore** database in production mode.
4. Register the Android package `com.aistudio.mhopsongs.kxmpzr` (with your debug/release SHA-1 and SHA-256 certificates) and place `google-services.json` inside `app/`.
5. Uncomment the Firebase, Firestore, Auth, and Credential Manager dependencies in `app/build.gradle.kts`.

## 2. Deploy Security Rules
Deploy the included `firestore.rules` file at the root of this repository:
```bash
firebase deploy --only firestore:rules
```
These rules guarantee:
- Anyone in the community can read songs where `isPublished == true`.
- Only users whose `users/{uid}` document has `role == "admin"` can view drafts, create/edit/publish/delete songs, or read `import_history`.
- Users cannot self-assign or elevate themselves to `role == "admin"`.

## 3. Create the First Authorized Administrator (Console Procedure)
Because public admin self-registration is insecure and disabled by design:
1. Have the primary church administrator sign in with Google in the app (or retrieve their UID from **Firebase Console -> Authentication -> Users**).
2. Open **Firebase Console -> Firestore Database**.
3. Create (or open) the **`users`** collection.
4. Add a document whose **Document ID** is the administrator's exact **Authentication UID**.
5. Add these fields to that document:
   - `uid` (string): `<their Authentication UID>`
   - `displayName` (string): `<Administrator Name>`
   - `email` (string): `<Administrator Email>`
   - `role` (string): `admin`
6. Save the document. Once saved, signing in with that Google account unlocks the Admin Dashboard and song publishing controls. Additional administrators can be granted the `admin` role in the same way.
