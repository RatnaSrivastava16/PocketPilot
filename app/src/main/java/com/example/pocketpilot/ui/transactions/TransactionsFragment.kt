package com.example.pocketpilot.ui.transactions

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pocketpilot.R
import com.example.pocketpilot.data.local.TransactionEntity
import com.example.pocketpilot.databinding.FragmentTransactionsBinding
import kotlin.math.min

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val transactionViewModel: TransactionViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    private val budgetPrefs by lazy {
        requireContext().getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
    }

    // tracks latest expense map so budget cards can refresh after dialog save
    private var latestExpenseMap: Map<String, Double> = emptyMap()

    private val categories = listOf(
        "Food", "Travel", "Shopping", "Bills", "Health", "Entertainment", "Other"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeTransactions()

        binding.btnSetBudgetTx.setOnClickListener {
            showBudgetDialog()
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            true,
            onEditClick = { transaction ->
                val bundle = bundleOf(
                    "transactionId" to transaction.id,
                    "transactionAmount" to transaction.amount,
                    "transactionType" to transaction.type,
                    "transactionCategory" to transaction.category,
                    "transactionNote" to transaction.note
                )
                findNavController().navigate(
                    R.id.action_transactionsFragment_to_addEditTransactionFragment,
                    bundle
                )
            },
            onDeleteClick = { transaction ->
                showDeleteConfirmationDialog(transaction)
            }
        )

        binding.recyclerViewTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }

    private fun observeTransactions() {
        transactionViewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.submitList(transactions)

            // Build expense map from transactions
            val expenseMap = mutableMapOf<String, Double>()
            for (t in transactions) {
                if (t.type != "Income") {
                    expenseMap[t.category] = (expenseMap[t.category] ?: 0.0) + t.amount
                }
            }
            latestExpenseMap = expenseMap
            refreshBudgetCards(expenseMap)
        }
    }

    private fun refreshBudgetCards(expenseMap: Map<String, Double>) {
        val container = binding.llBudgetCards
        container.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())

        for (category in categories) {
            val budget = budgetPrefs.getFloat(category, 0f).toDouble()
            if (budget <= 0) continue  // skip categories with no budget set

            val spent = expenseMap[category] ?: 0.0
            val percent = ((spent / budget) * 100).toInt().coerceIn(0, 100)
            val remaining = budget - spent

            val cardView = inflater.inflate(
                R.layout.item_budget_card, container, false
            )

            val emoji = when (category.lowercase()) {
                "food" -> "🍔"
                "travel" -> "✈️"
                "shopping" -> "🛍️"
                "bills" -> "💡"
                "health" -> "❤️"
                "entertainment" -> "🎬"
                else -> "💰"
            }

            cardView.findViewById<TextView>(R.id.tvBudgetEmoji).text = emoji
            cardView.findViewById<TextView>(R.id.tvBudgetCategory).text = category
            cardView.findViewById<TextView>(R.id.tvBudgetSpent).text = "₹${spent.toInt()}"
            cardView.findViewById<TextView>(R.id.tvBudgetLimit).text = "of ₹${budget.toInt()}"

            val progressBar = cardView.findViewById<ProgressBar>(R.id.progressBudget)
            progressBar.progress = percent

            val statusView = cardView.findViewById<TextView>(R.id.tvBudgetStatus)

            when {
                spent > budget -> {
                    // Over budget — red bar + warning text
                    progressBar.progressTintList =
                        android.content.res.ColorStateList.valueOf(
                            requireContext().getColor(R.color.expense_red)
                        )
                    progressBar.progressBackgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            requireContext().getColor(R.color.expense_bg)
                        )
                    statusView.text = "Over by ₹${(spent - budget).toInt()}"
                    statusView.setTextColor(requireContext().getColor(R.color.expense_red))
                }
                percent >= 80 -> {
                    // Warning zone — amber bar
                    progressBar.progressTintList =
                        android.content.res.ColorStateList.valueOf(
                            requireContext().getColor(R.color.warning_amber)
                        )
                    progressBar.progressBackgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            requireContext().getColor(R.color.border_color)
                        )
                    statusView.text = "₹${remaining.toInt()} left"
                    statusView.setTextColor(requireContext().getColor(R.color.warning_amber))
                }
                else -> {
                    // Safe — purple/accent bar
                    progressBar.progressTintList =
                        android.content.res.ColorStateList.valueOf(
                            requireContext().getColor(R.color.purple_500)
                        )
                    progressBar.progressBackgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            requireContext().getColor(R.color.border_color)
                        )
                    statusView.text = "₹${remaining.toInt()} left"
                    statusView.setTextColor(requireContext().getColor(R.color.income_green))
                }
            }

            container.addView(cardView)
        }

        // If no budgets set at all, show a hint card
        if (container.childCount == 0) {
            val hint = TextView(requireContext()).apply {
                text = "Tap 'Set Budget' to track your spending limits"
                textSize = 12f
                setTextColor(requireContext().getColor(R.color.text_muted))
                setPadding(4, 8, 4, 8)
            }
            container.addView(hint)
        }
    }

    private fun showBudgetDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_set_budget, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Prefill values
        categories.forEach { category ->
            val editTextId = getEditTextId(category)
            if (editTextId != -1) {
                dialogView.findViewById<EditText>(editTextId)?.setText(
                    budgetPrefs.getFloat(category, 0f).let {
                        if (it == 0f) "" else it.toInt().toString()
                    }
                )
            }
        }

        // Cancel button
        dialogView.findViewById<TextView>(R.id.btnCancelBudget).setOnClickListener {
            dialog.dismiss()
        }

        // Save button
        dialogView.findViewById<TextView>(R.id.btnSaveBudget).setOnClickListener {

            val editor = budgetPrefs.edit()

            categories.forEach { category ->
                val editTextId = getEditTextId(category)

                if (editTextId != -1) {
                    val value = dialogView.findViewById<EditText>(editTextId)
                        ?.text?.toString()?.toFloatOrNull() ?: 0f

                    editor.putFloat(category, value)
                }
            }

            editor.apply()

            // Refresh UI
            refreshBudgetCards(latestExpenseMap)

            Toast.makeText(requireContext(), "Budget saved", Toast.LENGTH_SHORT).show()

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getEditTextId(category: String): Int {
        return when (category) {
            "Food" -> R.id.etFoodBudget
            "Travel" -> R.id.etTravelBudget
            "Shopping" -> R.id.etShoppingBudget
            "Bills" -> R.id.etBillsBudget
            "Health" -> R.id.etHealthBudget
            "Entertainment" -> R.id.etEntertainmentBudget
            "Other" -> R.id.etOtherBudget
            else -> -1
        }
    }

    private fun showDeleteConfirmationDialog(transaction: TransactionEntity) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_delete, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Cancel
        dialogView.findViewById<TextView>(R.id.btnCancelDelete).setOnClickListener {
            dialog.dismiss()
        }

        // Delete
        dialogView.findViewById<TextView>(R.id.btnConfirmDelete).setOnClickListener {
            transactionViewModel.deleteTransaction(transaction)

            Toast.makeText(
                requireContext(),
                "Transaction Deleted",
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}