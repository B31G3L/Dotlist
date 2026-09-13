package com.beigel.list2share.data

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Preisformat für den Anschaffungsmodus.
 *
 * Feste Währung Euro, aber mit den Trennzeichen der Gerätesprache – eine
 * Währungsauswahl wäre ein Feld an der Liste und ein Einstellungsdialog, und
 * für einen gemeinsamen Haushalt gibt es ohnehin nur eine Währung.
 *
 * Das Format wird bei jedem Aufruf neu erzeugt: NumberFormat ist nicht
 * threadsicher, und beim Wechsel der Systemsprache wäre eine zwischen-
 * gespeicherte Instanz sonst veraltet.
 */
fun formatPrice(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        currency = Currency.getInstance("EUR")
        maximumFractionDigits = if (value % 1.0 == 0.0) 0 else 2
    }.format(value)
