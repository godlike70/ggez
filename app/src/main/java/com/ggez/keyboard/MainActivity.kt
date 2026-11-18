package com.ggez.keyboard

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

/** GGEZ dashboard: all preferences are local. Imported ZIP packs are declarative data only. */
class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences(GgezPrefs.FILE, MODE_PRIVATE) }
    private val dark = 0xFF171720.toInt()
    private val panel = 0xFF252532.toInt()
    private val white = 0xFFF5F5FA.toInt()
    private val muted = 0xFFC5C5D2.toInt()
    private val pickPlugin = 401
    private val pickImage = 402

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        renderDashboard()
    }

    private fun renderDashboard() {
        val pad = dp(18)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(dark)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, pad)
        }
        scroll.addView(content)

        content.addView(TextView(this).apply {
            text = "GGEZ"
            textSize = 34f
            setTextColor(white)
            gravity = Gravity.CENTER_HORIZONTAL
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        content.addView(TextView(this).apply {
            text = "Keyboard dashboard"
            textSize = 16f
            setTextColor(muted)
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, dp(18))
        })

        addSection(
            content,
            "GET STARTED",
            "Enable GGEZ in Android/HyperOS keyboard settings, then choose it as your current keyboard."
        )
        addButton(content, "Enable keyboard", "Opens system input-method settings") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        addButton(content, "Choose GGEZ keyboard", "Open the Android keyboard picker") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .showInputMethodPicker()
        }

        addSection(
            content,
            "MODES",
            "Choose a default mode. You can also switch modes from the keyboard toolbar."
        )
        val modes = listOf("Auto", "Normal", "Coding", "Gaming")
        val mode = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                modes
            )
            setSelection(modes.indexOf(prefs.getString(GgezPrefs.MODE, "Auto")).coerceAtLeast(0))
            onItemSelectedListener =
                simpleSelection { prefs.edit().putString(GgezPrefs.MODE, it).apply() }
        }
        content.addView(mode, matchWrap())

        addToggle(
            content, "Auto-detect coding context", GgezPrefs.AUTO_CODE, true,
            "Detect likely code fields and common syntax from the current input context."
        )
        addToggle(
            content, "Auto-capitalize sentences", GgezPrefs.AUTO_CAPS, true,
            "Capitalize the first letter after sentence-ending punctuation."
        )
        addToggle(
            content, "Autocorrection", GgezPrefs.AUTOCORRECT, true,
            "Use local spelling rules and the installed word packs."
        )
        addToggle(
            content, "Show suggestion bar", GgezPrefs.SUGGESTIONS, true,
            "Show word or coding suggestions above the keys."
        )

        addSection(
            content,
            "APPEARANCE",
            "Settings apply locally and are used by the keyboard immediately when it refreshes."
        )
        content.addView(label("Keyboard size"))
        addSeek(content, 75, 140, prefs.getInt(GgezPrefs.SIZE, 100), "%") {
            prefs.edit().putInt(GgezPrefs.SIZE, it).apply()
        }

        content.addView(label("Gaming transparency"))
        addSeek(content, 20, 100, prefs.getInt(GgezPrefs.TRANSPARENCY, 92), "% opaque") {
            prefs.edit().putInt(GgezPrefs.TRANSPARENCY, it).apply()
        }

        content.addView(label("Font size"))
        addSeek(content, 12, 24, prefs.getInt(GgezPrefs.FONT_SIZE, 16), " sp") {
            prefs.edit().putInt(GgezPrefs.FONT_SIZE, it).apply()
        }

        content.addView(label("Layout"))
        val layouts = listOf("QWERTY", "Compact QWERTY")
        val layout = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                layouts
            )
            setSelection(
                if (prefs.getString(GgezPrefs.LAYOUT, "QWERTY") == "Compact QWERTY") 1 else 0
            )
            onItemSelectedListener =
                simpleSelection { prefs.edit().putString(GgezPrefs.LAYOUT, it).apply() }
        }
        content.addView(layout, matchWrap())

        content.addView(label("Interface language"))
        val languages = listOf("English (US)", "English (UK)")
        val language = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                languages
            )
            setSelection(
                languages.indexOf(
                    prefs.getString(GgezPrefs.LANGUAGE, "English (US)")
                ).coerceAtLeast(0)
            )
            onItemSelectedListener =
                simpleSelection { prefs.edit().putString(GgezPrefs.LANGUAGE, it).apply() }
        }
        content.addView(language, matchWrap())

        content.addView(label("Coding language preference"))
        val codeLanguages = listOf(
            "Auto-detect", "Python", "JavaScript", "TypeScript", "Kotlin",
            "Java", "HTML", "CSS", "Lua", "SQL"
        )
        val codeLanguage = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                codeLanguages
            )
            setSelection(
                codeLanguages.indexOf(
                    prefs.getString(GgezPrefs.CODE_LANGUAGE, "Auto-detect")
                ).coerceAtLeast(0)
            )
            onItemSelectedListener =
                simpleSelection { prefs.edit().putString(GgezPrefs.CODE_LANGUAGE, it).apply() }
        }
        content.addView(codeLanguage, matchWrap())

        content.addView(label("Keyboard font"))
        val fontFamilies = listOf("Default", "Monospace", "Serif", "Sans-serif")
        val fontFamily = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                fontFamilies
            )
            setSelection(
                fontFamilies.indexOf(
                    prefs.getString(GgezPrefs.FONT_FAMILY, "Default")
                ).coerceAtLeast(0)
            )
            onItemSelectedListener =
                simpleSelection { prefs.edit().putString(GgezPrefs.FONT_FAMILY, it).apply() }
        }
        content.addView(fontFamily, matchWrap())

        addSection(
            content,
            "DICTIONARIES & PLUGINS",
            "Import a GGEZ ZIP pack containing a manifest.json and optional dictionary.txt, snippets.json, or theme.json. ZIP packs cannot run code."
        )
        addButton(
            content, "Import plugin / language pack (.zip)",
            "Choose a ZIP file from your phone"
        ) { openPluginPicker() }
        addButton(
            content, "Download plugin from HTTPS link",
            "Paste a direct link to a GGEZ .zip pack; web pages and executable plugins are rejected"
        ) { promptPluginUrl() }
        addButton(
            content, "Choose image for keyboard",
            "Pick an image; the keyboard can offer it to apps that support image input"
        ) { openImagePicker() }

        // FIX: SharedPreferences.getString() is nullable. Convert nullable values
        // to safe strings before calling split(), isNotBlank(), or other String methods.
        val pluginSummary = TextView(this).apply {
            val pluginName = prefs.getString(GgezPrefs.PLUGIN_NAME, null) ?: "None"
            val pluginWords = prefs.getString(GgezPrefs.PLUGIN_WORDS, null).orEmpty()
            val pluginSnippets = prefs.getString(GgezPrefs.PLUGIN_SNIPPETS, null).orEmpty()
            val wordCount = pluginWords.split(Regex("\\s+")).count { it.isNotBlank() }
            val snippetStatus = if (pluginSnippets.isNotBlank()) "installed" else "none"

            text = "Installed word pack: $pluginName\n" +
                "Extra words: $wordCount\n" +
                "Code snippets: $snippetStatus"
            textSize = 14f
            setTextColor(muted)
            setPadding(0, dp(8), 0, dp(8))
        }
        content.addView(pluginSummary)

        addButton(content, "Clear imported pack", "Remove imported words and snippets") {
            prefs.edit()
                .remove(GgezPrefs.PLUGIN_NAME)
                .remove(GgezPrefs.PLUGIN_WORDS)
                .remove(GgezPrefs.PLUGIN_SNIPPETS)
                .remove(GgezPrefs.PLUGIN_THEME)
                .apply()
            File(filesDir, "ggez/plugins").deleteRecursively()
            renderDashboard()
            Toast.makeText(this, "Imported pack cleared", Toast.LENGTH_SHORT).show()
        }

        addSection(
            content,
            "TRANSLATION",
            "Translation sends the text you explicitly translate to the HTTPS service you configure. Do not translate passwords or confidential content. There is no built-in translation provider or API key."
        )
        val endpoint = EditText(this).apply {
            hint = "HTTPS LibreTranslate-compatible endpoint (e.g. your own server)"
            setText(prefs.getString(GgezPrefs.TRANSLATE_ENDPOINT, "").orEmpty())
            setSingleLine(true)
            setTextColor(white)
            setHintTextColor(muted)
        }
        content.addView(endpoint, matchWrap())

        val apiKey = EditText(this).apply {
            hint = "Optional translation API key"
            setText(prefs.getString(GgezPrefs.TRANSLATE_KEY, "").orEmpty())
            setSingleLine(true)
            inputType = 129
            setTextColor(white)
            setHintTextColor(muted)
        }
        content.addView(apiKey, matchWrap())

        val sourceLang = EditText(this).apply {
            hint = "Source language code (auto or en)"
            setText(prefs.getString(GgezPrefs.TRANSLATE_SOURCE, "auto").orEmpty())
            setSingleLine(true)
            setTextColor(white)
            setHintTextColor(muted)
        }
        content.addView(sourceLang, matchWrap())

        val targetLang = EditText(this).apply {
            hint = "Target language code (e.g. en, ja)"
            setText(prefs.getString(GgezPrefs.TRANSLATE_TARGET, "en").orEmpty())
            setSingleLine(true)
            setTextColor(white)
            setHintTextColor(muted)
        }
        content.addView(targetLang, matchWrap())

        addButton(
            content, "Save translation settings",
            "Used by the keyboard's Translate buttons"
        ) {
            val ep = endpoint.text.toString().trim()
            if (ep.isNotBlank() && !ep.startsWith("https://", true)) {
                Toast.makeText(
                    this,
                    "Translation endpoint must use HTTPS",
                    Toast.LENGTH_LONG
                ).show()
                return@addButton
            }

            prefs.edit()
                .putString(GgezPrefs.TRANSLATE_ENDPOINT, ep)
                .putString(GgezPrefs.TRANSLATE_KEY, apiKey.text.toString().trim())
                .putString(
                    GgezPrefs.TRANSLATE_SOURCE,
                    sourceLang.text.toString().trim().ifBlank { "auto" }
                )
                .putString(
                    GgezPrefs.TRANSLATE_TARGET,
                    targetLang.text.toString().trim().ifBlank { "en" }
                )
                .apply()

            Toast.makeText(this, "Translation settings saved", Toast.LENGTH_SHORT).show()
        }

        addToggle(
            content, "Translate typed draft on demand", GgezPrefs.TRANSLATE_DRAFT, false,
            "Adds a keyboard Translate action; text is sent only when you tap it."
        )

        addSection(
            content,
            "PRIVACY & STORAGE",
            "GGEZ keeps imported packs in its private app directory. HTTPS plugin downloads require network access. Translation sends text only after you press Translate. ZIP plugins are data-only; they cannot execute code."
        )

        addButton(
            content, "Reset keyboard preferences",
            "Restore defaults (keeps installed plugin data)"
        ) {
            val keepName = prefs.getString(GgezPrefs.PLUGIN_NAME, null)
            val keepWords = prefs.getString(GgezPrefs.PLUGIN_WORDS, null)
            val keepSnippets = prefs.getString(GgezPrefs.PLUGIN_SNIPPETS, null)
            val keepTheme = prefs.getString(GgezPrefs.PLUGIN_THEME, null)

            prefs.edit().clear().apply()

            if (keepName != null) {
                prefs.edit()
                    .putString(GgezPrefs.PLUGIN_NAME, keepName)
                    .putString(GgezPrefs.PLUGIN_WORDS, keepWords)
                    .putString(GgezPrefs.PLUGIN_SNIPPETS, keepSnippets)
                    .putString(GgezPrefs.PLUGIN_THEME, keepTheme)
                    .apply()
            }
            renderDashboard()
        }

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun addSection(parent: LinearLayout, title: String, desc: String) {
        parent.addView(TextView(this).apply {
            text = title
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(0xFFB9A0FF.toInt())
            setPadding(0, dp(20), 0, dp(5))
        })
        parent.addView(TextView(this).apply {
            text = desc
            textSize = 14f
            setTextColor(muted)
            setPadding(0, 0, 0, dp(8))
        })
    }

    private fun label(s: String) = TextView(this).apply {
        text = s
        textSize = 14f
        setTextColor(white)
        setPadding(0, dp(10), 0, dp(4))
    }

    private fun addButton(
        parent: LinearLayout,
        title: String,
        subtitle: String,
        action: () -> Unit
    ) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(11), dp(14), dp(11))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(panel)
            }
            setOnClickListener { action() }
        }

        box.addView(TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(white)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        box.addView(TextView(this).apply {
            text = subtitle
            textSize = 12f
            setTextColor(muted)
        })

        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, dp(4), 0, dp(5))
        parent.addView(box, lp)
    }

    private fun addToggle(
        parent: LinearLayout,
        title: String,
        key: String,
        default: Boolean,
        subtitle: String
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(4))
        }
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        texts.addView(TextView(this).apply {
            text = title
            textSize = 15f
            setTextColor(white)
        })
        texts.addView(TextView(this).apply {
            text = subtitle
            textSize = 11f
            setTextColor(muted)
        })

        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Switch(this).apply {
            isChecked = prefs.getBoolean(key, default)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(key, checked).apply()
            }
        })
        parent.addView(row)
    }

    private fun addSeek(
        parent: LinearLayout,
        min: Int,
        max: Int,
        value: Int,
        suffix: String,
        save: (Int) -> Unit
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val amount = TextView(this).apply {
            text = "$value$suffix"
            setTextColor(muted)
            minWidth = dp(65)
            gravity = Gravity.END
        }
        val seek = SeekBar(this).apply {
            this.max = max - min
            progress = (value - min).coerceIn(0, max - min)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    sb: SeekBar?,
                    p: Int,
                    fromUser: Boolean
                ) {
                    amount.text = "${p + min}$suffix"
                    if (fromUser) save(p + min)
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        row.addView(seek, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(amount)
        parent.addView(row)
    }

    private fun simpleSelection(save: (String) -> Unit) =
        object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                (parent?.getItemAtPosition(position) as? String)?.let(save)
            }
        }

    private fun openPluginPicker() {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
            },
            pickPlugin
        )
    }

    @Deprecated("Legacy callback retained for wide Android compatibility")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return

        try {
            when (requestCode) {
                pickPlugin -> {
                    val dir = File(filesDir, "ggez/plugins").apply { mkdirs() }
                    val temp = File.createTempFile("import-", ".zip", dir)

                    contentResolver.openInputStream(uri).use { source ->
                        requireNotNull(source) { "Unable to read selected ZIP" }.use { input ->
                            temp.outputStream().use { output -> input.copyTo(output) }
                        }
                    }

                    FileInputStream(temp).use { importPluginStream(it) }
                    temp.renameTo(File(dir, "imported-${System.currentTimeMillis()}.zip"))
                    Toast.makeText(this, "GGEZ pack imported", Toast.LENGTH_LONG).show()
                    renderDashboard()
                }

                pickImage -> {
                    val flags = data.flags
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            flags and (
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                )
                        )
                    } catch (_: Exception) {
                    }

                    prefs.edit().putString(GgezPrefs.IMAGE_URI, uri.toString()).apply()
                    Toast.makeText(
                        this,
                        "Image ready for supported apps",
                        Toast.LENGTH_LONG
                    ).show()
                    renderDashboard()
                }
            }
        } catch (e: Exception) {
            AlertDialog.Builder(this)
                .setTitle("Could not complete action")
                .setMessage(e.message ?: "Invalid file or plugin")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun importPlugin(uri: Uri) {
        contentResolver.openInputStream(uri).use {
            importPluginStream(requireNotNull(it))
        }
    }

    private fun importPluginStream(input: InputStream) {
        var manifest: JSONObject? = null
        var words = ""
        var snippets = ""
        var theme = ""
        var total = 0

        input.use { stream ->
            ZipInputStream(stream).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(!entry.isDirectory) {
                        "ZIP directory entries are not supported; place pack files at the ZIP root"
                    }

                    val name = entry.name.replace('\\', '/')
                    require(!name.startsWith("/") && !name.split('/').contains("..")) {
                        "Unsafe path inside ZIP"
                    }
                    require(
                        name in setOf(
                            "manifest.json", "dictionary.txt", "snippets.json",
                            "theme.json", "translation.json"
                        )
                    ) {
                        "Unsupported file '$name'. Packs may contain only manifest.json, dictionary.txt, snippets.json, theme.json and translation.json."
                    }

                    val out = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var entryBytes = 0

                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        entryBytes += count
                        total += count
                        require(total <= 1_000_000 && entryBytes <= 500_000) {
                            "Pack exceeds size limit"
                        }
                        out.write(buffer, 0, count)
                    }

                    val text = out.toByteArray().toString(Charsets.UTF_8)
                    when (name) {
                        "manifest.json" -> manifest = JSONObject(text)

                        "dictionary.txt" -> {
                            words = text.lineSequence()
                                .map { it.trim().lowercase() }
                                .filter {
                                    it.matches(Regex("[\\p{L}][\\p{L}'’-]{0,39}"))
                                }
                                .distinct()
                                .take(20000)
                                .joinToString(" ")
                        }

                        "snippets.json" -> {
                            val json = JSONObject(text)
                            require(json.length() <= 200) { "Too many snippets" }
                            snippets = json.toString()
                        }

                        "theme.json" -> {
                            val json = JSONObject(text)
                            val background = json.optString("background", "")
                            require(
                                background.matches(
                                    Regex("#?[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")
                                ) || background.isEmpty()
                            ) { "Invalid theme background color" }
                            theme = json.toString()
                        }

                        "translation.json" -> {
                            val json = JSONObject(text)
                            val ep = json.optString("endpoint", "")
                            require(ep.isBlank() || ep.startsWith("https://", true)) {
                                "Translation endpoint must use HTTPS"
                            }
                            prefs.edit()
                                .putString(GgezPrefs.TRANSLATE_ENDPOINT, ep)
                                .putString(
                                    GgezPrefs.TRANSLATE_SOURCE,
                                    json.optString("source", "auto")
                                )
                                .putString(
                                    GgezPrefs.TRANSLATE_TARGET,
                                    json.optString("target", "en")
                                )
                                .apply()
                        }
                    }
                    zip.closeEntry()
                }
            }
        }

        val m = requireNotNull(manifest) { "ZIP must include manifest.json" }
        require(m.optString("format") == "ggez-pack-v1") {
            "manifest format must be ggez-pack-v1"
        }

        val name = m.optString("name", "GGEZ Pack").take(60)
        val pluginDir = File(filesDir, "ggez/plugins").apply { mkdirs() }
        File(
            pluginDir,
            name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(60) + ".installed"
        ).writeText(
            "installedAt=${System.currentTimeMillis()}\nformat=ggez-pack-v1\n"
        )

        val oldWords = prefs.getString(GgezPrefs.PLUGIN_WORDS, null)
            .orEmpty()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val combinedWords = (
            oldWords + words.split(Regex("\\s+")).filter { it.isNotBlank() }
            )
            .distinct()
            .take(100000)
            .joinToString(" ")

        val oldSnippets = try {
            JSONObject(prefs.getString(GgezPrefs.PLUGIN_SNIPPETS, null).orEmpty())
        } catch (_: Exception) {
            JSONObject()
        }

        val newSnippets = if (snippets.isNotBlank()) JSONObject(snippets) else JSONObject()
        val keys = newSnippets.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            if (oldSnippets.length() < 500 || oldSnippets.has(key)) {
                oldSnippets.put(key, newSnippets.get(key))
            }
        }

        val oldNames = prefs.getString(GgezPrefs.PLUGIN_NAME, null)
            .orEmpty()
            .split(" + ")
            .filter { it.isNotBlank() }

        val combinedNames = (oldNames + name).distinct().takeLast(10).joinToString(" + ")

        prefs.edit()
            .putString(GgezPrefs.PLUGIN_NAME, combinedNames)
            .putString(GgezPrefs.PLUGIN_WORDS, combinedWords)
            .putString(GgezPrefs.PLUGIN_SNIPPETS, oldSnippets.toString())
            .putString(
                GgezPrefs.PLUGIN_THEME,
                if (theme.isNotBlank()) theme
                else prefs.getString(GgezPrefs.PLUGIN_THEME, null).orEmpty()
            )
            .apply()
    }

    private fun openImagePicker() {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            },
            pickImage
        )
    }

    private fun promptPluginUrl() {
        val input = EditText(this).apply {
            hint = "https://example.com/ggez-pack.zip"
            setSingleLine(true)
            setTextColor(white)
            setHintTextColor(muted)
        }

        AlertDialog.Builder(this)
            .setTitle("Download GGEZ plugin pack")
            .setMessage(
                "Use a direct HTTPS link to a .zip file. GGEZ imports only safe data files and never runs plugin code."
            )
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Download") { _, _ ->
                val link = input.text.toString().trim()

                if (!link.startsWith("https://", true)) {
                    Toast.makeText(
                        this,
                        "Only HTTPS links are allowed",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setPositiveButton
                }

                Toast.makeText(this, "Downloading plugin…", Toast.LENGTH_SHORT).show()

                Thread {
                    try {
                        var url = URL(link)
                        var conn: HttpURLConnection? = null
                        var redirects = 0

                        while (true) {
                            require(url.protocol.equals("https", true)) {
                                "Only HTTPS links and HTTPS redirects are allowed"
                            }

                            conn = (url.openConnection() as HttpURLConnection).apply {
                                connectTimeout = 15000
                                readTimeout = 20000
                                instanceFollowRedirects = false
                                requestMethod = "GET"
                            }

                            val code = conn.responseCode
                            if (code in listOf(301, 302, 303, 307, 308)) {
                                val location = conn.getHeaderField("Location")
                                    ?: error("Redirect did not include a destination")
                                conn.disconnect()
                                require(++redirects <= 5) { "Too many redirects" }
                                url = URL(url, location)
                                continue
                            }

                            require(code in 200..299) {
                                "Download failed (HTTP $code). Use a direct file URL, not a search results page."
                            }
                            break
                        }

                        val active = requireNotNull(conn)
                        try {
                            val type = active.contentType.orEmpty().lowercase()
                            require(
                                type.contains("zip") ||
                                    url.path.endsWith(".zip", true) ||
                                    type.contains("octet-stream")
                            ) {
                                "The link did not return a ZIP file. Use a direct .zip download link."
                            }

                            val dest = File(filesDir, "ggez/plugins").apply { mkdirs() }
                            val temp = File.createTempFile("download-", ".zip", dest)

                            active.inputStream.use { ins ->
                                temp.outputStream().use { out ->
                                    val buffer = ByteArray(8192)
                                    var total = 0
                                    while (true) {
                                        val n = ins.read(buffer)
                                        if (n < 0) break
                                        total += n
                                        require(total <= 5 * 1024 * 1024) {
                                            "Plugin ZIP exceeds 5 MB"
                                        }
                                        out.write(buffer, 0, n)
                                    }
                                }
                            }

                            FileInputStream(temp).use { importPluginStream(it) }
                            val installed = File(
                                dest,
                                "downloaded-${System.currentTimeMillis()}.zip"
                            )
                            require(temp.renameTo(installed)) {
                                "Plugin imported, but could not save the ZIP to GGEZ storage"
                            }

                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    this,
                                    "Plugin downloaded and imported",
                                    Toast.LENGTH_LONG
                                ).show()
                                renderDashboard()
                            }
                        } finally {
                            active.disconnect()
                        }
                    } catch (e: Exception) {
                        Handler(Looper.getMainLooper()).post {
                            AlertDialog.Builder(this)
                                .setTitle("Plugin download failed")
                                .setMessage(e.message ?: "Could not download plugin")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                }.start()
            }
            .show()
    }

    private fun matchWrap() =
        LinearLayout.LayoutParams(-1, dp(48)).apply {
            bottomMargin = dp(4)
        }

    private fun dp(n: Int) =
        (resources.displayMetrics.density * n).toInt()
}