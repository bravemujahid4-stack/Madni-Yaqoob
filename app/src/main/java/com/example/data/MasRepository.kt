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
        Account("1", "1010", "Cash in Hand — Munawar", AccountType.Assets, "Cash in Hand", opening = 0.0, system = true),
        Account("2", "1011", "Cash in Hand — Khalid", AccountType.Assets, "Cash in Hand", opening = 0.0, system = true),
        Account("3", "1020", "Bank", AccountType.Assets, "Bank", opening = 0.0, system = true),
        Account("4", "1030", "Accounts Receivable", AccountType.Assets, "Accounts Receivable", opening = 0.0, system = true),
        Account("5", "1040", "Inventory", AccountType.Assets, "Inventory", opening = 0.0, system = true),
        Account("6", "1050", "Fixed Assets", AccountType.Assets, "Fixed Assets", opening = 0.0, system = true),
        Account("7", "1055", "Accumulated Depreciation", AccountType.Assets, "Accumulated Depreciation", opening = 0.0, nature = "Credit", system = true),
        Account("8", "2010", "Accounts Payable", AccountType.Liabilities, "Accounts Payable", opening = 0.0, system = true),
        Account("9", "2020", "Loans", AccountType.Liabilities, "Loans", opening = 0.0, system = true),
        Account("10", "2030", "Accrued Expenses", AccountType.Liabilities, "Accrued Expenses", opening = 0.0, system = true),
        Account("11", "2035", "Expenses Payable", AccountType.Liabilities, "Expenses Payable", opening = 0.0, system = true),
        Account("12", "3010", "Owner Capital", AccountType.Equity, "Owner Capital", opening = 0.0, system = true),
        Account("13", "3020", "Retained Earnings", AccountType.Equity, "Retained Earnings", opening = 0.0, system = true),
        Account("14", "3030", "Drawings", AccountType.Equity, "Drawings", opening = 0.0, nature = "Debit", system = true),
        Account("15", "4010", "Sales Revenue", AccountType.Revenue, "Sales Revenue", opening = 0.0, system = true),
        Account("16", "4020", "Service Revenue", AccountType.Revenue, "Service Revenue", opening = 0.0, system = true),
        Account("17", "4090", "Other Income", AccountType.Revenue, "Other Income", opening = 0.0, system = true),
        Account("18", "5005", "Purchases", AccountType.Expenses, "Purchases", opening = 0.0, system = true),
        Account("19", "5010", "Cost of Goods Sold", AccountType.Expenses, "Cost of Goods Sold", opening = 0.0, system = true),
        Account("20", "5015", "Depreciation Expense", AccountType.Expenses, "Depreciation Expense", opening = 0.0, system = true),
        Account("21", "5020", "Salaries", AccountType.Expenses, "Salaries", opening = 0.0, system = true),
        Account("22", "5030", "Rent", AccountType.Expenses, "Rent", opening = 0.0, system = true),
        Account("23", "5040", "Utilities", AccountType.Expenses, "Utilities", opening = 0.0, system = true),
        Account("24", "5050", "Transportation", AccountType.Expenses, "Transportation", opening = 0.0, system = true),
        Account("25", "5060", "Marketing", AccountType.Expenses, "Marketing", opening = 0.0, system = true),
        Account("26", "5070", "Office Expenses", AccountType.Expenses, "Office Expenses", opening = 0.0, system = true),
        Account("27", "5090", "Other Expenses", AccountType.Expenses, "Other Expenses", opening = 0.0, system = true)
    )

    private val defaultJournal = emptyList<JournalEntry>()
    private val defaultCustomers = emptyList<Customer>()
    private val defaultSuppliers = emptyList<Supplier>()
    private val defaultSalesDocs = emptyList<SalesDoc>()
    private val defaultPurchaseDocs = emptyList<PurchaseDoc>()

    private val defaultCashBankAccounts = listOf(
        CashBankAccount("CSH-MUN", "Cash in Hand — Munawar", "Cash", openingBalance = 0.0),
        CashBankAccount("CSH-KHL", "Cash in Hand — Khalid", "Cash", openingBalance = 0.0),
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
    val partyAccounts = MutableStateFlow(emptyList<PartyAccount>())

    fun getNextPartyCode(type: PartyAccountType): String {
        val existingCodes = partyAccounts.value.map { it.code.trim().uppercase() }.toSet()
        var maxNum = 0
        partyAccounts.value.filter { it.accountType == type }.forEach { acc ->
            val code = acc.code.trim()
            if (code.startsWith(type.codePrefix, ignoreCase = true)) {
                val numPart = code.removePrefix(type.codePrefix).trim('-', '_', ' ')
                numPart.toIntOrNull()?.let { num ->
                    if (num > maxNum) maxNum = num
                }
            }
        }
        var nextNum = maxNum + 1
        var candidate = String.format("%s-%03d", type.codePrefix, nextNum)
        while (existingCodes.contains(candidate.uppercase())) {
            nextNum++
            candidate = String.format("%s-%03d", type.codePrefix, nextNum)
        }
        return candidate
    }

    fun savePartyAccount(account: PartyAccount, updateExisting: Boolean = false): Boolean {
        val currentList = partyAccounts.value
        val existingIndex = currentList.indexOfFirst {
            it.id == account.id || it.code.equals(account.code, ignoreCase = true) || (it.name.equals(account.name, ignoreCase = true) && it.accountType == account.accountType)
        }

        if (existingIndex >= 0) {
            if (updateExisting) {
                val updated = currentList.toMutableList()
                updated[existingIndex] = account
                partyAccounts.value = updated
                syncPartyToSubsystems(account)
                logAudit("Parties", "Update", account.code, "Updated party ${account.name} (${account.accountType.displayName})")
                return true
            } else {
                return false
            }
        } else {
            partyAccounts.value = partyAccounts.value + account
            syncPartyToSubsystems(account)
            logAudit("Parties", "Create", account.code, "Created party ${account.name} (${account.accountType.displayName})")
            return true
        }
    }

    fun deletePartyAccount(id: String) {
        val acc = partyAccounts.value.find { it.id == id }
        if (acc != null) {
            partyAccounts.value = partyAccounts.value.filter { it.id != id }
            if (acc.accountType == PartyAccountType.Customer) {
                customers.value = customers.value.filterNot { it.id == acc.code || it.name.equals(acc.name, ignoreCase = true) }
            }
            if (acc.accountType == PartyAccountType.CashInHand) {
                cashBankAccounts.value = cashBankAccounts.value.filterNot { it.id == acc.code || it.name.equals(acc.name, ignoreCase = true) }
            }
            if (acc.accountType == PartyAccountType.Factory) {
                warehouses.value = warehouses.value.filterNot { it.id == acc.code || it.name.equals(acc.name, ignoreCase = true) }
            }
            accounts.value = accounts.value.filterNot { it.code == acc.code }
            logAudit("Parties", "Delete", acc.code, "Deleted party ${acc.name}")
        }
    }

    fun importPartyAccounts(rows: List<ImportedAccountRow>, duplicateStrategy: DuplicateStrategy): Pair<Int, Int> {
        var importedCount = 0
        var updatedCount = 0
        val currentList = partyAccounts.value.toMutableList()

        rows.forEach { row ->
            val type = row.resolvedType ?: return@forEach
            val code = if (row.assignedCode.isNotBlank()) row.assignedCode else getNextPartyCode(type)
            val name = row.name.trim()
            if (name.isBlank()) return@forEach

            val existingIndex = currentList.indexOfFirst { existing ->
                existing.code.equals(code, ignoreCase = true) ||
                (existing.name.equals(name, ignoreCase = true) && existing.accountType == type) ||
                (row.phone.isNotBlank() && existing.phone.isNotBlank() && existing.phone == row.phone && existing.accountType == type)
            }

            val party = PartyAccount(
                id = if (existingIndex >= 0) currentList[existingIndex].id else "PTY-${System.currentTimeMillis() % 1000000}-$importedCount",
                code = code,
                name = name,
                accountType = type,
                openingBalance = row.openingBalance,
                balanceType = row.balanceType,
                phone = row.phone,
                address = row.address,
                notes = row.notes
            )

            if (existingIndex >= 0) {
                if (duplicateStrategy == DuplicateStrategy.UpdateExisting) {
                    currentList[existingIndex] = party
                    syncPartyToSubsystems(party)
                    updatedCount++
                }
            } else {
                currentList.add(party)
                syncPartyToSubsystems(party)
                importedCount++
            }
        }

        partyAccounts.value = currentList
        logAudit("Parties", "Import", "EXCEL/CSV", "Imported $importedCount new, updated $updatedCount parties")
        return Pair(importedCount, updatedCount)
    }

    private fun syncPartyToSubsystems(party: PartyAccount) {
        // 1. If Customer, sync to customers list
        if (party.accountType == PartyAccountType.Customer) {
            val signedOpening = if (party.balanceType == "Debit") party.openingBalance else -party.openingBalance
            val existingCust = customers.value.find { it.id == party.code || it.name.equals(party.name, ignoreCase = true) }
            if (existingCust != null) {
                customers.value = customers.value.map {
                    if (it.id == existingCust.id) it.copy(
                        name = party.name,
                        phone = party.phone,
                        address = party.address,
                        openingBalance = signedOpening
                    ) else it
                }
            } else {
                customers.value = customers.value + Customer(
                    id = party.code,
                    name = party.name,
                    phone = party.phone,
                    address = party.address,
                    openingBalance = signedOpening
                )
            }
        }

        // 2. If Cash In Hand, sync to cashBankAccounts
        if (party.accountType == PartyAccountType.CashInHand) {
            val signedOpening = if (party.balanceType.equals("Credit", ignoreCase = true) || party.balanceType.equals("Cr", ignoreCase = true) || party.balanceType.contains("Give", ignoreCase = true)) {
                -party.openingBalance
            } else {
                party.openingBalance
            }
            val existingCash = cashBankAccounts.value.find { it.id == party.code || it.name.equals(party.name, ignoreCase = true) }
            if (existingCash != null) {
                cashBankAccounts.value = cashBankAccounts.value.map {
                    if (it.id == existingCash.id) it.copy(
                        name = party.name,
                        openingBalance = signedOpening
                    ) else it
                }
            } else {
                cashBankAccounts.value = cashBankAccounts.value + CashBankAccount(
                    id = party.code,
                    name = party.name,
                    kind = "Cash",
                    openingBalance = signedOpening
                )
            }
        }

        // 4. If Factory, sync to Warehouses / Locations for inventory tracking
        if (party.accountType == PartyAccountType.Factory) {
            val existingWh = warehouses.value.find { it.id == party.code || it.name.equals(party.name, ignoreCase = true) }
            if (existingWh != null) {
                warehouses.value = warehouses.value.map {
                    if (it.id == existingWh.id) it.copy(
                        name = party.name,
                        location = "Factory: ${party.address.ifBlank { "Factory Location" }}"
                    ) else it
                }
            } else {
                warehouses.value = warehouses.value + Warehouse(
                    id = party.code,
                    name = party.name,
                    location = "Factory: ${party.address.ifBlank { "Factory Location" }}"
                )
            }
        }

        // 5. Sync to General Ledger Chart of Accounts
        val glAccName = "${party.name} (${party.accountType.codePrefix})"
        val existingGl = accounts.value.find { it.code == party.code || it.name.equals(glAccName, ignoreCase = true) }
        if (existingGl != null) {
            accounts.value = accounts.value.map {
                if (it.id == existingGl.id) it.copy(
                    code = party.code,
                    name = glAccName,
                    type = party.accountType.defaultGlType,
                    category = party.accountType.defaultCategory,
                    opening = party.openingBalance,
                    nature = party.balanceType
                ) else it
            }
        } else {
            accounts.value = accounts.value + Account(
                id = "GL-${party.code}",
                code = party.code,
                name = glAccName,
                type = party.accountType.defaultGlType,
                category = party.accountType.defaultCategory,
                opening = party.openingBalance,
                nature = party.balanceType,
                system = false
            )
        }
    }

    // Helper: double-entry posting engine
    fun postJournalEntry(entry: JournalEntry): Pair<Boolean, String> {
        val lines = entry.lines
        if (lines.size < 2) return Pair(false, "A double-entry journal requires at least two lines.")
        val totalDebit = Math.round(lines.sumOf { it.debit } * 100.0) / 100.0
        val totalCredit = Math.round(lines.sumOf { it.credit } * 100.0) / 100.0
        if (totalDebit <= 0.0) {
            return Pair(false, "Transaction amount must be greater than zero.")
        }
        if (Math.abs(totalDebit - totalCredit) > 0.005) {
            return Pair(false, "Unbalanced double-entry: Total Debit ($totalDebit) ≠ Total Credit ($totalCredit).")
        }
        journal.value = journal.value + entry
        logAudit(entry.source, "Post", entry.id, "${entry.description} (Dr $totalDebit = Cr $totalCredit)")
        return Pair(true, "Journal entry ${entry.id} posted successfully.")
    }

    fun voidJournalEntry(id: String): Boolean {
        val entry = journal.value.find { it.id == id } ?: return false
        journal.value = journal.value.map {
            if (it.id == id) it.copy(status = "Voided") else it
        }
        logAudit("General Ledger", "Void", id, "Voided journal entry ${entry.description}")
        return true
    }

    fun deleteJournalEntry(id: String): Boolean {
        val entry = journal.value.find { it.id == id } ?: return false
        journal.value = journal.value.filter { it.id != id }
        logAudit("General Ledger", "Delete", id, "Deleted journal entry ${entry.description}")
        return true
    }

    // Calculate strict double-entry ledger balance for any account
    fun getAccountLedgerBalance(account: Account): AccountLedgerBalance {
        val activeEntries = journal.value.filter { it.status == "Posted" }
        var totalDr = 0.0
        var totalCr = 0.0

        activeEntries.forEach { entry ->
            entry.lines.forEach { line ->
                if (matchesAccount(line.account, account)) {
                    totalDr += line.debit
                    totalCr += line.credit
                }
            }
        }

        val opening = account.opening
        val isNormalDebit = account.nature.equals("Debit", ignoreCase = true)

        val netAmount = if (isNormalDebit) {
            opening + totalDr - totalCr
        } else {
            opening + totalCr - totalDr
        }

        val indicator = if (isNormalDebit) {
            if (netAmount >= 0) "Dr" else "Cr"
        } else {
            if (netAmount >= 0) "Cr" else "Dr"
        }

        return AccountLedgerBalance(
            accountCode = account.code,
            accountName = account.name,
            accountType = account.type.name,
            normalNature = account.nature,
            openingBalance = opening,
            totalDebit = totalDr,
            totalCredit = totalCr,
            currentBalance = Math.abs(netAmount),
            drCrIndicator = indicator
        )
    }

    private fun matchesAccount(lineAccount: String, account: Account): Boolean {
        if (lineAccount.equals(account.name, ignoreCase = true)) return true
        if (lineAccount.equals(account.code, ignoreCase = true)) return true
        if (lineAccount.startsWith("${account.name} (", ignoreCase = true)) return true
        if (lineAccount.contains(account.code, ignoreCase = true)) return true
        return false
    }

    // Double-Entry Integrity Check across all posted entries
    fun checkDoubleEntryIntegrity(): DoubleEntryIntegrityCheck {
        val postedEntries = journal.value.filter { it.status == "Posted" }
        var sumDebits = 0.0
        var sumCredits = 0.0
        val imbalanced = mutableListOf<JournalEntry>()

        postedEntries.forEach { entry ->
            val entryDr = entry.lines.sumOf { it.debit }
            val entryCr = entry.lines.sumOf { it.credit }
            sumDebits += entryDr
            sumCredits += entryCr
            if (Math.abs(entryDr - entryCr) > 0.005) {
                imbalanced.add(entry)
            }
        }

        val diff = Math.abs(sumDebits - sumCredits)
        return DoubleEntryIntegrityCheck(
            isBalanced = diff <= 0.01 && imbalanced.isEmpty(),
            totalDebits = sumDebits,
            totalCredits = sumCredits,
            difference = diff,
            imbalancedEntries = imbalanced
        )
    }

    // Stock Transfer between any two locations (Factory / Warehouse)
    fun recordStockTransfer(
        fromLocationId: String,
        toLocationId: String,
        itemId: String,
        qty: Double,
        date: String,
        reference: String
    ): Boolean {
        if (qty <= 0 || fromLocationId == toLocationId) return false
        val item = stockItems.value.find { it.id == itemId } ?: return false
        val unitCost = item.costPrice
        val transferId = "TRF-${System.currentTimeMillis() % 100000}"

        val moveOut = StockMove(
            id = "MV-OUT-$transferId",
            date = date,
            itemId = itemId,
            warehouseId = fromLocationId,
            type = "Transfer Out",
            qty = qty,
            unitCost = unitCost,
            reference = "Transfer to $toLocationId ($reference)"
        )

        val moveIn = StockMove(
            id = "MV-IN-$transferId",
            date = date,
            itemId = itemId,
            warehouseId = toLocationId,
            type = "Transfer In",
            qty = qty,
            unitCost = unitCost,
            reference = "Transfer from $fromLocationId ($reference)"
        )

        stockMoves.value = stockMoves.value + listOf(moveOut, moveIn)
        logAudit("Inventory", "Transfer", transferId, "Transferred $qty ${item.unit} of ${item.name} from $fromLocationId to $toLocationId")
        return true
    }

    // Factory Stock Synchronization
    fun getFactoryStockRecords(): List<FactoryStockRecord> {
        val factoryParties = partyAccounts.value.filter { it.accountType == PartyAccountType.Factory }
        val allItems = stockItems.value
        val moves = stockMoves.value

        return factoryParties.map { factory ->
            val factoryLocIds = setOf(factory.code, factory.name, factory.id)
            val itemStocks = allItems.mapNotNull { item ->
                val factoryMoves = moves.filter { it.itemId == item.id && it.warehouseId in factoryLocIds }
                val stockIn = factoryMoves.filter { it.type in listOf("In", "Opening", "Transfer In", "Adjustment +") }.sumOf { it.qty }
                val stockOut = factoryMoves.filter { it.type in listOf("Out", "Transfer Out", "Adjustment -") }.sumOf { it.qty }
                val currentQty = stockIn - stockOut

                if (currentQty != 0.0 || stockIn > 0.0 || stockOut > 0.0) {
                    FactoryItemStock(
                        itemId = item.id,
                        itemName = item.name,
                        sku = item.sku,
                        unit = item.unit,
                        costPrice = item.costPrice,
                        quantity = currentQty,
                        totalValue = currentQty * item.costPrice,
                        stockIn = stockIn,
                        stockOut = stockOut
                    )
                } else null
            }

            val totalQty = itemStocks.sumOf { it.quantity }
            val totalVal = itemStocks.sumOf { it.totalValue }

            FactoryStockRecord(
                factoryId = factory.id,
                factoryName = factory.name,
                factoryCode = factory.code,
                totalQuantity = totalQty,
                totalValue = totalVal,
                items = itemStocks
            )
        }
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
