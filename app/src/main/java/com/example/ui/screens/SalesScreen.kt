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
                        Text("${doc.date} · ${doc.saleType ?: "Standard"}", color = MasMuted, fontSize = 11.sp)
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
    var docType by remember { mutableStateOf("Sales Invoice") }
    var saleType by remember { mutableStateOf("Credit") }
    var selectedCustId by remember { mutableStateOf(customers.firstOrNull()?.id ?: "WALKIN") }
    var date by remember { mutableStateOf(sdf.format(Date())) }

    var itemDesc by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf("1") }
    var itemRate by remember { mutableStateOf("") }
    var selectedStockItem by remember { mutableStateOf<StockItem?>(null) }

    val qtyVal = itemQty.toDoubleOrNull() ?: 1.0
    val rateVal = itemRate.toDoubleOrNull() ?: 0.0
    val totalAmount = qtyVal * rateVal
    val canSubmit = itemDesc.isNotBlank() && rateVal > 0.0

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
                    Text("Create Sales Transaction", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = docType, onValueChange = { docType = it }, label = { Text("Document Type") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = saleType, onValueChange = { saleType = it }, label = { Text("Payment (Cash/Credit)") }, modifier = Modifier.weight(1f))
                    }

                    Text("Customer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    var custExpanded by remember { mutableStateOf(false) }
                    val currentCust = customers.find { it.id == selectedCustId }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { custExpanded = true }, shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(currentCust?.name ?: if (selectedCustId == "WALKIN") "Walk-in Customer" else selectedCustId, fontSize = 13.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = custExpanded, onDismissRequest = { custExpanded = false }) {
                            DropdownMenuItem(text = { Text("Walk-in Customer") }, onClick = { selectedCustId = "WALKIN"; custExpanded = false })
                            customers.forEach { c ->
                                DropdownMenuItem(text = { Text(c.name) }, onClick = { selectedCustId = c.id; custExpanded = false })
                            }
                        }
                    }

                    Text("Quick Stock Item Selector (Optional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    var stockExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { stockExpanded = true }, shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(selectedStockItem?.name ?: "Custom Line / Pick Stock Item", fontSize = 13.sp, color = if (selectedStockItem != null) MasInk else MasMuted)
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
                        OutlinedTextField(value = itemRate, onValueChange = { itemRate = it }, label = { Text("Rate per Unit") }, modifier = Modifier.weight(1f))
                    }

                    Surface(color = MasPaperSoft, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Amount", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(formatMoney(totalAmount), fontWeight = FontWeight.Bold, color = MasGreen, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Button(
                        onClick = {
                            if (canSubmit) {
                                val prefix = if (docType == "Quotation") "QTN" else if (docType == "Sales Order") "SO" else "INV"
                                val nextNum = 1000 + viewModel.salesDocs.value.size + 1
                                val doc = SalesDoc(
                                    id = "$prefix-$nextNum",
                                    type = docType,
                                    saleType = saleType,
                                    customerId = selectedCustId,
                                    date = date,
                                    items = listOf(LineItem(itemId = selectedStockItem?.id, description = itemDesc.trim(), qty = qtyVal, rate = rateVal)),
                                    status = "Posted"
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
                        Text("Post Sales Document", fontWeight = FontWeight.Bold)
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
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(doc.id, fontWeight = FontWeight.Bold, color = MasRed, fontFamily = FontFamily.Monospace)
                        Text(formatMoney(total), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Text("GL Impact: Dr Accounts Receivable ${formatMoney(total)} / Cr Sales Revenue ${formatMoney(total)}", color = MasMuted, fontSize = 10.5.sp)
                }
            }
        }
    }
}
