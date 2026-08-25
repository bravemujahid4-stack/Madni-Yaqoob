package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MasViewModel

@Composable
fun DashboardScreen(
    viewModel: MasViewModel,
    onNavigateToModule: (String) -> Unit
) {
    val totalSales by viewModel.totalSales.collectAsState()
    val totalPurchases by viewModel.totalPurchases.collectAsState()
    val totalReceivable by viewModel.totalReceivable.collectAsState()
    val totalPayable by viewModel.totalPayable.collectAsState()
    val cashInHand by viewModel.cashInHand.collectAsState()
    val bankBalance by viewModel.bankBalance.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val netProfit = totalSales - totalPurchases - totalExpenses
    val totalStockValue = remember(totalSales, totalPurchases) { viewModel.getTotalStockValue() }

    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val auditLog by viewModel.auditLog.collectAsState()

    // Quick action dialog states
    var showQuickSaleDialog by remember { mutableStateOf(false) }
    var showQuickExpenseDialog by remember { mutableStateOf(false) }
    var showQuickReceiptDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
    ) {
        // Welcome Header & Brand Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MasInk),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MasLogoBadge(size = 28.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MAS Executive Dashboard",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Live Business & Accounting Overview",
                            color = MasRailMuted,
                            fontSize = 11.5.sp
                        )
                    }
                    Surface(
                        color = MasGreenSoft,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Fiscal 2026",
                            color = MasGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 9 KPI Metrics Cards (Adaptive Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "KEY FINANCIAL INDICATORS",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                // Row 1
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        label = "Total Sales",
                        value = formatMoney(totalSales),
                        icon = Icons.Default.TrendingUp,
                        tone = MasGreen,
                        sub = "Net of returns",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Total Purchases",
                        value = formatMoney(totalPurchases),
                        icon = Icons.Default.ShoppingBag,
                        tone = MasAmber,
                        sub = "Raw + Bills",
                        modifier = Modifier.weight(1f)
                    )
                }
                // Row 2
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        label = "Receivables",
                        value = formatMoney(totalReceivable),
                        icon = Icons.Default.AccountBalanceWallet,
                        tone = MasGreen,
                        sub = "Customer due",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Payables",
                        value = formatMoney(totalPayable),
                        icon = Icons.Default.Payments,
                        tone = MasRed,
                        sub = "Supplier owed",
                        modifier = Modifier.weight(1f)
                    )
                }
                // Row 3
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        label = "Cash in Hand",
                        value = formatMoney(cashInHand),
                        icon = Icons.Default.Money,
                        tone = MasInkLight,
                        sub = "Drawers & petty",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Bank Balance",
                        value = formatMoney(bankBalance),
                        icon = Icons.Default.AccountBalance,
                        tone = MasBlue,
                        sub = "Operating banks",
                        modifier = Modifier.weight(1f)
                    )
                }
                // Row 4
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        label = "Net Profit",
                        value = formatMoney(netProfit),
                        icon = Icons.Default.MonetizationOn,
                        tone = if (netProfit >= 0) MasGreen else MasRed,
                        sub = "Sales - Buy - Exp",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Stock Value",
                        value = formatMoney(totalStockValue),
                        icon = Icons.Default.Inventory2,
                        tone = MasInkLight,
                        sub = "At cost price",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Actions Grid
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "QUICK ACTIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickActionItem("New Sale", Icons.Default.PointOfSale, MasRed) { onNavigateToModule("step6") }
                        QuickActionItem("Purchase", Icons.Default.AddShoppingCart, MasAmber) { onNavigateToModule("step8") }
                        QuickActionItem("Parties/Excel", Icons.Default.Groups, Color(0xFF673AB7)) { onNavigateToModule("parties") }
                        QuickActionItem("Expense", Icons.Default.CreditCard, MasBlue) { onNavigateToModule("step10") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickActionItem("Journal JE", Icons.Default.PostAdd, MasInkLight) { onNavigateToModule("step4") }
                        QuickActionItem("Customer", Icons.Default.PersonAdd, MasGreen) { onNavigateToModule("step5") }
                        QuickActionItem("Supplier", Icons.Default.LocalShipping, MasAmber) { onNavigateToModule("step7") }
                        QuickActionItem("Reports", Icons.Default.Assessment, MasRed) { onNavigateToModule("step17") }
                    }
                }
            }
        }

        // Monthly Sales & Expenses Bar Charts
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monthly Sales Trend",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        PillBadge("Sales", "green")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    val salesData = listOf(
                        ChartBarData("Jan", 42000.0, MasGreen),
                        ChartBarData("Feb", 31000.0, MasGreen),
                        ChartBarData("Mar", 40000.0, MasGreen),
                        ChartBarData("Apr", 28000.0, MasGreen),
                        ChartBarData("May", 55000.0, MasGreen),
                        ChartBarData("Jun", 48000.0, MasGreen),
                        ChartBarData("Jul", 62000.0, MasGreen),
                        ChartBarData("Aug", 75000.0, MasRed)
                    )
                    MasBarChart(data = salesData)
                }
            }
        }

        // Revenue vs Expenses + Profit Trend
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Profit & Cash Flow Trend",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val profitPoints = listOf(
                        Pair("Jan", 12000.0),
                        Pair("Feb", 8000.0),
                        Pair("Mar", 15000.0),
                        Pair("Apr", 9500.0),
                        Pair("May", 22000.0),
                        Pair("Jun", 18500.0),
                        Pair("Jul", 26000.0),
                        Pair("Aug", 34000.0)
                    )
                    MasLineTrendChart(points = profitPoints, lineColor = MasGreen)
                }
            }
        }

        // Receivables vs Payables Breakdown Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Receivable vs Payable",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MasGreen))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Receivable: ${formatMoney(totalReceivable)}", fontSize = 11.5.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MasRed))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Payable: ${formatMoney(totalPayable)}", fontSize = 11.5.sp)
                        }
                    }
                    MasDonutChart(
                        values = listOf(Pair("AR", totalReceivable), Pair("AP", totalPayable)),
                        colors = listOf(MasGreen, MasRed)
                    )
                }
            }
        }

        // Money Receivable Customers Top List
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top Receivables (Customers)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                        TextButton(onClick = { onNavigateToModule("step5") }) {
                            Text("View All", color = MasRed, fontSize = 11.sp)
                        }
                    }
                    customers.take(4).forEach { customer ->
                        val bal = viewModel.getCustomerBalance(customer.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(customer.name, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                Text("${customer.id} · ${customer.paymentTerms}", color = MasMuted, fontSize = 10.5.sp)
                            }
                            Text(
                                formatMoney(bal),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (bal > customer.creditLimit) MasRed else MasInk,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // Recent Activity Feed
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Recent Activity Log",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    auditLog.take(5).forEach { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MasRedLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = log.module.take(1),
                                    color = MasRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.details, fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${log.module} · ${log.user} · ${log.time}", color = MasMuted, fontSize = 10.sp)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionItem(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
