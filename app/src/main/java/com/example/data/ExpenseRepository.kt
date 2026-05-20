package com.example.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpensesFlow()
    val allBudgets: Flow<List<CategoryBudget>> = expenseDao.getAllBudgetsFlow()

    suspend fun insertExpense(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteExpenseById(id: Int) {
        expenseDao.deleteExpenseById(id)
    }

    suspend fun deleteAllExpenses() {
        expenseDao.deleteAllExpenses()
    }

    suspend fun insertBudget(budget: CategoryBudget) {
        expenseDao.insertBudget(budget)
    }

    suspend fun deleteBudget(category: String) {
        expenseDao.deleteBudget(category)
    }

    suspend fun deleteAllBudgets() {
        expenseDao.deleteAllBudgets()
    }

    suspend fun getAllExpensesDirect() = expenseDao.getAllExpenses()
    suspend fun getAllBudgetsDirect() = expenseDao.getAllBudgets()
}
