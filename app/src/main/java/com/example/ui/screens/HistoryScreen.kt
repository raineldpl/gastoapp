package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Expense
import com.example.ui.ExpenseViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: ExpenseViewModel,
    onEditExpense: (Expense) -> Unit
) {
    val expenses by viewModel.filteredExpenses.collectAsState()
    val tags by viewModel.allTags.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeCategory by viewModel.selectedCategoryFilter.collectAsState()
    val activePayMethod by viewModel.selectedPaymentMethodFilter.collectAsState()
    val activeTag by viewModel.selectedTagFilter.collectAsState()
    val activeSortOrder by viewModel.sortOrder.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

    val categories = listOf(
        "Comida", "Transporte", "Vivienda", "Servicios",
        "Entretenimiento", "Salud", "Ropa & Compras", "Otros"
    )

    val paymentMethods = listOf(
        "Efectivo", "Tarjeta de Débito", "Tarjeta de Crédito", "Transferencia", "Otro"
    )

    // Calculate sum of filtered elements
    val subtotal = remember(expenses) {
        expenses.sumOf { it.amount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_screen")
    ) {
        // --- Search bar & Sort Icon ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Buscar concepto, nota, etiqueta...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                } else null,
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("history_search_input")
            )

            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.testTag("history_sort_btn")
                ) {
                    Icon(
                        Icons.Default.Sort,
                        contentDescription = "Ordenar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Más Nuevos Primero") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        onClick = {
                            viewModel.sortOrder.value = ExpenseViewModel.SortOrder.NEWEST
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Más Antiguos Primero") },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                        onClick = {
                            viewModel.sortOrder.value = ExpenseViewModel.SortOrder.OLDEST
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Mayor Importe Primero") },
                        leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
                        onClick = {
                            viewModel.sortOrder.value = ExpenseViewModel.SortOrder.HIGHEST_AMOUNT
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Menor Importe Primero") },
                        leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                        onClick = {
                            viewModel.sortOrder.value = ExpenseViewModel.SortOrder.LOWEST_AMOUNT
                            showSortMenu = false
                        }
                    )
                }
            }
        }

        // --- Active Filter Summary Board (Counts & subtotal) ---
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mostrando ${expenses.size} transacciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Subtotal: ${String.format("%,.2f", subtotal)} €",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                // Clear all filters button if any is active
                if (activeCategory != null || activePayMethod != null || activeTag != null || searchQuery.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            viewModel.selectedCategoryFilter.value = null
                            viewModel.selectedPaymentMethodFilter.value = null
                            viewModel.selectedTagFilter.value = null
                            viewModel.searchQuery.value = ""
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.FilterListOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset filtros")
                    }
                }
            }
        }

        // --- Filter Carousels ---
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Category Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = activeCategory == null,
                        onClick = { viewModel.selectedCategoryFilter.value = null },
                        label = { Text("Todas Categorías") }
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = activeCategory == category,
                        onClick = { viewModel.selectedCategoryFilter.value = category },
                        label = { Text(category) },
                        modifier = Modifier.testTag("filter_chip_cat_$category")
                    )
                }
            }

            // Payment Methods Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = activePayMethod == null,
                        onClick = { viewModel.selectedPaymentMethodFilter.value = null },
                        label = { Text("Cualquier Pago") }
                    )
                }
                items(paymentMethods) { method ->
                    FilterChip(
                        selected = activePayMethod == method,
                        onClick = { viewModel.selectedPaymentMethodFilter.value = method },
                        label = { Text(method) }
                    )
                }
            }

            // Tags Row
            if (tags.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = activeTag == null,
                            onClick = { viewModel.selectedTagFilter.value = null },
                            label = { Text("Cualquier Etiqueta") }
                        )
                    }
                    items(tags) { tag ->
                        FilterChip(
                            selected = activeTag == tag,
                            onClick = { viewModel.selectedTagFilter.value = tag },
                            label = { Text("#$tag") }
                        )
                    }
                }
            }
        }

        Divider(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        // --- Main List of Expenses ---
        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.FilterAlt,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Sin coincidencias",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Prueba con otros criterios de búsqueda o limpia los filtros activos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.wrapContentSize()
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenses, key = { it.id }) { expense ->
                    HistoryExpenseCard(
                        expense = expense,
                        onClick = { onEditExpense(expense) },
                        onDelete = { viewModel.deleteExpense(expense) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryExpenseCard(
    expense: Expense,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryIcon = when (expense.category) {
        "Comida" -> Icons.Default.Restaurant
        "Transporte" -> Icons.Default.DirectionsCar
        "Vivienda" -> Icons.Default.Home
        "Servicios" -> Icons.Default.ElectricalServices
        "Entretenimiento" -> Icons.Default.ConfirmationNumber
        "Salud" -> Icons.Default.LocalHospital
        "Ropa & Compras" -> Icons.Default.Checkroom
        else -> Icons.Default.Category
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_expense_item_${expense.id}"),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(categoryIcon, contentDescription = expense.category, modifier = Modifier.size(20.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = expense.title,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = expense.paymentMethod,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (expense.details.isNotEmpty()) {
                    Text(
                        text = expense.details,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = expense.getFormattedDate(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (expense.isRecurring) {
                        Text(
                            text = "• Recurrente",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Tags badges inside the card
                    expense.getTagsList().take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                Text(
                    text = "-${String.format("%.2f", expense.amount)} €",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_btn_history_${expense.id}")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Borrar",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
