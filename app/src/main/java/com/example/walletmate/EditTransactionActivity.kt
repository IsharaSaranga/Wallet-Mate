package com.example.walletmate

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.walletmate.databinding.ActivityEditTransactionBinding
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditTransactionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditTransactionBinding
    private lateinit var storageHelper: StorageHelper
    private var selectedDate: Date? = null
    private lateinit var transactionId: String
    private lateinit var transaction: Transaction

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storageHelper = StorageHelper(this)

        // Get transaction data from intent
        transactionId = intent.getStringExtra("TRANSACTION_ID") ?: ""
        transaction = intent.getSerializableExtra("TRANSACTION") as Transaction

        // Set up UI
        binding.etTitle.setText(transaction.title)
        binding.etAmount.setText(transaction.amount.toString())
        selectedDate = transaction.date
        binding.etDate.setText(SimpleDateFormat("MMM dd, yyyy", Locale.US).format(transaction.date))
        if (transaction.type == "Add Funds") {
            binding.tilRecipient.visibility = View.GONE
            binding.spinnerCategory.adapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_item, listOf("Salary", "Gift", "Investment", "Other")
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        } else {
            binding.tilRecipient.visibility = View.VISIBLE
            binding.etRecipient.setText(transaction.recipient ?: "")
            binding.spinnerCategory.adapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_item, listOf("Food", "Bills", "Shopping", "Travel", "Other")
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
        binding.spinnerCategory.setSelection(
            (binding.spinnerCategory.adapter as ArrayAdapter<String>).getPosition(transaction.category)
        )

        // Set up date picker
        binding.etDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(selectedDate?.time ?: MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDate = Date(selection)
                binding.etDate.setText(SimpleDateFormat("MMM dd, yyyy", Locale.US).format(selectedDate!!))
            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        // Submit button
        binding.btnSubmit.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val amountStr = binding.etAmount.text.toString().trim()
            val category = binding.spinnerCategory.selectedItem.toString()
            val recipient = binding.etRecipient.text.toString().trim()

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

            if (transaction.type == "Payment" && recipient.isEmpty()) {
                binding.tilRecipient.error = "Recipient name is required"
                return@setOnClickListener
            }
            binding.tilRecipient.error = null

            if (selectedDate == null) {
                Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedTransaction = Transaction(
                id = transaction.id, // Preserve original ID
                title = title,
                type = transaction.type,
                amount = amount,
                category = category,
                date = selectedDate!!,
                recipient = if (transaction.type == "Payment") recipient else null
            )

            lifecycleScope.launch {
                storageHelper.updateTransaction(transactionId, updatedTransaction)
                setResult(RESULT_OK)
                finish()
            }
        }
    }
}