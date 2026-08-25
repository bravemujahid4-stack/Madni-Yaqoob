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
fun SalesScreen(viewModel: MasViewModel) {
    var subTab by remember { mutableStateOf("docs") } // docs, new, return, postings
    val salesDocs by viewModel.salesDocs.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val stockItems by viewModel.stockItems.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        SectionHeader(
            title = "Sales Module",
            subtitle = "Quotations, Orders, Invoices (Cash & Credit), Returns, and GL Postings.",
            actionButton = {
                Button(
                    onClick = { subTab = "new" },
                    colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Sale", fontSize = 12.sp)
                }
            }
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { FilterChip(selected = subTab == "docs", onClick = { subTab = "docs" }, label = { Text("Documents (${salesDocs.size})") }) }
            item { FilterChip(selected = subTab == "new", onClick = { subTab = "new" }, label = { Text("+ Create Sale") }) }
            item { FilterChip(selected = subTab == "return", onClick = { subTab = "return" }, label = { Text("Sales Return") }) }
            item { FilterChip(selected = subTab == "postings", onClick = { subTab = "postings" }, label = { Text("Posting Impact") }) }
        }

        when (subTab) {
            "docs" -> SalesDocumentsList(salesDocs, customers)
            "new" -> NewSaleForm(viewModel, customers, stockItems) { subTab = "docs" }
            "return" -> SalesReturnForm(viewModel, salesDocs, customers) { subTab = "docs" }
            "postings" -> SalesPostingsImpact(salesDocs, customers)
        }
    }
}

