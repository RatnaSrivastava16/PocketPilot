package com.example.pocketpilot.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.pocketpilot.R
import com.example.pocketpilot.data.local.TransactionEntity
import com.example.pocketpilot.databinding.FragmentAddEditTransactionBinding


class AddEditTransactionFragment : Fragment() {
    private var _binding: FragmentAddEditTransactionBinding? = null
    private val binding get() = _binding!!
    private val transactionViewModel: TransactionViewModel by viewModels()
    private var transactionId: Int = 0
    private var isEditMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
    _binding = FragmentAddEditTransactionBinding.inflate(inflater, container, false)
    return binding.root// Inflate the layout for this fragment
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinner()
        checkEditMode()
        setupSaveButton()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun setupSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.transaction_types,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = adapter
    }

    private fun checkEditMode() {
        arguments?.let { bundle ->
            if (bundle.containsKey("transactionId")) {
                isEditMode = true
                transactionId = bundle.getInt("transactionId")

                val amount = bundle.getDouble("transactionAmount")
                val type = bundle.getString("transactionType", "Expense")
                val category = bundle.getString("transactionCategory", "")
                val note = bundle.getString("transactionNote", "")

                binding.etAmount.setText(amount.toString())
                binding.etCategory.setText(category)
                binding.etNote.setText(note)

                val spinnerPosition =
                    (0 until binding.spinnerType.count).firstOrNull {
                        binding.spinnerType.getItemAtPosition(it).toString() == type
                    } ?: 0
                binding.spinnerType.setSelection(spinnerPosition)

                binding.btnSaveTransaction.text = "Update Transaction"
            }
        }
    }
    private fun setupSaveButton() {
        binding.btnSaveTransaction.setOnClickListener {
            val amountText = binding.etAmount.text.toString().trim()
            val category = binding.etCategory.text.toString().trim()
            val note = binding.etNote.text.toString().trim()
            val type = binding.spinnerType.selectedItem.toString()

            if (amountText.isEmpty() || category.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill required fields", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(requireContext(), "Enter valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val transaction = if (isEditMode) {
                TransactionEntity(
                    id = transactionId,   // very important
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


}