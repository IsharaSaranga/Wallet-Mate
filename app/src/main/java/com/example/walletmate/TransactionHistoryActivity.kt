package com.example.walletmate

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.walletmate.databinding.ActivityTransactionHistoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class TransactionHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransactionHistoryBinding
    private lateinit var storageHelper: StorageHelper
    private lateinit var adapter: TransactionAdapter
    private val EDIT_REQUEST_CODE = 100
    private var allTransactions: List<Transaction> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_add_funds -> {
                    startActivity(Intent(this, AddFundsActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_make_payment -> {
                    startActivity(Intent(this, MakePaymentActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_transactions -> {
                    // Already on transactions, do nothing
                    true
                }
                R.id.navigation_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        // Set the selected item
        binding.bottomNavigation.selectedItemId = R.id.navigation_transactions

        storageHelper = StorageHelper(this)

        // Set up RecyclerView
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        observeTransactions()

        // Set up filter dropdown
        setupFilterSpinner()

        // Show category summary button
        binding.btnShowSummary.setOnClickListener {
            showCategorySummary()
        }

        // Update budget progress
        observeBudgetProgress()
    }

    private fun setupFilterSpinner() {
        lifecycleScope.launch {
            storageHelper.getAllCategories().collectLatest { categories ->
                val filterOptions = mutableListOf("All", "Income", "Payments")
                filterOptions.addAll(categories)
                val adapter = ArrayAdapter(this@TransactionHistoryActivity, android.R.layout.simple_spinner_item, filterOptions)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerFilter.adapter = adapter

                binding.spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        val selectedFilter = filterOptions[position]
                        val filteredTransactions = when (selectedFilter) {
                            "All" -> allTransactions
                            "Income" -> allTransactions.filter { it.type == "Add Funds" }
                            "Payments" -> allTransactions.filter { it.type == "Payment" }
                            else -> allTransactions.filter { it.category == selectedFilter }
                        }
                        updateTransactionList(filteredTransactions)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                        updateTransactionList(allTransactions)
                    }
                }
            }
        }
    }

    private fun observeTransactions() {
        lifecycleScope.launch {
            storageHelper.getTransactions().collectLatest { transactions ->
                allTransactions = transactions
                updateTransactionList(transactions)
            }
        }
    }

    private fun updateTransactionList(transactions: List<Transaction>) {
        if (transactions.isEmpty()) {
            binding.tvEmptyMessage.visibility = View.VISIBLE
            binding.rvTransactions.visibility = View.GONE
        } else {
            binding.tvEmptyMessage.visibility = View.GONE
            binding.rvTransactions.visibility = View.VISIBLE
            adapter = TransactionAdapter(
                transactions,
                onEditClick = { id, transaction ->
                    val intent = Intent(this, EditTransactionActivity::class.java).apply {
                        putExtra("TRANSACTION_ID", id)
                        putExtra("TRANSACTION", transaction)
                    }
                    startActivityForResult(intent, EDIT_REQUEST_CODE)
                },
                onDeleteClick = { id ->
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Delete Transaction")
                        .setMessage("Are you sure you want to delete this transaction?")
                        .setPositiveButton("Delete") { _, _ ->
                            lifecycleScope.launch {
                                storageHelper.deleteTransaction(id)
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            )
            binding.rvTransactions.adapter = adapter
        }
    }

    private fun showCategorySummary() {
        lifecycleScope.launch {
            storageHelper.getCategorySummary().collectLatest { summary ->
                val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
                val summaryText = summary.entries.joinToString("\n\n") { (category, pair) ->
                    val (income, expense) = pair
                    "$category:\nIncome: ${currencyFormat.format(income)}\nExpense: ${currencyFormat.format(expense)}"
                }
                MaterialAlertDialogBuilder(this@TransactionHistoryActivity)
                    .setTitle("Category-wise Summary")
                    .setMessage(summaryText.takeIf { summary.isNotEmpty() } ?: "No transactions available")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun observeBudgetProgress() {
        lifecycleScope.launch {
            storageHelper.getTransactions().collectLatest { transactions ->
                val budget = storageHelper.getMonthlyBudget()
                val expenses = storageHelper.getMonthlyExpenses()
                val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
                if (budget > 0) {
                    val spentPercentage = (expenses / budget * 100).toInt()
                    binding.tvBudgetProgress.text = "Spent: ${currencyFormat.format(expenses)} of ${currencyFormat.format(budget)} ($spentPercentage%)"
                    binding.tvBudgetProgress.setTextColor(getColor(R.color.black))
                    binding.tvBudgetWarning.text = when {
                        spentPercentage >= 100 -> "Warning: Budget Exceeded!"
                        spentPercentage >= 80 -> "Warning: Nearing Budget Limit!"
                        else -> ""
                    }
                    binding.tvBudgetWarning.setTextColor(getColor(R.color.red))
                } else {
                    binding.tvBudgetProgress.text = "No budget set"
                    binding.tvBudgetWarning.text = ""
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_REQUEST_CODE && resultCode == RESULT_OK) {
            observeTransactions()
        }
    }
}