package com.example.gestaofeiracooperativa // <<--- ATENÇÃO: MUDE PARA O SEU PACKAGE REAL

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch

// IMPORTS DE TELAS
// ... (seus imports de telas) ...
import com.example.gestaofeiracooperativa.HomeScreen
import com.example.gestaofeiracooperativa.ListaFeirasSalvasScreen
import com.example.gestaofeiracooperativa.CriarNovaFeiraScreen
import com.example.gestaofeiracooperativa.GerenciarFeiraScreen
import com.example.gestaofeiracooperativa.LancamentoScreen
import com.example.gestaofeiracooperativa.PerdasTotaisScreen
import com.example.gestaofeiracooperativa.ResultadosFeiraScreen
import com.example.gestaofeiracooperativa.CadastroProdutosScreen
import com.example.gestaofeiracooperativa.CadastroAgricultoresScreen


// IMPORTS PARA ROOM E REPOSITORY
import com.example.gestaofeiracooperativa.AppDatabase
import com.example.gestaofeiracooperativa.FeiraRepository
import com.example.gestaofeiracooperativa.AgricultorRepository
import com.example.gestaofeiracooperativa.ProdutoRepository // <<< NOVO IMPORT (se ainda não tiver)
import com.example.gestaofeiracooperativa.DadosCompletosFeira
import com.example.gestaofeiracooperativa.FairDetails
import com.example.gestaofeiracooperativa.Agricultor
import com.example.gestaofeiracooperativa.Produto // <<< NOVO IMPORT (se ainda não tiver)


// Suas funções de utilidade
import com.example.gestaofeiracooperativa.loadProductsFromAssets
import com.example.gestaofeiracooperativa.calcularResultadosFeira
// import androidx.compose.runtime.getValue // Para a delegação 'by' // Já deve estar aí
// import androidx.compose.runtime.collectAsState // Para co // Já deve estar aí


object AppRoutes {
    // ... (definições de rotas permanecem as mesmas) ...
    const val HOME_SCREEN = "homeScreen"
    const val LISTA_FEIRAS_SALVAS = "listaFeirasSalvas"
    const val CRIAR_NOVA_FEIRA = "criarNovaFeira"
    const val GERENCIAR_FEIRA_PATTERN = "gerenciarFeira/{feiraId}"
    const val LANCAMENTO_PRODUTOS_ROUTE_PATTERN = "lancamentoProdutos/{feiraId}/{agricultorId}"
    const val LANCAMENTO_PERDAS_TOTAIS_PATTERN = "lancamentoPerdasTotais/{feiraId}"
    const val RESULTADOS_FEIRA_PATTERN = "resultadosFeira/{feiraId}"
    const val CADASTRO_PRODUTOS = "cadastroProdutos"
    const val CADASTRO_AGRICULTORES = "cadastroAgricultores"

    fun gerenciarFeiraRoute(feiraId: String) = "gerenciarFeira/$feiraId"
    fun lancamentoProdutosRoute(feiraId: String, agricultorId: String) = "lancamentoProdutos/$feiraId/$agricultorId"
    fun lancamentoPerdasTotaisRoute(feiraId: String) = "lancamentoPerdasTotais/$feiraId"
    fun resultadosFeiraRoute(feiraId: String) = "resultadosFeira/$feiraId"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val database = remember { AppDatabase.getDatabase(context, scope) }

    // DAOs (você já os obtém assim, o que é bom)
    val produtoDao = remember { database.produtoDao() }
    val agricultorDao = remember { database.agricultorDao() }
    val feiraDao = remember { database.feiraDao() }
    val entradaDao = remember { database.entradaDao() }
    val perdaDao = remember { database.perdaDao() }

    // Repositórios
    val feiraRepository = remember {
        FeiraRepository(
            appDatabase = database,
            feiraDao = feiraDao,
            entradaDao = entradaDao,
            perdaDao = perdaDao,
            produtoDao = produtoDao,
            agricultorDao = agricultorDao,
            applicationContext = context.applicationContext
        )
    }
    val agricultorRepository = remember { AgricultorRepository(agricultorDao) }
    val produtoRepository = remember { ProdutoRepository(produtoDao) } // Você já tinha isso, ótimo!

