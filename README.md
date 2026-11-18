GGEZ Keyboard

Your keyboard. Your rules. Your experience.

GGEZ Keyboard is an Android keyboard project focused on customization, privacy, productivity, gaming, and coding. It's designed to provide a flexible typing experience with offline functionality and an expandable plugin ecosystem.

The goal is to build a keyboard that gives users more control over how they type, customize, and interact with their device.

---

✦ Features

⌨️ Typing Experience

- Native Android keyboard implementation.
- Standard QWERTY keyboard layout.
- Customizable keyboard appearance.
- Adjustable keyboard size and scaling.
- Configurable font size and font family.
- Sentence capitalization settings.
- Autocorrection and word suggestions.

🧠 Smart Suggestions

- Offline word suggestions.
- Built-in dictionary.
- Common typo correction.
- Context-aware typing improvements.
- Support for expanding language dictionaries.

🎮 Gaming Mode

A dedicated keyboard experience for gaming and users who prefer a different keyboard layout.

- Adjustable keyboard opacity.
- Keyboard scaling.
- Customizable appearance.
- Compact layout options.

💻 Coding Mode

A typing environment designed for developers.

- Coding-oriented quick-access keys.
- Convenient access to programming symbols.
- Support for common programming languages.
- Syntax-friendly typing experience.

Supported language contexts include:

- Python
- JavaScript
- TypeScript
- Kotlin
- Java
- HTML
- CSS
- Lua
- SQL

🌐 Offline Language Support

GGEZ aims to support downloadable offline language resources.

- Language-specific dictionaries.
- Offline word suggestions.
- Downloadable language packs.
- Local access to installed language resources.

The goal is to keep essential typing functionality available without requiring a permanent internet connection.

🎨 Customization

Make the keyboard feel like your own.

- Multiple keyboard modes.
- Adjustable dimensions.
- Custom fonts.
- Theme customization.
- Opacity controls.
- Layout preferences.

---

✦ Keyboard Modes

GGEZ is designed around different typing environments.

Mode| Purpose
Normal| Everyday typing
Coding| Programming and syntax-oriented typing
Gaming| Gaming-focused keyboard experience
Auto| Automatically adapts the typing experience

Switch between modes using the keyboard's mode control.

---

✦ Plugin Ecosystem

GGEZ is designed to support an expandable plugin ecosystem.

Plugins allow users to extend their keyboard experience without modifying the core application.

Supported plugin resource types include:

- Dictionary Packs — Additional vocabulary and word resources.
- Snippet Packs — Reusable text and coding snippets.
- Theme Packs — Custom keyboard appearances.
- Translation Configurations — Translation-related configuration resources.
- Language Resources — Additional language support.

Plugins use a structured, data-only ZIP format.

Arbitrary executable code is not accepted as a plugin.

Plugin Studio

Plugin Studio is planned as the dedicated environment for creating and managing plugins.

Its intended capabilities include:

- Creating new plugins.
- Editing existing plugins.
- Testing plugin configurations.
- Importing and exporting plugins.
- Managing plugin resources.

Plugin Studio is intended to be a premium feature, while essential keyboard functionality and core resources remain free.

---

✦ GGEZ Owner

GGEZ Owner is a separate companion Android application maintained in its own repository.

It is designed for owner-side administration and management of GGEZ services.

Its intended responsibilities include:

- Owner authentication.
- Managing premium access requests.
- Reviewing payment requests.
- Approving or rejecting requests.
- Managing subscription entitlements.

GGEZ Keyboard and GGEZ Owner are separate applications with separate source repositories.

The keyboard can communicate with the companion through a protected Android interface. Both applications must use the same signing certificate for signature-protected communication.

---

✦ Premium System

GGEZ is intended to follow a free-core, premium-expansion model.

Free functionality

- Core keyboard functionality.
- Essential typing features.
- Basic customization.
- Offline typing resources.

Premium functionality

- Plugin Studio.
- Advanced plugin creation and management.
- Additional premium tools and capabilities.

The intention is to keep everyday typing accessible while offering additional tools for users who want to expand their experience.

---

✦ Privacy First

Privacy is an important part of GGEZ's design philosophy.

The project aims to prioritize:

- Offline-first functionality.
- Local language resources.
- Minimal unnecessary data collection.
- Controlled plugin imports.
- No arbitrary executable plugin code.
- Secure communication between companion components.

A keyboard can handle sensitive information, so privacy and security must remain central to its development.

---

✦ Technology

GGEZ Keyboard is built around native Android development.

Component| Technology
Application| Android
Programming language| Kotlin
Build system| Gradle
UI and keyboard integration| Android APIs
Backend| Node.js
Backend framework| Express
Database| SQLite
Plugin format| Structured ZIP resources

Android package: "com.ggez.keyboard"

Owner package: "com.ggez.owner"

---

✦ Project Structure

The Keyboard repository contains the following main components:

Directory / File| Purpose
"app/"| Android keyboard application
"server/"| Backend service
"plugin-template/"| Example plugin resources
".github/workflows/"| Automated Android build workflow
"PRIVACY.md"| Privacy information
"PLUGIN_FORMAT.md"| Plugin format documentation
"PAYMENTS_AND_PLUGIN_ARCHITECTURE.md"| Premium and plugin architecture

---

✦ Development

GGEZ Keyboard is an actively developed project.

The long-term vision is to build a complete Android keyboard ecosystem with:

- Intelligent typing assistance.
- Expanded offline language support.
- Flexible customization.
- Dedicated coding and gaming experiences.
- A secure plugin ecosystem.
- A companion owner application.
- A dedicated Plugin Studio.

Some capabilities are still under development and will evolve as the project progresses.

---

✦ Repositories

- GGEZ Keyboard: https://github.com/godlike70/ggez
- GGEZ Owner: Maintained separately.

---

✦ Vision

GGEZ isn't meant to be just another keyboard.

It's intended to be a customizable typing platform where users can shape their own experience, developers can extend functionality, and essential features remain accessible without depending on constant internet connectivity.

GGEZ — Type beyond limits.
