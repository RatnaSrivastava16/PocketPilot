package com.example.pocketpilot.ui.transactions

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.pocketpilot.R
import com.example.pocketpilot.data.local.TransactionEntity
import com.example.pocketpilot.databinding.DialogSetBudgetBinding
import com.example.pocketpilot.databinding.FragmentAddEditTransactionBinding
import kotlin.math.abs

class AddEditTransactionFragment : Fragment() {

    private var _binding: FragmentAddEditTransactionBinding? = null
    private val binding get() = _binding!!

    private val transactionViewModel: TransactionViewModel by viewModels()

    private var transactionId: Int = 0
    private var isEditMode = false

    // Tracks which type and category the user has selected
    private var selectedType = "Expense"
    private var selectedCategory = ""

    // All chip buttons mapped to their category string
    private val chipMap: Map<String, Int> by lazy {
        mapOf(
            "Food"          to R.id.chipFood,
            "Travel"        to R.id.chipTravel,
            "Shopping"      to R.id.chipShopping,
            "Bills"         to R.id.chipBills,
            "Health"        to R.id.chipHealth,
            "Entertainment" to R.id.chipEntertainment,
            "Salary"        to R.id.chipSalary,
            "Other"         to R.id.chipOther
        )
    }
    private val budgetPrefs by lazy {
        requireContext().getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
    }

    private var latestCategoryExpenseMap: Map<String, Double> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinner()
        setupTypeToggle()
        setupCategoryChips()
        checkEditMode()
        setupSaveButton()
        observeTransactions()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

    }
    private fun setupSpinner() {
        // Keep spinner working for ViewModel compatibility
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.transaction_types,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = adapter
    }

    private fun setupTypeToggle() {
        // Default: Expense selected
        selectType("Expense")

        binding.btnTypeExpense.setOnClickListener { selectType("Expense") }
        binding.btnTypeIncome.setOnClickListener  { selectType("Income") }
    }

    private fun selectType(type: String) {
        selectedType = type

        // Sync hidden spinner so ViewModel reads correct value
        val spinnerPosition = if (type == "Income") 1 else 0
        binding.spinnerType.setSelection(spinnerPosition)

        if (type == "Expense") {
            // Expense button — active red style
            binding.btnTypeExpense.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.expense_bg)
            binding.btnTypeExpense.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.expense_red)
            )
            binding.btnTypeExpense.strokeColor=
                ContextCompat.getColorStateList(requireContext(), R.color.expense_red)
            binding.btnTypeExpense.strokeWidth=1


            // Income button — inactive
            binding.btnTypeIncome.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.card_bg)
            binding.btnTypeIncome.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_muted)
            )
            binding.btnTypeIncome.strokeColor=(
                ContextCompat.getColorStateList(requireContext(),R.color.text_muted)
                    )
            binding.btnTypeIncome.strokeWidth=1

        } else {
            // Income button — active green style
            binding.btnTypeIncome.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.income_bg)
            binding.btnTypeIncome.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.income_green)
            )
            binding.btnTypeIncome.strokeColor=
                ContextCompat.getColorStateList(requireContext(), R.color.income_green)
            binding.btnTypeIncome.strokeWidth=1

            // Expense button — inactive
            binding.btnTypeExpense.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.card_bg)
            binding.btnTypeExpense.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_muted)
            )
            binding.btnTypeExpense.strokeColor=
                ContextCompat.getColorStateList(requireContext(), R.color.text_muted)
            binding.btnTypeExpense.strokeWidth=1
        }
    }

    private fun setupCategoryChips() {
        chipMap.forEach { (category, chipId) ->
            binding.root.findViewById<Button>(chipId).setOnClickListener {
                selectCategory(category)
            }
        }
    }

    private fun selectCategory(category: String) {
        selectedCategory = category

        // Sync hidden EditText so save logic reads it
        binding.etCategory.setText(category)

        // Reset all chips to inactive style first
        chipMap.values.forEach { chipId ->
            binding.root.findViewById<Button>(chipId).apply {
                backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.card_bg)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            }
        }

        // Highlight selected chip with accent color
        chipMap[category]?.let { chipId ->
            binding.root.findViewById<Button>(chipId).apply {
                backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.white)
                setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.card_bg)
                )
            }
        }
    }

    private fun checkEditMode() {
        arguments?.let { bundle ->
            if (bundle.containsKey("transactionId")) {
                isEditMode = true
                transactionId = bundle.getInt("transactionId")

                val amount   = bundle.getDouble("transactionAmount")
                val type     = bundle.getString("transactionType", "Expense")
                val category = bundle.getString("transactionCategory", "")
                val note     = bundle.getString("transactionNote", "")

                binding.etAmount.setText(amount.toString())
                binding.etNote.setText(note)
                binding.tvScreenTitle.text = "Edit Transaction"
                binding.btnSaveTransaction.text = "Update Transaction"

                // Restore type and category selections
                selectType(type)
                if (category.isNotEmpty()) selectCategory(category)
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveTransaction.setOnClickListener {

            val amountText = binding.etAmount.text.toString().trim()
            val category   = binding.etCategory.text.toString().trim()
            val note       = binding.etNote.text.toString().trim()
            val type       = selectedType

            if (amountText.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (category.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val transaction = if (isEditMode) {
                TransactionEntity(
                    id = transactionId,
                    amount = amount,
                    type = type,
                    category = category,
                    date = System.currentTimeMillis(),
                    note = note
                )
            } else {
                TransactionEntity(
                    amount = amount,
                    type = type,
                    category = category,
                    date = System.currentTimeMillis(),
                    note = note
                )
            }

            if (isEditMode) {
                transactionViewModel.updateTransaction(transaction)
                Toast.makeText(requireContext(), "Transaction Updated", Toast.LENGTH_SHORT).show()
            } else {
                transactionViewModel.insertTransaction(transaction)
                Toast.makeText(requireContext(), "Transaction Saved", Toast.LENGTH_SHORT).show()
            }

            findNavController().popBackStack()
        }
    }

    private fun observeTransactions() {
        transactionViewModel.allTransactions.observe(viewLifecycleOwner) { list ->

            val categoryExpenseMap = mutableMapOf<String, Double>()

            for (t in list) {
                if (t.type.equals("Expense", true)) {
                    val oldValue = categoryExpenseMap[t.category] ?: 0.0
                    categoryExpenseMap[t.category] = oldValue + t.amount
                }
            }

            latestCategoryExpenseMap = categoryExpenseMap

            updateBudgetSection(categoryExpenseMap)
        }
    }

    private fun updateBudgetSection(categoryExpenseMap: Map<String, Double>) {
        val categories = listOf(
            "Food",
            "Travel",
            "Shopping",
            "Bills",
            "Health",
            "Entertainment",
            "Other"
        )

        val alertLines = mutableListOf<String>()

        for (category in categories) {
            val budget = budgetPrefs.getFloat(category, 0f).toDouble()
            val spent = categoryExpenseMap[category] ?: 0.0

            if (budget > 0) {
                if (spent > budget) {
                    alertLines.add(
                        "• $category: ₹${spent.toInt()} / ₹${budget.toInt()}  (Exceeded by ₹${abs(spent - budget).toInt()})"
                    )
                } else {
                    alertLines.add(
                        "• $category: ₹${spent.toInt()} / ₹${budget.toInt()}  (Left ₹${(budget - spent).toInt()})"
                    )
                }
            }
        }

        binding.tvBudgetSummaryEdit.text =
            if (alertLines.isEmpty()) "No budget set yet"
            else alertLines.joinToString("\n")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}