@Composable
fun SalesDocumentsList(docs: List<SalesDoc>, customers: List<Customer>) {
    var query by remember { mutableStateOf("") }
    val filtered = docs.filter {
        query.isBlank() || it.id.contains(query, ignoreCase = true) || it.type.contains(query, ignoreCase = true)
    }.reversed()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            SearchBarField(query = query, onQueryChange = { query = it }, placeholder = "Search sales vouchers...")
        }

        items(filtered) { doc ->
            var expanded by remember { mutableStateOf(false) }
            val cust = customers.find { it.id == doc.customerId }
            val custName = cust?.name ?: (if (doc.customerId == "WALKIN") "Walk-in Customer" else doc.customerId)
            val total = doc.items.sumOf { it.qty * it.rate }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(doc.id, fontWeight = FontWeight.Bold, color = MasRed, fontFamily = FontFamily.Monospace, fontSize = 12.5.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            PillBadge(doc.type, if (doc.type.contains("Return")) "red" else "green")
                        }
                        PillBadge(doc.status, doc.status)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(custName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (doc.saleType == "Cash") "Cash Sale (${doc.paymentAccount ?: "Cash"})" else "Credit Sale",
                                color = if (doc.saleType == "Cash") MasGreen else MasBlue,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp
                            )
                            Text(" · ${doc.date}", color = MasMuted, fontSize = 11.sp)
                        }
                        Text(formatMoney(total), fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }

                    if (expanded) {
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(6.dp))
                        doc.items.forEach { item ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${item.description} (${formatQty(item.qty)} @ ${formatMoney(item.rate)})", fontSize = 11.sp, color = MasInk)
                                Text(formatMoney(item.qty * item.rate), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewSaleForm(
    viewModel: MasViewModel,
    customers: List<Customer>,
    stockItems: List<StockItem>,
    onSaved: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cashBankAccounts by viewModel.cashBankAccounts.collectAsState()
    
    var docType by remember { mutableStateOf("Sales Invoice") }
    var saleType by remember { mutableStateOf("Cash") } // "Cash" or "Credit"
    var selectedPaymentAccount by remember { mutableStateOf(cashBankAccounts.firstOrNull()?.name ?: "Khalid Cash 1") }
    var selectedCustId by remember { mutableStateOf(customers.firstOrNull()?.id ?: "") }
    var date by remember { mutableStateOf(sdf.format(Date())) }

    var itemDesc by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf("1") }
    var itemRate by remember { mutableStateOf("") }
    var selectedStockItem by remember { mutableStateOf<StockItem?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val qtyVal = itemQty.toDoubleOrNull() ?: 1.0
    val rateVal = itemRate.toDoubleOrNull() ?: 0.0
    val totalAmount = qtyVal * rateVal

    // Validation checks
    val isCredit = saleType == "Credit"
    val isCustomerValid = if (isCredit) selectedCustId.isNotBlank() else true
    val isAccountValid = if (!isCredit) selectedPaymentAccount.isNotBlank() else true
    val canSubmit = itemDesc.isNotBlank() && rateVal > 0.0 && isCustomerValid && isAccountValid && !isSubmitting

    val selectedCustomer = customers.find { it.id == selectedCustId }
    val currentCustBalance = if (selectedCustomer != null) viewModel.getCustomerBalance(selectedCustomer.id) else 0.0
    val newCustBalance = currentCustBalance + totalAmount

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
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Create Sales Transaction", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    // 1. Transaction Type Toggle (Cash Sale vs Credit Sale)
                    Text("Sale Category *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MasInk)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { saleType = "Cash" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (saleType == "Cash") MasGreen else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (saleType == "Cash") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cash Sale", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { 
                                saleType = "Credit"
                                if (selectedCustId.isBlank() && customers.isNotEmpty()) {
                                    selectedCustId = customers.first().id
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (saleType == "Credit") MasBlue else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (saleType == "Credit") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Credit Sale", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // 2. Dynamic Selection based on Cash or Credit
                    if (saleType == "Cash") {
                        // Cash Sale -> Payment Account Selector
                        Text("Deposit / Received In Account *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        var accExpanded by remember { mutableStateOf(false) }
                        val accountOptions = if (cashBankAccounts.isNotEmpty()) cashBankAccounts.map { it.name } else listOf("Cash in Hand", "Bank")

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth().clickable { accExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MasGreen)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (selectedPaymentAccount.contains("Bank", ignoreCase = true)) Icons.Default.AccountBalance else Icons.Default.Money,
                                            contentDescription = null,
                                            tint = MasGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(selectedPaymentAccount, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(expanded = accExpanded, onDismissRequest = { accExpanded = false }) {
                                accountOptions.forEach { accName ->
                                    DropdownMenuItem(
                                        text = { Text(accName, fontWeight = FontWeight.Medium) },
                                        leadingIcon = {
                                            Icon(
                                                if (accName.contains("Bank", ignoreCase = true)) Icons.Default.AccountBalance else Icons.Default.Money,
                                                contentDescription = null,
                                                tint = MasGreen
                                            )
                                        },
                                        onClick = {
                                            selectedPaymentAccount = accName
                                            accExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Surface(
                            color = MasGreen.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MasGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MasGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Sale amount will automatically be debited to '$selectedPaymentAccount' and credited to Sales Revenue.",
                                    fontSize = 11.5.sp,
                                    color = MasGreen
                                )
                            }
                        }
                    } else {
                        // Credit Sale -> Customer Selector
                        Text("Customer (Receivable Account) *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        var custExpanded by remember { mutableStateOf(false) }

                        if (customers.isEmpty()) {
                            Surface(
                                color = MasRed.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MasRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "No customers found. Please add a customer in the Customers module first to make a Credit Sale.",
                                        fontSize = 11.5.sp,
                                        color = MasRed
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth().clickable { custExpanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MasBlue)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = MasBlue, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                selectedCustomer?.name ?: "Select Customer *",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }
                                DropdownMenu(expanded = custExpanded, onDismissRequest = { custExpanded = false }) {
                                    customers.forEach { c ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(c.name, fontWeight = FontWeight.SemiBold)
                                                    Text("Current Balance: ${formatMoney(viewModel.getCustomerBalance(c.id))}", fontSize = 11.sp, color = MasMuted)
                                                }
                                            },
                                            onClick = {
                                                selectedCustId = c.id
                                                custExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (selectedCustomer != null) {
                                Surface(
                                    color = MasBlue.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MasBlue.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            "Credit Sale Impact: Posted to Accounts Receivable (${selectedCustomer.name})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = MasBlue
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Current Receivable: ${formatMoney(currentCustBalance)}", fontSize = 11.sp, color = MasInk)
                                            Text("New Balance: ${formatMoney(newCustBalance)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MasRed)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Document details
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = docType,
                            onValueChange = { docType = it },
                            label = { Text("Document Type") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 4. Quick stock item selector
                    Text("Stock Item (Optional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    var stockExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { stockExpanded = true }, shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(selectedStockItem?.name ?: "Pick Stock Item or Enter Custom Line", fontSize = 13.sp, color = if (selectedStockItem != null) MasInk else MasMuted)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = stockExpanded, onDismissRequest = { stockExpanded = false }) {
                            stockItems.forEach { si ->
                                DropdownMenuItem(
                                    text = { Text("${si.name} (Selling: ${formatMoney(si.sellingPrice)})") },
                                    onClick = {
                                        selectedStockItem = si
                                        itemDesc = si.name
                                        itemRate = si.sellingPrice.toString()
                                        stockExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(value = itemDesc, onValueChange = { itemDesc = it }, label = { Text("Item Description *") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = itemQty, onValueChange = { itemQty = it }, label = { Text("Quantity") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = itemRate, onValueChange = { itemRate = it }, label = { Text("Rate per Unit *") }, modifier = Modifier.weight(1f))
                    }

                    // 5. Total & Summary Banner
                    Surface(color = MasPaperSoft, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Sale Amount", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(formatMoney(totalAmount), fontWeight = FontWeight.Bold, color = if (saleType == "Cash") MasGreen else MasBlue, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Type: ${if (saleType == "Cash") "CASH SALE (into $selectedPaymentAccount)" else "CREDIT SALE (Customer: ${selectedCustomer?.name ?: "None"})"}",
                                fontSize = 11.sp,
                                color = MasMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (canSubmit) {
                                isSubmitting = true
                                val prefix = if (docType == "Quotation") "QTN" else if (docType == "Sales Order") "SO" else "INV"
                                val nextNum = 1000 + viewModel.salesDocs.value.size + 1
                                val doc = SalesDoc(
                                    id = "$prefix-$nextNum",
                                    type = docType,
                                    saleType = saleType,
                                    paymentAccount = if (saleType == "Cash") selectedPaymentAccount else null,
                                    customerId = if (saleType == "Credit") selectedCustId else "WALKIN",
                                    date = date,
                                    items = listOf(LineItem(itemId = selectedStockItem?.id, description = itemDesc.trim(), qty = qtyVal, rate = rateVal)),
                                    status = "Posted"
                                )
                                viewModel.addSalesDoc(doc)
                                onSaved()
                            }
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = if (saleType == "Cash") MasGreen else MasBlue),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Post ${if (saleType == "Cash") "Cash Sale" else "Credit Sale"}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SalesReturnForm(
    viewModel: MasViewModel,
    docs: List<SalesDoc>,
    customers: List<Customer>,
    onSaved: () -> Unit
) {
    val invoices = docs.filter { it.type == "Sales Invoice" }
    var selectedInvId by remember { mutableStateOf(invoices.firstOrNull()?.id ?: "") }
    var reason by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    val amountVal = amount.toDoubleOrNull() ?: 0.0
    val canSubmit = selectedInvId.isNotBlank() && reason.isNotBlank() && amountVal > 0.0

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
                    Text("Record Sales Return", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(value = selectedInvId, onValueChange = { selectedInvId = it }, label = { Text("Original Invoice #") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason for Return") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Return Amount") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            if (canSubmit) {
                                val original = invoices.find { it.id == selectedInvId }
                                val doc = SalesDoc(
                                    id = "SR-${1000 + docs.size + 1}",
                                    type = "Sales Return",
                                    customerId = original?.customerId ?: "CUST-001",
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                    reference = selectedInvId,
                                    items = listOf(LineItem(description = reason.trim(), qty = 1.0, rate = amountVal)),
                                    status = "Recorded"
                                )
                                viewModel.addSalesDoc(doc)
                                onSaved()
                            }
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Record Sales Return", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SalesPostingsImpact(docs: List<SalesDoc>, customers: List<Customer>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(docs.filter { it.type == "Sales Invoice" }) { doc ->
            val total = doc.items.sumOf { it.qty * it.rate }
            val debitAcc = if (doc.saleType == "Cash") (doc.paymentAccount ?: "Cash in Hand") else "Accounts Receivable"
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(doc.id, fontWeight = FontWeight.Bold, color = MasRed, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.width(6.dp))
                            PillBadge(if (doc.saleType == "Cash") "Cash" else "Credit", if (doc.saleType == "Cash") "green" else "blue")
                        }
                        Text(formatMoney(total), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("GL Impact: Dr $debitAcc ${formatMoney(total)} / Cr Sales Revenue ${formatMoney(total)}", color = MasMuted, fontSize = 10.5.sp)
                }
            }
        }
    }
}
