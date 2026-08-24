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
import com.example.data.JournalEntry
import com.example.data.JournalLine
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MasViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralLedgerScreen(viewModel: MasViewModel) {
    var currentSubTab by remember { mutableStateOf("journal") } // journal, new, approvals, gl, taccount
    val journal by viewModel.journal.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser.role == "Admin" || currentUser.role == "Administrator"

    val pendingCount = journal.count { it.status == "Pending Approval" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        SectionHeader(
            title = "General Ledger & Journal",
            subtitle = "Central double-entry engine powering all accounts and financial reports."
        )

        // Sub Navigation Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = currentSubTab == "journal",
                    onClick = { currentSubTab = "journal" },
                    label = { Text("Journal Entries (${journal.size})") }
                )
            }
            item {
                FilterChip(
                    selected = currentSubTab == "new",
                    onClick = { currentSubTab = "new" },
                    label = { Text("+ New Journal Entry") }
                )
            }
            item {
                FilterChip(
                    selected = currentSubTab == "approvals",
                    onClick = { currentSubTab = "approvals" },
                    label = { Text("Approvals (${pendingCount})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (pendingCount > 0) MasAmber else MasInk
                    )
                )
            }
            item {
                FilterChip(
                    selected = currentSubTab == "gl",
                    onClick = { currentSubTab = "gl" },
                    label = { Text("General Ledger") }
                )
            }
            item {
                FilterChip(
                    selected = currentSubTab == "taccount",
                    onClick = { currentSubTab = "taccount" },
                    label = { Text("Account Ledger") }
                )
            }
        }

        when (currentSubTab) {
            "journal" -> JournalEntriesList(journal)
            "new" -> NewJournalEntryForm(viewModel, accounts) { currentSubTab = "journal" }
            "approvals" -> JournalApprovalsList(viewModel, journal, isAdmin)
            "gl" -> GeneralLedgerRollup(journal, accounts)
            "taccount" -> AccountLedgerView(journal, accounts)
        }
    }
}

