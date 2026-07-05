package de.beigel.todo.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Verwaltet die anonyme Geräte-ID.
 * Diese wird beim ersten Start generiert und lokal gespeichert.
 * Sie identifiziert den Nutzer ohne Login.
 */
object DeviceIdManager {

    private const val PREFS_NAME = "device_prefs"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_DEVICE_NAME = "device_name"

    fun getDeviceId(context: Context): String {
        val prefs = prefs(context)
        return prefs.getString(KEY_DEVICE_ID, null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
            newId
        }
    }

    fun getDeviceName(context: Context): String {
        val prefs = prefs(context)
        return prefs.getString(KEY_DEVICE_NAME, null) ?: android.os.Build.MODEL
    }

    fun setDeviceName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_DEVICE_NAME, name).apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
