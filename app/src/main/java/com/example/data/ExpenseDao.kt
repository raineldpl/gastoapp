package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC, id DESC")
    fun getAllExpensesFlow(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC, id DESC")
    suspend fun getAllExpenses(): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Int)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    // --- Budgets ---
    @Query("SELECT * FROM category_budgets")
    fun getAllBudgetsFlow(): Flow<List<CategoryBudget>>

    @Query("SELECT * FROM category_budgets")
    suspend fun getAllBudgets(): List<CategoryBudget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: CategoryBudget)

    @Query("DELETE FROM category_budgets WHERE category = :category")
    suspend fun deleteBudget(category: String)

    @Query("DELETE FROM category_budgets")
    suspend fun deleteAllBudgets()
}
