package com.example.pocketpilot.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pocketpilot.databinding.DialogSetBudgetBinding
import com.example.pocketpilot.databinding.FragmentHomeBinding
import com.example.pocketpilot.ui.transactions.TransactionAdapter
import com.example.pocketpilot.ui.transactions.TransactionViewModel
import kotlin.math.abs

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter

    private val goalPrefs by lazy {
        requireContext().getSharedPreferences("goals_prefs", Context.MODE_PRIVATE)
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
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecycler()

        viewModel.allTransactions.observe(viewLifecycleOwner) { list ->
            var income = 0.0
            var expense = 0.0

            val categoryExpenseMap = mutableMapOf<String, Double>()

            for (t in list) {
                if (t.type == "Income") {
                    income += t.amount
                } else {
                    expense += t.amount
                    val oldValue = categoryExpenseMap[t.category] ?: 0.0
                    categoryExpenseMap[t.category] = oldValue + t.amount
                }
            }
            latestCategoryExpenseMap = categoryExpenseMap

            val balance = income - expense

            // Home cards
            binding.tvIncome.text = "₹$income"
            binding.tvExpense.text = "₹$expense"
            binding.tvBalance.text = "₹$balance"

            // Merged Insights logic
            updateSmartInsight(income, expense, categoryExpenseMap)

            // Merged Goals logic
            updateGoalSection(balance)

            // Existing/Home budget summary logic
            updateBudgetSection(categoryExpenseMap)

            // Recent transactions
            val recentList = list.take(5)
            adapter.submitList(recentList)

            if (recentList.isEmpty()) {
                binding.tvEmptyRecent.visibility = View.VISIBLE
                binding.recyclerRecent.visibility = View.GONE
            } else {
                binding.tvEmptyRecent.visibility = View.GONE
                binding.recyclerRecent.visibility = View.VISIBLE
            }
        }
        binding.btnSetBudget.setOnClickListener {
            showBudgetDialog()
        }
    }

    private fun setupRecycler() {
        adapter = TransactionAdapter(
            onEditClick = {},
            onDeleteClick = {}
        )

        binding.recyclerRecent.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRecent.adapter = adapter
    }

    private fun updateSmartInsight(
        income: Double,
        expense: Double,
        categoryExpenseMap: Map<String, Double>
    ) {
        val highestCategory = categoryExpenseMap.maxByOrNull { it.value }

        if (highestCategory != null) {
            binding.tvTopCategoryHome.text =
                "${highestCategory.key} (₹${highestCategory.value})"
        } else {
            binding.tvTopCategoryHome.text = "No expense data yet"
        }

        val insight = when {
            income == 0.0 && expense == 0.0 ->
                "No transaction data available yet. Start adding transactions."

            expense > income ->
                "Your expenses are higher than your income. Try reducing spending on ${highestCategory?.key ?: "extra categories"}."

            expense == 0.0 ->
                "Great! No expenses recorded yet. Keep tracking wisely."

            highestCategory != null ->
                "Your highest spending is in ${highestCategory.key}. Monitor this category to improve savings."

            else ->
                "Your finances look stable. Keep tracking regularly."
        }

        binding.tvSmartInsight.text = insight
    }

    private fun updateGoalSection(balance: Double) {
        val goalAmount = goalPrefs.getFloat("monthly_goal", 0f).toDouble()

        binding.tvGoalTargetHome.text = "Goal: ₹$goalAmount"

        if (goalAmount <= 0) {
            binding.progressGoalHome.progress = 0
            binding.tvGoalProgressHome.text = "Set a goal to track progress"
            return
        }

        val progressPercent = ((balance / goalAmount) * 100).toInt().coerceIn(0, 100)
        binding.progressGoalHome.progress = progressPercent
        binding.tvGoalProgressHome.text = "$progressPercent% completed"
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

        var alertText = "No budget set yet"
        var foundBudget = false

        for (category in categories) {
            val budget = budgetPrefs.getFloat(category, 0f).toDouble()
            val spent = categoryExpenseMap[category] ?: 0.0

            if (budget > 0) {
                foundBudget = true

                if (spent > budget) {
                    alertText = "$category budget exceeded by ₹${abs(spent - budget)}"
                    break
                } else {
                    alertText = "$category budget is under control. Remaining ₹${budget - spent}"
                }
            }
        }

        if (!foundBudget) {
            alertText = "No budget set yet"
        }

        binding.tvBudgetSummaryHome.text = alertText
    }

    private fun showBudgetDialog() {
        val dialogBinding = DialogSetBudgetBinding.inflate(layoutInflater)

        dialogBinding.etFoodBudget.setText(budgetPrefs.getFloat("Food", 0f).toString())
        dialogBinding.etTravelBudget.setText(budgetPrefs.getFloat("Travel", 0f).toString())
        dialogBinding.etShoppingBudget.setText(budgetPrefs.getFloat("Shopping", 0f).toString())
        dialogBinding.etBillsBudget.setText(budgetPrefs.getFloat("Bills", 0f).toString())
        dialogBinding.etHealthBudget.setText(budgetPrefs.getFloat("Health", 0f).toString())
        dialogBinding.etEntertainmentBudget.setText(budgetPrefs.getFloat("Entertainment", 0f).toString())
        dialogBinding.etOtherBudget.setText(budgetPrefs.getFloat("Other", 0f).toString())

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Set Monthly Budget")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                budgetPrefs.edit()
                    .putFloat("Food", dialogBinding.etFoodBudget.text.toString().toFloatOrNull() ?: 0f)
                    .putFloat("Travel", dialogBinding.etTravelBudget.text.toString().toFloatOrNull() ?: 0f)
                    .putFloat("Shopping", dialogBinding.etShoppingBudget.text.toString().toFloatOrNull() ?: 0f)
                    .putFloat("Bills", dialogBinding.etBillsBudget.text.toString().toFloatOrNull() ?: 0f)
                    .putFloat("Health", dialogBinding.etHealthBudget.text.toString().toFloatOrNull() ?: 0f)
                    .putFloat("Entertainment", dialogBinding.etEntertainmentBudget.text.toString().toFloatOrNull() ?: 0f)
                    .putFloat("Other", dialogBinding.etOtherBudget.text.toString().toFloatOrNull() ?: 0f)
                    .apply()

                updateBudgetSection(latestCategoryExpenseMap)

                android.widget.Toast.makeText(
                    requireContext(),
                    "Budget saved successfully",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}