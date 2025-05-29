package com.example.gestaofeiracooperativa

import android.app.DatePickerDialog
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange // Ícone para o DatePicker
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriarNovaFeiraScreen(
    navController: NavHostController,
    onConfirmarCriacaoFeira: (feiraId: String, startDate: String, endDate: String) -> Unit // Callback para criar nova feira
) {
    var feiraIdInput by remember { mutableStateOf("") }
    var startDateInput by remember { mutableStateOf("") }
    var endDateInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // DatePicker para data de início
    val startDatePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth)
            startDateInput = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedCalendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // DatePicker para data de fim
    val endDatePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth)
            endDateInput = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedCalendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Criar Nova Feira") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { // Volta para a HomeScreen
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Insira os detalhes da nova feira:",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = feiraIdInput,
                onValueChange = { feiraIdInput = it.filter { char -> char.isDigit() } }, // Permite apenas dígitos
                label = { Text("Número da Feira (ex: 20)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Campo para Data de Início
            OutlinedTextField(
                value = startDateInput,
                onValueChange = { startDateInput = it }, // onValueChange para permitir colagem ou auto-preenchimento
                label = { Text("Data de Início (DD/MM/AAAA)") },
                readOnly = true, // Não permite digitar diretamente, apenas via DatePicker
                trailingIcon = {
                    IconButton(onClick = { startDatePickerDialog.show() }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Selecionar Data de Início")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Campo para Data de Fim
            OutlinedTextField(
                value = endDateInput,
                onValueChange = { endDateInput = it }, // onValueChange para permitir colagem ou auto-preenchimento
                label = { Text("Data de Fim (DD/MM/AAAA)") },
                readOnly = true, // Não permite digitar diretamente, apenas via DatePicker
                trailingIcon = {
                    IconButton(onClick = { endDatePickerDialog.show() }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Selecionar Data de Fim")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (feiraIdInput.isNotBlank() && startDateInput.isNotBlank() && endDateInput.isNotBlank()) {
                        // Opcional: Validar se as datas são válidas e se a data de fim é depois da de início
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        try {
                            val start = dateFormat.parse(startDateInput)
                            val end = dateFormat.parse(endDateInput)
                            if (start != null && end != null && end.before(start)) {
                                Toast.makeText(context, "A data de fim não pode ser antes da data de início.", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Formato de data inválido. Use DD/MM/AAAA.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        onConfirmarCriacaoFeira(feiraIdInput, startDateInput, endDateInput)
                        // A navegação será feita pelo AppNavigation após confirmar a criação
                    } else {
                        Toast.makeText(context, "Preencha todos os campos.", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = feiraIdInput.isNotBlank() && startDateInput.isNotBlank() && endDateInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Criar Nova Feira e Gerenciar")
            }
        }
    }
}