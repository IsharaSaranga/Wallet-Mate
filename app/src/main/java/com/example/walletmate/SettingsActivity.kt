package com.example.walletmate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.walletmate.databinding.ActivitySettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var storageHelper: StorageHelper
    private var isInitialSelection = true // Flag to skip initial spinner selection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
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
                    startActivity(Intent(this, TransactionHistoryActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_settings -> {
                    true
                }
                else -> false
            }
        }

        storageHelper = StorageHelper(this)

        // Set up currency spinner
        val currencySymbols = mapOf(
            "LKR (Rs.)" to "Rs.",
            "USD ($)" to "$",
            "JPY (¥)" to "¥",
            "GBP (£)" to "£"
        )
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.currency_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCurrency.adapter = adapter

        // Set current currency asynchronously
        lifecycleScope.launch {
            val currentSymbol = storageHelper.getCurrencySymbol()
            val currentCurrency = currencySymbols.entries.find { it.value == currentSymbol }?.key ?: "LKR (Rs.)"
            binding.spinnerCurrency.setSelection(
                resources.getStringArray(R.array.currency_options).indexOf(currentCurrency)
            )
            isInitialSelection = false // Allow user selections after initial setup
        }

        // Handle currency selection
        binding.spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (isInitialSelection) {
                    return // Skip initial selection
                }
                val selectedCurrency = parent.getItemAtPosition(position).toString()
                val symbol = currencySymbols[selectedCurrency] ?: "Rs."
                lifecycleScope.launch {
                    val currentSymbol = storageHelper.getCurrencySymbol()
                    if (symbol != currentSymbol) { // Only update if currency changed
                        storageHelper.setCurrencySymbol(symbol)
                        Toast.makeText(this@SettingsActivity, "Currency changed to $selectedCurrency", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No action needed
            }
        }

        // Handle backup
        binding.btnBackup.setOnClickListener {
            lifecycleScope.launch {
                if (storageHelper.backupData()) {
                    Toast.makeText(this@SettingsActivity, "Backup successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SettingsActivity, "Backup failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Handle download backup
        binding.btnDownloadBackup.setOnClickListener {
            lifecycleScope.launch {
                val backupFile = File(filesDir, "backup.json")
                if (backupFile.exists()) {
                    try {
                        // Create a copy in the downloads directory
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        val downloadFile = File(downloadsDir, "walletmate_backup_$timestamp.json")
                        backupFile.copyTo(downloadFile, overwrite = true)
                        
                        // Notify user of success
                        Toast.makeText(
                            this@SettingsActivity,
                            "Backup downloaded to: ${downloadFile.absolutePath}",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@SettingsActivity, "Failed to download backup: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@SettingsActivity, "No backup file found. Please create a backup first.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Handle restore
        binding.btnRestore.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Restore Data")
                .setMessage("This will replace all current data with the backup. Are you sure?")
                .setPositiveButton("Restore") { _, _ ->
                    lifecycleScope.launch {
                        if (storageHelper.restoreData()) {
                            Toast.makeText(this@SettingsActivity, "Restore successful", Toast.LENGTH_SHORT).show()
                            // Refresh currency spinner
                            val currentSymbol = storageHelper.getCurrencySymbol()
                            val currentCurrency = currencySymbols.entries.find { it.value == currentSymbol }?.key ?: "LKR (Rs.)"
                            isInitialSelection = true // Prevent toast on programmatic selection
                            binding.spinnerCurrency.setSelection(
                                resources.getStringArray(R.array.currency_options).indexOf(currentCurrency)
                            )
                            isInitialSelection = false
                        } else {
                            Toast.makeText(this@SettingsActivity, "Restore failed: No backup found or invalid data", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Handle reset
        binding.btnReset.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Reset All Data")
                .setMessage("Are you sure you want to reset all app data? This action cannot be undone.")
                .setPositiveButton("Reset") { _, _ ->
                    lifecycleScope.launch {
                        storageHelper.resetAllData()
                        Toast.makeText(this@SettingsActivity, "All data has been reset", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}