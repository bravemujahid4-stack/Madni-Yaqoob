package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialReportsScreen(viewModel: MasViewModel) {
    var reportType by remember { mutableStateOf("pl") } // pl, bs, tb, cf, equity
    val accounts by viewModel.accounts.collectAsState()
    val journal by viewModel.journal.collectAsState()
    val totalSales by viewModel.totalSales.collectAsState()
    val totalPurchases by viewModel.totalPurchases.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val totalReceivable by viewModel.totalReceivable.collectAsState()
    val totalPayable by viewModel.totalPayable.collectAsState()
    val cashInHand by viewModel.cashInHand.collectAsState()
    val bankBalance by viewModel.bankBalance.collectAsState()
    val stockValuation = remember(totalSales, totalPurchases) { viewModel.getTotalStockValue() }
    val assets by viewModel.fixedAssets.collectAsState()
    val deps by viewModel.assetDepreciations.collectAsState()
    val netFixedAssets = assets.sumOf { it.cost } - deps.sumOf { it.amount }

    val netProfit = totalSales - totalPurchases - totalExpenses

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        SectionHeader(
            title = "Financial Statements",
            subtitle = "Standard accounting reports generated directly from posted journal entries."
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { FilterChip(selected = reportType == "pl", onClick = { reportType = "pl" }, label = { Text("Profit & Loss (P&L)") }) }
            item { FilterChip(selected = reportType == "bs", onClick = { reportType = "bs" }, label = { Text("Balance Sheet") }) }
            item { FilterChip(selected = reportType == "tb", onClick = { reportType = "tb" }, label = { Text("Trial Balance") }) }
            item { FilterChip(selected = reportType == "cf", onClick = { reportType = "cf" }, label = { Text("Cash Flow") }) }
            item { FilterChip(selected = reportType == "equity", onClick = { reportType = "equity" }, label = { Text("Changes in Equity") }) }
        }

        when (reportType) {
            "pl" -> ProfitAndLossStatement(totalSales, totalPurchases, totalExpenses, netProfit)
            "bs" -> BalanceSheetStatement(accounts, cashInHand, bankBalance, totalReceivable, stockValuation, netFixedAssets, totalPayable, netProfit)
            "tb" -> TrialBalanceStatement(accounts, journal)
            "cf" -> CashFlowStatement(totalSales, totalPurchases, totalExpenses, cashInHand + bankBalance)
            "equity" -> ChangesInEquityStatement(accounts, netProfit)
        }
    }
}

