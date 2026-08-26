package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyAccountsScreen(viewModel: MasViewModel) {
    val context = LocalContext.current
    val partyAccounts by viewModel.partyAccounts.collectAsState()

    var selectedTypeFilter by remember { mutableStateOf<PartyAccountType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<PartyAccount?>(null) }

    var importPreviewResult by remember { mutableStateOf<ImportPreviewResult?>(null) }
    var duplicateStrategy by remember { mutableStateOf(DuplicateStrategy.SkipDuplicates) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var partyToDelete by remember { mutableStateOf<PartyAccount?>(null) }

    // File picker launcher for Excel (.xlsx) and CSV (.csv)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val preview = ExcelCsvAccountParser.parseFile(context, uri, partyAccounts)
                if (preview.totalRows == 0) {
                    Toast.makeText(context, "No rows could be parsed from selected file.", Toast.LENGTH_LONG).show()
                } else {
                    importPreviewResult = preview
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Filter accounts
    val filteredAccounts = partyAccounts.filter { acc ->
        (selectedTypeFilter == null || acc.accountType == selectedTypeFilter) &&
        (searchQuery.isBlank() ||
            acc.name.contains(searchQuery, ignoreCase = true) ||
            acc.code.contains(searchQuery, ignoreCase = true) ||
            acc.phone.contains(searchQuery))
    }

    // Totals
    val totalDebitOpening = partyAccounts.filter { it.balanceType == "Debit" }.sumOf { it.openingBalance }
    val totalCreditOpening = partyAccounts.filter { it.balanceType == "Credit" }.sumOf { it.openingBalance }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
    ) {
        // Section Header
        item {
            SectionHeader(
                title = "Parties & Account Types",
                subtitle = "Owner, Investor, Factory, Labour, Customer & Cash in Hand accounts with auto-codes.",
                actionButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MasRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MasRed)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Excel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                editingAccount = null
                                showAddDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Account", fontSize = 12.sp)
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
                    label = "Total Parties",
                    value = "${partyAccounts.size}",
                    icon = Icons.Default.Groups,
                    tone = MasInk,
                    sub = "6 Types Registered",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Debit (Get)",
                    value = formatMoney(totalDebitOpening),
                    icon = Icons.Default.TrendingUp,
                    tone = MasGreen,
                    sub = "Dr Receivable",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Credit (Give)",
                    value = formatMoney(totalCreditOpening),
                    icon = Icons.Default.TrendingDown,
                    tone = MasRed,
                    sub = "Cr Payable",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Excel Import / Sample Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MasGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = MasGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Excel / CSV Account Import",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MasInk
                            )
                            Text(
                                text = "Upload .xlsx or .csv to import with preview, auto codes & opening balance",
                                fontSize = 11.sp,
                                color = MasMuted
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            onClick = { showTemplateDialog = true }
                        ) {
                            Text("Template", fontSize = 11.5.sp, color = MasBlue)
                        }

                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = MasGreen),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload", fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by party name, code (e.g. OWN-001, CUS-001) or phone...", fontSize = 12.sp) },
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

        // 6 Account Types Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text("All (${partyAccounts.size})", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MasRed,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                items(PartyAccountType.values()) { type ->
                    val count = partyAccounts.count { it.accountType == type }
                    FilterChip(
                        selected = selectedTypeFilter == type,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == type) null else type },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(type.displayName, fontSize = 11.5.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(${type.codePrefix}) $count",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTypeFilter == type) Color.White else MasMuted
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = getPartyTypeColor(type),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Empty state or account items
        if (filteredAccounts.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MasRule),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = MasMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (partyAccounts.isEmpty()) "No Party Accounts Yet" else "No matching accounts found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MasInk
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (partyAccounts.isEmpty())
                                "Upload your Excel / CSV file or tap 'New Account' to create Owner, Investor, Factory, Labour, Customer, and Cash accounts."
                            else "Try clearing search or filter to see more accounts.",
                            fontSize = 12.sp,
                            color = MasMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (partyAccounts.isEmpty()) {
                                    filePickerLauncher.launch("*/*")
                                } else {
                                    searchQuery = ""
                                    selectedTypeFilter = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (partyAccounts.isEmpty()) "Import from Excel / CSV" else "Clear Filters", fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            items(filteredAccounts, key = { it.id }) { party ->
                PartyAccountCard(
                    account = party,
                    onEdit = {
                        editingAccount = party
                        showAddDialog = true
                    },
                    onDelete = {
                        partyToDelete = party
                    }
                )
            }
        }
    }

    // Double Confirmation Dialog for Party Deletion
    partyToDelete?.let { party ->
        DoubleConfirmDeleteDialog(
            title = "Delete Party Account",
            itemName = party.name,
            itemCode = party.code,
            itemType = party.accountType.displayName,
            additionalDetail = "Opening Balance: ${formatMoney(party.openingBalance)} (${party.balanceType}) · Phone: ${party.phone.ifBlank { "None" }}",
            isPermanent = false,
            onDismiss = { partyToDelete = null },
            onConfirm = {
                viewModel.deletePartyAccount(party.id)
                partyToDelete = null
            }
        )
    }

    // Add / Edit Account Dialog
    if (showAddDialog) {
        AddEditPartyAccountDialog(
            account = editingAccount,
            viewModel = viewModel,
            onDismiss = {
                showAddDialog = false
                editingAccount = null
            }
        )
    }

    // Excel / CSV Import Preview Dialog
    importPreviewResult?.let { preview ->
        ImportPreviewDialog(
            preview = preview,
            duplicateStrategy = duplicateStrategy,
            onDuplicateStrategyChange = { duplicateStrategy = it },
            onConfirmImport = {
                viewModel.importPartyAccounts(preview.rows, duplicateStrategy)
                importPreviewResult = null
            },
            onDismiss = { importPreviewResult = null }
        )
    }

    // Sample Excel / CSV Format Template Dialog
    if (showTemplateDialog) {
        SampleTemplateDialog(
            onLoadDemoData = {
                val demoRows = ExcelCsvAccountParser.parseCsvStream(
                    ExcelCsvAccountParser.generateSampleCsv().byteInputStream()
                )
                val preview = ExcelCsvAccountParser.processRawRows("sample_parties_template.csv", demoRows, partyAccounts)
                importPreviewResult = preview
                showTemplateDialog = false
            },
            onDismiss = { showTemplateDialog = false }
        )
    }
}

