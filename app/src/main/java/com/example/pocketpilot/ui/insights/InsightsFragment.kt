package com.example.pocketpilot.ui.insights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.pocketpilot.R
import com.example.pocketpilot.databinding.FragmentInsightsBinding
import com.example.pocketpilot.ui.transactions.TransactionViewModel

class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!

    private val transactionViewModel: TransactionViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding= FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionViewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->
            var totalIncome = 0.0
            var totalExpense = 0.0

            val categoryExpenseMap = mutableMapOf<String, Double>()

            for (transaction in transactions) {
                if (transaction.type == "Income") {
                    totalIncome += transaction.amount
                } else {
                    totalExpense += transaction.amount
                    val oldValue = categoryExpenseMap[transaction.category] ?: 0.0
                    categoryExpenseMap[transaction.category] = oldValue + transaction.amount
                }
            }

            val balance = totalIncome - totalExpense

            binding.tvInsightIncome.text = "₹$totalIncome"
            binding.tvInsightExpense.text = "₹$totalExpense"
            binding.tvInsightBalance.text = "₹$balance"

            val highestCategory = categoryExpenseMap.maxByOrNull { it.value }

            if (highestCategory != null) {
                binding.tvTopCategory.text =
                    "${highestCategory.key} (₹${highestCategory.value})"
            } else {
                binding.tvTopCategory.text = "No expense data yet"
            }

            binding.tvSmartInsight.text = getSmartInsight(totalIncome, totalExpense, highestCategory?.key)
        }
    }

    private fun getSmartInsight(
        income: Double,
        expense: Double,
        topCategory: String?
    ): String {
        return when {
            income == 0.0 && expense == 0.0 ->
                "No transaction data available yet. Start adding transactions to view insights."

            expense > income ->
                "Your expenses are higher than your income. Try reducing spending on ${topCategory ?: "unnecessary categories"}."

            expense == 0.0 ->
                "Great! No expenses recorded yet. Keep tracking wisely."

            topCategory != null ->
                "Your highest spending is in $topCategory. Monitor this category to improve savings."

            else ->
                "Your finances look stable. Keep tracking regularly."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}







