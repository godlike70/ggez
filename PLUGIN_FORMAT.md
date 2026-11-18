# GGEZ data-only plugin packs

GGEZ currently supports **ZIP data packs**, not executable plugins. This is intentional: executing arbitrary code shipped inside a keyboard plugin would create a serious security risk.

## Required file

`manifest.json` at the ZIP root:

```json
{
  "format": "ggez-pack-v1",
  "name": "My Coding Pack"
}
```

## Optional files

### `dictionary.txt`

UTF-8 text, one word per line. Example:

```text
autocomplete
compiler
function
parameter
syntax
variable
```

Words are normalized to lowercase and limited to letters plus apostrophe/hyphen characters, with a maximum length of 40 characters. The importer accepts up to 20,000 distinct words.

### `snippets.json`

A JSON object where a trigger maps to a replacement string:

```json
{
  "clog": "console.log()",
  "pydef": "def function_name():",
  "kfun": "fun functionName() {\n}"
}
```

When a coding suggestion matching a snippet trigger is tapped, GGEZ inserts the snippet text. Cursor placement and placeholder fields are not yet supported.

### `translation.json`

Optional configuration for a user-managed LibreTranslate-compatible translation service. Importing this file sets the endpoint and language defaults; it does not provide a translation server or API key.

```json
{
  "endpoint": "https://your-translation-server.example/translate",
  "source": "auto",
  "target": "en"
}
```

Only HTTPS endpoints are accepted. Configure the optional API key in the dashboard. Text is sent to this service only after you tap a Translate action. Never put API secrets inside a public plugin ZIP.

### `theme.json`

```json
{
  "background": "20202A"
}
```

`background` accepts six-digit RGB or eight-digit ARGB hex, with an optional `#` prefix.

## Restrictions and safety

- ZIP may contain only `manifest.json`, `dictionary.txt`, `snippets.json`, `theme.json`, and `translation.json` at the root.
- Total uncompressed data is capped at 1 MB; each file is capped at 500 KB.
- Directory entries, absolute paths, parent traversal and unknown filenames are rejected.
- No executable code is loaded or run from a pack.
- Word lists and snippets from multiple imported packs are merged. The latest imported theme wins. The dashboard currently clears all packs together; individual removal/export are not implemented yet.
