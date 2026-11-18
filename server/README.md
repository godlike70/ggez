# GGEZ purchase & authorization service (first backend milestone)

This service implements manual purchase claims, Google account identity, an owner-only pending-request console, and server-side entitlement records. It does **not** yet integrate the Android keyboard UI, Google Play Billing purchase verification, a payment gateway, push notifications, or a native Android owner app.

## Run

Requires Node.js 20+ and a working native build toolchain for `better-sqlite3`.

```sh
cp .env.example .env
# Fill every required value in .env. Generate secrets with: openssl rand -hex 32
npm install
npm start
```

Open `/admin` for the private owner console. It is a small installable web app/PWA; it is not yet a separately signed Android APK. The owner key is entered at runtime and is never embedded in the keyboard APK.

## Endpoints

- `GET /health`
- `GET /v1/store/config` — configured UPI and bank transfer instructions, plan prices.
- `POST /v1/auth/google` — verifies a Google ID token; body `{ "credential": "..." }`.
- `POST /v1/claims` — authenticated manual payment claim; body `{ "plan":"monthly|yearly|lifetime", "paymentMethod":"upi|bank_transfer", "paymentReference":"...", "payerName":"...", "note":"..." }`.
- `GET /v1/claims/mine`
- `GET /v1/entitlements/me`
- `GET /v1/admin/claims` — owner bearer key required.
- `POST /v1/admin/claims/:id/review` — owner bearer key required; body `{ "decision":"approve|deny", "note":"..." }`.

## Security / production checklist

- Use HTTPS only; set `PUBLIC_BASE_URL` to the deployed HTTPS origin.
- Set a long random `JWT_SECRET` and `OWNER_ADMIN_KEY`; rotate them if exposed.
- Configure a Google OAuth **Web application** client ID and restrict its authorized origins/redirects to your real domains.
- Do not commit `.env`, bank details, merchant UPI secrets, OAuth secrets, or owner keys.
- Back up and encrypt the SQLite database; restrict server filesystem access.
- Add email/FCM push notifications after deployment; current console refreshes manually.
- Add Google Play Billing purchase-token verification on the server before accepting Play purchases. Never trust a client-reported “paid” flag.
- For Play-distributed builds, implement Play Billing and comply with current Google Play payments policy for digital features. For direct APK distribution, a UPI/bank manual flow may be offered subject to applicable laws and provider terms.
- For bank/UPI manual payments, owner approval is the grant gate. A submitted transaction reference is not proof of payment.
- Add privacy policy, terms, refund/cancellation process, support contact, retention/deletion controls, audit log, account recovery, and abuse monitoring before public launch.
- Implement a dedicated native Android developer app only after backend hosting and notification credentials are configured. This PWA is the initial owner-console prototype.


## Native GGEZ Owner app

The Android `:owner` module is the private companion client for `/v1/admin/claims` and `/v1/admin/claims/:id/review`. Configure `OWNER_ADMIN_KEY` on the server, then enter that value in the Owner app over an HTTPS backend URL. The app does not contain the key by default. The keyboard reads the session through the signature-protected companion provider and validates it with `/v1/admin/session`.

Both Android APKs must be signed by the same certificate for the signature permission to work. Do not distribute the Owner APK publicly. Rotate the bootstrap admin key if the device is lost. Replace this bootstrap flow with owner Google authentication and short-lived server-issued sessions before production.
