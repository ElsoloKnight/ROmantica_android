package com.example.app_romantica

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class MenuActivity : AppCompatActivity() {

    private lateinit var miRol: String
    private val handler = Handler(Looper.getMainLooper())
    private val intervaloMs = 5000L
    private var activo = false

    private val refrescar = object : Runnable {
        override fun run() {
            cargarContadores()
            if (activo) handler.postDelayed(this, intervaloMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        miRol = intent.getStringExtra("rol") ?: Prefs.getRol(this) ?: "juan_carlos"

        findViewById<LinearLayout>(R.id.cardTeExtrano).setOnClickListener { incrementar("te_extrano", R.id.numeroTeExtrano) }
        findViewById<LinearLayout>(R.id.cardTomaAgua).setOnClickListener { incrementar("toma_agua", R.id.numeroTomaAgua) }
        findViewById<LinearLayout>(R.id.cardTeAmo).setOnClickListener { incrementar("te_amo", R.id.numeroTeAmo) }

        findViewById<LinearLayout>(R.id.cardGaleria).setOnClickListener {
            startActivity(Intent(this, GaleriaActivity::class.java).putExtra("rol", miRol))
        }
        findViewById<LinearLayout>(R.id.cardBuzon).setOnClickListener {
            startActivity(Intent(this, BuzonActivity::class.java).putExtra("rol", miRol))
        }
        findViewById<LinearLayout>(R.id.cardEstado).setOnClickListener {
            startActivity(Intent(this, EstadoActivity::class.java).putExtra("rol", miRol))
        }
        findViewById<android.widget.Button>(R.id.botonVolver).setOnClickListener { finish() }
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

    private fun cargarContadores() {
        lifecycleScope.launch {
            try {
                val data = Api.getJson("/contadores") as JSONObject
                findViewById<TextView>(R.id.numeroTeExtrano).text = data.optInt("te_extrano").toString()
                findViewById<TextView>(R.id.numeroTomaAgua).text = data.optInt("toma_agua").toString()
                findViewById<TextView>(R.id.numeroTeAmo).text = data.optInt("te_amo").toString()
            } catch (_: Exception) {
                // Sin conexión: se mantiene el último valor mostrado.
            }
        }
    }

    private fun incrementar(tipo: String, idVista: Int) {
        lifecycleScope.launch {
            try {
                val data = Api.postSinCuerpo("/contadores/$tipo", mapOf("X-Usuario" to miRol))
                if (data.has("nuevo_valor")) {
                    findViewById<TextView>(idVista).text = data.optInt("nuevo_valor").toString()
                }
            } catch (_: Exception) { }
        }
    }
}
