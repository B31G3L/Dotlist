package de.beigel.anschaffungsliste.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AnschaffungDao {

    @Query("SELECT * FROM anschaffungen WHERE listenId = :listenId ORDER BY prioritaet ASC, erstellt DESC")
    fun getAlleAnschaffungen(listenId: String): Flow<List<Anschaffung>>

    @Query("SELECT * FROM anschaffungen WHERE listenId = :listenId AND erledigt = 0 ORDER BY prioritaet ASC, erstellt DESC")
    fun getOffeneAnschaffungen(listenId: String): Flow<List<Anschaffung>>

    @Query("SELECT * FROM anschaffungen WHERE id = :id LIMIT 1")
    fun getAnschaffungById(id: String): Anschaffung?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAnschaffung(anschaffung: Anschaffung): Long

    @Update
    fun updateAnschaffung(anschaffung: Anschaffung): Int

    @Delete
    fun deleteAnschaffung(anschaffung: Anschaffung): Int

    @Query("UPDATE anschaffungen SET prioritaet = :neuePrioritaet WHERE id = :id")
    fun updatePrioritaet(id: String, neuePrioritaet: Int): Int

    @Query("UPDATE anschaffungen SET erledigt = :erledigt WHERE id = :id")
    fun markiereAlsErledigt(id: String, erledigt: Boolean): Int

    @Query("DELETE FROM anschaffungen WHERE listenId = :listenId")
    fun deleteAlleFuerListe(listenId: String): Int

    // Suspend versions für Coroutines (wrapper functions)
    suspend fun insertAnschaffungSuspend(anschaffung: Anschaffung): Long {
        return insertAnschaffung(anschaffung)
    }

    suspend fun updateAnschaffungSuspend(anschaffung: Anschaffung): Int {
        return updateAnschaffung(anschaffung)
    }

    suspend fun deleteAnschaffungSuspend(anschaffung: Anschaffung): Int {
        return deleteAnschaffung(anschaffung)
    }

    suspend fun updatePrioritaetSuspend(id: String, neuePrioritaet: Int): Int {
        return updatePrioritaet(id, neuePrioritaet)
    }

    suspend fun markiereAlsErledigtSuspend(id: String, erledigt: Boolean): Int {
        return markiereAlsErledigt(id, erledigt)
    }
}

@Database(
    entities = [Anschaffung::class],
    version = 1,
    exportSchema = false
)
abstract class AnschaffungDatabase : RoomDatabase() {
    abstract fun anschaffungDao(): AnschaffungDao
}