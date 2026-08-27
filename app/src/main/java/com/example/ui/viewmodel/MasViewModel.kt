package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.components.AccountPickerOption
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MasViewModel : ViewModel() {

    // Repository bindings
    val currentUser = MasRepository.currentUser
    val companyProfile = MasRepository.companyProfile
    val accounts = MasRepository.accounts
    val partyAccounts = MasRepository.partyAccounts
    val journal = MasRepository.journal
    val customers = MasRepository.customers
    val suppliers = MasRepository.suppliers
    val salesDocs = MasRepository.salesDocs
    val purchaseDocs = MasRepository.purchaseDocs
    val cashBankAccounts = MasRepository.cashBankAccounts
    val cashBankTxns = MasRepository.cashBankTxns
    val stockItems = MasRepository.stockItems
    val stockMoves = MasRepository.stockMoves
    val expenseVouchers = MasRepository.expenseVouchers
    val recurringExpenses = MasRepository.recurringExpenses
    val expenseCategories = MasRepository.expenseCategories
    val rawMaterials = MasRepository.rawMaterials
    val finishedGoods = MasRepository.finishedGoods
    val boms = MasRepository.boms
    val productionOrders = MasRepository.productionOrders
    val consumptions = MasRepository.consumptions
    val productionOutputs = MasRepository.productionOutputs
    val manufacturingOverheads = MasRepository.manufacturingOverheads
    val fixedAssets = MasRepository.fixedAssets
    val assetCategories = MasRepository.assetCategories
    val assetDepreciations = MasRepository.assetDepreciations
    val assetTransfers = MasRepository.assetTransfers
    val assetDisposals = MasRepository.assetDisposals
    val branches = MasRepository.branches
    val departments = MasRepository.departments
    val projects = MasRepository.projects
    val costCenters = MasRepository.costCenters
    val warehouses = MasRepository.warehouses
    val users = MasRepository.users
    val auditLog = MasRepository.auditLog
    val periodStatuses = MasRepository.periodStatuses
    val isFiscalYearClosed = MasRepository.isFiscalYearClosed
    val deletedRecords = MasRepository.deletedRecords
    val openingStockRecords = MasRepository.openingStockRecords
    val cloudSyncState = MasStorageManager.cloudSyncState

    fun canModify(): Boolean = MasRepository.canModify()
    fun isViewer(): Boolean = MasRepository.isViewer()
    fun getCashAccountBalances(): List<CashAccountBalance> = MasRepository.getCashAccountBalances()

    // UI Feedback state
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    // Helper: Customer balance calculation from Double-Entry Ledger & Sales Subsystem
    fun getCustomerBalance(customerId: String): Double {
        return MasRepository.getCustomerLedgerBalance(customerId)
    }

    // Helper: Supplier balance calculation from Double-Entry Ledger & Purchase Subsystem
    fun getSupplierBalance(supplierId: String): Double {
        return MasRepository.getSupplierLedgerBalance(supplierId)
    }

    // Double-Entry Ledger Balance Calculation for any Account
    fun getAccountLedgerBalance(account: Account): AccountLedgerBalance {
        return MasRepository.getAccountLedgerBalance(account)
    }

    // Helper: Stock item balance (unified across warehouses and factory locations without duplication)
    fun getStockItemQuantity(itemId: String, warehouseId: String? = null): Double {
        val moves = stockMoves.value.filter { it.itemId == itemId && (warehouseId == null || it.warehouseId == warehouseId) }
        return moves.sumOf {
            when (it.type) {
                "Opening", "In", "Transfer In", "Adjustment +" -> it.qty
                "Out", "Transfer Out", "Adjustment -" -> -it.qty
                else -> 0.0
            }
        }
    }

    fun getTotalStockValue(): Double {
        return stockItems.value.sumOf { item ->
            val qty = getStockItemQuantity(item.id)
            (if (qty > 0) qty else 0.0) * item.costPrice
        }
    }

    // Helper: Executive Metrics calculation
    val totalSales = salesDocs.map { docs ->
        val posted = docs.filter { it.type == "Sales Invoice" && it.status == "Posted" }
        val returns = docs.filter { it.type == "Sales Return" }
        posted.sumOf { it.items.sumOf { l -> l.qty * l.rate } } - returns.sumOf { it.items.sumOf { l -> l.qty * l.rate } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val totalPurchases = purchaseDocs.map { docs ->
        val posted = docs.filter { it.type == "Purchase Bill" && it.status == "Posted" }
        val returns = docs.filter { it.type == "Purchase Return" }
        posted.sumOf { it.items.sumOf { l -> l.qty * l.rate } } - returns.sumOf { it.items.sumOf { l -> l.qty * l.rate } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // Total Receivable: Sum of Customer debit balances from ledger (NEVER mixed with cash in hand)
    val totalReceivable = combine(customers, partyAccounts, salesDocs, cashBankTxns, journal) { custList, parties, _, _, _ ->
        val allCustomerCodes = (custList.map { it.id } + parties.filter { it.accountType == PartyAccountType.Customer }.map { it.code }).distinct()
        allCustomerCodes.sumOf { custCode ->
            val bal = MasRepository.getCustomerLedgerBalance(custCode)
            if (bal > 0.001) bal else 0.0
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // Total Payable: Sum of Supplier credit balances from ledger (NEVER mixed with cash in hand)
    val totalPayable = combine(suppliers, partyAccounts, purchaseDocs, cashBankTxns, journal) { suppList, parties, _, _, _ ->
        val allSupplierCodes = (suppList.map { it.id } + parties.filter { it.accountType == PartyAccountType.Supplier }.map { it.code }).distinct()
        allSupplierCodes.sumOf { suppCode ->
            val bal = MasRepository.getSupplierLedgerBalance(suppCode)
            if (bal > 0.001) bal else 0.0
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // Cash in Hand - Munawar (Strictly ledger-based: Dr - Cr)
    val munawarCash = combine(cashBankAccounts, cashBankTxns, partyAccounts, journal) { accountsList, txnsList, parties, _ ->
        val munParty = parties.find { it.accountType == PartyAccountType.CashInHand && it.name.contains("Munawar", ignoreCase = true) }
        val munAcc = accountsList.find { it.kind == "Cash" && (it.name.contains("Munawar", ignoreCase = true) || it.id.contains("MUN", ignoreCase = true)) }
        val idOrCode = munParty?.code ?: munAcc?.id ?: "CSH-MUN"
        val name = munParty?.name ?: munAcc?.name ?: "Cash in Hand — Munawar"
        val signedOpening = if (munParty != null) {
            if (munParty.balanceType.equals("Credit", ignoreCase = true) || munParty.balanceType.equals("Cr", ignoreCase = true) || munParty.balanceType.contains("Give", ignoreCase = true)) -munParty.openingBalance else munParty.openingBalance
        } else {
            munAcc?.openingBalance ?: 0.0
        }
        val inTx = txnsList.filter { (it.accountId == idOrCode || it.accountId == name || it.toAccountId == idOrCode || it.toAccountId == name) && (it.type == "Receipt" || it.type == "Transfer") }.sumOf { it.amount }
        val outTx = txnsList.filter { (it.accountId == idOrCode || it.accountId == name || it.fromAccountId == idOrCode || it.fromAccountId == name) && (it.type == "Payment" || it.type == "Transfer") }.sumOf { it.amount }
        signedOpening + inTx - outTx
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // Cash in Hand - Khalid (Strictly ledger-based: Dr - Cr)
    val khalidCash = combine(cashBankAccounts, cashBankTxns, partyAccounts, journal) { accountsList, txnsList, parties, _ ->
        val khalParty = parties.find { it.accountType == PartyAccountType.CashInHand && it.name.contains("Khalid", ignoreCase = true) }
        val khalAcc = accountsList.find { it.kind == "Cash" && (it.name.contains("Khalid", ignoreCase = true) || it.id.contains("KHL", ignoreCase = true)) }
        val idOrCode = khalParty?.code ?: khalAcc?.id ?: "CSH-KHL"
        val name = khalParty?.name ?: khalAcc?.name ?: "Cash in Hand — Khalid"
        val signedOpening = if (khalParty != null) {
            if (khalParty.balanceType.equals("Credit", ignoreCase = true) || khalParty.balanceType.equals("Cr", ignoreCase = true) || khalParty.balanceType.contains("Give", ignoreCase = true)) -khalParty.openingBalance else khalParty.openingBalance
        } else {
            khalAcc?.openingBalance ?: 0.0
        }
        val inTx = txnsList.filter { (it.accountId == idOrCode || it.accountId == name || it.toAccountId == idOrCode || it.toAccountId == name) && (it.type == "Receipt" || it.type == "Transfer") }.sumOf { it.amount }
        val outTx = txnsList.filter { (it.accountId == idOrCode || it.accountId == name || it.fromAccountId == idOrCode || it.fromAccountId == name) && (it.type == "Payment" || it.type == "Transfer") }.sumOf { it.amount }
        signedOpening + inTx - outTx
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // Total Cash in Hand (Sum of independent cash accounts for Balance Sheet / Summary views)
    val cashInHand = combine(munawarCash, khalidCash) { mun, khal ->
        mun + khal
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // Bank Balance: Strictly derived from Bank accounts
    val bankBalance = combine(cashBankAccounts, cashBankTxns, journal) { accountsList, txnsList, _ ->
        accountsList.filter { it.kind == "Bank" }.sumOf { acc ->
            val inTx = txnsList.filter { (it.accountId == acc.id && it.type == "Receipt") || it.toAccountId == acc.id }.sumOf { it.amount }
            val outTx = txnsList.filter { (it.accountId == acc.id && it.type == "Payment") || it.fromAccountId == acc.id }.sumOf { it.amount }
            acc.openingBalance + inTx - outTx
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val totalExpenses = expenseVouchers.map { list ->
        list.filter { it.status == "Posted" || it.status == "Paid" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // Helper to get ledger details for specific cash accounts
    fun getCashAccountBalance(accountKey: String): AccountLedgerBalance {
        val isMunawar = accountKey.contains("Munawar", ignoreCase = true) || accountKey.contains("MUN", ignoreCase = true)
        val targetName = if (isMunawar) "Munawar" else "Khalid"
        val fullName = if (isMunawar) "Cash in Hand — Munawar" else "Cash in Hand — Khalid"
        val codeDefault = if (isMunawar) "CSH-MUN" else "CSH-KHL"

        val txns = cashBankTxns.value
        val party = partyAccounts.value.find { it.accountType == PartyAccountType.CashInHand && it.name.contains(targetName, ignoreCase = true) }
        val acc = cashBankAccounts.value.find { it.kind == "Cash" && (it.name.contains(targetName, ignoreCase = true) || it.id == codeDefault) }

        val code = party?.code ?: acc?.id ?: codeDefault
        val name = party?.name ?: acc?.name ?: fullName
        val signedOpening = if (party != null) {
            if (party.balanceType.equals("Credit", ignoreCase = true) || party.balanceType.equals("Cr", ignoreCase = true) || party.balanceType.contains("Give", ignoreCase = true)) -party.openingBalance else party.openingBalance
        } else {
            acc?.openingBalance ?: 0.0
        }

        val inTx = txns.filter { (it.accountId == code || it.accountId == name || it.toAccountId == code || it.toAccountId == name) && (it.type == "Receipt" || it.type == "Transfer") }.sumOf { it.amount }
        val outTx = txns.filter { (it.accountId == code || it.accountId == name || it.fromAccountId == code || it.fromAccountId == name) && (it.type == "Payment" || it.type == "Transfer") }.sumOf { it.amount }

        val net = signedOpening + inTx - outTx
        val totalDebit = (if (signedOpening > 0) signedOpening else 0.0) + inTx
        val totalCredit = (if (signedOpening < 0) -signedOpening else 0.0) + outTx

        return AccountLedgerBalance(
            accountCode = code,
            accountName = name,
            accountType = "Cash in Hand",
            normalNature = if (net >= 0) "Debit" else "Credit",
            openingBalance = Math.abs(signedOpening),
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            currentBalance = Math.abs(net),
            drCrIndicator = if (net >= 0) "Dr" else "Cr"
        )
    }

    // Cash in Hand Accounts list with individual Ledger Balances and Dr/Cr tags
    fun getIndividualCashInHandAccounts(): List<AccountLedgerBalance> {
        return listOf(
            getCashAccountBalance("Munawar"),
            getCashAccountBalance("Khalid")
        )
    }

    // Helper to get all accounts formatted for SearchableAccountPicker with current live balances
    fun getAllAccountPickerOptions(): List<AccountPickerOption> {
        val list = mutableListOf<AccountPickerOption>()

        // 1. Customers
        val customerParties = partyAccounts.value.filter { it.accountType == PartyAccountType.Customer }
        val customerCodes = customerParties.map { it.code }.toSet()
        customerParties.forEach { p ->
            val bal = MasRepository.getCustomerLedgerBalance(p.code)
            list.add(
                AccountPickerOption(
                    id = p.id,
                    code = p.code,
                    name = p.name,
                    category = "Customer",
                    balance = bal,
                    drCrIndicator = if (bal >= 0) "Dr" else "Cr",
                    subtitle = p.phone
                )
            )
        }
        customers.value.filter { it.id !in customerCodes }.forEach { c ->
            val bal = MasRepository.getCustomerLedgerBalance(c.id)
            list.add(
                AccountPickerOption(
                    id = c.id,
                    code = c.id,
                    name = c.name,
                    category = "Customer",
                    balance = bal,
                    drCrIndicator = if (bal >= 0) "Dr" else "Cr",
                    subtitle = c.phone
                )
            )
        }

        // 2. Suppliers
        val supplierParties = partyAccounts.value.filter { it.accountType == PartyAccountType.Supplier }
        val supplierCodes = supplierParties.map { it.code }.toSet()
        supplierParties.forEach { p ->
            val bal = MasRepository.getSupplierLedgerBalance(p.code)
            list.add(
                AccountPickerOption(
                    id = p.id,
                    code = p.code,
                    name = p.name,
                    category = "Supplier",
                    balance = bal,
                    drCrIndicator = if (bal >= 0) "Cr" else "Dr",
                    subtitle = p.phone
                )
            )
        }
        suppliers.value.filter { it.id !in supplierCodes }.forEach { s ->
            val bal = MasRepository.getSupplierLedgerBalance(s.id)
            list.add(
                AccountPickerOption(
                    id = s.id,
                    code = s.id,
                    name = s.name,
                    category = "Supplier",
                    balance = bal,
                    drCrIndicator = if (bal >= 0) "Cr" else "Dr",
                    subtitle = s.phone
                )
            )
        }

        // 3. Cash & Bank Accounts
        cashBankAccounts.value.forEach { acc ->
            val bal = if (acc.kind == "Cash") {
                val cal = getCashAccountBalance(acc.name)
                if (cal.drCrIndicator == "Dr") cal.currentBalance else -cal.currentBalance
            } else {
                val inTx = cashBankTxns.value.filter { it.accountId == acc.id && it.type == "Receipt" || it.toAccountId == acc.id }.sumOf { it.amount }
                val outTx = cashBankTxns.value.filter { it.accountId == acc.id && it.type == "Payment" || it.fromAccountId == acc.id }.sumOf { it.amount }
                acc.openingBalance + inTx - outTx
            }
            list.add(
                AccountPickerOption(
                    id = acc.id,
                    code = acc.id,
                    name = acc.name,
                    category = if (acc.kind == "Cash") "Cash in Hand" else "Bank",
                    balance = Math.abs(bal),
                    drCrIndicator = if (bal >= 0) "Dr" else "Cr",
                    subtitle = acc.bankName ?: acc.kind
                )
            )
        }

        // 4. Other Chart of Accounts (Expenses, Revenues, Equity, Fixed Assets)
        accounts.value.forEach { acc ->
            if (list.none { opt: AccountPickerOption -> opt.name.equals(acc.name, ignoreCase = true) || opt.code.equals(acc.code, ignoreCase = true) }) {
                val bal = MasRepository.getAccountLedgerBalance(acc)
                list.add(
                    AccountPickerOption(
                        id = acc.id,
                        code = acc.code,
                        name = acc.name,
                        category = acc.category.ifBlank { acc.type.name },
                        balance = bal.currentBalance,
                        drCrIndicator = bal.drCrIndicator,
                        subtitle = acc.type.name
                    )
                )
            }
        }

        return list
    }

    // Factory Stock Synchronization Records
    fun getFactoryStockRecords(): List<FactoryStockRecord> {
        return MasRepository.getFactoryStockRecords()
    }

    // Stock Transfer between Factory locations or Warehouses
    fun recordStockTransfer(
        fromLocationId: String,
        toLocationId: String,
        itemId: String,
        qty: Double,
        date: String,
        reference: String
    ): Boolean {
        val success = MasRepository.recordStockTransfer(fromLocationId, toLocationId, itemId, qty, date, reference)
        if (success) {
            showMessage("Transferred $qty units from $fromLocationId to $toLocationId successfully.")
        } else {
            showMessage("Failed to transfer stock. Check item and location selections.")
        }
        return success
    }

    // Owner Capital Investment (Double-Entry: Dr Cash/Bank, Cr Owner Capital)
    fun recordOwnerInvestment(
        ownerPartyId: String,
        depositAccountId: String,
        amount: Double,
        date: String,
        ref: String
    ): Boolean {
        val party = partyAccounts.value.find { it.id == ownerPartyId || it.code == ownerPartyId }
        val ownerName = party?.name ?: "Owner Capital"
        val capitalAccName = "$ownerName (Owner Capital)"
        val txnId = "INV-${System.currentTimeMillis() % 10000}"

        val matchedAcc = cashBankAccounts.value.find { it.name.equals(depositAccountId, ignoreCase = true) }
            ?: cashBankAccounts.value.firstOrNull { if (depositAccountId.contains("Bank", ignoreCase = true)) it.kind == "Bank" else it.kind == "Cash" }
            ?: cashBankAccounts.value.firstOrNull()

        // 1. Post to Cash/Bank Txn
        val txn = CashBankTxn(
            id = txnId,
            type = "Receipt",
            accountId = matchedAcc?.id ?: "ACC-001",
            date = date,
            description = "Owner Capital Investment by $ownerName",
            contraAccount = "Owner Capital",
            amount = amount,
            reference = ref
        )
        cashBankTxns.value = cashBankTxns.value + txn

        // 2. Post Strict Double-Entry Journal
        val (posted, msg) = MasRepository.postJournalEntry(
            JournalEntry(
                id = "JE-$txnId",
                date = date,
                source = "Owner Investment",
                description = "Capital introduced by $ownerName",
                reference = ref.ifBlank { txnId },
                lines = listOf(
                    JournalLine(depositAccountId, amount, 0.0),
                    JournalLine("Owner Capital", 0.0, amount)
                )
            )
        )

        if (posted) {
            showMessage("Recorded capital investment of Rs $amount by $ownerName.")
        } else {
            showMessage(msg)
        }
        return posted
    }

    // Owner Withdrawal / Drawings (Double-Entry: Dr Drawings, Cr Cash/Bank)
    fun recordOwnerWithdrawal(
        ownerPartyId: String,
        payFromAccountId: String,
        amount: Double,
        date: String,
        ref: String
    ): Boolean {
        val party = partyAccounts.value.find { it.id == ownerPartyId || it.code == ownerPartyId }
        val ownerName = party?.name ?: "Owner"
        val txnId = "DRW-${System.currentTimeMillis() % 10000}"

        val matchedAcc = cashBankAccounts.value.find { it.name.equals(payFromAccountId, ignoreCase = true) }
            ?: cashBankAccounts.value.firstOrNull { if (payFromAccountId.contains("Bank", ignoreCase = true)) it.kind == "Bank" else it.kind == "Cash" }
            ?: cashBankAccounts.value.firstOrNull()

        // 1. Post to Cash/Bank Txn
        val txn = CashBankTxn(
            id = txnId,
            type = "Payment",
            accountId = matchedAcc?.id ?: "ACC-001",
            date = date,
            description = "Owner Drawings / Withdrawal by $ownerName",
            contraAccount = "Drawings",
            amount = amount,
            reference = ref
        )
        cashBankTxns.value = cashBankTxns.value + txn

        // 2. Post Strict Double-Entry Journal
        val (posted, msg) = MasRepository.postJournalEntry(
            JournalEntry(
                id = "JE-$txnId",
                date = date,
                source = "Drawings",
                description = "Personal drawings by $ownerName",
                reference = ref.ifBlank { txnId },
                lines = listOf(
                    JournalLine("Drawings", amount, 0.0),
                    JournalLine(payFromAccountId, 0.0, amount)
                )
            )
        )

        if (posted) {
            showMessage("Recorded drawings of Rs $amount by $ownerName.")
        } else {
            showMessage(msg)
        }
        return posted
    }

    // Live Double-Entry Integrity Check
    fun checkDoubleEntryIntegrity(): DoubleEntryIntegrityCheck {
        return MasRepository.checkDoubleEntryIntegrity()
    }

    // Double-Entry Actions
    fun addJournalEntry(entry: JournalEntry): Pair<Boolean, String> {
        return MasRepository.postJournalEntry(entry)
    }

    fun approveJournalEntry(id: String) {
        journal.value = journal.value.map {
            if (it.id == id) it.copy(status = "Posted", approvedBy = currentUser.value.name) else it
        }
        MasRepository.logAudit("General Ledger", "Approve", id, "Journal Entry approved and posted")
        showMessage("Journal Entry $id approved & posted.")
    }

    fun rejectJournalEntry(id: String) {
        journal.value = journal.value.map {
            if (it.id == id) it.copy(status = "Rejected") else it
        }
        MasRepository.logAudit("General Ledger", "Reject", id, "Journal Entry rejected")
        showMessage("Journal Entry $id rejected.")
    }

    // Customer Actions
    fun addCustomer(customer: Customer) {
        customers.value = customers.value + customer
        MasRepository.logAudit("Customers", "Create", customer.id, "Added customer ${customer.name}")
        showMessage("Customer ${customer.name} created.")
    }

    fun recordCustomerReceipt(customerId: String, invoiceId: String?, amount: Double, method: String, depositAccount: String, ref: String) {
        val cust = customers.value.find { it.id == customerId } ?: return
        val docId = "RCPT-${1000 + salesDocs.value.count { it.type == "Customer Payment" } + 1}"
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())

        val paymentDoc = SalesDoc(
            id = docId,
            type = "Customer Payment",
            customerId = customerId,
            date = today,
            reference = invoiceId,
            method = method,
            items = listOf(LineItem(description = "Payment received — $method", qty = 1.0, rate = amount)),
            status = "Received",
            postings = listOf(
                JournalLine(depositAccount, amount, 0.0),
                JournalLine("Accounts Receivable", 0.0, amount)
            )
        )
        salesDocs.value = salesDocs.value + paymentDoc

        // Post into cash bank transactions and journal
        val matchedAcc = cashBankAccounts.value.find { it.name.equals(depositAccount, ignoreCase = true) }
            ?: cashBankAccounts.value.firstOrNull { if (depositAccount.contains("Bank", ignoreCase = true)) it.kind == "Bank" else it.kind == "Cash" }
            ?: cashBankAccounts.value.firstOrNull()

        val txn = CashBankTxn(
            id = "RCT-${System.currentTimeMillis() % 10000}",
            type = "Receipt",
            accountId = matchedAcc?.id ?: "ACC-001",
            date = today,
            description = "Receipt from ${cust.name} ${if (invoiceId != null) "for $invoiceId" else ""}",
            contraAccount = "Accounts Receivable",
            amount = amount,
            reference = ref
        )
        cashBankTxns.value = cashBankTxns.value + txn

        MasRepository.postJournalEntry(
            JournalEntry(
                id = "JE-$docId",
                date = today,
                source = "Receipts",
                description = "Customer payment: ${cust.name}",
                reference = docId,
                lines = listOf(
                    JournalLine(depositAccount, amount, 0.0),
                    JournalLine("Accounts Receivable", 0.0, amount)
                )
            )
        )
        showMessage("Receipt of $amount recorded for ${cust.name}.")
    }

    // Sales Actions
    fun addSalesDoc(doc: SalesDoc) {
        salesDocs.value = salesDocs.value + doc

        // If it's a posted invoice, post journal and update inventory if items are linked
        if (doc.type == "Sales Invoice" && doc.status == "Posted") {
            val total = doc.items.sumOf { it.qty * it.rate }

            if (doc.saleType == "Cash") {
                val selectedAccount = doc.paymentAccount ?: (cashBankAccounts.value.firstOrNull()?.name ?: "Khalid Cash 1")
                val glAccountName = selectedAccount

                // 1. Post General Ledger double-entry
                MasRepository.postJournalEntry(
                    JournalEntry(
                        id = "JE-${doc.id}",
                        date = doc.date,
                        source = "Sales",
                        description = "Cash Sale — Invoice ${doc.id}",
                        reference = doc.id,
                        lines = listOf(
                            JournalLine(glAccountName, total, 0.0),
                            JournalLine("Sales Revenue", 0.0, total)
                        )
                    )
                )

                // 2. Post Cash / Bank Ledger Transaction
                val matchedAccount = cashBankAccounts.value.find { it.name.equals(selectedAccount, ignoreCase = true) }
                    ?: cashBankAccounts.value.firstOrNull { if (selectedAccount.contains("Bank", ignoreCase = true)) it.kind == "Bank" else it.kind == "Cash" }
                    ?: cashBankAccounts.value.firstOrNull()

                val cashTxn = CashBankTxn(
                    id = "RCT-${System.currentTimeMillis() % 10000}",
                    type = "Receipt",
                    accountId = matchedAccount?.id ?: "ACC-001",
                    date = doc.date,
                    description = "Cash Sale — ${doc.id} (${doc.items.firstOrNull()?.description ?: "Goods"})",
                    contraAccount = "Sales Revenue",
                    amount = total,
                    reference = doc.id
                )
                cashBankTxns.value = cashBankTxns.value + cashTxn
            } else {
                // Credit Sale
                val cust = customers.value.find { it.id == doc.customerId }
                MasRepository.postJournalEntry(
                    JournalEntry(
                        id = "JE-${doc.id}",
                        date = doc.date,
                        source = "Sales",
                        description = "Credit Sale — Invoice ${doc.id} (${cust?.name ?: doc.customerId})",
                        reference = doc.id,
                        lines = listOf(
                            JournalLine("Accounts Receivable", total, 0.0),
                            JournalLine("Sales Revenue", 0.0, total)
                        )
                    )
                )
            }

            // Deduct inventory for linked stock items
            doc.items.filter { it.itemId != null }.forEach { item ->
                val move = StockMove(
                    id = "MOV-${System.currentTimeMillis() % 10000}",
                    date = doc.date,
                    itemId = item.itemId!!,
                    warehouseId = "WH-01",
                    type = "Out",
                    qty = item.qty,
                    reference = "Sale — ${doc.id}"
                )
                stockMoves.value = stockMoves.value + move
            }
        }
        showMessage("${doc.type} ${doc.id} recorded successfully.")
    }

    // Purchase Actions
    fun addPurchaseDoc(doc: PurchaseDoc) {
        purchaseDocs.value = purchaseDocs.value + doc

        if (doc.type == "Purchase Bill" && doc.status == "Posted") {
            val total = doc.items.sumOf { it.qty * it.rate }

            if (doc.saleType == "Cash") {
                val selectedAccount = doc.paymentAccount ?: (cashBankAccounts.value.firstOrNull()?.name ?: "Khalid Cash 1")
                val glAccountName = selectedAccount

                // 1. Post General Ledger double-entry
                MasRepository.postJournalEntry(
                    JournalEntry(
                        id = "JE-${doc.id}",
                        date = doc.date,
                        source = "Purchases",
                        description = "Cash Purchase — Bill ${doc.id}",
                        reference = doc.id,
                        lines = listOf(
                            JournalLine("Purchases", total, 0.0),
                            JournalLine(glAccountName, 0.0, total)
                        )
                    )
                )

                // 2. Post Cash / Bank Ledger Transaction
                val matchedAccount = cashBankAccounts.value.find { it.name.equals(selectedAccount, ignoreCase = true) }
                    ?: cashBankAccounts.value.firstOrNull { if (selectedAccount.contains("Bank", ignoreCase = true)) it.kind == "Bank" else it.kind == "Cash" }
                    ?: cashBankAccounts.value.firstOrNull()

                val cashTxn = CashBankTxn(
                    id = "PMT-${System.currentTimeMillis() % 10000}",
                    type = "Payment",
                    accountId = matchedAccount?.id ?: "ACC-001",
                    date = doc.date,
                    description = "Cash Purchase — ${doc.id} (${doc.items.firstOrNull()?.description ?: "Goods"})",
                    contraAccount = "Purchases",
                    amount = total,
                    reference = doc.id
                )
                cashBankTxns.value = cashBankTxns.value + cashTxn
            } else {
                // Credit Purchase
                val supp = suppliers.value.find { it.id == doc.supplierId }
                MasRepository.postJournalEntry(
                    JournalEntry(
                        id = "JE-${doc.id}",
                        date = doc.date,
                        source = "Purchases",
                        description = "Credit Purchase — Bill ${doc.id} (${supp?.name ?: doc.supplierId})",
                        reference = doc.id,
                        lines = listOf(
                            JournalLine("Purchases", total, 0.0),
                            JournalLine("Accounts Payable", 0.0, total)
                        )
                    )
                )
            }

            // Add stock for linked items
            doc.items.filter { it.itemId != null }.forEach { item ->
                val move = StockMove(
                    id = "MOV-${System.currentTimeMillis() % 10000}",
                    date = doc.date,
                    itemId = item.itemId!!,
                    warehouseId = "WH-01",
                    type = "In",
                    qty = item.qty,
                    unitCost = item.rate,
                    reference = "Purchase — ${doc.id}"
                )
                stockMoves.value = stockMoves.value + move
            }
        }
        showMessage("${doc.type} ${doc.id} recorded successfully.")
    }

    // Supplier Payment
    fun recordSupplierPayment(supplierId: String, billId: String?, amount: Double, method: String, paidFromAccount: String, ref: String) {
        val supp = suppliers.value.find { it.id == supplierId } ?: return
        val docId = "PMT-${1000 + purchaseDocs.value.count { it.type == "Supplier Payment" } + 1}"
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())

        val paymentDoc = PurchaseDoc(
            id = docId,
            type = "Supplier Payment",
            supplierId = supplierId,
            date = today,
            reference = billId,
            method = method,
            items = listOf(LineItem(description = "Payment made — $method", qty = 1.0, rate = amount)),
            status = "Paid",
            postings = listOf(
                JournalLine("Accounts Payable", amount, 0.0),
                JournalLine(paidFromAccount, 0.0, amount)
            )
        )
        purchaseDocs.value = purchaseDocs.value + paymentDoc

        // Post cash bank transaction
        val txn = CashBankTxn(
            id = "PMT-${System.currentTimeMillis() % 10000}",
            type = "Payment",
            accountId = cashBankAccounts.value.find { it.name.equals(paidFromAccount, ignoreCase = true) }?.id ?: "ACC-001",
            date = today,
            description = "Payment to ${supp.name} ${if (billId != null) "for $billId" else ""}",
            contraAccount = "Accounts Payable",
            amount = amount,
            reference = ref
        )
        cashBankTxns.value = cashBankTxns.value + txn

        MasRepository.postJournalEntry(
            JournalEntry(
                id = "JE-$docId",
                date = today,
                source = "Payments",
                description = "Supplier payment: ${supp.name}",
                reference = docId,
                lines = listOf(
                    JournalLine("Accounts Payable", amount, 0.0),
                    JournalLine(paidFromAccount, 0.0, amount)
                )
            )
        )
        showMessage("Payment of $amount made to ${supp.name}.")
    }

    // Cash & Bank Transfers
    fun transferFunds(fromAccountId: String, toAccountId: String, amount: Double, description: String, date: String) {
        val fromAcc = cashBankAccounts.value.find { it.id == fromAccountId } ?: return
        val toAcc = cashBankAccounts.value.find { it.id == toAccountId } ?: return
        val txnId = "TRF-${System.currentTimeMillis() % 10000}"

        val txn = CashBankTxn(
            id = txnId,
            type = "Transfer",
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            date = date,
            description = description.ifEmpty { "Transfer from ${fromAcc.name} to ${toAcc.name}" },
            amount = amount
        )
        cashBankTxns.value = cashBankTxns.value + txn

        // GL impact if crossing Cash vs Bank
        if (fromAcc.kind != toAcc.kind) {
            MasRepository.postJournalEntry(
                JournalEntry(
                    id = "JE-$txnId",
                    date = date,
                    source = "Cash & Bank",
                    description = "Fund transfer ${fromAcc.name} -> ${toAcc.name}",
                    lines = listOf(
                        JournalLine(toAcc.name, amount, 0.0),
                        JournalLine(fromAcc.name, 0.0, amount)
                    )
                )
            )
        }
        showMessage("Transfer of $amount completed.")
    }

    // Expense Actions
    fun addExpenseVoucher(voucher: ExpenseVoucher) {
        expenseVouchers.value = expenseVouchers.value + voucher
        if (voucher.status == "Posted") {
            val (debitAcc, creditAcc) = if (voucher.paymentType == "Cash") Pair(voucher.category, voucher.paidFrom ?: "Cash") else Pair(voucher.category, "Expenses Payable")
            MasRepository.postJournalEntry(
                JournalEntry(
                    id = "JE-${voucher.id}",
                    date = voucher.date,
                    source = "Expenses",
                    description = "Expense ${voucher.id} — ${voucher.category}",
                    reference = voucher.id,
                    lines = listOf(
                        JournalLine(debitAcc, voucher.amount, 0.0),
                        JournalLine(creditAcc, 0.0, voucher.amount)
                    )
                )
            )
        }
        showMessage("Expense voucher ${voucher.id} recorded.")
    }

    // Inventory Actions
    fun addStockItem(item: StockItem) {
        stockItems.value = stockItems.value + item
        MasRepository.logAudit("Inventory", "Create", item.id, "Added item ${item.name} (${item.sku})")
        showMessage("Item ${item.name} added.")
    }

    fun addStockMove(move: StockMove) {
        stockMoves.value = stockMoves.value + move
        MasRepository.logAudit("Inventory", "Move", move.id, "${move.type} of ${move.qty} on ${move.itemId}")
        showMessage("Stock movement ${move.type} recorded.")
    }

    // Manufacturing Actions
    fun addProductionOrder(order: ProductionOrder) {
        productionOrders.value = productionOrders.value + order
        showMessage("Production Order ${order.id} scheduled.")
    }

    fun recordMaterialConsumption(mc: MaterialConsumption) {
        consumptions.value = consumptions.value + mc
        // Auto decrease raw material
        val rm = rawMaterials.value.find { it.id == mc.rawMaterialId }
        if (rm != null) {
            rawMaterials.value = rawMaterials.value.map {
                if (it.id == rm.id) it.copy(openingStock = it.openingStock - mc.qty) else it
            }
        }
        showMessage("Material consumption recorded for ${mc.orderId}.")
    }

    fun recordProductionOutput(output: ProductionOutput) {
        productionOutputs.value = productionOutputs.value + output
        showMessage("Production output of ${output.qty} units recorded for ${output.orderId}.")
    }

    // Fixed Assets Actions
    fun addFixedAsset(asset: FixedAsset) {
        fixedAssets.value = fixedAssets.value + asset
        MasRepository.postJournalEntry(
            JournalEntry(
                id = "JE-${asset.id}",
                date = asset.purchaseDate,
                source = "Fixed Assets",
                description = "Purchase of ${asset.name}",
                reference = asset.id,
                lines = listOf(
                    JournalLine("Fixed Assets", asset.cost, 0.0),
                    JournalLine("Bank", 0.0, asset.cost)
                )
            )
        )
        showMessage("Fixed Asset ${asset.name} registered.")
    }

    fun runDepreciationBatch(): Pair<Int, Double> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())
        val activeAssets = fixedAssets.value.filter { it.status == "Active" }
        var count = 0
        var totalAmount = 0.0

        val newDepEntries = mutableListOf<AssetDepreciation>()
        activeAssets.forEach { asset ->
            val depAmount = when (asset.method) {
                "WDV" -> (asset.cost - assetDepreciations.value.filter { it.assetId == asset.id }.sumOf { it.amount }) * (1.0 / asset.usefulLifeYears) / 12.0
                "SYD" -> (asset.cost - asset.salvageValue) * 2.0 / (asset.usefulLifeYears * (asset.usefulLifeYears + 1))
                else -> (asset.cost - asset.salvageValue) / (asset.usefulLifeYears * 12.0)
            }
            if (depAmount > 1.0) {
                val depId = "DEP-${System.currentTimeMillis() % 10000 + count}"
                newDepEntries.add(AssetDepreciation(depId, asset.id, today, Math.round(depAmount * 100.0) / 100.0, "Monthly depreciation"))
                count++
                totalAmount += depAmount
            }
        }

        if (count > 0) {
            assetDepreciations.value = assetDepreciations.value + newDepEntries
            MasRepository.postJournalEntry(
                JournalEntry(
                    id = "JE-DEP-$today",
                    date = today,
                    source = "Fixed Assets",
                    description = "Depreciation run across $count asset(s)",
                    lines = listOf(
                        JournalLine("Depreciation Expense", totalAmount, 0.0),
                        JournalLine("Accumulated Depreciation", 0.0, totalAmount)
                    )
                )
            )
            showMessage("Depreciation posted for $count asset(s).")
        }
        return Pair(count, totalAmount)
    }

    // Period Closing Actions
    fun setPeriodStatus(month: String, status: String) {
        periodStatuses.value = periodStatuses.value.toMutableMap().apply { put(month, status) }
        MasRepository.logAudit("Period Closing", status, month, "Period $month set to $status")
        showMessage("Period $month is now $status.")
    }

    fun runYearEndClose(): Boolean {
        isFiscalYearClosed.value = true
        MasRepository.logAudit("Period Closing", "Year-End Close", "FY2026", "Fiscal Year 2026 closed into Retained Earnings")
        showMessage("Fiscal Year 2026 closed successfully.")
        return true
    }

    // Account creation & Opening balance update
    fun addAccount(account: Account) {
        accounts.value = accounts.value + account
        MasRepository.logAudit("Chart of Accounts", "Create", account.code, "Created account ${account.name}")
        showMessage("Account ${account.name} added.")
    }

    fun updateCashBankAccountOpening(accountId: String, newOpening: Double) {
        cashBankAccounts.value = cashBankAccounts.value.map {
            if (it.id == accountId) it.copy(openingBalance = newOpening) else it
        }
        val acc = cashBankAccounts.value.find { it.id == accountId }
        if (acc != null) {
            // Also update in General Ledger Chart of Accounts opening
            accounts.value = accounts.value.map {
                if (it.name.equals(acc.name, ignoreCase = true)) it.copy(opening = newOpening) else it
            }
            MasRepository.logAudit("Cash & Bank", "Update Opening", accountId, "Set opening balance of ${acc.name} to $newOpening")
            showMessage("Opening balance of ${acc.name} updated to Rs $newOpening.")
        }
    }

    // Party / Account Management & Excel/CSV Import
    fun getNextPartyCode(type: PartyAccountType): String {
        return MasRepository.getNextPartyCode(type)
    }

    fun savePartyAccount(account: PartyAccount, updateExisting: Boolean = false): Boolean {
        val success = MasRepository.savePartyAccount(account, updateExisting)
        if (success) {
            showMessage("${account.accountType.displayName} ${account.name} (${account.code}) saved successfully.")
        } else {
            showMessage("Account with code or name already exists in ${account.accountType.displayName}.")
        }
        return success
    }

    fun deletePartyAccount(id: String) {
        val success = MasRepository.deletePartyAccount(id)
        if (success) {
            showMessage("Account safely moved to Deleted Items folder.")
        }
    }

    fun deleteAccount(id: String) {
        val success = MasRepository.deleteAccount(id)
        if (success) {
            showMessage("Account moved to Deleted Items folder.")
        } else {
            showMessage("System accounts cannot be deleted.")
        }
    }

    fun deleteJournalEntry(id: String) {
        val success = MasRepository.deleteJournalEntry(id)
        if (success) {
            showMessage("Journal entry moved to Deleted Items folder.")
        }
    }

    fun deleteSalesDoc(id: String) {
        val success = MasRepository.deleteSalesDoc(id)
        if (success) {
            showMessage("Sales record moved to Deleted Items folder.")
        }
    }

    fun deletePurchaseDoc(id: String) {
        val success = MasRepository.deletePurchaseDoc(id)
        if (success) {
            showMessage("Purchase record moved to Deleted Items folder.")
        }
    }

    fun deleteExpense(id: String) {
        val success = MasRepository.deleteExpense(id)
        if (success) {
            showMessage("Expense voucher moved to Deleted Items folder.")
        }
    }

    fun deleteCashBankTxn(id: String) {
        val success = MasRepository.deleteCashBankTxn(id)
        if (success) {
            showMessage("Cash/Bank transaction moved to Deleted Items folder.")
        }
    }

    fun deleteCustomer(id: String) {
        val success = MasRepository.deleteCustomer(id)
        if (success) {
            showMessage("Customer moved to Deleted Items folder.")
        }
    }

    fun deleteSupplier(id: String) {
        val success = MasRepository.deleteSupplier(id)
        if (success) {
            showMessage("Supplier moved to Deleted Items folder.")
        }
    }

    fun deleteFixedAsset(id: String) {
        val success = MasRepository.deleteFixedAsset(id)
        if (success) {
            showMessage("Fixed asset moved to Deleted Items folder.")
        }
    }

    fun deleteStockItem(id: String) {
        val success = MasRepository.deleteStockItem(id)
        if (success) {
            showMessage("Stock item moved to Deleted Items folder.")
        }
    }

    fun restoreDeletedRecord(recordId: String) {
        val success = MasRepository.restoreDeletedRecord(recordId)
        if (success) {
            showMessage("Record successfully restored back to active accounts.")
        }
    }

    fun permanentlyDeleteRecord(recordId: String) {
        val success = MasRepository.permanentlyDeleteRecord(recordId)
        if (success) {
            showMessage("Record permanently deleted.")
        }
    }

    fun emptyDeletedRecords() {
        val count = MasRepository.emptyDeletedRecords()
        showMessage("Cleared $count items from Deleted Items folder.")
    }

    fun saveOpeningStockRecord(record: OpeningStockRecord, updateExisting: Boolean = false): Boolean {
        val success = MasRepository.saveOpeningStockRecord(record, updateExisting)
        if (success) {
            showMessage("Opening stock for ${record.itemName} (${record.factoryName}) saved.")
        }
        return success
    }

    fun deleteOpeningStockRecord(id: String) {
        val success = MasRepository.deleteOpeningStockRecord(id)
        if (success) {
            showMessage("Opening stock record moved to Deleted Items folder.")
        }
    }

    fun addCashBankAccount(account: CashBankAccount) {
        val current = MasRepository.cashBankAccounts.value.toMutableList()
        current.add(account)
        MasRepository.cashBankAccounts.value = current
        MasStorageManager.saveToPersistentStorage()
        showMessage("Account ${account.name} added successfully.")
    }

    fun addUser(user: AppUser) {
        val current = MasRepository.users.value.toMutableList()
        current.add(user)
        MasRepository.users.value = current
        MasStorageManager.saveToPersistentStorage()
        showMessage("User ${user.name} (${user.role}) added successfully.")
    }

    fun triggerCloudSync() {
        MasStorageManager.syncWithCloud()
        showMessage("Cloud sync completed. All data persisted.")
    }

    fun importPartyAccounts(rows: List<ImportedAccountRow>, duplicateStrategy: DuplicateStrategy) {
        val (imported, updated) = MasRepository.importPartyAccounts(rows, duplicateStrategy)
        showMessage("Import completed: $imported new accounts added, $updated updated.")
    }
}