    // Estados coletados dos Flows dos repositórios
    val todosOsAgricultoresState by agricultorRepository.getAllAgricultores().collectAsState(initial = emptyList())
    val todosOsProdutosState by produtoRepository.getAllProducts().collectAsState(initial = emptyList()) // Você já tinha isso, ótimo!

    // Este catálogo do CSV pode ser mantido para `calcularResultadosFeira` se essa função
    // especificamente precisar dos dados como estavam no CSV original, ou se você não quiser
    // refatorá-la para usar o `todosOsProdutosState` agora.
    // Para consistência, o ideal a longo prazo seria `calcularResultadosFeira` também usar `todosOsProdutosState`.
    val catalogoProdutosOriginalDoCSV by remember { mutableStateOf(loadProductsFromAssets(context, "CAD PROD.csv")) }

    var feiraEmProcessamento by remember { mutableStateOf<DadosCompletosFeira?>(null) }
    var showDialogFeiraJaExiste by remember { mutableStateOf(false) }
    var novaFeiraIdParaCriar by remember { mutableStateOf("") }
    var novaStartDateParaCriar by remember { mutableStateOf("") }
    var novaEndDateParaCriar by remember { mutableStateOf("") }

    fun carregarOuIniciarNovaFeiraState(
        feiraId: String,
        startDateForNew: String?,
        endDateForNew: String?
    ) {
        // ... (esta função permanece como na versão anterior) ...
        scope.launch {
            Log.d("AppNavigation", "Tentando carregar/iniciar feira: $feiraId")
            val dadosCarregados = feiraRepository.carregarDadosCompletosFeira(feiraId)
            if (dadosCarregados != null) {
                feiraEmProcessamento = dadosCarregados
                Toast.makeText(context, "Feira $feiraId carregada do banco de dados!", Toast.LENGTH_SHORT).show()
                Log.d("AppNavigation", "Feira $feiraId carregada: $feiraEmProcessamento")
            } else {
                if (startDateForNew != null && endDateForNew != null && startDateForNew.isNotBlank() && endDateForNew.isNotBlank()) {
                    feiraEmProcessamento = DadosCompletosFeira(
                        fairDetails = FairDetails(feiraId, startDateForNew, endDateForNew),
                        entradasTodosAgricultores = mutableStateMapOf(),
                        perdasTotaisDaFeira = mutableStateListOf(),
                        resultadoGeralCalculado = null
                    )
                    Toast.makeText(context, "Iniciando nova Feira $feiraId.", Toast.LENGTH_SHORT).show()
                    Log.d("AppNavigation", "Nova Feira $feiraId iniciada: $feiraEmProcessamento")
                } else {
                    Toast.makeText(context, "Feira $feiraId não encontrada e datas para nova feira ausentes.", Toast.LENGTH_LONG).show()
                    Log.w("AppNavigation", "Falha ao carregar ou iniciar feira $feiraId. Datas para nova: S:$startDateForNew E:$endDateForNew")
                    feiraEmProcessamento = null
                }
            }
        }
    }

    if (showDialogFeiraJaExiste) {
        // ... (AlertDialog permanece como na versão anterior) ...
        AlertDialog(
            onDismissRequest = { showDialogFeiraJaExiste = false },
            title = { Text("Feira Já Existe") },
            text = { Text("A Feira Nº $novaFeiraIdParaCriar (${novaStartDateParaCriar} - ${novaEndDateParaCriar}) já possui dados salvos no banco. Deseja carregar esses dados ou iniciar uma nova feira (sobrescreverá os dados anteriores ao salvar)?") },
            confirmButton = {
                TextButton(onClick = {
                    carregarOuIniciarNovaFeiraState(novaFeiraIdParaCriar, null, null)
                    showDialogFeiraJaExiste = false
                    navController.navigate(AppRoutes.gerenciarFeiraRoute(novaFeiraIdParaCriar))
                }) { Text("Carregar Dados") }
            },
            dismissButton = {
                TextButton(onClick = {
                    carregarOuIniciarNovaFeiraState(novaFeiraIdParaCriar, novaStartDateParaCriar, novaEndDateParaCriar)
                    showDialogFeiraJaExiste = false
                    navController.navigate(AppRoutes.gerenciarFeiraRoute(novaFeiraIdParaCriar))
                }) { Text("Iniciar Nova (Sobrescrever ao Salvar)") }
            }
        )
    }

