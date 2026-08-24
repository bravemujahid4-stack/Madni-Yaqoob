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
import com.example.data.Customer
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MasViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(viewModel: MasViewModel) {
    var subTab by remember { mutableStateOf("list") } // list, new, receipt, aging, profile
    val customers by viewModel.customers.collectAsState()
    var selectedCustomerId by remember { mutableStateOf(customers.firstOrNull()?.id ?: "") }
    var searchQuery by remember { mutableStateOf("") }

    val totalReceivable by viewModel.totalReceivable.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        SectionHeader(
            title = "Customers & Receivables",
            subtitle = "Manage customer accounts, payment terms, receipts and aging ledgers.",
            actionButton = {
                Button(
                    onClick = { subTab = "new" },
                    colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Customer", fontSize = 12.sp)
                }
            }
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { FilterChip(selected = subTab == "list", onClick = { subTab = "list" }, label = { Text("Customer List (${customers.size})") }) }
            item { FilterChip(selected = subTab == "receipt", onClick = { subTab = "receipt" }, label = { Text("+ Record Receipt") }) }
            item { FilterChip(selected = subTab == "aging", onClick = { subTab = "aging" }, label = { Text("Aging Report") }) }
            item { FilterChip(selected = subTab == "profile", onClick = { subTab = "profile" }, label = { Text("Customer Profile") }) }
        }

        when (subTab) {
            "list" -> {
                val filtered = customers.filter {
                    searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.id.contains(searchQuery, ignoreCase = true)
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        SearchBarField(query = searchQuery, onQueryChange = { searchQuery = it }, placeholder = "Search customers by name or ID...")
                    }
                    items(filtered) { c ->
                        val balance = viewModel.getCustomerBalance(c.id)
                        val overLimit = balance > c.creditLimit

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedCustomerId = c.id
                                subTab = "profile"
                            }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(c.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                        Text("${c.id} · ${c.type} · ${c.paymentTerms}", color = MasMuted, fontSize = 11.sp)
                                    }
                                    PillBadge(if (overLimit) "Over Limit" else "Active", if (overLimit) "red" else "green")
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Phone: ${c.phone}", color = MasMuted, fontSize = 11.sp)
                                    Text(
                                        "Bal: ${formatMoney(balance)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (overLimit) MasRed else MasInk,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
            "new" -> NewCustomerForm(viewModel) { subTab = "list" }
            "receipt" -> RecordReceiptForm(viewModel, customers) { subTab = "list" }
            "aging" -> CustomerAgingView(viewModel, customers)
            "profile" -> CustomerProfileView(viewModel, customers, selectedCustomerId) { selectedCustomerId = it }
        }
    }
}

@Composable
fun NewCustomerForm(viewModel: MasViewModel, onSaved: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Wholesale") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var opening by remember { mutableStateOf("0") }
    var creditLimit by remember { mutableStateOf("300000") }
    var terms by remember { mutableStateOf("Net 30") }

    val canSave = name.isNotBlank() && phone.isNotBlank()

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
                    Text("Add Customer Account", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Customer Name *") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone *") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type (e.g. Retail, Wholesale)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = terms, onValueChange = { terms = it }, label = { Text("Payment Terms") }, modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = opening, onValueChange = { opening = it }, label = { Text("Opening Balance") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = creditLimit, onValueChange = { creditLimit = it }, label = { Text("Credit Limit") }, modifier = Modifier.weight(1f))
                    }

                    Button(
                        onClick = {
                            if (canSave) {
                                val nextNum = viewModel.customers.value.size + 1
                                val customer = Customer(
                                    id = "CUST-${String.format("%03d", nextNum)}",
                                    name = name.trim(),
                                    type = type.trim(),
                                    phone = phone.trim(),
                                    email = email.trim(),
                                    address = address.trim(),
                                    openingBalance = opening.toDoubleOrNull() ?: 0.0,
                                    creditLimit = creditLimit.toDoubleOrNull() ?: 100000.0,
                                    paymentTerms = terms.trim()
                                )
                                viewModel.addCustomer(customer)
                                onSaved()
                            }
                        },
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Customer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordReceiptForm(viewModel: MasViewModel, customers: List<Customer>, onSaved: () -> Unit) {
    var selectedCustId by remember { mutableStateOf(customers.firstOrNull()?.id ?: "") }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Cash") }
    var depositAccount by remember { mutableStateOf("Cash") }
    var ref by remember { mutableStateOf("") }

    val amountVal = amount.toDoubleOrNull() ?: 0.0
    val canSave = selectedCustId.isNotBlank() && amountVal > 0.0

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
                    Text("Record Customer Receipt", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Text("Customer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    var custExpanded by remember { mutableStateOf(false) }
                    val currentCust = customers.find { it.id == selectedCustId }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth().clickable { custExpanded = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(currentCust?.name ?: "Select Customer", fontSize = 13.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = custExpanded, onDismissRequest = { custExpanded = false }) {
                            customers.forEach { c ->
                                DropdownMenuItem(text = { Text("${c.name} (Bal: ${formatMoney(viewModel.getCustomerBalance(c.id))})") }, onClick = {
                                    selectedCustId = c.id
                                    custExpanded = false
                                })
                            }
                        }
                    }

                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount Received") }, modifier = Modifier.fillMaxWidth())

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = method, onValueChange = { method = it }, label = { Text("Method (Cash/Bank)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = depositAccount, onValueChange = { depositAccount = it }, label = { Text("Deposit To Account") }, modifier = Modifier.weight(1f))
                    }

                    OutlinedTextField(value = ref, onValueChange = { ref = it }, label = { Text("Reference / Slip #") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            if (canSave) {
                                viewModel.recordCustomerReceipt(selectedCustId, null, amountVal, method, depositAccount, ref)
                                onSaved()
                            }
                        },
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(containerColor = MasGreen),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Record Receipt & Post to GL", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerAgingView(viewModel: MasViewModel, customers: List<Customer>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(customers) { c ->
            val bal = viewModel.getCustomerBalance(c.id)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(c.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(formatMoney(bal), fontWeight = FontWeight.Bold, color = if (bal > 0) MasRed else MasGreen, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Terms: ${c.paymentTerms}", color = MasMuted, fontSize = 11.sp)
                        Text("Limit: ${formatMoney(c.creditLimit)}", color = MasMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerProfileView(
    viewModel: MasViewModel,
    customers: List<Customer>,
    selectedCustomerId: String,
    onSelectCustomer: (String) -> Unit
) {
    val cust = customers.find { it.id == selectedCustomerId } ?: customers.firstOrNull() ?: return
    val balance = viewModel.getCustomerBalance(cust.id)

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${cust.id} · ${cust.type}", color = MasMuted, fontSize = 11.5.sp)
                        }
                        PillBadge(if (balance > cust.creditLimit) "Over Limit" else "Good Standing", if (balance > cust.creditLimit) "red" else "green")
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatCard("Receivable", formatMoney(balance), tone = if (balance > 0) MasRed else MasGreen, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        StatCard("Credit Limit", formatMoney(cust.creditLimit), modifier = Modifier.weight(1f))
                    }
                    Text("Phone: ${cust.phone}", fontSize = 11.5.sp)
                    Text("Email: ${cust.email}", fontSize = 11.5.sp)
                    Text("Address: ${cust.address}", fontSize = 11.5.sp)
                }
            }
        }
    }
}
