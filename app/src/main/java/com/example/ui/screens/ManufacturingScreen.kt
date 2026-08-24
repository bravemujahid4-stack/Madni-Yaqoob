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
fun ManufacturingScreen(viewModel: MasViewModel) {
    var subTab by remember { mutableStateOf("orders") } // orders, boms, consume, output, costing
    val orders by viewModel.productionOrders.collectAsState()
    val boms by viewModel.boms.collectAsState()
    val rawMaterials by viewModel.rawMaterials.collectAsState()
    val finishedGoods by viewModel.finishedGoods.collectAsState()
    val consumptions by viewModel.consumptions.collectAsState()
    val outputs by viewModel.productionOutputs.collectAsState()
    val overheads by viewModel.manufacturingOverheads.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        SectionHeader(
            title = "Manufacturing & Production",
            subtitle = "Bill of Materials (BOM), WIP tracking, material consumptions & production costing."
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { FilterChip(selected = subTab == "orders", onClick = { subTab = "orders" }, label = { Text("Orders (${orders.size})") }) }
            item { FilterChip(selected = subTab == "boms", onClick = { subTab = "boms" }, label = { Text("BOM Recipes (${boms.size})") }) }
            item { FilterChip(selected = subTab == "consume", onClick = { subTab = "consume" }, label = { Text("Issue Raw Materials") }) }
            item { FilterChip(selected = subTab == "output", onClick = { subTab = "output" }, label = { Text("Record Finished Goods") }) }
            item { FilterChip(selected = subTab == "costing", onClick = { subTab = "costing" }, label = { Text("Costing & WIP") }) }
        }

        when (subTab) {
            "orders" -> ProductionOrdersList(orders, finishedGoods)
            "boms" -> BomListView(boms, rawMaterials)
            "consume" -> MaterialConsumptionForm(viewModel, orders, rawMaterials) { subTab = "orders" }
            "output" -> ProductionOutputForm(viewModel, orders, finishedGoods) { subTab = "orders" }
            "costing" -> ManufacturingCostingView(orders, consumptions, outputs, overheads, boms)
        }
    }
}

