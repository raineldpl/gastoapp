package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CategoryBudget
import com.example.data.Expense
import com.example.data.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    // --- Core Data Flows ---
    val allExpenses = repository.allExpenses
    val allBudgets = repository.allBudgets

    // --- Search & Filter States ---
    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedPaymentMethodFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter = MutableStateFlow<String?>(null)
    val sortOrder = MutableStateFlow(SortOrder.NEWEST)

    // Enum representing sorting options
    enum class SortOrder {
        NEWEST, OLDEST, HIGHEST_AMOUNT, LOWEST_AMOUNT
    }

    // --- Filtered Expenses Flow ---
    val filteredExpenses: StateFlow<List<Expense>> = combine(
        allExpenses,
        searchQuery,
        selectedCategoryFilter,
        selectedPaymentMethodFilter,
        selectedTagFilter,
        sortOrder
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val expenses = array[0] as List<Expense>
        val query = array[1] as String
        val cat = array[2] as String?
        val pay = array[3] as String?
        val tag = array[4] as String?
        val order = array[5] as SortOrder

        var list = expenses

        // Apply query search (title, details, tags)
        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.details.contains(query, ignoreCase = true) ||
                it.tags.contains(query, ignoreCase = true)
            }
        }

        // Filter by category
        if (cat != null) {
            list = list.filter { it.category == cat }
        }

        // Filter by payment method
        if (pay != null) {
            list = list.filter { it.paymentMethod == pay }
        }

        // Filter by tag
        if (tag != null) {
            list = list.filter { it.getTagsList().contains(tag) }
        }

        // Apply sorting
        list = when (order) {
            SortOrder.NEWEST -> list.sortedByDescending { it.dateMillis }
            SortOrder.OLDEST -> list.sortedBy { it.dateMillis }
            SortOrder.HIGHEST_AMOUNT -> list.sortedByDescending { it.amount }
            SortOrder.LOWEST_AMOUNT -> list.sortedBy { it.amount }
        }

        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Available Tags Flow ---
    val allTags: StateFlow<List<String>> = allExpenses.combine(searchQuery) { expenses, _ ->
        expenses.flatMap { it.getTagsList() }.distinct().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Budget Status Analysis (Computed helper) ---
    val budgetStatus: StateFlow<Map<String, BudgetAnalysis>> = combine(
        allExpenses,
        allBudgets
    ) { expenses, budgets ->
        val currentMonthExpenses = getCurrentMonthExpenses(expenses)
        
        val categoryExpenses = currentMonthExpenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        budgets.associate { budget ->
            val spent = categoryExpenses[budget.category] ?: 0.0
            budget.category to BudgetAnalysis(
                category = budget.category,
                spent = spent,
                limit = budget.budgetLimit,
                remaining = budget.budgetLimit - spent,
                percentSpent = if (budget.budgetLimit > 0) (spent / budget.budgetLimit) else 0.0
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // --- Summary Statistics Flow ---
    val summaryStats: StateFlow<SummaryStatistics> = combine(
        allExpenses,
        allBudgets
    ) { expenses, budgets ->
        val currentMonthExpenses = getCurrentMonthExpenses(expenses)
        val totalSpentThisMonth = currentMonthExpenses.sumOf { it.amount }
        
        // Sum total monthly budget of set categories
        val totalBudget = budgets.sumOf { it.budgetLimit }
        
        // Count today's spending
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val spendToday = expenses.filter { it.dateMillis >= todayStart }.sumOf { it.amount }

        // Find highest spending category
        val categoryTotals = currentMonthExpenses.groupBy { it.category }
            .mapValues { it.value.sumOf { e -> e.amount } }
        val highestCategory = categoryTotals.maxByOrNull { it.value }?.toPair()

        // Daily average spending this month
        val daysPassedThisMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val dailyAverage = if (daysPassedThisMonth > 0) totalSpentThisMonth / daysPassedThisMonth else 0.0

        SummaryStatistics(
            totalSpentThisMonth = totalSpentThisMonth,
            totalBudgetLimit = totalBudget,
            spentToday = spendToday,
            dailyAverage = dailyAverage,
            highestCategory = highestCategory,
            totalCount = expenses.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SummaryStatistics()
    )

    // --- CRUD Actions ---
    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            repository.insertExpense(expense)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun deleteExpenseById(id: Int) {
        viewModelScope.launch {
            repository.deleteExpenseById(id)
        }
    }

    fun setBudget(category: String, limit: Double) {
        viewModelScope.launch {
            repository.insertBudget(CategoryBudget(category, limit))
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            repository.deleteBudget(category)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAllExpenses()
            repository.deleteAllBudgets()
        }
    }

    // --- Sample Data Prepopulation ---
    fun loadSampleData() {
        viewModelScope.launch {
            // First clear existing data
            repository.deleteAllExpenses()
            repository.deleteAllBudgets()

            // Set some nice budgets
            val sampleBudgets = listOf(
                CategoryBudget("Comida", 450.0),
                CategoryBudget("Transporte", 120.0),
                CategoryBudget("Vivienda", 800.0),
                CategoryBudget("Servicios", 200.0),
                CategoryBudget("Entretenimiento", 150.0),
                CategoryBudget("Salud", 100.0),
                CategoryBudget("Ropa & Compras", 200.0),
                CategoryBudget("Otros", 100.0)
            )
            sampleBudgets.forEach { repository.insertBudget(it) }

            // Set some expenses
            val cal = Calendar.getInstance()
            val now = cal.timeInMillis

            // We generate past days expenses
            val sampleExpenses = listOf(
                Expense(
                    title = "Supermercado Mercadona",
                    amount = 84.50,
                    category = "Comida",
                    dateMillis = now - (1000 * 60 * 60 * 6), // 6 hours ago
                    paymentMethod = "Tarjeta de Débito",
                    details = "Compras semanales de carne, verduras y limpieza",
                    tags = "supermercado, despensa"
                ),
                Expense(
                    title = "Combustible Repsol",
                    amount = 55.00,
                    category = "Transporte",
                    dateMillis = now - (1000 * 60 * 60 * 24 * 1), // 1 day ago
                    paymentMethod = "Tarjeta de Crédito",
                    details = "Tanque lleno para el viaje de trabajo",
                    tags = "gasolina, auto"
                ),
                Expense(
                    title = "Alquiler Piso Mayo",
                    amount = 750.00,
                    category = "Vivienda",
                    dateMillis = now - (1000 * 60 * 60 * 24 * 4), // 4 days ago
                    paymentMethod = "Transferencia",
                    details = "Pago correspondiente al mes actual",
                    tags = "fijo, alquiler",
                    isRecurring = true
                ),
                Expense(
                    title = "Suscripción Netflix & Spotify",
                    amount = 26.98,
                    category = "Servicios",
                    dateMillis = now - (1000 * 60 * 60 * 24 * 3), // 3 days ago
                    paymentMethod = "Tarjeta de Crédito",
                    details = "Abonos mensuales de entretenimiento digital",
                    tags = "suscripción, ocio",
                    isRecurring = true
                ),
                Expense(
                    title = "Cena Restaurante Sushi",
                    amount = 45.00,
                    category = "Comida",
                    dateMillis = now - (1000 * 60 * 60 * 24 * 2), // 2 days ago
                    paymentMethod = "Tarjeta de Débito",
                    details = "Compartido con amigos los viernes",
                    tags = "restaurante, antojo"
                ),
                Expense(
                    title = "Zapatillas Deportivas",
                    amount = 89.90,
                    category = "Ropa & Compras",
                    dateMillis = now - (1000 * 60 * 60 * 24 * 5), // 5 days ago
                    paymentMethod = "Tarjeta de Crédito",
                    details = "Zapatillas nuevas de running en descuento",
                    tags = "deporte, calzado"
                ),
                Expense(
                    title = "Consulta Dentista",
                    amount = 60.00,
                    category = "Salud",
                    dateMillis = now - (1000 * 60 * 60 * 24 * 7), // 7 days ago
                    paymentMethod = "Efectivo",
                    details = "Revisión anual y limpieza",
                    tags = "salud, dental"
                ),
                Expense(
                    title = "Entradas de Cine",
                    amount = 18.50,
                    category = "Entretenimiento",
                    dateMillis = now - (1000 * 60 * 60 * 24 * 6), // 6 days ago
                    paymentMethod = "Tarjeta de Débito",
                    details = "Estreno del fin de semana con palomitas",
                    tags = "cine, ocio"
                ),
                Expense(
                    title = "Café y Panadería",
                    amount = 4.20,
                    category = "Comida",
                    dateMillis = now, // Today
                    paymentMethod = "Efectivo",
                    details = "Desayuno rápido antes de entrar a la oficina",
                    tags = "cafe, desayuno"
                ),
                Expense(
                    title = "Billete de Tren",
                    amount = 32.00,
                    category = "Transporte",
                    dateMillis = now - (1000 * 60 * 60 * 24 * 8), // 8 days ago
                    paymentMethod = "Tarjeta de Débito",
                    details = "Ida y vuelta de fin de semana",
                    tags = "tren, viaje"
                )
            )
            sampleExpenses.forEach { repository.insertExpense(it) }
        }
    }

    // --- Helper to filter expenses of the current calendar month ---
    private fun getCurrentMonthExpenses(expenses: List<Expense>): List<Expense> {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        
        val tempCal = Calendar.getInstance()
        return expenses.filter {
            tempCal.timeInMillis = it.dateMillis
            tempCal.get(Calendar.MONTH) == currentMonth && tempCal.get(Calendar.YEAR) == currentYear
        }
    }
}

// --- Data Representation Helper Classes ---

data class BudgetAnalysis(
    val category: String,
    val spent: Double,
    val limit: Double,
    val remaining: Double,
    val percentSpent: Double
)

data class SummaryStatistics(
    val totalSpentThisMonth: Double = 0.0,
    val totalBudgetLimit: Double = 0.0,
    val spentToday: Double = 0.0,
    val dailyAverage: Double = 0.0,
    val highestCategory: Pair<String, Double>? = null,
    val totalCount: Int = 0
)
