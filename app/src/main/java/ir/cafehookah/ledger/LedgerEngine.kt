package ir.cafehookah.ledger

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.Calendar
import java.util.UUID

object LedgerEngine {
    private const val PREFS_NAME = "cafe_hookah_native_prefs_v1"
    private const val KEY_STATE_JSON = "cafe_ledger_state_json"
    private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
    private const val AUTH_USER = "Hamedqp"
    private const val AUTH_PASS_HASH = "3f6e4e089e311a4c6919fad685a1408b14aecf8c8a8cca3d8751e7ccdb6b22f0"

    val jalaliMonths = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    val jsonFormat = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toLatinDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                '۰', '٠' -> sb.append('0')
                '۱', '١' -> sb.append('1')
                '۲', '٢' -> sb.append('2')
                '۳', '٣' -> sb.append('3')
                '۴', '٤' -> sb.append('4')
                '۵', '٥' -> sb.append('5')
                '۶', '٦' -> sb.append('6')
                '۷', '٧' -> sb.append('7')
                '۸', '٨' -> sb.append('8')
                '۹', '٩' -> sb.append('9')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toPersianDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append('۰' + (ch - '0'))
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatPrice(amount: Long): String {
        val absStr = kotlin.math.abs(amount).toString()
        val grouped = StringBuilder()
        for (i in absStr.indices) {
            if (i > 0 && (absStr.length - i) % 3 == 0) {
                grouped.append(',')
            }
            grouped.append(absStr[i])
        }
        val sign = if (amount < 0) "-" else ""
        return "$sign${toPersianDigits(grouped.toString())} تومان"
    }

    fun parseDotNumber(raw: String): Long {
        val latin = toLatinDigits(raw).trim()
        if (latin.isEmpty() || latin == "-" || latin == "+") return 0L
        val negative = latin.startsWith("-")
        val digits = latin.filter { it in '0'..'9' }
        if (digits.isEmpty()) return 0L
        val parsed = digits.toLongOrNull() ?: 0L
        return if (negative) -parsed else parsed
    }

