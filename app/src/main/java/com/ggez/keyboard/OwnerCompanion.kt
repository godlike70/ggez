package com.ggez.keyboard

import android.content.Context
import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reads the owner session only through the companion's signature-protected provider.
 * The backend remains the authority: package presence or a local flag never unlocks owner access.
 */
object OwnerCompanion {
    private val sessionUri = Uri.parse("content://com.ggez.owner.session/session")

    fun verify(context: Context): Boolean {
        val resolver = context.contentResolver
        val row = try {
            resolver.query(sessionUri, null, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) null else {
                    val serverIndex = cursor.getColumnIndex("server_url")
                    val tokenIndex = cursor.getColumnIndex("owner_token")
                    val enabledIndex = cursor.getColumnIndex("enabled")
                    Triple(
                        if (serverIndex >= 0) cursor.getString(serverIndex).orEmpty() else "",
                        if (tokenIndex >= 0) cursor.getString(tokenIndex).orEmpty() else "",
                        enabledIndex >= 0 && cursor.getInt(enabledIndex) == 1
                    )
                }
            }
        } catch (_: SecurityException) { null } catch (_: Exception) { null }

        if (row == null || !row.third || !row.first.startsWith("https://") || row.second.isBlank()) return false
        return try {
            val connection = URL(row.first.trimEnd('/') + "/v1/admin/session").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Authorization", "Bearer ${row.second}")
            val success = connection.responseCode in 200..299
            connection.disconnect()
            success
        } catch (_: Exception) { false }
    }
}