    NavHost(navController = navController, startDestination = AppRoutes.HOME_SCREEN) {

        composable(AppRoutes.HOME_SCREEN) { /* ... (sem alterações aqui) ... */
            HomeScreen(
                onNavigateToNovaFeira = { navController.navigate(AppRoutes.CRIAR_NOVA_FEIRA) },
                onNavigateToFeirasSalvas = { navController.navigate(AppRoutes.LISTA_FEIRAS_SALVAS) },
                onNavigateToCadastroProdutos = { navController.navigate(AppRoutes.CADASTRO_PRODUTOS) },
                onNavigateToCadastroAgricultores = { navController.navigate(AppRoutes.CADASTRO_AGRICULTORES) }
            )
        }

        composable(AppRoutes.LISTA_FEIRAS_SALVAS) { /* ... (sem alterações aqui) ... */
            val listaDeFairDetailsState by feiraRepository.getTodasAsFeirasInfo().collectAsState(initial = emptyList())
            ListaFeirasSalvasScreen(
                navController = navController,
                feirasSalvasDetails = listaDeFairDetailsState,
                onAbrirFeira = { feiraId ->
                    navController.navigate(AppRoutes.gerenciarFeiraRoute(feiraId))
                },
                onDeletarFeira = { idFeiraDeletada ->
                    scope.launch {
                        if (feiraRepository.deletarFeira(idFeiraDeletada)) {
                            Toast.makeText(context, "Feira $idFeiraDeletada deletada do banco.", Toast.LENGTH_SHORT).show()
                            if (feiraEmProcessamento?.fairDetails?.feiraId == idFeiraDeletada) {
                                feiraEmProcessamento = null
                            }
                        } else {
                            Toast.makeText(context, "Falha ao deletar Feira $idFeiraDeletada do banco.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        composable(AppRoutes.CRIAR_NOVA_FEIRA) { /* ... (sem alterações aqui) ... */
            CriarNovaFeiraScreen(
                navController = navController,
                onConfirmarCriacaoFeira = { feiraId, startDate, endDate ->
                    scope.launch {
                        if (feiraRepository.feiraExiste(feiraId)) {
                            novaFeiraIdParaCriar = feiraId
                            novaStartDateParaCriar = startDate
                            novaEndDateParaCriar = endDate
                            showDialogFeiraJaExiste = true
                        } else {
                            carregarOuIniciarNovaFeiraState(feiraId, startDate, endDate)
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
            LaunchedEffect(key1 = feiraIdArg, key2 = feiraRepository) {
                if (feiraEmProcessamento == null || feiraEmProcessamento?.fairDetails?.feiraId != feiraIdArg) {
                    Log.d("AppNavigation", "GerenciarFeira: Carregando feira $feiraIdArg via LaunchedEffect")
                    carregarOuIniciarNovaFeiraState(feiraIdArg, null, null)
                }
            }

            val currentFeira = feiraEmProcessamento
            if (currentFeira != null && currentFeira.fairDetails.feiraId == feiraIdArg) {
                GerenciarFeiraScreen(
                    navController = navController,
                    feiraDetails = currentFeira.fairDetails,
                    listaDeAgricultores = todosOsAgricultoresState, // Já estava correto
                    onNavigateToLancamentos = { agricultorId ->
                        navController.navigate(AppRoutes.lancamentoProdutosRoute(currentFeira.fairDetails.feiraId, agricultorId))
                    },
                    onNavigateToPerdasTotais = {
                        navController.navigate(AppRoutes.lancamentoPerdasTotaisRoute(currentFeira.fairDetails.feiraId))
                    },
                    onNavigateToResultados = { /* ... (lógica de resultados como antes) ... */
                        scope.launch {
                            val feiraAtualizada = if (currentFeira.resultadoGeralCalculado == null) {
                                val temEntradas = currentFeira.entradasTodosAgricultores.values.any { it.isNotEmpty() }
                                val temPerdas = currentFeira.perdasTotaisDaFeira.isNotEmpty()
                                if (temEntradas || temPerdas) {
                                    currentFeira.copy(
                                        resultadoGeralCalculado = calcularResultadosFeira(
                                            fairDetails = currentFeira.fairDetails,
                                            entradasTodosAgricultores = currentFeira.entradasTodosAgricultores,
                                            perdasTotaisDaFeira = currentFeira.perdasTotaisDaFeira,
                                            catalogoProdutos = catalogoProdutosOriginalDoCSV // <<< Usando o CSV aqui por enquanto
                                        )
                                    )
                                } else {
                                    Toast.makeText(context, "Não há entradas ou perdas lançadas para a Feira ${currentFeira.fairDetails.feiraId}.", Toast.LENGTH_LONG).show()
                                    null
                                }
                            } else {
                                currentFeira
                            }

                            feiraAtualizada?.let {
                                feiraEmProcessamento = it
                                navController.navigate(AppRoutes.resultadosFeiraRoute(it.fairDetails.feiraId))
                            }
                        }
                    },
                    onSalvarFeira = { /* ... (lógica de salvar como antes) ... */
                        scope.launch {
                            if (feiraRepository.salvarDadosCompletosFeira(currentFeira)) {
                                Toast.makeText(context, "Feira ${currentFeira.fairDetails.feiraId} salva no banco!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Erro ao salvar Feira ${currentFeira.fairDetails.feiraId} no banco.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            } else { /* ... (loading indicator como antes) ... */
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Log.d("AppNavigation", "GerenciarFeira: Mostrando CircularProgressIndicator para feiraIdArg: $feiraIdArg, feiraEmProcessamentoId: ${feiraEmProcessamento?.fairDetails?.feiraId}")
                }
            }
        }

        composable(
            route = AppRoutes.LANCAMENTO_PRODUTOS_ROUTE_PATTERN,
            // ... arguments ...
        ) { backStackEntry ->
            val feiraIdArg = backStackEntry.arguments?.getString("feiraId") ?: return@composable
            val agricultorIdArg = backStackEntry.arguments?.getString("agricultorId") ?: return@composable

            LaunchedEffect(key1 = feiraIdArg, key2 = feiraRepository) {
                if (feiraEmProcessamento == null || feiraEmProcessamento?.fairDetails?.feiraId != feiraIdArg) {
                    carregarOuIniciarNovaFeiraState(feiraIdArg, null, null)
                }
            }

            val currentFeira = feiraEmProcessamento
            if (currentFeira != null && currentFeira.fairDetails.feiraId == feiraIdArg) {
                // <<< ALTERAÇÃO: Verifica todosOsProdutosState em vez de catalogoProdutos.isEmpty() >>>
                val agricultorSelecionado = todosOsAgricultoresState.find { it.id == agricultorIdArg }
                val nomeAgricultorParaDisplay = agricultorSelecionado?.nome ?: "Agricultor $agricultorIdArg"
                if (todosOsProdutosState.isEmpty()) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text("Carregando produtos ou nenhum produto cadastrado...") }
                } else {
                    LancamentoScreen(
                        feiraId = currentFeira.fairDetails.feiraId,
                        agricultorId = agricultorIdArg,
                        nomeAgricultor = nomeAgricultorParaDisplay,
                        catalogoProdutos = todosOsProdutosState, // <<< ALTERAÇÃO: Passa a lista do banco
                        entradasIniciais = currentFeira.entradasTodosAgricultores[agricultorIdArg] ?: emptyList(),
                        onFinalizar = { novasEntradasDoAgricultor ->
                            val feiraAtualizada = currentFeira.copy(
                                entradasTodosAgricultores = currentFeira.entradasTodosAgricultores.toMutableMap().apply {
                                    put(agricultorIdArg, novasEntradasDoAgricultor)
                                },
                                resultadoGeralCalculado = null
                            )
                            feiraEmProcessamento = feiraAtualizada
                            Toast.makeText(context, "Entradas do Agr. $agricultorIdArg atualizadas. Salve a feira!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onVoltar = { navController.popBackStack() }
                    )
                }
            } else { /* ... (loading indicator como antes) ... */
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }

        composable(
            route = AppRoutes.LANCAMENTO_PERDAS_TOTAIS_PATTERN,
            // ... arguments ...
        ) { backStackEntry ->
            val feiraIdArg = backStackEntry.arguments?.getString("feiraId") ?: return@composable

            LaunchedEffect(key1 = feiraIdArg, key2 = feiraRepository) {
                if (feiraEmProcessamento == null || feiraEmProcessamento?.fairDetails?.feiraId != feiraIdArg) {
                    carregarOuIniciarNovaFeiraState(feiraIdArg, null, null)
                }
            }
            val currentFeira = feiraEmProcessamento
            if (currentFeira != null && currentFeira.fairDetails.feiraId == feiraIdArg) {
                // <<< ALTERAÇÃO: Verifica todosOsProdutosState em vez de catalogoProdutos.isEmpty() >>>
                if (todosOsProdutosState.isEmpty()) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text("Carregando produtos ou nenhum produto cadastrado...") }
                } else {
                    PerdasTotaisScreen(
                        feiraId = currentFeira.fairDetails.feiraId,
                        catalogoProdutos = todosOsProdutosState, // <<< ALTERAÇÃO: Passa a lista do banco
                        perdasIniciais = currentFeira.perdasTotaisDaFeira.toList(),
                        onFinalizarPerdas = { novasPerdasDaFeira ->
                            val feiraAtualizada = currentFeira.copy(
                                perdasTotaisDaFeira = novasPerdasDaFeira,
                                resultadoGeralCalculado = null
                            )
                            feiraEmProcessamento = feiraAtualizada
                            Toast.makeText(context, "Perdas da Feira ${currentFeira.fairDetails.feiraId} atualizadas. Salve a feira!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onVoltar = { navController.popBackStack() }
                    )
                }
            } else { /* ... (loading indicator como antes) ... */
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }

        composable(AppRoutes.CADASTRO_PRODUTOS) { /* ... (sem alterações aqui) ... */
            CadastroProdutosScreen(navController = navController)
        }

        composable(AppRoutes.CADASTRO_AGRICULTORES) { /* ... (sem alterações aqui) ... */
            CadastroAgricultoresScreen(navController = navController)
        }

        composable(
            route = AppRoutes.RESULTADOS_FEIRA_PATTERN,
            // ... arguments ...
        ) { backStackEntry ->
            val feiraIdArg = backStackEntry.arguments?.getString("feiraId") ?: return@composable
            LaunchedEffect(key1 = feiraIdArg, key2 = feiraRepository) {
                if (feiraEmProcessamento == null || feiraEmProcessamento?.fairDetails?.feiraId != feiraIdArg) {
                    carregarOuIniciarNovaFeiraState(feiraIdArg, null, null)
                }
            }

            var currentFeira = feiraEmProcessamento
            if (currentFeira != null && currentFeira.fairDetails.feiraId == feiraIdArg) {
                if (currentFeira.resultadoGeralCalculado == null) {
                    val novoResultado = calcularResultadosFeira(
                        fairDetails = currentFeira.fairDetails,
                        entradasTodosAgricultores = currentFeira.entradasTodosAgricultores,
                        perdasTotaisDaFeira = currentFeira.perdasTotaisDaFeira,
                        catalogoProdutos = catalogoProdutosOriginalDoCSV // <<< Usando o CSV aqui por enquanto
                    )
                    feiraEmProcessamento = currentFeira.copy(resultadoGeralCalculado = novoResultado)
                    currentFeira = feiraEmProcessamento
                }

                currentFeira?.resultadoGeralCalculado?.let { resultado ->
                    ResultadosFeiraScreen(
                        resultadoGeralFeira = resultado,
                        listaDeAgricultores = todosOsAgricultoresState,
                        onVoltar = { navController.popBackStack() },
                        onSalvarFeira = {
                            currentFeira?.let { feiraParaSalvar ->
                                scope.launch {
                                    if (feiraRepository.salvarDadosCompletosFeira(feiraParaSalvar)) {
                                        Toast.makeText(context, "Feira ${feiraParaSalvar.fairDetails.feiraId} salva no banco!", Toast.LENGTH_SHORT).show()
                                        navController.navigate(AppRoutes.HOME_SCREEN) {
                                            popUpTo(AppRoutes.HOME_SCREEN) { inclusive = true }
                                        }
                                    } else {
                                        Toast.makeText(context, "Erro ao salvar Feira ${feiraParaSalvar.fairDetails.feiraId} no banco.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                } ?: run { /* ... (Mensagem de erro de cálculo como antes) ... */
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Não foi possível obter ou calcular os resultados para a Feira ${currentFeira?.fairDetails?.feiraId}.")
                        Text("Verifique se há entradas ou perdas lançadas.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { navController.popBackStack() }) { Text("Voltar") }
                    }
                }
            } else { /* ... (loading indicator como antes) ... */
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}