@Composable
fun JournalEntriesList(journal: List<JournalEntry>) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = journal.filter {
        searchQuery.isBlank() || it.id.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
    }.reversed()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            SearchBarField(query = searchQuery, onQueryChange = { searchQuery = it }, placeholder = "Search vouchers by ID or narration...")
        }

        items(filtered) { entry ->
            var expanded by remember { mutableStateOf(false) }
            val totalDebit = entry.lines.sumOf { it.debit }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(entry.id, fontWeight = FontWeight.Bold, color = MasRed, fontFamily = FontFamily.Monospace, fontSize = 12.5.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            PillBadge(entry.source, "blue")
                        }
                        PillBadge(entry.status, entry.status)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(entry.description, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(entry.date, color = MasMuted, fontSize = 11.sp)
                        Text(formatMoney(totalDebit), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, fontFamily = FontFamily.Monospace)
                    }

                    if (expanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(6.dp))
                        entry.lines.forEach { line ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(line.account, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                                if (line.debit > 0) {
                                    Text("Dr ${formatMoney(line.debit)}", color = MasGreen, fontSize = 11.5.sp, fontFamily = FontFamily.Monospace)
                                } else {
                                    Text("Cr ${formatMoney(line.credit)}", color = MasRed, fontSize = 11.5.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewJournalEntryForm(
    viewModel: MasViewModel,
    accounts: List<com.example.data.Account>,
    onSuccess: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var date by remember { mutableStateOf(sdf.format(Date())) }
    var voucherNo by remember { mutableStateOf("JE-${1000 + viewModel.journal.value.size + 1}") }
    var narration by remember { mutableStateOf("") }

    var debitAccount by remember { mutableStateOf(accounts.firstOrNull()?.name ?: "Cash") }
    var creditAccount by remember { mutableStateOf(accounts.getOrNull(1)?.name ?: "Sales Revenue") }
    var amount by remember { mutableStateOf("") }

    val amountVal = amount.toDoubleOrNull() ?: 0.0
    val canSubmit = narration.isNotBlank() && amountVal > 0.0 && debitAccount != creditAccount

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
                    Text("Manual Journal Voucher", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = voucherNo, onValueChange = { voucherNo = it }, label = { Text("Voucher #") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(
                        value = narration,
                        onValueChange = { narration = it },
                        label = { Text("Narration / Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount (Balanced Debit & Credit)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Debit Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MasGreen)
                    AccountSelector(accounts = accounts, selected = debitAccount, onSelect = { debitAccount = it })

                    Text("Credit Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MasRed)
                    AccountSelector(accounts = accounts, selected = creditAccount, onSelect = { creditAccount = it })

                    Button(
                        onClick = {
                            if (canSubmit) {
                                val entry = JournalEntry(
                                    id = voucherNo,
                                    date = date,
                                    source = "Manual",
                                    description = narration.trim(),
                                    status = "Posted",
                                    lines = listOf(
                                        JournalLine(debitAccount, amountVal, 0.0),
                                        JournalLine(creditAccount, 0.0, amountVal)
                                    )
                                )
                                val (ok, msg) = viewModel.addJournalEntry(entry)
                                if (ok) {
                                    viewModel.showMessage("Voucher $voucherNo posted.")
                                    onSuccess()
                                } else {
                                    viewModel.showMessage(msg)
                                }
                            }
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Post Journal Entry", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountSelector(
    accounts: List<com.example.data.Account>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selected, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { acc ->
                DropdownMenuItem(
                    text = { Text("${acc.code} — ${acc.name} (${acc.type.displayName})") },
                    onClick = {
                        onSelect(acc.name)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun JournalApprovalsList(viewModel: MasViewModel, journal: List<JournalEntry>, isAdmin: Boolean) {
    val pending = journal.filter { it.status == "Pending Approval" }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        if (pending.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "No journal entries waiting for approval.",
                        fontSize = 12.5.sp,
                        color = MasMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        items(pending) { entry ->
            val total = entry.lines.sumOf { it.debit }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MasAmber.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(entry.id, fontWeight = FontWeight.Bold, color = MasRed, fontFamily = FontFamily.Monospace)
                        PillBadge("Pending", "amber")
                    }
                    Text(entry.description, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Amount: ${formatMoney(total)} · Date: ${entry.date}", color = MasMuted, fontSize = 11.5.sp)

                    Spacer(modifier = Modifier.height(8.dp))
                    if (isAdmin) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { viewModel.rejectJournalEntry(entry.id) }) {
                                Text("Reject", color = MasRed)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.approveJournalEntry(entry.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = MasGreen),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Approve & Post")
                            }
                        }
                    } else {
                        Text("Only Administrator can approve.", color = MasMuted, fontSize = 10.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun GeneralLedgerRollup(journal: List<JournalEntry>, accounts: List<com.example.data.Account>) {
    val postedEntries = journal.filter { it.status == "Posted" }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(accounts) { acc ->
            val lines = postedEntries.flatMap { it.lines }.filter { it.account.equals(acc.name, ignoreCase = true) }
            val totalDebit = lines.sumOf { it.debit }
            val totalCredit = lines.sumOf { it.credit }
            val netBalance = acc.opening + if (acc.nature == "Debit") totalDebit - totalCredit else totalCredit - totalDebit

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
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(acc.code, color = MasRed, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(acc.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Text("Dr: ${formatMoney(totalDebit)} · Cr: ${formatMoney(totalCredit)}", fontSize = 11.sp, color = MasMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatMoney(netBalance), fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        PillBadge(acc.nature, if (acc.nature == "Debit") "red" else "green")
                    }
                }
            }
        }
    }
}

@Composable
fun AccountLedgerView(journal: List<JournalEntry>, accounts: List<com.example.data.Account>) {
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()?.name ?: "Cash") }
    val acc = accounts.find { it.name.equals(selectedAccount, ignoreCase = true) } ?: accounts.first()

    val relevantLines = journal.filter { it.status == "Posted" }
        .flatMap { je -> je.lines.filter { it.account.equals(acc.name, ignoreCase = true) }.map { Pair(je, it) } }
        .sortedBy { it.first.date }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            AccountSelector(accounts = accounts, selected = selectedAccount, onSelect = { selectedAccount = it })
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Opening Balance", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(formatMoney(acc.opening), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        var runningBalance = acc.opening
        items(relevantLines) { (je, line) ->
            if (acc.nature == "Debit") {
                runningBalance += (line.debit - line.credit)
            } else {
                runningBalance += (line.credit - line.debit)
            }
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
                        Text(je.description, fontWeight = FontWeight.Medium, fontSize = 12.5.sp)
                        Text("${je.date} · ${je.id}", color = MasMuted, fontSize = 10.5.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (line.debit > 0) {
                            Text("Dr ${formatMoney(line.debit)}", color = MasGreen, fontSize = 11.5.sp, fontFamily = FontFamily.Monospace)
                        } else {
                            Text("Cr ${formatMoney(line.credit)}", color = MasRed, fontSize = 11.5.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text("Bal: ${formatMoney(runningBalance)}", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
