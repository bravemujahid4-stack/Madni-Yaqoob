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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Supplier
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen(viewModel: MasViewModel) {
    var subTab by remember { mutableStateOf("list") } // list, new, aging, profile
    val suppliers by viewModel.suppliers.collectAsState()
    var selectedSupplierId by remember { mutableStateOf(suppliers.firstOrNull()?.id ?: "") }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        SectionHeader(
            title = "Suppliers & Payables",
            subtitle = "Manage vendor payables, payment terms, and vendor ledgers.",
            actionButton = {
                Button(
                    onClick = { subTab = "new" },
                    colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Supplier", fontSize = 12.sp)
                }
            }
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { FilterChip(selected = subTab == "list", onClick = { subTab = "list" }, label = { Text("Supplier List (${suppliers.size})") }) }
            item { FilterChip(selected = subTab == "new", onClick = { subTab = "new" }, label = { Text("+ New Supplier") }) }
            item { FilterChip(selected = subTab == "aging", onClick = { subTab = "aging" }, label = { Text("Payables Aging") }) }
            item { FilterChip(selected = subTab == "profile", onClick = { subTab = "profile" }, label = { Text("Supplier Profile") }) }
        }

        when (subTab) {
            "list" -> {
                val filtered = suppliers.filter {
                    searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.id.contains(searchQuery, ignoreCase = true)
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        SearchBarField(query = searchQuery, onQueryChange = { searchQuery = it }, placeholder = "Search suppliers by name or ID...")
                    }
                    items(filtered) { s ->
                        val balance = viewModel.getSupplierBalance(s.id)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedSupplierId = s.id
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
                                        Text(s.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                        Text("${s.id} · ${s.type} · ${s.creditTerms}", color = MasMuted, fontSize = 11.sp)
                                    }
                                    PillBadge(if (balance > 0) "Balance Due" else "Settled", if (balance > 0) "amber" else "green")
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Phone: ${s.phone}", color = MasMuted, fontSize = 11.sp)
                                    Text(
                                        "Payable: ${formatMoney(balance)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (balance > 0) MasRed else MasGreen,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
            "new" -> NewSupplierForm(viewModel) { subTab = "list" }
            "aging" -> SupplierAgingView(viewModel, suppliers)
            "profile" -> SupplierProfileView(viewModel, suppliers, selectedSupplierId) { selectedSupplierId = it }
        }
    }
}

@Composable
fun NewSupplierForm(viewModel: MasViewModel, onSaved: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Scrap Vendor") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var opening by remember { mutableStateOf("0") }
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
                    Text("Add Supplier Account", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Supplier Name *") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone *") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Vendor Type") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = terms, onValueChange = { terms = it }, label = { Text("Credit Terms") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = opening, onValueChange = { opening = it }, label = { Text("Opening Payable Balance") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            if (canSave) {
                                val nextNum = viewModel.suppliers.value.size + 1
                                val supp = Supplier(
                                    id = "SUPP-${String.format("%03d", nextNum)}",
                                    name = name.trim(),
                                    type = type.trim(),
                                    phone = phone.trim(),
                                    email = email.trim(),
                                    address = address.trim(),
                                    openingBalance = opening.toDoubleOrNull() ?: 0.0,
                                    creditTerms = terms.trim()
                                )
                                viewModel.suppliers.value = viewModel.suppliers.value + supp
                                viewModel.showMessage("Supplier ${supp.name} added.")
                                onSaved()
                            }
                        },
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Supplier", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SupplierAgingView(viewModel: MasViewModel, suppliers: List<Supplier>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(suppliers) { s ->
            val bal = viewModel.getSupplierBalance(s.id)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(s.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(formatMoney(bal), fontWeight = FontWeight.Bold, color = if (bal > 0) MasRed else MasGreen, fontFamily = FontFamily.Monospace)
                    }
                    Text("Terms: ${s.creditTerms} · Account: ${s.payableAccount}", color = MasMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun SupplierProfileView(
    viewModel: MasViewModel,
    suppliers: List<Supplier>,
    selectedSupplierId: String,
    onSelectSupplier: (String) -> Unit
) {
    val supp = suppliers.find { it.id == selectedSupplierId } ?: suppliers.firstOrNull() ?: return
    val balance = viewModel.getSupplierBalance(supp.id)

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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(supp.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${supp.id} · ${supp.type}", color = MasMuted, fontSize = 11.5.sp)
                        }
                        PillBadge(if (balance > 0) "Balance Due" else "Settled", if (balance > 0) "amber" else "green")
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    StatCard("Payable Balance", formatMoney(balance), tone = if (balance > 0) MasRed else MasGreen)
                    Text("Phone: ${supp.phone}", fontSize = 11.5.sp)
                    Text("Email: ${supp.email}", fontSize = 11.5.sp)
                    Text("Address: ${supp.address}", fontSize = 11.5.sp)
                }
            }
        }
    }
}
