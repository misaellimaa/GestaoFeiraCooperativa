package com.example.gestaofeiracooperativa

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch


// IMPORTS DE TELAS
import com.example.gestaofeiracooperativa.HomeScreen
import com.example.gestaofeiracooperativa.ListaFeirasSalvasScreen
import com.example.gestaofeiracooperativa.CriarNovaFeiraScreen
import com.example.gestaofeiracooperativa.GerenciarFeiraScreen
import com.example.gestaofeiracooperativa.LancamentoScreen // Lançamento de Entradas de Produtos
import com.example.gestaofeiracooperativa.PerdasTotaisScreen
import com.example.gestaofeiracooperativa.ResultadosFeiraScreen
import com.example.gestaofeiracooperativa.CadastroProdutosScreen
import com.example.gestaofeiracooperativa.CadastroAgricultoresScreen
import com.example.gestaofeiracooperativa.CadastroItensDespesaScreen
import com.example.gestaofeiracooperativa.LancamentoDespesasFeiraScreen

// IMPORTS PARA ROOM E REPOSITORY
import com.example.gestaofeiracooperativa.AppDatabase
import com.example.gestaofeiracooperativa.FeiraRepository
import com.example.gestaofeiracooperativa.AgricultorRepository
import com.example.gestaofeiracooperativa.ProdutoRepository
// Repositórios de despesa serão acessados via 'application' nas factories
// import com.example.gestaofeiracooperativa.ItemDespesaRepository
// import com.example.gestaofeiracooperativa.DespesaFeiraRepository

// Modelos de Dados
import com.example.gestaofeiracooperativa.DadosCompletosFeira
import com.example.gestaofeiracooperativa.FairDetails
import com.example.gestaofeiracooperativa.Agricultor
import com.example.gestaofeiracooperativa.Produto
import com.example.gestaofeiracooperativa.DespesaFeiraUiItem
import com.example.gestaofeiracooperativa.MyApplication


// Funções de Utilidade
import com.example.gestaofeiracooperativa.loadProductsFromAssets
import com.example.gestaofeiracooperativa.calcularResultadosFeira


object AppRoutes {
    const val HOME_SCREEN = "homeScreen"
    const val LISTA_FEIRAS_SALVAS = "listaFeirasSalvas"
    const val CRIAR_NOVA_FEIRA = "criarNovaFeira"
    const val GERENCIAR_FEIRA_PATTERN = "gerenciarFeira/{feiraId}"
    const val LANCAMENTO_PRODUTOS_ROUTE_PATTERN = "lancamentoProdutos/{feiraId}/{agricultorId}"
    const val LANCAMENTO_PERDAS_TOTAIS_PATTERN = "lancamentoPerdasTotais/{feiraId}"
    const val RESULTADOS_FEIRA_PATTERN = "resultadosFeira/{feiraId}"
    const val CADASTRO_PRODUTOS = "cadastroProdutos"
    const val CADASTRO_AGRICULTORES = "cadastroAgricultores"
    const val CADASTRO_ITENS_DESPESA = "cadastroItensDespesa"
    const val LANCAMENTO_DESPESAS_FEIRA_PATTERN = "lancamentoDespesasFeira/{feiraId}"
    const val SELECIONAR_MES_ANO_DESPESA = "selecionarMesAnoDespesa"
    const val DISTRIBUIR_SOBRAS_PATTERN = "distribuirSobras/{feiraIdAtual}"

    fun gerenciarFeiraRoute(feiraId: String) = "gerenciarFeira/$feiraId"
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

    val database = remember { AppDatabase.getDatabase(context, scope) }

    // DAOs
    val produtoDao = remember { database.produtoDao() }
    val agricultorDao = remember { database.agricultorDao() }
    val feiraDao = remember { database.feiraDao() }
    val entradaDao = remember { database.entradaDao() }
    val perdaDao = remember { database.perdaDao() }
    val itemDespesaDao = remember { database.itemDespesaDao() }
    val despesaFeiraDao = remember { database.despesaFeiraDao() }

