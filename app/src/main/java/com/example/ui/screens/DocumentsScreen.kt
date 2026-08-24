package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(viewModel: MasViewModel) {
    val salesDocs by viewModel.salesDocs.collectAsState()
    val companyProfile by viewModel.companyProfile.collectAsState()
    val customers by viewModel.customers.collectAsState()

    val invoices = salesDocs.filter { it.type == "Sales Invoice" }
    var selectedInvoiceId by remember { mutableStateOf(invoices.firstOrNull()?.id ?: "") }
    val currentInvoice = invoices.find { it.id == selectedInvoiceId } ?: invoices.firstOrNull()
    val cust = customers.find { it.id == currentInvoice?.customerId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
    ) {
        item {
            SectionHeader(
                title = "Document Print & Share Center",
                subtitle = "Generate branded invoices, sales orders, bills & customer statements."
            )
        }

        // Invoice Selector dropdown
        item {
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { expanded = true }, shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Selected Invoice: ${currentInvoice?.id ?: "None"}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    invoices.forEach { inv ->
                        DropdownMenuItem(text = { Text("${inv.id} — ${inv.date} (${formatMoney(inv.items.sumOf { it.qty * it.rate })})") }, onClick = {
                            selectedInvoiceId = inv.id
                            expanded = false
                        })
                    }
                }
            }
        }

        // Printable Document Sheet (MAS Branded)
        if (currentInvoice != null) {
            val totalAmount = currentInvoice.items.sumOf { it.qty * it.rate }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MasRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MasLogoBadge(size = 32.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(companyProfile.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(companyProfile.address, fontSize = 10.sp, color = MasMuted)
                                    Text("Tel: ${companyProfile.phone} · ${companyProfile.email}", fontSize = 10.sp, color = MasMuted)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("SALES INVOICE", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MasRed)
                                Text(currentInvoice.id, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Text("Date: ${currentInvoice.date}", fontSize = 11.sp, color = MasMuted)
                            }
                        }

                        HorizontalDivider(color = MasRed.copy(alpha = 0.4f), thickness = 1.5.dp)

                        // Buyer info
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("INVOICE TO:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MasMuted)
                                Text(cust?.name ?: "Walk-in Customer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (cust != null) {
                                    Text(cust.address, fontSize = 11.sp, color = MasMuted)
                                    Text("Phone: ${cust.phone}", fontSize = 11.sp, color = MasMuted)
                                    Text("Terms: ${cust.paymentTerms}", fontSize = 11.sp, color = MasMuted)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("PAYMENT:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MasMuted)
                                Text(currentInvoice.saleType ?: "Credit", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                PillBadge(currentInvoice.status, "green")
                            }
                        }

                        // Line items table
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Description", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f))
                                Text("Qty", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.7f))
                                Text("Rate", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Text("Amount", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                            currentInvoice.items.forEach { line ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(line.description, fontSize = 11.sp, modifier = Modifier.weight(2f))
                                    Text(formatQty(line.qty), fontSize = 11.sp, modifier = Modifier.weight(0.7f), fontFamily = FontFamily.Monospace)
                                    Text(formatMoney(line.rate), fontSize = 11.sp, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace)
                                    Text(formatMoney(line.qty * line.rate), fontSize = 11.sp, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Subtotal & Total
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Subtotal:", fontSize = 12.sp, color = MasMuted)
                                Text(formatMoney(totalAmount), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Tax / VAT (0%):", fontSize = 12.sp, color = MasMuted)
                                Text(formatMoney(0.0), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("TOTAL DUE:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MasInk)
                                Text(formatMoney(totalAmount), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MasRed, fontFamily = FontFamily.Monospace)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        // Signature & Footer
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Column {
                                Text("Thank you for your business!", fontSize = 10.sp, color = MasMuted)
                                Text("Powered by MAS Mobile ERP", fontSize = 9.sp, color = MasRed, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(modifier = Modifier.width(100.dp).height(1.dp).background(MasMuted))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Authorized Signatory", fontSize = 9.5.sp, color = MasMuted)
                            }
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.showMessage("Invoice ${currentInvoice.id} sent to printer / PDF.") },
                        colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print / PDF")
                    }
                    OutlinedButton(
                        onClick = { viewModel.showMessage("Invoice link copied for WhatsApp sharing.") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share Link")
                    }
                }
            }
        }
    }
}
