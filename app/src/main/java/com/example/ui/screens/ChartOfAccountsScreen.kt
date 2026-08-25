package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Account
import com.example.data.AccountType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartOfAccountsScreen(
    viewModel: MasViewModel,
    onNavigateToParties: () -> Unit = {}
) {
    val accounts by viewModel.accounts.collectAsState()
    var selectedTypeFilter by remember { mutableStateOf<AccountType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }

    // Trial balance check for opening balances
    val totalDebits = accounts.filter { it.active && it.nature == "Debit" }.sumOf { it.opening }
    val totalCredits = accounts.filter { it.active && it.nature == "Credit" }.sumOf { it.opening }
    val isBalanced = Math.abs(totalDebits - totalCredits) < 0.01

    val filteredAccounts = accounts.filter { acc ->
        (selectedTypeFilter == null || acc.type == selectedTypeFilter) &&
        (searchQuery.isBlank() || acc.name.contains(searchQuery, ignoreCase = true) || acc.code.contains(searchQuery))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
    ) {
        item {
            SectionHeader(
                title = "Chart of Accounts",
                subtitle = "Central accounts structure powering all double-entry postings.",
                actionButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onNavigateToParties,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MasRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MasRed)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Parties & Excel Import", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Account", fontSize = 12.sp)
                        }
                    }
                }
            )
        }

        // Trial Balance Verification Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isBalanced) MasGreenSoft else MasRedLight),
                border = BorderStroke(1.dp, if (isBalanced) MasGreen.copy(alpha = 0.3f) else MasRed.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Opening Trial Balance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = if (isBalanced) MasGreen else MasRed
                        )
                        Text(
                            text = "Dr ${formatMoney(totalDebits)} · Cr ${formatMoney(totalCredits)}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MasInk
                        )
                    }
                    PillBadge(if (isBalanced) "Balanced" else "Out of Balance", if (isBalanced) "green" else "red")
                }
            }
        }

        // Search Bar
        item {
            SearchBarField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search accounts by code or name..."
            )
        }

        // Category Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text("All (${accounts.size})") }
                    )
                }
                items(AccountType.values()) { type ->
                    val count = accounts.count { it.type == type }
                    FilterChip(
                        selected = selectedTypeFilter == type,
                        onClick = { selectedTypeFilter = type },
                        label = { Text("${type.displayName} ($count)") }
                    )
                }
            }
        }

        // Accounts Table Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    filteredAccounts.forEach { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = acc.code,
                                        color = MasRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = acc.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    text = "${acc.type.displayName} · ${acc.category}",
                                    color = MasMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatMoney(acc.opening),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    PillBadge(acc.nature, if (acc.nature == "Debit") "red" else "green")
                                }
                                if (!acc.system) {
                                    IconButton(
                                        onClick = { accountToDelete = acc },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Account",
                                            tint = MasRed.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    }
                    if (filteredAccounts.isEmpty()) {
                        Text(
                            text = "No accounts match this filter.",
                            color = MasMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }

    // Double Confirmation Dialog for Account Deletion
    accountToDelete?.let { acc ->
        DoubleConfirmDeleteDialog(
            title = "Delete Account",
            itemName = acc.name,
            itemCode = acc.code,
            itemType = acc.type.displayName,
            additionalDetail = "${acc.category} · Opening: ${formatMoney(acc.opening)} (${acc.nature})",
            isPermanent = false,
            onDismiss = { accountToDelete = null },
            onConfirm = {
                viewModel.deleteAccount(acc.id)
                accountToDelete = null
            }
        )
    }

    // Add Account Dialog
    if (showAddDialog) {
        var newCode by remember { mutableStateOf("1095") }
        var newName by remember { mutableStateOf("") }
        var newType by remember { mutableStateOf(AccountType.Assets) }
        var newCategory by remember { mutableStateOf("Cash") }
        var newOpening by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Account Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCode,
                        onValueChange = { newCode = it },
                        label = { Text("Account Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newOpening,
                        onValueChange = { newOpening = it },
                        label = { Text("Opening Balance") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newCode.isNotBlank()) {
                            viewModel.addAccount(
                                Account(
                                    id = "acc_${System.currentTimeMillis()}",
                                    code = newCode.trim(),
                                    name = newName.trim(),
                                    type = newType,
                                    category = newCategory,
                                    opening = newOpening.toDoubleOrNull() ?: 0.0
                                )
                            )
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MasRed)
                ) {
                    Text("Save Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
