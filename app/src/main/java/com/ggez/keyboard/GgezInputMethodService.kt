package com.ggez.keyboard

import android.content.res.Configuration
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.InputContentInfo
import android.view.inputmethod.InputConnection
import android.widget.Toast
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import kotlin.math.abs

/** Offline-first keyboard. ZIP packs are data-only; no imported executable code is loaded. */
class GgezInputMethodService : InputMethodService() {
    private lateinit var root: LinearLayout
    private lateinit var suggestions: LinearLayout
    private var caps = true
    private var symbols = false
    private var modeOverride: String? = null
    private val prefs by lazy { getSharedPreferences(GgezPrefs.FILE, MODE_PRIVATE) }

    private val baseWords = setOf(
        "a", "about", "above", "after", "again", "all", "also", "am", "an", "and", "any", "are", "as", "at", "awesome", "back", "be", "because", "been", "before", "best", "better", "but", "by", "can", "can't", "come", "could", "day", "did", "do", "does", "doing", "done", "dont", "don't", "down", "each", "even", "every", "for", "from", "fun", "get", "getting", "give", "go", "good", "got", "great", "had", "has", "have", "he", "hello", "help", "her", "here", "hey", "him", "his", "how", "i", "if", "ill", "i'm", "im", "in", "into", "is", "it", "its", "it's", "just", "know", "like", "lol", "look", "love", "make", "me", "more", "my", "need", "never", "new", "no", "not", "now", "of", "off", "ok", "okay", "on", "one", "only", "or", "other", "our", "out", "please", "really", "right", "said", "say", "see", "she", "should", "so", "some", "sorry", "still", "sure", "take", "than", "that", "the", "their", "them", "then", "there", "these", "they", "thing", "think", "this", "time", "to", "today", "too", "try", "up", "us", "use", "very", "want", "was", "way", "we", "well", "were", "what", "when", "where", "which", "who", "why", "will", "with", "work", "would", "yeah", "yes", "you", "your", "you're", "youre", "thanks", "thank", "friend", "keyboard", "typing", "suggestion", "suggestions", "word", "words", "because", "definitely", "different", "language", "privacy", "coding", "function", "return", "class", "const", "let", "var", "import", "export", "public", "private", "void", "string", "int", "boolean", "true", "false", "null", "none", "self", "print", "println", "console", "log", "if", "else", "for", "while", "fun", "val", "override", "package", "android", "javascript", "typescript", "python", "kotlin", "java", "html", "css", "json", "lua", "sql", "git", "commit", "push", "pull", "async", "await", "try", "catch", "throw", "new", "this", "super"
    )
    private val commonFixes = mapOf("reallt" to "really", "realy" to "really", "relly" to "really", "teh" to "the", "thier" to "their", "wierd" to "weird", "definately" to "definitely", "becuase" to "because", "becouse" to "because", "dont" to "don't", "cant" to "can't", "wont" to "won't", "doesnt" to "doesn't", "isnt" to "isn't", "im" to "I'm", "ive" to "I've", "ill" to "I'll", "youre" to "you're", "thats" to "that's", "whats" to "what's", "hwo" to "how", "adn" to "and", "taht" to "that", "waht" to "what", "goood" to "good", "helllo" to "hello", "thnaks" to "thanks", "becasue" to "because")
    private val codeWords = listOf("if", "else", "for", "while", "return", "class", "fun", "function", "const", "let", "var", "val", "import", "export", "public", "private", "static", "void", "string", "int", "boolean", "true", "false", "null", "None", "self", "print", "println", "console.log", "async", "await", "try", "catch", "throw", "new", "this", "super", "def", "elif", "lambda", "from", "as", "with", "yield", "package", "override", "interface", "object", "SELECT", "FROM", "WHERE", "JOIN", "CREATE", "TABLE", "local", "then", "end", "nil", "and", "or", "not")

