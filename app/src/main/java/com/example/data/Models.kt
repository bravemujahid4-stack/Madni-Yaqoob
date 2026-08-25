package com.example.data

// ============================================================================
// MAS ERP Core Data Models
// ============================================================================

enum class AccountType(val displayName: String, val normalBalance: String) {
    Assets("Assets", "Debit"),
    Liabilities("Liabilities", "Credit"),
    Equity("Equity", "Credit"),
    Revenue("Revenue", "Credit"),
    Expenses("Expenses", "Debit")
}

// 6 Dedicated Party / Account Types with exact Code Prefixes
enum class PartyAccountType(
    val displayName: String,
    val codePrefix: String,
    val defaultGlType: AccountType,
    val defaultNature: String,
    val defaultCategory: String
) {
    Owner("Owner", "OWN", AccountType.Equity, "Credit", "Owner Capital"),
    Investor("Investor", "INS", AccountType.Liabilities, "Credit", "Investor Account"),
    Factory("Factory", "FAC", AccountType.Expenses, "Debit", "Factory Operations"),
    LabourEmployee("Labour & Employee", "LAB", AccountType.Expenses, "Debit", "Labour & Salaries"),
    Customer("Customer", "CUS", AccountType.Assets, "Debit", "Accounts Receivable"),
    CashInHand("Cash In Hand", "CASH", AccountType.Assets, "Debit", "Cash in Hand");

    companion object {
        fun fromString(str: String?): PartyAccountType? {
            if (str.isNullOrBlank()) return null
            val s = str.trim()
            return when {
                s.equals("Owner", ignoreCase = true) || s.startsWith("OWN", ignoreCase = true) || s.contains("Owner", ignoreCase = true) || s.contains("Proprietor", ignoreCase = true) || s.contains("Partner", ignoreCase = true) -> Owner
                s.equals("Investor", ignoreCase = true) || s.startsWith("INS", ignoreCase = true) || s.startsWith("INV", ignoreCase = true) || s.contains("Investor", ignoreCase = true) || s.contains("Investment", ignoreCase = true) -> Investor
                s.equals("Factory", ignoreCase = true) || s.startsWith("FAC", ignoreCase = true) || s.contains("Factory", ignoreCase = true) || s.contains("Karkhana", ignoreCase = true) || s.contains("Mill", ignoreCase = true) || s.contains("Plant", ignoreCase = true) -> Factory
                s.equals("Labour & Employee", ignoreCase = true) || s.startsWith("LAB", ignoreCase = true) || s.contains("Labour", ignoreCase = true) || s.contains("Labor", ignoreCase = true) || s.contains("Employee", ignoreCase = true) || s.contains("Worker", ignoreCase = true) || s.contains("Staff", ignoreCase = true) || s.contains("Salary", ignoreCase = true) -> LabourEmployee
                s.equals("Customer", ignoreCase = true) || s.startsWith("CUS", ignoreCase = true) || s.contains("Customer", ignoreCase = true) || s.contains("Client", ignoreCase = true) || s.contains("Buyer", ignoreCase = true) || s.contains("Debtor", ignoreCase = true) -> Customer
                s.equals("Cash In Hand", ignoreCase = true) || s.startsWith("CASH", ignoreCase = true) || s.contains("Cash In Hand", ignoreCase = true) || s.contains("Cash", ignoreCase = true) || s.contains("Cashier", ignoreCase = true) || s.contains("Till", ignoreCase = true) || s.contains("Petty", ignoreCase = true) -> CashInHand
                else -> null
            }
        }
    }
}