@Composable
fun ProfitAndLossStatement(sales: Double, costOfSales: Double, expenses: Double, netProfit: Double) {
    val grossProfit = sales - costOfSales

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
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Statement of Profit or Loss", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("For Period Ended 31 August 2026", color = MasMuted, fontSize = 11.sp)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    ReportRow("Revenue / Gross Sales", sales, isBold = true)
                    ReportRow("Less: Sales Returns & Discounts", 0.0, indent = true)
                    ReportRow("Net Revenue", sales, isBold = true, color = MasGreen)

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ReportRow("Cost of Goods Sold (COGS)", costOfSales, isBold = true)
                    ReportRow("Opening Raw Stock + Purchases", costOfSales, indent = true)

                    Surface(color = MasPaperSoft, shape = RoundedCornerShape(6.dp)) {
                        ReportRow("Gross Profit", grossProfit, isBold = true, color = MasGreen, modifier = Modifier.padding(8.dp))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Text("Operating Overheads & Expenses", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    ReportRow("Factory Power, Wages, Admin, Depr", expenses, indent = true)

                    Surface(color = if (netProfit >= 0) MasGreenSoft else MasRedLight, shape = RoundedCornerShape(6.dp)) {
                        ReportRow("Net Profit / (Loss) for the Period", netProfit, isBold = true, color = if (netProfit >= 0) MasGreen else MasRed, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceSheetStatement(
    accounts: List<Account>,
    cash: Double,
    bank: Double,
    receivables: Double,
    stock: Double,
    fixedAssets: Double,
    payables: Double,
    netProfit: Double
) {
    val currentAssets = cash + bank + receivables + stock
    val totalAssets = currentAssets + fixedAssets
    val capitalAccount = accounts.find { it.type == AccountType.Equity && (it.name.contains("Capital", ignoreCase = true) || it.name.contains("Owner", ignoreCase = true)) }
    val openingCapital = capitalAccount?.opening ?: 0.0
    val retainedAccount = accounts.find { it.type == AccountType.Equity && it.name.contains("Retained", ignoreCase = true) }
    val openingRetained = retainedAccount?.opening ?: 0.0
    val retainedEarnings = openingRetained + netProfit
    val totalEquity = openingCapital + retainedEarnings
    val totalLiabilitiesAndEquity = payables + totalEquity

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
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Statement of Financial Position (Balance Sheet)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("As of 31 August 2026", color = MasMuted, fontSize = 11.sp)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    Text("ASSETS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MasInk)
                    Text("Current Assets:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    ReportRow("Cash in Hand & Drawers", cash, indent = true)
                    ReportRow("Bank Operating Balances", bank, indent = true)
                    ReportRow("Accounts Receivable", receivables, indent = true)
                    ReportRow("Closing Inventory (Stock)", stock, indent = true)
                    ReportRow("Total Current Assets", currentAssets, isBold = true)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Non-Current Assets:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    ReportRow("Fixed Assets (Net of Depr)", fixedAssets, indent = true)

                    Surface(color = MasPaperSoft, shape = RoundedCornerShape(6.dp)) {
                        ReportRow("TOTAL ASSETS", totalAssets, isBold = true, color = MasInk, modifier = Modifier.padding(8.dp))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Text("LIABILITIES & EQUITY", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MasInk)
                    Text("Current Liabilities:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    ReportRow("Accounts Payable", payables, indent = true)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Equity:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    ReportRow("Paid-up Capital", openingCapital, indent = true)
                    ReportRow("Retained Earnings + Current Profit", retainedEarnings, indent = true)
                    ReportRow("Total Equity", totalEquity, isBold = true)

                    Surface(color = MasPaperSoft, shape = RoundedCornerShape(6.dp)) {
                        ReportRow("TOTAL LIABILITIES & EQUITY", totalLiabilitiesAndEquity, isBold = true, color = MasInk, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TrialBalanceStatement(accounts: List<Account>, journal: List<JournalEntry>) {
    val posted = journal.filter { it.status == "Posted" }
    var totalDr = 0.0
    var totalCr = 0.0

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Working Trial Balance", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Debits & Credits must strictly balance", color = MasMuted, fontSize = 11.sp)
                }
            }
        }

        items(accounts) { acc ->
            val lines = posted.flatMap { it.lines }.filter { it.account.equals(acc.name, ignoreCase = true) }
            val d = lines.sumOf { it.debit }
            val c = lines.sumOf { it.credit }
            val net = acc.opening + if (acc.nature == "Debit") d - c else c - d
            val isDebit = acc.nature == "Debit"

            if (isDebit) totalDr += net else totalCr += net

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${acc.code} ${acc.name}", fontSize = 12.sp, modifier = Modifier.weight(1f))
                    if (isDebit) {
                        Text(formatMoney(net), fontSize = 12.sp, color = MasGreen, fontFamily = FontFamily.Monospace)
                    } else {
                        Text(formatMoney(net), fontSize = 12.sp, color = MasRed, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun CashFlowStatement(sales: Double, purchases: Double, expenses: Double, cashClosing: Double) {
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
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Statement of Cash Flows (Direct Method)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    Text("Operating Activities:", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                    ReportRow("Cash Receipts from Sales", sales, indent = true)
                    ReportRow("Cash Paid for Purchases", -purchases, indent = true)
                    ReportRow("Cash Paid for Operating Expenses", -expenses, indent = true)

                    val netOp = sales - purchases - expenses
                    ReportRow("Net Cash from Operations", netOp, isBold = true, color = if (netOp >= 0) MasGreen else MasRed)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Investing & Financing Activities:", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                    ReportRow("Capital Infusion / Drawings", 0.0, indent = true)

                    Surface(color = MasPaperSoft, shape = RoundedCornerShape(6.dp)) {
                        ReportRow("Closing Cash & Bank Balance", cashClosing, isBold = true, color = MasInk, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChangesInEquityStatement(accounts: List<Account>, netProfit: Double) {
    val capitalAccount = accounts.find { it.type == AccountType.Equity && (it.name.contains("Capital", ignoreCase = true) || it.name.contains("Owner", ignoreCase = true)) }
    val openingCapital = capitalAccount?.opening ?: 0.0
    val retainedAccount = accounts.find { it.type == AccountType.Equity && it.name.contains("Retained", ignoreCase = true) }
    val openingRetained = retainedAccount?.opening ?: 0.0
    val drawingsAccount = accounts.find { it.type == AccountType.Equity && it.name.contains("Drawing", ignoreCase = true) }
    val drawings = drawingsAccount?.opening ?: 0.0
    val endingEquity = openingCapital + openingRetained + netProfit - drawings

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
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Statement of Changes in Equity", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    ReportRow("Share / Owner Capital (Opening)", openingCapital)
                    ReportRow("Retained Earnings (Opening)", openingRetained)
                    ReportRow("Net Profit for the Period", netProfit, color = if (netProfit >= 0) MasGreen else MasRed)
                    ReportRow("Dividends / Drawings", drawings)

                    Surface(color = MasPaperSoft, shape = RoundedCornerShape(6.dp)) {
                        ReportRow("Total Equity at End of Period", endingEquity, isBold = true, color = MasInk, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ReportRow(
    title: String,
    amount: Double,
    isBold: Boolean = false,
    color: Color = Color.Unspecified,
    indent: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = if (indent) 12.dp else 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isBold) 12.5.sp else 12.sp,
            color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = formatMoney(amount),
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isBold) 12.5.sp else 12.sp,
            fontFamily = FontFamily.Monospace,
            color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
        )
    }
}
