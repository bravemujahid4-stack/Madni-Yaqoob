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

    // Default Chart of Accounts matching Step 3
    private val defaultAccounts = listOf(
        Account("1", "1010", "Cash", AccountType.Assets, "Cash", opening = 50000.0, system = true),
        Account("2", "1020", "Bank", AccountType.Assets, "Bank", opening = 250000.0, system = true),
        Account("3", "1030", "Accounts Receivable", AccountType.Assets, "Accounts Receivable", opening = 120000.0, system = true),
        Account("4", "1040", "Inventory", AccountType.Assets, "Inventory", opening = 300000.0, system = true),
        Account("5", "1050", "Fixed Assets", AccountType.Assets, "Fixed Assets", opening = 800000.0, system = true),
        Account("6", "1055", "Accumulated Depreciation", AccountType.Assets, "Accumulated Depreciation", opening = 0.0, nature = "Credit", system = true),
        Account("7", "2010", "Accounts Payable", AccountType.Liabilities, "Accounts Payable", opening = 80000.0, system = true),
        Account("8", "2020", "Loans", AccountType.Liabilities, "Loans", opening = 300000.0, system = true),
        Account("9", "2030", "Accrued Expenses", AccountType.Liabilities, "Accrued Expenses", opening = 20000.0, system = true),
        Account("10", "2035", "Expenses Payable", AccountType.Liabilities, "Expenses Payable", opening = 0.0, system = true),
        Account("11", "3010", "Owner Capital", AccountType.Equity, "Owner Capital", opening = 1120000.0, system = true),
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

    private val defaultJournal = listOf(
        JournalEntry("JE-1001", "2026-08-05", "Sales", "Credit sale — Invoice INV-000101", "INV-000101", lines = listOf(JournalLine("Accounts Receivable", 45000.0, 0.0), JournalLine("Sales Revenue", 0.0, 45000.0))),
        JournalEntry("JE-1002", "2026-08-06", "Sales", "Cash sale — Invoice INV-000102", "INV-000102", lines = listOf(JournalLine("Cash", 12000.0, 0.0), JournalLine("Sales Revenue", 0.0, 12000.0))),
        JournalEntry("JE-1003", "2026-08-07", "Purchases", "Credit purchase — Bill BILL-000021", "BILL-000021", lines = listOf(JournalLine("Inventory", 30000.0, 0.0), JournalLine("Accounts Payable", 0.0, 30000.0))),
        JournalEntry("JE-1004", "2026-08-09", "Receipts", "Customer payment received — INV-000101", "INV-000101", lines = listOf(JournalLine("Bank", 45000.0, 0.0), JournalLine("Accounts Receivable", 0.0, 45000.0))),
        JournalEntry("JE-1005", "2026-08-10", "Payments", "Supplier payment — BILL-000021", "BILL-000021", lines = listOf(JournalLine("Accounts Payable", 30000.0, 0.0), JournalLine("Bank", 0.0, 30000.0))),
        JournalEntry("JE-1006", "2026-08-11", "Expenses", "Office rent — August", lines = listOf(JournalLine("Rent", 25000.0, 0.0), JournalLine("Bank", 0.0, 25000.0))),
        JournalEntry("JE-1007", "2026-08-12", "Inventory", "Stock write-off — damaged goods", lines = listOf(JournalLine("Cost of Goods Sold", 5000.0, 0.0), JournalLine("Inventory", 0.0, 5000.0))),
        JournalEntry("JE-1008", "2026-08-14", "Manual", "Owner capital injection", status = "Pending Approval", lines = listOf(JournalLine("Bank", 200000.0, 0.0), JournalLine("Owner Capital", 0.0, 200000.0)))
    )

    private val defaultCustomers = listOf(
        Customer("CUST-001", "Al-Falah Steel Traders", "Wholesale", "+92-321-4456781", "accounts@alfalahsteel.pk", "Plot 14, Site Area, Karachi", 35000.0, 300000.0, "Net 30"),
        Customer("CUST-002", "Karachi Metal Recyclers", "Scrap Dealer", "+92-300-2219087", "kmr.scrap@gmail.com", "Sher Shah Scrap Market, Karachi", 0.0, 500000.0, "Net 15"),
        Customer("CUST-003", "Sundar Scrap Point", "Retail", "+92-333-8890021", "sundarscrap@yahoo.com", "Sundar Industrial Estate, Lahore", 8000.0, 50000.0, "Due on Receipt"),
        Customer("CUST-004", "National Re-Rolling Mills", "Corporate", "+92-42-35678901", "procurement@nationalrerolling.com", "Industrial Zone 3, Lahore", 60000.0, 800000.0, "Net 45"),
        Customer("CUST-005", "Bilal & Sons", "Retail", "+92-345-1123456", "bilalandsons@outlook.com", "Shershah Road, Karachi", 0.0, 40000.0, "Net 15")
    )

    private val defaultSuppliers = listOf(
        Supplier("SUPP-001", "Haji Scrap Suppliers", "Scrap Vendor", "+92-321-7789012", "haji.scrap@gmail.com", "Sher Shah Scrap Market, Karachi", 22000.0, "Net 15"),
        Supplier("SUPP-002", "Chaudhry Transport Co.", "Transporter", "+92-300-4456712", "chaudhry.transport@yahoo.com", "Truck Stand, Super Highway, Karachi", 0.0, "Due on Receipt"),
        Supplier("SUPP-003", "Zafar Weighbridge & Equipment", "Equipment Supplier", "+92-332-9981234", "zafar.equip@outlook.com", "Site Area, Karachi", 40000.0, "Net 30"),
        Supplier("SUPP-004", "Al-Rehman Scrap Yard", "Scrap Vendor", "+92-345-6612390", "alrehmanscrap@gmail.com", "Sundar Industrial Estate, Lahore", 15000.0, "Net 15"),
        Supplier("SUPP-005", "Malik Cutting Services", "Service Provider", "+92-311-2245678", "malikcutting@gmail.com", "Gadap Road, Karachi", 0.0, "Net 30")
    )

    private val defaultSalesDocs = listOf(
        SalesDoc("QTN-0001", "Quotation", customerId = "CUST-004", date = "2026-08-10", items = listOf(LineItem(description = "Mixed steel scrap — bulk lot", qty = 1.0, rate = 85000.0)), status = "Sent"),
        SalesDoc("SO-0001", "Sales Order", customerId = "CUST-001", date = "2026-08-11", items = listOf(LineItem(description = "Copper scrap — 500kg", qty = 1.0, rate = 62000.0)), status = "Confirmed"),
        SalesDoc("INV-000201", "Sales Invoice", "Credit", "CUST-002", "2026-08-12", items = listOf(LineItem(description = "Aluminium scrap — grade A", qty = 1.0, rate = 54000.0)), status = "Posted",
            postings = listOf(JournalLine("Accounts Receivable", 54000.0, 0.0), JournalLine("Sales Revenue", 0.0, 54000.0))),
        SalesDoc("INV-000202", "Sales Invoice", "Cash", "WALKIN", "2026-08-13", items = listOf(LineItem(description = "Scrap sale — over the counter", qty = 1.0, rate = 9000.0)), status = "Posted",
            postings = listOf(JournalLine("Cash", 9000.0, 0.0), JournalLine("Sales Revenue", 0.0, 9000.0))),
        SalesDoc("SR-0001", "Sales Return", customerId = "CUST-002", date = "2026-08-14", reference = "INV-000201", items = listOf(LineItem(description = "Aluminium scrap — rejected, below grade", qty = 1.0, rate = 6000.0)), status = "Recorded"),
        SalesDoc("RCPT-0001", "Customer Payment", customerId = "CUST-001", date = "2026-08-09", reference = "INV-000101", method = "Bank Transfer", items = listOf(LineItem(description = "Payment received — Bank Transfer", qty = 1.0, rate = 45000.0)), status = "Received",
            postings = listOf(JournalLine("Bank", 45000.0, 0.0), JournalLine("Accounts Receivable", 0.0, 45000.0)))
    )

    private val defaultPurchaseDocs = listOf(
        PurchaseDoc("PO-0001", "Purchase Order", supplierId = "SUPP-003", date = "2026-08-10", items = listOf(LineItem(description = "Digital weighbridge platform — 40 ton", qty = 1.0, rate = 210000.0)), status = "Confirmed"),
        PurchaseDoc("BILL-1001", "Purchase Bill", "Credit", "SUPP-001", "2026-08-12", items = listOf(LineItem(description = "Mixed steel scrap — bulk lot", qty = 1.0, rate = 58000.0)), status = "Posted",
            postings = listOf(JournalLine("Purchases", 58000.0, 0.0), JournalLine("Accounts Payable", 0.0, 58000.0))),
        PurchaseDoc("BILL-1002", "Purchase Bill", "Cash", "CASH-SUPP", "2026-08-13", items = listOf(LineItem(description = "Loose scrap bought over the counter", qty = 1.0, rate = 7500.0)), status = "Posted",
            postings = listOf(JournalLine("Purchases", 7500.0, 0.0), JournalLine("Cash", 0.0, 7500.0))),
        PurchaseDoc("PR-0001", "Purchase Return", supplierId = "SUPP-001", date = "2026-08-14", reference = "BILL-1001", items = listOf(LineItem(description = "Contaminated scrap — rejected on inspection", qty = 1.0, rate = 5000.0)), status = "Recorded"),
        PurchaseDoc("PMT-0001", "Supplier Payment", supplierId = "SUPP-003", date = "2026-08-09", reference = "BILL-1007", method = "Cheque", items = listOf(LineItem(description = "Payment made — Cheque", qty = 1.0, rate = 40000.0)), status = "Paid",
            postings = listOf(JournalLine("Accounts Payable", 40000.0, 0.0), JournalLine("Bank", 0.0, 40000.0)))
    )

    private val defaultCashBankAccounts = listOf(
        CashBankAccount("ACC-001", "Cash in Hand", "Cash", openingBalance = 15000.0),
        CashBankAccount("ACC-002", "Petty Cash", "Cash", openingBalance = 5000.0),
        CashBankAccount("ACC-003", "Main Bank", "Bank", "Meezan Bank", "PK00-MEZN-1234567890", openingBalance = 250000.0),
        CashBankAccount("ACC-004", "Branch Bank", "Bank", "HBL", "PK00-HABB-9988776655", openingBalance = 80000.0)
    )

    private val defaultStockItems = listOf(
        StockItem("ITM-0001", "IRN-HVY-01", "8901234500011", "Iron Scrap — Heavy", "Scrap Iron", "Kg", 110.0, 140.0, 110.0, 1000.0),
        StockItem("ITM-0002", "CPR-WIR-01", "8901234500028", "Copper Wire Scrap", "Scrap Copper", "Kg", 950.0, 1050.0, 950.0, 100.0),
        StockItem("ITM-0003", "ALU-SHT-01", "8901234500035", "Aluminum Sheet Scrap", "Scrap Aluminum", "Kg", 320.0, 370.0, 320.0, 200.0),
        StockItem("ITM-0004", "PLS-BTL-01", "8901234500042", "Plastic Bottle Scrap", "Plastic Scrap", "Bag", 450.0, 550.0, 450.0, 50.0)
    )

    private val defaultStockMoves = listOf(
        StockMove("MOV-0001", "2026-07-01", "ITM-0001", "WH-01", "Opening", 1500.0, 110.0, "Opening Balance"),
        StockMove("MOV-0002", "2026-07-01", "ITM-0002", "WH-01", "Opening", 80.0, 950.0, "Opening Balance"),
        StockMove("MOV-0003", "2026-07-01", "ITM-0003", "WH-01", "Opening", 600.0, 320.0, "Opening Balance"),
        StockMove("MOV-0004", "2026-07-01", "ITM-0004", "WH-02", "Opening", 50.0, 450.0, "Opening Balance"),
        StockMove("MOV-0005", "2026-08-05", "ITM-0001", "WH-01", "In", 500.0, 115.0, "Purchase — Rafiq Traders"),
        StockMove("MOV-0006", "2026-08-10", "ITM-0001", "WH-01", "Out", 300.0, null, "Sale — Al-Noor Steel"),
        StockMove("MOV-0007", "2026-08-12", "ITM-0003", "WH-01", "Transfer Out", 200.0, null, "Branch stock balancing"),
        StockMove("MOV-0008", "2026-08-12", "ITM-0003", "WH-02", "Transfer In", 200.0, null, "Branch stock balancing"),
        StockMove("MOV-0009", "2026-08-14", "ITM-0004", "WH-02", "Adjustment -", 5.0, null, "Damaged in storage")
    )

    private val defaultExpenses = listOf(
        ExpenseVoucher("EXP-0001", "Rent", "Cash", "2026-08-10", "Shop rent — August", 25000.0, "Bank", postings = listOf(JournalLine("Rent", 25000.0, 0.0), JournalLine("Bank", 0.0, 25000.0))),
        ExpenseVoucher("EXP-0002", "Fuel", "Cash", "2026-08-11", "Fuel for delivery truck", 3500.0, "Cash", postings = listOf(JournalLine("Transportation", 3500.0, 0.0), JournalLine("Cash", 0.0, 3500.0))),
        ExpenseVoucher("EXP-0003", "Utilities", "Credit", "2026-08-12", "Electricity bill — July cycle", 14000.0, payee = "K-Electric", dueDate = "2026-08-27", postings = listOf(JournalLine("Utilities", 14000.0, 0.0), JournalLine("Expenses Payable", 0.0, 14000.0))),
        ExpenseVoucher("EXP-0004", "Salaries", "Cash", "2026-08-01", "Staff salaries — July", 60000.0, "Bank", status = "Pending Approval")
    )

    private val defaultFixedAssets = listOf(
        FixedAsset("FA-0001", "Dell Precision Workstation", "CAT-0001", "2024-02-01", 300000.0, 3, "SLM", 30000.0, "Head Office — IT"),
        FixedAsset("FA-0002", "Executive Office Desk Set", "CAT-0002", "2023-06-15", 420000.0, 7, "SLM", 21000.0, "Head Office — Admin"),
        FixedAsset("FA-0003", "CNC Milling Machine", "CAT-0003", "2022-01-10", 8500000.0, 10, "WDV", 850000.0, "Plant 1 — Floor A"),
        FixedAsset("FA-0004", "Delivery Van — Hino Truck", "CAT-0004", "2023-09-01", 3200000.0, 5, "SYD", 480000.0, "Logistics — Yard")
    )

    private val defaultFixedAssetDep = listOf(
        AssetDepreciation("DEP-0001", "FA-0001", "2024-12-31", 75000.0, "FY2024 depreciation"),
        AssetDepreciation("DEP-0002", "FA-0001", "2025-12-31", 90000.0, "FY2025 depreciation"),
        AssetDepreciation("DEP-0003", "FA-0002", "2023-12-31", 28500.0, "FY2023 depreciation"),
        AssetDepreciation("DEP-0004", "FA-0002", "2024-12-31", 57000.0, "FY2024 depreciation"),
        AssetDepreciation("DEP-0005", "FA-0003", "2022-12-31", 1748500.0, "FY2022 depreciation (WDV)"),
        AssetDepreciation("DEP-0006", "FA-0003", "2023-12-31", 1388800.0, "FY2023 depreciation (WDV)")
    )

    private val defaultRawMaterials = listOf(
        RawMaterial("RM-0001", "Steel Sheet", "Kg", 130.0, 2000.0),
        RawMaterial("RM-0002", "Copper Rod", "Kg", 980.0, 60.0),
        RawMaterial("RM-0003", "Plastic Granules", "Kg", 210.0, 500.0)
    )

    private val defaultFinishedGoods = listOf(
        FinishedGood("FG-0001", "Steel Bracket", "FG-BRK-01", "Pcs", 450.0),
        FinishedGood("FG-0002", "Copper Fitting", "FG-FIT-01", "Pcs", 1200.0)
    )

    private val defaultBoms = listOf(
        BillOfMaterials("BOM-0001", "FG-0001", "Standard Steel Bracket", 50.0, listOf(BOMComponent("RM-0001", 0.8))),
        BillOfMaterials("BOM-0002", "FG-0002", "Standard Copper Fitting", 80.0, listOf(BOMComponent("RM-0002", 0.05)))
    )

    private val defaultUsers = listOf(
        AppUser(1, "Mujahid Jatoi", "mujahid@mas.local", "Admin", "Active"),
        AppUser(2, "Sara Khan", "sara.khan@mas.local", "Accountant", "Active"),
        AppUser(3, "Bilal Ahmed", "bilal.ahmed@mas.local", "Manager", "Active"),
        AppUser(4, "Usman Tariq", "usman.tariq@mas.local", "Sales User", "Active"),
        AppUser(5, "Hina Malik", "hina.malik@mas.local", "Purchase User", "Active")
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
    val cashBankTxns = MutableStateFlow(listOf(
        CashBankTxn("RCT-0001", "Receipt", "ACC-003", date = "2026-08-09", description = "Payment received — Al-Falah Steel", contraAccount = "Accounts Receivable", amount = 45000.0, reference = "TRF-88213"),
        CashBankTxn("PMT-0001", "Payment", "ACC-001", date = "2026-08-11", description = "Fuel for delivery truck", contraAccount = "Transportation", amount = 3500.0),
        CashBankTxn("TRF-0001", "Transfer", fromAccountId = "ACC-003", toAccountId = "ACC-001", date = "2026-08-12", description = "Cash top-up for daily float", amount = 20000.0)
    ))
    val stockItems = MutableStateFlow(defaultStockItems)
    val stockMoves = MutableStateFlow(defaultStockMoves)
    val expenseVouchers = MutableStateFlow(defaultExpenses)
    val recurringExpenses = MutableStateFlow(listOf(
        RecurringExpense("REC-0001", "Rent", 25000.0, "Monthly", "Cash", "Bank", "2026-09-01"),
        RecurringExpense("REC-0002", "Salaries", 60000.0, "Monthly", "Cash", "Bank", "2026-08-31")
    ))
    val expenseCategories = MutableStateFlow(listOf("Rent", "Salaries", "Utilities", "Transportation", "Marketing", "Office Expenses", "Other Expenses"))
    val rawMaterials = MutableStateFlow(defaultRawMaterials)
    val finishedGoods = MutableStateFlow(defaultFinishedGoods)
    val boms = MutableStateFlow(defaultBoms)
    val productionOrders = MutableStateFlow(listOf(
        ProductionOrder("PO-0001", "2026-08-01", "FG-0001", "BOM-0001", 1000.0),
        ProductionOrder("PO-0002", "2026-08-10", "FG-0002", "BOM-0002", 500.0)
    ))
    val consumptions = MutableStateFlow(listOf(
        MaterialConsumption("MC-0001", "2026-08-02", "PO-0001", "RM-0001", 820.0, 130.0, "Cutting & shaping"),
        MaterialConsumption("MC-0002", "2026-08-11", "PO-0002", "RM-0002", 16.0, 980.0, "Casting")
    ))
    val productionOutputs = MutableStateFlow(listOf(
        ProductionOutput("PRO-0001", "2026-08-05", "PO-0001", 1000.0, "Batch complete"),
        ProductionOutput("PRO-0002", "2026-08-12", "PO-0002", 300.0, "Partial batch")
    ))
    val manufacturingOverheads = MutableStateFlow(listOf(
        ManufacturingOverhead("OH-0001", "2026-08-05", "PO-0001", "Machine depreciation & electricity", 8000.0),
        ManufacturingOverhead("OH-0002", "2026-08-12", "PO-0002", "Electricity & indirect labor", 3000.0)
    ))
    val fixedAssets = MutableStateFlow(defaultFixedAssets)
    val assetCategories = MutableStateFlow(listOf(
        AssetCategory("CAT-0001", "Computer Equipment", "SLM", 3, 10.0),
        AssetCategory("CAT-0002", "Furniture & Fixtures", "SLM", 7, 5.0),
        AssetCategory("CAT-0003", "Plant & Machinery", "WDV", 10, 10.0),
        AssetCategory("CAT-0004", "Vehicles", "SYD", 5, 15.0)
    ))
    val assetDepreciations = MutableStateFlow(defaultFixedAssetDep)
    val assetTransfers = MutableStateFlow(listOf(
        AssetTransfer("AT-0001", "FA-0002", "2024-03-01", "Warehouse — Storage", "Head Office — Admin", "Relocated after office refit")
    ))
    val assetDisposals = MutableStateFlow(emptyList<AssetDisposal>())
    val branches = MutableStateFlow(listOf(Branch("BR-0001", "Main Branch", "Karachi"), Branch("BR-0002", "Lahore Branch", "Lahore")))
    val departments = MutableStateFlow(listOf(Department("DP-0001", "Sales"), Department("DP-0002", "Finance"), Department("DP-0003", "Inventory"), Department("DP-0004", "Manufacturing")))
    val projects = MutableStateFlow(listOf(Project("PJ-0001", "Expansion Phase 1"), Project("PJ-0002", "Solar Installation")))
    val costCenters = MutableStateFlow(listOf(CostCenter("CC-0001", "Karachi Yard"), CostCenter("CC-0002", "Lahore Plant")))
    val warehouses = MutableStateFlow(listOf(Warehouse("WH-01", "Main Yard", "Karachi"), Warehouse("WH-02", "Hyderabad Branch", "Hyderabad")))
    val users = MutableStateFlow(defaultUsers)
    val auditLog = MutableStateFlow(listOf(
        AuditLogEntry("AUD-1", "2026-08-14", "10:22", "Sara Khan", "General Ledger", "Post", "INV-000104", "Credit sale posted"),
        AuditLogEntry("AUD-2", "2026-08-14", "09:05", "Mujahid Jatoi", "Period Closing", "Lock", "2026-07", "July 2026 period locked"),
        AuditLogEntry("AUD-3", "2026-08-13", "16:40", "Usman Tariq", "Sales", "Create", "INV-000202", "Created Cash Sale"),
        AuditLogEntry("AUD-4", "2026-08-12", "14:12", "Bilal Ahmed", "Inventory", "Move", "MOV-0008", "Stock transfer Karachi -> Hyderabad")
    ))
    val periodStatuses = MutableStateFlow(mapOf(
        "2026-01" to "Closed", "2026-02" to "Closed", "2026-03" to "Closed", "2026-04" to "Closed",
        "2026-05" to "Closed", "2026-06" to "Closed", "2026-07" to "Locked", "2026-08" to "Open"
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
