package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Expense
import com.example.ui.ExpenseViewModel

@Composable
fun AnalyticsScreen(viewModel: ExpenseViewModel) {
    val expenses by viewModel.allExpenses.collectAsState(initial = emptyList())

    val totalSpent = remember(expenses) {
        expenses.sumOf { it.amount }
    }

    if (expenses.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("analytics_screen_empty"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Text(
                    text = "No hay datos suficientes",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Registra algunos gastos (o genera datos de prueba en la pestaña Inicio) para ver tu análisis detallado de gastos.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("analytics_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Pie / Circle donut of general division ---
            item {
                DonutChartSection(expenses, totalSpent)
            }

            // --- Categoría Proporciones ---
            item {
                CategoryBreakdownSection(expenses, totalSpent)
            }

            // --- Distribución de Métodos de Pago ---
            item {
                PaymentMethodsSection(expenses, totalSpent)
            }

            // --- Estadísticas Hito / Análisis Extremo ---
            item {
                ExtremeStatsSection(expenses)
            }
        }
    }
}

@Composable
fun DonutChartSection(expenses: List<Expense>, totalSpent: Double) {
    // Group categories
    val categoryTotals = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { e -> e.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val categoryColors = remember {
        mapOf(
            "Comida" to Color(0xFF4CAF50),       // Green
            "Transporte" to Color(0xFF2196F3),   // Blue
            "Vivienda" to Color(0xFFFF9800),     // Orange
            "Servicios" to Color(0xFFE91E63),    // Pink
            "Entretenimiento" to Color(0xFF9C27B0), // Purple
            "Salud" to Color(0xFF00BCD4),        // Cyan
            "Ropa & Compras" to Color(0xFF3F51B5), // Indigo
            "Otros" to Color(0xFF9E9E9E)         // Grey
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Distribución por Categorías",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circular Canvas for donut simulation
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = 0f
                        categoryTotals.forEach { (category, value) ->
                            val sweepAngle = ((value / totalSpent) * 360f).toFloat()
                            val color = categoryColors[category] ?: Color.LightGray
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 16.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            )
                            startAngle += sweepAngle
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format("%.0f", totalSpent)}€",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Legend table on the right side
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoryTotals.take(4).forEach { (category, amount) ->
                        val color = categoryColors[category] ?: Color.LightGray
                        val percentage = (amount / totalSpent) * 100
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(color, CircleShape)
                            )
                            Text(
                                text = "$category (${String.format("%.1f", percentage)}%)",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (categoryTotals.size > 4) {
                        Text(
                            text = "+ ${categoryTotals.size - 4} categorías más",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownSection(expenses: List<Expense>, totalSpent: Double) {
    val categoryTotals = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { e -> e.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val categoryColors = remember {
        mapOf(
            "Comida" to Color(0xFF4CAF50),
            "Transporte" to Color(0xFF2196F3),
            "Vivienda" to Color(0xFFFF9800),
            "Servicios" to Color(0xFFE91E63),
            "Entretenimiento" to Color(0xFF9C27B0),
            "Salud" to Color(0xFF00BCD4),
            "Ropa & Compras" to Color(0xFF3F51B5),
            "Otros" to Color(0xFF9E9E9E)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Desglose de Categorías",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            categoryTotals.forEach { (category, amount) ->
                val percentage = (amount / totalSpent)
                val animatedProgress by animateFloatAsState(targetValue = percentage.toFloat(), label = "percentage")
                val color = categoryColors[category] ?: Color.LightGray

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${String.format("%,.2f", amount)} € (${String.format("%.1f", percentage * 100)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentMethodsSection(expenses: List<Expense>, totalSpent: Double) {
    val methodTotals = remember(expenses) {
        expenses.groupBy { it.paymentMethod }
            .mapValues { it.value.sumOf { e -> e.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Distribución de Pago",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            methodTotals.forEach { (method, amount) ->
                val percentage = (amount / totalSpent)
                val animatedProgress by animateFloatAsState(targetValue = percentage.toFloat(), label = "percentage")

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = method,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${String.format("%,.2f", amount)} € (${String.format("%.1f", percentage * 100)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ExtremeStatsSection(expenses: List<Expense>) {
    val maxExpense = remember(expenses) { expenses.maxByOrNull { it.amount } }
    val minExpense = remember(expenses) { expenses.minByOrNull { it.amount } }

    val recurringExpenses = remember(expenses) { expenses.filter { it.isRecurring } }
    val totalRecurringCost = remember(recurringExpenses) {
        // Group by title to identify monthly drains
        recurringExpenses.distinctBy { it.title }.sumOf { it.amount }
    }

    val averageSize = remember(expenses) {
        if (expenses.isNotEmpty()) expenses.map { it.amount }.average() else 0.0
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Estadísticas y Límites",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 1. Max Expense Row
            if (maxExpense != null) {
                StatHitoRow(
                    icon = Icons.Default.TrendingUp,
                    iconColor = MaterialTheme.colorScheme.error,
                    title = "Gasto Más Alto",
                    subtitle = maxExpense.title,
                    value = "${String.format("%.2f", maxExpense.amount)} €",
                    caption = maxExpense.getFormattedDate()
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            }

            // 2. Min Expense Row
            if (minExpense != null) {
                StatHitoRow(
                    icon = Icons.Default.TrendingDown,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = "Gasto Más Bajo",
                    subtitle = minExpense.title,
                    value = "${String.format("%.2f", minExpense.amount)} €",
                    caption = minExpense.getFormattedDate()
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            }

            // 3. Average Transaction size
            StatHitoRow(
                icon = Icons.Default.Calculate,
                iconColor = MaterialTheme.colorScheme.secondary,
                title = "Gasto Transaccional Medio",
                subtitle = "Importe promedio de tus tickets",
                value = "${String.format("%.2f", averageSize)} €",
                caption = "De ${expenses.size} compras registradas"
            )

            // 4. Recurring Subscription Drain (silent outflow)
            if (totalRecurringCost > 0.0) {
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                StatHitoRow(
                    icon = Icons.Default.ReceiptLong,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    title = "Abonos y Suscripciones",
                    subtitle = "Suma de gastos fijos mensuales",
                    value = "${String.format("%.2f", totalRecurringCost)} €",
                    caption = "Fuga fija de fondos calculada"
                )
            }
        }
    }
}

@Composable
fun StatHitoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    value: String,
    caption: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = iconColor.copy(alpha = 0.15f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        Text(
            value,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
