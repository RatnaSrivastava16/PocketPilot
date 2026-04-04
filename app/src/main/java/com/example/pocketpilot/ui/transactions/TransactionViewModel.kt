package com.example.pocketpilot.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.pocketpilot.data.local.AppDatabase
import com.example.pocketpilot.data.local.TransactionEntity
import com.example.pocketpilot.data.repository.FinanceRepository
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository
    val allTransactions: LiveData<List<TransactionEntity>>

    init {
        val dao = AppDatabase.getDatabase(application).transactionDao()
        repository = FinanceRepository(dao)
        allTransactions = repository.getAllTransactions()
    }
    fun insertTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }


}