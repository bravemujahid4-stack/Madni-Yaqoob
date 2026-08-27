package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MasViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashBankScreen(viewModel: MasViewModel) {
    var subTab by remember { mutableStateOf("accounts") } // accounts, entry, transfer, ledger, reconcile
    val accounts by viewModel.cashBankAccounts.collectAsState()
    val txns by viewModel.cashBankTxns.collectAsState()
    val cashInHand by viewModel.cashInHand.collectAsState()
    val bankBalance by viewModel.bankBalance.collectAsState()

    var showAddAccountDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        SectionHeader(
            title = "Cash & Bank Management",
            subtitle = "Multiple Cash in Hand accounts, petty cash, bank accounts & transfers.",
            actionButton = {
                Button(
                    onClick = { showAddAccountDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Account", fontSize = 12.sp)
                }
            }
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { FilterChip(selected = subTab == "accounts", onClick = { subTab = "accounts" }, label = { Text("Accounts (${accounts.size})") }) }
            item { FilterChip(selected = subTab == "entry", onClick = { subTab = "entry" }, label = { Text("+ Receipt/Payment") }) }
            item { FilterChip(selected = subTab == "transfer", onClick = { subTab = "transfer" }, label = { Text("⇄ Transfer") }) }
            item { FilterChip(selected = subTab == "ledger", onClick = { subTab = "ledger" }, label = { Text("Account Ledger") }) }
            item { FilterChip(selected = subTab == "reconcile", onClick = { subTab = "reconcile" }, label = { Text("Bank Reconciliation") }) }
        }

        when (subTab) {
            "accounts" -> CashBankAccountsList(viewModel, accounts, txns, cashInHand, bankBalance)
            "entry" -> CashBankEntryForm(viewModel, accounts) { subTab = "accounts" }
            "transfer" -> CashBankTransferForm(viewModel, accounts) { subTab = "accounts" }
            "ledger" -> CashBankLedgerView(accounts, txns)
            "reconcile" -> BankReconciliationView(accounts, txns)
        }

        if (showAddAccountDialog) {
            AddCashBankAccountDialog(
                onSave = { newAcc ->
                    viewModel.addCashBankAccount(newAcc)
                    showAddAccountDialog = false
                },
                onDismiss = { showAddAccountDialog = false }
            )
        }
    }
}

