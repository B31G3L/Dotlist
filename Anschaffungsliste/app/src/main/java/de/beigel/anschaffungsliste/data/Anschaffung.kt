package de.beigel.anschaffungsliste.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "anschaffungen")
class Anschaffung {
    @PrimaryKey
    var id: String = ""
    var name: String = ""
    var preis: Double = 0.0
    var link: String = ""
    var prioritaet: Int = 0
    var erstellt: Long = 0L
    var erledigt: Boolean = false
    var listenId: String = "default"
    var erstelltVon: String = ""

    // Hauptkonstruktor für Room (parametlos)
    constructor()

    // Ignore this constructor für Room
    @Ignore
    constructor(
        name: String,
        preis: Double,
        link: String = "",
        prioritaet: Int = 0,
        listenId: String = "default"
    ) {
        this.id = java.util.UUID.randomUUID().toString()
        this.name = name
        this.preis = preis
        this.link = link
        this.prioritaet = prioritaet
        this.erstellt = System.currentTimeMillis()
        this.erledigt = false
        this.listenId = listenId
        this.erstelltVon = ""
    }

    // Formatierter Preis für UI
    fun formatierterPreis(): String {
        return "€ %.2f".format(preis)
    }

    // Kurzer Link für UI (erste 30 Zeichen)
    fun kurzerLink(): String {
        return if (link.length > 30) {
            "${link.take(30)}..."
        } else {
            link
        }
    }

    // Copy-Methode für Immutability-ähnliche Verwendung
    fun copy(
        id: String = this.id,
        name: String = this.name,
        preis: Double = this.preis,
        link: String = this.link,
        prioritaet: Int = this.prioritaet,
        erstellt: Long = this.erstellt,
        erledigt: Boolean = this.erledigt,
        listenId: String = this.listenId,
        erstelltVon: String = this.erstelltVon
    ): Anschaffung {
        val neue = Anschaffung()
        neue.id = id
        neue.name = name
        neue.preis = preis
        neue.link = link
        neue.prioritaet = prioritaet
        neue.erstellt = erstellt
        neue.erledigt = erledigt
        neue.listenId = listenId
        neue.erstelltVon = erstelltVon
        return neue
    }
}