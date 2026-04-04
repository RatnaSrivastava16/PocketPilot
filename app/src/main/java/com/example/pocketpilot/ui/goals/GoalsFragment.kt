package com.example.pocketpilot.ui.goals

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.pocketpilot.databinding.FragmentGoalsBinding
import com.example.pocketpilot.ui.transactions.TransactionViewModel

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    private val transactionViewModel: TransactionViewModel by viewModels()

    private val prefs by lazy {
        requireContext().getSharedPreferences("goals_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSavedGoal()
        observeTransactions()
        setupSaveGoal()
    }

    private fun loadSavedGoal() {
        val savedGoal = prefs.getFloat("monthly_goal", 0f).toDouble()
        if (savedGoal > 0) {
            binding.etGoalAmount.setText(savedGoal.toString())
        }
    }

    private fun setupSaveGoal() {
        binding.btnSaveGoal.setOnClickListener {
            val goalText = binding.etGoalAmount.text.toString().trim()

            if (goalText.isEmpty()) {
                Toast.makeText(requireContext(), "Enter goal amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val goalAmount = goalText.toDoubleOrNull()
            if (goalAmount == null || goalAmount <= 0) {
                Toast.makeText(requireContext(), "Enter valid goal amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit().putFloat("monthly_goal", goalAmount.toFloat()).apply()
            Toast.makeText(requireContext(), "Goal saved", Toast.LENGTH_SHORT).show()
            updateGoalUI(currentBalance = getCurrentBalance(), goalAmount = goalAmount)
        }
    }

    private var latestBalance = 0.0

    private fun observeTransactions() {
        transactionViewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->
            var income = 0.0
            var expense = 0.0

            for (transaction in transactions) {
                if (transaction.type == "Income") {
                    income += transaction.amount
                } else {
                    expense += transaction.amount
                }
            }

            latestBalance = income - expense

            val goalAmount = prefs.getFloat("monthly_goal", 0f).toDouble()
            updateGoalUI(latestBalance, goalAmount)
        }
    }

    private fun getCurrentBalance(): Double = latestBalance

    private fun updateGoalUI(currentBalance: Double, goalAmount: Double) {
        binding.tvCurrentSavings.text = "₹$currentBalance"
        binding.tvGoalTarget.text = "₹$goalAmount"

        if (goalAmount <= 0) {
            binding.progressGoal.progress = 0
            binding.tvGoalProgress.text = "Set a goal to track progress"
            binding.tvRemainingAmount.text = "Remaining: --"
            return
        }

        val progressPercent = ((currentBalance / goalAmount) * 100).toInt().coerceIn(0, 100)
        val remaining = (goalAmount - currentBalance).coerceAtLeast(0.0)

        binding.progressGoal.progress = progressPercent
        binding.tvGoalProgress.text = "$progressPercent% completed"
        binding.tvRemainingAmount.text = "Remaining: ₹$remaining"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}