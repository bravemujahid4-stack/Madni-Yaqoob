package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        SectionHeader(
            title = "Cash & Bank Management",
            subtitle = "Cash in hand, petty cash, bank accounts, fund transfers & reconciliations."
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
            "accounts" -> CashBankAccountsList(accounts, txns, cashInHand, bankBalance)
            "entry" -> CashBankEntryForm(viewModel, accounts) { subTab = "accounts" }
            "transfer" -> CashBankTransferForm(viewModel, accounts) { subTab = "accounts" }
            "ledger" -> CashBankLedgerView(accounts, txns)
            "reconcile" -> BankReconciliationView(accounts, txns)
        }
    }
}

@Composable
fun CashBankAccountsList(
    accounts: List<CashBankAccount>,
    txns: List<CashBankTxn>,
    totalCash: Double,
    totalBank: Double
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Total Cash in Hand", formatMoney(totalCash), icon = Icons.Default.Money, tone = MasGreen, modifier = Modifier.weight(1f))
                StatCard("Total Bank Balance", formatMoney(totalBank), icon = Icons.Default.AccountBalance, tone = MasBlue, modifier = Modifier.weight(1f))
            }
        }

        item {
            Text("Cash Accounts", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        items(accounts.filter { it.kind == "Cash" }) { acc ->
            val inTx = txns.filter { it.accountId == acc.id && it.type == "Receipt" || it.toAccountId == acc.id }.sumOf { it.amount }
            val outTx = txns.filter { it.accountId == acc.id && it.type == "Payment" || it.fromAccountId == acc.id }.sumOf { it.amount }
            val balance = acc.openingBalance + inTx - outTx

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        Text("${acc.id} · Cash Account", color = MasMuted, fontSize = 11.sp)
                    }
                    Text(formatMoney(balance), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MasGreen, fontFamily = FontFamily.Monospace)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Bank Accounts", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        items(accounts.filter { it.kind == "Bank" }) { acc ->
            val inTx = txns.filter { it.accountId == acc.id && it.type == "Receipt" || it.toAccountId == acc.id }.sumOf { it.amount }
            val outTx = txns.filter { it.accountId == acc.id && it.type == "Payment" || it.fromAccountId == acc.id }.sumOf { it.amount }
            val balance = acc.openingBalance + inTx - outTx

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        Text("${acc.bankName ?: ""} · ${acc.accountNumber ?: ""}", color = MasMuted, fontSize = 11.sp)
                    }
                    Text(formatMoney(balance), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MasBlue, fontFamily = FontFamily.Monospace)
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
    var contraAccount by remember { mutableStateOf("Other Income") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(sdf.format(Date())) }

    val amountVal = amount.toDoubleOrNull() ?: 0.0
    val canSave = description.isNotBlank() && amountVal > 0.0

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
                    Text("Cash / Bank Transaction Voucher", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type (Receipt/Payment)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date") }, modifier = Modifier.weight(1f))
                    }

                    Text(if (type == "Receipt") "Deposit to Account" else "Pay from Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    var accExpanded by remember { mutableStateOf(false) }
                    val currentAcc = accounts.find { it.id == selectedAccId }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { accExpanded = true }, shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(currentAcc?.name ?: "Select Account", fontSize = 13.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = accExpanded, onDismissRequest = { accExpanded = false }) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(text = { Text("${acc.name} (${acc.kind})") }, onClick = {
                                    selectedAccId = acc.id
                                    accExpanded = false
                                })
                            }
                        }
                    }

                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Narration / Purpose") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = contraAccount, onValueChange = { contraAccount = it }, label = { Text("Contra Account (e.g. Sales, Rent, Capital)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            if (canSave) {
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
                                viewModel.addJournalEntry(
                                    JournalEntry(
                                        id = "JE-${txn.id}",
                                        date = date,
                                        source = "Cash & Bank",
                                        description = description.trim(),
                                        reference = txn.id,
                                        lines = listOf(JournalLine(debit, amountVal, 0.0), JournalLine(credit, 0.0, amountVal))
                                    )
                                )
                                onSaved()
                            }
                        },
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Record Transaction", fontWeight = FontWeight.Bold)
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
