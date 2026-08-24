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
fun InventoryScreen(viewModel: MasViewModel) {
    var subTab by remember { mutableStateOf("items") } // items, new, transfer, adjust, ledger
    val items by viewModel.stockItems.collectAsState()
    val moves by viewModel.stockMoves.collectAsState()
    val warehouses by viewModel.warehouses.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val totalValuation = remember(items, moves) { viewModel.getTotalStockValue() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        SectionHeader(
            title = "Inventory & Stock Control",
            subtitle = "Track stock on hand, reorder levels, multi-warehouse movements & adjustments.",
            actionButton = {
                Button(
                    onClick = { subTab = "new" },
                    colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item", fontSize = 12.sp)
                }
            }
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { FilterChip(selected = subTab == "items", onClick = { subTab = "items" }, label = { Text("Stock Items (${items.size})") }) }
            item { FilterChip(selected = subTab == "new", onClick = { subTab = "new" }, label = { Text("+ New Item") }) }
            item { FilterChip(selected = subTab == "transfer", onClick = { subTab = "transfer" }, label = { Text("Warehouse Transfer") }) }
            item { FilterChip(selected = subTab == "adjust", onClick = { subTab = "adjust" }, label = { Text("Adjustment") }) }
            item { FilterChip(selected = subTab == "ledger", onClick = { subTab = "ledger" }, label = { Text("Stock Movements") }) }
        }

        when (subTab) {
            "items" -> StockItemsListView(viewModel, items, searchQuery, { searchQuery = it }, totalValuation)
            "new" -> NewStockItemForm(viewModel) { subTab = "items" }
            "transfer" -> WarehouseTransferForm(viewModel, items, warehouses) { subTab = "items" }
            "adjust" -> StockAdjustmentForm(viewModel, items, warehouses) { subTab = "items" }
            "ledger" -> StockLedgerListView(items, moves)
        }
    }
}

