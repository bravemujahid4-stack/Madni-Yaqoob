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
    val openingStockRecords = MutableStateFlow(emptyList<OpeningStockRecord>())
    val deletedRecords = MutableStateFlow<List<DeletedRecord>>(emptyList())

    // Role check helper
    fun canModify(): Boolean {
        return currentUser.value.role.equals("Admin", ignoreCase = true)
    }

    fun isViewer(): Boolean {
        return currentUser.value.role.equals("Viewer", ignoreCase = true)
    }

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

    fun deletePartyAccount(id: String): Boolean {
        val acc = partyAccounts.value.find { it.id == id } ?: return false
        partyAccounts.value = partyAccounts.value.filter { it.id != id }
        if (acc.accountType == PartyAccountType.Customer) {
            customers.value = customers.value.filterNot { it.id == acc.code || it.name.equals(acc.name, ignoreCase = true) }
        }
        if (acc.accountType == PartyAccountType.Supplier) {
            suppliers.value = suppliers.value.filterNot { it.id == acc.code || it.name.equals(acc.name, ignoreCase = true) }
        }
        if (acc.accountType == PartyAccountType.CashInHand) {
            cashBankAccounts.value = cashBankAccounts.value.filterNot { it.id == acc.code || it.name.equals(acc.name, ignoreCase = true) }
        }
        if (acc.accountType == PartyAccountType.Factory) {
            warehouses.value = warehouses.value.filterNot { it.id == acc.code || it.name.equals(acc.name, ignoreCase = true) }
        }
        accounts.value = accounts.value.filterNot { it.code == acc.code }
        
        // Move to Deleted Items / Recycle Bin
        val deleted = DeletedRecord(
            id = "DEL-${System.currentTimeMillis()}-${(100..999).random()}",
            itemType = "Party Account",
            itemCode = acc.code,
            title = acc.name,
            subtitle = "${acc.accountType.displayName} · ${acc.phone.ifBlank { acc.address.ifBlank { "No contact" } }}",
            amount = acc.openingBalance,
            originalPayload = acc
        )
        deletedRecords.value = listOf(deleted) + deletedRecords.value
        logAudit("Parties", "Delete", acc.code, "Moved party ${acc.name} (${acc.code}) to Deleted Items folder")
        return true
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

            // If account has Opening Stock (e.g. Factory accounts or rows with opening qty/value)
            if (row.hasOpeningStock || (type == PartyAccountType.Factory && (row.openingQty > 0.0 || row.openingValue > 0.0))) {
                val opQty = if (row.openingQty > 0.0) row.openingQty else (if (row.openingValue > 0.0) 1.0 else 0.0)
                val opRate = if (row.openingRate > 0.0) row.openingRate else (if (opQty > 0.0 && row.openingValue > 0.0) row.openingValue / opQty else 250.0)
                val opVal = if (row.openingValue > 0.0) row.openingValue else (opQty * opRate)
                val opRec = OpeningStockRecord(
                    id = "OP-IMP-${party.code}-${System.currentTimeMillis() % 10000}",
                    itemId = "ITM-${party.code}",
                    itemName = "${party.name} Stock",
                    factoryId = party.code,
                    factoryName = party.name,
                    openingQty = opQty,
                    unit = row.unit.ifBlank { "Kg" },
                    openingRate = opRate,
                    openingValue = opVal,
                    openingDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    notes = "Opening stock imported from Excel"
                )
                saveOpeningStockRecord(opRec, updateExisting = true)
            }
        }

        partyAccounts.value = currentList
        MasStorageManager.saveToPersistentStorage()
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

        // 1b. If Supplier, sync to suppliers list
        if (party.accountType == PartyAccountType.Supplier) {
            val signedOpening = if (party.balanceType == "Credit") party.openingBalance else -party.openingBalance
            val existingSupp = suppliers.value.find { it.id == party.code || it.name.equals(party.name, ignoreCase = true) }
            if (existingSupp != null) {
                suppliers.value = suppliers.value.map {
                    if (it.id == existingSupp.id) it.copy(
                        name = party.name,
                        phone = party.phone,
                        address = party.address,
                        openingBalance = signedOpening
                    ) else it
                }
            } else {
                suppliers.value = suppliers.value + Supplier(
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
        
        val deleted = DeletedRecord(
            id = "DEL-${System.currentTimeMillis()}-${(100..999).random()}",
            itemType = "Journal Entry",
            itemCode = entry.id,
            title = entry.description,
            subtitle = "${entry.date} · ${entry.source} · ${entry.lines.size} Lines",
            amount = entry.lines.sumOf { it.debit },
            originalPayload = entry
        )
        deletedRecords.value = listOf(deleted) + deletedRecords.value
        logAudit("General Ledger", "Delete", id, "Moved journal entry ${entry.description} to Deleted Items folder")
        return true
    }

    fun deleteAccount(id: String): Boolean {
        val acc = accounts.value.find { it.id == id || it.code == id } ?: return false
        if (acc.system) return false // protect system accounts
        accounts.value = accounts.value.filterNot { it.id == acc.id || it.code == acc.code }

        val deleted = DeletedRecord(
            id = "DEL-${System.currentTimeMillis()}-${(100..999).random()}",
            itemType = "Chart of Account",
            itemCode = acc.code,
            title = acc.name,
            subtitle = "${acc.type.displayName} · ${acc.category}",
            amount = acc.opening,
            originalPayload = acc
        )
        deletedRecords.value = listOf(deleted) + deletedRecords.value
        logAudit("Chart of Accounts", "Delete", acc.code, "Moved account ${acc.name} (${acc.code}) to Deleted Items folder")
        return true
    }

    fun deleteSalesDoc(id: String): Boolean {
        val doc = salesDocs.value.find { it.id == id } ?: return false
        salesDocs.value = salesDocs.value.filterNot { it.id == id }
        val docTotal = doc.items.sumOf { it.qty * it.rate }
        val customerName = customers.value.find { it.id == doc.customerId }?.name ?: doc.customerId

        val deleted = DeletedRecord(
            id = "DEL-${System.currentTimeMillis()}-${(100..999).random()}",
            itemType = "Sales Invoice",
            itemCode = doc.id,
            title = "${doc.type}: $customerName",
            subtitle = "${doc.date} · ${doc.status} · Total: Rs $docTotal",
            amount = docTotal,
            originalPayload = doc
        )
        deletedRecords.value = listOf(deleted) + deletedRecords.value
        logAudit("Sales", "Delete", id, "Moved sales doc $id to Deleted Items folder")
        return true
    }

    fun deletePurchaseDoc(id: String): Boolean {
        val doc = purchaseDocs.value.find { it.id == id } ?: return false
        purchaseDocs.value = purchaseDocs.value.filterNot { it.id == id }
        val docTotal = doc.items.sumOf { it.qty * it.rate }
        val supplierName = suppliers.value.find { it.id == doc.supplierId }?.name ?: doc.supplierId

        val deleted = DeletedRecord(
            id = "DEL-${System.currentTimeMillis()}-${(100..999).random()}",
            itemType = "Purchase Bill",
            itemCode = doc.id,
            title = "${doc.type}: $supplierName",
            subtitle = "${doc.date} · ${doc.status} · Total: Rs $docTotal",
            amount = docTotal,
            originalPayload = doc
        )
        deletedRecords.value = listOf(deleted) + deletedRecords.value
        logAudit("Purchases", "Delete", id, "Moved purchase doc $id to Deleted Items folder")
        return true
    }

    fun deleteExpense(id: String): Boolean {
        val exp = expenseVouchers.value.find { it.id == id } ?: return false
        expenseVouchers.value = expenseVouchers.value.filterNot { it.id == id }

        val deleted = DeletedRecord(
            id = "DEL-${System.currentTimeMillis()}-${(100..999).random()}",
            itemType = "Expense Voucher",
            itemCode = exp.id,
            title = "${exp.category}: ${exp.description}",
            subtitle = "${exp.date} · Paid via ${exp.paidFrom} · Status: ${exp.status}",
            amount = exp.amount,
            originalPayload = exp
        )
        deletedRecords.value = listOf(deleted) + deletedRecords.value
        logAudit("Expenses", "Delete", id, "Moved expense $id to Deleted Items folder")
        return true
    }

    fun deleteCashBankTxn(id: String): Boolean {
        val txn = cashBankTxns.value.find { it.id == id } ?: return false
        cashBankTxns.value = cashBankTxns.value.filterNot { it.id == id }

        val deleted = DeletedRecord(
            id = "DEL-${System.currentTimeMillis()}-${(100..999).random()}",
            itemType = "Cash/Bank Txn",
            itemCode = txn.id,
            title = "${txn.type}: ${txn.description}",
            subtitle = "${txn.date} · Account: ${txn.accountId}",
            amount = txn.amount,
            originalPayload = txn
        )
        deletedRecords.value = listOf(deleted) + deletedRecords.value
        logAudit("Cash & Bank", "Delete", id, "Moved transaction $id to Deleted Items folder")
        return true
    }

    fun deleteCustomer(id: String): Boolean {
        val cust = customers.value.find { it.id == id } ?: return false
        customers.value = customers.value.filterNot { it.id == id }

        val deleted = DeletedRecord(
            id = "DEL-${System.currentTimeMillis()}-${(100..999).random()}",
            itemType = "Customer",
            itemCode = cust.id,
            title = cust.name,
            subtitle = "${cust.type} · ${cust.phone.ifBlank { cust.address }}",
            amount = cust.openingBalance,
            originalPayload = cust
        )
        deletedRecords.value = listOf(deleted) + deletedRecords.value
        logAudit("Customers", "Delete", id, "Moved customer ${cust.name} to Deleted Items folder")
        return true
    }

    fun deleteSupplier(id: String): Boolean {
        val supp = suppliers.value.find { it.id == id } ?: return false
        suppliers.value = suppliers.value.filterNot { it.id == id }

        val deleted = DeletedRecord(
            id = "DEL-${System.currentTimeMillis()}-${(100..999).random()}",
            itemType = "Supplier",
            itemCode = supp.id,
            title = supp.name,
            subtitle = "${supp.phone.ifBlank { supp.address }}",
            amount = supp.openingBalance,
            originalPayload = supp
        )
        deletedRecords.value = listOf(deleted) + deletedRecords.value
        logAudit("Suppliers", "Delete", id, "Moved supplier ${supp.name} to Deleted Items folder")
        return true
    }

    fun deleteFixedAsset(id: String): Boolean {
        val asset = fixedAssets.value.find { it.id == id } ?: return false
        fixedAssets.value = fixedAssets.value.filterNot { it.id == id }

        val deleted = DeletedRecord(
            id = "DEL-${System.currentTimeMillis()}-${(100..999).random()}",
            itemType = "Fixed Asset",
            itemCode = asset.id,
            title = asset.name,
            subtitle = "${asset.categoryId} · Purchased: ${asset.purchaseDate}",
            amount = asset.cost,
            originalPayload = asset
        )
        deletedRecords.value = listOf(deleted) + deletedRecords.value
        logAudit("Fixed Assets", "Delete", id, "Moved fixed asset ${asset.name} to Deleted Items folder")
        return true
    }

    fun deleteStockItem(id: String): Boolean {
        val item = stockItems.value.find { it.id == id } ?: return false
        stockItems.value = stockItems.value.filterNot { it.id == id }

        val deleted = DeletedRecord(
            id = "DEL-${System.currentTimeMillis()}-${(100..999).random()}",
            itemType = "Stock Item",
            itemCode = item.sku,
            title = item.name,
            subtitle = "${item.category} · Unit: ${item.unit}",
            amount = item.costPrice,
            originalPayload = item
        )
        deletedRecords.value = listOf(deleted) + deletedRecords.value
        logAudit("Inventory", "Delete", item.sku, "Moved stock item ${item.name} to Deleted Items folder")
        return true
    }

    // ========================================================================
    // Deleted Records (Recycle Bin / Others Delete Folder) Management
    // ========================================================================

    fun restoreDeletedRecord(recordId: String): Boolean {
        val record = deletedRecords.value.find { it.id == recordId } ?: return false
        val payload = record.originalPayload

        when (record.itemType) {
            "Party Account" -> {
                if (payload is PartyAccount) {
                    savePartyAccount(payload, updateExisting = true)
                }
            }
            "Chart of Account" -> {
                if (payload is Account) {
                    accounts.value = accounts.value.filterNot { it.id == payload.id || it.code == payload.code } + payload
                }
            }
            "Journal Entry" -> {
                if (payload is JournalEntry) {
                    journal.value = journal.value.filterNot { it.id == payload.id } + payload
                }
            }
            "Sales Invoice" -> {
                if (payload is SalesDoc) {
                    salesDocs.value = salesDocs.value.filterNot { it.id == payload.id } + payload
                }
            }
            "Purchase Bill" -> {
                if (payload is PurchaseDoc) {
                    purchaseDocs.value = purchaseDocs.value.filterNot { it.id == payload.id } + payload
                }
            }
            "Expense Voucher" -> {
                if (payload is ExpenseVoucher) {
                    expenseVouchers.value = expenseVouchers.value.filterNot { it.id == payload.id } + payload
                }
            }
            "Cash/Bank Txn" -> {
                if (payload is CashBankTxn) {
                    cashBankTxns.value = cashBankTxns.value.filterNot { it.id == payload.id } + payload
                }
            }
            "Customer" -> {
                if (payload is Customer) {
                    customers.value = customers.value.filterNot { it.id == payload.id } + payload
                }
            }
            "Supplier" -> {
                if (payload is Supplier) {
                    suppliers.value = suppliers.value.filterNot { it.id == payload.id } + payload
                }
            }
            "Fixed Asset" -> {
                if (payload is FixedAsset) {
                    fixedAssets.value = fixedAssets.value.filterNot { it.id == payload.id } + payload
                }
            }
            "Stock Item" -> {
                if (payload is StockItem) {
                    stockItems.value = stockItems.value.filterNot { it.id == payload.id } + payload
                }
            }
        }

        deletedRecords.value = deletedRecords.value.filterNot { it.id == recordId }
        logAudit("Deleted Items", "Restore", record.itemCode, "Restored ${record.title} (${record.itemType}) back to active records")
        return true
    }

    fun permanentlyDeleteRecord(recordId: String): Boolean {
        val record = deletedRecords.value.find { it.id == recordId } ?: return false
        deletedRecords.value = deletedRecords.value.filterNot { it.id == recordId }
        logAudit("Deleted Items", "Permanent Delete", record.itemCode, "Permanently erased ${record.title} (${record.itemType})")
        return true
    }

    fun emptyDeletedRecords(): Int {
        val count = deletedRecords.value.size
        deletedRecords.value = emptyList()
        logAudit("Deleted Items", "Empty Trash", "ALL", "Permanently emptied $count deleted records")
        return count
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

    // Double-Entry Integrity Check across ALL accounts combined in the complete ledger
    fun checkDoubleEntryIntegrity(): DoubleEntryIntegrityCheck {
        val postedEntries = journal.value.filter { it.status == "Posted" }
        var sumDebits = 0.0
        var sumCredits = 0.0
        val imbalanced = mutableListOf<JournalEntry>()

        // 1. Sum up all posted journal entries
        postedEntries.forEach { entry ->
            val entryDr = entry.lines.sumOf { it.debit }
            val entryCr = entry.lines.sumOf { it.credit }
            sumDebits += entryDr
            sumCredits += entryCr
            if (Math.abs(entryDr - entryCr) > 0.005) {
                imbalanced.add(entry)
            }
        }

        // 2. Include opening balances of all Chart of Accounts & Party Accounts
        val systemAccounts = accounts.value
        val partyList = partyAccounts.value

        var openingDr = 0.0
        var openingCr = 0.0

        // COA opening balances
        systemAccounts.forEach { acc ->
            if (acc.nature.equals("Debit", ignoreCase = true)) {
                openingDr += acc.opening
            } else {
                openingCr += acc.opening
            }
        }

        // Party opening balances not already in COA
        partyList.forEach { party ->
            if (systemAccounts.none { it.code.equals(party.code, ignoreCase = true) }) {
                if (party.balanceType.equals("Debit", ignoreCase = true) || party.balanceType.equals("Dr", ignoreCase = true)) {
                    openingDr += party.openingBalance
                } else if (party.balanceType.equals("Credit", ignoreCase = true) || party.balanceType.equals("Cr", ignoreCase = true)) {
                    openingCr += party.openingBalance
                }
            }
        }

        val totalAllDr = sumDebits + openingDr
        val totalAllCr = sumCredits + openingCr
        val diff = Math.abs(totalAllDr - totalAllCr)

        return DoubleEntryIntegrityCheck(
            isBalanced = diff <= 0.01 && imbalanced.isEmpty(),
            totalDebits = totalAllDr,
            totalCredits = totalAllCr,
            difference = diff,
            imbalancedEntries = imbalanced
        )
    }

    // Helper: Customer balance calculation from Double-Entry Ledger & Sales Subsystem
    fun getCustomerLedgerBalance(customerIdentifier: String): Double {
        val cust = customers.value.find { it.id.equals(customerIdentifier, ignoreCase = true) || it.name.equals(customerIdentifier, ignoreCase = true) }
        val party = partyAccounts.value.find { it.accountType == PartyAccountType.Customer && (it.id.equals(customerIdentifier, ignoreCase = true) || it.code.equals(customerIdentifier, ignoreCase = true) || it.name.equals(customerIdentifier, ignoreCase = true)) }

        val name = party?.name ?: cust?.name ?: customerIdentifier
        val code = party?.code ?: cust?.id ?: customerIdentifier

        val signedOpening = if (party != null) {
            if (party.balanceType.equals("Credit", ignoreCase = true) || party.balanceType.equals("Cr", ignoreCase = true) || party.balanceType.contains("Give", ignoreCase = true)) {
                -party.openingBalance
            } else {
                party.openingBalance
            }
        } else {
            cust?.openingBalance ?: 0.0
        }

        // Credit Invoices (Dr Customer)
        val creditInvoices = salesDocs.value.filter {
            (it.customerId.equals(code, ignoreCase = true) || it.customerId.equals(name, ignoreCase = true)) &&
            it.type == "Sales Invoice" && it.status == "Posted" && it.saleType != "Cash"
        }.sumOf { doc -> doc.items.sumOf { it.qty * it.rate } }

        // Sales Returns (Cr Customer)
        val returns = salesDocs.value.filter {
            (it.customerId.equals(code, ignoreCase = true) || it.customerId.equals(name, ignoreCase = true)) &&
            it.type == "Sales Return"
        }.sumOf { doc -> doc.items.sumOf { it.qty * it.rate } }

        // Customer Payments / Receipts received (Cr Customer)
        val directReceipts = salesDocs.value.filter {
            (it.customerId.equals(code, ignoreCase = true) || it.customerId.equals(name, ignoreCase = true)) &&
            it.type == "Customer Payment"
        }.sumOf { doc -> doc.items.sumOf { it.qty * it.rate } }

        val cashTxnReceipts = cashBankTxns.value.filter { txn ->
            txn.type == "Receipt" && (
                txn.contraAccount.equals(name, ignoreCase = true) ||
                txn.contraAccount.equals(code, ignoreCase = true) ||
                txn.description.contains(name, ignoreCase = true) ||
                txn.description.contains(code, ignoreCase = true)
            )
        }.sumOf { it.amount }

        // Journal debits and credits
        val journalEntries = journal.value.filter { it.status == "Posted" }
        var glDr = 0.0
        var glCr = 0.0
        journalEntries.forEach { entry ->
            // Exclude already counted subsystem source entries to prevent double count
            if (entry.source != "Sales" && entry.source != "Receipts") {
                entry.lines.forEach { line ->
                    if (line.account.equals(name, ignoreCase = true) || line.account.equals(code, ignoreCase = true) || line.account.startsWith("$name (", ignoreCase = true)) {
                        glDr += line.debit
                        glCr += line.credit
                    }
                }
            }
        }

        return signedOpening + creditInvoices + glDr - returns - directReceipts - cashTxnReceipts - glCr
    }

    // Helper: Supplier balance calculation from Double-Entry Ledger & Purchase Subsystem
    fun getSupplierLedgerBalance(supplierIdentifier: String): Double {
        val supp = suppliers.value.find { it.id.equals(supplierIdentifier, ignoreCase = true) || it.name.equals(supplierIdentifier, ignoreCase = true) }
        val party = partyAccounts.value.find { it.accountType == PartyAccountType.Supplier && (it.id.equals(supplierIdentifier, ignoreCase = true) || it.code.equals(supplierIdentifier, ignoreCase = true) || it.name.equals(supplierIdentifier, ignoreCase = true)) }

        val name = party?.name ?: supp?.name ?: supplierIdentifier
        val code = party?.code ?: supp?.id ?: supplierIdentifier

        val signedOpening = if (party != null) {
            if (party.balanceType.equals("Debit", ignoreCase = true) || party.balanceType.equals("Dr", ignoreCase = true) || party.balanceType.contains("Get", ignoreCase = true)) {
                -party.openingBalance
            } else {
                party.openingBalance
            }
        } else {
            supp?.openingBalance ?: 0.0
        }

        // Credit Purchase Bills (Cr Supplier)
        val creditBills = purchaseDocs.value.filter {
            (it.supplierId.equals(code, ignoreCase = true) || it.supplierId.equals(name, ignoreCase = true)) &&
            it.type == "Purchase Bill" && it.status == "Posted" && it.saleType != "Cash"
        }.sumOf { doc -> doc.items.sumOf { it.qty * it.rate } }

        // Purchase Returns (Dr Supplier)
        val returns = purchaseDocs.value.filter {
            (it.supplierId.equals(code, ignoreCase = true) || it.supplierId.equals(name, ignoreCase = true)) &&
            it.type == "Purchase Return"
        }.sumOf { doc -> doc.items.sumOf { it.qty * it.rate } }

        // Supplier Payments made (Dr Supplier)
        val directPayments = purchaseDocs.value.filter {
            (it.supplierId.equals(code, ignoreCase = true) || it.supplierId.equals(name, ignoreCase = true)) &&
            it.type == "Supplier Payment"
        }.sumOf { doc -> doc.items.sumOf { it.qty * it.rate } }

        val cashTxnPayments = cashBankTxns.value.filter { txn ->
            txn.type == "Payment" && (
                txn.contraAccount.equals(name, ignoreCase = true) ||
                txn.contraAccount.equals(code, ignoreCase = true) ||
                txn.description.contains(name, ignoreCase = true) ||
                txn.description.contains(code, ignoreCase = true)
            )
        }.sumOf { it.amount }

        // Journal debits and credits
        val journalEntries = journal.value.filter { it.status == "Posted" }
        var glDr = 0.0
        var glCr = 0.0
        journalEntries.forEach { entry ->
            if (entry.source != "Purchases" && entry.source != "Payments") {
                entry.lines.forEach { line ->
                    if (line.account.equals(name, ignoreCase = true) || line.account.equals(code, ignoreCase = true) || line.account.startsWith("$name (", ignoreCase = true)) {
                        glDr += line.debit
                        glCr += line.credit
                    }
                }
            }
        }

        return signedOpening + creditBills + glCr - returns - directPayments - cashTxnPayments - glDr
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

    // Opening Stock Management
    fun saveOpeningStockRecord(record: OpeningStockRecord, updateExisting: Boolean = false): Boolean {
        val currentList = openingStockRecords.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == record.id }
        val isNew = existingIndex < 0

        val openingVal = if (record.openingValue > 0.0) record.openingValue else (record.openingQty * record.openingRate)
        val finalRecord = record.copy(openingValue = openingVal)

        if (isNew) {
            currentList.add(finalRecord)
        } else {
            currentList[existingIndex] = finalRecord
        }
        openingStockRecords.value = currentList

        // 1. Ensure or update StockItem
        val existingItem = stockItems.value.find { it.name.equals(record.itemName, ignoreCase = true) || it.id == record.itemId }
        val itemToUse = if (existingItem != null) {
            val updated = existingItem.copy(
                unit = record.unit,
                costPrice = if (record.openingRate > 0.0) record.openingRate else existingItem.costPrice,
                purchasePrice = if (record.openingRate > 0.0) record.openingRate else existingItem.purchasePrice
            )
            stockItems.value = stockItems.value.map { if (it.id == existingItem.id) updated else it }
            updated
        } else {
            val newItem = StockItem(
                id = "ITM-${System.currentTimeMillis() % 100000}-${(10..99).random()}",
                sku = "SKU-${record.factoryId}-${record.itemName.take(4).uppercase()}",
                name = record.itemName,
                category = "Factory Inventory",
                unit = record.unit,
                purchasePrice = record.openingRate,
                sellingPrice = record.openingRate * 1.25,
                costPrice = record.openingRate,
                minStock = 10.0
            )
            stockItems.value = stockItems.value + newItem
            newItem
        }

        // 2. Post / update StockMove
        val existingMove = stockMoves.value.find { it.reference == "Opening — ${record.id}" || (it.itemId == itemToUse.id && it.warehouseId == record.factoryId && it.type == "Opening") }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = if (record.openingDate.isNotBlank()) record.openingDate else sdf.format(Date())

        val move = StockMove(
            id = existingMove?.id ?: "MOV-OP-${System.currentTimeMillis() % 100000}",
            date = today,
            itemId = itemToUse.id,
            warehouseId = record.factoryId,
            type = "Opening",
            qty = record.openingQty,
            unitCost = record.openingRate,
            reference = "Opening — ${record.id}"
        )
        if (existingMove != null) {
            stockMoves.value = stockMoves.value.map { if (it.id == existingMove.id) move else it }
        } else {
            stockMoves.value = stockMoves.value + move
        }

        // 3. Balanced Double-entry Journal Entry: Debit Inventory / Credit Owner Capital
        val jeId = "JE-OP-STK-${record.id}"
        journal.value = journal.value.filterNot { it.id == jeId }
        if (openingVal > 0.0) {
            postJournalEntry(
                JournalEntry(
                    id = jeId,
                    date = today,
                    source = "Opening Stock",
                    description = "Opening Stock — ${record.itemName} (${record.factoryName})",
                    reference = record.id,
                    lines = listOf(
                        JournalLine("Inventory", openingVal, 0.0, warehouseId = record.factoryId),
                        JournalLine("Owner Capital", 0.0, openingVal)
                    )
                )
            )
        }

        logAudit("Inventory", if (isNew) "Create Opening" else "Update Opening", record.id, "${record.openingQty} ${record.unit} of ${record.itemName} at ${record.factoryName} (Rs $openingVal)")
        MasStorageManager.saveToPersistentStorage()
        return true
    }

    fun deleteOpeningStockRecord(id: String): Boolean {
        val rec = openingStockRecords.value.find { it.id == id } ?: return false
        openingStockRecords.value = openingStockRecords.value.filterNot { it.id == id }
        journal.value = journal.value.filterNot { it.id == "JE-OP-STK-$id" || it.reference == id }
        stockMoves.value = stockMoves.value.filterNot { it.reference == "Opening — $id" }

        val deleted = DeletedRecord(
            id = "DEL-${System.currentTimeMillis()}-${(100..999).random()}",
            itemType = "Opening Stock",
            itemCode = rec.id,
            title = "Opening Stock: ${rec.itemName}",
            subtitle = "${rec.factoryName} · ${rec.openingQty} ${rec.unit} @ Rs ${rec.openingRate}",
            amount = rec.openingValue,
            originalPayload = rec
        )
        deletedRecords.value = listOf(deleted) + deletedRecords.value
        logAudit("Inventory", "Delete Opening", id, "Deleted opening stock of ${rec.itemName} from ${rec.factoryName}")
        MasStorageManager.saveToPersistentStorage()
        return true
    }

    // Multiple Cash In Hand Accounts calculation
    fun getCashAccountBalances(): List<CashAccountBalance> {
        val cashAccounts = cashBankAccounts.value.filter { it.kind.equals("Cash", ignoreCase = true) }
        val txns = cashBankTxns.value

        return cashAccounts.map { acc ->
            val accTxns = txns.filter { it.accountId == acc.id || it.accountId == acc.name || it.fromAccountId == acc.id || it.toAccountId == acc.id }
            val debits = accTxns.filter { it.type == "Receipt" || it.toAccountId == acc.id }.sumOf { it.amount }
            val credits = accTxns.filter { it.type == "Payment" || it.fromAccountId == acc.id }.sumOf { it.amount }
            val currentBal = acc.openingBalance + debits - credits

            CashAccountBalance(
                id = acc.id,
                code = acc.id,
                name = acc.name,
                kind = "Cash",
                openingBalance = acc.openingBalance,
                totalDebit = debits,
                totalCredit = credits,
                currentBalance = currentBal,
                status = "Active"
            )
        }
    }

    // Factory Stock Synchronization
    fun getFactoryStockRecords(): List<FactoryStockRecord> {
        val factoryParties = partyAccounts.value.filter { it.accountType == PartyAccountType.Factory }
        val allItems = stockItems.value
        val moves = stockMoves.value
        val opRecords = openingStockRecords.value

        return factoryParties.map { factory ->
            val factoryLocIds = setOf(factory.code, factory.name, factory.id)
            
            val factoryOpRecs = opRecords.filter { it.factoryId in factoryLocIds || it.factoryName.equals(factory.name, ignoreCase = true) }
            val opStockQty = factoryOpRecs.sumOf { it.openingQty }
            val opStockVal = factoryOpRecs.sumOf { it.openingValue }

            val factoryMoves = moves.filter { it.warehouseId in factoryLocIds || (it.warehouseId != null && it.warehouseId.contains(factory.code, ignoreCase = true)) }
            val purchasesQty = factoryMoves.filter { it.type == "In" || it.reference.contains("Purchase", ignoreCase = true) }.sumOf { it.qty }
            val purchasesVal = factoryMoves.filter { it.type == "In" || it.reference.contains("Purchase", ignoreCase = true) }.sumOf { it.qty * (it.unitCost ?: 0.0) }
            val salesQty = factoryMoves.filter { it.type == "Out" || it.reference.contains("Sale", ignoreCase = true) }.sumOf { it.qty }
            val salesVal = factoryMoves.filter { it.type == "Out" || it.reference.contains("Sale", ignoreCase = true) }.sumOf { it.qty * (it.unitCost ?: 0.0) }

            val stockIn = factoryMoves.filter { it.type in listOf("In", "Opening", "Transfer In", "Adjustment +") }.sumOf { it.qty }
            val stockOut = factoryMoves.filter { it.type in listOf("Out", "Transfer Out", "Adjustment -") }.sumOf { it.qty }

            val itemStocks = mutableListOf<FactoryItemStock>()

            // 1. Items from opening stock
            factoryOpRecs.forEach { op ->
                val itmMoves = factoryMoves.filter { it.reference.contains(op.id) || it.itemId == op.itemId }
                val itmPurchases = itmMoves.filter { it.type == "In" }.sumOf { it.qty }
                val itmSales = itmMoves.filter { it.type == "Out" }.sumOf { it.qty }
                val curQty = (op.openingQty + itmPurchases - itmSales).coerceAtLeast(0.0)
                itemStocks.add(
                    FactoryItemStock(
                        itemId = op.itemId.ifBlank { op.id },
                        itemName = op.itemName,
                        sku = "SKU-${factory.code}",
                        unit = op.unit,
                        costPrice = op.openingRate,
                        openingQty = op.openingQty,
                        openingValue = op.openingValue,
                        purchasesQty = itmPurchases,
                        purchasesValue = itmPurchases * op.openingRate,
                        salesQty = itmSales,
                        salesValue = itmSales * op.openingRate,
                        stockIn = op.openingQty + itmPurchases,
                        stockOut = itmSales,
                        quantity = curQty,
                        totalValue = curQty * op.openingRate
                    )
                )
            }

            // 2. Additional items from general moves
            allItems.forEach { item ->
                if (itemStocks.none { it.itemName.equals(item.name, ignoreCase = true) }) {
                    val itmMoves = factoryMoves.filter { it.itemId == item.id }
                    if (itmMoves.isNotEmpty()) {
                        val itmIn = itmMoves.filter { it.type in listOf("In", "Opening", "Transfer In", "Adjustment +") }.sumOf { it.qty }
                        val itmOut = itmMoves.filter { it.type in listOf("Out", "Transfer Out", "Adjustment -") }.sumOf { it.qty }
                        val curQty = (itmIn - itmOut).coerceAtLeast(0.0)
                        if (itmIn > 0.0 || itmOut > 0.0 || curQty > 0.0) {
                            val opQty = itmMoves.filter { it.type == "Opening" }.sumOf { it.qty }
                            val purQty = itmMoves.filter { it.type == "In" }.sumOf { it.qty }
                            val salQty = itmMoves.filter { it.type == "Out" }.sumOf { it.qty }
                            itemStocks.add(
                                FactoryItemStock(
                                    itemId = item.id,
                                    itemName = item.name,
                                    sku = item.sku,
                                    unit = item.unit,
                                    costPrice = item.costPrice,
                                    openingQty = opQty,
                                    openingValue = opQty * item.costPrice,
                                    purchasesQty = purQty,
                                    purchasesValue = purQty * item.costPrice,
                                    salesQty = salQty,
                                    salesValue = salQty * item.costPrice,
                                    stockIn = itmIn,
                                    stockOut = itmOut,
                                    quantity = curQty,
                                    totalValue = curQty * item.costPrice
                                )
                            )
                        }
                    }
                }
            }

            val totalQty = if (itemStocks.isNotEmpty()) itemStocks.sumOf { it.quantity } else (opStockQty + purchasesQty - salesQty).coerceAtLeast(0.0)
            val totalVal = if (itemStocks.isNotEmpty()) itemStocks.sumOf { it.totalValue } else (totalQty * 280.0)

            FactoryStockRecord(
                factoryId = factory.id,
                factoryName = factory.name,
                factoryCode = factory.code,
                openingStockQty = opStockQty,
                openingStockValue = opStockVal,
                purchasesQty = purchasesQty,
                purchasesValue = purchasesVal,
                salesQty = salesQty,
                salesValue = salesVal,
                totalStockIn = stockIn,
                totalStockOut = stockOut,
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
