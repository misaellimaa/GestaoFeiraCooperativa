package com.example.gestaofeiracooperativa

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch

// Imports de Telas
import com.example.gestaofeiracooperativa.HomeScreen
import com.example.gestaofeiracooperativa.ListaFeirasSalvasScreen
import com.example.gestaofeiracooperativa.CriarNovaFeiraScreen
import com.example.gestaofeiracooperativa.GerenciarFeiraScreen
import com.example.gestaofeiracooperativa.LancamentoScreen
import com.example.gestaofeiracooperativa.PerdasTotaisScreen
import com.example.gestaofeiracooperativa.ResultadosFeiraScreen
import com.example.gestaofeiracooperativa.CadastroProdutosScreen
import com.example.gestaofeiracooperativa.CadastroAgricultoresScreen
import com.example.gestaofeiracooperativa.CadastroItensDespesaScreen
import com.example.gestaofeiracooperativa.LancamentoDespesasFeiraScreen
import com.example.gestaofeiracooperativa.DistribuirSobrasScreen
import com.example.gestaofeiracooperativa.SelecionarMesAnoDespesaScreen

// Imports de Dados e Lógica
import com.example.gestaofeiracooperativa.MyApplication
import com.example.gestaofeiracooperativa.FeiraRepository
import com.example.gestaofeiracooperativa.DadosCompletosFeira
import com.example.gestaofeiracooperativa.FairDetails
import com.example.gestaofeiracooperativa.calcularResultadosFeira

object AppRoutes {
    const val HOME_SCREEN = "homeScreen"
    const val LISTA_FEIRAS_SALVAS = "listaFeirasSalvas"
    const val CRIAR_NOVA_FEIRA = "criarNovaFeira"
    const val GERENCIAR_FEIRA_PATTERN = "gerenciarFeira/{feiraId}?startDate={startDate}&endDate={endDate}"
    const val LANCAMENTO_PRODUTOS_ROUTE_PATTERN = "lancamentoProdutos/{feiraId}/{agricultorId}"
    const val LANCAMENTO_PERDAS_TOTAIS_PATTERN = "lancamentoPerdasTotais/{feiraId}"
    const val RESULTADOS_FEIRA_PATTERN = "resultadosFeira/{feiraId}"
    const val CADASTRO_PRODUTOS = "cadastroProdutos"
    const val CADASTRO_AGRICULTORES = "cadastroAgricultores"
    const val CADASTRO_ITENS_DESPESA = "cadastroItensDespesa"
    const val LANCAMENTO_DESPESAS_FEIRA_PATTERN = "lancamentoDespesasFeira/{feiraId}"
    const val SELECIONAR_MES_ANO_DESPESA = "selecionarMesAnoDespesa"
    const val DISTRIBUIR_SOBRAS_PATTERN = "distribuirSobras/{feiraIdAtual}"

    fun gerenciarFeiraRoute(feiraId: String, startDate: String? = null, endDate: String? = null): String {
        return if (startDate != null && endDate != null) {
            "gerenciarFeira/$feiraId?startDate=$startDate&endDate=$endDate"
        } else {
            "gerenciarFeira/$feiraId"
        }
    }
    fun lancamentoProdutosRoute(feiraId: String, agricultorId: String) = "lancamentoProdutos/$feiraId/$agricultorId"
    fun lancamentoPerdasTotaisRoute(feiraId: String) = "lancamentoPerdasTotais/$feiraId"
    fun resultadosFeiraRoute(feiraId: String) = "resultadosFeira/$feiraId"
    fun lancamentoDespesasFeiraRoute(feiraId: String) = "lancamentoDespesasFeira/$feiraId"
    fun distribuirSobrasRoute(feiraIdAtual: String) = "distribuirSobras/$feiraIdAtual"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val application = context.applicationContext as MyApplication

    // Repositórios
    val produtoRepository = application.produtoRepository
    val agricultorRepository = application.agricultorRepository
    val feiraRepository = application.feiraRepository
    val itemDespesaRepository = application.itemDespesaRepository
    val despesaFeiraRepository = application.despesaFeiraRepository
    val entradaRepository = application.entradaRepository
    val perdaRepository = application.perdaRepository

    // Estados
    val todosOsAgricultoresState by agricultorRepository.getAllAgricultores().collectAsState(initial = emptyList())
    val todosOsProdutosState by produtoRepository.getAllProducts().collectAsState(initial = emptyList())