@Composable
fun StockItemsListView(
    viewModel: MasViewModel,
    items: List<StockItem>,
    query: String,
    onQueryChange: (String) -> Unit,
    totalValuation: Double
) {
    val filtered = items.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true) || it.sku.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
    }

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
                        Text("Total Stock Valuation (Cost)", fontSize = 11.5.sp, color = MasMuted)
                        Text(formatMoney(totalValuation), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MasInk, fontFamily = FontFamily.Monospace)
                    }
                    PillBadge("Weighted Cost", "blue")
                }
            }
        }

        item {
            SearchBarField(query = query, onQueryChange = onQueryChange, placeholder = "Search items by SKU, name, category...")
        }

        items(filtered) { item ->
            val qty = viewModel.getStockItemQuantity(item.id)
            val isLowStock = qty <= item.minStock

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (isLowStock) MasRed.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Text("${item.sku} · ${item.category} · ${item.unit}", color = MasMuted, fontSize = 11.sp)
                        }
                        PillBadge(if (isLowStock) "Low Stock" else "In Stock", if (isLowStock) "red" else "green")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cost: ${formatMoney(item.costPrice)} | Sell: ${formatMoney(item.sellingPrice)}", color = MasMuted, fontSize = 11.5.sp)
                        Text(
                            text = "${formatQty(qty)} ${item.unit}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = if (isLowStock) MasRed else MasGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewStockItemForm(viewModel: MasViewModel, onSaved: () -> Unit) {
    var sku by remember { mutableStateOf("SKU-${System.currentTimeMillis() % 10000}") }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Finished Products") }
    var uom by remember { mutableStateOf("Pieces") }
    var costPrice by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }
    var reorder by remember { mutableStateOf("20") }
    var openingQty by remember { mutableStateOf("0") }

    val canSave = name.isNotBlank()

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
                    Text("Add New Inventory Item", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name *") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU / Item Code") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = uom, onValueChange = { uom = it }, label = { Text("Unit of Measure") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = costPrice, onValueChange = { costPrice = it }, label = { Text("Cost Price") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = sellPrice, onValueChange = { sellPrice = it }, label = { Text("Selling Price") }, modifier = Modifier.weight(1f))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = openingQty, onValueChange = { openingQty = it }, label = { Text("Opening Stock Qty") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = reorder, onValueChange = { reorder = it }, label = { Text("Reorder Level") }, modifier = Modifier.weight(1f))
                    }

                    Button(
                        onClick = {
                            if (canSave) {
                                val item = StockItem(
                                    id = "ITM-${System.currentTimeMillis() % 10000}",
                                    sku = sku.trim(),
                                    name = name.trim(),
                                    category = category.trim(),
                                    unit = uom.trim(),
                                    purchasePrice = costPrice.toDoubleOrNull() ?: 0.0,
                                    sellingPrice = sellPrice.toDoubleOrNull() ?: 0.0,
                                    costPrice = costPrice.toDoubleOrNull() ?: 0.0,
                                    minStock = reorder.toDoubleOrNull() ?: 10.0
                                )
                                viewModel.addStockItem(item)

                                val opVal = openingQty.toDoubleOrNull() ?: 0.0
                                if (opVal > 0) {
                                    viewModel.addStockMove(
                                        StockMove(
                                            id = "MOV-${System.currentTimeMillis() % 10000}",
                                            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                            itemId = item.id,
                                            warehouseId = "WH-01",
                                            type = "Opening",
                                            qty = opVal,
                                            reference = "Initial Opening"
                                        )
                                    )
                                }
                                onSaved()
                            }
                        },
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Inventory Item", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WarehouseTransferForm(
    viewModel: MasViewModel,
    items: List<StockItem>,
    warehouses: List<Warehouse>,
    onSaved: () -> Unit
) {
    var selectedItemId by remember { mutableStateOf(items.firstOrNull()?.id ?: "") }
    var fromWh by remember { mutableStateOf(warehouses.firstOrNull()?.id ?: "WH-01") }
    var toWh by remember { mutableStateOf(warehouses.getOrNull(1)?.id ?: "WH-02") }
    var qty by remember { mutableStateOf("") }

    val qtyVal = qty.toDoubleOrNull() ?: 0.0
    val canSubmit = selectedItemId.isNotBlank() && fromWh != toWh && qtyVal > 0.0

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
                    Text("Inter-Warehouse Stock Transfer", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = fromWh, onValueChange = { fromWh = it }, label = { Text("From Warehouse") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = toWh, onValueChange = { toWh = it }, label = { Text("To Warehouse") }, modifier = Modifier.weight(1f))
                    }

                    OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Transfer Quantity") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            if (canSubmit) {
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                viewModel.addStockMove(StockMove("MOV-${System.currentTimeMillis() % 10000}", sdf, selectedItemId, fromWh, "Transfer Out", qtyVal, reference = "Transfer to $toWh"))
                                viewModel.addStockMove(StockMove("MOV-${System.currentTimeMillis() % 10000 + 1}", sdf, selectedItemId, toWh, "Transfer In", qtyVal, reference = "Transfer from $fromWh"))
                                viewModel.showMessage("Stock transfer completed.")
                                onSaved()
                            }
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Execute Stock Transfer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StockAdjustmentForm(
    viewModel: MasViewModel,
    items: List<StockItem>,
    warehouses: List<Warehouse>,
    onSaved: () -> Unit
) {
    var selectedItemId by remember { mutableStateOf(items.firstOrNull()?.id ?: "") }
    var adjustType by remember { mutableStateOf("Adjustment +") } // Adjustment +, Adjustment -
    var qty by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Physical count variance") }

    val qtyVal = qty.toDoubleOrNull() ?: 0.0
    val canSubmit = selectedItemId.isNotBlank() && qtyVal > 0.0

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
                    Text("Stock Adjustment / Physical Count", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = adjustType, onValueChange = { adjustType = it }, label = { Text("Type (+ / -)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Variance Qty") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason for Adjustment") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            if (canSubmit) {
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                viewModel.addStockMove(StockMove("MOV-${System.currentTimeMillis() % 10000}", sdf, selectedItemId, "WH-01", adjustType, qtyVal, reference = reason))
                                onSaved()
                            }
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Post Stock Adjustment", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StockLedgerListView(items: List<StockItem>, moves: List<StockMove>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(moves.reversed()) { m ->
            val item = items.find { it.id == m.itemId }
            val isPlus = m.type == "In" || m.type == "Opening" || m.type == "Adjustment +" || m.type == "Transfer In"

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
                        Text(item?.name ?: m.itemId, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        Text("${m.date} · ${m.warehouseId} · ${m.reference ?: m.type}", color = MasMuted, fontSize = 10.5.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = (if (isPlus) "+ " else "- ") + "${formatQty(m.qty)} ${item?.unit ?: ""}",
                            color = if (isPlus) MasGreen else MasRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        PillBadge(m.type, if (isPlus) "green" else "red")
                    }
                }
            }
        }
    }
}
