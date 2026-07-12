package de.beigel.list.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.beigel.list.R
import de.beigel.list.data.MemberRole
import de.beigel.list.data.TodoList
import de.beigel.list.data.canManageMembers
import de.beigel.list.data.displayLabel
import de.beigel.list.data.displayNameFor
import de.beigel.list.data.roleOf
import de.beigel.list.repository.TodoRepository
import de.beigel.list.utils.HapticFeedback
import kotlinx.coroutines.launch

@Composable
fun ListeTeilenScreen(
    list            : TodoList,
    currentDeviceId : String,
    repository      : TodoRepository,
    haptic          : HapticFeedback,
    onBack          : () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val scope     = rememberCoroutineScope()
    var copied    by remember { mutableStateOf(false) }

    val listColor  = listColor(list.color)
    val myRole     = list.roleOf(currentDeviceId)
    val canManage  = list.canManageMembers(currentDeviceId)

    var expandedMemberId    by remember { mutableStateOf<String?>(null) }
    var removeConfirmMember by remember { mutableStateOf<String?>(null) }
    var transferTargetMember by remember { mutableStateOf<String?>(null) }

    val avatarColors = listOf(
        Color(0xFF4F378B), Color(0xFF5B8DEF), Color(0xFF2FB6A0),
        Color(0xFFE8A04E), Color(0xFFE06FA0)
    )

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
        // App-Bar
        item {
            Row(
                modifier          = Modifier.fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(stringResource(R.string.title_share_list), fontSize = 20.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
        // Listen-Summary
        item {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier         = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp))
                        .background(listColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(de.beigel.list.ui.theme.iconFor(list, 0), null, tint = listColor, modifier = Modifier.size(26.dp))
                }
                Column {
                    Text(list.name, fontSize = 18.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.members_count, list.memberIds.size),
                        fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        // Einladungscode – nur für Besitzer und Admins sichtbar
        if (canManage) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape    = RoundedCornerShape(16.dp),
                    color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier         = Modifier.size(40.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ContentCopy, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_invite_code), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(list.id.take(12) + "…", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                        }
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(list.id))
                            copied = true
                            haptic.tick()
                        }) {
                            Icon(
                                if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                null, modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (copied) stringResource(R.string.action_copied) else stringResource(R.string.action_copy), fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            item {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Lock, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Text(
                        stringResource(R.string.invite_code_restricted),
                        fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // Mitglieder-Section
        item { SectionLabel(stringResource(R.string.section_members), modifier = Modifier.padding(top = 12.dp)) }
        items(list.memberIds.toList(), key = { it }) { memberId ->
            val idx        = list.memberIds.indexOf(memberId)
            val name       = list.displayNameFor(memberId)
            val initial    = name.take(1).uppercase().ifEmpty { "?" }
            val targetRole = list.roleOf(memberId)
            val isSelf     = memberId == currentDeviceId
            val canActOnThis = canManage && !isSelf && targetRole != MemberRole.BESITZER

            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (canActOnThis) it.clickable { expandedMemberId = memberId } else it }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier         = Modifier.size(42.dp).clip(CircleShape)
                            .background(avatarColors[idx % avatarColors.size]),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initial, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isSelf) stringResource(R.string.label_me) else name,
                            fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(memberId.take(14), fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(targetRole.displayLabel(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (canActOnThis) {
                            Icon(Icons.Default.ExpandMore, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                DropdownMenu(
                    expanded         = expandedMemberId == memberId,
                    onDismissRequest = { expandedMemberId = null }
                ) {
                    if (targetRole == MemberRole.MITGLIED) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_make_admin)) },
                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, null) },
                            onClick = {
                                expandedMemberId = null
                                haptic.tick()
                                scope.launch { repository.promoteToAdmin(list.id, memberId) }
                            }
                        )
                    } else if (targetRole == MemberRole.ADMIN) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_remove_admin)) },
                            leadingIcon = { Icon(Icons.Default.RemoveModerator, null) },
                            onClick = {
                                expandedMemberId = null
                                haptic.tick()
                                scope.launch { repository.demoteAdmin(list.id, memberId) }
                            }
                        )
                    }
                    if (myRole == MemberRole.BESITZER) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_transfer_ownership)) },
                            leadingIcon = { Icon(Icons.Default.WorkspacePremium, null) },
                            onClick = {
                                expandedMemberId = null
                                transferTargetMember = memberId
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_remove_from_list), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.PersonRemove, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            expandedMemberId = null
                            removeConfirmMember = memberId
                        }
                    )
                }
            }
        }
    }

    // ── Mitglied entfernen bestätigen ────────────────────────────────────
    removeConfirmMember?.let { memberId ->
        AlertDialog(
            onDismissRequest = { removeConfirmMember = null },
            title   = { Text(stringResource(R.string.dialog_remove_member_title)) },
            text    = { Text(stringResource(R.string.dialog_remove_member_message, list.displayNameFor(memberId))) },
            confirmButton = {
                TextButton(onClick = {
                    haptic.heavy()
                    scope.launch { repository.removeMember(list.id, memberId) }
                    removeConfirmMember = null
                }) { Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { removeConfirmMember = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    // ── Besitz übertragen bestätigen (mit Warnung) ───────────────────────
    transferTargetMember?.let { memberId ->
        AlertDialog(
            onDismissRequest = { transferTargetMember = null },
            title   = { Text(stringResource(R.string.dialog_transfer_ownership_title)) },
            text    = {
                Text(stringResource(R.string.dialog_transfer_ownership_message, list.displayNameFor(memberId)))
            },
            confirmButton = {
                TextButton(onClick = {
                    haptic.heavy()
                    scope.launch { repository.transferOwnership(list.id, memberId) }
                    transferTargetMember = null
                }) { Text(stringResource(R.string.action_transfer)) }
            },
            dismissButton = { TextButton(onClick = { transferTargetMember = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}