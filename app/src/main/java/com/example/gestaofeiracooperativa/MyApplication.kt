package com.example.gestaofeiracooperativa

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MyApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())
    private val database by lazy { AppDatabase.getDatabase(this, applicationScope) }

    // Repositórios existentes
    val produtoRepository by lazy { ProdutoRepository(database.produtoDao()) }
    val agricultorRepository by lazy { AgricultorRepository(database.agricultorDao()) }
    val itemDespesaRepository by lazy { ItemDespesaRepository(database.itemDespesaDao()) }
    val despesaFeiraRepository by lazy { DespesaFeiraRepository(database.despesaFeiraDao()) } // Para DespesaFeiraEntity

    // <<< ADICIONAR FeiraRepository AQUI >>>
    val feiraRepository by lazy {
        FeiraRepository(
            appDatabase = database,
            feiraDao = database.feiraDao(),
            entradaDao = database.entradaDao(),
            perdaDao = database.perdaDao(),
            produtoDao = database.produtoDao(),
            agricultorDao = database.agricultorDao(),
            itemDespesaDao = database.itemDespesaDao(),
            despesaFeiraDao = database.despesaFeiraDao(),
            applicationContext = this // 'this' aqui se refere à instância de MyApplication
        )
    }
    val perdaRepository by lazy { PerdaRepository(database.perdaDao()) }
    val entradaRepository by lazy { EntradaRepository(database.entradaDao()) }
}