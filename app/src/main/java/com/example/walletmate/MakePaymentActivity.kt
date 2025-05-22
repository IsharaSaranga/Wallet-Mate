package com.example.walletmate

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.walletmate.databinding.ActivityMakePaymentBinding
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MakePaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMakePaymentBinding
    private lateinit var storageHelper: StorageHelper
    private var selectedDate: Date = Date() // Default to current date

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMakePaymentBinding.inflate(layoutInflater)
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
                    // Already on make payment, do nothing
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
        binding.bottomNavigation.selectedItemId = R.id.navigation_make_payment

        storageHelper = StorageHelper(this)

        // Set up category spinner
        val categories = listOf("Food", "Bills", "Shopping", "Travel", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter

        // Set up date picker
        binding.etDate.setText(SimpleDateFormat("MMM dd, yyyy", Locale.US).format(selectedDate))
        binding.etDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDate = Date(selection)
                binding.etDate.setText(SimpleDateFormat("MMM dd, yyyy", Locale.US).format(selectedDate))
            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        // Submit button
        binding.btnSubmit.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val amountStr = binding.etAmount.text.toString()
            val category = binding.spinnerCategory.selectedItem.toString()
            val recipient = binding.etRecipient.text.toString().trim()

            // Validation
            if (title.isEmpty()) {
                binding.tilTitle.error = "Title is required"
                return@setOnClickListener
            } else {
                binding.tilTitle.error = null
            }
            if (amountStr.isEmpty() || amountStr.toDoubleOrNull() == null || amountStr.toDouble() <= 0) {
                binding.tilAmount.error = "Enter a valid amount"
                return@setOnClickListener
            } else {
                binding.tilAmount.error = null
            }
            if (recipient.isEmpty()) {
                binding.tilRecipient.error = "Recipient name is required"
                return@setOnClickListener
            } else {
                binding.tilRecipient.error = null
            }

            val amount = amountStr.toDouble()
            val balance = storageHelper.getBalance()
            if (amount > balance) {
                Toast.makeText(this, "Insufficient balance", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                storageHelper.addBalance(-amount)
                storageHelper.saveTransaction(
                    Transaction(
                        title = title,
                        type = "Payment",
                        amount = amount,
                        category = category,
                        date = selectedDate,
                        recipient = recipient
                    )
                )
                Toast.makeText(this@MakePaymentActivity, "Payment successful", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}