    override fun onCreateInputView(): View {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(3), dp(3), dp(3), dp(4)) }
        suggestions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        root.addView(suggestions, LinearLayout.LayoutParams(-1, dp(38)))
        buildKeyboard(); updateSuggestions(); return root
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        Thread {
            val verified = OwnerCompanion.verify(this)
            prefs.edit().putBoolean(GgezPrefs.OWNER_VERIFIED, verified).apply()
        }.start()
        caps = prefs.getBoolean(GgezPrefs.AUTO_CAPS, true); symbols = false; modeOverride = null
        if (::root.isInitialized) { buildKeyboard(); updateSuggestions() }
    }

    private fun activeMode(): String {
        val chosen = modeOverride ?: prefs.getString(GgezPrefs.MODE, "Auto") ?: "Auto"
        if (chosen != "Auto") return chosen
        if (prefs.getBoolean(GgezPrefs.AUTO_CODE, true)) {
            val info = currentInputEditorInfo
            val inputType = info?.inputType ?: 0
            if ((inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_PASSWORD || (inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD || (inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD || (inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_NUMBER && (inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_NUMBER_VARIATION_PASSWORD) return "Normal"
            val before = currentInputConnection?.getTextBeforeCursor(100, 0)?.toString().orEmpty()
            if (before.contains("fun ") || before.contains("def ") || before.contains("function ") || before.contains("<div") || before.contains("console.") || before.contains("println(") || before.contains("{") || before.contains("=>") || before.contains("</")) return "Coding"
        }
        return "Normal"
    }

    private fun buildKeyboard() {
        if (!::root.isInitialized) return
        while (root.childCount > 1) root.removeViewAt(1)
        val mode = activeMode()
        val scale = (prefs.getInt(GgezPrefs.SIZE, 100).coerceIn(75, 140) / 100f)
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val rowHeight = ((if (landscape) 38 else 46) * scale).toInt().coerceIn(30, 64)
        val alpha = if (mode == "Gaming") (prefs.getInt(GgezPrefs.TRANSPARENCY, 92).coerceIn(20, 100) * 255 / 100) else 255
        val theme = try { JSONObject(prefs.getString(GgezPrefs.PLUGIN_THEME, "") ?: "").optString("background", "") } catch (_: Exception) { "" }
        val baseColor = if (theme.isNotBlank()) try { Color.parseColor(if (theme.startsWith("#")) theme else "#$theme") } catch (_: Exception) { Color.rgb(24, 24, 32) } else Color.rgb(24, 24, 32)
        root.background = GradientDrawable().apply { cornerRadius = dp(10).toFloat(); setColor(Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))) }
        val font = prefs.getInt(GgezPrefs.FONT_SIZE, 16).coerceIn(12, 24).toFloat()
        if (mode == "Coding") addRow(listOf("()", "{}", "[]", "=>", "=", ";", "'", "\"", "<", ">"), rowHeight, font)
        val rows = if (symbols) listOf(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), listOf("@", "#", "\$", "%", "&", "-", "+", "(", ")", "/"), listOf("=", "*", "\"", "'", ":", ";", "!", "?", "⌫"), listOf("ABC", ",", "space", ".", "↵"))
        else if (prefs.getString(GgezPrefs.LAYOUT, "QWERTY") == "Compact QWERTY") listOf(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"), listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"), listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫"), listOf("?123", ",", "space", ".", "↵"))
        else listOf(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"), listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"), listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫"), listOf("?123", ",", "space", ".", "↵"))
        rows.forEach { addRow(it, rowHeight, font) }
        if (mode == "Gaming") {
            val hint = TextView(this).apply { text = "GAMING MODE · ${prefs.getInt(GgezPrefs.TRANSPARENCY, 92)}% opaque · change in GGEZ dashboard"; textSize = 10f; gravity = Gravity.CENTER; setTextColor(Color.WHITE) }
            root.addView(hint, LinearLayout.LayoutParams(-1, dp(18)))
        }
    }

    private fun addRow(keys: List<String>, height: Int, font: Float) {
        val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        keys.forEach { key ->
            val label = when (key) { "space" -> prefs.getString(GgezPrefs.LANGUAGE, "English (US)")?.replace(" (US)", "")?.replace(" (UK)", "") ?: "English"; else -> if (caps && key.length == 1 && key[0].isLetter()) key.uppercase() else key }
            val button = Button(this).apply {
                text = label; textSize = if (key == "space") 10f else font; setTextColor(Color.WHITE); isAllCaps = false; typeface = when (prefs.getString(GgezPrefs.FONT_FAMILY, "Default")) { "Monospace" -> Typeface.MONOSPACE; "Serif" -> Typeface.SERIF; "Sans-serif" -> Typeface.SANS_SERIF; else -> Typeface.DEFAULT }
                background = keyBackground(key); minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0; setPadding(dp(1), 0, dp(1), 0)
                setOnClickListener { onKeyPressed(key) }
            }
            val weight = when (key) { "space" -> 5f; "?123", "ABC", "⇧", "⌫", "↵" -> 1.35f; else -> 1f }
            line.addView(button, LinearLayout.LayoutParams(0, height, weight).apply { setMargins(dp(1), dp(2), dp(1), dp(2)) })
        }
        root.addView(line, LinearLayout.LayoutParams(-1, height + dp(4)))
    }

    private fun keyBackground(key: String): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(6).toFloat()
        val opaque = if (activeMode() == "Gaming") prefs.getInt(GgezPrefs.TRANSPARENCY, 92).coerceIn(20, 100) * 255 / 100 else 255
        val rgb = when (key) { "space" -> Color.rgb(70, 67, 87); "↵" -> Color.rgb(124, 82, 220); "⌫", "⇧", "?123", "ABC" -> Color.rgb(61, 60, 76); else -> Color.rgb(48, 48, 61) }
        setColor(Color.argb(opaque, Color.red(rgb), Color.green(rgb), Color.blue(rgb)))
    }

    private fun onKeyPressed(key: String) {
        val ic = currentInputConnection ?: return
        when (key) {
            "⇧" -> { caps = !caps; buildKeyboard() }
            "?123" -> { symbols = true; buildKeyboard() }
            "ABC" -> { symbols = false; buildKeyboard() }
            "⌫" -> { val selected = ic.getSelectedText(0); if (!selected.isNullOrEmpty()) ic.commitText("", 1) else if (!ic.getTextBeforeCursor(1, 0).isNullOrEmpty()) ic.deleteSurroundingText(1, 0); updateSuggestions() }
            "space" -> { maybeAutoCorrect(); ic.commitText(" ", 1); caps = prefs.getBoolean(GgezPrefs.AUTO_CAPS, true) && shouldCapitalize(); updateSuggestions(); buildKeyboard() }
            "↵" -> { val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE; if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) ic.performEditorAction(action) else ic.commitText("\n", 1); caps = prefs.getBoolean(GgezPrefs.AUTO_CAPS, true); updateSuggestions() }
            else -> {
                val out = when (key) { "()" -> "()"; "{}" -> "{}"; "[]" -> "[]"; "=>" -> "=>"; else -> if (key.length == 1 && key[0].isLetter() && caps) key.uppercase() else key }
                ic.commitText(out, 1)
                val wasCaps = caps
                if (key.length == 1 && key[0].isLetter()) caps = false
                updateSuggestions(); if (wasCaps != caps) buildKeyboardIfNeeded()
            }
        }
    }

    private fun buildKeyboardIfNeeded() { if (::root.isInitialized) buildKeyboard() }
    private fun shouldCapitalize(): Boolean {
        val before = currentInputConnection?.getTextBeforeCursor(3, 0)?.toString().orEmpty()
        return before.isBlank() || before.endsWith(". ") || before.endsWith("! ") || before.endsWith("? ") || before.endsWith("\n")
    }
    private fun maybeAutoCorrect() {
        if (!prefs.getBoolean(GgezPrefs.AUTOCORRECT, true) || activeMode() == "Coding") return
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        val word = before.takeLastWhile { it.isLetter() || it == '\'' || it == '’' }
        if (word.isBlank()) return
        val fix = commonFixes[word.lowercase()] ?: return
        ic.deleteSurroundingText(word.length, 0); ic.commitText(matchCase(word, fix), 1)
    }

    private fun updateSuggestions() {
        if (!::suggestions.isInitialized) return
        suggestions.removeAllViews()
        val ic = currentInputConnection
        val before = ic?.getTextBeforeCursor(100, 0)?.toString().orEmpty()
        val current = before.takeLastWhile { it.isLetter() || it == '\'' || it == '_' }
        val mode = activeMode()
        val password = (currentInputEditorInfo?.inputType ?: 0) and InputType.TYPE_TEXT_VARIATION_PASSWORD != 0
        val candidates = when {
            password || !prefs.getBoolean(GgezPrefs.SUGGESTIONS, true) -> emptyList()
            mode == "Coding" -> codeSuggestions(current)
            current.isBlank() -> listOf(mode, "⚙")
            else -> suggest(current)
        }
        candidates.take(3).forEach { candidate ->
            val item = TextView(this).apply { text = candidate; textSize = 14f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); setOnClickListener { if (candidate == "⚙") { val i = packageManager.getLaunchIntentForPackage(packageName); i?.let { it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(it) } } else if (candidate in listOf("Auto", "Normal", "Coding", "Gaming")) { modeOverride = if (candidate == "Auto") null else candidate; buildKeyboard(); updateSuggestions() } else if (mode == "Coding") { val snippets = try { JSONObject(prefs.getString(GgezPrefs.PLUGIN_SNIPPETS, "") ?: "") } catch (_: Exception) { JSONObject() }; if (snippets.has(candidate)) replaceCurrentWord(snippets.optString(candidate)) else replaceCurrentWord(candidate) } else replaceCurrentWord(candidate) } }
            suggestions.addView(item, LinearLayout.LayoutParams(0, -1, 1f))
        }
        val modeButton = TextView(this).apply { text = "${mode.take(1)}"; textSize = 15f; gravity = Gravity.CENTER; setTextColor(0xFFCEBFFF.toInt()); contentDescription = "Switch keyboard mode"; setOnClickListener { val modes = listOf("Auto", "Normal", "Coding", "Gaming"); val currentMode = modeOverride ?: prefs.getString(GgezPrefs.MODE, "Auto") ?: "Auto"; modeOverride = modes[(modes.indexOf(currentMode).coerceAtLeast(0) + 1) % modes.size]; buildKeyboard(); updateSuggestions() } }
        suggestions.addView(modeButton, LinearLayout.LayoutParams(dp(34), -1))
        if (prefs.getBoolean(GgezPrefs.TRANSLATE_DRAFT, false)) {
            val translateButton = TextView(this).apply { text = "TR"; textSize = 13f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); contentDescription = "Translate clipboard or draft"; setOnClickListener { showTranslateActions() } }
            suggestions.addView(translateButton, LinearLayout.LayoutParams(dp(38), -1))
        }
        val imageButton = TextView(this).apply { text = "IMG"; textSize = 11f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); contentDescription = "Insert selected image"; setOnClickListener { insertSelectedImage() } }
        suggestions.addView(imageButton, LinearLayout.LayoutParams(dp(42), -1))
    }

    private fun showTranslateActions() {
        android.app.AlertDialog.Builder(this)
            .setTitle("GGEZ Translate")
            .setItems(arrayOf("Translate copied text", "Translate draft before cursor")) { _, which ->
                if (which == 0) translateClipboard() else translateDraft()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun translateClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val sourceText = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (sourceText.isBlank()) { Toast.makeText(this, "Copy text first, then tap TR → Translate copied text", Toast.LENGTH_LONG).show(); return }
        Toast.makeText(this, "Translating copied text…", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val result = TranslationClient.translate(this, sourceText)
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("GGEZ translation", result))
                Handler(Looper.getMainLooper()).post { Toast.makeText(this, "Translated text copied. Paste it into your reply.", Toast.LENGTH_LONG).show() }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { Toast.makeText(this, e.message ?: "Translation failed", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun translateDraft() {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(5000, 0)?.toString().orEmpty()
        val start = before.lastIndexOf('\n').let { if (it < 0) 0 else it + 1 }
        val draft = before.substring(start)
        if (draft.isBlank()) { Toast.makeText(this, "Type a draft first", Toast.LENGTH_SHORT).show(); return }
        Toast.makeText(this, "Translating draft…", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val result = TranslationClient.translate(this, draft)
                Handler(Looper.getMainLooper()).post {
                    val current = currentInputConnection
                    if (current != null) {
                        current.deleteSurroundingText(draft.length, 0)
                        current.commitText(result, 1)
                        updateSuggestions()
                        Toast.makeText(this, "Draft translated. Review it before sending.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { Toast.makeText(this, e.message ?: "Translation failed", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun insertSelectedImage() {
        val uriText = prefs.getString(GgezPrefs.IMAGE_URI, "").orEmpty()
        if (uriText.isBlank()) {
            Toast.makeText(this, "Choose an image in the GGEZ dashboard first", Toast.LENGTH_LONG).show()
            val launch = packageManager.getLaunchIntentForPackage(packageName)
            launch?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            if (launch != null) startActivity(launch)
            return
        }
        val uri = Uri.parse(uriText)
        val mime = try { contentResolver.getType(uri) ?: "image/*" } catch (_: Exception) { "image/*" }
        val ic = currentInputConnection ?: return
        if (Build.VERSION.SDK_INT >= 25) {
            try {
                val info = InputContentInfo(uri, ClipDescription("GGEZ image", arrayOf(mime)), null)
                val accepted = ic.commitContent(info, InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, Bundle())
                if (!accepted) Toast.makeText(this, "This app does not accept images from keyboards. Try the app's attach button.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) { Toast.makeText(this, "Could not insert image: ${e.message}", Toast.LENGTH_LONG).show() }
        } else {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newUri(contentResolver, "GGEZ image", uri))
            Toast.makeText(this, "Image copied. Open the app's paste menu; image paste support varies by app.", Toast.LENGTH_LONG).show()
        }
    }

    private fun codeSuggestions(raw: String): List<String> {
        val prefix = raw.lowercase()
        val imported = try { JSONObject(prefs.getString(GgezPrefs.PLUGIN_SNIPPETS, "") ?: "") } catch (_: Exception) { JSONObject() }
        val snippetKeys = imported.keys().asSequence().filter { it.startsWith(prefix, true) }.take(3).toList()
        val selectedLanguage = prefs.getString(GgezPrefs.CODE_LANGUAGE, "Auto-detect") ?: "Auto-detect"
        val language = if (selectedLanguage == "Auto-detect") inferCodeLanguage() else selectedLanguage
        val languageWords = when (language) {
            "Python" -> listOf("def", "class", "self", "None", "True", "False", "elif", "lambda", "import", "from", "with", "yield", "print", "async", "await", "try", "except", "finally", "return")
            "JavaScript", "TypeScript" -> listOf("const", "let", "var", "function", "return", "console.log", "async", "await", "export", "import", "interface", "type", "undefined", "null", "true", "false", "class", "new", "this", "try", "catch")
            "Kotlin" -> listOf("fun", "val", "var", "class", "object", "companion", "override", "suspend", "when", "is", "in", "null", "true", "false", "println", "package", "import", "return")
            "Java" -> listOf("public", "private", "static", "void", "class", "interface", "extends", "implements", "new", "return", "String", "int", "boolean", "null", "true", "false", "System.out.println", "try", "catch")
            "HTML" -> listOf("html", "head", "body", "div", "span", "button", "input", "script", "style", "class", "id", "href", "src", "meta", "title")
            "CSS" -> listOf("display", "flex", "grid", "position", "relative", "absolute", "color", "background", "margin", "padding", "border", "width", "height", "font-size")
            "Lua" -> listOf("local", "function", "end", "then", "elseif", "nil", "true", "false", "return", "require", "pairs", "ipairs", "print", "self")
            "SQL" -> listOf("SELECT", "FROM", "WHERE", "JOIN", "ON", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE", "CREATE", "TABLE", "ORDER BY", "GROUP BY", "LIMIT")
            else -> codeWords
        }
        val words = languageWords + prefs.getString(GgezPrefs.PLUGIN_WORDS, "")!!.split(Regex("\\s+")).filter { it.isNotBlank() }
        val matched = words.filter { it.startsWith(prefix, true) }.distinct().take(3)
        return (snippetKeys + matched).distinct().take(3).ifEmpty { listOf("if", "fun", "return") }
    }

    private fun inferCodeLanguage(): String {
        val text = currentInputConnection?.getTextBeforeCursor(240, 0)?.toString().orEmpty()
        return when {
            text.contains("console.") || text.contains("const ") || text.contains("=>") -> "JavaScript"
            text.contains("interface ") || text.contains(": string") || text.contains(": number") -> "TypeScript"
            text.contains("fun ") || text.contains("val ") || text.contains("println(") -> "Kotlin"
            text.contains("public class ") || text.contains("System.out.") -> "Java"
            text.contains("def ") || text.contains("elif ") || text.contains("print(") -> "Python"
            text.contains("<html") || text.contains("<div") || text.contains("</") -> "HTML"
            text.contains("SELECT ") || text.contains("FROM ") || text.contains("WHERE ") -> "SQL"
            text.contains("local ") || text.contains("then") && text.contains("end") -> "Lua"
            text.contains("{ ") && text.contains("color:") || text.contains("display:") -> "CSS"
            else -> "Auto-detect"
        }
    }

    private fun replaceCurrentWord(word: String) {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(100, 0)?.toString().orEmpty()
        val current = before.takeLastWhile { it.isLetter() || it == '\'' || it == '_' }
        if (current.isNotEmpty()) ic.deleteSurroundingText(current.length, 0)
        ic.commitText(matchCase(current, word), 1); updateSuggestions()
    }
    private fun matchCase(original: String, suggestion: String): String = when { original.isNotEmpty() && original.all { !it.isLetter() || it.isUpperCase() } -> suggestion.uppercase(); original.firstOrNull()?.isUpperCase() == true -> suggestion.replaceFirstChar { it.uppercase() }; else -> suggestion }
    private fun suggest(raw: String): List<String> {
        val word = raw.lowercase()
        val imported = prefs.getString(GgezPrefs.PLUGIN_WORDS, "")?.split(Regex("\\s+"))?.filter { it.isNotBlank() } ?: emptyList()
        val words = (baseWords + imported).toList()
        val results = linkedSetOf<String>()
        if (prefs.getBoolean(GgezPrefs.AUTOCORRECT, true)) commonFixes[word]?.let { results.add(it) }
        if (words.contains(word)) results.add(raw)
        if (word.length >= 2) words.asSequence().filter { abs(it.length - word.length) <= 1 }.map { it to editDistanceAtMostTwo(word, it) }.filter { it.second <= if (word.length <= 4) 1 else 2 }.sortedWith(compareBy<Pair<String, Int>> { it.second }.thenBy { if (it.first.startsWith(word.first())) 0 else 1 }.thenBy { it.first.length }).take(8).forEach { results.add(it.first) }
        if (results.isEmpty()) results.add(raw)
        return results.take(3).map { matchCase(raw, it) }
    }
    private fun editDistanceAtMostTwo(a: String, b: String): Int {
        if (abs(a.length - b.length) > 2) return 3
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }; for (i in 0..a.length) dp[i][0] = i; for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) { var rowMin = Int.MAX_VALUE; for (j in 1..b.length) { val cost = if (a[i - 1] == b[j - 1]) 0 else 1; dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost); rowMin = minOf(rowMin, dp[i][j]) }; if (rowMin > 2) return 3 }
        return dp[a.length][b.length]
    }
    private fun dp(value: Int) = (resources.displayMetrics.density * value).toInt()
}
