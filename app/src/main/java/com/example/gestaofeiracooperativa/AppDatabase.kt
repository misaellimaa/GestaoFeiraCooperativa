package com.example.gestaofeiracooperativa // <<--- MUDE PARA O SEU PACKAGE REAL

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters // Novo import para @TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Database(
    entities = [
        Produto::class,         // Entidade existente
        Agricultor::class,      // Entidade existente
        FeiraEntity::class,     // <<< NOVA ENTIDADE ADICIONADA
        EntradaEntity::class,   // <<< NOVA ENTIDADE ADICIONADA
        PerdaEntity::class,      // <<< NOVA ENTIDADE ADICIONA
        ItemDespesaEntity::class,     // <<< NOVA ENTIDADE ADICIONADA
        DespesaFeiraEntity::class
    ],
    version = 6, // <<< VERSÃO INCREMENTADA (era 1)
    exportSchema = false
)
@TypeConverters(
    MapStringDoubleConverter::class // <<< TYPECONVERTER REGISTRADO
    // Adicione outros TypeConverters aqui se necessário, separados por vírgula
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun produtoDao(): ProdutoDao         // DAO existente
    abstract fun agricultorDao(): AgricultorDao   // DAO existente

    // Declarações para os NOVOS DAOs
    abstract fun feiraDao(): FeiraDao           // <<< NOVO DAO
    abstract fun entradaDao(): EntradaDao         // <<< NOVO DAO
    abstract fun perdaDao(): PerdaDao             // <<< NOVO DAO
    abstract fun despesaFeiraDao(): DespesaFeiraDao // <<< NOVO DAO

    // Declarações para os NOVOS DAOs de Despesa
    abstract fun itemDespesaDao(): ItemDespesaDao                 // <<< NOVO DAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "feira_database" // Nome do arquivo do banco de dados
                )
                    // Essencial ao mudar a versão do schema e não ter migrações definidas:
                    .fallbackToDestructiveMigration() // Recria o banco se a versão mudar (apaga dados!)
                    .addCallback(AppDatabaseCallback(scope, context)) // Adiciona callback para população inicial
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope,
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    // A lógica de populateDatabase existente foca em Produto e Agricultor,
                    // o que está bom. As feiras serão criadas/gerenciadas pela UI.
                    populateInitialStaticData(database.produtoDao(), database.agricultorDao(), context)
                }
            }
        }

        // Renomeei a função para maior clareza, já que agora ela popula apenas dados estáticos iniciais.
        suspend fun populateInitialStaticData(produtoDao: ProdutoDao, agricultorDao: AgricultorDao, context: Context) {
            // Popula Produtos
            Log.d("DB_POPULATE", "Iniciando populateInitialStaticData...")
            val produtosDoCsv = loadProductsFromAssets(context, "CAD PROD.csv")
            Log.d("DB_POPULATE", "loadProductsFromAssets retornou ${produtosDoCsv.size} produtos.")
            if (produtosDoCsv.isNotEmpty()) {
                produtoDao.insertAll(produtosDoCsv)
                println("LOG_DB: Banco de dados Room populado com ${produtosDoCsv.size} produtos do CSV.")
            } else {
                val produtosIniciaisFixos = listOf(
                    Produto(numero = "1", item = "ABACATE", unidade = "KG", valorUnidade = 10.00),
                    Produto(numero = "2", item = "ABACAXI", unidade = "UN", valorUnidade = 8.00),
                    Produto(numero = "3", item = "ALFACE CRESPA", unidade = "UN", valorUnidade = 3.50)
                )
                produtoDao.insertAll(produtosIniciaisFixos)
                println("LOG_DB: Banco de dados Room populado com produtos de exemplo (CSV vazio ou não encontrado).")
            }

            // Popula Agricultores
            val agricultoresIniciais = (1..28).map { id -> Agricultor(id = id.toString(), nome = "Agricultor ${String.format("%02d", id)}") }
            Log.d("DB_POPULATE", "Preparando para inserir ${agricultoresIniciais.size} agricultores. Ex: ID=${agricultoresIniciais.firstOrNull()?.id}")
            try {
                agricultorDao.insertAll(agricultoresIniciais)
                Log.d("DB_POPULATE", "SUCESSO ao chamar agricultorDao.insertAll com ${agricultoresIniciais.size} agricultores.")
            } catch (e: Exception) {
                Log.e("DB_POPULATE", "ERRO ao chamar agricultorDao.insertAll: ${e.message}", e)
            }
            Log.d("DB_POPULATE", "Fim de populateInitialStaticData.")
        }
    }
}