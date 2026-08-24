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
import com.example.data.FixedAsset
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MasViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedAssetsScreen(viewModel: MasViewModel) {
    var subTab by remember { mutableStateOf("register") } // register, new, depreciation, schedule
    val assets by viewModel.fixedAssets.collectAsState()
    val depreciations by viewModel.assetDepreciations.collectAsState()
    val totalAssetCost = assets.sumOf { it.cost }
    val totalAccumDep = depreciations.sumOf { it.amount }
    val netBookValue = totalAssetCost - totalAccumDep

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        SectionHeader(
            title = "Fixed Assets Management",
            subtitle = "Asset register, Net Book Value (NBV), multi-method depreciation runs & disposals.",
            actionButton = {
                Button(
                    onClick = { subTab = "new" },
                    colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Asset", fontSize = 12.sp)
                }
            }
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { FilterChip(selected = subTab == "register", onClick = { subTab = "register" }, label = { Text("Asset Register (${assets.size})") }) }
            item { FilterChip(selected = subTab == "new", onClick = { subTab = "new" }, label = { Text("+ Register Asset") }) }
            item { FilterChip(selected = subTab == "depreciation", onClick = { subTab = "depreciation" }, label = { Text("Run Depreciation") }) }
            item { FilterChip(selected = subTab == "schedule", onClick = { subTab = "schedule" }, label = { Text("Depreciation History") }) }
        }

        when (subTab) {
            "register" -> AssetRegisterListView(assets, depreciations, totalAssetCost, netBookValue)
            "new" -> NewAssetForm(viewModel) { subTab = "register" }
            "depreciation" -> RunDepreciationView(viewModel, assets)
            "schedule" -> DepreciationHistoryView(depreciations, assets)
        }
    }
}

@Composable
fun AssetRegisterListView(
    assets: List<FixedAsset>,
    depreciations: List<com.example.data.AssetDepreciation>,
    totalCost: Double,
    netBookValue: Double
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Gross Asset Cost", formatMoney(totalCost), modifier = Modifier.weight(1f))
                StatCard("Net Book Value", formatMoney(netBookValue), tone = MasGreen, modifier = Modifier.weight(1f))
            }
        }

        items(assets) { asset ->
            val assetDep = depreciations.filter { it.assetId == asset.id }.sumOf { it.amount }
            val nbv = asset.cost - assetDep

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
                            Text(asset.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Text("${asset.id} · ${asset.categoryId} · ${asset.method}", color = MasMuted, fontSize = 11.sp)
                        }
                        PillBadge(asset.status, if (asset.status == "Active") "green" else "red")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cost: ${formatMoney(asset.cost)} | Dep: ${formatMoney(assetDep)}", color = MasMuted, fontSize = 11.5.sp)
                        Text(
                            "NBV: ${formatMoney(nbv)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MasGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewAssetForm(viewModel: MasViewModel, onSaved: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Plant & Machinery") }
    var cost by remember { mutableStateOf("") }
    var salvage by remember { mutableStateOf("0") }
    var lifeYears by remember { mutableStateOf("10") }
    var method by remember { mutableStateOf("Straight Line") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

    val costVal = cost.toDoubleOrNull() ?: 0.0
    val canSave = name.isNotBlank() && costVal > 0.0

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
                    Text("Register Fixed Asset", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Asset Name *") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Asset Category") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = method, onValueChange = { method = it }, label = { Text("Method (SLM/WDV)") }, modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Purchase Cost *") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = salvage, onValueChange = { salvage = it }, label = { Text("Salvage Value") }, modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = lifeYears, onValueChange = { lifeYears = it }, label = { Text("Useful Life (Years)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Purchase Date") }, modifier = Modifier.weight(1f))
                    }

                    Button(
                        onClick = {
                            if (canSave) {
                                val asset = FixedAsset(
                                    id = "AST-${1000 + viewModel.fixedAssets.value.size + 1}",
                                    name = name.trim(),
                                    categoryId = category.trim(),
                                    cost = costVal,
                                    purchaseDate = date,
                                    usefulLifeYears = lifeYears.toIntOrNull() ?: 10,
                                    salvageValue = salvage.toDoubleOrNull() ?: 0.0,
                                    method = method.trim()
                                )
                                viewModel.addFixedAsset(asset)
                                onSaved()
                            }
                        },
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Register Asset & Post to GL", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RunDepreciationView(viewModel: MasViewModel, assets: List<FixedAsset>) {
    var resultText by remember { mutableStateOf<String?>(null) }

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
                    Text("Automated Monthly Depreciation Run", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "This will calculate the monthly depreciation across all ${assets.count { it.status == "Active" }} active assets according to their assigned method (SLM/WDV) and post the voucher into the General Ledger (Dr Depreciation Expense / Cr Accumulated Depreciation).",
                        fontSize = 12.sp,
                        color = MasMuted
                    )

                    Button(
                        onClick = {
                            val (count, total) = viewModel.runDepreciationBatch()
                            resultText = "Calculated & posted depreciation for $count asset(s) amounting to ${formatMoney(total)}."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MasGreen),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Execute Monthly Depreciation Run", fontWeight = FontWeight.Bold)
                    }

                    if (resultText != null) {
                        Surface(color = MasGreenSoft, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(resultText!!, color = MasGreen, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DepreciationHistoryView(
    depreciations: List<com.example.data.AssetDepreciation>,
    assets: List<FixedAsset>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(depreciations.reversed()) { d ->
            val asset = assets.find { it.id == d.assetId }
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
                    Column {
                        Text(asset?.name ?: d.assetId, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                        Text("${d.date} · ${d.id} · ${d.note}", color = MasMuted, fontSize = 10.5.sp)
                    }
                    Text(formatMoney(d.amount), fontWeight = FontWeight.Bold, color = MasRed, fontSize = 12.5.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
