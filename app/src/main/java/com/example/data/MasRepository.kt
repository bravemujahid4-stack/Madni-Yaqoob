package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// MAS Central Repository & Double-Entry State
// ============================================================================

object MasRepository {

    // Clean initial Chart of Accounts with zero opening balances
    private val defaultAccounts = listOf(
        Account("1", "1010", "Cash in Hand", AccountType.Assets, "Cash in Hand", opening = 0.0, system = true),
        Account("2", "1020", "Bank", AccountType.Assets, "Bank", opening = 0.0, system = true),
        Account("3", "1030", "Accounts Receivable", AccountType.Assets, "Accounts Receivable", opening = 0.0, system = true),
        Account("4", "1040", "Inventory", AccountType.Assets, "Inventory", opening = 0.0, system = true),
        Account("5", "1050", "Fixed Assets", AccountType.Assets, "Fixed Assets", opening = 0.0, system = true),
        Account("6", "1055", "Accumulated Depreciation", AccountType.Assets, "Accumulated Depreciation", opening = 0.0, nature = "Credit", system = true),
        Account("7", "2010", "Accounts Payable", AccountType.Liabilities, "Accounts Payable", opening = 0.0, system = true),
        Account("8", "2020", "Loans", AccountType.Liabilities, "Loans", opening = 0.0, system = true),
        Account("9", "2030", "Accrued Expenses", AccountType.Liabilities, "Accrued Expenses", opening = 0.0, system = true),
        Account("10", "2035", "Expenses Payable", AccountType.Liabilities, "Expenses Payable", opening = 0.0, system = true),
        Account("11", "3010", "Owner Capital", AccountType.Equity, "Owner Capital", opening = 0.0, system = true),
        Account("12", "3020", "Retained Earnings", AccountType.Equity, "Retained Earnings", opening = 0.0, system = true),
        Account("13", "3030", "Drawings", AccountType.Equity, "Drawings", opening = 0.0, nature = "Debit", system = true),
        Account("14", "4010", "Sales Revenue", AccountType.Revenue, "Sales Revenue", opening = 0.0, system = true),
        Account("15", "4020", "Service Revenue", AccountType.Revenue, "Service Revenue", opening = 0.0, system = true),
        Account("16", "4090", "Other Income", AccountType.Revenue, "Other Income", opening = 0.0, system = true),
        Account("17", "5005", "Purchases", AccountType.Expenses, "Purchases", opening = 0.0, system = true),
        Account("18", "5010", "Cost of Goods Sold", AccountType.Expenses, "Cost of Goods Sold", opening = 0.0, system = true),
        Account("19", "5015", "Depreciation Expense", AccountType.Expenses, "Depreciation Expense", opening = 0.0, system = true),
        Account("20", "5020", "Salaries", AccountType.Expenses, "Salaries", opening = 0.0, system = true),
        Account("21", "5030", "Rent", AccountType.Expenses, "Rent", opening = 0.0, system = true),
        Account("22", "5040", "Utilities", AccountType.Expenses, "Utilities", opening = 0.0, system = true),
        Account("23", "5050", "Transportation", AccountType.Expenses, "Transportation", opening = 0.0, system = true),
        Account("24", "5060", "Marketing", AccountType.Expenses, "Marketing", opening = 0.0, system = true),
        Account("25", "5070", "Office Expenses", AccountType.Expenses, "Office Expenses", opening = 0.0, system = true),
        Account("26", "5090", "Other Expenses", AccountType.Expenses, "Other Expenses", opening = 0.0, system = true)
    )

    private val defaultJournal = emptyList<JournalEntry>()
    private val defaultCustomers = emptyList<Customer>()
    private val defaultSuppliers = emptyList<Supplier>()
    private val defaultSalesDocs = emptyList<SalesDoc>()
    private val defaultPurchaseDocs = emptyList<PurchaseDoc>()

    private val defaultCashBankAccounts = listOf(
        CashBankAccount("ACC-001", "Cash in Hand", "Cash", openingBalance = 0.0),
        CashBankAccount("ACC-002", "Bank", "Bank", "Main Bank Account", "", openingBalance = 0.0)
    )

    private val defaultStockItems = emptyList<StockItem>()
    private val defaultStockMoves = emptyList<StockMove>()
    private val defaultExpenses = emptyList<ExpenseVoucher>()
    private val defaultRecurringExpenses = emptyList<RecurringExpense>()
    private val defaultRawMaterials = emptyList<RawMaterial>()
    private val defaultFinishedGoods = emptyList<FinishedGood>()
    private val defaultBoms = emptyList<BillOfMaterials>()
    private val defaultFixedAssets = emptyList<FixedAsset>()
    private val defaultFixedAssetDep = emptyList<AssetDepreciation>()

    private val defaultUsers = listOf(
        AppUser(1, "Admin User", "admin@mas.local", "Admin", "Active")
    )

