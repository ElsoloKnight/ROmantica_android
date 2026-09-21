package com.example.app_romantica

import android.content.Context

// Reemplaza a AsyncStorage del proyecto Expo original.
object Prefs {
    private const val FILE = "app_romantica_prefs"
    private const val KEY_ROL = "rol_usuario"

    fun getRol(context: Context): String? =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_ROL, null)

    fun setRol(context: Context, rol: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY_ROL, rol).apply()
    }
}
