package com.beigel.list2share.ui.components

import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Profilbild aus dem Google-Konto.
 *
 * Bewusst ohne Bildbibliothek: es geht um genau ein kleines, rundes Bild an
 * einer Stelle. Coil dafür einzubinden wäre eine Abhängigkeit mehr, die
 * gepflegt und aktuell gehalten werden will. Kommen später Bilder an mehreren
 * Stellen dazu – etwa Avatare pro Mitglied –, ist der Wechsel auf Coil fällig;
 * dann ersetzt `AsyncImage` genau die eine Zeile unten.
 *
 * Schlägt das Laden fehl oder gibt es kein Bild, bleibt es beim Buchstaben.
 * Ein Platzhalter-Symbol wäre hier schlechter: der Anfangsbuchstabe sagt mehr
 * als eine graue Silhouette.
 */
private object AvatarCache {

    /** Ein paar Bilder reichen: es geht um den eigenen Avatar, nicht um eine Galerie. */
    private val cache = LruCache<String, ImageBitmap>(4)

    /**
     * Google liefert das Bild in der angefragten Größe, wenn man `=sNN` an die
     * URL hängt. Ohne das kommt ein 96er zurück, das auf großen Avataren
     * sichtbar unscharf ist.
     */
    private fun sized(url: String, px: Int): String =
        if (url.contains("=s")) url.substringBefore("=s") + "=s$px" else "$url=s$px"

    suspend fun load(rawUrl: String, px: Int): ImageBitmap? {
        val url = sized(rawUrl, px)
        cache[url]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5_000
                    readTimeout = 5_000
                }
                val bitmap = connection.inputStream.use { BitmapFactory.decodeStream(it) }
                connection.disconnect()

                bitmap?.asImageBitmap()?.also { cache.put(url, it) }
            } catch (e: Exception) {
                // Kein Netz, kaputte URL, Server weg: der Buchstabe tut es auch.
                Log.w("Avatar", "Profilbild nicht geladen", e)
                null
            }
        }
    }
}

/**
 * Runder Avatar: Profilbild, wenn eines vorliegt, sonst der Anfangsbuchstabe.
 *
 * @param photoUrl Bild-URL des Kontos, null ohne Google-Anmeldung.
 * @param fallback Text, aus dem der Buchstabe gebildet wird.
 */
@Composable
fun Avatar(
    photoUrl: String?,
    fallback: String,
    size: Dp,
    fontSize: TextUnit,
) {
    var image by remember(photoUrl) { mutableStateOf<ImageBitmap?>(null) }
    val density = LocalDensity.current

    LaunchedEffect(photoUrl, size) {
        val url = photoUrl
        if (url.isNullOrBlank()) {
            image = null
            return@LaunchedEffect
        }
        val px = with(density) { size.roundToPx() }
        image = AvatarCache.load(url, px)
        if (image == null) Log.i("Avatar", "Kein Bild geladen für $url")
    }

    Box(
        modifier = Modifier.size(size).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = fallback.take(1).uppercase().ifEmpty { "?" },
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
