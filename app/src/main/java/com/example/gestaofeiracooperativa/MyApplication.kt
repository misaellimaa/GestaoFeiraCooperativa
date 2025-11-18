package com.example.gestaofeiracooperativa

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MyApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())
    private val database by lazy { AppDatabase.getDatabase(this, applicationScope) }

    // Repositórios
    val produtoRepository by lazy { ProdutoRepository(database.produtoDao()) }
    val agricultorRepository by lazy { AgricultorRepository(database.agricultorDao()) }
    val itemDespesaRepository by lazy { ItemDespesaRepository(database.itemDespesaDao()) }
    val despesaFeiraRepository by lazy { DespesaFeiraRepository(database.despesaFeiraDao()) }
    val entradaRepository by lazy { EntradaRepository(database.entradaDao()) }
    val perdaRepository by lazy { PerdaRepository(database.perdaDao()) }

    val feiraRepository by lazy {
        FeiraRepository(
            appDatabase = database,
            feiraDao = database.feiraDao(),
            entradaDao = database.entradaDao(),
            perdaDao = database.perdaDao(),
            produtoDao = database.produtoDao(),
            itemDespesaDao = database.itemDespesaDao(),
            despesaFeiraDao = database.despesaFeiraDao(),
            agricultorDao = database.agricultorDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MyApplication", "onCreate: Iniciando app com prioridade LOCAL -> NUVEM.")

        applicationScope.launch(Dispatchers.IO) {
            // 1. Garante que dados estáticos básicos existam (se for primeira instalação)
            migrarDadosIniciaisParaFirestore()

            // 2. UPLOAD: Garante que o que está no celular suba para a nuvem.
            // Isso resolve o problema de "fonte principal é o celular".
            Log.d("MyApplication", "Enviando feiras locais para a nuvem...")
            feiraRepository.sincronizarFeirasLocaisParaNuvem()

            // 3. DOWNLOAD: Atualiza dados estáticos (Produtos, etc) da nuvem
            Log.d("MyApplication", "Baixando dados estáticos da nuvem...")
            iniciarSincronizacaoDeDados()

            // 4. LISTEN: Começa a ouvir atualizações de feiras (sem apagar as locais)
            Log.d("MyApplication", "Iniciando ouvinte de feiras...")
            feiraRepository.iniciarOuvinteDaListaDeFeiras()

            Log.d("MyApplication", "Inicialização concluída.")
        }
    }

    private fun iniciarSincronizacaoDeDados() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                produtoRepository.sincronizarProdutosDoFirestore()
                agricultorRepository.sincronizarAgricultoresDoFirestore()
                itemDespesaRepository.sincronizarItensDespesaDoFirestore()
            } catch (e: Exception) {
                Log.e("MyApplication", "Falha na sincronização de dados estáticos.", e)
            }
        }
    }

    private fun migrarDadosIniciaisParaFirestore() {
        migrarProdutosIniciais()
        migrarAgricultoresIniciais()
    }

    // ... (Funções migrarProdutosIniciais e migrarAgricultoresIniciais permanecem iguais) ...
    private fun migrarProdutosIniciais() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val jaMigrouProdutos = prefs.getBoolean("PRODUTOS_MIGRADOS_V1", false)

        if (!jaMigrouProdutos) {
            Log.d("MyApplication_Migration", "Iniciando migração de produtos para o Firestore...")
            applicationScope.launch {
                try {
                    val firestore = Firebase.firestore
                    val produtosCollection = firestore.collection("produtos")
                    val snapshot = produtosCollection.limit(1).get().await()
                    if (snapshot.isEmpty) {
                        val produtosDoCsv = loadProductsFromAssets(this@MyApplication, "CAD PROD.csv")
                        if (produtosDoCsv.isNotEmpty()) {
                            val batch = firestore.batch()
                            produtosDoCsv.forEach { produto ->
                                batch.set(produtosCollection.document(produto.numero), produto)
                            }
                            batch.commit().await()
                            prefs.edit().putBoolean("PRODUTOS_MIGRADOS_V1", true).apply()
                            Log.d("MyApplication_Migration", "SUCESSO! Produtos iniciais migrados para o Firestore.")
                        }
                    } else {
                        prefs.edit().putBoolean("PRODUTOS_MIGRADOS_V1", true).apply()
                        Log.d("MyApplication_Migration", "Firestore de produtos já contém dados. Migração pulada.")
                    }
                } catch (e: Exception) {
                    Log.e("MyApplication_Migration", "ERRO na migração de produtos.", e)
                }
            }
        }
    }

    private fun migrarAgricultoresIniciais() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val jaMigrouAgricultores = prefs.getBoolean("AGRICULTORES_MIGRADOS_V1", false)

        if (!jaMigrouAgricultores) {
            Log.d("MyApplication_Migration", "Iniciando migração de agricultores para o Firestore...")
            applicationScope.launch {
                try {
                    val firestore = Firebase.firestore
                    val agricultoresCollection = firestore.collection("agricultores")
                    val snapshot = agricultoresCollection.limit(1).get().await()
                    if (snapshot.isEmpty) {
                        val agricultoresIniciais = (1..28).map { id ->
                            Agricultor(id = id.toString(), nome = "Agricultor ${String.format("%02d", id)}")
                        }
                        val batch = firestore.batch()
                        agricultoresIniciais.forEach { agricultor ->
                            batch.set(agricultoresCollection.document(agricultor.id), agricultor)
                        }
                        batch.commit().await()
                        prefs.edit().putBoolean("AGRICULTORES_MIGRADOS_V1", true).apply()
                        Log.d("MyApplication_Migration", "SUCESSO! Agricultores iniciais migrados para o Firestore.")
                    } else {
                        prefs.edit().putBoolean("AGRICULTORES_MIGRADOS_V1", true).apply()
                        Log.d("MyApplication_Migration", "Firestore de agricultores já contém dados. Migração pulada.")
                    }
                } catch (e: Exception) {
                    Log.e("MyApplication_Migration", "ERRO na migração de agricultores.", e)
                }
            }
        }
    }
}