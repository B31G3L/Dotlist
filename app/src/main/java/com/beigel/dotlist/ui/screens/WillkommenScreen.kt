package com.beigel.dotlist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beigel.dotlist.R
import com.beigel.dotlist.data.DeviceIdManager
import com.beigel.dotlist.utils.HapticFeedback

/**
 * Wird einmalig beim allerersten App-Start gezeigt (siehe DeviceIdManager.isNameSet).
 * Fragt nach dem Anzeigenamen, der z. B. in geteilten Listen für andere Mitglieder
 * sichtbar ist (Ersteller, Kommentare, "erledigt von" usw.).
 */
@Composable
fun WillkommenScreen(
    haptic : HapticFeedback,
    onDone : (name: String) -> Unit,
) {
    val context          = LocalContext.current
    val focusRequester    = remember { FocusRequester() }
    val focusManager      = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var name by remember {
        mutableStateOf(
            DeviceIdManager.getDeviceName(context).takeIf {
                // Build.MODEL als Vorbelegung wollen wir nicht 1:1 übernehmen,
                // ein leeres Feld wirkt hier einladender.
                DeviceIdManager.isNameSet(context)
            } ?: ""
        )
    }

    fun confirm() {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        haptic.click()
        DeviceIdManager.setDeviceName(context, trimmed)
        keyboardController?.hide()
        onDone(trimmed)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                Image(
                    painter            = painterResource(id = R.mipmap.ic_launcher_background),
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize().scale(1.5f)
                )
                Image(
                    painter            = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize().scale(1.5f)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text          = stringResource(R.string.welcome_title),
                fontSize      = 26.sp,
                fontWeight    = FontWeight.SemiBold,
                textAlign     = TextAlign.Center,
                color         = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text       = stringResource(R.string.welcome_subtitle),
                fontSize   = 15.sp,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier   = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value         = name,
                onValueChange = { if (it.length <= 30) name = it },
                singleLine    = true,
                placeholder   = { Text(stringResource(R.string.placeholder_your_name)) },
                shape         = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
                modifier      = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick  = { confirm() },
                enabled  = name.trim().isNotEmpty(),
                shape    = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(stringResource(R.string.action_get_started), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.weight(1.4f))
        }
    }
}