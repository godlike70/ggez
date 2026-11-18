# GGEZ Plugin Studio and purchase architecture

## Product boundary

The normal GGEZ keyboard remains free: language packs, dictionaries, typing suggestions, autocorrection, layouts, themes, clipboard features, and ordinary keyboard customization are core keyboard features. Plugin Studio is a separate paid developer product for authoring/testing/importing/exporting user-created plugins and reading the plugin format/source documentation.

## Distribution and payments

GGEZ will be distributed through direct GitHub APK downloads and Google Play. Payment handling must be distribution-aware:

- Direct APK: manually verified UPI or bank-transfer claims can be submitted to the backend. Owner approval creates an entitlement.
- Google Play: digital Plugin Studio purchases must use the applicable Play Billing flow unless a current policy exception/program applies. The purchase token must be verified server-side with Google Play Developer APIs before an entitlement is created. Do not treat a screenshot, email, or client-side boolean as proof of a Play purchase.

The first backend milestone in `server/` implements the manual claim and owner-review path. Play Billing verification remains a separate integration milestone and is deliberately not simulated.

## Owner setup

Merchant UPI and bank payout instructions are configured in the backend `.env` file. They are returned by `/v1/store/config` so the customer purchase screen can display the configured methods. Never put merchant credentials, owner keys, OAuth client secrets, or server secrets in the Android APK.

The owner console in `server/public/` is a minimal installable web console for pending requests. It is not yet a native Android owner app. It can be hosted privately behind HTTPS and accessed only with the owner key. A later Android wrapper/native app can use the same authenticated endpoints and Firebase Cloud Messaging for notifications.

## Authorization

Google identity is verified server-side. A submitted payment reference creates a pending claim, not access. Only the owner review endpoint can approve a manual claim and create a subscription/lifetime entitlement. Monthly and yearly entitlements receive expiry timestamps; lifetime entitlements have no expiry.

The owner bypass must be a server-side owner account/role, not a universal code compiled into the APK. Do not distribute a reusable owner code. Use a separate owner identity with strong authentication and server-side role checks.