    // State Flows
    val currentUser = MutableStateFlow(defaultUsers[0])
    val companyProfile = MutableStateFlow(CompanyProfile())
    val accounts = MutableStateFlow(defaultAccounts)
    val journal = MutableStateFlow(defaultJournal)
    val customers = MutableStateFlow(defaultCustomers)
    val suppliers = MutableStateFlow(defaultSuppliers)
    val salesDocs = MutableStateFlow(defaultSalesDocs)
    val purchaseDocs = MutableStateFlow(defaultPurchaseDocs)
    val cashBankAccounts = MutableStateFlow(defaultCashBankAccounts)
    val cashBankTxns = MutableStateFlow(emptyList<CashBankTxn>())
    val stockItems = MutableStateFlow(defaultStockItems)
    val stockMoves = MutableStateFlow(defaultStockMoves)
    val expenseVouchers = MutableStateFlow(defaultExpenses)
    val recurringExpenses = MutableStateFlow(defaultRecurringExpenses)
    val expenseCategories = MutableStateFlow(listOf("Rent", "Salaries", "Utilities", "Transportation", "Marketing", "Office Expenses", "Other Expenses"))
    val rawMaterials = MutableStateFlow(defaultRawMaterials)
    val finishedGoods = MutableStateFlow(defaultFinishedGoods)
    val boms = MutableStateFlow(defaultBoms)
    val productionOrders = MutableStateFlow(emptyList<ProductionOrder>())
    val consumptions = MutableStateFlow(emptyList<MaterialConsumption>())
    val productionOutputs = MutableStateFlow(emptyList<ProductionOutput>())
    val manufacturingOverheads = MutableStateFlow(emptyList<ManufacturingOverhead>())
    val fixedAssets = MutableStateFlow(defaultFixedAssets)
    val assetCategories = MutableStateFlow(listOf(
        AssetCategory("CAT-0001", "Computer Equipment", "SLM", 3, 10.0),
        AssetCategory("CAT-0002", "Furniture & Fixtures", "SLM", 7, 5.0),
        AssetCategory("CAT-0003", "Plant & Machinery", "WDV", 10, 10.0),
        AssetCategory("CAT-0004", "Vehicles", "SYD", 5, 15.0)
    ))
    val assetDepreciations = MutableStateFlow(defaultFixedAssetDep)
    val assetTransfers = MutableStateFlow(emptyList<AssetTransfer>())
    val assetDisposals = MutableStateFlow(emptyList<AssetDisposal>())
    val branches = MutableStateFlow(listOf(Branch("BR-0001", "Main Branch", "Karachi")))
    val departments = MutableStateFlow(listOf(Department("DP-0001", "Sales"), Department("DP-0002", "Finance"), Department("DP-0003", "Inventory"), Department("DP-0004", "Purchases")))
    val projects = MutableStateFlow(emptyList<Project>())
    val costCenters = MutableStateFlow(emptyList<CostCenter>())
    val warehouses = MutableStateFlow(listOf(Warehouse("WH-01", "Main Warehouse", "Karachi")))
    val users = MutableStateFlow(defaultUsers)
    val auditLog = MutableStateFlow(emptyList<AuditLogEntry>())
    val periodStatuses = MutableStateFlow(mapOf(
        "2026-01" to "Open", "2026-02" to "Open", "2026-03" to "Open", "2026-04" to "Open",
        "2026-05" to "Open", "2026-06" to "Open", "2026-07" to "Open", "2026-08" to "Open"
    ))
    val isFiscalYearClosed = MutableStateFlow(false)

    // Helper: double-entry posting engine
    fun postJournalEntry(entry: JournalEntry): Pair<Boolean, String> {
        val lines = entry.lines
        if (lines.size < 2) return Pair(false, "A journal entry needs at least two lines.")
        val totalDebit = Math.round(lines.sumOf { it.debit } * 100.0) / 100.0
        val totalCredit = Math.round(lines.sumOf { it.credit } * 100.0) / 100.0
        if (totalDebit <= 0.0 || Math.abs(totalDebit - totalCredit) > 0.004) {
            return Pair(false, "Entry is not balanced: Debit $totalDebit vs Credit $totalCredit.")
        }
        journal.value = journal.value + entry
        logAudit(entry.source, "Post", entry.id, entry.description)
        return Pair(true, "Posted successfully.")
    }

    fun logAudit(module: String, action: String, txnId: String, details: String) {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()
        val entry = AuditLogEntry(
            id = "AUD-${System.currentTimeMillis()}",
            date = sdfDate.format(now),
            time = sdfTime.format(now),
            user = currentUser.value.name,
            module = module,
            action = action,
            transactionId = txnId,
            details = details
        )
        auditLog.value = listOf(entry) + auditLog.value
    }
}