@Composable
fun AddCashBankAccountDialog(
    onSave: (CashBankAccount) -> Unit,
    onDismiss: () -> Unit
) {
    var kind by remember { mutableStateOf("Cash") } // Cash, Bank
    var name by remember { mutableStateOf("") }
    var openingStr by remember { mutableStateOf("0") }
    var bankName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Cash / Bank Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = kind == "Cash",
                        onClick = { kind = "Cash" },
                        label = { Text("Cash In Hand", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = kind == "Bank",
                        onClick = { kind = "Bank" },
                        label = { Text("Bank Account", fontSize = 11.sp) }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name (e.g. Factory Floor Cash, Main Cash)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = openingStr,
                    onValueChange = { openingStr = it },
                    label = { Text("Opening Balance (Rs)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (kind == "Bank") {
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank Name (e.g. Meezan Bank, HBL)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text("Account Number / IBAN", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                errorMessage?.let {
                    Text(it, color = MasRed, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Account name is required"
                        return@Button
                    }
                    val opening = openingStr.toDoubleOrNull() ?: 0.0
                    val accId = if (kind == "Cash") "CASH-${System.currentTimeMillis() % 10000}" else "BNK-${System.currentTimeMillis() % 10000}"
                    val newAcc = CashBankAccount(
                        id = accId,
                        name = name.trim(),
                        kind = kind,
                        openingBalance = opening,
                        bankName = if (kind == "Bank") bankName.trim() else null,
                        accountNumber = if (kind == "Bank") accountNumber.trim() else null
                    )
                    onSave(newAcc)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MasRed)
            ) {
                Text("Save Account", fontSize = 12.sp)
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
fun CashBankAccountsList(
    viewModel: MasViewModel,
    accounts: List<CashBankAccount>,
    txns: List<CashBankTxn>,
    totalCash: Double,
    totalBank: Double
) {
    var editingAccount by remember { mutableStateOf<CashBankAccount?>(null) }
    var newOpeningInput by remember { mutableStateOf("") }

    if (editingAccount != null) {
        val acc = editingAccount!!
        AlertDialog(
            onDismissRequest = { editingAccount = null },
            title = { Text("Set Opening Balance", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Account: ${acc.name} (${acc.kind})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Enter initial cash opening balance in hand prior to recorded transactions:", fontSize = 12.sp, color = MasMuted)
                    OutlinedTextField(
                        value = newOpeningInput,
                        onValueChange = { newOpeningInput = it },
                        label = { Text("Opening Balance (Rs)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = newOpeningInput.toDoubleOrNull() ?: 0.0
                        viewModel.updateCashBankAccountOpening(acc.id, amount)
                        editingAccount = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MasGreen)
                ) {
                    Text("Save Opening Balance")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAccount = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val cashDrCr = if (totalCash >= 0) "Dr" else "Cr"
                val bankDrCr = if (totalBank >= 0) "Dr" else "Cr"
                StatCard(
                    "Total Cash in Hand",
                    "${formatMoney(Math.abs(totalCash))} $cashDrCr",
                    icon = Icons.Default.Money,
                    tone = if (totalCash >= 0) MasGreen else MasRed,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    "Total Bank Balance",
                    "${formatMoney(Math.abs(totalBank))} $bankDrCr",
                    icon = Icons.Default.AccountBalance,
                    tone = if (totalBank >= 0) MasBlue else MasRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cash Accounts", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Tap account to edit opening balance", fontSize = 11.sp, color = MasMuted)
            }
        }

        items(accounts.filter { it.kind == "Cash" }) { acc ->
            val inTx = txns.filter { it.accountId == acc.id && it.type == "Receipt" || it.toAccountId == acc.id }.sumOf { it.amount }
            val outTx = txns.filter { it.accountId == acc.id && it.type == "Payment" || it.fromAccountId == acc.id }.sumOf { it.amount }
            val balance = acc.openingBalance + inTx - outTx
            val drCr = if (balance >= 0) "Dr" else "Cr"
            val tone = if (balance >= 0) MasGreen else MasRed

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().clickable {
                    editingAccount = acc
                    newOpeningInput = if (acc.openingBalance != 0.0) Math.abs(acc.openingBalance).toString() else ""
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Edit, contentDescription = "Edit Opening Balance", tint = MasGreen, modifier = Modifier.size(14.dp))
                        }
                        val openingDrCr = if (acc.openingBalance >= 0) "Dr" else "Cr"
                        Text("Opening: ${formatMoney(Math.abs(acc.openingBalance))} $openingDrCr · ${acc.id}", color = MasMuted, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${formatMoney(Math.abs(balance))} $drCr", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = tone, fontFamily = FontFamily.Monospace)
                        Text("Current Balance", fontSize = 10.sp, color = MasMuted)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bank Accounts", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Tap to edit opening balance", fontSize = 11.sp, color = MasMuted)
            }
        }

        items(accounts.filter { it.kind == "Bank" }) { acc ->
            val inTx = txns.filter { it.accountId == acc.id && it.type == "Receipt" || it.toAccountId == acc.id }.sumOf { it.amount }
            val outTx = txns.filter { it.accountId == acc.id && it.type == "Payment" || it.fromAccountId == acc.id }.sumOf { it.amount }
            val balance = acc.openingBalance + inTx - outTx
            val drCr = if (balance >= 0) "Dr" else "Cr"
            val tone = if (balance >= 0) MasBlue else MasRed

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().clickable {
                    editingAccount = acc
                    newOpeningInput = if (acc.openingBalance != 0.0) Math.abs(acc.openingBalance).toString() else ""
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Edit, contentDescription = "Edit Opening Balance", tint = MasBlue, modifier = Modifier.size(14.dp))
                        }
                        val openingDrCr = if (acc.openingBalance >= 0) "Dr" else "Cr"
                        Text("Opening: ${formatMoney(Math.abs(acc.openingBalance))} $openingDrCr · ${acc.bankName ?: ""}", color = MasMuted, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${formatMoney(Math.abs(balance))} $drCr", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = tone, fontFamily = FontFamily.Monospace)
                        Text("Current Balance", fontSize = 10.sp, color = MasMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun CashBankEntryForm(
    viewModel: MasViewModel,
    accounts: List<CashBankAccount>,
    onSaved: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var type by remember { mutableStateOf("Receipt") } // Receipt, Payment
    var selectedAccId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var contraAccount by remember { mutableStateOf("Sales Revenue") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(sdf.format(Date())) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val accountPickerOptions = remember(viewModel.partyAccounts, viewModel.accounts, viewModel.cashBankAccounts) {
        viewModel.getAllAccountPickerOptions()
    }

    val amountVal = amount.toDoubleOrNull() ?: 0.0
    val currentAcc = accounts.find { it.id == selectedAccId }
    val isSameAccount = currentAcc != null && currentAcc.name.equals(contraAccount, ignoreCase = true)
    val canSave = description.isNotBlank() && amountVal > 0.0 && selectedAccId.isNotBlank() && contraAccount.isNotBlank() && !isSameAccount

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cash / Bank Transaction Voucher", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    // 1. Interactive Receipt / Payment Toggle Buttons (Requirement 5)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "TRANSACTION TYPE",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Receipt Button (Inflow)
                            Button(
                                onClick = {
                                    type = "Receipt"
                                    if (contraAccount.isBlank() || contraAccount == "General Expenses") {
                                        contraAccount = "Sales Revenue"
                                    }
                                },
                                modifier = Modifier.weight(1f).height(42.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (type == "Receipt") MasGreen else Color.Transparent,
                                    contentColor = if (type == "Receipt") Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                elevation = if (type == "Receipt") ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Receipt (Inflow)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }
                            }

                            // Payment Button (Outflow)
                            Button(
                                onClick = {
                                    type = "Payment"
                                    if (contraAccount.isBlank() || contraAccount == "Sales Revenue") {
                                        contraAccount = "General Expenses"
                                    }
                                },
                                modifier = Modifier.weight(1f).height(42.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (type == "Payment") MasRed else Color.Transparent,
                                    contentColor = if (type == "Payment") Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                elevation = if (type == "Payment") ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Payment (Outflow)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }
                            }
                        }

                        // Informational entry hint
                        Surface(
                            color = if (type == "Receipt") MasGreenSoft else MasRedLight,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (type == "Receipt")
                                    "✓ INFLOW: [Dr] ${currentAcc?.name ?: "Cash/Bank Account"}  ⇄  [Cr] $contraAccount"
                                else
                                    "✓ OUTFLOW: [Dr] $contraAccount  ⇄  [Cr] ${currentAcc?.name ?: "Cash/Bank Account"}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (type == "Receipt") MasGreen else MasRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // 2. Deposit / Payment Account Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (type == "Receipt") "DEPOSIT INTO (CASH / BANK ACCOUNT)" else "PAY FROM (CASH / BANK ACCOUNT)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        var accExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth().clickable { accExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MasRed)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (currentAcc?.kind == "Bank") Icons.Default.AccountBalance else Icons.Default.Money,
                                            contentDescription = null,
                                            tint = MasRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            currentAcc?.let { "${it.name} (${it.kind})" } ?: "Select Account",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(expanded = accExpanded, onDismissRequest = { accExpanded = false }) {
                                accounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(if (acc.kind == "Bank") Icons.Default.AccountBalance else Icons.Default.Money, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("${acc.name} (${acc.kind})")
                                            }
                                        },
                                        onClick = {
                                            selectedAccId = acc.id
                                            accExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Searchable Contra Account Picker (Requirement 6)
                    SearchableAccountPicker(
                        label = if (type == "Receipt") "RECEIVED FROM / CONTRA ACCOUNT (CREDIT)" else "PAID TO / CONTRA ACCOUNT (DEBIT)",
                        selectedAccountName = contraAccount,
                        options = accountPickerOptions,
                        onAccountSelected = { opt ->
                            contraAccount = opt.name
                            if (description.isBlank()) {
                                description = if (type == "Receipt") "Received from ${opt.name}" else "Payment to ${opt.name}"
                            }
                        },
                        placeholder = "Search party, customer, supplier or GL account...",
                        hideZeroBalancesByDefault = false
                    )

                    // 4. Amount and Date
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = {
                                amount = it
                                errorMessage = null
                            },
                            label = { Text("Amount (Rs)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = amount.isNotBlank() && (amountVal <= 0.0)
                        )
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // 5. Narration
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            errorMessage = null
                        },
                        label = { Text("Narration / Purpose / Remarks") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Validation warning
                    if (isSameAccount) {
                        Text("Error: Deposit account and contra account cannot be the same.", color = MasRed, fontSize = 11.5.sp)
                    }
                    errorMessage?.let {
                        Text(it, color = MasRed, fontSize = 11.5.sp)
                    }

                    Button(
                        onClick = {
                            if (amountVal <= 0.0) {
                                errorMessage = "Amount must be greater than zero."
                                return@Button
                            }
                            if (description.isBlank()) {
                                errorMessage = "Narration / description is required."
                                return@Button
                            }
                            if (isSameAccount) {
                                errorMessage = "Cannot post debit and credit to the exact same account."
                                return@Button
                            }

                            val txn = CashBankTxn(
                                id = "${type.take(3).uppercase()}-${System.currentTimeMillis() % 10000}",
                                type = type,
                                accountId = selectedAccId,
                                date = date,
                                description = description.trim(),
                                contraAccount = contraAccount.trim(),
                                amount = amountVal
                            )
                            viewModel.cashBankTxns.value = viewModel.cashBankTxns.value + txn

                            val accName = currentAcc?.name ?: "Cash"
                            val (debit, credit) = if (type == "Receipt") Pair(accName, contraAccount) else Pair(contraAccount, accName)
                            val (posted, msg) = viewModel.addJournalEntry(
                                JournalEntry(
                                    id = "JE-${txn.id}",
                                    date = date,
                                    source = "Cash & Bank",
                                    description = "${type}: ${description.trim()} ($accName ⇄ $contraAccount)",
                                    reference = txn.id,
                                    lines = listOf(JournalLine(debit, amountVal, 0.0), JournalLine(credit, 0.0, amountVal))
                                )
                            )
                            if (posted) {
                                onSaved()
                            } else {
                                errorMessage = msg
                            }
                        },
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(containerColor = if (type == "Receipt") MasGreen else MasRed),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(if (type == "Receipt") Icons.Default.Check else Icons.Default.Payment, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Record ${type} (Rs ${formatMoney(amountVal, "")})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CashBankTransferForm(
    viewModel: MasViewModel,
    accounts: List<CashBankAccount>,
    onSaved: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var fromAccId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var toAccId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: "") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(sdf.format(Date())) }

    val amountVal = amount.toDoubleOrNull() ?: 0.0
    val canSubmit = fromAccId.isNotBlank() && toAccId.isNotBlank() && fromAccId != toAccId && amountVal > 0.0

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Transfer Funds Between Accounts", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Text("From Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    AccountDropSelector(accounts = accounts, selectedId = fromAccId, onSelect = { fromAccId = it })

                    Text("To Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    AccountDropSelector(accounts = accounts, selectedId = toAccId, onSelect = { toAccId = it })

                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Transfer Amount") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description / Note") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            if (canSubmit) {
                                viewModel.transferFunds(fromAccId, toAccId, amountVal, description, date)
                                onSaved()
                            }
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Execute Fund Transfer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountDropSelector(
    accounts: List<CashBankAccount>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val current = accounts.find { it.id == selectedId }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { expanded = true }, shape = RoundedCornerShape(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(current?.name ?: "Select Account", fontSize = 13.sp)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { acc ->
                DropdownMenuItem(text = { Text("${acc.name} (${acc.kind})") }, onClick = {
                    onSelect(acc.id)
                    expanded = false
                })
            }
        }
    }
}

@Composable
fun CashBankLedgerView(accounts: List<CashBankAccount>, txns: List<CashBankTxn>) {
    var selectedAccId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    val acc = accounts.find { it.id == selectedAccId } ?: accounts.first()

    val relevantTxns = txns.filter { it.accountId == acc.id || it.fromAccountId == acc.id || it.toAccountId == acc.id }
        .sortedBy { it.date }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            AccountDropSelector(accounts = accounts, selectedId = selectedAccId, onSelect = { selectedAccId = it })
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Opening Balance", fontWeight = FontWeight.Bold)
                    Text(formatMoney(acc.openingBalance), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        var running = acc.openingBalance
        items(relevantTxns) { t ->
            val isIn = (t.type == "Receipt" && t.accountId == acc.id) || (t.type == "Transfer" && t.toAccountId == acc.id)
            val delta = if (isIn) t.amount else -t.amount
            running += delta

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t.description, fontWeight = FontWeight.Medium, fontSize = 12.5.sp)
                        Text("${t.date} · ${t.id} · ${t.type}", color = MasMuted, fontSize = 10.5.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = (if (isIn) "+ " else "- ") + formatMoney(t.amount),
                            color = if (isIn) MasGreen else MasRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text("Bal: ${formatMoney(running)}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun BankReconciliationView(accounts: List<CashBankAccount>, txns: List<CashBankTxn>) {
    var statementBalance by remember { mutableStateOf("") }
    val bankAccounts = accounts.filter { it.kind == "Bank" }
    var selectedBankId by remember { mutableStateOf(bankAccounts.firstOrNull()?.id ?: "") }
    val bankAcc = bankAccounts.find { it.id == selectedBankId }

    val clearedIds = remember { mutableStateListOf<String>() }

    val relevant = txns.filter { it.accountId == bankAcc?.id || it.fromAccountId == bankAcc?.id || it.toAccountId == bankAcc?.id }
    val bookBalance = bankAcc?.openingBalance ?: 0.0 + relevant.sumOf {
        val isIn = (it.type == "Receipt" && it.accountId == bankAcc?.id) || (it.type == "Transfer" && it.toAccountId == bankAcc?.id)
        if (isIn) it.amount else -it.amount
    }
    val clearedBalance = (bankAcc?.openingBalance ?: 0.0) + relevant.filter { clearedIds.contains(it.id) }.sumOf {
        val isIn = (it.type == "Receipt" && it.accountId == bankAcc?.id) || (it.type == "Transfer" && it.toAccountId == bankAcc?.id)
        if (isIn) it.amount else -it.amount
    }
    val stmtVal = statementBalance.toDoubleOrNull() ?: clearedBalance
    val diff = stmtVal - clearedBalance
    val isReconciled = Math.abs(diff) < 0.5 && statementBalance.isNotBlank()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bank Reconciliation", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Book Balance: ${formatMoney(bookBalance)}", fontSize = 12.sp)
                        Text("Cleared: ${formatMoney(clearedBalance)}", fontSize = 12.sp, color = MasGreen)
                    }
                    OutlinedTextField(
                        value = statementBalance,
                        onValueChange = { statementBalance = it },
                        label = { Text("Bank Statement Closing Balance") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Difference: ${formatMoney(diff)}", fontWeight = FontWeight.Bold, color = if (isReconciled) MasGreen else MasRed)
                        PillBadge(if (isReconciled) "Reconciled" else "Unreconciled", if (isReconciled) "green" else "red")
                    }
                }
            }
        }

        item {
            Text("Tick Cleared Transactions", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        items(relevant) { t ->
            val checked = clearedIds.contains(t.id)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().clickable {
                    if (checked) clearedIds.remove(t.id) else clearedIds.add(t.id)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Checkbox(checked = checked, onCheckedChange = {
                            if (it) clearedIds.add(t.id) else clearedIds.remove(t.id)
                        })
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(t.description, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                            Text("${t.date} · ${t.id}", color = MasMuted, fontSize = 10.5.sp)
                        }
                    }
                    Text(formatMoney(t.amount), fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