    // Repositórios
    val produtoRepository = remember { ProdutoRepository(produtoDao) }
    val agricultorRepository = remember { AgricultorRepository(agricultorDao) }
    val feiraRepository = remember { // Este é o feiraRepository principal
        FeiraRepository(
            appDatabase = database,
            feiraDao = feiraDao,
            entradaDao = entradaDao,
            perdaDao = perdaDao,
            produtoDao = produtoDao,
            agricultorDao = agricultorDao,
            itemDespesaDao = itemDespesaDao,
            despesaFeiraDao = despesaFeiraDao,
            applicationContext = context.applicationContext
        )
    }
    // Os repositórios ItemDespesaRepository e DespesaFeiraRepository são acessados via 'application'
    // nas Factories dos ViewModels de despesa. Certifique-se que 'MyApplication.kt' os provê.

    // Estados
    val todosOsAgricultoresState by agricultorRepository.getAllAgricultores().collectAsState(initial = emptyList())
    val todosOsProdutosState by produtoRepository.getAllProducts().collectAsState(initial = emptyList())
    val catalogoProdutosOriginalDoCSV by remember { mutableStateOf(loadProductsFromAssets(context, "CAD PROD.csv")) }
    var feiraEmProcessamento by remember { mutableStateOf<DadosCompletosFeira?>(null) }
    var isCurrentFeiraPersisted by remember { mutableStateOf(false) }
    var showDialogFeiraJaExiste by remember { mutableStateOf(false) }
    var novaFeiraIdParaCriar by remember { mutableStateOf("") }
    var novaStartDateParaCriar by remember { mutableStateOf("") }
    var novaEndDateParaCriar by remember { mutableStateOf("") }

    fun carregarOuIniciarNovaFeiraState(
        feiraId: String,
        startDateForNew: String?,
        endDateForNew: String?
    ) {
        scope.launch {
            Log.d("AppNavigation", "carregarOuIniciarNovaFeiraState para Feira ID: $feiraId.")
            val dadosCarregados = feiraRepository.carregarDadosCompletosFeira(feiraId)
            if (dadosCarregados != null) {
                feiraEmProcessamento = dadosCarregados
                isCurrentFeiraPersisted = true
                Toast.makeText(context, "Feira $feiraId carregada!", Toast.LENGTH_SHORT).show()
            } else {
                if (startDateForNew != null && endDateForNew != null && startDateForNew.isNotBlank() && endDateForNew.isNotBlank()) {
                    feiraEmProcessamento = DadosCompletosFeira(
                        fairDetails = FairDetails(feiraId, startDateForNew, endDateForNew),
                        entradasTodosAgricultores = mutableStateMapOf(),
                        perdasTotaisDaFeira = mutableStateListOf(),
                        despesasDaFeira = mutableStateListOf(),
                        resultadoGeralCalculado = null
                    )
                    isCurrentFeiraPersisted = false
                    Toast.makeText(context, "Iniciando nova Feira $feiraId.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Falha ao carregar/iniciar Feira $feiraId.", Toast.LENGTH_LONG).show()
                    feiraEmProcessamento = null
                    isCurrentFeiraPersisted = false
                }
            }
        }
    }

    if (showDialogFeiraJaExiste) {
        AlertDialog(
            onDismissRequest = { showDialogFeiraJaExiste = false },
            title = { Text("Feira Já Existe") },
            text = { Text("A Feira Nº $novaFeiraIdParaCriar (${novaStartDateParaCriar} - ${novaEndDateParaCriar}) já possui dados. Carregar ou iniciar nova?") },
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
                }) { Text("Iniciar Nova") }
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

