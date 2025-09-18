package de.beigel.list.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import de.beigel.list.MainActivity

/**
 * Quick-Add Widget - Ermöglicht schnelles Hinzufügen von Aufgaben
 */
class QuickAddWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: android.content.Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                QuickAddContent()
            }
        }
    }
}

@Composable
private fun QuickAddContent() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.White))
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.Start,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = "Schnell hinzufügen",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color(0xFF009966))
                ),
                modifier = GlanceModifier.defaultWeight()
            )

            Text(
                text = "⚡",
                style = TextStyle(fontSize = 18.sp)
            )
        }

        Spacer(modifier = GlanceModifier.height(12.dp))

        // Vereinfachte Quick-Tasks
        QuickTaskButton(
            text = "🔥 Wichtige Aufgabe",
            backgroundColor = ColorProvider(Color(0x1AE53E3E))
        )

        Spacer(modifier = GlanceModifier.height(6.dp))

        QuickTaskButton(
            text = "📝 Normale Aufgabe",
            backgroundColor = ColorProvider(Color(0x1A009966))
        )

        Spacer(modifier = GlanceModifier.height(6.dp))

        QuickTaskButton(
            text = "⏰ Später erledigen",
            backgroundColor = ColorProvider(Color(0x1A4A5568))
        )

        Spacer(modifier = GlanceModifier.height(12.dp))

        // Trennlinie (als Text)
        Text(
            text = "─────────────────",
            style = TextStyle(
                fontSize = 10.sp,
                color = ColorProvider(Color(0xFFCCCCCC))
            ),
            modifier = GlanceModifier.fillMaxWidth()
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Custom Add Button
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(Color(0x1A009966)))
                .padding(12.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text(
                text = "➕",
                style = TextStyle(
                    fontSize = 16.sp,
                    color = ColorProvider(Color(0xFF009966))
                )
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            Text(
                text = "Eigene Aufgabe erstellen",
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorProvider(Color(0xFF009966))
                )
            )
        }
    }
}

@Composable
private fun QuickTaskButton(
    text: String,
    backgroundColor: ColorProvider
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 13.sp,
                color = ColorProvider(Color(0xFF333333))
            ),
            modifier = GlanceModifier.defaultWeight()
        )

        Text(
            text = "→",
            style = TextStyle(
                fontSize = 12.sp,
                color = ColorProvider(Color(0xFF009966))
            )
        )
    }
}