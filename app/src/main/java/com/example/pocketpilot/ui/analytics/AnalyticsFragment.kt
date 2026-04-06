package com.example.pocketpilot.ui.analytics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.pocketpilot.R
import com.example.pocketpilot.databinding.FragmentAnalyticsBinding
import com.example.pocketpilot.ui.transactions.TransactionViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.Calendar

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val transactionViewModel: TransactionViewModel by viewModels()

    // Dark theme chart colors matching Zorvyn palette
    private val chartColors = listOf(
        Color.parseColor("#7B6FFF"),  // purple accent
        Color.parseColor("#4ADE80"),  // income green
        Color.parseColor("#F87171"),  // expense red
        Color.parseColor("#F59E0B"),  // amber
        Color.parseColor("#60A5FA"),  // blue
        Color.parseColor("#F472B6")   // pink
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionViewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->

            var totalIncome = 0.0
            var totalExpense = 0.0
            var monthlyIncome = 0.0
            var monthlyExpense = 0.0
            val expenseCategoryMap = mutableMapOf<String, Double>()

            var thisWeekExpense = 0.0
            var lastWeekExpense = 0.0

            val now = Calendar.getInstance()
            val currentMonth = now.get(Calendar.MONTH)
            val currentYear = now.get(Calendar.YEAR)

            // Start of this week
            val startOfThisWeek = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Start of last week
            val startOfLastWeek = Calendar.getInstance().apply {
                timeInMillis = startOfThisWeek.timeInMillis
                add(Calendar.WEEK_OF_YEAR, -1)
            }

            for (transaction in transactions) {
                val cal = Calendar.getInstance()
                cal.timeInMillis = transaction.date

                if (transaction.type == "Income") {
                    totalIncome += transaction.amount
                } else {
                    totalExpense += transaction.amount
                    expenseCategoryMap[transaction.category] =
                        (expenseCategoryMap[transaction.category] ?: 0.0) + transaction.amount
                }

                // Monthly totals
                if (cal.get(Calendar.MONTH) == currentMonth &&
                    cal.get(Calendar.YEAR) == currentYear
                ) {
                    if (transaction.type == "Income") {
                        monthlyIncome += transaction.amount
                    } else {
                        monthlyExpense += transaction.amount
                    }
                }

                // Weekly comparison (expense only)
                if (transaction.type == "Expense") {
                    when {
                        cal.timeInMillis >= startOfThisWeek.timeInMillis -> {
                            thisWeekExpense += transaction.amount
                        }
                        cal.timeInMillis >= startOfLastWeek.timeInMillis &&
                                cal.timeInMillis < startOfThisWeek.timeInMillis -> {
                            lastWeekExpense += transaction.amount
                        }
                    }
                }
            }

            val monthlyBalance = monthlyIncome - monthlyExpense

            // Update summary cards
            binding.tvMonthlyIncome.text = "₹${monthlyIncome.toInt()}"
            binding.tvMonthlyExpense.text = "₹${monthlyExpense.toInt()}"
            binding.tvMonthlyBalance.text = "₹${monthlyBalance.toInt()}"

            // Savings rate badge
            val savingsRate = if (monthlyIncome > 0) {
                ((monthlyBalance / monthlyIncome) * 100).toInt().coerceIn(0, 100)
            } else 0
            binding.tvSavingsRate.text = "$savingsRate%"

            // Top category
            val topCategory = expenseCategoryMap.maxByOrNull { it.value }
            if (topCategory != null) {
                binding.tvTopCategory.text = topCategory.key
                binding.tvTopCategoryAmount.text = "₹${topCategory.value.toInt()} spent this period"
            } else {
                binding.tvTopCategory.text = "No expense data yet"
                binding.tvTopCategoryAmount.text = ""
            }

            // Weekly comparison insight
            updateWeeklyComparison(thisWeekExpense, lastWeekExpense)

            setupPieChart(expenseCategoryMap)
            setupBarChart(totalIncome, totalExpense)
        }
    }

    private fun updateWeeklyComparison(thisWeekExpense: Double, lastWeekExpense: Double) {
        when {
            thisWeekExpense == 0.0 && lastWeekExpense == 0.0 -> {
                binding.tvWeeklyComparison.text = "No weekly expense data yet"
                binding.tvWeeklyComparisonDetails.text = ""
            }

            thisWeekExpense > lastWeekExpense -> {
                val diff = thisWeekExpense - lastWeekExpense
                val percent = if (lastWeekExpense > 0) {
                    ((diff / lastWeekExpense) * 100).toInt()
                } else 100

                binding.tvWeeklyComparison.text = "You spent more this week"
                binding.tvWeeklyComparisonDetails.text =
                    "₹${thisWeekExpense.toInt()} this week vs ₹${lastWeekExpense.toInt()} last week (+$percent%)"
            }

            thisWeekExpense < lastWeekExpense -> {
                val diff = lastWeekExpense - thisWeekExpense
                val percent = if (lastWeekExpense > 0) {
                    ((diff / lastWeekExpense) * 100).toInt()
                } else 0

                binding.tvWeeklyComparison.text = "You spent less this week"
                binding.tvWeeklyComparisonDetails.text =
                    "₹${thisWeekExpense.toInt()} this week vs ₹${lastWeekExpense.toInt()} last week (-$percent%)"
            }

            else -> {
                binding.tvWeeklyComparison.text = "Your spending stayed the same"
                binding.tvWeeklyComparisonDetails.text =
                    "₹${thisWeekExpense.toInt()} this week and last week"
            }
        }
    }
    private fun setupPieChart(categoryMap: Map<String, Double>) {
        val chart = binding.pieChart

        // Style the chart background to match dark theme
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.setHoleColor(Color.parseColor("#111827"))
        chart.setTransparentCircleColor(Color.parseColor("#111827"))
        chart.setTransparentCircleAlpha(80)
        chart.holeRadius = 52f
        chart.transparentCircleRadius = 57f
        chart.setDrawCenterText(true)
        chart.centerText = "Expenses"
        chart.setCenterTextColor(Color.parseColor("#8A9AB0"))
        chart.setCenterTextSize(13f)
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.legend.textColor = Color.parseColor("#8A9AB0")
        chart.legend.textSize = 11f
        chart.setEntryLabelColor(Color.parseColor("#E8EDF5"))
        chart.setEntryLabelTextSize(11f)

        if (categoryMap.isEmpty()) {
            chart.clear()
            chart.setNoDataText("No expense data available")
            chart.setNoDataTextColor(Color.parseColor("#4A5568"))
            return
        }

        val entries = categoryMap.map { (category, amount) ->
            PieEntry(amount.toFloat(), category)
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = chartColors
            sliceSpace = 2f
            selectionShift = 6f
            valueTextColor = Color.parseColor("#E8EDF5")
            valueTextSize = 11f
        }

        chart.data = PieData(dataSet)
        chart.animateY(900)
        chart.invalidate()
    }

    private fun setupBarChart(totalIncome: Double, totalExpense: Double) {
        val chart = binding.barChart

        // Dark theme styling
        chart.setBackgroundColor(Color.TRANSPARENT)
        chart.setDrawBarShadow(false)
        chart.setDrawValueAboveBar(true)
        chart.description.isEnabled = false
        chart.setPinchZoom(false)
        chart.setDrawGridBackground(false)
        chart.axisRight.isEnabled = false

        // X axis
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            granularity = 1f
            textColor = Color.parseColor("#4A5568")
            textSize = 11f
            axisLineColor = Color.parseColor("#1E2330")
            valueFormatter = IndexAxisValueFormatter(listOf("Income", "Expense"))
        }

        // Y axis (left)
        chart.axisLeft.apply {
            textColor = Color.parseColor("#4A5568")
            textSize = 10f
            gridColor = Color.parseColor("#1E2330")
            axisLineColor = Color.parseColor("#1E2330")
            setDrawZeroLine(false)
        }

        // Legend
        chart.legend.apply {
            textColor = Color.parseColor("#8A9AB0")
            textSize = 11f
        }

        val entries = arrayListOf(
            BarEntry(0f, totalIncome.toFloat()),
            BarEntry(1f, totalExpense.toFloat())
        )

        val dataSet = BarDataSet(entries, "All Time").apply {
            colors = listOf(
                Color.parseColor("#4ADE80"),  // income green
                Color.parseColor("#F87171")   // expense red
            )
            valueTextColor = Color.parseColor("#E8EDF5")
            valueTextSize = 12f
        }

        chart.data = BarData(dataSet).apply {
            barWidth = 0.45f
        }

        chart.animateY(900)
        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}