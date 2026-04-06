package com.example.pocketpilot.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pocketpilot.R
import com.example.pocketpilot.data.local.TransactionEntity
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
            val calendarNow = java.util.Calendar.getInstance()
            val currentMonth = calendarNow.get(java.util.Calendar.MONTH)
            val currentYear = calendarNow.get(java.util.Calendar.YEAR)

            var monthlyIncome = 0.0
            var monthlyExpense = 0.0

            for (t in list) {
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = t.date

                val month = cal.get(java.util.Calendar.MONTH)
                val year = cal.get(java.util.Calendar.YEAR)

                if (month == currentMonth && year == currentYear) {
                    if (t.type == "Income") {
                        monthlyIncome += t.amount
                    } else {
                        monthlyExpense += t.amount
                    }
                }
            }

            val monthlyChange = monthlyIncome - monthlyExpense

            binding.tvMonthlyChange.text =
                if (monthlyChange >= 0) {
                    "↑ +₹${monthlyChange.toInt()} this month"
                } else {
                    "↓ -₹${abs(monthlyChange).toInt()} this month"
                }

            binding.tvMonthlyChange.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (monthlyChange >= 0) R.color.income_green else R.color.expense_red
                )
            )
            binding.tvIncome.text = "₹$income"
            binding.tvExpense.text = "₹$expense"
            binding.tvBalance.text = "₹$balance"

            updateSmartInsight(income, expense, categoryExpenseMap)

            updateGoalSection(balance)

            updateBudgetSection(categoryExpenseMap)

            updateWeeklyTrend(list)

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

        binding.btnSetGoal.setOnClickListener {
            showGoalDialog()
        }

    }

    private fun updateWeeklyTrend(transactions: List<TransactionEntity>) {

        val incomeList = MutableList(7) { 0.0 }
        val expenseList = MutableList(7) { 0.0 }

        val calendar = java.util.Calendar.getInstance()
        calendar.firstDayOfWeek = java.util.Calendar.MONDAY

        val today = java.util.Calendar.getInstance()

        val startOfWeek = calendar.apply {
            timeInMillis = today.timeInMillis
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfWeek = calendar.apply {
            timeInMillis = startOfWeek
            add(java.util.Calendar.DAY_OF_MONTH, 6)
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }.timeInMillis

        for (t in transactions) {
            if (t.date in startOfWeek..endOfWeek) {
                val index = getDayIndexFromMillis(t.date)

                if (index in 0..6) {
                    if (t.type.equals("Income", true)) {
                        incomeList[index] += t.amount
                    } else {
                        expenseList[index] += t.amount
                    }
                }
            }
        }

        val maxAmount = (incomeList + expenseList).maxOrNull() ?: 0.0

        setBar(binding.barMon, incomeList[0], expenseList[0], maxAmount)
        setBar(binding.barTue, incomeList[1], expenseList[1], maxAmount)
        setBar(binding.barWed, incomeList[2], expenseList[2], maxAmount)
        setBar(binding.barThu, incomeList[3], expenseList[3], maxAmount)
        setBar(binding.barFri, incomeList[4], expenseList[4], maxAmount)
        setBar(binding.barSat, incomeList[5], expenseList[5], maxAmount)
        setBar(binding.barSun, incomeList[6], expenseList[6], maxAmount)
    }
    private fun getDayIndexFromMillis(dateMillis: Long): Int {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = dateMillis

        return when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> 0
            java.util.Calendar.TUESDAY -> 1
            java.util.Calendar.WEDNESDAY -> 2
            java.util.Calendar.THURSDAY -> 3
            java.util.Calendar.FRIDAY -> 4
            java.util.Calendar.SATURDAY -> 5
            java.util.Calendar.SUNDAY -> 6
            else -> -1
        }
    }

    private fun setBar(
        bar: View,
        income: Double,
        expense: Double,
        maxAmount: Double
    ) {
        val total = income + expense

        val minHeight = 24
        val maxHeight = 90

        var height = if (maxAmount == 0.0) {
            minHeight
        } else {
            (minHeight + ((total / maxAmount) * (maxHeight - minHeight))).toInt()
        }
        if(height>=90) {
            height = 90
        }
        val params = bar.layoutParams
        params.height = dpToPx(height)
        bar.layoutParams = params

        if (bar.background == null) {
            bar.setBackgroundResource(R.drawable.bar_bg)
        }

        val colorRes = when {
            income > expense -> R.color.income_green
            expense > income -> R.color.expense_red
            else -> R.color.primary_accent
        }

        bar.background.setTint(
            ContextCompat.getColor(requireContext(), colorRes)
        )
    }


    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    private fun setupRecycler() {
        adapter = TransactionAdapter(
            false,
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

        var foundBudget = false

    }

    private fun showGoalDialog() {
        val dialogBinding = com.example.pocketpilot.databinding.DialogSetGoalBinding.inflate(layoutInflater)

        val savedGoal = goalPrefs.getFloat("monthly_goal", 0f)
        if (savedGoal > 0f) {
            dialogBinding.etGoalAmount.setText(savedGoal.toString())
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnCancelGoal.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSaveGoal.setOnClickListener {
            val enteredValue = dialogBinding.etGoalAmount.text.toString().toFloatOrNull() ?: 0f

            goalPrefs.edit()
                .putFloat("monthly_goal", enteredValue)
                .apply()

            val incomeText = binding.tvIncome.text.toString().replace("₹", "").toDoubleOrNull() ?: 0.0
            val expenseText = binding.tvExpense.text.toString().replace("₹", "").toDoubleOrNull() ?: 0.0
            val balance = incomeText - expenseText

            updateGoalSection(balance)

            android.widget.Toast.makeText(
                requireContext(),
                "Goal saved successfully",
                android.widget.Toast.LENGTH_SHORT
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