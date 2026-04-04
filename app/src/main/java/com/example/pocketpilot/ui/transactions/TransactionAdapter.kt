package com.example.pocketpilot.ui.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pocketpilot.data.local.TransactionEntity
import com.example.pocketpilot.databinding.ItemTransactionBinding

class TransactionAdapter(
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

        holder.binding.tvCategory.text = transaction.category
        holder.binding.tvNote.text = transaction.note
        holder.binding.tvType.text = transaction.type
        holder.binding.tvAmount.text = "₹${transaction.amount}"

        holder.binding.btnEdit.setOnClickListener {
            onEditClick(transaction)
        }

        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick(transaction)
        }
    }

    override fun getItemCount(): Int = transactionList.size

    fun submitList(list: List<TransactionEntity>) {
        transactionList = list
        notifyDataSetChanged()
    }
}