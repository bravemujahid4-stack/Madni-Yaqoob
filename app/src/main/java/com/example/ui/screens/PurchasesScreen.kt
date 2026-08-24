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
fun PurchasesScreen(viewModel: MasViewModel) {
    var subTab by remember { mutableStateOf("docs") } // docs, new, return, payment, postings
    val purchaseDocs by viewModel.purchaseDocs.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val stockItems by viewModel.stockItems.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        SectionHeader(
            title = "Purchase Module",
            subtitle = "Purchase Orders, Purchase Bills (Cash & Credit), Returns, and Supplier Payments.",
            actionButton = {
                Button(
                    onClick = { subTab = "new" },
                    colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Purchase", fontSize = 12.sp)
                }
            }
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { FilterChip(selected = subTab == "docs", onClick = { subTab = "docs" }, label = { Text("Documents (${purchaseDocs.size})") }) }
            item { FilterChip(selected = subTab == "new", onClick = { subTab = "new" }, label = { Text("+ Purchase Bill") }) }
            item { FilterChip(selected = subTab == "payment", onClick = { subTab = "payment" }, label = { Text("Supplier Payment") }) }
            item { FilterChip(selected = subTab == "return", onClick = { subTab = "return" }, label = { Text("Purchase Return") }) }
            item { FilterChip(selected = subTab == "postings", onClick = { subTab = "postings" }, label = { Text("Posting Impact") }) }
        }

        when (subTab) {
            "docs" -> PurchaseDocumentsList(purchaseDocs, suppliers)
            "new" -> NewPurchaseForm(viewModel, suppliers, stockItems) { subTab = "docs" }
            "payment" -> SupplierPaymentForm(viewModel, suppliers) { subTab = "docs" }
            "return" -> PurchaseReturnForm(viewModel, purchaseDocs, suppliers) { subTab = "docs" }
            "postings" -> PurchasePostingsImpact(purchaseDocs)
        }
    }
}

