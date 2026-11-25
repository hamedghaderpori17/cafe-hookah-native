package ir.cafehookah.ledger

import kotlinx.serialization.Serializable

@Serializable
data class InvoiceLineItem(
    val productName: String,
    val quantity: Int = 1,
    val amount: Long = 0L
)

@Serializable
data class Customer(
    val id: String,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val openingBalance: Long = 0L,
    val openingBalanceDate: String = "",
    val openingBalanceTime: String = "",
    val totalOrdersCount: Int = 0,
    val openDebt: Long = 0L,
    val debtByDate: Map<String, Long> = emptyMap(),
    val totalSpent: Long = 0L,
    val todayItemsCount: Int = 0
)

@Serializable
data class Product(
    val id: String,
    val name: String,
    val unitPrice: Long = 0L,
    val category: String = "hookah"
)

@Serializable
data class PurchaseRecord(
    val id: String,
    val recordType: String = "invoice",
    val customerId: String = "",
    val customerName: String = "مشتری عمومی",
    val productName: String = "",
    val quantity: Int = 1,
    val amount: Long = 0L,
    val paidAmount: Long = 0L,
    val items: List<InvoiceLineItem> = emptyList(),
    val status: String = "credit",
    val dateJalali: String = "",
    val timeJalali: String = ""
)

@Serializable
data class SupplierDebt(
    val id: String,
    val supplierName: String,
    val item: String = "",
    val phone: String = "",
    val note: String = "",
    val amount: Long = 0L,
    val settled: Boolean = false,
    val dateJalali: String = ""
)

@Serializable
data class ExpenseItem(
    val id: String,
    val title: String,
    val amount: Long = 0L,
    val dateJalali: String = "",
    val timeJalali: String = "",
    val note: String = ""
)

@Serializable
data class ShoppingItem(
    val id: String,
    val title: String,
    val checked: Boolean = false,
    val checkedAtMs: Long? = null,
    val createdAt: String = ""
)

@Serializable
data class AppPrefs(
    val customerDebtSort: String = "debt_desc",
    val selectedDebtDay: String = "ALL",
    val dailySalesOpen: Boolean = false,
    val darkTheme: Boolean = false
)

@Serializable
data class AuthDeviceState(
    val deviceId: String = "",
    val username: String = "Hamedqp",
    val trustDays: Int = 3,
    val trustedUntilMs: Long = 0L
)

@Serializable
data class CafeLedgerState(
    val customers: List<Customer> = emptyList(),
    val products: List<Product> = emptyList(),
    val purchases: List<PurchaseRecord> = emptyList(),
    val supplierDebts: List<SupplierDebt> = emptyList(),
    val expenses: List<ExpenseItem> = emptyList(),
    val shoppingList: List<ShoppingItem> = emptyList(),
    val cardBalance: Long = 0L,
    val assetAdditions: Long = 0L,
    val cashAdjustment: Long = 0L,
    val prefs: AppPrefs = AppPrefs(),
    val auth: AuthDeviceState = AuthDeviceState()
)
