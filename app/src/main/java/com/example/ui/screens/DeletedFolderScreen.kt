package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DeletedRecord
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MasViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletedFolderScreen(
    viewModel: MasViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val deletedRecords by viewModel.deletedRecords.collectAsState()
    var selectedTypeFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Double confirmation state for permanent delete of a single record
    var recordToPurge by remember { mutableStateOf<DeletedRecord?>(null) }
    // Double confirmation state for emptying all deleted records
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    val distinctTypes = remember(deletedRecords) {
        deletedRecords.map { it.itemType }.distinct().sorted()
    }

    val filteredRecords = deletedRecords.filter { rec ->
        (selectedTypeFilter == null || rec.itemType == selectedTypeFilter) &&
        (searchQuery.isBlank() ||
         rec.title.contains(searchQuery, ignoreCase = true) ||
         rec.itemCode.contains(searchQuery, ignoreCase = true) ||
         rec.itemType.contains(searchQuery, ignoreCase = true) ||
         rec.subtitle.contains(searchQuery, ignoreCase = true))
    }

    val totalTrashValue = deletedRecords.mapNotNull { it.amount }.sum()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
    ) {
        // Header Section
        item {
            SectionHeader(
                title = "Deleted Items Folder",
                subtitle = "Safe recycle bin for accounts, transactions & entries with double confirmation protection.",
                actionButton = {
                    if (deletedRecords.isNotEmpty()) {
                        Button(
                            onClick = { showEmptyTrashDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("empty_trash_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Empty Trash", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }

        // Summary Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Deleted Records",
                    value = "${deletedRecords.size}",
                    sub = "Archived safely",
                    icon = Icons.Default.DeleteSweep,
                    tone = MasRed,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Total Value",
                    value = formatMoney(totalTrashValue),
                    sub = "Recoverable sum",
                    icon = Icons.Default.AccountBalanceWallet,
                    tone = MasAmber,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Categories",
                    value = "${distinctTypes.size}",
                    sub = "Entity types",
                    icon = Icons.Default.Category,
                    tone = MasBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search deleted accounts, invoices, entries...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MasMuted, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = MasMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MasRed,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )
        }

        // Filter Chips for Types
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text("All (${deletedRecords.size})") }
                    )
                }
                items(distinctTypes) { type ->
                    val count = deletedRecords.count { it.itemType == type }
                    FilterChip(
                        selected = selectedTypeFilter == type,
                        onClick = { selectedTypeFilter = type },
                        label = { Text("$type ($count)") }
                    )
                }
            }
        }

        // Deleted Items List
        if (filteredRecords.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MasGreenSoft, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                tint = MasGreen,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = if (deletedRecords.isEmpty()) "Deleted Folder is Empty" else "No matching records found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (deletedRecords.isEmpty()) {
                                "All deleted accounts, entries, and transactions will safely appear here before permanent removal."
                            } else {
                                "Try adjusting your search query or filter chip."
                            },
                            color = MasMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(filteredRecords, key = { it.id }) { record ->
                DeletedRecordCard(
                    record = record,
                    onRestore = { viewModel.restoreDeletedRecord(record.id) },
                    onPermanentDelete = { recordToPurge = record }
                )
            }
        }
    }

    // Single Record Permanent Purge Double Confirmation Dialog
    recordToPurge?.let { rec ->
        DoubleConfirmDeleteDialog(
            title = "Permanent Record Deletion",
            itemName = rec.title,
            itemCode = rec.itemCode,
            itemType = rec.itemType,
            additionalDetail = rec.subtitle,
            isPermanent = true,
            onDismiss = { recordToPurge = null },
            onConfirm = {
                viewModel.permanentlyDeleteRecord(rec.id)
                recordToPurge = null
            }
        )
    }

    // Empty Trash Double Confirmation Dialog
    if (showEmptyTrashDialog) {
        DoubleConfirmDeleteDialog(
            title = "Empty Deleted Items Folder",
            itemName = "All ${deletedRecords.size} Deleted Records",
            itemCode = "TRASH-ALL",
            itemType = "Bulk Purge",
            additionalDetail = "Total value: ${formatMoney(totalTrashValue)} across ${distinctTypes.size} categories",
            isPermanent = true,
            onDismiss = { showEmptyTrashDialog = false },
            onConfirm = {
                viewModel.emptyDeletedRecords()
                showEmptyTrashDialog = false
            }
        )
    }
}

@Composable
private fun DeletedRecordCard(
    record: DeletedRecord,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(record.deletedAt) { sdf.format(Date(record.deletedAt)) }

    val badgeColor = when (record.itemType) {
        "Party Account" -> "red"
        "Chart of Account" -> "blue"
        "Journal Entry" -> "purple"
        "Customer", "Supplier" -> "green"
        "Sales Invoice", "Purchase Bill" -> "amber"
        "Expense Voucher" -> "red"
        else -> "slate"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PillBadge(record.itemType, badgeColor)
                    Text(
                        text = record.itemCode,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MasRed
                    )
                }
                Text(
                    text = "Deleted: $formattedDate",
                    fontSize = 10.5.sp,
                    color = MasMuted
                )
            }

            Column {
                Text(
                    text = record.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (record.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = record.subtitle,
                        fontSize = 12.sp,
                        color = MasMuted
                    )
                }
            }

            if (record.amount != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Original Amount:", fontSize = 11.5.sp, color = MasMuted)
                    Text(
                        text = formatMoney(record.amount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MasInk
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPermanentDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MasRed),
                    border = BorderStroke(1.dp, MasRed.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Forever", fontSize = 11.5.sp)
                }

                Button(
                    onClick = onRestore,
                    colors = ButtonDefaults.buttonColors(containerColor = MasGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(Icons.Default.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