@Composable
fun PurchaseDocumentsList(docs: List<PurchaseDoc>, suppliers: List<Supplier>) {
    var query by remember { mutableStateOf("") }
    val filtered = docs.filter {
        query.isBlank() || it.id.contains(query, ignoreCase = true) || it.type.contains(query, ignoreCase = true)
    }.reversed()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            SearchBarField(query = query, onQueryChange = { query = it }, placeholder = "Search purchase vouchers...")
        }

        items(filtered) { doc ->
            var expanded by remember { mutableStateOf(false) }
            val supp = suppliers.find { it.id == doc.supplierId }
            val suppName = supp?.name ?: (if (doc.supplierId == "CASH-SUPP") "Cash Purchase (No Account)" else doc.supplierId)
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
                            PillBadge(doc.type, "amber")
                        }
                        PillBadge(doc.status, doc.status)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(suppName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
fun NewPurchaseForm(
    viewModel: MasViewModel,
    suppliers: List<Supplier>,
    stockItems: List<StockItem>,
    onSaved: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var docType by remember { mutableStateOf("Purchase Bill") }
    var saleType by remember { mutableStateOf("Credit") }
    var selectedSuppId by remember { mutableStateOf(suppliers.firstOrNull()?.id ?: "CASH-SUPP") }
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
                    Text("Record Purchase Bill", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = docType, onValueChange = { docType = it }, label = { Text("Doc Type") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = saleType, onValueChange = { saleType = it }, label = { Text("Cash / Credit") }, modifier = Modifier.weight(1f))
                    }

                    Text("Supplier", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    var suppExpanded by remember { mutableStateOf(false) }
                    val currentSupp = suppliers.find { it.id == selectedSuppId }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { suppExpanded = true }, shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(currentSupp?.name ?: if (selectedSuppId == "CASH-SUPP") "Cash Purchase (No Account)" else selectedSuppId, fontSize = 13.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = suppExpanded, onDismissRequest = { suppExpanded = false }) {
                            DropdownMenuItem(text = { Text("Cash Purchase (No Account)") }, onClick = { selectedSuppId = "CASH-SUPP"; suppExpanded = false })
                            suppliers.forEach { s ->
                                DropdownMenuItem(text = { Text(s.name) }, onClick = { selectedSuppId = s.id; suppExpanded = false })
                            }
                        }
                    }

                    Text("Pick Stock Item (Optional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    var stockExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { stockExpanded = true }, shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(selectedStockItem?.name ?: "Custom Purchase Line", fontSize = 13.sp, color = if (selectedStockItem != null) MasInk else MasMuted)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = stockExpanded, onDismissRequest = { stockExpanded = false }) {
                            stockItems.forEach { si ->
                                DropdownMenuItem(
                                    text = { Text("${si.name} (Cost: ${formatMoney(si.costPrice)})") },
                                    onClick = {
                                        selectedStockItem = si
                                        itemDesc = si.name
                                        itemRate = si.costPrice.toString()
                                        stockExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(value = itemDesc, onValueChange = { itemDesc = it }, label = { Text("Purchase Item Description *") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = itemQty, onValueChange = { itemQty = it }, label = { Text("Quantity") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = itemRate, onValueChange = { itemRate = it }, label = { Text("Purchase Rate") }, modifier = Modifier.weight(1f))
                    }

                    Surface(color = MasPaperSoft, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Bill Amount", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(formatMoney(totalAmount), fontWeight = FontWeight.Bold, color = MasAmber, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Button(
                        onClick = {
                            if (canSubmit) {
                                val prefix = if (docType == "Purchase Order") "PO" else "BILL"
                                val nextNum = 1000 + viewModel.purchaseDocs.value.size + 1
                                val doc = PurchaseDoc(
                                    id = "$prefix-$nextNum",
                                    type = docType,
                                    saleType = saleType,
                                    supplierId = selectedSuppId,
                                    date = date,
                                    items = listOf(LineItem(itemId = selectedStockItem?.id, description = itemDesc.trim(), qty = qtyVal, rate = rateVal)),
                                    status = "Posted"
                                )
                                viewModel.addPurchaseDoc(doc)
                                onSaved()
                            }
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Post Purchase Bill", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SupplierPaymentForm(
    viewModel: MasViewModel,
    suppliers: List<Supplier>,
    onSaved: () -> Unit
) {
    var selectedSuppId by remember { mutableStateOf(suppliers.firstOrNull()?.id ?: "") }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Bank Transfer") }
    var paidFromAccount by remember { mutableStateOf("Bank") }
    var ref by remember { mutableStateOf("") }

    val amountVal = amount.toDoubleOrNull() ?: 0.0
    val canSave = selectedSuppId.isNotBlank() && amountVal > 0.0

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
                    Text("Make Supplier Payment", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Text("Supplier", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    var suppExpanded by remember { mutableStateOf(false) }
                    val currentSupp = suppliers.find { it.id == selectedSuppId }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { suppExpanded = true }, shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(currentSupp?.name ?: "Select Supplier", fontSize = 13.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = suppExpanded, onDismissRequest = { suppExpanded = false }) {
                            suppliers.forEach { s ->
                                DropdownMenuItem(text = { Text("${s.name} (Payable: ${formatMoney(viewModel.getSupplierBalance(s.id))})") }, onClick = {
                                    selectedSuppId = s.id
                                    suppExpanded = false
                                })
                            }
                        }
                    }

                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount Paid") }, modifier = Modifier.fillMaxWidth())

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = method, onValueChange = { method = it }, label = { Text("Payment Mode") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = paidFromAccount, onValueChange = { paidFromAccount = it }, label = { Text("Paid From Account") }, modifier = Modifier.weight(1f))
                    }

                    OutlinedTextField(value = ref, onValueChange = { ref = it }, label = { Text("Cheque / Ref #") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            if (canSave) {
                                viewModel.recordSupplierPayment(selectedSuppId, null, amountVal, method, paidFromAccount, ref)
                                onSaved()
                            }
                        },
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Record Payment & Clear Payable", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseReturnForm(
    viewModel: MasViewModel,
    docs: List<PurchaseDoc>,
    suppliers: List<Supplier>,
    onSaved: () -> Unit
) {
    val bills = docs.filter { it.type == "Purchase Bill" }
    var selectedBillId by remember { mutableStateOf(bills.firstOrNull()?.id ?: "") }
    var reason by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    val amountVal = amount.toDoubleOrNull() ?: 0.0
    val canSubmit = selectedBillId.isNotBlank() && reason.isNotBlank() && amountVal > 0.0

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
                    Text("Record Purchase Return", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(value = selectedBillId, onValueChange = { selectedBillId = it }, label = { Text("Original Bill #") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason for Return") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Return Amount") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            if (canSubmit) {
                                val original = bills.find { it.id == selectedBillId }
                                val doc = PurchaseDoc(
                                    id = "PR-${1000 + docs.size + 1}",
                                    type = "Purchase Return",
                                    supplierId = original?.supplierId ?: "SUPP-001",
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                    reference = selectedBillId,
                                    items = listOf(LineItem(description = reason.trim(), qty = 1.0, rate = amountVal)),
                                    status = "Recorded"
                                )
                                viewModel.addPurchaseDoc(doc)
                                onSaved()
                            }
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Record Purchase Return", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PurchasePostingsImpact(docs: List<PurchaseDoc>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(docs.filter { it.type == "Purchase Bill" }) { doc ->
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
                    Text("GL Impact: Dr Purchases ${formatMoney(total)} / Cr Accounts Payable ${formatMoney(total)}", color = MasMuted, fontSize = 10.5.sp)
                }
            }
        }
    }
}
