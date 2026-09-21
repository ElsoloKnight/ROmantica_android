package com.example.app_romantica

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.app_romantica.databinding.ActivityBuzonBinding
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class BuzonActivity : AppCompatActivity() {

    private companion object {
        const val ENDPOINT_MENSAJES = "/mensajes"
        const val KEY_ID = "id"
        const val KEY_TEXTO = "texto"
        const val KEY_REMITENTE = "remitente"
        const val KEY_ROL = "rol"
        const val INTERVALO_POLLING_MS = 3000L
        const val BURBUJA_PADDING_DP = 15
        const val BURBUJA_MARGIN_BOTTOM_DP = 15
    }

    private lateinit var binding: ActivityBuzonBinding
    private lateinit var miRol: String

    private val idsMostrados = mutableSetOf<Int>()
    private val handler = Handler(Looper.getMainLooper())
    private var activo = false

    private val refrescar = object : Runnable {
        override fun run() {
            cargarMensajes()
            if (activo) handler.postDelayed(this, INTERVALO_POLLING_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBuzonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        miRol = intent.getStringExtra(KEY_ROL) ?: Prefs.getRol(this) ?: "juan_carlos"

        configurarListeners()
    }

    private fun configurarListeners() {
        // Asignar volver al botón correspondiente (puedes ajustar a binding.botonVolver o tu ID de flecha)
        binding.tvBotonVolver.setOnClickListener { finish() }

        binding.btnEnviarMensjae.setOnClickListener { enviarMensaje() }
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

    private fun cargarMensajes() {
        lifecycleScope.launch {
            try {
                val data = Api.getJson(ENDPOINT_MENSAJES) as JSONArray
                for (i in 0 until data.length()) {
                    agregarBurbuja(data.getJSONObject(i))
                }
            } catch (_: Exception) {
                // Sin conexión: se conservan los mensajes ya cargados.
            }
        }
    }

    private fun agregarBurbuja(msg: JSONObject) {
        val id = msg.optInt(KEY_ID, -1)
        if (id != -1 && !idsMostrados.add(id)) return

        val esMio = msg.optString(KEY_REMITENTE) == miRol
        val paddingPx = dp(BURBUJA_PADDING_DP)

        val burbuja = TextView(this).apply {
            text = msg.optString(KEY_TEXTO)
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            setTextColor(
                ContextCompat.getColor(
                    this@BuzonActivity,
                    if (esMio) R.color.white else R.color.gris_medio
                )
            )
            background = ContextCompat.getDrawable(
                this@BuzonActivity,
                if (esMio) R.drawable.burbuja_mia else R.drawable.burbuja_suya
            )
        }

        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (esMio) Gravity.END else Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(BURBUJA_MARGIN_BOTTOM_DP)
            }
            addView(burbuja)
        }

        // Acceso directo a vistas sin findViewById
        binding.contenedorMensajes.addView(fila)
        binding.scrollChat.post {
            binding.scrollChat.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun enviarMensaje() {
        val input = binding.etInputMensaje
        val texto = input.text.toString().trim()
        if (texto.isEmpty()) return

        lifecycleScope.launch {
            try {
                val body = JSONObject().apply {
                    put(KEY_TEXTO, texto)
                    put(KEY_REMITENTE, miRol)
                }
                val data = Api.postJson(ENDPOINT_MENSAJES, body)
                agregarBurbuja(data)
                input.setText("")
            } catch (_: Exception) {
                // El polling reintentará mostrar el estado real del servidor.
            }
        }
    }

    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()
}