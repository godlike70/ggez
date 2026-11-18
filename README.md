# GGEZ Keyboard

Standalone Android project for the public GGEZ keyboard app.
- Android application module: `app/`
- Package: `com.ggez.keyboard`
- Build: `gradle assembleDebug`
- GitHub Actions produces `GGEZ-Keyboard-debug-APK`.

The entitlement backend source is in `server/`. Deploy it separately and configure its `.env` from `.env.example`. Never commit real secrets.

The keyboard can query the GGEZ Owner companion using Android's signature-protected provider. For that permission to work, Keyboard and Owner APKs must be signed with the exact same certificate. Independently generated GitHub debug keys will not match; configure the same private release keystore for both workflows before testing companion recognition. Do not distribute the Owner signing key.

This repository contains the keyboard app source only. GGEZ Owner is maintained in its own repository.
