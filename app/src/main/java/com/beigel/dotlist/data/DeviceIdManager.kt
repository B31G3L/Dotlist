package com.beigel.dotlist.data

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
    private const val KEY_NAME_SET = "device_name_set"

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
        prefs(context).edit()
            .putString(KEY_DEVICE_NAME, name)
            .putBoolean(KEY_NAME_SET, true)
            .apply()
    }

    /**
     * True, sobald der Nutzer seinen Namen einmal explizit gesetzt hat
     * (z. B. im Willkommens-Screen oder beim Google-Login).
     * Steuert, ob der Willkommens-Screen beim App-Start noch angezeigt wird.
     */
    fun isNameSet(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NAME_SET, false)

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}