package com.example.pocketpilot.ui.analytics
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.pocketpilot.databinding.FragmentAnalyticsBinding
import com.example.pocketpilot.ui.transactions.TransactionViewModel
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.util.Calendar

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val transactionViewModel: TransactionViewModel by viewModels()

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

            val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)

            for (transaction in transactions) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = transaction.date

                val transactionMonth = calendar.get(Calendar.MONTH)
                val transactionYear = calendar.get(Calendar.YEAR)

                if (transaction.type == "Income") {
                    totalIncome += transaction.amount
                } else {
                    totalExpense += transaction.amount
                    val oldValue = expenseCategoryMap[transaction.category] ?: 0.0
                    expenseCategoryMap[transaction.category] = oldValue + transaction.amount
                }

                if (transactionMonth == currentMonth && transactionYear == currentYear) {
                    if (transaction.type == "Income") {
                        monthlyIncome += transaction.amount
                    } else {
                        monthlyExpense += transaction.amount
                    }
                }
            }

            val monthlyBalance = monthlyIncome - monthlyExpense

            binding.tvMonthlyIncome.text = "₹$monthlyIncome"
            binding.tvMonthlyExpense.text = "₹$monthlyExpense"
            binding.tvMonthlyBalance.text = "₹$monthlyBalance"

            setupPieChart(expenseCategoryMap)
            setupBarChart(totalIncome, totalExpense)
        }
    }

    private fun setupPieChart(categoryMap: Map<String, Double>) {
        if (categoryMap.isEmpty()) {
            binding.pieChart.clear()
            binding.pieChart.setNoDataText("No expense data available")
            return
        }
        val entries = ArrayList<PieEntry>()

        for ((category, amount) in categoryMap) {
            entries.add(PieEntry(amount.toFloat(), category))
        }

        val dataSet = PieDataSet(entries, "Expense Categories")
        dataSet.colors = listOf(
            Color.parseColor("#EF5350"),
            Color.parseColor("#42A5F5"),
            Color.parseColor("#66BB6A"),
            Color.parseColor("#FFA726"),
            Color.parseColor("#AB47BC"),
            Color.parseColor("#26C6DA")
        )

        val data = PieData(dataSet)
        data.setValueTextSize(12f)
        data.setValueTextColor(Color.BLACK)

        binding.pieChart.data = data
        binding.pieChart.description.isEnabled = false
        binding.pieChart.centerText = "Expenses"
        binding.pieChart.setEntryLabelColor(Color.BLACK)
        binding.pieChart.animateY(1000)
        binding.pieChart.invalidate()
    }

    private fun setupBarChart(totalIncome: Double, totalExpense: Double) {
        val entries = arrayListOf(
            BarEntry(0f, totalIncome.toFloat()),
            BarEntry(1f, totalExpense.toFloat())
        )

        val dataSet = BarDataSet(entries, "Income vs Expense")
        dataSet.colors = listOf(
            Color.parseColor("#43A047"),
            Color.parseColor("#E53935")
        )

        val data = BarData(dataSet)
        data.barWidth = 0.5f
        data.setValueTextSize(12f)

        binding.barChart.data = data
        binding.barChart.description.isEnabled = false
        binding.barChart.axisRight.isEnabled = false
        binding.barChart.xAxis.granularity = 1f

        val labels = listOf("Income", "Expense")
        binding.barChart.xAxis.valueFormatter =
            object : com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels) {}
        binding.barChart.xAxis.position =
            com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        binding.barChart.xAxis.setDrawGridLines(false)

        binding.barChart.animateY(1000)
        binding.barChart.invalidate()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}