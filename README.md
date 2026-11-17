# DIGITAL KABARIYA — Pakistan-First Digital Scrap & Recycling Ecosystem

> **"Sell Scrap. Get Paid. Recycle Better."**
> **"کباڑ بیچیں • رقم کمائیں • ماحول بچائیں"**

Digital Kabariya connects Households, Shops, Offices, Schools, Restaurants, Factories, and Construction Sites with Verified Collectors (*Kabariyas*), Collection Hubs, Sorting Centers, Recyclers, and Corporate Buyers across Rawalpindi, Islamabad, Lahore, Karachi, and Faisalabad.

---

## 🌐 Live Web / PWA Multi-Portal App (GitHub Pages)

The root [`index.html`](./index.html) provides an immediately usable, mobile-responsive **Customer Web/PWA** + **13-Role Operations Ecosystem** compatible with GitHub Pages (`main / (root)`):

1. **Customer Web/PWA** — 18-step doorstep pickup booking, **AI Advisory Estimate** (final payout strictly follows verified physical weight & inspection), Live PKR Rates, Wallet & Easypaisa/JazzCash/Raast Cashouts, OTP Verification, Digital Tax Receipt, Saved Addresses & Rewards.
2. **Collector App** — Job Radar, Accept/Arrive workflow, Customer 4-Digit OTP check, Digital Scale Weight capture, Commission calculation.
3. **Collection Hub** — Inbound receiving, weighbridge variance check (`Gross vs Net kg`), grade assignment, batch creation (`BATCH-HUB-*`).
4. **Recycler / B2B Marketplace** — Wholesale scrap lots, sealed bidding, fixed-price Buy Now, B2B invoices.
5. **Corporate Client Portal** — Multi-branch ESG tracking, waste diverted, CO2 & tree-equivalence certificates.
6. **Admin Command**, **Finance & Double-Entry Ledger**, **Operations Dispatch**, **Warehouse Inventory**, **Pricing Guardrails Engine**, **Customer Support Tickets**, **Cities & Franchise Network**, and **Super Admin RBAC & Audit Logs**.

### Deploying to GitHub Pages
1. Push this repository to GitHub.
2. Open **Settings → Pages → Build and deployment → Source: Deploy from a branch (`main`, `/ (root)`)**.
3. Save — `.nojekyll` and `index.html` ensure GitHub Pages serves the full interactive web ecosystem immediately.

---

## 📱 Android Native App (Kotlin + Jetpack Compose + Firebase)

- **Package / Application ID**: `com.aistudio.digitalkabariya.pkecos`
- **Architecture**: MVVM + Repository Pattern (`Material Design 3`, Bilingual English / اردو UI, Edge-to-Edge Compose)
- **Backend & Security**:
  - **Firebase Authentication** (Google Sign-In via Jetpack `CredentialManager`)
  - **Cloud Firestore** with zero-trust owner-scoped `firestore.rules`
  - **Firebase App Check** (`RecaptchaEnterpriseProviderFactory` / `DebugAppCheckProviderFactory`)
  - **Firebase AI Logic (`Firebase.ai`)** for Gemini Vision scrap classification & bilingual support assistant

### Building the Android App Locally
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```
*(In AI Studio cloud containers, run `gradle :app:testDebugUnitTest` directly.)*
