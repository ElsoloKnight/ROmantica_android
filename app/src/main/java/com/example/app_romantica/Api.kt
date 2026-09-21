package com.example.app_romantica

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Cliente HTTP simple hacia el backend FastAPI (mismo API_URL que la app Expo original).
 */
object Api {
    const val BASE_URL = "https://api-romantica.onrender.com"
    private const val TIMEOUT_MS = 15000

    suspend fun getJson(path: String): Any = withContext(Dispatchers.IO) {
        val conn = (URL("$BASE_URL$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }
        try {
            val code = conn.responseCode
            val texto = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw Exception("HTTP $code")
            parseJson(texto)
        } finally {
            conn.disconnect()
        }
    }

    suspend fun postJson(path: String, body: JSONObject, headers: Map<String, String> = emptyMap()): JSONObject =
        withContext(Dispatchers.IO) {
            val conn = (URL("$BASE_URL$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json")
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            try {
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = conn.responseCode
                val texto = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: "{}"
                val json = if (texto.isBlank()) JSONObject() else JSONObject(texto)
                if (code !in 200..299) throw Exception(json.optString("detail", "HTTP $code"))
                json
            } finally {
                conn.disconnect()
            }
        }

    suspend fun postSinCuerpo(path: String, headers: Map<String, String> = emptyMap()): JSONObject =
        withContext(Dispatchers.IO) {
            val conn = (URL("$BASE_URL$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
                setFixedLengthStreamingMode(0)
            }
            try {
                conn.outputStream.close()
                val code = conn.responseCode
                val texto = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: "{}"
                val json = if (texto.isBlank()) JSONObject() else JSONObject(texto)
                if (code !in 200..299) throw Exception("HTTP $code")
                json
            } finally {
                conn.disconnect()
            }
        }

    suspend fun subirFoto(path: String, archivo: File, mimeType: String, usuario: String): Int =
        withContext(Dispatchers.IO) {
            val boundary = "----AppRomantica${UUID.randomUUID()}"
            val conn = (URL("$BASE_URL$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("X-Usuario", usuario)
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }
            try {
                conn.outputStream.use { out ->
                    out.write("--$boundary\r\n".toByteArray())
                    out.write("Content-Disposition: form-data; name=\"foto\"; filename=\"${archivo.name}\"\r\n".toByteArray())
                    out.write("Content-Type: $mimeType\r\n\r\n".toByteArray())
                    out.write(archivo.readBytes())
                    out.write("\r\n--$boundary--\r\n".toByteArray())
                }
                conn.responseCode
            } finally {
                conn.disconnect()
            }
        }

    suspend fun descargarArchivo(url: String, destino: File): File = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }
        try {
            if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode}")
            conn.inputStream.use { input ->
                FileOutputStream(destino).use { output -> input.copyTo(output) }
            }
            destino
        } finally {
            conn.disconnect()
        }
    }

    private fun parseJson(texto: String): Any =
        if (texto.trim().startsWith("[")) JSONArray(texto) else JSONObject(texto)
}
