package com.example.walletmate

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.walletmate.databinding.ActivityAddFundsBinding
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class AddFundsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddFundsBinding
    private lateinit var storageHelper: StorageHelper
    private var selectedDate: Date = Date() // Initialize with current date and time

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddFundsBinding.inflate(layoutInflater)
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
                    // Already on add funds, do nothing
                    true
                }
                R.id.navigation_make_payment -> {
                    startActivity(Intent(this, MakePaymentActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_transactions -> {
                    startActivity(Intent(this, TransactionHistoryActivity::class.java))
                    finish()
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
        binding.bottomNavigation.selectedItemId = R.id.navigation_add_funds

        storageHelper = StorageHelper(this)

        // Set up category spinner
        val categories = listOf("Salary", "Gift", "Investment", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        // Set initial date to current date
        binding.etDate.setText(android.text.format.DateFormat.format("MMM dd, yyyy", selectedDate))

        // Set up date picker
        binding.etDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(selectedDate.time)
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                // Preserve the current time when updating the date
                val calendar = Calendar.getInstance()
                calendar.time = selectedDate
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(Calendar.MINUTE)
                val currentSecond = calendar.get(Calendar.SECOND)

                calendar.timeInMillis = selection
                calendar.set(Calendar.HOUR_OF_DAY, currentHour)
                calendar.set(Calendar.MINUTE, currentMinute)
                calendar.set(Calendar.SECOND, currentSecond)

                selectedDate = calendar.time
                binding.etDate.setText(android.text.format.DateFormat.format("MMM dd, yyyy", selectedDate))
            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        // Submit button
        binding.btnSubmit.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val amountStr = binding.etAmount.text.toString().trim()
            val category = binding.spinnerCategory.selectedItem.toString()

            // Validation
            if (title.isEmpty()) {
                binding.tilTitle.error = "Title is required"
                return@setOnClickListener
            }
            binding.tilTitle.error = null

            val amount = amountStr.toDoubleOrNull()
            if (amountStr.isEmpty() || amount == null || amount <= 0) {
                binding.tilAmount.error = "Enter a valid amount"
                return@setOnClickListener
            }
            binding.tilAmount.error = null

            val transaction = Transaction(
                title = title,
                type = "Add Funds",
                amount = amount,
                category = category,
                date = selectedDate,
                recipient = null
            )

            lifecycleScope.launch {
                storageHelper.addBalance(amount) // Increase balance
                storageHelper.saveTransaction(transaction)
                Toast.makeText(this@AddFundsActivity, "Funds added successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}