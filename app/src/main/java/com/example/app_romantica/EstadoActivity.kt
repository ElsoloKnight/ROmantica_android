package com.example.app_romantica

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.app_romantica.databinding.ActivityEstadoBinding
import kotlinx.coroutines.launch
import org.json.JSONObject

// 1. Enums con su valor de serialización para la API
enum class Animo(val clave: String) {
    TRISTE("triste"),
    NORMAL("normal"),
    FELIZ("feliz");

    companion object {
        fun desde(clave: String?): Animo = entries.find { it.clave == clave } ?: NORMAL
    }
}

enum class Estres(val clave: String) {
    RELAJADO("relajado"),
    NORMAL("normal"),
    ESTRESADO("estresado");

    companion object {
        fun desde(clave: String?): Estres = entries.find { it.clave == clave } ?: NORMAL
    }
}

class EstadoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEstadoBinding
    private lateinit var usuario: String

    private var animoActual = Animo.NORMAL
    private var estresActual = Estres.NORMAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEstadoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usuario = intent.getStringExtra("rol") ?: Prefs.getRol(this) ?: "juan_carlos"

        configurarListeners()
        cargarEstado()
    }

    private fun configurarListeners() {
        binding.btnBotonVolver.setOnClickListener { finish() }

        // Opciones de Ánimo
        binding.opcionTriste.setOnClickListener { actualizarYGuardar(nuevoAnimo = Animo.TRISTE) }
        binding.opcionNormalAnimo.setOnClickListener { actualizarYGuardar(nuevoAnimo = Animo.NORMAL) }
        binding.opcionFeliz.setOnClickListener { actualizarYGuardar(nuevoAnimo = Animo.FELIZ) }

        // Opciones de Estrés
        binding.opcionRelajado.setOnClickListener { actualizarYGuardar(nuevoEstres = Estres.RELAJADO) }
        binding.opcionNormalEstres.setOnClickListener { actualizarYGuardar(nuevoEstres = Estres.NORMAL) }
        binding.opcionEstresado.setOnClickListener { actualizarYGuardar(nuevoEstres = Estres.ESTRESADO) }
    }

    private fun actualizarYGuardar(
        nuevoAnimo: Animo = animoActual,
        nuevoEstres: Estres = estresActual
    ) {
        animoActual = nuevoAnimo
        estresActual = nuevoEstres
        actualizarSeleccionVisual()

        binding.textoInfo.visibility = View.VISIBLE
        binding.textoError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val body = JSONObject().apply {
                    put("usuario", usuario)
                    put("animo", animoActual.clave)
                    put("estres", estresActual.clave)
                }
                Api.postJson("/estado", body)
            } catch (_: Exception) {
                // Mensaje desde strings.xml para evitar hardcodeo
                binding.textoError.setText(R.string.error_servidor_estado)
                binding.textoError.visibility = View.VISIBLE
            } finally {
                binding.textoInfo.visibility = View.GONE
            }
        }
    }

    private fun cargarEstado() {
        lifecycleScope.launch {
            try {
                val data = Api.getJson("/estado/$usuario") as JSONObject
                animoActual = Animo.desde(data.optString("animo"))
                estresActual = Estres.desde(data.optString("estres"))
                actualizarSeleccionVisual()
            } catch (_: Exception) {
                // Sin conexión o sin endpoint: mantiene el estado local inicial
                actualizarSeleccionVisual()
            }
        }
    }

    private fun actualizarSeleccionVisual() {
        val colorSeleccionado = ContextCompat.getColor(this, R.color.rosa_seleccion)
        val colorBlanco = ContextCompat.getColor(this, R.color.white)

        // Mapeo directo Vista -> Valor para iterar limpiamente
        val vistasAnimo = mapOf(
            binding.opcionTriste to Animo.TRISTE,
            binding.opcionNormalAnimo to Animo.NORMAL,
            binding.opcionFeliz to Animo.FELIZ
        )

        val vistasEstres = mapOf(
            binding.opcionRelajado to Estres.RELAJADO,
            binding.opcionNormalEstres to Estres.NORMAL,
            binding.opcionEstresado to Estres.ESTRESADO
        )

        vistasAnimo.forEach { (layout, valor) ->
            layout.setBackgroundColor(if (animoActual == valor) colorSeleccionado else colorBlanco)
        }

        vistasEstres.forEach { (layout, valor) ->
            layout.setBackgroundColor(if (estresActual == valor) colorSeleccionado else colorBlanco)
        }

        // Formato tipado usando strings.xml con placeholders (%1$s / %2$s)
        binding.textoEstadoActual.text = getString(
            R.string.formato_estado_actual,
            animoActual.clave,
            estresActual.clave
        )
    }
}