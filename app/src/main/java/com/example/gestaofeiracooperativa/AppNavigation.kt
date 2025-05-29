package com.example.gestaofeiracooperativa

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

// IMPORTS DE TELAS (CERTIFIQUE-SE QUE ESTES ARQUIVOS EXISTEM E ESTÃO NO MESMO PACOTE)
import com.example.gestaofeiracooperativa.HomeScreen
import com.example.gestaofeiracooperativa.ListaFeirasSalvasScreen
import com.example.gestaofeiracooperativa.CriarNovaFeiraScreen
import com.example.gestaofeiracooperativa.GerenciarFeiraScreen
import com.example.gestaofeiracooperativa.LancamentoScreen
import com.example.gestaofeiracooperativa.PerdasTotaisScreen
import com.example.gestaofeiracooperativa.ResultadosFeiraScreen

object AppRoutes {
    const val HOME_SCREEN = "homeScreen"
    const val LISTA_FEIRAS_SALVAS = "listaFeirasSalvas"
    const val CRIAR_NOVA_FEIRA = "criarNovaFeira"
    const val GERENCIAR_FEIRA_PATTERN = "gerenciarFeira/{feiraId}"
    const val LANCAMENTO_PRODUTOS_ROUTE_PATTERN = "lancamentoProdutos/{feiraId}/{agricultorId}"
    const val LANCAMENTO_PERDAS_TOTAIS_PATTERN = "lancamentoPerdasTotais/{feiraId}"
    const val RESULTADOS_FEIRA_PATTERN = "resultadosFeira/{feiraId}"

    fun gerenciarFeiraRoute(feiraId: String) = "gerenciarFeira/$feiraId"
    fun lancamentoProdutosRoute(feiraId: String, agricultorId: String) = "lancamentoProdutos/$feiraId/$agricultorId"
    fun lancamentoPerdasTotaisRoute(feiraId: String) = "lancamentoPerdasTotais/$feiraId"
    fun resultadosFeiraRoute(feiraId: String) = "resultadosFeira/$feiraId"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current

    val catalogoProdutos by remember { mutableStateOf(loadProductsFromAssets(context, "CAD PROD.csv")) }

    var feiraEmProcessamento by remember { mutableStateOf<DadosCompletosFeira?>(null) }


    var showDialogFeiraJaExiste by remember { mutableStateOf(false) }
    var novaFeiraIdParaCriar by remember { mutableStateOf("") }
    var novaStartDateParaCriar by remember { mutableStateOf("") }
    var novaEndDateParaCriar by remember { mutableStateOf("") }


    fun prepararFeiraParaGerenciamento(
        feiraId: String,
        startDate: String? = null,
        endDate: String? = null
    ) {
        val dadosCarregados = carregarDadosFeira(context, feiraId)
        if (dadosCarregados != null) {
            feiraEmProcessamento = dadosCarregados
            Toast.makeText(context, "Feira $feiraId carregada!", Toast.LENGTH_SHORT).show()
        } else {
            if (startDate != null && endDate != null && startDate.isNotBlank() && endDate.isNotBlank()) {
                feiraEmProcessamento = DadosCompletosFeira(
                    fairDetails = FairDetails(feiraId, startDate, endDate),
                    entradasTodosAgricultores = mutableStateMapOf(),
                    perdasTotaisDaFeira = mutableStateListOf(),
                    resultadoGeralCalculado = null
                )
                Toast.makeText(context, "Iniciando nova Feira $feiraId.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Erro: Detalhes da feira incompletos para iniciar. Verifique datas.", Toast.LENGTH_LONG).show()
                feiraEmProcessamento = null
                return
            }
        }
    }


