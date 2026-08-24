package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun BusinessReportsScreen(viewModel: MasViewModel) {
    var reportTab by remember { mutableStateOf("sales") } // sales, inventory, customers, manufacturing
    val customers by viewModel.customers.collectAsState()
    val salesDocs by viewModel.salesDocs.collectAsState()
    val stockItems by viewModel.stockItems.collectAsState()
    val orders by viewModel.productionOrders.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        SectionHeader(
            title = "Operational Business Analytics",
            subtitle = "Sales trends, top customer performance, stock turn & production yield."
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { FilterChip(selected = reportTab == "sales", onClick = { reportTab = "sales" }, label = { Text("Sales Analysis") }) }
            item { FilterChip(selected = reportTab == "inventory", onClick = { reportTab = "inventory" }, label = { Text("Inventory Turnover") }) }
            item { FilterChip(selected = reportTab == "customers", onClick = { reportTab = "customers" }, label = { Text("Top Customers") }) }
            item { FilterChip(selected = reportTab == "manufacturing", onClick = { reportTab = "manufacturing" }, label = { Text("Production Yield") }) }
        }

        when (reportTab) {
            "sales" -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Sales by Customer Contribution", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                customers.forEach { cust ->
                                    val sales = salesDocs.filter { it.customerId == cust.id && it.type == "Sales Invoice" }.sumOf { doc -> doc.items.sumOf { it.qty * it.rate } }
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(cust.name, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                                        Text(formatMoney(sales), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }
            }
            "inventory" -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Stock Valuation & Margins", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                stockItems.forEach { item ->
                                    val qty = viewModel.getStockItemQuantity(item.id)
                                    val valAtCost = qty * item.costPrice
                                    val margin = if (item.costPrice > 0) ((item.sellingPrice - item.costPrice) / item.sellingPrice * 100).toInt() else 0

                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                            Text("${formatQty(qty)} ${item.unit} on hand · Margin: $margin%", color = MasMuted, fontSize = 11.sp)
                                        }
                                        Text(formatMoney(valAtCost), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }
            }
            "customers" -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Customer Balances & Credit Utilization", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                customers.forEach { cust ->
                                    val bal = viewModel.getCustomerBalance(cust.id)
                                    val util = if (cust.creditLimit > 0) (bal / cust.creditLimit * 100).toInt() else 0

                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(cust.name, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                            Text("Limit: ${formatMoney(cust.creditLimit)} · Util: $util%", color = MasMuted, fontSize = 11.sp)
                                        }
                                        Text(formatMoney(bal), fontWeight = FontWeight.Bold, color = if (bal > cust.creditLimit) MasRed else MasInk, fontSize = 12.5.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }
            }
            "manufacturing" -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Production Run Yield & Efficiency", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                orders.forEach { ord ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${ord.id} · Order Date: ${ord.date}", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                            Text("Planned Qty: ${formatQty(ord.plannedQty)} · BOM: ${ord.bomId}", color = MasMuted, fontSize = 11.sp)
                                        }
                                        PillBadge("Active WIP", "green")
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