    fun formatDotThousands(raw: String, allowNegative: Boolean = false): String {
        val latin = toLatinDigits(raw)
        val hasMinus = allowNegative && latin.trimStart().startsWith("-")
        val digits = latin.filter { it in '0'..'9' }
        if (digits.isEmpty()) return if (hasMinus) "-" else ""
        val sb = StringBuilder()
        for (i in digits.indices) {
            if (i > 0 && (digits.length - i) % 3 == 0) {
                sb.append('.')
            }
            sb.append(digits[i])
        }
        return if (hasMinus) "-$sb" else sb.toString()
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        var year = gy
        var jy = if (year > 1600) 979 else 0
        year -= if (year > 1600) 1600 else 621
        val gy2 = if (gm > 2) year + 1 else year
        var days = (365 * year) +
            ((gy2 + 3) / 4) -
            ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) -
            80 +
            gd +
            gDaysInMonth[(gm - 1).coerceIn(0, 11)]
        jy += 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }
        return Triple(jy, jm, jd)
    }

    fun formatJalali(y: Int, m: Int, d: Int): String {
        val yy = y.toString().padStart(4, '0')
        val mm = m.toString().padStart(2, '0')
        val dd = d.toString().padStart(2, '0')
        return toPersianDigits("$yy/$mm/$dd")
    }

    fun getTodayJalali(): String {
        val cal = Calendar.getInstance()
        val (y, m, d) = gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        return formatJalali(y, m, d)
    }

    fun getNowTime(includeSeconds: Boolean = false): String {
        val cal = Calendar.getInstance()
        val hh = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val mm = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
        val ss = cal.get(Calendar.SECOND).toString().padStart(2, '0')
        return toPersianDigits(if (includeSeconds) "$hh:$mm:$ss" else "$hh:$mm")
    }

    fun parseJalaliParts(dateStr: String): Triple<Int, Int, Int> {
        val clean = toLatinDigits(dateStr.ifBlank { getTodayJalali() }).trim()
        val parts = clean.split("/").mapNotNull { it.toIntOrNull() }
        if (parts.size == 3) {
            return Triple(
                if (parts[0] > 0) parts[0] else 1405,
                parts[1].coerceIn(1, 12),
                parts[2].coerceIn(1, 31)
            )
        }
        val todayClean = toLatinDigits(getTodayJalali())
        val tParts = todayClean.split("/").mapNotNull { it.toIntOrNull() }
        return Triple(
            tParts.getOrElse(0) { 1405 },
            tParts.getOrElse(1) { 9 },
            tParts.getOrElse(2) { 1 }
        )
    }

    fun isJalaliLeap(year: Int): Boolean {
        val mod = ((year - 474) % 128 + 128) % 128
        val leapMods = setOf(1, 5, 9, 13, 17, 22, 26, 30, 34, 38, 43, 47, 51, 55, 59, 63, 67, 72, 76, 80, 84, 88, 92, 97, 101, 105, 109, 113, 117, 122, 126)
        return mod in leapMods
    }

    fun getJalaliMonthDays(year: Int, month: Int): Int {
        return when (month) {
            in 1..6 -> 31
            in 7..11 -> 30
            else -> if (isJalaliLeap(year)) 30 else 29
        }
    }

    fun buildRecent30Days(todayJalali: String = getTodayJalali()): List<String> {
        var (y, m, d) = parseJalaliParts(todayJalali)
        val result = mutableListOf<String>()
        for (i in 0 until 30) {
            result.add(formatJalali(y, m, d))
            d -= 1
            if (d < 1) {
                m -= 1
                if (m < 1) {
                    m = 12
                    y -= 1
                }
                d = getJalaliMonthDays(y, m)
            }
        }
        return result.reversed()
    }

    fun sha256Hex(plainText: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(plainText.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyCredentials(username: String, password: String): Boolean {
        return username.trim() == AUTH_USER && sha256Hex(password) == AUTH_PASS_HASH
    }

    fun isTrustedDeviceValid(auth: AuthDeviceState, nowMs: Long = System.currentTimeMillis()): Boolean {
        return auth.username == AUTH_USER && auth.trustedUntilMs > nowMs
    }

    fun ensureDeviceId(auth: AuthDeviceState): AuthDeviceState {
        if (auth.deviceId.length >= 8) return auth
        return auth.copy(deviceId = "dev-${UUID.randomUUID().toString().take(12)}")
    }

    fun defaultState(today: String = getTodayJalali()): CafeLedgerState {
        val c1 = Customer(
            id = "cust-1",
            name = "آرش کریمی",
            phone = "09120000001",
            openingBalance = 0L,
            openingBalanceDate = today,
            totalOrdersCount = 2,
            openDebt = 360000L,
            debtByDate = mapOf(today to 360000L),
            totalSpent = 360000L,
            todayItemsCount = 2
        )
        val c2 = Customer(
            id = "cust-2",
            name = "مشتری عمومی",
            phone = "",
            openingBalance = 0L,
            openingBalanceDate = today
        )
        val c3 = Customer(
            id = "cust-3",
            name = "سارا رضایی",
            phone = "09120000002",
            openingBalance = 0L,
            openingBalanceDate = today,
            totalOrdersCount = 1,
            openDebt = 0L,
            totalSpent = 180000L,
            todayItemsCount = 1
        )
        val products = listOf(
            Product(id = "prod-1", name = "قلیون لیمو نعناع", unitPrice = 180000L, category = "hookah"),
            Product(id = "prod-2", name = "ذغال", unitPrice = 2500000L, category = "supply")
        )
        val purchases = listOf(
            PurchaseRecord(
                id = "pur-1",
                recordType = "invoice",
                customerId = "cust-3",
                customerName = "سارا رضایی",
                productName = "قلیون لیمو نعناع",
                quantity = 1,
                amount = 180000L,
                paidAmount = 180000L,
                items = listOf(InvoiceLineItem("قلیون لیمو نعناع", 1, 180000L)),
                status = "paid",
                dateJalali = today
            ),
            PurchaseRecord(
                id = "pur-2",
                recordType = "invoice",
                customerId = "cust-1",
                customerName = "آرش کریمی",
                productName = "قلیون لیمو نعناع",
                quantity = 2,
                amount = 360000L,
                paidAmount = 0L,
                items = listOf(InvoiceLineItem("قلیون لیمو نعناع", 2, 360000L)),
                status = "credit",
                dateJalali = today
            )
        )
        val suppliers = listOf(
            SupplierDebt(
                id = "supp-1",
                supplierName = "تأمین‌کننده ذغال",
                item = "ذغال",
                note = "بدون توضیح",
                amount = 2500000L,
                settled = false,
                dateJalali = today
            )
        )
        return CafeLedgerState(
            customers = listOf(c1, c2, c3),
            products = products,
            purchases = purchases,
            supplierDebts = suppliers,
            auth = AuthDeviceState(deviceId = "dev-${UUID.randomUUID().toString().take(12)}")
        )
    }

    fun purchaseTotalAmount(record: PurchaseRecord): Long {
        if (record.recordType == "payment") return 0L
        val itemsSum = record.items.sumOf { it.amount }
        return if (itemsSum > 0L) itemsSum else record.amount
    }

    fun purchaseTotalQty(record: PurchaseRecord): Int {
        if (record.recordType == "payment") return 0
        return if (record.items.isNotEmpty()) record.items.sumOf { it.quantity.coerceAtLeast(1) } else record.quantity.coerceAtLeast(1)
    }

    fun purchasePaidAmount(record: PurchaseRecord): Long {
        if (record.recordType == "payment") return record.paidAmount.coerceAtLeast(0L)
        val total = purchaseTotalAmount(record)
        return record.paidAmount.coerceIn(0L, total.coerceAtLeast(0L))
    }

    fun purchaseRemainingAmount(record: PurchaseRecord): Long {
        if (record.recordType == "payment") return 0L
        return (purchaseTotalAmount(record) - purchasePaidAmount(record)).coerceAtLeast(0L)
    }

    fun matchesCustomer(record: PurchaseRecord, customer: Customer): Boolean {
        if (record.customerId.isNotBlank() && record.customerId == customer.id) return true
        return record.customerName.trim() == customer.name.trim()
    }

    fun cleanupExpiredShopping(items: List<ShoppingItem>, nowMs: Long = System.currentTimeMillis()): List<ShoppingItem> {
        return items.filter { item ->
            if (!item.checked) true
            else {
                val checkedAt = item.checkedAtMs ?: return@filter true
                (nowMs - checkedAt) < ONE_DAY_MS
            }
        }
    }

    fun recalculateState(rawState: CafeLedgerState, todayJalali: String = getTodayJalali()): CafeLedgerState {
        val cleanedShopping = cleanupExpiredShopping(rawState.shoppingList)
        val customersMutable = rawState.customers.toMutableList()

        for (p in rawState.purchases) {
            val pName = p.customerName.trim().ifEmpty { "مشتری عمومی" }
            val exists = customersMutable.any { it.id == p.customerId || it.name.trim() == pName }
            if (!exists) {
                customersMutable.add(
                    Customer(
                        id = p.customerId.ifBlank { "cust-${UUID.randomUUID().toString().take(8)}" },
                        name = pName,
                        openingBalanceDate = todayJalali
                    )
                )
            }
        }

        val updatedCustomers = customersMutable.map { cust ->
            val custPurchases = rawState.purchases.filter { matchesCustomer(it, cust) }
            val debtMap = mutableMapOf<String, Long>()
            val opDate = toPersianDigits(cust.openingBalanceDate.ifBlank { todayJalali })
            if (cust.openingBalance != 0L) {
                debtMap[opDate] = cust.openingBalance
            }

            var ordersCount = 0
            var invoiceSpent = 0L
            var todayItems = 0

            for (p in custPurchases) {
                val dKey = toPersianDigits(p.dateJalali.ifBlank { todayJalali })
                if (p.recordType == "payment") {
                    val payAmt = p.paidAmount.coerceAtLeast(0L)
                    if (payAmt > 0L) {
                        debtMap[dKey] = (debtMap[dKey] ?: 0L) - payAmt
                    }
                    continue
                }
                val qty = purchaseTotalQty(p)
                val amt = purchaseTotalAmount(p)
                ordersCount += qty
                invoiceSpent += amt
                if (dKey == toPersianDigits(todayJalali)) {
                    todayItems += qty
                }
                val rem = purchaseRemainingAmount(p)
                if (rem > 0L) {
                    debtMap[dKey] = (debtMap[dKey] ?: 0L) + rem
                }
            }

            val nonZeroDebtMap = debtMap.filterValues { it != 0L }
            val totalOpenDebt = nonZeroDebtMap.values.sum()

            cust.copy(
                openingBalanceDate = opDate,
                debtByDate = nonZeroDebtMap,
                openDebt = totalOpenDebt,
                totalOrdersCount = ordersCount,
                totalSpent = maxOf(0L, invoiceSpent + maxOf(0L, cust.openingBalance), totalOpenDebt),
                todayItemsCount = todayItems
            )
        }

        return rawState.copy(
            customers = updatedCustomers,
            shoppingList = cleanedShopping,
            auth = ensureDeviceId(rawState.auth)
        )
    }

    fun cashVaultBalance(state: CafeLedgerState): Long {
        val totalReceived = state.purchases.sumOf { purchasePaidAmount(it) }
        val totalExpenses = state.expenses.sumOf { it.amount }
        return totalReceived - totalExpenses + state.cashAdjustment
    }

    fun totalVaultBalance(state: CafeLedgerState): Long {
        return cashVaultBalance(state) + state.cardBalance
    }

    fun totalAssets(state: CafeLedgerState): Long {
        return totalVaultBalance(state) + state.assetAdditions
    }

    fun totalCustomerReceivables(state: CafeLedgerState): Long {
        return state.customers.sumOf { if (it.openDebt > 0L) it.openDebt else 0L }
    }

    fun totalSupplierPayables(state: CafeLedgerState): Long {
        return state.supplierDebts.filter { !it.settled }.sumOf { it.amount.coerceAtLeast(0L) }
    }

    fun netCapital(state: CafeLedgerState): Long {
        return totalAssets(state) + totalCustomerReceivables(state) - totalSupplierPayables(state)
    }

    fun loadState(context: Context): CafeLedgerState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_STATE_JSON, null) ?: return recalculateState(defaultState())
        return try {
            val decoded = jsonFormat.decodeFromString<CafeLedgerState>(rawJson)
            recalculateState(decoded)
        } catch (_: Exception) {
            recalculateState(defaultState())
        }
    }

    fun saveState(context: Context, state: CafeLedgerState): CafeLedgerState {
        val recalculated = recalculateState(state)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_STATE_JSON, jsonFormat.encodeToString(recalculated)).apply()
        return recalculated
    }

    fun exportStateToJsonString(state: CafeLedgerState): String {
        return jsonFormat.encodeToString(recalculateState(state))
    }

    fun importStateFromJsonString(context: Context, jsonText: String, currentState: CafeLedgerState): CafeLedgerState? {
        return try {
            val imported = jsonFormat.decodeFromString<CafeLedgerState>(jsonText)
            val mergedCustomers = (currentState.customers.associateBy { it.id } + imported.customers.associateBy { it.id }).values.toList()
            val mergedProducts = (currentState.products.associateBy { it.id } + imported.products.associateBy { it.id }).values.toList()
            val mergedPurchases = (currentState.purchases.associateBy { it.id } + imported.purchases.associateBy { it.id }).values.toList()
            val mergedSuppliers = (currentState.supplierDebts.associateBy { it.id } + imported.supplierDebts.associateBy { it.id }).values.toList()
            val mergedExpenses = (currentState.expenses.associateBy { it.id } + imported.expenses.associateBy { it.id }).values.toList()
            val mergedShopping = cleanupExpiredShopping(
                (currentState.shoppingList.associateBy { it.id } + imported.shoppingList.associateBy { it.id }).values.toList()
            )
            val combined = currentState.copy(
                customers = mergedCustomers,
                products = mergedProducts,
                purchases = mergedPurchases,
                supplierDebts = mergedSuppliers,
                expenses = mergedExpenses,
                shoppingList = mergedShopping,
                cardBalance = if (imported.cardBalance != 0L) imported.cardBalance else currentState.cardBalance,
                assetAdditions = if (imported.assetAdditions != 0L) imported.assetAdditions else currentState.assetAdditions,
                cashAdjustment = if (imported.cashAdjustment != 0L) imported.cashAdjustment else currentState.cashAdjustment,
                prefs = imported.prefs
            )
            saveState(context, combined)
        } catch (_: Exception) {
            null
        }
    }
}
