package com.beigel.dotlist.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beigel.dotlist.R
import com.beigel.dotlist.data.ListCounts
import com.beigel.dotlist.data.TodoItem
import com.beigel.dotlist.data.TodoList
import com.beigel.dotlist.repository.TodoRepository
import com.beigel.dotlist.utils.HapticFeedback
import com.beigel.dotlist.viewmodel.ListsViewModel


@Composable
fun ListenScreen(
    viewModel  : ListsViewModel,
    repository : TodoRepository,
    deviceId   : String,
    haptic     : HapticFeedback,
    padding    : PaddingValues,
    onOpenList : (TodoList) -> Unit,
    onShare    : (TodoList) -> Unit,
    onErstellen: () -> Unit,
    onSearch   : () -> Unit,
    onOpenTask : (TodoList, TodoItem) -> Unit = { _, _ -> },
) {
    val uiState           by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }
    LaunchedEffect(uiState.joinSuccess) {
        uiState.joinSuccess?.let {
            snackbarHostState.showSnackbar(context.getString(R.string.toast_joined_list, it.name))
            viewModel.clearJoinSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyVerticalGrid(
                columns             = GridCells.Fixed(2),
                contentPadding      = PaddingValues(start = 22.dp, end = 22.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier            = Modifier.fillMaxSize()
            ) {
                // Header (full width)
                item(span = { GridItemSpan(2) }) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 16.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.title_my_lists), fontSize = 34.sp, fontWeight = FontWeight.Medium,
                                letterSpacing = (-0.5).sp, color = MaterialTheme.colorScheme.onSurface)
                            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Link, contentDescription = stringResource(R.string.cd_join_with_code),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable { haptic.tick(); viewModel.showJoinDialog() })
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_search), tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable { haptic.tick(); onSearch() })
                            }
                        }
                        val shared = uiState.lists.count { it.memberIds.size > 1 }
                        Text(
                            stringResource(R.string.lists_summary, uiState.lists.size, shared),
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                }

                // New list card - immer als erste Kachel (oben links)
                item {
                    NewListCard(onClick = { haptic.click(); onErstellen() })
                }

                // List cards
                items(uiState.lists.toList(), key = { it.id }) { list ->
                    val idx      = uiState.lists.indexOf(list)
                    val isShared = list.memberIds.size > 1
                    ListCard(
                        list     = list,
                        index    = idx,
                        isShared = isShared,
                        isSelected = list.id in uiState.selectedListIds,
                        counts   = uiState.listCounts[list.id] ?: ListCounts(),
                        onClick  = { haptic.click(); onOpenList(list) },
                        onShare  = { e -> haptic.tick(); onShare(list) },
                        onToggleSelect = { haptic.tick(); viewModel.toggleListSelection(list.id) }
                    )
                }

                // Beitreten per Code
                item(span = { GridItemSpan(2) }) {
                    TextButton(
                        onClick  = { haptic.tick(); viewModel.showJoinDialog() },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Link, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_join_with_code))
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (uiState.showJoinDialog) {
        JoinListDialog(
            onDismiss = { haptic.tick(); viewModel.hideJoinDialog() },
            onJoin    = { viewModel.previewInvite(it) }
        )
    }

    uiState.invitePreview?.let { previewList ->
        Dialog(
            onDismissRequest = { viewModel.clearInvitePreview() },
            properties       = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows  = false
            )
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                EinladungScreen(
                    list      = previewList,
                    haptic    = haptic,
                    onAccept  = { viewModel.confirmJoin(previewList.id) },
                    onDecline = { viewModel.clearInvitePreview() }
                )
            }
        }
    }
}

private val avatarColors: List<Color> = listOf(
    Color(0xFF5B8DEF), Color(0xFF4EC9A8), Color(0xFFFFA552), Color(0xFFE06FA0), Color(0xFF9B7EDE),
)

private fun avatarColorFor(memberId: String): Color =
    avatarColors[(memberId.hashCode() and 0x7fffffff) % avatarColors.size]

private fun avatarLetterFor(memberId: String): String =
    memberId.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"

@Composable
private fun NewListCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape   = RoundedCornerShape(22.dp),
        border  = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        color   = Color.Transparent,
        modifier = Modifier.fillMaxWidth().height(210.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier         = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.action_new_list), fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ListCard(
    list        : TodoList,
    index       : Int,
    isShared    : Boolean,
    isSelected  : Boolean,
    counts      : ListCounts,
    onClick     : () -> Unit,
    onShare     : (Any) -> Unit,
    onToggleSelect: () -> Unit,
) {
    val color = listColor(list.color)
    val bgAlpha = color.copy(alpha = 0.16f)

    Card(
        onClick = onClick,
        shape   = RoundedCornerShape(22.dp),
        colors  = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().height(210.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                // Icon tile
                Box(
                    modifier         = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(bgAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(com.beigel.dotlist.ui.theme.iconFor(list, index), null, tint = color, modifier = Modifier.size(24.dp))
                }
                // Share indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick  = onToggleSelect,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (isSelected) stringResource(R.string.cd_toggle_active) else stringResource(R.string.cd_toggle_inactive),
                            tint     = if (isSelected) color else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick  = { onShare(Unit) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (isShared) Icons.Default.Group else Icons.Default.Person,
                            null,
                            tint     = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(list.name, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.progress_done_of_total, counts.done, counts.total),
                fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp))
            // Fortschrittsbalken (echte Erledigt-Quote)
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Box(modifier = Modifier.fillMaxWidth(counts.fraction).fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp)).background(color))
            }
            Spacer(Modifier.height(14.dp))
            if (isShared) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row {
                        list.memberIds.take(3).forEachIndexed { i, memberId ->
                            Box(
                                modifier = Modifier
                                    .offset(x = if (i == 0) 0.dp else (-6).dp * i)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(avatarColorFor(memberId))
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text       = avatarLetterFor(memberId),
                                    fontSize   = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White,
                                    lineHeight = 8.sp,
                                    style      = LocalTextStyle.current.copy(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    )
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(stringResource(R.string.members_count_people, list.memberIds.size),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null,
                        tint     = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.label_only_you), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun JoinListDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_join_list_title)) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.dialog_join_list_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = code, onValueChange = { code = it.trim() },
                    label = { Text(stringResource(R.string.placeholder_invite_code)) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onJoin(code); onDismiss() }, enabled = code.isNotBlank()) {
                Text(stringResource(R.string.action_join))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}