@Composable
fun ProductionOrdersList(orders: List<ProductionOrder>, fgList: List<FinishedGood>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(orders) { order ->
            val fg = fgList.find { it.id == order.finishedGoodId }
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
                            Text(order.id, fontWeight = FontWeight.Bold, color = MasRed, fontFamily = FontFamily.Monospace, fontSize = 12.5.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            PillBadge("In Production", "blue")
                        }
                        Text("${formatQty(order.plannedQty)} ${fg?.unit ?: "Pcs"}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(fg?.name ?: order.finishedGoodId, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Order Date: ${order.date}", color = MasMuted, fontSize = 11.sp)
                        Text("BOM: ${order.bomId}", color = MasGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun BomListView(boms: List<BillOfMaterials>, rawMaterials: List<RawMaterial>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(boms) { bom ->
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
                        Text(bom.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        PillBadge(bom.id, "blue")
                    }
                    Text("Standard Labor Cost: ${formatMoney(bom.laborCostPerUnit)}/unit", color = MasMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Raw Material Recipe:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    bom.components.forEach { comp ->
                        val rm = rawMaterials.find { it.id == comp.rawMaterialId }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• ${rm?.name ?: comp.rawMaterialId}", fontSize = 11.sp, color = MasInk)
                            Text("${formatQty(comp.qtyPerUnit)} ${rm?.unit ?: "Kg"}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialConsumptionForm(
    viewModel: MasViewModel,
    orders: List<ProductionOrder>,
    rawMaterials: List<RawMaterial>,
    onSaved: () -> Unit
) {
    var selectedOrderId by remember { mutableStateOf(orders.firstOrNull()?.id ?: "") }
    var selectedRmId by remember { mutableStateOf(rawMaterials.firstOrNull()?.id ?: "") }
    var qty by remember { mutableStateOf("") }

    val qtyVal = qty.toDoubleOrNull() ?: 0.0
    val currentRm = rawMaterials.find { it.id == selectedRmId }
    val canSubmit = selectedOrderId.isNotBlank() && selectedRmId.isNotBlank() && qtyVal > 0.0

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
                    Text("Issue Raw Material to WIP", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Text("Production Order", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    var ordExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { ordExpanded = true }, shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(selectedOrderId.ifEmpty { "Select Production Order" }, fontSize = 13.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = ordExpanded, onDismissRequest = { ordExpanded = false }) {
                            orders.forEach { ord ->
                                DropdownMenuItem(text = { Text("${ord.id} (Planned: ${formatQty(ord.plannedQty)})") }, onClick = {
                                    selectedOrderId = ord.id
                                    ordExpanded = false
                                })
                            }
                        }
                    }

                    Text("Raw Material", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    var rmExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { rmExpanded = true }, shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(currentRm?.name ?: "Select Material", fontSize = 13.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = rmExpanded, onDismissRequest = { rmExpanded = false }) {
                            rawMaterials.forEach { rm ->
                                DropdownMenuItem(text = { Text("${rm.name} (${formatMoney(rm.costPrice)}/${rm.unit})") }, onClick = {
                                    selectedRmId = rm.id
                                    rmExpanded = false
                                })
                            }
                        }
                    }

                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it },
                        label = { Text("Quantity Issued (${currentRm?.unit ?: "Kg"})") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (canSubmit) {
                                val mc = MaterialConsumption(
                                    id = "MC-${System.currentTimeMillis() % 10000}",
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                    orderId = selectedOrderId,
                                    rawMaterialId = selectedRmId,
                                    qty = qtyVal,
                                    unitCost = currentRm?.costPrice ?: 0.0,
                                    reference = "Issued to $selectedOrderId"
                                )
                                viewModel.recordMaterialConsumption(mc)
                                onSaved()
                            }
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Issue to Production WIP", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductionOutputForm(
    viewModel: MasViewModel,
    orders: List<ProductionOrder>,
    finishedGoods: List<FinishedGood>,
    onSaved: () -> Unit
) {
    var selectedOrderId by remember { mutableStateOf(orders.firstOrNull()?.id ?: "") }
    var qty by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("Standard Production Batch") }

    val qtyVal = qty.toDoubleOrNull() ?: 0.0
    val canSubmit = selectedOrderId.isNotBlank() && qtyVal > 0.0

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
                    Text("Record Finished Goods Output", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(value = selectedOrderId, onValueChange = { selectedOrderId = it }, label = { Text("Production Order ID") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Completed Units Qty") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Output Reference / Batch Notes") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            if (canSubmit) {
                                val output = ProductionOutput(
                                    id = "POUT-${System.currentTimeMillis() % 10000}",
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                    orderId = selectedOrderId,
                                    qty = qtyVal,
                                    reference = notes.trim()
                                )
                                viewModel.recordProductionOutput(output)
                                onSaved()
                            }
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = MasGreen),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Record Finished Goods", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ManufacturingCostingView(
    orders: List<ProductionOrder>,
    consumptions: List<MaterialConsumption>,
    outputs: List<ProductionOutput>,
    overheads: List<ManufacturingOverhead>,
    boms: List<BillOfMaterials>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(orders) { order ->
            val orderConsumptions = consumptions.filter { it.orderId == order.id }
            val orderOutputs = outputs.filter { it.orderId == order.id }
            val orderOverheads = overheads.filter { it.orderId == order.id }
            val bom = boms.find { it.id == order.bomId }

            val totalMaterialCost = orderConsumptions.sumOf { it.qty * it.unitCost }
            val producedUnits = orderOutputs.sumOf { it.qty }
            val totalLaborCost = producedUnits * (bom?.laborCostPerUnit ?: 200.0)
            val totalOverhead = orderOverheads.sumOf { it.amount }
            val totalBatchCost = totalMaterialCost + totalLaborCost + totalOverhead
            val unitCost = if (producedUnits > 0) totalBatchCost / producedUnits else (if (order.plannedQty > 0) totalBatchCost / order.plannedQty else 0.0)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(order.id, fontWeight = FontWeight.Bold, color = MasRed, fontFamily = FontFamily.Monospace)
                        PillBadge("WIP Costed", "green")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Planned: ${formatQty(order.plannedQty)} | Produced: ${formatQty(producedUnits)}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Materials: ${formatMoney(totalMaterialCost)} | Labor: ${formatMoney(totalLaborCost)} | OH: ${formatMoney(totalOverhead)}", color = MasMuted, fontSize = 11.sp)
                    Text("Unit Production Cost: ${formatMoney(unitCost)}", fontWeight = FontWeight.Bold, color = MasGreen, fontSize = 12.5.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
