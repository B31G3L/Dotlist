package com.beigel.list2share.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.beigel.list2share.ui.theme.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Einmalige Hinweise, die der Nutzer wegklicken kann.
 *
 * Bewusst dauerhaft weg und nicht „in drei Tagen nochmal": ein Hinweis, den
 * man schon gelesen und bewusst geschlossen hat, ein zweites Mal zu zeigen,
 * ist Drängeln.
 */
object HintPreferences {

    private val LOCAL_ONLY_DISMISSED = booleanPreferencesKey("hint_local_only_dismissed")

    fun localOnlyDismissed(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[LOCAL_ONLY_DISMISSED] ?: false }

    suspend fun dismissLocalOnly(context: Context) {
        context.dataStore.edit { it[LOCAL_ONLY_DISMISSED] = true }
    }
}
