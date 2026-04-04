package com.example.pocketpilot.data.repository

import androidx.lifecycle.LiveData
import com.example.pocketpilot.data.local.TransactionDao
import com.example.pocketpilot.data.local.TransactionEntity

class FinanceRepository(private val transactionDao: TransactionDao) {
    fun getAllTransactions(): LiveData<List<TransactionEntity>> {
        return transactionDao.getAllTransactions()
    }
    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }
    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }
    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }
    suspend fun getTransactionById(id: Int): TransactionEntity? {
        return transactionDao.getTransactionById(id)
    }
}