    if (showDialogFeiraJaExiste) {
        AlertDialog(
            onDismissRequest = { showDialogFeiraJaExiste = false },
            title = { Text("Feira Já Existe") },
            text = { Text("A Feira Nº $novaFeiraIdParaCriar (${novaStartDateParaCriar} - ${novaEndDateParaCriar}) já possui dados salvos. Deseja carregar esses dados ou iniciar uma nova feira (sobrescreverá os dados anteriores ao salvar)?") },
            confirmButton = {
                TextButton(onClick = {
                    prepararFeiraParaGerenciamento(novaFeiraIdParaCriar, null, null)
                    showDialogFeiraJaExiste = false
                    navController.navigate(AppRoutes.gerenciarFeiraRoute(novaFeiraIdParaCriar))
                }) { Text("Carregar Dados") }
            },
            dismissButton = {
                TextButton(onClick = {
                    prepararFeiraParaGerenciamento(novaFeiraIdParaCriar, novaStartDateParaCriar, novaEndDateParaCriar)
                    showDialogFeiraJaExiste = false
                    navController.navigate(AppRoutes.gerenciarFeiraRoute(novaFeiraIdParaCriar))
                }) { Text("Iniciar Nova") }
            }
        )
    }

    NavHost(navController = navController, startDestination = AppRoutes.HOME_SCREEN) {

        composable(AppRoutes.HOME_SCREEN) {
            HomeScreen(
                onNavigateToNovaFeira = {
                    navController.navigate(AppRoutes.CRIAR_NOVA_FEIRA)
                },
                onNavigateToFeirasSalvas = {
                    navController.navigate(AppRoutes.LISTA_FEIRAS_SALVAS)
                }
            )
        }

        composable(AppRoutes.LISTA_FEIRAS_SALVAS) {
            ListaFeirasSalvasScreen(
                navController = navController,
                onAbrirFeira = { idFeira ->
                    prepararFeiraParaGerenciamento(idFeira, null, null)
                    if (feiraEmProcessamento != null) {
                        navController.navigate(AppRoutes.gerenciarFeiraRoute(idFeira))
                    }
                },
                onDeletarFeira = { idFeiraDeletada ->
                    if (deletarDadosFeira(context, idFeiraDeletada)) {
                        Toast.makeText(context, "Feira $idFeiraDeletada deletada.", Toast.LENGTH_SHORT).show()
                        if (feiraEmProcessamento?.fairDetails?.feiraId == idFeiraDeletada) {
                            feiraEmProcessamento = null
                        }
                    } else {
                        Toast.makeText(context, "Falha ao deletar Feira $idFeiraDeletada.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        composable(AppRoutes.CRIAR_NOVA_FEIRA) {
            CriarNovaFeiraScreen(
                navController = navController,
                onConfirmarCriacaoFeira = { feiraId, startDate, endDate ->
                    if (listarFeirasSalvas(context).contains(feiraId)) {
                        novaFeiraIdParaCriar = feiraId
                        novaStartDateParaCriar = startDate
                        novaEndDateParaCriar = endDate
                        showDialogFeiraJaExiste = true
                    } else {
                        prepararFeiraParaGerenciamento(feiraId, startDate, endDate)
                        if (feiraEmProcessamento != null) {
                            navController.navigate(AppRoutes.gerenciarFeiraRoute(feiraId))
                        }
                    }
                }
            )
        }

        composable(
            route = AppRoutes.GERENCIAR_FEIRA_PATTERN,
            arguments = listOf(navArgument("feiraId") { type = NavType.StringType })
        ) { backStackEntry ->
            val feiraIdArg = backStackEntry.arguments?.getString("feiraId") ?: return@composable

            if (feiraEmProcessamento == null || feiraEmProcessamento!!.fairDetails.feiraId != feiraIdArg) {
                prepararFeiraParaGerenciamento(feiraIdArg, null, null)
                if (feiraEmProcessamento == null) {
                    Toast.makeText(context, "Não foi possível carregar a Feira $feiraIdArg.", Toast.LENGTH_LONG).show()
                    navController.popBackStack(AppRoutes.HOME_SCREEN, inclusive = false)
                    return@composable
                }
            }
            val currentFeira = feiraEmProcessamento!!

            GerenciarFeiraScreen(
                navController = navController,
                feiraDetails = currentFeira.fairDetails,
                onNavigateToLancamentos = { agricultorId ->
                    navController.navigate(AppRoutes.lancamentoProdutosRoute(currentFeira.fairDetails.feiraId, agricultorId))
                },
                onNavigateToPerdasTotais = {
                    navController.navigate(AppRoutes.lancamentoPerdasTotaisRoute(currentFeira.fairDetails.feiraId))
                },
                onNavigateToResultados = {
                    if (currentFeira.resultadoGeralCalculado == null) {
                        val temEntradas = currentFeira.entradasTodosAgricultores.values.any { it.isNotEmpty() }
                        val temPerdas = currentFeira.perdasTotaisDaFeira.isNotEmpty()

                        if (temEntradas || temPerdas) {
                            feiraEmProcessamento = currentFeira.copy(
                                resultadoGeralCalculado = calcularResultadosFeira(
                                    fairDetails = currentFeira.fairDetails,
                                    entradasTodosAgricultores = currentFeira.entradasTodosAgricultores,
                                    perdasTotaisDaFeira = currentFeira.perdasTotaisDaFeira,
                                    catalogoProdutos = catalogoProdutos
                                )
                            )
                            navController.navigate(AppRoutes.resultadosFeiraRoute(currentFeira.fairDetails.feiraId))
                        } else {
                            Toast.makeText(context, "Não há entradas ou perdas lançadas para a Feira ${currentFeira.fairDetails.feiraId}.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        navController.navigate(AppRoutes.resultadosFeiraRoute(currentFeira.fairDetails.feiraId))
                    }
                },
                onSalvarFeira = {
                    if (salvarDadosFeira(context, currentFeira)) {
                        Toast.makeText(context, "Feira ${currentFeira.fairDetails.feiraId} salva!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Erro ao salvar Feira ${currentFeira.fairDetails.feiraId}.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        composable(
            route = AppRoutes.LANCAMENTO_PRODUTOS_ROUTE_PATTERN,
            arguments = listOf(
                navArgument("feiraId") { type = NavType.StringType },
                navArgument("agricultorId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val feiraIdArg = backStackEntry.arguments?.getString("feiraId") ?: return@composable
            val agricultorIdArg = backStackEntry.arguments?.getString("agricultorId") ?: return@composable

            if (feiraEmProcessamento == null || feiraEmProcessamento!!.fairDetails.feiraId != feiraIdArg) {
                prepararFeiraParaGerenciamento(feiraIdArg, null, null)
                if (feiraEmProcessamento == null) {
                    Toast.makeText(context, "Feira $feiraIdArg não encontrada ou erro ao carregar.", Toast.LENGTH_LONG).show()
                    navController.popBackStack()
                    return@composable
                }
            }
            val currentFeira = feiraEmProcessamento!!

            if (catalogoProdutos.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text("Erro: Catálogo de produtos não carregado.") }
            } else {
                LancamentoScreen(
                    feiraId = currentFeira.fairDetails.feiraId,
                    agricultorId = agricultorIdArg,
                    catalogoProdutos = catalogoProdutos,
                    entradasIniciais = currentFeira.entradasTodosAgricultores[agricultorIdArg] ?: emptyList(),
                    onFinalizar = { novasEntradasDoAgricultor ->
                        feiraEmProcessamento = currentFeira.copy(
                            entradasTodosAgricultores = currentFeira.entradasTodosAgricultores.toMutableMap().apply {
                                put(agricultorIdArg, novasEntradasDoAgricultor)
                            }
                        )
                        // CORREÇÃO AQUI: Atualiza resultadoGeralCalculado para null na mesma cópia
                        feiraEmProcessamento = feiraEmProcessamento!!.copy(resultadoGeralCalculado = null)
                        Toast.makeText(context, "Entradas do Agr. $agricultorIdArg atualizadas. Salve a feira!", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    },
                    onVoltar = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = AppRoutes.LANCAMENTO_PERDAS_TOTAIS_PATTERN,
            arguments = listOf(navArgument("feiraId") { type = NavType.StringType })
        ) { backStackEntry ->
            val feiraIdArg = backStackEntry.arguments?.getString("feiraId") ?: return@composable

            if (feiraEmProcessamento == null || feiraEmProcessamento!!.fairDetails.feiraId != feiraIdArg) {
                prepararFeiraParaGerenciamento(feiraIdArg, null, null)
                if (feiraEmProcessamento == null) {
                    Toast.makeText(context, "Feira $feiraIdArg não encontrada ou erro ao carregar.", Toast.LENGTH_LONG).show()
                    navController.popBackStack()
                    return@composable
                }
            }
            val currentFeira = feiraEmProcessamento!!

            if (catalogoProdutos.isEmpty()) { /* ... erro catálogo ... */ } else {
                PerdasTotaisScreen(
                    feiraId = currentFeira.fairDetails.feiraId,
                    catalogoProdutos = catalogoProdutos,
                    perdasIniciais = currentFeira.perdasTotaisDaFeira.toList(),
                    onFinalizarPerdas = { novasPerdasDaFeira ->
                        feiraEmProcessamento = currentFeira.copy(
                            perdasTotaisDaFeira = novasPerdasDaFeira
                        )
                        // CORREÇÃO AQUI: Atualiza resultadoGeralCalculado para null na mesma cópia
                        feiraEmProcessamento = feiraEmProcessamento!!.copy(resultadoGeralCalculado = null)
                        Toast.makeText(context, "Perdas da Feira ${currentFeira.fairDetails.feiraId} atualizadas. Salve a feira!", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    },
                    onVoltar = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = AppRoutes.RESULTADOS_FEIRA_PATTERN,
            arguments = listOf(navArgument("feiraId") { type = NavType.StringType })
        ) { backStackEntry ->
            val feiraIdArg = backStackEntry.arguments?.getString("feiraId") ?: return@composable

            if (feiraEmProcessamento == null || feiraEmProcessamento!!.fairDetails.feiraId != feiraIdArg) {
                prepararFeiraParaGerenciamento(feiraIdArg, null, null)
                if (feiraEmProcessamento == null) {
                    Toast.makeText(context, "Feira $feiraIdArg não encontrada ou erro ao carregar.", Toast.LENGTH_LONG).show()
                    navController.popBackStack()
                    return@composable
                }
            }
            val currentFeira = feiraEmProcessamento!!

            if (currentFeira.resultadoGeralCalculado == null) {
                val novoResultado = calcularResultadosFeira(
                    fairDetails = currentFeira.fairDetails,
                    entradasTodosAgricultores = currentFeira.entradasTodosAgricultores,
                    perdasTotaisDaFeira = currentFeira.perdasTotaisDaFeira,
                    catalogoProdutos = catalogoProdutos
                )
                feiraEmProcessamento = currentFeira.copy(
                    resultadoGeralCalculado = novoResultado
                )
            }

            currentFeira.resultadoGeralCalculado?.let { resultado ->
                ResultadosFeiraScreen(
                    resultadoGeralFeira = resultado,
                    onVoltar = { navController.popBackStack() },
                    onSalvarFeira = {
                        if (salvarDadosFeira(context, currentFeira)) {
                            Toast.makeText(context, "Feira ${currentFeira.fairDetails.feiraId} salva!", Toast.LENGTH_SHORT).show()
                            navController.navigate(AppRoutes.HOME_SCREEN) {
                                popUpTo(AppRoutes.HOME_SCREEN) { inclusive = true }
                            }
                        } else {
                            Toast.makeText(context, "Erro ao salvar Feira ${currentFeira.fairDetails.feiraId}.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } ?: run {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Não foi possível calcular os resultados para a Feira ${currentFeira.fairDetails.feiraId}.")
                    Text("Verifique se há entradas ou perdas lançadas.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { navController.popBackStack() }) { Text("Voltar") }
                }
            }
        }
    }
}
