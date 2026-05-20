package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.BudgetAnalysis
import com.example.ui.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(viewModel: ExpenseViewModel) {
    val analysisByCat by viewModel.budgetStatus.collectAsState()
    val rawBudgets by viewModel.allBudgets.collectAsState(initial = emptyList())

    var activeEditingCategory by remember { mutableStateOf<String?>(null) }
    var currentEditAmountStr by remember { mutableStateOf("") }
    var editAmountError by remember { mutableStateOf(false) }

    val categories = listOf(
        "Comida", "Transporte", "Vivienda", "Servicios",
        "Entretenimiento", "Salud", "Ropa & Compras", "Otros"
    )

    // Calculate spent summary
    val totalBudget = remember(rawBudgets) { rawBudgets.sumOf { it.budgetLimit } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("budgets_screen")
    ) {
        // --- Summary top banner ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Límite Total Mensual",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = "${String.format("%,.2f", totalBudget)} €",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = "Configura límites para categorías individuales abajo para regular tus hábitos de consumo detalladamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.surfaceVariant)

        // --- Category List ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories) { category ->
                val stats = analysisByCat[category] ?: BudgetAnalysis(
                    category = category,
                    spent = 0.0,
                    limit = 0.0,
                    remaining = 0.0,
                    percentSpent = 0.0
                )

                BudgetCategoryCard(
                    category = category,
                    stats = stats,
                    onEditLimit = {
                        activeEditingCategory = category
                        currentEditAmountStr = if (stats.limit > 0.0) stats.limit.toString() else ""
                        editAmountError = false
                    }
                )
            }
        }
    }

    // --- Budget Configurator Dialog ---
    if (activeEditingCategory != null) {
        val catName = activeEditingCategory!!
        AlertDialog(
            onDismissRequest = { activeEditingCategory = null },
            title = { Text("Preconfigurar de Límite") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Establecer presupuesto mensual máximo para la categoría: $catName",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = currentEditAmountStr,
                        onValueChange = { input ->
                            val cleanInput = input.filter { it.isDigit() || it == '.' }
                            currentEditAmountStr = cleanInput
                            val amt = cleanInput.toDoubleOrNull()
                            editAmountError = cleanInput.isNotEmpty() && (amt == null || amt < 0.0)
                        },
                        label = { Text("Límite mensual (€)") },
                        placeholder = { Text("Ej. 300.00") },
                        leadingIcon = { Icon(Icons.Default.Euro, contentDescription = null) },
                        trailingIcon = if (currentEditAmountStr.isNotEmpty()) {
                            {
                                IconButton(onClick = { currentEditAmountStr = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = editAmountError,
                        supportingText = {
                            if (editAmountError) Text("Introduce un importe numérico mayor o igual a 0")
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_input_field")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { currentEditAmountStr = "50" },
                            modifier = Modifier.weight(1f)
                        ) { Text("50€") }
                        TextButton(
                            onClick = { currentEditAmountStr = "150" },
                            modifier = Modifier.weight(1f)
                        ) { Text("150€") }
                        TextButton(
                            onClick = { currentEditAmountStr = "300" },
                            modifier = Modifier.weight(1f)
                        ) { Text("300€") }
                        TextButton(
                            onClick = { currentEditAmountStr = "500" },
                            modifier = Modifier.weight(1f)
                        ) { Text("500€") }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = currentEditAmountStr.toDoubleOrNull() ?: 0.0
                        if (!editAmountError) {
                            if (amount > 0.0) {
                                viewModel.setBudget(catName, amount)
                            } else {
                                // If amount is empty or 0, remove the budget
                                viewModel.deleteBudget(catName)
                            }
                            activeEditingCategory = null
                        }
                    },
                    modifier = Modifier.testTag("budget_save_btn")
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeEditingCategory = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun BudgetCategoryCard(
    category: String,
    stats: BudgetAnalysis,
    onEditLimit: () -> Unit
) {
    val categoryIcon = when (category) {
        "Comida" -> Icons.Default.Restaurant
        "Transporte" -> Icons.Default.DirectionsCar
        "Vivienda" -> Icons.Default.Home
        "Servicios" -> Icons.Default.ElectricalServices
        "Entretenimiento" -> Icons.Default.ConfirmationNumber
        "Salud" -> Icons.Default.LocalHospital
        "Ropa & Compras" -> Icons.Default.Checkroom
        else -> Icons.Default.Category
    }

    val isBudgetSet = stats.limit > 0.0
    val progressAnimated by animateFloatAsState(
        targetValue = stats.percentSpent.coerceIn(0.0, 1.0).toFloat(),
        label = "progress"
    )

    // Alert Colors depending on spending limits
    val indicatorColor = when {
        !isBudgetSet -> MaterialTheme.colorScheme.primary
        stats.percentSpent > 1.0 -> MaterialTheme.colorScheme.error             // Red (Exceeded!)
        stats.percentSpent > 0.8 -> Color(0xFFFF9800)                            // Orange (>80%)
        else -> Color(0xFF4CAF50)                                                 // Green (Safe)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditLimit() }
            .testTag("budget_category_row_$category"),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(categoryIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(
                        text = category,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Configured limit badge
                if (isBudgetSet) {
                    Text(
                        text = "${String.format("%,.1f", stats.spent)} € / ${String.format("%,.1f", stats.limit)} €",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (stats.spent > stats.limit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onEditLimit() }
                    ) {
                        Text(
                            text = "Sin límite",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (isBudgetSet) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { progressAnimated },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = indicatorColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (stats.spent > stats.limit) {
                                "¡Límite excedido por ${String.format("%.2f", -stats.remaining)} €!"
                            } else {
                                "Te restan ${String.format("%.2f", stats.remaining)} €"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (stats.spent > stats.limit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (stats.spent > stats.limit) FontWeight.Bold else FontWeight.Normal
                        )

                        Text(
                            text = "${String.format("%.0f", stats.percentSpent * 100)}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = indicatorColor
                        )
                    }
                }
            } else {
                Text(
                    text = "Presiona para establecer un presupuesto máximo y prevenir excesos en este rubro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