@Composable
fun PartyAccountCard(
    account: PartyAccount,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val typeColor = getPartyTypeColor(account.accountType)
    val isDebit = account.balanceType == "Debit"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MasRule),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Code Badge
                    Surface(
                        color = typeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, typeColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = account.code,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = account.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MasInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Account Type Badge
                Surface(
                    color = typeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = account.accountType.displayName,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = typeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details row: Phone & Address
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (account.phone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = MasMuted, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = account.phone, fontSize = 11.5.sp, color = MasInk)
                        }
                    }
                    if (account.address.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MasMuted, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = account.address, fontSize = 11.sp, color = MasMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (account.phone.isBlank() && account.address.isBlank()) {
                        Text(text = "No contact details", fontSize = 11.sp, color = MasMuted)
                    }
                }

                // Opening Balance Display
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Opening Balance",
                        fontSize = 10.sp,
                        color = MasMuted
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatMoney(account.openingBalance),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = if (isDebit) MasGreen else MasRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = if (isDebit) MasGreen.copy(alpha = 0.15f) else MasRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isDebit) "Dr (Get)" else "Cr (Give)",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDebit) MasGreen else MasRed,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            if (account.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Note: ${account.notes}",
                    fontSize = 10.5.sp,
                    color = MasMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MasRule, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(4.dp))

            // Action footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = MasBlue)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 11.5.sp, color = MasBlue)
                }

                TextButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = MasRed)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontSize = 11.5.sp, color = MasRed)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPartyAccountDialog(
    account: PartyAccount?,
    viewModel: MasViewModel,
    onDismiss: () -> Unit
) {
    val isEdit = account != null
    var selectedType by remember { mutableStateOf(account?.accountType ?: PartyAccountType.Customer) }
    var code by remember { mutableStateOf(account?.code ?: viewModel.getNextPartyCode(selectedType)) }
    var name by remember { mutableStateOf(account?.name ?: "") }
    var openingStr by remember { mutableStateOf(if (account != null && account.openingBalance > 0) account.openingBalance.toString() else "0") }
    var balanceType by remember { mutableStateOf(account?.balanceType ?: selectedType.defaultNature) }
    var phone by remember { mutableStateOf(account?.phone ?: "") }
    var address by remember { mutableStateOf(account?.address ?: "") }
    var notes by remember { mutableStateOf(account?.notes ?: "") }

    var openingStockQty by remember { mutableStateOf("") }
    var openingStockUnit by remember { mutableStateOf("Kg") }
    var openingStockRate by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // When type changes and not editing existing code, auto-update code
    LaunchedEffect(selectedType) {
        if (!isEdit) {
            code = viewModel.getNextPartyCode(selectedType)
            balanceType = selectedType.defaultNature
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Edit ${selectedType.displayName} Account" else "Create New Party Account",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Account Type / Category:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = MasInk)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(PartyAccountType.values()) { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.displayName, fontSize = 10.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = getPartyTypeColor(type),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text("Account Code (Auto Prefix: ${selectedType.codePrefix})", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = null
                        },
                        label = { Text("Party / Account Name *", fontSize = 11.sp) },
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = openingStr,
                            onValueChange = { openingStr = it },
                            label = { Text("Opening Balance (Rs)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nature", fontSize = 10.5.sp, color = MasMuted)
                            Row(modifier = Modifier.fillMaxWidth()) {
                                FilterChip(
                                    selected = balanceType == "Debit",
                                    onClick = { balanceType = "Debit" },
                                    label = { Text("Dr (Get)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                FilterChip(
                                    selected = balanceType == "Credit",
                                    onClick = { balanceType = "Credit" },
                                    label = { Text("Cr (Give)", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // If Factory Account, show Opening Stock fields
                if (selectedType == PartyAccountType.Factory) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MasAmber.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, MasAmber.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Factory Opening Stock (Optional)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MasAmber)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(
                                        value = openingStockQty,
                                        onValueChange = {
                                            openingStockQty = it
                                            val q = it.toDoubleOrNull() ?: 0.0
                                            val r = openingStockRate.toDoubleOrNull() ?: 0.0
                                            if (q > 0 && r > 0) openingStr = (q * r).toInt().toString()
                                        },
                                        label = { Text("Opening Qty", fontSize = 10.5.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = openingStockUnit,
                                        onValueChange = { openingStockUnit = it },
                                        label = { Text("Unit (Kg/Bag)", fontSize = 10.5.sp) },
                                        modifier = Modifier.weight(0.9f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = openingStockRate,
                                        onValueChange = {
                                            openingStockRate = it
                                            val q = openingStockQty.toDoubleOrNull() ?: 0.0
                                            val r = it.toDoubleOrNull() ?: 0.0
                                            if (q > 0 && r > 0) openingStr = (q * r).toInt().toString()
                                        },
                                        label = { Text("Rate (Rs)", fontSize = 10.5.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / Location", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Remarks", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                errorMessage?.let { msg ->
                    item {
                        Text(text = msg, color = MasRed, fontSize = 11.5.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Account name is required."
                        return@Button
                    }
                    val cleanCode = if (code.isNotBlank()) code.trim() else viewModel.getNextPartyCode(selectedType)
                    val openingAmount = openingStr.replace(",", "").toDoubleOrNull() ?: 0.0

                    val party = PartyAccount(
                        id = account?.id ?: "PTY-${System.currentTimeMillis() % 1000000}",
                        code = cleanCode,
                        name = name.trim(),
                        accountType = selectedType,
                        openingBalance = openingAmount,
                        balanceType = balanceType,
                        phone = phone.trim(),
                        address = address.trim(),
                        notes = notes.trim()
                    )

                    val saved = viewModel.savePartyAccount(party, updateExisting = isEdit)
                    if (saved) {
                        val opQ = openingStockQty.toDoubleOrNull() ?: 0.0
                        val opR = openingStockRate.toDoubleOrNull() ?: 0.0
                        if (selectedType == PartyAccountType.Factory && (opQ > 0.0 || opR > 0.0)) {
                            val opVal = if (opQ > 0 && opR > 0) opQ * opR else openingAmount
                            val opRec = OpeningStockRecord(
                                id = "OP-${party.code}-${System.currentTimeMillis() % 10000}",
                                itemId = "ITM-${party.code}",
                                itemName = "${party.name} Stock",
                                factoryId = party.code,
                                factoryName = party.name,
                                openingQty = if (opQ > 0) opQ else 1.0,
                                unit = openingStockUnit.ifBlank { "Kg" },
                                openingRate = if (opR > 0) opR else (if (opQ > 0) opVal / opQ else 250.0),
                                openingValue = opVal,
                                openingDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                notes = "Manual factory opening stock"
                            )
                            viewModel.saveOpeningStockRecord(opRec, updateExisting = true)
                        }
                        onDismiss()
                    } else {
                        errorMessage = "Account with code or name already exists in ${selectedType.displayName}."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MasRed)
            ) {
                Text(if (isEdit) "Update" else "Save Account", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun ImportPreviewDialog(
    preview: ImportPreviewResult,
    duplicateStrategy: DuplicateStrategy,
    onDuplicateStrategyChange: (DuplicateStrategy) -> Unit,
    onConfirmImport: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Excel / CSV Import Preview", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    PillBadge("${preview.totalRows} Rows", "blue")
                }
                Text("File: ${preview.fileName}", fontSize = 11.sp, color = MasMuted)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stats summary row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MasGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ready", fontSize = 9.5.sp, color = MasGreen, fontWeight = FontWeight.SemiBold)
                            Text("${preview.readyCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MasGreen)
                        }
                    }

                    Surface(
                        color = MasAmber.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Duplicates", fontSize = 9.5.sp, color = MasAmber, fontWeight = FontWeight.SemiBold)
                            Text("${preview.duplicateCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MasAmber)
                        }
                    }

                    Surface(
                        color = MasRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Invalid", fontSize = 9.5.sp, color = MasRed, fontWeight = FontWeight.SemiBold)
                            Text("${preview.invalidCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MasRed)
                        }
                    }
                }

                // Duplicate strategy selector if duplicates exist
                if (preview.duplicateCount > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MasAmber.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, MasAmber.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            Text("Duplicate Handling Strategy:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MasAmber)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = duplicateStrategy == DuplicateStrategy.SkipDuplicates,
                                    onClick = { onDuplicateStrategyChange(DuplicateStrategy.SkipDuplicates) },
                                    label = { Text("Skip Duplicates", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = duplicateStrategy == DuplicateStrategy.UpdateExisting,
                                    onClick = { onDuplicateStrategyChange(DuplicateStrategy.UpdateExisting) },
                                    label = { Text("Update Existing", fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }

                Text("Preview Accounts to Import:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MasInk)

                // Scrollable rows preview
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(preview.rows, key = { it.rowIndex }) { row ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                1.dp,
                                when (row.status) {
                                    "Ready" -> MasGreen.copy(alpha = 0.4f)
                                    "Duplicate" -> MasAmber.copy(alpha = 0.4f)
                                    else -> MasRed.copy(alpha = 0.4f)
                                }
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = row.assignedCode.ifEmpty { "AUTO" },
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MasBlue
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = row.name.ifEmpty { "(Missing Name)" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (row.name.isBlank()) MasRed else MasInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    PillBadge(
                                        row.status,
                                        when (row.status) {
                                            "Ready" -> "green"
                                            "Duplicate" -> "amber"
                                            else -> "red"
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Category: ${row.resolvedType?.displayName ?: row.categoryString.ifEmpty { "Unknown" }}",
                                        fontSize = 10.5.sp,
                                        color = if (row.resolvedType == null) MasRed else MasMuted
                                    )

                                    Text(
                                        text = "Op. Bal: ${formatMoney(row.openingBalance)} (${row.balanceType})",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.5.sp,
                                        color = if (row.balanceType == "Debit") MasGreen else MasRed
                                    )
                                }

                                if (row.hasOpeningStock || row.openingQty > 0.0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "📦 Opening Stock: ${row.openingQty} ${row.unit.ifBlank { "Kg" }} @ Rs ${row.openingRate} (Val: Rs ${row.openingValue})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MasAmber
                                    )
                                }

                                if (row.prefixWarning != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "⚠️ ${row.prefixWarning}",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (row.resolvedType == null) MasRed else MasAmber
                                    )
                                }

                                if (row.duplicateReason != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "⚠️ ${row.duplicateReason}",
                                        fontSize = 9.5.sp,
                                        color = MasAmber
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmImport,
                enabled = preview.readyCount > 0 || (preview.duplicateCount > 0 && duplicateStrategy == DuplicateStrategy.UpdateExisting),
                colors = ButtonDefaults.buttonColors(containerColor = MasGreen)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Confirm Import", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun SampleTemplateDialog(
    onLoadDemoData: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Excel / CSV Import Format", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Your Excel (.xlsx) or CSV (.csv) file should have the following column headers:",
                    fontSize = 11.5.sp,
                    color = MasInk
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MasInk),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "1. Account Code (e.g. OWN-001, SUP-001, CUS-001 or Leave blank for auto)\n" +
                               "2. Account Name (e.g. Al-Madina Scrap / Munawar Hussain)\n" +
                               "3. Category (Owner, Investor, Supplier, Factory, Labour & Employee, Customer, Cash In Hand)\n" +
                               "4. Opening Balance (e.g. 50000)\n" +
                               "5. Balance Type (Debit / Dr / Get or Credit / Cr / Give)\n" +
                               "6. Phone Number (e.g. +92 300 1234567)\n" +
                               "7. Address (e.g. Industrial Area)\n" +
                               "8. Notes (e.g. Vendor share)",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Text(
                    text = "Tip: Same party names across different categories (e.g. 'Munawar Hussain' as Owner and 'Munawar Hussain' as Cash In Hand) are fully supported and maintained separately.",
                    fontSize = 11.sp,
                    color = MasMuted
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onLoadDemoData,
                colors = ButtonDefaults.buttonColors(containerColor = MasBlue)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test Demo Import", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontSize = 12.sp)
            }
        }
    )
}

fun getPartyTypeColor(type: PartyAccountType): Color {
    return when (type) {
        PartyAccountType.Owner -> Color(0xFF673AB7) // Purple
        PartyAccountType.Investor -> Color(0xFF00897B) // Teal
        PartyAccountType.Supplier -> Color(0xFF8E24AA) // Amethyst / Violet
        PartyAccountType.Factory -> Color(0xFFE65100) // Deep Orange
        PartyAccountType.LabourEmployee -> Color(0xFF1565C0) // Deep Blue
        PartyAccountType.Customer -> Color(0xFF2E7D32) // Forest Green
        PartyAccountType.CashInHand -> Color(0xFFC2185B) // Magenta
    }
}
