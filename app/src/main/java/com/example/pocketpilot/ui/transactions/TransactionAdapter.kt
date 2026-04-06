package com.example.pocketpilot.ui.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketpilot.R
import com.example.pocketpilot.data.local.TransactionEntity
import com.example.pocketpilot.databinding.ItemTransactionBinding

class TransactionAdapter(
    private val showActions: Boolean = true,
    private val onEditClick: (TransactionEntity) -> Unit,
    private val onDeleteClick: (TransactionEntity) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private var transactionList = listOf<TransactionEntity>()

    inner class TransactionViewHolder(val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactionList[position]

        // Category emoji icon
        val emoji = when (transaction.category.lowercase()) {
            "food" -> "🍔"
            "travel" -> "✈️"
            "shopping" -> "🛍️"
            "bills" -> "💡"
            "health" -> "❤️"
            "entertainment" -> "🎬"
            "salary" -> "💼"
            else -> "💰"
        }
        holder.binding.tvCategoryIcon.text = emoji
        holder.binding.tvCategory.text = transaction.category
        holder.binding.tvNote.text = transaction.note.ifEmpty { "No note" }

        holder.binding.btnEdit.visibility =
            if (showActions) View.VISIBLE else View.GONE

        holder.binding.btnDelete.visibility =
            if (showActions) View.VISIBLE else View.GONE

        // Color-code amount
        if (transaction.type == "Income") {
            holder.binding.tvAmount.text = "+₹${transaction.amount}"
            holder.binding.tvAmount.setTextColor(
                holder.itemView.context.getColor(R.color.income_green)
            )
        } else {
            holder.binding.tvAmount.text = "-₹${transaction.amount}"
            holder.binding.tvAmount.setTextColor(
                holder.itemView.context.getColor(R.color.expense_red)
            )
        }

        holder.binding.btnEdit.setOnClickListener { onEditClick(transaction) }
        holder.binding.btnDelete.setOnClickListener { onDeleteClick(transaction) }
    }

    override fun getItemCount(): Int = transactionList.size

    fun submitList(list: List<TransactionEntity>) {
        transactionList = list
        notifyDataSetChanged()
    }
}