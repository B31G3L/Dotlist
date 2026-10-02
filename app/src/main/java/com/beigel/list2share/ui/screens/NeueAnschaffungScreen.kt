package com.beigel.list2share.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.beigel.list2share.R
import kotlinx.coroutines.delay

/**
 * Anlegen einer Anschaffung.
 *
 * Bewusst ein eigener, schlanker Dialog statt [NeueAufgabeScreen]: dort ginge
 * es um Priorität, Termin, Zuständigkeit und Erinnerung, bei einer Anschaffung
 * zählen nur Was, Preis und Link. Der Cursor steht sofort im Titel.
 */
@Composable
fun NeueAnschaffungScreen(
    onDismiss: () -> Unit,
    onSave   : (title: String, price: Double?, link: String) -> Unit,
) {
    var title      by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var link       by remember { mutableStateOf("") }

    val titleFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val priceValid = isPriceInputValid(priceInput)
    val canSave = title.isNotBlank() && priceValid

    fun save() {
        if (!canSave) return
        onSave(title.trim(), parsePrice(priceInput), link.trim())
    }

    // Im Dialog ist das Fenster beim ersten Frame noch nicht fokussierbar –
    // ein kurzer Moment Geduld, sonst bleibt die Tastatur zu.
    LaunchedEffect(Unit) {
        delay(150)
        titleFocus.requestFocus()
        keyboard?.show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                // ── Kopfzeile ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close))
                        }
                        Text(
                            text       = stringResource(R.string.title_new_purchase),
                            fontSize   = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(enabled = canSave, onClick = { save() }) {
                        Text(stringResource(R.string.action_save), fontWeight = FontWeight.SemiBold)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Titel ────────────────────────────────────────────
                    TextField(
                        value         = title,
                        onValueChange = { title = it },
                        placeholder   = { Text(stringResource(R.string.placeholder_purchase_title), fontSize = 22.sp) },
                        textStyle     = TextStyle(fontSize = 22.sp),
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction      = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor   = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .focusRequester(titleFocus)
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── Preis ────────────────────────────────────────────
                    OutlinedTextField(
                        value         = priceInput,
                        onValueChange = { priceInput = it },
                        label         = { Text(stringResource(R.string.label_price)) },
                        placeholder   = { Text(stringResource(R.string.hint_price)) },
                        suffix        = { Text("€") },
                        isError       = !priceValid,
                        supportingText = if (!priceValid) {
                            { Text(stringResource(R.string.error_price_invalid)) }
                        } else null,
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction    = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier      = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    // ── Link ─────────────────────────────────────────────
                    OutlinedTextField(
                        value         = link,
                        onValueChange = { link = it },
                        label         = { Text(stringResource(R.string.label_link)) },
                        placeholder   = { Text(stringResource(R.string.hint_link)) },
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { save() }),
                        modifier      = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
