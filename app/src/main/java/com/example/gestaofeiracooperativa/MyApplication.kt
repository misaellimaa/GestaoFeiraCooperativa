package com.example.gestaofeiracooperativa // <<--- MUDE PARA O SEU PACKAGE REAL

import android.app.Application
import com.example.gestaofeiracooperativa.AppDatabase // Importe sua classe de banco de dados
import com.example.gestaofeiracooperativa.ProdutoRepository // Importe seu repositório de produto
import com.example.gestaofeiracooperativa.AgricultorRepository // Importe seu repositório de agricultor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MyApplication : Application() {
    // CoroutineScope para operações de banco de dados (que podem ser longas)
    // SupervisorJob permite que coroutines filhas falhem independentemente
    val applicationScope = CoroutineScope(SupervisorJob())

    // Instância do banco de dados (inicializada lazy)
    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }

    // Instâncias dos repositórios (inicializadas lazy)
    val produtoRepository by lazy { ProdutoRepository(database.produtoDao()) }
    val agricultorRepository by lazy { AgricultorRepository(database.agricultorDao()) }
    val itemDespesaRepository by lazy { ItemDespesaRepository(database.itemDespesaDao()) }
    val lancamentoDespesaRepository by lazy { LancamentoDespesaRepository(database.lancamentoMensalDespesaDao()) }
    val despesaFeiraRepository by lazy { DespesaFeiraRepository(database.despesaFeiraDao()) }
}
