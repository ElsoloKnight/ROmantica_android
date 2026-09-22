package com.example.app_romantica

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Calendar

class MenuActivity : AppCompatActivity() {

    private lateinit var miRol: String
    private val handler = Handler(Looper.getMainLooper())
    private val intervaloMs = 5000L
    private var activo = false

    private val refrescar = object : Runnable {
        override fun run() {
            cargarContadores()
            actualizarFondoPorHora()
            if (activo) handler.postDelayed(this, intervaloMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        miRol = intent.getStringExtra("rol") ?: Prefs.getRol(this) ?: "juan_carlos"

        actualizarFondoPorHora()

        findViewById<LinearLayout>(R.id.cardTeExtrano).setOnClickListener { incrementar("te_extrano", R.id.numeroTeExtrano) }
        findViewById<LinearLayout>(R.id.cardTomaAgua).setOnClickListener { incrementar("toma_agua", R.id.numeroTomaAgua) }
        findViewById<LinearLayout>(R.id.cardTeAmo).setOnClickListener { incrementar("te_amo", R.id.numeroTeAmo) }

        // Nuevos contadores si existen en el layout
        findViewById<LinearLayout>(R.id.cardBuenDia)?.setOnClickListener { incrementar("buen_dia", R.id.numeroBuenDia) }
        findViewById<LinearLayout>(R.id.cardTeAdmira)?.setOnClickListener { incrementar("te_admira", R.id.numeroTeAdmira) }

        findViewById<LinearLayout>(R.id.cardGaleria).setOnClickListener {
            startActivity(Intent(this, GaleriaActivity::class.java).putExtra("rol", miRol))
        }
        findViewById<LinearLayout>(R.id.cardBuzon).setOnClickListener {
            startActivity(Intent(this, BuzonActivity::class.java).putExtra("rol", miRol))
        }
        findViewById<LinearLayout>(R.id.cardEstado).setOnClickListener {
            startActivity(Intent(this, EstadoActivity::class.java).putExtra("rol", miRol))
        }
        findViewById<android.widget.Button>(R.id.btnBotonVolver).setOnClickListener { finish() }
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

    private fun actualizarFondoPorHora() {
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val esDia = hora in 7..18 // 7 AM a 6:59 PM
        
        val colorFondo = if (esDia) R.color.rosa_fondo else R.color.rosa_fondo_noche
        val colorTextoPrincipal = if (esDia) R.color.gris_oscuro else R.color.white
        val colorTextoSecundario = if (esDia) R.color.gris_texto else R.color.rosa_seleccion

        findViewById<ScrollView>(R.id.scrollRoot)?.setBackgroundColor(ContextCompat.getColor(this, colorFondo))
        findViewById<TextView>(R.id.tvParaTi)?.setTextColor(ContextCompat.getColor(this, colorTextoPrincipal))
        findViewById<TextView>(R.id.tvContadores)?.setTextColor(ContextCompat.getColor(this, colorTextoSecundario))
        findViewById<TextView>(R.id.tvRecuerdos)?.setTextColor(ContextCompat.getColor(this, colorTextoSecundario))
    }

    private fun cargarContadores() {
        lifecycleScope.launch {
            try {
                val data = Api.getJson("/contadores") as JSONObject
                findViewById<TextView>(R.id.numeroTeExtrano).text = data.optInt("te_extrano").toString()
                findViewById<TextView>(R.id.numeroTomaAgua).text = data.optInt("toma_agua").toString()
                findViewById<TextView>(R.id.numeroTeAmo).text = data.optInt("te_amo").toString()
                findViewById<TextView>(R.id.numeroBuenDia)?.text = data.optInt("buen_dia").toString()
                findViewById<TextView>(R.id.numeroTeAdmira)?.text = data.optInt("te_admira").toString()
            } catch (_: Exception) { }
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