        composable(AppRoutes.SELECIONAR_MES_ANO_DESPESA) {
            SelecionarMesAnoDespesaScreen(
                navController = navController,
                viewModel = viewModel(
                    factory = RelatorioDespesaViewModelFactory(
                        feiraRepository = application.feiraRepository,
                        despesaFeiraRepository = application.despesaFeiraRepository,
                        itemDespesaRepository = application.itemDespesaRepository
                    )
                )
            )
        }

        composable(
            route = AppRoutes.DISTRIBUIR_SOBRAS_PATTERN, // O padrão da rota que definimos
            arguments = listOf(navArgument("feiraIdAtual") { type = NavType.StringType })
        ) { backStackEntry ->
            // Pega o ID da feira de destino que foi passado na navegação
            val feiraIdDestino = backStackEntry.arguments?.getString("feiraIdAtual")

            if (feiraIdDestino != null) {
                // Instancia o ViewModel usando a Factory e os repositórios da sua classe Application
                val viewModel: RegistrarSobrasViewModel = viewModel(
                    factory = RegistrarSobrasViewModelFactory(
                        feiraIdAtual = feiraIdDestino,
                        feiraRepository = application.feiraRepository,
                        produtoRepository = application.produtoRepository,
                        perdaRepository = application.perdaRepository,
                        entradaRepository = application.entradaRepository
                    )
                )

                // Chama a nova tela, passando o navController, o ID e o ViewModel
                DistribuirSobrasScreen(
                    navController = navController,
                    feiraIdAtual = feiraIdDestino,
                    viewModel = viewModel,
                    onDistribuicaoConcluida = {
                        // Força o recarregamento do feiraEmProcessamento com os dados atualizados do banco
                        carregarOuIniciarNovaFeiraState(feiraIdDestino, null, null)
                    }
                )
            } else {
                // Caso de erro: o ID não foi passado corretamente na rota
                Text("Erro: ID da feira de destino não foi fornecido.")
                LaunchedEffect(Unit) {
                    Toast.makeText(context, "Erro de navegação: ID da feira ausente.", Toast.LENGTH_LONG).show()
                    navController.popBackStack()
                }
            }
        }

        composable(AppRoutes.LISTA_FEIRAS_SALVAS) {
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
                            Toast.makeText(context, "Feira $idFeiraDeletada deletada.", Toast.LENGTH_SHORT).show()
                            if (feiraEmProcessamento?.fairDetails?.feiraId == idFeiraDeletada) {
                                feiraEmProcessamento = null
                                isCurrentFeiraPersisted = false
                            }
                        } else {
                            Toast.makeText(context, "Falha ao deletar Feira $idFeiraDeletada.", Toast.LENGTH_SHORT).show()
                        }
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
            LaunchedEffect(key1 = feiraIdArg) {
                if (feiraEmProcessamento == null || feiraEmProcessamento?.fairDetails?.feiraId != feiraIdArg) {
                    carregarOuIniciarNovaFeiraState(feiraIdArg, null, null)
                }
            }

            val currentFeira = feiraEmProcessamento
            val feiraEstaPersistida = isCurrentFeiraPersisted

