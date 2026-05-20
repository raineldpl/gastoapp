package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.Expense
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditExpenseDialog(
    expenseToEdit: Expense? = null,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    val context = LocalContext.current
    val isEditing = expenseToEdit != null

    // Input States
    var title by remember { mutableStateOf(expenseToEdit?.title ?: "") }
    var amountStr by remember { mutableStateOf(expenseToEdit?.amount?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf(expenseToEdit?.category ?: "Comida") }
    var selectedPaymentMethod by remember { mutableStateOf(expenseToEdit?.paymentMethod ?: "Tarjeta de Débito") }
    var details by remember { mutableStateOf(expenseToEdit?.details ?: "") }
    var tags by remember { mutableStateOf(expenseToEdit?.tags ?: "") }
    var isRecurring by remember { mutableStateOf(expenseToEdit?.isRecurring ?: false) }
    var dateMillis by remember { mutableStateOf(expenseToEdit?.dateMillis ?: System.currentTimeMillis()) }

    // Dropdown state for Category
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    // Validation
    var titleError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    val categories = listOf(
        "Comida", "Transporte", "Vivienda", "Servicios",
        "Entretenimiento", "Salud", "Ropa & Compras", "Otros"
    )

    val paymentMethods = listOf(
        "Efectivo", "Tarjeta de Débito", "Tarjeta de Crédito", "Transferencia", "Otro"
    )

    // Formatter for selected date button
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Editar Gasto" else "Registrar Nuevo Gasto",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- Title Field ---
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = it.isBlank()
                    },
                    label = { Text("Título / Concepto *") },
                    placeholder = { Text("Ej. Supermercado, Almuerzo") },
                    leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) },
                    isError = titleError,
                    supportingText = {
                        if (titleError) Text("El título no puede estar vacío", color = MaterialTheme.colorScheme.error)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input")
                )

                // --- Amount Field ---
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { input ->
                        // Only allow numbers and decimal separator
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        amountStr = filtered
                        amountError = filtered.toDoubleOrNull() == null || filtered.toDouble() <= 0.0
                    },
                    label = { Text("Monto (€) *") },
                    placeholder = { Text("0.00") },
                    leadingIcon = { Icon(Icons.Default.Euro, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = {
                        if (amountError) Text("Introduce un importe válido mayor que 0", color = MaterialTheme.colorScheme.error)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                // --- Category Selector (M3 Dropdown) ---
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = isCategoryDropdownExpanded,
                        onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoría") },
                            leadingIcon = {
                                val categoryIcon = when (selectedCategory) {
                                    "Comida" -> Icons.Default.Restaurant
                                    "Transporte" -> Icons.Default.DirectionsCar
                                    "Vivienda" -> Icons.Default.Home
                                    "Servicios" -> Icons.Default.ElectricalServices
                                    "Entretenimiento" -> Icons.Default.ConfirmationNumber
                                    "Salud" -> Icons.Default.LocalHospital
                                    "Ropa & Compras" -> Icons.Default.Checkroom
                                    else -> Icons.Default.Category
                                }
                                Icon(categoryIcon, contentDescription = null)
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("expense_category_selector")
                        )
                        ExposedDropdownMenu(
                            expanded = isCategoryDropdownExpanded,
                            onDismissRequest = { isCategoryDropdownExpanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        isCategoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // --- Date Selection ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fecha:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = dateMillis
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(Calendar.YEAR, year)
                                    newCal.set(Calendar.MONTH, month)
                                    newCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    dateMillis = newCal.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.testTag("expense_date_picker_btn")
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(dateFormatter.format(Date(dateMillis)))
                    }

                    // Quick Today/Yesterday offsets
                    TextButton(
                        onClick = { dateMillis = System.currentTimeMillis() }
                    ) {
                        Text("Hoy")
                    }
                }

                // --- Payment Method Selector (Horizontal Rows/Chips) ---
                Text(
                    text = "Método de Pago",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    paymentMethods.forEach { method ->
                        FilterChip(
                            selected = selectedPaymentMethod == method,
                            onClick = { selectedPaymentMethod = method },
                            label = { Text(method) },
                            leadingIcon = if (selectedPaymentMethod == method) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.testTag("paymethod_chip_$method")
                        )
                    }
                }

                // --- Details Field ---
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Notas / Detalles adicionales") },
                    placeholder = { Text("Ej. Lugar, notas sobre el gasto, etc.") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_details_input")
                )

                // --- Tags Field ---
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Etiquetas (separadas por comas)") },
                    placeholder = { Text("Ej. super, antojo, fin_de_semana") },
                    leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null) },
                    supportingText = { Text("Ayuda a clasificar tus gastos de forma hiper-detallada") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_tags_input")
                )

                // --- Recurring Switch ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "Recurrent monthly",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Gasto Recurrente",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Suscripciones o pagos fijos mensuales",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it },
                        modifier = Modifier.testTag("expense_recurring_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalAmount = amountStr.toDoubleOrNull() ?: 0.0
                    if (title.isBlank()) {
                        titleError = true
                    }
                    if (finalAmount <= 0) {
                        amountError = true
                    }

                    if (title.isNotBlank() && finalAmount > 0.0) {
                        val formattedTags = tags.split(",")
                            .map { it.trim().lowercase() }
                            .filter { it.isNotEmpty() }
                            .joinToString(",")

                        val expense = Expense(
                            id = expenseToEdit?.id ?: 0,
                            title = title.trim(),
                            amount = finalAmount,
                            category = selectedCategory,
                            dateMillis = dateMillis,
                            paymentMethod = selectedPaymentMethod,
                            details = details.trim(),
                            tags = formattedTags,
                            isRecurring = isRecurring
                        )
                        onSave(expense)
                    }
                },
                modifier = Modifier.testTag("expense_save_btn")
            ) {
                Text(if (isEditing) "Guardar Cambios" else "Registrar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("expense_dialog_cancel_btn")
            ) {
                Text("Cancelar")
            }
        }
    )
}
