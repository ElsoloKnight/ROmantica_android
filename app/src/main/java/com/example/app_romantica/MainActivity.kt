package com.example.app_romantica

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.app_romantica.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var rolSeleccionado = "juan_carlos"
    private var puedeCambiarFrase = false

    private val pedirPermisoNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        crearCanalNotificaciones()
        pedirPermisoNotificacionesSiHaceFalta()

        binding.btnBonGuardarFrase.setOnClickListener { guardarFrase() }

        val rolGuardado = Prefs.getRol(this)
        if (rolGuardado != null) {
            mostrarBienvenida()
            cargarFraseDelDia()
        } else {
            mostrarConfiguracion()
        }
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                "romantica-default",
                "Romantica",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    private fun pedirPermisoNotificacionesSiHaceFalta() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val yaConcedido = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!yaConcedido) {
                pedirPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun mostrarBienvenida() {
        binding.grupoConfig.visibility = View.GONE
        binding.grupoBienvenida.visibility = View.VISIBLE
        binding.grupoCambioFrase.visibility = if (Prefs.getRol(this) == "juan_carlos") View.VISIBLE else View.GONE

        binding.btnBotonTeQuiero.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java).putExtra("rol", Prefs.getRol(this)))
        }
    }

    private fun actualizarUiCambioFrase() {
        binding.inputFrase.visibility = if (puedeCambiarFrase) View.VISIBLE else View.GONE
        binding.btnBonGuardarFrase.visibility = if (puedeCambiarFrase) View.VISIBLE else View.GONE
        binding.tvTextoAviso.visibility = if (puedeCambiarFrase) View.GONE else View.VISIBLE
    }

    private fun cargarFraseDelDia() {
        val progreso = binding.progresoFrase
        val texto = binding.textoFrase
        lifecycleScope.launch {
            try {
                val data = Api.getJson("/frase-del-dia") as JSONObject
                texto.text = "\"${data.optString("frase")}\""
                puedeCambiarFrase = data.optBoolean("puede_cambiar", false)
                actualizarUiCambioFrase()
            } catch (e: Exception) {
                texto.text = "\"Siempre pienso en ti.\""
            } finally {
                progreso.visibility = View.GONE
                texto.visibility = View.VISIBLE
            }
        }
    }

    private fun mostrarConfiguracion() {
        findViewById<View>(R.id.grupoBienvenida).visibility = View.GONE
        findViewById<View>(R.id.grupoConfig).visibility = View.VISIBLE

        val botonJuan = findViewById<Button>(R.id.botonJuanCarlos)
        val botonRoro = findViewById<Button>(R.id.botonRoro)
        val grupoCambioFrase = findViewById<View>(R.id.grupoCambioFrase)

        fun actualizarSeleccion() {
            botonJuan.setBackgroundColor(ContextCompat.getColor(this, if (rolSeleccionado == "juan_carlos") R.color.rosa_seleccion else R.color.white))
            botonRoro.setBackgroundColor(ContextCompat.getColor(this, if (rolSeleccionado == "roro") R.color.rosa_seleccion else R.color.white))
            grupoCambioFrase.visibility = if (rolSeleccionado == "juan_carlos") View.VISIBLE else View.GONE
        }

        botonJuan.setOnClickListener { rolSeleccionado = "juan_carlos"; actualizarSeleccion() }
        botonRoro.setOnClickListener { rolSeleccionado = "roro"; actualizarSeleccion() }
        actualizarSeleccion()

        consultarSiPuedeCambiarFrase()

        findViewById<Button>(R.id.btnBotonEntrar).setOnClickListener { iniciarSesion() }
    }

    private fun consultarSiPuedeCambiarFrase() {
        lifecycleScope.launch {
            try {
                val data = Api.getJson("/frase-del-dia") as JSONObject
                puedeCambiarFrase = data.optBoolean("puede_cambiar", false)
                actualizarUiCambioFrase()
            } catch (_: Exception) { }
        }
    }

    private fun guardarFrase() {
        val input = findViewById<EditText>(R.id.inputFrase)
        val textoError = findViewById<TextView>(R.id.textoErrorFrase)
        val boton = findViewById<Button>(R.id.btnBonGuardarFrase)
        val frase = input.text.toString().trim()

        if (frase.isEmpty()) {
            textoError.text = "Escribe una frase antes de guardarla."
            textoError.visibility = View.VISIBLE
            return
        }

        textoError.visibility = View.GONE
        boton.isEnabled = false
        boton.text = "Guardando..."

        lifecycleScope.launch {
            try {
                val body = JSONObject().put("texto", frase).put("rol", "juan_carlos")
                Api.postJson("/frase-del-dia", body)
                Prefs.setRol(this@MainActivity, "juan_carlos")
                startActivity(Intent(this@MainActivity, MenuActivity::class.java).putExtra("rol", "juan_carlos"))
                finish()
            } catch (e: Exception) {
                textoError.text = e.message ?: "No se pudo guardar la frase."
                textoError.visibility = View.VISIBLE
            } finally {
                boton.isEnabled = true
                boton.text = "Guardar frase"
            }
        }
    }

    private fun iniciarSesion() {
        lifecycleScope.launch {
            try {
                val body = JSONObject().put("usuario", rolSeleccionado).put("password", "")
                Api.postJson("/login", body)
            } catch (_: Exception) {
                // Login remoto opcional; seguimos con el acceso local.
            }
            Prefs.setRol(this@MainActivity, rolSeleccionado)
            startActivity(Intent(this@MainActivity, MenuActivity::class.java).putExtra("rol", rolSeleccionado))
            finish()
        }
    }
}
