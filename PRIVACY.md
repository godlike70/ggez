# GGEZ Keyboard — prototype privacy notice

**Status:** This notice describes this prototype source and must be reviewed before public release.

## Local typing

Ordinary key input, built-in spelling suggestions, and dictionary matching are processed on-device. GGEZ has no advertising SDK, analytics SDK, account system, or GGEZ-operated server in this project. Like any third-party keyboard, GGEZ receives text through Android's input-method interface while the user types; avoid using an experimental keyboard for passwords or other highly sensitive fields.

## Network access

The app requests Internet permission for two user-initiated functions: (1) downloading a GGEZ data-only ZIP pack from a direct HTTPS URL pasted by the user, and (2) translating text after the user explicitly taps a Translate action. Plugin downloads are limited in size, HTTPS redirects only, and accepted archive contents are allowlisted. Imported plugin packs cannot execute code.

When translation is used, the text the user chose to translate is sent to the HTTPS translation endpoint configured in the dashboard. That provider may process or retain the text under its own policy. Users should configure a trusted provider and must not translate passwords, financial information, or confidential content. GGEZ does not ship with a translation provider or API key. API keys entered into the dashboard are stored in local app preferences; do not share screenshots or backups containing secrets.

## Files and images

Users select plugin ZIPs and images through Android's system document picker. GGEZ stores imported ZIPs and small plugin metadata in its app-private `filesDir/ggez/plugins` directory and remembers the chosen image content URI when Android allows persistent access. GGEZ does not request broad access to all files. Image insertion is offered through Android's rich-content input API and only works in target apps that support it.

## Retention and deletion

Plugin ZIPs are stored in app-private storage until app data is cleared or the app is uninstalled. Preferences such as language settings, the optional translation endpoint/key, imported dictionary words and snippets are stored locally. Clear imported packs or clear GGEZ app data to remove locally stored settings and packs. Translation providers may retain text according to their own policies.

## Future changes

If future versions add cloud sync, AI services, analytics, ads, accounts, or other data collection, this notice must be updated before release and users must be informed. Do not publish this prototype notice unchanged until the final app behavior, developer contact details, and applicable legal requirements have been reviewed.