    var feiraEmProcessamento by remember { mutableStateOf<DadosCompletosFeira?>(null) }
    var isCurrentFeiraPersisted by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var showDialogFeiraJaExiste by remember { mutableStateOf(false) }
    var novaFeiraIdParaCriar by remember { mutableStateOf("") }

    fun carregarOuIniciarNovaFeiraState(feiraId: String, startDateForNew: String?, endDateForNew: String?) {
        scope.launch {
            val dadosCarregados = feiraRepository.carregarDadosCompletosFeira(feiraId)
            if (dadosCarregados != null) {
                feiraEmProcessamento = dadosCarregados
                isCurrentFeiraPersisted = true
                Toast.makeText(context, "Feira $feiraId carregada.", Toast.LENGTH_SHORT).show()
            } else if (startDateForNew != null && endDateForNew != null) {
                feiraEmProcessamento = DadosCompletosFeira(fairDetails = FairDetails(feiraId, startDateForNew, endDateForNew))
                isCurrentFeiraPersisted = false
            } else {
                Toast.makeText(context, "Falha ao carregar Feira $feiraId.", Toast.LENGTH_LONG).show()
                navController.popBackStack()
            }
        }
    }

    if (showDialogFeiraJaExiste) {
        AlertDialog(
            onDismissRequest = { showDialogFeiraJaExiste = false },
            title = { Text("Feira Já Existe") },
            text = { Text("A Feira Nº $novaFeiraIdParaCriar já existe. Deseja carregar os dados salvos?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialogFeiraJaExiste = false
                    navController.navigate(AppRoutes.gerenciarFeiraRoute(novaFeiraIdParaCriar))
                }) { Text("Sim, Carregar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialogFeiraJaExiste = false }) { Text("Não, Cancelar") }
            }
        )
    }

    NavHost(navController = navController, startDestination = AppRoutes.HOME_SCREEN) {

        composable(AppRoutes.HOME_SCREEN) {
            HomeScreen(
                onNavigateToNovaFeira = { navController.navigate(AppRoutes.CRIAR_NOVA_FEIRA) },
                onNavigateToFeirasSalvas = { navController.navigate(AppRoutes.LISTA_FEIRAS_SALVAS) },
                onNavigateToCadastroProdutos = { navController.navigate(AppRoutes.CADASTRO_PRODUTOS) },
                onNavigateToCadastroAgricultores = { navController.navigate(AppRoutes.CADASTRO_AGRICULTORES) },
                onNavigateToCadastroItensDespesa = { navController.navigate(AppRoutes.CADASTRO_ITENS_DESPESA) },
                onNavigateToRelatorioDespesas = { navController.navigate(AppRoutes.SELECIONAR_MES_ANO_DESPESA) }
            )
        }

        composable(AppRoutes.LISTA_FEIRAS_SALVAS) {
            val listaDeFairDetailsState by feiraRepository.getTodasAsFeirasInfo().collectAsState(initial = emptyList())
            ListaFeirasSalvasScreen(
                navController = navController,
                feirasSalvasDetails = listaDeFairDetailsState,
                onAbrirFeira = { feiraId -> navController.navigate(AppRoutes.gerenciarFeiraRoute(feiraId)) },
                onDeletarFeira = { idFeiraDeletada ->
                    scope.launch {
                        if (feiraRepository.deletarFeira(idFeiraDeletada)) {
                            Toast.makeText(context, "Feira $idFeiraDeletada deletada.", Toast.LENGTH_SHORT).show()
                            if (feiraEmProcessamento?.fairDetails?.feiraId == idFeiraDeletada) {
                                feiraEmProcessamento = null; isCurrentFeiraPersisted = false
                            }
                        } else { Toast.makeText(context, "Falha ao deletar Feira $idFeiraDeletada.", Toast.LENGTH_SHORT).show() }
                    }
                }
            )
        }

        composable(AppRoutes.CRIAR_NOVA_FEIRA) {
            CriarNovaFeiraScreen(
                navController = navController,
                onConfirmarCriacaoFeira = { feiraId, startDate, endDate ->
                    scope.launch {
                        if (feiraRepository.feiraExiste(feiraId)) {
                            novaFeiraIdParaCriar = feiraId
                            showDialogFeiraJaExiste = true
                        } else {
                            val novaFeira = DadosCompletosFeira(fairDetails = FairDetails(feiraId, startDate, endDate))
                            if (feiraRepository.salvarDadosCompletosFeira(novaFeira)) {
                                Toast.makeText(context, "Feira Nº $feiraId criada e salva!", Toast.LENGTH_SHORT).show()
                                navController.navigate(AppRoutes.gerenciarFeiraRoute(feiraId)) {
                                    popUpTo(AppRoutes.HOME_SCREEN)
                                }
                            } else {
                                Toast.makeText(context, "Erro ao criar a nova feira.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            )
        }

        composable(route = AppRoutes.GERENCIAR_FEIRA_PATTERN, arguments = listOf(navArgument("feiraId") { type = NavType.StringType },navArgument("startDate") { type = NavType.StringType; nullable = true },navArgument("endDate") { type = NavType.StringType; nullable = true })) { backStackEntry ->
            val feiraIdArg = backStackEntry.arguments?.getString("feiraId") ?: return@composable
            val startDateArg = backStackEntry.arguments?.getString("startDate")
            val endDateArg = backStackEntry.arguments?.getString("endDate")

            LaunchedEffect(key1 = feiraIdArg) { carregarOuIniciarNovaFeiraState(feiraIdArg, startDateArg, endDateArg) }

            val currentFeira = feiraEmProcessamento
            if (currentFeira != null && currentFeira.fairDetails.feiraId == feiraIdArg) {
                GerenciarFeiraScreen(
                    navController = navController, feiraDetails = currentFeira.fairDetails, listaDeAgricultores = todosOsAgricultoresState,
                    isFeiraPersistida = isCurrentFeiraPersisted,
                    onNavigateToLancamentos = { agricultorId -> navController.navigate(AppRoutes.lancamentoProdutosRoute(currentFeira.fairDetails.feiraId, agricultorId)) },
                    onNavigateToDespesasFeira = { idFeira -> navController.navigate(AppRoutes.lancamentoDespesasFeiraRoute(idFeira)) },
                    onNavigateToPerdasTotais = { navController.navigate(AppRoutes.lancamentoPerdasTotaisRoute(currentFeira.fairDetails.feiraId)) },
                    onNavigateToDistribuirSobras = { feiraIdAtual -> navController.navigate(AppRoutes.distribuirSobrasRoute(feiraIdAtual)) },
                    onNavigateToResultados = {
                        scope.launch {
                            val feiraParaProcessar = feiraEmProcessamento ?: return@launch
                            val resultadoCalculado = calcularResultadosFeira(
                                fairDetails = feiraParaProcessar.fairDetails, entradasTodosAgricultores = feiraParaProcessar.entradasTodosAgricultores,
                                perdasTotaisDaFeira = feiraParaProcessar.perdasTotaisDaFeira, catalogoProdutos = todosOsProdutosState
                            )
                            feiraEmProcessamento = feiraParaProcessar.copy(resultadoGeralCalculado = resultadoCalculado)
                            navController.navigate(AppRoutes.resultadosFeiraRoute(feiraParaProcessar.fairDetails.feiraId))
                        }
                    },
                    onSalvarFeira = {
                        scope.launch {
                            val feiraParaSalvar = feiraEmProcessamento ?: return@launch
                            if (feiraRepository.salvarDadosCompletosFeira(feiraParaSalvar)) {
                                isCurrentFeiraPersisted = true
                                Toast.makeText(context, "Feira salva com sucesso!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Erro ao salvar a feira.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            } else { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }

        composable(route = AppRoutes.LANCAMENTO_PRODUTOS_ROUTE_PATTERN, arguments = listOf(navArgument("feiraId") { type = NavType.StringType }, navArgument("agricultorId") { type = NavType.StringType })) { backStackEntry ->
            val feiraIdArg = backStackEntry.arguments?.getString("feiraId")!!
            val agricultorIdArg = backStackEntry.arguments?.getString("agricultorId")!!
            val feiraAtual = feiraEmProcessamento

            if (feiraAtual != null && feiraAtual.fairDetails.feiraId == feiraIdArg) {
                LancamentoScreen(
                    nomeAgricultor = todosOsAgricultoresState.find { it.id == agricultorIdArg }?.nome ?: "ID: $agricultorIdArg",
                    catalogoProdutos = todosOsProdutosState,
                    entradasIniciais = feiraAtual.entradasTodosAgricultores[agricultorIdArg] ?: emptyList(),
                    isSaving = isSaving,
                    onFinalizar = { novasEntradas ->
                        if (isSaving) return@LancamentoScreen
                        isSaving = true

                        val feiraParaAtualizar = feiraEmProcessamento ?: run { isSaving = false; return@LancamentoScreen }
                        val mapaDeEntradasAtualizado = feiraParaAtualizar.entradasTodosAgricultores.toMutableMap().apply { this[agricultorIdArg] = novasEntradas }
                        val feiraAtualizada = feiraParaAtualizar.copy(entradasTodosAgricultores = mapaDeEntradasAtualizado)
                        feiraEmProcessamento = feiraAtualizada

                        scope.launch {
                            try {
                                if (feiraRepository.salvarDadosCompletosFeira(feiraAtualizada)) {
                                    isCurrentFeiraPersisted = true
                                    Toast.makeText(context, "Entradas salvas e feira atualizada!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack() // <<< SÓ VOLTA EM CASO DE SUCESSO
                                } else {
                                    Toast.makeText(context, "Erro ao salvar. Tente novamente.", Toast.LENGTH_LONG).show()
                                }
                            } finally {
                                isSaving = false // <<< SEMPRE LIBERA O BOTÃO
                            }
                        }
                    },
                    onVoltar = { if (!isSaving) navController.popBackStack() }
                )
            } else { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }

        composable(route = AppRoutes.LANCAMENTO_PERDAS_TOTAIS_PATTERN, arguments = listOf(navArgument("feiraId") { type = NavType.StringType })) { backStackEntry ->
            val feiraIdArg = backStackEntry.arguments?.getString("feiraId")!!
            val currentFeira = feiraEmProcessamento
            if (currentFeira != null && currentFeira.fairDetails.feiraId == feiraIdArg) {
                PerdasTotaisScreen(
                    feiraId = currentFeira.fairDetails.feiraId,
                    catalogoProdutos = todosOsProdutosState,
                    perdasIniciais = currentFeira.perdasTotaisDaFeira.toList(),
                    isSaving = isSaving,
                    onFinalizarPerdas = { novasPerdas ->
                        if(isSaving) return@PerdasTotaisScreen
                        isSaving = true

                        val feiraParaAtualizar = feiraEmProcessamento ?: run { isSaving = false; return@PerdasTotaisScreen }
                        val feiraAtualizada = feiraParaAtualizar.copy(perdasTotaisDaFeira = novasPerdas)
                        feiraEmProcessamento = feiraAtualizada

                        scope.launch {
                            try {
                                if (feiraRepository.salvarDadosCompletosFeira(feiraAtualizada)) {
                                    isCurrentFeiraPersisted = true
                                    Toast.makeText(context, "Perdas salvas e feira atualizada!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack() // <<< SÓ VOLTA EM CASO DE SUCESSO
                                } else {
                                    Toast.makeText(context, "Erro ao salvar. Verifique sua conexão e tente novamente.", Toast.LENGTH_LONG).show()
                                }
                            } finally {
                                isSaving = false // <<< SEMPRE LIBERA O BOTÃO
                            }
                        }
                    },
                    onVoltar = { if (!isSaving) navController.popBackStack() }
                )
            } else { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }

        composable(route = AppRoutes.LANCAMENTO_DESPESAS_FEIRA_PATTERN, arguments = listOf(navArgument("feiraId") { type = NavType.StringType })) { backStackEntry ->
            val feiraIdArgumento = backStackEntry.arguments?.getString("feiraId")
            val fairDetailsDaFeiraAtual = feiraEmProcessamento?.fairDetails?.takeIf { it.feiraId == feiraIdArgumento }
            if (feiraIdArgumento != null && fairDetailsDaFeiraAtual != null) {
                LancamentoDespesasFeiraScreen(
                    navController = navController,
                    fairDetails = fairDetailsDaFeiraAtual,
                    viewModel = viewModel(factory = LancamentoDespesasFeiraViewModelFactory(itemDespesaRepository, despesaFeiraRepository, feiraIdArgumento))
                )
            } else { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Carregando...") } }
        }

        composable(route = AppRoutes.DISTRIBUIR_SOBRAS_PATTERN, arguments = listOf(navArgument("feiraIdAtual") { type = NavType.StringType })) { backStackEntry ->
            val feiraIdDestino = backStackEntry.arguments?.getString("feiraIdAtual")
            if (feiraIdDestino != null && feiraEmProcessamento != null) {
                DistribuirSobrasScreen(
                    navController = navController,
                    feiraIdAtual = feiraIdDestino,
                    viewModel = viewModel(factory = RegistrarSobrasViewModelFactory(feiraIdAtual = feiraIdDestino, feiraRepository = feiraRepository, produtoRepository = produtoRepository, perdaRepository = perdaRepository, entradaRepository = entradaRepository)),
                    entradasAtuaisDaFeira = feiraEmProcessamento!!.entradasTodosAgricultores,
                    isSaving = isSaving,
                    onDistribuicaoConcluida = { novasEntradas ->
                        if(isSaving) return@DistribuirSobrasScreen
                        isSaving = true

                        val feiraParaAtualizar = feiraEmProcessamento ?: run { isSaving = false; return@DistribuirSobrasScreen }
                        val feiraAtualizada = feiraParaAtualizar.copy(entradasTodosAgricultores = novasEntradas)
                        feiraEmProcessamento = feiraAtualizada

                        scope.launch {
                            try {
                                if (feiraRepository.salvarDadosCompletosFeira(feiraAtualizada)) {
                                    isCurrentFeiraPersisted = true
                                    Toast.makeText(context, "Sobras distribuídas e feira salva!", Toast.LENGTH_SHORT).show()
                                } else { Toast.makeText(context, "Erro ao salvar distribuição de sobras.", Toast.LENGTH_LONG).show() }
                            } finally {
                                isSaving = false
                                navController.popBackStack()
                            }
                        }
                    }
                )
            } else { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {Text("Erro: Não foi possível carregar dados.")} }
        }

        composable(AppRoutes.CADASTRO_PRODUTOS) { CadastroProdutosScreen(navController = navController) }
        composable(AppRoutes.CADASTRO_AGRICULTORES) { CadastroAgricultoresScreen(navController = navController) }
        composable(AppRoutes.CADASTRO_ITENS_DESPESA) { CadastroItensDespesaScreen(navController = navController) }
        composable(AppRoutes.SELECIONAR_MES_ANO_DESPESA) { SelecionarMesAnoDespesaScreen(navController = navController) }


        composable(
            route = AppRoutes.RESULTADOS_FEIRA_PATTERN,
            arguments = listOf(navArgument("feiraId") { type = NavType.StringType })
        ) { backStackEntry ->
            val feiraIdArg = backStackEntry.arguments?.getString("feiraId") ?: return@composable
            val currentFeiraLocal = feiraEmProcessamento
            if (currentFeiraLocal != null && currentFeiraLocal.fairDetails.feiraId == feiraIdArg) {
                if (currentFeiraLocal.resultadoGeralCalculado == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Processando resultados..."); CircularProgressIndicator() } }
                    LaunchedEffect(key1 = currentFeiraLocal) {
                        val novoResultado = calcularResultadosFeira(
                            fairDetails = currentFeiraLocal.fairDetails, entradasTodosAgricultores = currentFeiraLocal.entradasTodosAgricultores,
                            perdasTotaisDaFeira = currentFeiraLocal.perdasTotaisDaFeira, catalogoProdutos = todosOsProdutosState
                        )
                        feiraEmProcessamento = currentFeiraLocal.copy(resultadoGeralCalculado = novoResultado)
                    }
                } else {
                    ResultadosFeiraScreen(
                        resultadoGeralFeira = currentFeiraLocal.resultadoGeralCalculado!!,
                        listaDeAgricultores = todosOsAgricultoresState,
                        onVoltar = { navController.popBackStack() },
                        onSalvarFeira = {
                            currentFeiraLocal.let { feiraParaSalvar ->
                                scope.launch {
                                    if (feiraRepository.salvarDadosCompletosFeira(feiraParaSalvar)) {
                                        isCurrentFeiraPersisted = true
                                        Toast.makeText(context, "Feira e resultados salvos!", Toast.LENGTH_SHORT).show()
                                        navController.navigate(AppRoutes.HOME_SCREEN) { popUpTo(AppRoutes.HOME_SCREEN) { inclusive = true } }
                                    } else {
                                        Toast.makeText(context, "Erro ao salvar Feira.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }
            } else { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
    }
}