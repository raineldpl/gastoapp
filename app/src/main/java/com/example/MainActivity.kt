package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.Expense
import com.example.data.ExpenseRepository
import com.example.ui.ExpenseViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Room Database and Repository
        val database = AppDatabase.getDatabase(this)
        val repository = ExpenseRepository(database.expenseDao())

        setContent {
            MyApplicationTheme {
                // 2. Initialize ViewModel with custom factory for simple constructor injection
                val expenseViewModel: ExpenseViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                                @Suppress("UNCHECKED_CAST")
                                return ExpenseViewModel(repository) as T
                            }
                            throw IllegalArgumentException("Unknown ViewModel class")
                        }
                    }
                )

                ExpenseTrackerApp(viewModel = expenseViewModel)
            }
        }
    }
}

// Enum defining available tabs for navigation
enum class ScreenTab(val title: String) {
    DASHBOARD("Inicio"),
    HISTORY("Historial"),
    ANALYTICS("Analíticas"),
    BUDGETS("Presupuestos")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerApp(viewModel: ExpenseViewModel) {
    var activeTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }
    var isAddEditOpen by remember { mutableStateOf(false) }
    var selectedExpenseForEdit by remember { mutableStateOf<Expense?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "GastoDetalle",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Quick link to add a new expense
                            selectedExpenseForEdit = null
                            isAddEditOpen = true
                        },
                        modifier = Modifier.testTag("top_bar_add_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir gasto")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("app_top_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("app_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // Tab 1: Dashboard
                NavigationBarItem(
                    selected = activeTab == ScreenTab.DASHBOARD,
                    onClick = { activeTab = ScreenTab.DASHBOARD },
                    label = { Text(ScreenTab.DASHBOARD.title) },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == ScreenTab.DASHBOARD) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = ScreenTab.DASHBOARD.title
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                // Tab 2: History
                NavigationBarItem(
                    selected = activeTab == ScreenTab.HISTORY,
                    onClick = { activeTab = ScreenTab.HISTORY },
                    label = { Text(ScreenTab.HISTORY.title) },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == ScreenTab.HISTORY) Icons.Default.ReceiptLong else Icons.Outlined.ReceiptLong,
                            contentDescription = ScreenTab.HISTORY.title
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_history")
                )

                // Tab 3: Analytics
                NavigationBarItem(
                    selected = activeTab == ScreenTab.ANALYTICS,
                    onClick = { activeTab = ScreenTab.ANALYTICS },
                    label = { Text(ScreenTab.ANALYTICS.title) },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == ScreenTab.ANALYTICS) Icons.Default.Analytics else Icons.Outlined.Analytics,
                            contentDescription = ScreenTab.ANALYTICS.title
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_analytics")
                )

                // Tab 4: Budgets
                NavigationBarItem(
                    selected = activeTab == ScreenTab.BUDGETS,
                    onClick = { activeTab = ScreenTab.BUDGETS },
                    label = { Text(ScreenTab.BUDGETS.title) },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == ScreenTab.BUDGETS) Icons.Default.PieChart else Icons.Outlined.PieChart,
                            contentDescription = ScreenTab.BUDGETS.title
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_budgets")
                )
            }
        },
        floatingActionButton = {
            // Only show FAB on Dashboard and History for clean visuals
            if (activeTab == ScreenTab.DASHBOARD || activeTab == ScreenTab.HISTORY) {
                ExtendedFloatingActionButton(
                    text = { Text("Registrar Gasto") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {
                        selectedExpenseForEdit = null
                        isAddEditOpen = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .testTag("add_expense_fab")
                        .padding(bottom = 12.dp)
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            // Render active tab dynamically based on selected index
            when (activeTab) {
                ScreenTab.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToHistory = { activeTab = ScreenTab.HISTORY },
                    onNavigateToBudgets = { activeTab = ScreenTab.BUDGETS },
                    onEditExpense = { expense ->
                        selectedExpenseForEdit = expense
                        isAddEditOpen = true
                    }
                )
                ScreenTab.HISTORY -> HistoryScreen(
                    viewModel = viewModel,
                    onEditExpense = { expense ->
                        selectedExpenseForEdit = expense
                        isAddEditOpen = true
                    }
                )
                ScreenTab.ANALYTICS -> AnalyticsScreen(viewModel = viewModel)
                ScreenTab.BUDGETS -> BudgetsScreen(viewModel = viewModel)
            }
        }
    }

    // --- Bottom Sheet / Modal Dialog for adding/editing ---
    if (isAddEditOpen) {
        AddEditExpenseDialog(
            expenseToEdit = selectedExpenseForEdit,
            onDismiss = {
                isAddEditOpen = false
                selectedExpenseForEdit = null
            },
            onSave = { expense ->
                if (selectedExpenseForEdit != null) {
                    viewModel.updateExpense(expense)
                } else {
                    viewModel.addExpense(expense)
                }
                isAddEditOpen = false
                selectedExpenseForEdit = null
            }
        )
    }
}