            if (currentFeira != null && currentFeira.fairDetails.feiraId == feiraIdArg) {
                GerenciarFeiraScreen(
                    navController = navController,
                    feiraDetails = currentFeira.fairDetails,
                    listaDeAgricultores = todosOsAgricultoresState,
                    isFeiraPersistida = feiraEstaPersistida,
                    onNavigateToLancamentos = { agricultorId ->
                        navController.navigate(AppRoutes.lancamentoProdutosRoute(currentFeira.fairDetails.feiraId, agricultorId))
                    },
                    onNavigateToDespesasFeira = { idFeira -> // <<< CALLBACK ADICIONADO
                        navController.navigate(AppRoutes.lancamentoDespesasFeiraRoute(idFeira))
                    },
                    onNavigateToPerdasTotais = {
                        navController.navigate(AppRoutes.lancamentoPerdasTotaisRoute(currentFeira.fairDetails.feiraId))
                    },
                    onNavigateToResultados = {
                        scope.launch {
                            val feiraParaProcessar = feiraEmProcessamento
                            if (feiraParaProcessar != null) {
                                val resultadoCalculado = if (feiraParaProcessar.resultadoGeralCalculado == null) {
                                    val temAlgoParaProcessar = feiraParaProcessar.entradasTodosAgricultores.values.any { it.isNotEmpty() } ||
                                            feiraParaProcessar.perdasTotaisDaFeira.isNotEmpty() ||
                                            feiraParaProcessar.despesasDaFeira.isNotEmpty()

                                    if (temAlgoParaProcessar) {
                                        calcularResultadosFeira(
                                            fairDetails = feiraParaProcessar.fairDetails,
                                            entradasTodosAgricultores = feiraParaProcessar.entradasTodosAgricultores,
                                            perdasTotaisDaFeira = feiraParaProcessar.perdasTotaisDaFeira,
                                            catalogoProdutos = catalogoProdutosOriginalDoCSV
                                        )
                                    } else {
                                        Toast.makeText(context, "Não há dados para processar para Feira ${feiraParaProcessar.fairDetails.feiraId}.", Toast.LENGTH_LONG).show()
                                        null
                                    }
                                } else { feiraParaProcessar.resultadoGeralCalculado }

                                resultadoCalculado?.let { resultadoFinal ->
                                    feiraEmProcessamento = feiraParaProcessar.copy(resultadoGeralCalculado = resultadoFinal)
                                    navController.navigate(AppRoutes.resultadosFeiraRoute(feiraParaProcessar.fairDetails.feiraId))
                                }
                            }
                        }
                    },
                    onNavigateToDistribuirSobras = { feiraIdAtual -> // <<< ADICIONE ESTE
                        navController.navigate(AppRoutes.distribuirSobrasRoute(feiraIdAtual))
                    },
                    onSalvarFeira = {
                        scope.launch {
                            if (feiraRepository.salvarDadosCompletosFeira(currentFeira)) {
                                Toast.makeText(context, "Feira ${currentFeira.fairDetails.feiraId} salva!", Toast.LENGTH_SHORT).show()
                                isCurrentFeiraPersisted = true
                            } else {
                                    Toast.makeText(context, "Erro ao salvar Feira ${currentFeira.fairDetails.feiraId}.", Toast.LENGTH_SHORT).show()
                            }

                        }
                    }
                )
            } else { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
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
            LaunchedEffect(key1 = feiraIdArg) {
                if (feiraEmProcessamento == null || feiraEmProcessamento?.fairDetails?.feiraId != feiraIdArg) {
                    carregarOuIniciarNovaFeiraState(feiraIdArg, null, null)
                }
            }
            val currentFeira = feiraEmProcessamento
            if (currentFeira != null && currentFeira.fairDetails.feiraId == feiraIdArg) {
                val agricultorSelecionado = todosOsAgricultoresState.find { it.id == agricultorIdArg }
                val nomeAgricultorParaDisplay = agricultorSelecionado?.nome ?: "ID: $agricultorIdArg"
                if (todosOsProdutosState.isEmpty() && catalogoProdutosOriginalDoCSV.isEmpty()) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text("Carregando produtos...") }
                } else {
                    LancamentoScreen(
                        nomeAgricultor = nomeAgricultorParaDisplay,
                        catalogoProdutos = todosOsProdutosState,
                        entradasIniciais = currentFeira.entradasTodosAgricultores[agricultorIdArg] ?: emptyList(),
                        onFinalizar = { novasEntradasDoAgricultor ->
                            feiraEmProcessamento = currentFeira.copy(
                                entradasTodosAgricultores = currentFeira.entradasTodosAgricultores.toMutableMap().apply {
                                    put(agricultorIdArg, novasEntradasDoAgricultor)
                                },
                                resultadoGeralCalculado = null
                            )
                            Toast.makeText(context, "Entradas do Agr. $agricultorIdArg atualizadas. Salve a feira!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onVoltar = { navController.popBackStack() }
                    )
                }
            } else { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }

        composable(
            route = AppRoutes.LANCAMENTO_PERDAS_TOTAIS_PATTERN,
            arguments = listOf(navArgument("feiraId") { type = NavType.StringType })
        ) { backStackEntry ->
            val feiraIdArg = backStackEntry.arguments?.getString("feiraId") ?: return@composable
            LaunchedEffect(key1 = feiraIdArg) {
                if (feiraEmProcessamento == null || feiraEmProcessamento?.fairDetails?.feiraId != feiraIdArg) {
                    carregarOuIniciarNovaFeiraState(feiraIdArg, null, null)
                }
            }
            val currentFeira = feiraEmProcessamento
            if (currentFeira != null && currentFeira.fairDetails.feiraId == feiraIdArg) {
                if (todosOsProdutosState.isEmpty() && catalogoProdutosOriginalDoCSV.isEmpty()) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text("Carregando produtos...") }
                } else {
                    PerdasTotaisScreen(
                        feiraId = currentFeira.fairDetails.feiraId,
                        catalogoProdutos = todosOsProdutosState,
                        perdasIniciais = currentFeira.perdasTotaisDaFeira.toList(),
                        onFinalizarPerdas = { novasPerdasDaFeira ->
                            feiraEmProcessamento = currentFeira.copy(
                                perdasTotaisDaFeira = novasPerdasDaFeira,
                                resultadoGeralCalculado = null
                            )
                            Toast.makeText(context, "Perdas da Feira ${currentFeira.fairDetails.feiraId} atualizadas. Salve a feira!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onVoltar = { navController.popBackStack() }
                    )
                }
            } else { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }

        composable(AppRoutes.CADASTRO_PRODUTOS) {
            CadastroProdutosScreen(navController = navController)
        }

        composable(AppRoutes.CADASTRO_AGRICULTORES) {
            CadastroAgricultoresScreen(navController = navController)
        }

        composable(AppRoutes.CADASTRO_ITENS_DESPESA) {
            CadastroItensDespesaScreen(
                navController = navController,
                viewModel = viewModel(
                    factory = CadastroItensDespesaViewModelFactory(application.itemDespesaRepository)
                )
            )
        }

        // <<< ROTA ÚNICA E CORRIGIDA PARA LANCAMENTO_DESPESAS_FEIRA_PATTERN >>>
        composable(
            route = AppRoutes.LANCAMENTO_DESPESAS_FEIRA_PATTERN,
            arguments = listOf(navArgument("feiraId") { type = NavType.StringType })
        ) { backStackEntry ->
            val feiraIdArgumento = backStackEntry.arguments?.getString("feiraId")
            val fairDetailsDaFeiraAtual = feiraEmProcessamento?.fairDetails?.takeIf { it.feiraId == feiraIdArgumento }

            if (feiraIdArgumento != null && fairDetailsDaFeiraAtual != null) {
                LancamentoDespesasFeiraScreen(
                    navController = navController,
                    feiraId = feiraIdArgumento,
                    fairDetails = fairDetailsDaFeiraAtual,
                    viewModel = viewModel(
                        factory = LancamentoDespesasFeiraViewModelFactory(
                            itemDespesaRepository = application.itemDespesaRepository,
                            despesaFeiraRepository = application.despesaFeiraRepository,
                            feiraRepository = application.feiraRepository, // Passando FeiraRepository
                            feiraId = feiraIdArgumento
                        )
                    ),
                    // <<< IMPLEMENTAÇÃO DO CALLBACK onDespesasSalvas >>>
                    onDespesasSalvas = { despesasAtualizadasDaTela ->
                        feiraEmProcessamento = feiraEmProcessamento?.copy(
                            despesasDaFeira = despesasAtualizadasDaTela
                        )
                        Log.d("AppNavigation", "Callback 'onDespesasSalvas' executado. 'feiraEmProcessamento' atualizado com ${despesasAtualizadasDaTela.size} itens de despesa.")
                    }
                )
            } else {
                // Estado de carregamento ou erro se os detalhes da feira não estiverem prontos
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Carregando detalhes da feira...")
                    LaunchedEffect(key1 = feiraIdArgumento) {
                        if (feiraIdArgumento!= null && (feiraEmProcessamento == null || feiraEmProcessamento?.fairDetails?.feiraId != feiraIdArgumento)) {
                            carregarOuIniciarNovaFeiraState(feiraIdArgumento, null, null)
                        }
                    }
                }
            }
        }

        composable(
            route = AppRoutes.RESULTADOS_FEIRA_PATTERN,
            arguments = listOf(navArgument("feiraId") { type = NavType.StringType })
        ) { backStackEntry ->
            val feiraIdArg = backStackEntry.arguments?.getString("feiraId") ?: return@composable
            LaunchedEffect(key1 = feiraIdArg) {
                if (feiraEmProcessamento == null || feiraEmProcessamento?.fairDetails?.feiraId != feiraIdArg) {
                    carregarOuIniciarNovaFeiraState(feiraIdArg, null, null)
                }
            }

            var currentFeiraLocal = feiraEmProcessamento
            if (currentFeiraLocal != null && currentFeiraLocal.fairDetails.feiraId == feiraIdArg) {
                if (currentFeiraLocal.resultadoGeralCalculado == null) {
                    val temAlgoParaProcessar = currentFeiraLocal.entradasTodosAgricultores.values.any { it.isNotEmpty() } ||
                            currentFeiraLocal.perdasTotaisDaFeira.isNotEmpty() ||
                            currentFeiraLocal.despesasDaFeira.isNotEmpty()
                    if (temAlgoParaProcessar) {
                        val novoResultado = calcularResultadosFeira(
                            fairDetails = currentFeiraLocal.fairDetails,
                            entradasTodosAgricultores = currentFeiraLocal.entradasTodosAgricultores,
                            perdasTotaisDaFeira = currentFeiraLocal.perdasTotaisDaFeira,
                            catalogoProdutos = catalogoProdutosOriginalDoCSV
                            // TODO: Passar despesas para calcularResultadosFeira se elas afetam o resultado financeiro.
                        )
                        feiraEmProcessamento = currentFeiraLocal.copy(resultadoGeralCalculado = novoResultado)
                        currentFeiraLocal = feiraEmProcessamento
                    } else {
                        Toast.makeText(context, "Não há dados lançados para gerar resultados.", Toast.LENGTH_LONG).show()
                    }
                }

                currentFeiraLocal?.resultadoGeralCalculado?.let { resultado ->
                    ResultadosFeiraScreen(
                        resultadoGeralFeira = resultado,
                        listaDeAgricultores = todosOsAgricultoresState,
                        onVoltar = { navController.popBackStack() },
                        onSalvarFeira = {
                            currentFeiraLocal.let { feiraParaSalvar ->
                                scope.launch {
                                    if (feiraRepository.salvarDadosCompletosFeira(feiraParaSalvar)) {
                                        Toast.makeText(context, "Feira ${feiraParaSalvar.fairDetails.feiraId} salva!", Toast.LENGTH_SHORT).show()
                                        isCurrentFeiraPersisted = true
                                        navController.navigate(AppRoutes.HOME_SCREEN) {
                                            popUpTo(AppRoutes.HOME_SCREEN) { inclusive = true }
                                        }
                                    } else {
                                        Toast.makeText(context, "Erro ao salvar Feira ${feiraParaSalvar.fairDetails.feiraId}.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                } ?: run {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Não foi possível calcular ou exibir os resultados para a Feira ${currentFeiraLocal?.fairDetails?.feiraId}.")
                        Text("Verifique se há entradas, perdas ou despesas lançadas.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { navController.popBackStack() }) { Text("Voltar") }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}