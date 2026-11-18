# Collection Hub

> **"Your Scrap. Your Collector. Your Collection Hub."**

A scrap collection marketplace connecting households and shops in **Rawalpindi and Islamabad, Pakistan** with nearby registered *kabari* shops, scrap dealers, and independent collectors.

## Portals
- **Public Home (`/`)**: Overview, How It Works, 12 Scrap Categories (indicative rates only), Collector Registration CTA, FAQ, Support Contact. Admin portal link is never displayed publicly.
- **Customer Portal (`/customer`)**: 5-step multi-category pickup booking, Android Photo Picker attachment, live booking lifecycle, 4-digit OTP confirmation, verified weight & manual payout confirmation, notifications, and complaints.
- **Collector Portal (`/collector`)**: Application & admin approval gate, Online/Offline switch, privacy-preserving Open Pickup Pool (approximate area only before acceptance), atomic first-accept lock, active pickup workflow, and commission settlement ledger.
- **Private Admin Portal (`/hub-admin`)**: Unlisted administrative operations portal protected by verified admin authentication and zero-trust Firestore Security Rules.

## Architecture & Honest Runtime Notes
- **Android Native App**: Kotlin + Jetpack Compose + Material 3 (`#FFFFFF` clean white, `#14532D` dark green, `#DCFCE7` light green), Firebase Authentication (Google Sign-In via Jetpack `CredentialManager`), Cloud Firestore (`firestore.rules` + emulator tests), and Firebase App Check.
- **Web Preview**: Root `index.html` + `manifest.webmanifest` + `sw.js` provide a responsive PWA workflow preview compatible with GitHub Pages.
- **Build & Test**:
  ```bash
  gradle :app:testDebugUnitTest
  gradle :app:assembleDebug
  ```