data class PartyAccount(
    val id: String,
    val code: String,
    val name: String,
    val accountType: PartyAccountType,
    val openingBalance: Double = 0.0,
    val balanceType: String = if (accountType == PartyAccountType.Owner || accountType == PartyAccountType.Investor) "Credit" else "Debit", // Debit (Get) or Credit (Give)
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class ImportedAccountRow(
    val rowIndex: Int,
    val rawCode: String = "",
    val assignedCode: String = "",
    val name: String = "",
    val categoryString: String = "",
    val resolvedType: PartyAccountType? = null,
    val openingBalance: Double = 0.0,
    val balanceType: String = "Debit", // Debit (Get/Dr) or Credit (Give/Cr)
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val status: String = "Ready", // Ready, Duplicate, Invalid, Missing Name, Missing Category
    val duplicateReason: String? = null,
    val existingAccountId: String? = null
)

enum class DuplicateStrategy(val label: String) {
    UpdateExisting("Update Existing"),
    SkipDuplicates("Skip Duplicates")
}

data class ImportPreviewResult(
    val fileName: String,
    val totalRows: Int,
    val readyCount: Int,
    val duplicateCount: Int,
    val invalidCount: Int,
    val rows: List<ImportedAccountRow>
)

data class Account(
    val id: String,
    val code: String,
    val name: String,
    val type: AccountType,
    val category: String,
    val parentId: String? = null,
    val opening: Double = 0.0,
    val nature: String = if (type == AccountType.Assets || type == AccountType.Expenses) "Debit" else "Credit",
    val active: Boolean = true,
    val system: Boolean = false,
    val isCash: Boolean = name.contains("Cash", ignoreCase = true) || name.contains("Bank", ignoreCase = true),
    val cashFlowSection: String? = when (type) {
        AccountType.Assets -> if (name.contains("Fixed") || name.contains("Depreciation")) "Investing" else "Operating"
        AccountType.Liabilities -> if (name.contains("Loan")) "Financing" else "Operating"
        AccountType.Equity -> "Financing"
        else -> "Operating"
    }
)

data class JournalLine(
    val account: String,
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val branchId: String? = null,
    val departmentId: String? = null,
    val projectId: String? = null,
    val costCenterId: String? = null,
    val warehouseId: String? = null
)

data class JournalEntry(
    val id: String,
    val date: String,
    val source: String = "Manual",
    val description: String,
    val reference: String? = null,
    val attachment: String? = null,
    val branchId: String? = null,
    val departmentId: String? = null,
    val projectId: String? = null,
    val costCenterId: String? = null,
    val warehouseId: String? = null,
    val user: String = "Admin",
    val approvedBy: String? = null,
    val status: String = "Posted", // Posted, Pending Approval, Rejected, Voided
    val lines: List<JournalLine>
)

data class LineItem(
    val itemId: String? = null,
    val description: String,
    val qty: Double = 1.0,
    val rate: Double = 0.0
)

data class Customer(
    val id: String,
    val name: String,
    val type: String = "Wholesale",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val openingBalance: Double = 0.0,
    val creditLimit: Double = 100000.0,
    val paymentTerms: String = "Net 30",
    val receivableAccount: String = "Accounts Receivable",
    val branchId: String? = null
)

data class Supplier(
    val id: String,
    val name: String,
    val type: String = "Scrap Vendor",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val openingBalance: Double = 0.0,
    val creditTerms: String = "Net 30",
    val payableAccount: String = "Accounts Payable",
    val branchId: String? = null
)

data class SalesDoc(
    val id: String,
    val type: String, // Quotation, Sales Order, Sales Invoice, Sales Return, Credit Note, Customer Payment
    val saleType: String? = null, // Credit, Cash
    val customerId: String,
    val paymentAccount: String? = null, // Cash in Hand, Bank, etc.
    val date: String,
    val dueDate: String? = null,
    val items: List<LineItem>,
    val status: String = "Posted", // Sent, Confirmed, Posted, Credited, Received, Draft
    val reference: String? = null,
    val method: String? = null,
    val postings: List<JournalLine> = emptyList()
)

data class PurchaseDoc(
    val id: String,
    val type: String, // Purchase Order, Purchase Bill, Purchase Return, Debit Note, Supplier Payment
    val saleType: String? = null, // Credit, Cash
    val supplierId: String,
    val paymentAccount: String? = null, // Cash in Hand, Bank, etc.
    val date: String,
    val dueDate: String? = null,
    val items: List<LineItem>,
    val status: String = "Posted", // Confirmed, Posted, Debited, Paid, Draft
    val reference: String? = null,
    val method: String? = null,
    val postings: List<JournalLine> = emptyList()
)

data class CashBankAccount(
    val id: String,
    val name: String,
    val kind: String, // Cash, Bank
    val bankName: String? = null,
    val accountNumber: String? = null,
    val openingBalance: Double = 0.0
)

data class CashBankTxn(
    val id: String,
    val type: String, // Receipt, Payment, Transfer
    val accountId: String? = null,
    val fromAccountId: String? = null,
    val toAccountId: String? = null,
    val date: String,
    val description: String,
    val contraAccount: String = "Other Income",
    val amount: Double,
    val reference: String = ""
)

data class ExpenseVoucher(
    val id: String,
    val category: String,
    val paymentType: String, // Cash, Credit
    val date: String,
    val description: String,
    val amount: Double,
    val paidFrom: String? = null,
    val payee: String? = null,
    val dueDate: String? = null,
    val status: String = "Posted", // Posted, Paid, Pending Approval, Rejected
    val branchId: String? = null,
    val departmentId: String? = null,
    val projectId: String? = null,
    val postings: List<JournalLine> = emptyList()
)

data class RecurringExpense(
    val id: String,
    val category: String,
    val amount: Double,
    val frequency: String, // Weekly, Monthly
    val paymentType: String,
    val paidFrom: String?,
    val nextDueDate: String,
    val active: Boolean = true
)

data class StockItem(
    val id: String,
    val sku: String,
    val barcode: String? = null,
    val name: String,
    val category: String,
    val unit: String,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val costPrice: Double,
    val minStock: Double
)

data class StockMove(
    val id: String,
    val date: String,
    val itemId: String,
    val warehouseId: String?,
    val type: String, // Opening, In, Out, Transfer Out, Transfer In, Adjustment +, Adjustment -
    val qty: Double,
    val unitCost: Double? = null,
    val reference: String = ""
)

data class Warehouse(
    val id: String,
    val name: String,
    val location: String = "",
    val branchId: String? = null
)

data class RawMaterial(
    val id: String,
    val name: String,
    val unit: String = "Kg",
    val costPrice: Double,
    val openingStock: Double = 0.0
)

data class FinishedGood(
    val id: String,
    val name: String,
    val sku: String = "",
    val unit: String = "Pcs",
    val sellingPrice: Double
)

data class BOMComponent(
    val rawMaterialId: String,
    val qtyPerUnit: Double
)

data class BillOfMaterials(
    val id: String,
    val finishedGoodId: String,
    val name: String,
    val laborCostPerUnit: Double = 0.0,
    val components: List<BOMComponent>
)

data class ProductionOrder(
    val id: String,
    val date: String,
    val finishedGoodId: String,
    val bomId: String,
    val plannedQty: Double
)

data class MaterialConsumption(
    val id: String,
    val date: String,
    val orderId: String,
    val rawMaterialId: String,
    val qty: Double,
    val unitCost: Double,
    val reference: String = ""
)

data class ProductionOutput(
    val id: String,
    val date: String,
    val orderId: String,
    val qty: Double,
    val reference: String = ""
)

data class ManufacturingOverhead(
    val id: String,
    val date: String,
    val orderId: String,
    val description: String,
    val amount: Double
)

data class FixedAsset(
    val id: String,
    val name: String,
    val categoryId: String,
    val purchaseDate: String,
    val cost: Double,
    val usefulLifeYears: Int,
    val method: String = "SLM", // SLM, WDV, SYD
    val salvageValue: Double = 0.0,
    val location: String = "Head Office",
    val status: String = "Active" // Active, Disposed
)

data class AssetCategory(
    val id: String,
    val name: String,
    val method: String = "SLM",
    val usefulLifeYears: Int,
    val salvagePercent: Double = 10.0
)

data class AssetTransfer(
    val id: String,
    val assetId: String,
    val date: String,
    val fromLocation: String,
    val toLocation: String,
    val note: String = ""
)

data class AssetDepreciation(
    val id: String,
    val assetId: String,
    val date: String,
    val amount: Double,
    val note: String = ""
)

data class AssetDisposal(
    val id: String,
    val assetId: String,
    val date: String,
    val proceeds: Double,
    val bookValueAtDisposal: Double,
    val gainLoss: Double,
    val note: String = ""
)

data class Branch(
    val id: String,
    val name: String,
    val address: String = "",
    val active: Boolean = true
)

data class Department(
    val id: String,
    val name: String,
    val branchId: String? = null,
    val active: Boolean = true
)

data class Project(
    val id: String,
    val name: String,
    val active: Boolean = true
)

data class CostCenter(
    val id: String,
    val name: String,
    val active: Boolean = true
)

data class AppUser(
    val id: Int,
    val name: String,
    val email: String,
    val role: String, // Admin, Accountant, Manager, Sales User, Purchase User, Viewer
    val status: String = "Active"
)

data class AuditLogEntry(
    val id: String,
    val date: String,
    val time: String,
    val user: String,
    val module: String,
    val action: String,
    val transactionId: String = "",
    val details: String = ""
)

data class CompanyProfile(
    val name: String = "Mujahid Traders (Pvt) Ltd",
    val businessType: String = "Wholesale & Trading",
    val country: String = "Pakistan",
    val currency: String = "PKR",
    val address: String = "Plot 14, Site Area, Karachi, Pakistan",
    val phone: String = "+92 300 1234567",
    val email: String = "accounts@mujahidtraders.com",
    val fiscalYearStart: String = "January",
    val dateFormat: String = "DD/MM/YYYY",
    val invoicePrefix: String = "INV-",
    val invoiceStart: Int = 1001,
    val billPrefix: String = "BILL-",
    val billStart: Int = 1001
)

// ============================================================================
// Double-Entry Ledger & Factory Stock Models
// ============================================================================

data class AccountLedgerBalance(
    val accountCode: String,
    val accountName: String,
    val accountType: String,
    val normalNature: String = "Debit", // Debit or Credit
    val openingBalance: Double = 0.0,
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0,
    val currentBalance: Double = 0.0,
    val drCrIndicator: String = "Dr" // "Dr" or "Cr"
)

data class FactoryItemStock(
    val itemId: String,
    val itemName: String,
    val sku: String,
    val unit: String,
    val costPrice: Double,
    val quantity: Double,
    val totalValue: Double,
    val stockIn: Double = 0.0,
    val stockOut: Double = 0.0
)

data class FactoryStockRecord(
    val factoryId: String,
    val factoryName: String,
    val factoryCode: String,
    val totalQuantity: Double,
    val totalValue: Double,
    val items: List<FactoryItemStock>
)

data class DoubleEntryIntegrityCheck(
    val isBalanced: Boolean,
    val totalDebits: Double,
    val totalCredits: Double,
    val difference: Double,
    val imbalancedEntries: List<JournalEntry> = emptyList()
)

