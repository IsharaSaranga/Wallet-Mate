package com.example.walletmate

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.walletmate.databinding.ActivityMonthlyBudgetSetupBinding
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class MonthlyBudgetSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMonthlyBudgetSetupBinding
    private lateinit var storageHelper: StorageHelper
    private var selectedResetDate: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonthlyBudgetSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storageHelper = StorageHelper(this)

        // Pre-fill current budget and reset date
        val currentBudget = storageHelper.getMonthlyBudget()
        if (currentBudget > 0) {
            binding.etBudget.setText(currentBudget.toString())
        }
        val currentResetDate = storageHelper.getBudgetResetDate()
        if (currentResetDate != null) {
            selectedResetDate = currentResetDate
            binding.etResetDate.setText(android.text.format.DateFormat.format("MMM dd, yyyy", currentResetDate))
        }

        // Set up date picker
        binding.etResetDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Budget Reset Date")
                .setSelection(selectedResetDate?.time ?: MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedResetDate = Date(selection)
                binding.etResetDate.setText(android.text.format.DateFormat.format("MMM dd, yyyy", selectedResetDate))
            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        // Submit button
        binding.btnSubmit.setOnClickListener {
            val budgetStr = binding.etBudget.text.toString().trim()
            val budget = budgetStr.toDoubleOrNull()

            // Validation
            if (budgetStr.isEmpty() || budget == null || budget <= 0) {
                binding.tilBudget.error = "Enter a valid budget amount"
                return@setOnClickListener
            }
            binding.tilBudget.error = null

            if (selectedResetDate == null) {
                Toast.makeText(this, "Please select a reset date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Ensure reset date is in the future
            val calendar = Calendar.getInstance()
            val currentDate = Date()
            if (selectedResetDate!!.before(currentDate)) {
                Toast.makeText(this, "Reset date must be in the future", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                storageHelper.setMonthlyBudget(budget, selectedResetDate!!)
                Toast.makeText(this@MonthlyBudgetSetupActivity, "Budget of Rs. $budget added as Salary. Transactions cleared.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}