package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
    var subTab by remember { mutableStateOf("factory_stock") } // factory_stock, opening_stock, items, new, transfer, adjust, ledger
    val items by viewModel.stockItems.collectAsState()
    val moves by viewModel.stockMoves.collectAsState()
    val warehouses by viewModel.warehouses.collectAsState()
    val partyAccounts by viewModel.partyAccounts.collectAsState()
    val openingRecords by viewModel.openingStockRecords.collectAsState()
    val factoryStockRecords = remember(partyAccounts, items, moves, openingRecords) { MasRepository.getFactoryStockRecords() }
    var searchQuery by remember { mutableStateOf("") }

    val totalValuation = remember(items, moves) { viewModel.getTotalStockValue() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        SectionHeader(
            title = "Inventory & Factory Stock",
            subtitle = "Track factory-specific opening stock, purchases, sales, and total stock balances.",
            actionButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            }
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { FilterChip(selected = subTab == "factory_stock", onClick = { subTab = "factory_stock" }, label = { Text("Factory Stock (${factoryStockRecords.size})") }) }
            item { FilterChip(selected = subTab == "opening_stock", onClick = { subTab = "opening_stock" }, label = { Text("Opening Stock (${openingRecords.size})") }) }
            item { FilterChip(selected = subTab == "items", onClick = { subTab = "items" }, label = { Text("All Items (${items.size})") }) }
            item { FilterChip(selected = subTab == "transfer", onClick = { subTab = "transfer" }, label = { Text("Transfer") }) }
            item { FilterChip(selected = subTab == "adjust", onClick = { subTab = "adjust" }, label = { Text("Adjustment") }) }
            item { FilterChip(selected = subTab == "ledger", onClick = { subTab = "ledger" }, label = { Text("Movement Log") }) }
        }

        when (subTab) {
            "factory_stock" -> FactoryStockListView(viewModel, factoryStockRecords)
            "opening_stock" -> OpeningStockListView(viewModel, openingRecords, partyAccounts)
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

@Composable
fun FactoryStockListView(
    viewModel: MasViewModel,
    factories: List<FactoryStockRecord>
) {
    val totalFactoryQty = factories.sumOf { it.totalQuantity }
    val totalFactoryVal = factories.sumOf { it.totalValue }
    val totalOpeningQty = factories.sumOf { it.openingStockQty }
    val totalPurchasesQty = factories.sumOf { it.purchasesQty }
    val totalSalesQty = factories.sumOf { it.salesQty }

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
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Factory Stock Valuation", fontSize = 11.5.sp, color = MasMuted)
                            Text(formatMoney(totalFactoryVal), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MasInk, fontFamily = FontFamily.Monospace)
                        }
                        PillBadge("${factories.size} Factories", "orange")
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Opening", fontSize = 10.sp, color = MasMuted)
                            Text("${formatQty(totalOpeningQty)} Qty", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MasBlue)
                        }
                        Column {
                            Text("Purchases", fontSize = 10.sp, color = MasMuted)
                            Text("+ ${formatQty(totalPurchasesQty)} Qty", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MasGreen)
                        }
                        Column {
                            Text("Sales", fontSize = 10.sp, color = MasMuted)
                            Text("- ${formatQty(totalSalesQty)} Qty", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MasRed)
                        }
                        Column {
                            Text("Current Stock", fontSize = 10.sp, color = MasMuted)
                            Text("${formatQty(totalFactoryQty)} Qty", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MasInk)
                        }
                    }
                }
            }
        }

        if (factories.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Factory, contentDescription = null, tint = MasMuted, modifier = Modifier.size(36.dp))
                        Text("No Factory Accounts Found", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Create a party account with Category 'Factory / Inventory' (FAC prefix) or import via Excel.", fontSize = 11.5.sp, color = MasMuted)
                    }
                }
            }
        }

        items(factories) { fac ->
            var expanded by remember { mutableStateOf(false) }

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
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFE65100).copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Factory, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(fac.factoryName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                Text(fac.factoryCode, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = MasMuted)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatMoney(fac.totalValue), fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = MasInk, fontFamily = FontFamily.Monospace)
                            Text("${formatQty(fac.totalQuantity)} Total Qty", fontSize = 10.5.sp, color = MasGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Metrics row
                    Surface(
                        color = MasPaperSoft,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Opening", fontSize = 9.5.sp, color = MasMuted)
                                Text(formatQty(fac.openingStockQty), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MasBlue)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Purchases", fontSize = 9.5.sp, color = MasMuted)
                                Text("+ ${formatQty(fac.purchasesQty)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MasGreen)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Sales", fontSize = 9.5.sp, color = MasMuted)
                                Text("- ${formatQty(fac.salesQty)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MasRed)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Available", fontSize = 9.5.sp, color = MasMuted)
                                Text(formatQty(fac.totalQuantity), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MasInk)
                            }
                        }
                    }

                    if (fac.items.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${fac.items.size} Item(s) in Stock", fontSize = 11.sp, color = MasBlue, fontWeight = FontWeight.SemiBold)
                            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = MasBlue, modifier = Modifier.size(18.dp))
                        }

                        if (expanded) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                fac.items.forEach { itm ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(itm.itemName, fontSize = 11.sp, color = MasInk, modifier = Modifier.weight(1f))
                                        Text(
                                            "${formatQty(itm.quantity)} ${itm.unit} · ${formatMoney(itm.totalValue)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
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
fun OpeningStockListView(
    viewModel: MasViewModel,
    records: List<OpeningStockRecord>,
    partyAccounts: List<PartyAccount>
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<OpeningStockRecord?>(null) }

    val totalOpQty = records.sumOf { it.openingQty }
    val totalOpVal = records.sumOf { it.openingValue }

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
                        Text("Total Opening Stock Valuation", fontSize = 11.5.sp, color = MasMuted)
                        Text(formatMoney(totalOpVal), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MasInk, fontFamily = FontFamily.Monospace)
                        Text("${formatQty(totalOpQty)} Total Units across ${records.size} records", fontSize = 10.5.sp, color = MasMuted)
                    }
                    Button(
                        onClick = {
                            editingRecord = null
                            showAddDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Opening", fontSize = 11.5.sp)
                    }
                }
            }
        }

        if (records.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = MasMuted, modifier = Modifier.size(36.dp))
                        Text("No Opening Stock Records", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Opening stock entries can be added manually or imported from Excel with factory accounts.", fontSize = 11.5.sp, color = MasMuted)
                    }
                }
            }
        }

        items(records) { rec ->
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rec.itemName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Text("${rec.factoryName} (${rec.factoryId}) · ${rec.openingDate}", fontSize = 11.sp, color = MasMuted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatMoney(rec.openingValue), fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = MasInk, fontFamily = FontFamily.Monospace)
                            Text("${formatQty(rec.openingQty)} ${rec.unit} @ Rs ${rec.openingRate}", fontSize = 10.5.sp, color = MasBlue)
                        }
                    }

                    if (rec.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(rec.notes, fontSize = 10.5.sp, color = MasMuted)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = {
                                editingRecord = rec
                                showAddDialog = true
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MasMuted, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { viewModel.deleteOpeningStockRecord(rec.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MasRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddOpeningStockDialog(
            record = editingRecord,
            partyAccounts = partyAccounts,
            onSave = { newRec ->
                viewModel.saveOpeningStockRecord(newRec, updateExisting = editingRecord != null)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOpeningStockDialog(
    record: OpeningStockRecord?,
    partyAccounts: List<PartyAccount>,
    onSave: (OpeningStockRecord) -> Unit,
    onDismiss: () -> Unit
) {
    val factories = partyAccounts.filter { it.accountType == PartyAccountType.Factory }
    val defaultFactory = factories.firstOrNull()

    var itemName by remember { mutableStateOf(record?.itemName ?: "") }
    var selectedFactoryId by remember { mutableStateOf(record?.factoryId ?: defaultFactory?.code ?: "FAC-001") }
    var selectedFactoryName by remember { mutableStateOf(record?.factoryName ?: defaultFactory?.name ?: "Factory") }
    var qtyStr by remember { mutableStateOf(if (record != null && record.openingQty > 0) record.openingQty.toString() else "") }
    var unit by remember { mutableStateOf(record?.unit ?: "Kg") }
    var rateStr by remember { mutableStateOf(if (record != null && record.openingRate > 0) record.openingRate.toString() else "") }
    var notes by remember { mutableStateOf(record?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (record != null) "Edit Opening Stock" else "Add Opening Stock", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item / Material Name *", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Factory / Location:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MasMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (factories.isEmpty()) {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text("FAC-001 Factory", fontSize = 10.5.sp) }
                            )
                        }
                    } else {
                        items(factories) { fac ->
                            FilterChip(
                                selected = selectedFactoryId == fac.code,
                                onClick = {
                                    selectedFactoryId = fac.code
                                    selectedFactoryName = fac.name
                                },
                                label = { Text("${fac.code} - ${fac.name}", fontSize = 10.5.sp) }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("Opening Qty *", fontSize = 10.5.sp) },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit", fontSize = 10.5.sp) },
                        modifier = Modifier.weight(0.8f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = rateStr,
                    onValueChange = { rateStr = it },
                    label = { Text("Opening Cost Rate (Rs/Unit) *", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                val q = qtyStr.toDoubleOrNull() ?: 0.0
                val r = rateStr.toDoubleOrNull() ?: 0.0
                if (q > 0 && r > 0) {
                    Surface(
                        color = MasGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Total Opening Value: ${formatMoney(q * r)} (Posts Dr Inventory / Cr Capital)",
                            color = MasGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
                    Text(it, color = MasRed, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (itemName.isBlank()) {
                        errorMessage = "Item name is required"
                        return@Button
                    }
                    val qty = qtyStr.toDoubleOrNull() ?: 0.0
                    if (qty <= 0.0) {
                        errorMessage = "Please enter a valid quantity"
                        return@Button
                    }
                    val rate = rateStr.toDoubleOrNull() ?: 0.0
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                    val newRec = OpeningStockRecord(
                        id = record?.id ?: "OP-${System.currentTimeMillis() % 100000}",
                        itemId = record?.itemId ?: "ITM-${System.currentTimeMillis() % 10000}",
                        itemName = itemName.trim(),
                        factoryId = selectedFactoryId,
                        factoryName = selectedFactoryName,
                        openingQty = qty,
                        unit = unit.ifBlank { "Kg" },
                        openingRate = rate,
                        openingValue = qty * rate,
                        openingDate = record?.openingDate ?: sdf,
                        notes = notes.trim()
                    )
                    onSave(newRec)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MasRed)
            ) {
                Text(if (record != null) "Update" else "Save Opening", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    )
}
