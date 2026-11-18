package com.ggez.keyboard

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Sends text only when explicitly requested by the user. Endpoint must be HTTPS. */
object TranslationClient {
    fun translate(context: Context, text: String): String {
        require(text.isNotBlank()) { "There is no text to translate." }
        require(text.length <= 5000) { "Please translate 5,000 characters or fewer at a time." }
        val prefs = context.getSharedPreferences(GgezPrefs.FILE, Context.MODE_PRIVATE)
        val endpoint = prefs.getString(GgezPrefs.TRANSLATE_ENDPOINT, "").orEmpty().trim()
        require(endpoint.startsWith("https://", true)) { "Set an HTTPS translation endpoint in the GGEZ dashboard first." }
        val source = prefs.getString(GgezPrefs.TRANSLATE_SOURCE, "auto").orEmpty().ifBlank { "auto" }
        val target = prefs.getString(GgezPrefs.TRANSLATE_TARGET, "en").orEmpty().ifBlank { "en" }
        val key = prefs.getString(GgezPrefs.TRANSLATE_KEY, "").orEmpty()
        val payload = JSONObject().put("q", text).put("source", source).put("target", target).put("format", "text")
        if (key.isNotBlank()) payload.put("api_key", key)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            require(status in 200..299) { "Translation service returned HTTP $status${if (body.isNotBlank()) ": ${body.take(180)}" else ""}" }
            val translated = JSONObject(body).optString("translatedText", "")
            require(translated.isNotBlank()) { "The service response did not contain translatedText. Use a LibreTranslate-compatible endpoint." }
            return translated
        } finally { connection.disconnect() }
    }
}
