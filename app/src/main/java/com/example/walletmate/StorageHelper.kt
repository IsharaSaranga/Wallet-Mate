package com.example.walletmate

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class StorageHelper(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("WalletMatePrefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(context)
    private val transactionDao = database.transactionDao()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val transactionsFile = java.io.File(context.filesDir, "transactions.json")
    private val backupFile = java.io.File(context.filesDir, "backup.json")

    companion object {
        private const val KEY_BUDGET_AMOUNT = "budget_amount"
        private const val KEY_BUDGET_RESET_DATE = "budget_reset_date"
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
    }

    // Currency symbol management
    fun setCurrencySymbol(symbol: String) {
        prefs.edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply()
    }

    fun getCurrencySymbol(): String {
        return prefs.getString(KEY_CURRENCY_SYMBOL, "Rs.") ?: "Rs."
    }

    fun getBalance(): Double {
        return prefs.getFloat("balance", 0f).toDouble()
    }

    fun setBalance(amount: Double) {
        prefs.edit().putFloat("balance", amount.toFloat()).apply()
    }

    fun addBalance(amount: Double) {
        val currentBalance = getBalance()
        val newBalance = currentBalance + amount
        setBalance(newBalance)
    }

    suspend fun saveTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(id: String, updatedTransaction: Transaction) {
        val oldTransaction = transactionDao.getTransactionById(id)
        if (oldTransaction != null) {
            val balanceAdjustment = when {
                oldTransaction.type == "Add Funds" && updatedTransaction.type == "Add Funds" -> 
                    updatedTransaction.amount - oldTransaction.amount
                oldTransaction.type == "Add Funds" && updatedTransaction.type == "Payment" -> 
                    -oldTransaction.amount - updatedTransaction.amount
                oldTransaction.type == "Payment" && updatedTransaction.type == "Add Funds" -> 
                    oldTransaction.amount + updatedTransaction.amount
                oldTransaction.type == "Payment" && updatedTransaction.type == "Payment" -> 
                    oldTransaction.amount - updatedTransaction.amount
                else -> 0.0
            }
            addBalance(balanceAdjustment)
            transactionDao.updateTransaction(updatedTransaction)
        }
    }

    suspend fun deleteTransaction(id: String) {
        val transaction = transactionDao.getTransactionById(id)
        if (transaction != null) {
            val balanceAdjustment = if (transaction.type == "Add Funds") -transaction.amount else transaction.amount
            addBalance(balanceAdjustment)
            transactionDao.deleteTransaction(transaction)
        }
    }

    fun getTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions()
    }

    fun getCategorySummary(): Flow<Map<String, Pair<Double, Double>>> {
        return transactionDao.getAllTransactions().map { transactions ->
            val summary = mutableMapOf<String, Pair<Double, Double>>()
            transactions.forEach { transaction ->
                val category = transaction.category
                val (currentIncome, currentExpense) = summary.getOrDefault(category, 0.0 to 0.0)
                val newIncome = if (transaction.type == "Add Funds") currentIncome + transaction.amount else currentIncome
                val newExpense = if (transaction.type == "Payment") currentExpense + transaction.amount else currentExpense
                summary[category] = newIncome to newExpense
            }
            summary
        }
    }

    fun getAllCategories(): Flow<List<String>> {
        return transactionDao.getAllCategories()
    }

    suspend fun setMonthlyBudget(amount: Double, resetDate: Date) {
        val editor = prefs.edit()
        editor.putFloat(KEY_BUDGET_AMOUNT, amount.toFloat())
        editor.putLong(KEY_BUDGET_RESET_DATE, resetDate.time)
        editor.apply()

        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            title = "Monthly Budget",
            type = "Add Funds",
            amount = amount,
            category = "Salary",
            date = Date(),
            recipient = null
        )
        addBalance(amount)
        saveTransaction(transaction)
    }

    fun getMonthlyBudget(): Double {
        return prefs.getFloat(KEY_BUDGET_AMOUNT, 0f).toDouble()
    }

    fun getBudgetResetDate(): Date {
        val timestamp = prefs.getLong(KEY_BUDGET_RESET_DATE, 0L)
        return if (timestamp > 0) Date(timestamp) else Date()
    }

    fun getDaysLeftInMonth(): Int {
        val resetDate = getBudgetResetDate()
        val calendar = java.util.Calendar.getInstance()
        val currentDate = Date()
        if (currentDate.after(resetDate)) return 0
        val diff = resetDate.time - currentDate.time
        return (diff / (1000 * 60 * 60 * 24)).toInt() + 1
    }

    suspend fun getMonthlyExpenses(): Double {
        val transactions = getTransactions().first()
        val calendar = java.util.Calendar.getInstance()
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        return transactions
            .filter { it.type == "Payment" }
            .filter {
                calendar.time = it.date
                calendar.get(java.util.Calendar.MONTH) == currentMonth && calendar.get(java.util.Calendar.YEAR) == currentYear
            }
            .sumOf { it.amount }
    }

    suspend fun getMoneyLeft(): Double {
        val budget = getMonthlyBudget()
        val expenses = getMonthlyExpenses()
        return budget - expenses
    }

    // Reset all app data
    suspend fun resetAllData() {
        // Clear all transactions
        transactionDao.deleteAllTransactions()
        
        // Reset balance
        setBalance(0.0)
        
        // Reset budget
        val editor = prefs.edit()
        editor.putFloat(KEY_BUDGET_AMOUNT, 0f)
        editor.putLong(KEY_BUDGET_RESET_DATE, 0L)
        editor.apply()
        
        // Delete backup files
        transactionsFile.delete()
        backupFile.delete()
    }

    // Backup data to JSON
    suspend fun backupData(): Boolean {
        return try {
            val transactions = getTransactions().first()
            val transactionsArray = org.json.JSONArray()
            transactions.forEach { transaction ->
                val transactionJson = org.json.JSONObject().apply {
                    put("id", transaction.id)
                    put("title", transaction.title)
                    put("type", transaction.type)
                    put("amount", transaction.amount)
                    put("category", transaction.category)
                    put("date", transaction.date.time)
                    put("recipient", transaction.recipient)
                }
                transactionsArray.put(transactionJson)
            }

            val backupJson = org.json.JSONObject().apply {
                put("balance", getBalance())
                put("budget", getMonthlyBudget())
                put("resetDate", getBudgetResetDate().time)
                put("currencySymbol", getCurrencySymbol())
                put("transactions", transactionsArray)
            }
            backupFile.writeText(backupJson.toString())
            true
        } catch (e: Exception) {
            Log.e("StorageHelper", "Backup failed", e)
            false
        }
    }

    // Restore data from JSON
    suspend fun restoreData(): Boolean {
        return try {
            if (!backupFile.exists()) {
                Log.e("StorageHelper", "Backup file not found")
                return false
            }

            val backupJson = org.json.JSONObject(backupFile.readText())
            
            // Validate backup data
            if (!backupJson.has("balance") || !backupJson.has("budget") || 
                !backupJson.has("resetDate") || !backupJson.has("currencySymbol") || 
                !backupJson.has("transactions")) {
                Log.e("StorageHelper", "Invalid backup data format")
                return false
            }

            // Clear existing data
            resetAllData()

            // Restore preferences
            val editor = prefs.edit()
            editor.putFloat("balance", backupJson.optDouble("balance", 0.0).toFloat())
            editor.putFloat(KEY_BUDGET_AMOUNT, backupJson.optDouble("budget", 0.0).toFloat())
            editor.putLong(KEY_BUDGET_RESET_DATE, backupJson.optLong("resetDate", 0L))
            editor.putString(KEY_CURRENCY_SYMBOL, backupJson.optString("currencySymbol", "Rs."))
            editor.apply()

            // Restore transactions
            val transactionsArray = backupJson.optJSONArray("transactions") ?: org.json.JSONArray()
            for (i in 0 until transactionsArray.length()) {
                val transactionJson = transactionsArray.getJSONObject(i)
                val transaction = Transaction(
                    id = transactionJson.getString("id"),
                    title = transactionJson.getString("title"),
                    type = transactionJson.getString("type"),
                    amount = transactionJson.getDouble("amount"),
                    category = transactionJson.getString("category"),
                    date = Date(transactionJson.getLong("date")),
                    recipient = if (transactionJson.has("recipient")) transactionJson.getString("recipient") else null
                )
                saveTransaction(transaction)
            }

            true
        } catch (e: Exception) {
            Log.e("StorageHelper", "Restore failed: ${e.message}", e)
            false
        }
    }

    private fun getTransactionsJson(): org.json.JSONArray {
        return try {
            if (transactionsFile.exists()) {
                org.json.JSONArray(transactionsFile.readText())
            } else {
                org.json.JSONArray()
            }
        } catch (e: Exception) {
            Log.e("StorageHelper", "Failed to read transactions.json", e)
            org.json.JSONArray()
        }
    }
}