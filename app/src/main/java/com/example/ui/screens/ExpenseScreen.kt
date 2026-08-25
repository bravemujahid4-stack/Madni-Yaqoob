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
import com.example.data.ExpenseVoucher
import com.example.data.RecurringExpense
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MasViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(viewModel: MasViewModel) {
    var subTab by remember { mutableStateOf("vouchers") } // vouchers, new, recurring, breakdown
    val vouchers by viewModel.expenseVouchers.collectAsState()
    val recurring by viewModel.recurringExpenses.collectAsState()
    val categories by viewModel.expenseCategories.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        SectionHeader(
            title = "Expenses & Overheads",
            subtitle = "Track operating overheads, recurring charges & automatic journal postings.",
            actionButton = {
                Button(
                    onClick = { subTab = "new" },
                    colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Expense", fontSize = 12.sp)
                }
            }
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { FilterChip(selected = subTab == "vouchers", onClick = { subTab = "vouchers" }, label = { Text("Vouchers (${vouchers.size})") }) }
            item { FilterChip(selected = subTab == "new", onClick = { subTab = "new" }, label = { Text("+ Record Voucher") }) }
            item { FilterChip(selected = subTab == "recurring", onClick = { subTab = "recurring" }, label = { Text("Recurring (${recurring.size})") }) }
            item { FilterChip(selected = subTab == "breakdown", onClick = { subTab = "breakdown" }, label = { Text("Category Breakdown") }) }
        }

        when (subTab) {
            "vouchers" -> ExpenseVouchersList(vouchers, totalExpenses)
            "new" -> NewExpenseForm(viewModel, categories) { subTab = "vouchers" }
            "recurring" -> RecurringExpenseList(recurring)
            "breakdown" -> ExpenseBreakdownView(vouchers, categories)
        }
    }
}

@Composable
fun ExpenseVouchersList(vouchers: List<ExpenseVoucher>, total: Double) {
    var query by remember { mutableStateOf("") }
    val filtered = vouchers.filter {
        query.isBlank() || it.id.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) || (it.payee?.contains(query, ignoreCase = true) == true)
    }.reversed()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MasPaperSoft),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Recorded Expenses", fontSize = 11.5.sp, color = MasMuted)
                        Text(formatMoney(total), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MasRed, fontFamily = FontFamily.Monospace)
                    }
                    PillBadge("Posted to GL", "blue")
                }
            }
        }

        item {
            SearchBarField(query = query, onQueryChange = { query = it }, placeholder = "Search vouchers by payee or category...")
        }

        items(filtered) { v ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(v.id, fontWeight = FontWeight.Bold, color = MasRed, fontFamily = FontFamily.Monospace, fontSize = 12.5.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            PillBadge(v.category, "amber")
                        }
                        Text(formatMoney(v.amount), fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(v.payee ?: "Expense", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${v.date} · Paid via ${v.paidFrom ?: v.paymentType}", color = MasMuted, fontSize = 11.sp)
                        PillBadge(v.status, v.status)
                    }
                }
            }
        }
    }
}

@Composable
fun NewExpenseForm(
    viewModel: MasViewModel,
    categories: List<String>,
    onSaved: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var category by remember { mutableStateOf(categories.firstOrNull() ?: "Factory Power & Electricity") }
    var payee by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val cashBankAccounts by viewModel.cashBankAccounts.collectAsState()
    var paidFrom by remember { mutableStateOf(cashBankAccounts.firstOrNull()?.name ?: "Khalid Cash 1") }
    var date by remember { mutableStateOf(sdf.format(Date())) }

    val amountVal = amount.toDoubleOrNull() ?: 0.0
    val canSubmit = payee.isNotBlank() && amountVal > 0.0

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
                    Text("Record Operating Expense Voucher", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Text("Expense Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    var catExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { catExpanded = true }, shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(category, fontSize = 13.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                            categories.forEach { c ->
                                DropdownMenuItem(text = { Text(c) }, onClick = {
                                    category = c
                                    catExpanded = false
                                })
                            }
                        }
                    }

                    OutlinedTextField(value = payee, onValueChange = { payee = it }, label = { Text("Paid To / Vendor / Narration *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Expense Amount *") }, modifier = Modifier.fillMaxWidth())

                    Text("Paid From (Cash / Bank Account)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    var accExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { accExpanded = true }, shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(paidFrom, fontSize = 13.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = accExpanded, onDismissRequest = { accExpanded = false }) {
                            cashBankAccounts.forEach { acc ->
                                DropdownMenuItem(text = { Text("${acc.name} (${acc.kind})") }, onClick = {
                                    paidFrom = acc.name
                                    accExpanded = false
                                })
                            }
                        }
                    }

                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Expense Date") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            if (canSubmit) {
                                val docId = "EXP-${1000 + viewModel.expenseVouchers.value.size + 1}"
                                val voucher = ExpenseVoucher(
                                    id = docId,
                                    date = date,
                                    category = category,
                                    description = payee.trim(),
                                    amount = amountVal,
                                    paymentType = if (paidFrom.contains("Bank", ignoreCase = true)) "Bank" else "Cash",
                                    paidFrom = paidFrom,
                                    payee = payee.trim(),
                                    status = "Posted"
                                )
                                viewModel.addExpenseVoucher(voucher)
                                onSaved()
                            }
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Post Expense Voucher", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RecurringExpenseList(recurring: List<RecurringExpense>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(recurring) { r ->
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
                        Text(r.category, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        Text("${r.paymentType} · Next: ${r.nextDueDate} (${r.frequency})", color = MasMuted, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatMoney(r.amount), fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        PillBadge("Active Schedule", "green")
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseBreakdownView(vouchers: List<ExpenseVoucher>, categories: List<String>) {
    val total = vouchers.sumOf { it.amount }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(categories) { cat ->
            val catTotal = vouchers.filter { it.category == cat }.sumOf { it.amount }
            val pct = if (total > 0) (catTotal / total * 100).toInt() else 0

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(cat, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(formatMoney(catTotal), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (pct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MasRed,
                        trackColor = MasPaperSoft
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$pct% of total expenses", color = MasMuted, fontSize = 10.5.sp)
                }
            }
        }
    }
}
