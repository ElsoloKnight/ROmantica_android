package com.example.app_romantica

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class GaleriaActivity : AppCompatActivity() {

    private lateinit var miRol: String
    private lateinit var adaptador: FotosAdapter
    private val handler = Handler(Looper.getMainLooper())
    private val intervaloMs = 10000L
    private var activo = false

    private val refrescar = object : Runnable {
        override fun run() {
            cargarFotos()
            if (activo) handler.postDelayed(this, intervaloMs)
        }
    }

    private val selectorImagen = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) subirFoto(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_galeria)
        miRol = intent.getStringExtra("rol") ?: Prefs.getRol(this) ?: "juan_carlos"

        findViewById<TextView>(R.id.botonVolver).setOnClickListener { finish() }
        findViewById<Button>(R.id.botonSubir).setOnClickListener {
            selectorImagen.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        adaptador = FotosAdapter(onLongClick = { foto -> descargarYCompartir(foto) })
        findViewById<RecyclerView>(R.id.gridFotos).apply {
            layoutManager = GridLayoutManager(this@GaleriaActivity, 3)
            adapter = adaptador
        }

        findViewById<SwipeRefreshLayout>(R.id.swipeRefresh).setOnRefreshListener { cargarFotos() }
    }

    override fun onStart() {
        super.onStart()
        activo = true
        handler.post(refrescar)
    }

    override fun onStop() {
        super.onStop()
        activo = false
        handler.removeCallbacks(refrescar)
    }

    private fun cargarFotos() {
        lifecycleScope.launch {
            try {
                val data = Api.getJson("/fotos") as JSONArray
                val lista = (0 until data.length()).map { data.getJSONObject(it) }
                adaptador.actualizar(lista)
            } catch (_: Exception) {
                // Sin conexión: se mantiene la última lista cargada.
            } finally {
                findViewById<SwipeRefreshLayout>(R.id.swipeRefresh).isRefreshing = false
            }
        }
    }

    private fun subirFoto(uri: Uri) {
        lifecycleScope.launch {
            try {
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val extension = if (mimeType.contains("png")) "png" else "jpg"
                val nombreOriginal = nombreDeArchivo(uri) ?: "foto.$extension"
                val archivoTemporal = File(cacheDir, "subida_$nombreOriginal")
                contentResolver.openInputStream(uri)?.use { input ->
                    archivoTemporal.outputStream().use { output -> input.copyTo(output) }
                }

                val codigo = Api.subirFoto("/fotos", archivoTemporal, mimeType, miRol)
                archivoTemporal.delete()
                if (codigo !in 200..299) throw Exception("El servidor respondió $codigo.")
                cargarFotos()
            } catch (e: Exception) {
                Toast.makeText(this@GaleriaActivity, "Error subiendo foto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun nombreDeArchivo(uri: Uri): String? {
        var nombre: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val indice = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (indice != -1 && cursor.moveToFirst()) nombre = cursor.getString(indice)
        }
        return nombre
    }

    private fun descargarYCompartir(foto: JSONObject) {
        lifecycleScope.launch {
            try {
                val url = "${Api.BASE_URL}${foto.optString("url")}"
                val extension = url.substringAfterLast('.').substringBefore('?').ifBlank { "jpg" }
                val destino = File(cacheDir, "app-romantica-${foto.optInt("id")}.$extension")
                Api.descargarArchivo(url, destino)

                val uriCompartible = FileProvider.getUriForFile(this@GaleriaActivity, "$packageName.fileprovider", destino)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = if (extension == "png") "image/png" else "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uriCompartible)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Descargar foto"))
            } catch (_: Exception) {
                Toast.makeText(this@GaleriaActivity, "No se pudo descargar. Comprueba la conexión.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
