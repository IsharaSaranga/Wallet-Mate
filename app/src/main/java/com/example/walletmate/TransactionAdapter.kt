package com.example.walletmate

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.walletmate.databinding.ItemTransactionBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private val transactions: List<Transaction>,
    private val onEditClick: (String, Transaction) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int = transactions.size

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

        fun bind(transaction: Transaction) {
            binding.tvTitle.text = transaction.title.takeIf { it.isNotEmpty() } ?: "Untitled"
            binding.tvType.text = transaction.type.takeIf { it.isNotEmpty() } ?: "Unknown"
            binding.tvAmount.text = currencyFormat.format(transaction.amount)
            binding.tvCategory.text = transaction.category.takeIf { it.isNotEmpty() } ?: "Other"
            binding.tvDate.text = transaction.date?.let { dateFormat.format(it) } ?: "Unknown"
            binding.tvRecipient.text = transaction.recipient ?: "-"

            // Set background color
            val backgroundColor = if (transaction.type == "Add Funds") {
                ContextCompat.getColor(itemView.context, R.color.light_green)
            } else {
                ContextCompat.getColor(itemView.context, R.color.light_red)
            }
            binding.root.setCardBackgroundColor(backgroundColor)

            // Handle edit and delete clicks
            binding.btnEdit.setOnClickListener { onEditClick(transaction.id, transaction) }
            binding.btnDelete.setOnClickListener { onDeleteClick(transaction.id) }
        }
    }
}