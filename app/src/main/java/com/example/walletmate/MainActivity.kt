package com.example.walletmate

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.walletmate.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var storageHelper: StorageHelper
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    private val budgetSetupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            updateUI()
            Toast.makeText(this, "Budget updated successfully", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storageHelper = StorageHelper(this)

        binding.btnAddFunds.setOnClickListener {
            startActivity(Intent(this, AddFundsActivity::class.java))
        }

        binding.btnMakePayment.setOnClickListener {
            startActivity(Intent(this, MakePaymentActivity::class.java))
        }

        binding.btnViewTransactions.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }

        binding.btnSetBudget.setOnClickListener {
            val intent = Intent(this, MonthlyBudgetSetupActivity::class.java)
            budgetSetupLauncher.launch(intent)
        }

        // Set up bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Already on home, do nothing
                    true
                }
                R.id.navigation_add_funds -> {
                    startActivity(Intent(this, AddFundsActivity::class.java))
                    true
                }
                R.id.navigation_make_payment -> {
                    startActivity(Intent(this, MakePaymentActivity::class.java))
                    true
                }
                R.id.navigation_transactions -> {
                    startActivity(Intent(this, TransactionHistoryActivity::class.java))
                    true
                }
                R.id.navigation_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        observeTransactions()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun observeTransactions() {
        lifecycleScope.launch {
            storageHelper.getTransactions().collectLatest { transactions ->
                updateUI()
            }
        }
    }

    private fun updateUI() {
        // Update balance with currency symbol
        val balance = storageHelper.getBalance()
        val currencySymbol = storageHelper.getCurrencySymbol()
        binding.tvBalance.text = String.format("%s %.2f", currencySymbol, balance)

        // Update budget and spending
        val budget = storageHelper.getMonthlyBudget()
        binding.tvBudget.text = if (budget > 0) {
            String.format("Budget: %s %.2f", currencySymbol, budget)
        } else {
            "Budget: Not Set"
        }

        lifecycleScope.launch {
            val monthlyExpenses = storageHelper.getMonthlyExpenses()
            binding.tvMonthlySpent.text = String.format("Spent: %s %.2f", currencySymbol, monthlyExpenses)

            val progress = if (budget > 0) {
                ((monthlyExpenses / budget) * 100).toInt().coerceAtMost(100)
            } else {
                0
            }
            binding.progressBudget.progress = progress

            val daysLeft = storageHelper.getDaysLeftInMonth()
            binding.tvDaysLeft.text = if (daysLeft > 0) {
                "$daysLeft days left"
            } else {
                "-"
            }

            val moneyLeft = storageHelper.getMoneyLeft()
            binding.tvBudgetWarning.text = when {
                budget > 0 && moneyLeft <= 0 -> "Budget Exceeded!"
                budget > 0 && monthlyExpenses >= budget * 0.8 -> "Nearing Budget Limit!"
                else -> ""
            }
        }
    }
}