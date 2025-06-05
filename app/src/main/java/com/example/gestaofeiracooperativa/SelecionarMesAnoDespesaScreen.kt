package com.example.gestaofeiracooperativa

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import java.util.Calendar
import java.io.File

// Imports necessários para esta tela
// import com.example.gestaofeiracooperativa.StandardTopAppBar
// import com.example.gestaofeiracooperativa.RelatorioDespesaViewModel
// import com.example.gestaofeiracooperativa.RelatorioDespesaViewModelFactory
// import com.example.gestaofeiracooperativa.MyApplication
// import com.example.gestaofeiracooperativa.UiState
// import com.example.gestaofeiracooperativa.compartilharPdf


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelecionarMesAnoDespesaScreen(
    navController: NavHostController,
    viewModel: RelatorioDespesaViewModel = viewModel(
        factory = RelatorioDespesaViewModelFactory(
            (LocalContext.current.applicationContext as MyApplication).feiraRepository,
            (LocalContext.current.applicationContext as MyApplication).despesaFeiraRepository,
            (LocalContext.current.applicationContext as MyApplication).itemDespesaRepository
        )
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Estados para controlar a seleção nos dropdowns
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    var anoSelecionado by remember { mutableStateOf(currentYear) }
    var mesSelecionado by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }

    var expandedAno by remember { mutableStateOf(false) }
    var expandedMes by remember { mutableStateOf(false) }

    val anosDisponiveis = (currentYear downTo currentYear - 5).toList()
    val mesesDisponiveis = listOf(
        "Janeiro" to 1, "Fevereiro" to 2, "Março" to 3, "Abril" to 4, "Maio" to 5, "Junho" to 6,
        "Julho" to 7, "Agosto" to 8, "Setembro" to 9, "Outubro" to 10, "Novembro" to 11, "Dezembro" to 12
    )
    val nomeMesSelecionado = mesesDisponiveis.find { it.second == mesSelecionado }?.first ?: ""


    // LaunchedEffect para lidar com o resultado do ViewModel (sucesso, erro)
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UiState.Success<File> -> {
                // O PDF foi gerado, agora vamos compartilhá-lo
                val pdfFile = state.data
                val authorityString = context.applicationContext.packageName + ".provider"
                val tituloChooser = "Relatório Mensal de Despesas $mesSelecionado/$anoSelecionado"
                compartilharPdf(context, pdfFile, authorityString, tituloChooser)
                viewModel.resetUiState() // Reseta o estado para permitir nova geração
            }
            is UiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetUiState()
            }
            else -> {
                // Idle ou Loading, não faz nada aqui
            }
        }
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = "Relatório Mensal de Despesas",
                canNavigateBack = true,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Selecione o período para gerar o relatório de despesas mensal consolidado.",
                style = MaterialTheme.typography.bodyLarge
            )

            // Dropdown para selecionar o Mês
            ExposedDropdownMenuBox(
                expanded = expandedMes,
                onExpandedChange = { expandedMes = !expandedMes }
            ) {
                OutlinedTextField(
                    value = nomeMesSelecionado,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Mês") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMes) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedMes,
                    onDismissRequest = { expandedMes = false }
                ) {
                    mesesDisponiveis.forEach { (nome, numero) ->
                        DropdownMenuItem(
                            text = { Text(nome) },
                            onClick = {
                                mesSelecionado = numero
                                expandedMes = false
                            }
                        )
                    }
                }
            }

            // Dropdown para selecionar o Ano
            ExposedDropdownMenuBox(
                expanded = expandedAno,
                onExpandedChange = { expandedAno = !expandedAno }
            ) {
                OutlinedTextField(
                    value = anoSelecionado.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ano") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAno) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedAno,
                    onDismissRequest = { expandedAno = false }
                ) {
                    anosDisponiveis.forEach { ano ->
                        DropdownMenuItem(
                            text = { Text(ano.toString()) },
                            onClick = {
                                anoSelecionado = ano
                                expandedAno = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    // Inicia o processo de geração do PDF através do ViewModel
                    viewModel.gerarRelatorioMensal(context, anoSelecionado, mesSelecionado)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                // Desabilita o botão enquanto o PDF está sendo gerado
                enabled = uiState !is UiState.Loading
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState is UiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Text("Gerando...")
                    } else {
                        Text("Gerar Relatório PDF")
                    }
                }
            }
        }
    }
}