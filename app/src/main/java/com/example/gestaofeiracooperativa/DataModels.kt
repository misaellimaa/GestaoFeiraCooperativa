package com.example.gestaofeiracooperativa

import kotlinx.serialization.Serializable
import androidx.room.Entity // ADICIONAR IMPORT
import androidx.room.PrimaryKey // ADICIONAR IMPORT
import androidx.room.ForeignKey // ADICIONAR IMPORT
import androidx.room.Index

val diasDaSemanaFeira = listOf("TER", "QUA", "QUI", "SEX", "SAB", "DOM")

@Serializable
data class FairDetails(
    val feiraId: String,
    val startDate: String,
    val endDate: String
)

@Serializable
@Entity(tableName = "produtos") // Anotação para indicar que é uma tabela do Room
data class Produto(
    @PrimaryKey // Anotação para chave primária
    val numero: String,
    val item: String,
    val unidade: String,
    val valorUnidade: Double
)

@Serializable
@Entity(tableName = "agricultores") // <<--- ADICIONE ESTAS ANOTAÇÕES PARA AGRICULTOR
data class Agricultor(
    @PrimaryKey // Define 'id' como a chave primária
    val id: String, // ID do agricultor (ex: "1", "25")
    val nome: String // Nome do agricultor (ex: "José Silva")
)

// Nova Entidade Principal da Feira
@Entity(tableName = "feiras")
data class FeiraEntity(
    @PrimaryKey
    val feiraId: String,
    val startDate: String,
    val endDate: String,
    // Se decidir armazenar o resultado geral como JSON:
    val resultadoGeralJson: String? = null
)

// Nova Entidade para Entradas de Produtos por Agricultor
@Entity(
    tableName = "entradas_feira",
    primaryKeys = ["feiraId", "agricultorId", "produtoNumero"], // Chave primária composta
    foreignKeys = [
        ForeignKey(
            entity = FeiraEntity::class,
            parentColumns = ["feiraId"],
            childColumns = ["feiraId"],
            onDelete = ForeignKey.CASCADE // Se uma feira for deletada, suas entradas são deletadas
        ),
        ForeignKey(
            entity = Agricultor::class,
            parentColumns = ["id"],
            childColumns = ["agricultorId"],
            onDelete = ForeignKey.CASCADE // Se um agricultor for deletado, suas entradas são deletadas
        ),
        ForeignKey(
            entity = Produto::class,
            parentColumns = ["numero"],
            childColumns = ["produtoNumero"],
            onDelete = ForeignKey.CASCADE // Se um produto for deletado, suas entradas são deletadas
        )
    ],
    indices = [ // <<< ADICIONE ESTE BLOCO DE ÍNDICES
        Index(value = ["agricultorId"]),
        Index(value = ["produtoNumero"])
    ]
)
data class EntradaEntity(
    val feiraId: String,
    val agricultorId: String,
    val produtoNumero: String,
    val quantidadesPorDiaJson: String // Armazenará o Map<String, Double> como JSON
)

// Nova Entidade para Perdas de Produtos da Feira
@Entity(
    tableName = "perdas_feira",
    primaryKeys = ["feiraId", "produtoNumero"], // Chave primária composta
    foreignKeys = [
        ForeignKey(
            entity = FeiraEntity::class,
            parentColumns = ["feiraId"],
            childColumns = ["feiraId"],
            onDelete = ForeignKey.CASCADE // Se uma feira for deletada, suas perdas são deletadas
        ),
        ForeignKey(
            entity = Produto::class,
            parentColumns = ["numero"],
            childColumns = ["produtoNumero"],
            onDelete = ForeignKey.CASCADE // Se um produto for deletado, suas perdas são deletadas
        )
    ],
    indices = [ // <<< ADICIONE ESTE BLOCO DE ÍNDICES
        Index(value = ["produtoNumero"])
    ]
)
data class PerdaEntity(
    val feiraId: String,
    val produtoNumero: String,
    val perdasPorDiaJson: String // Armazenará o Map<String, Double> como JSON
)
@Serializable
data class EntradaItemAgricultor(
    val produto: Produto,
    val quantidadesPorDia: Map<String, Double>
) {
    fun getTotalEntregueNaSemana(): Double {
        return quantidadesPorDia.values.sum()
    }
}

@Serializable
data class PerdaItemFeira(
    val produto: Produto,
    var perdasPorDia: Map<String, Double>
) {
    fun getTotalPerdidoNaSemana(): Double {
        return perdasPorDia.values.sum()
    }
}

@Serializable
data class ItemProcessadoAgricultor(
    val produto: Produto,
    val quantidadeEntregueTotalSemana: Double,
    val quantidadePerdaAlocada: Double,
    val quantidadeVendida: Double,
    val valorTotalVendido: Double
)

@Serializable
data class ResultadoAgricultorFeira(
    val agricultorId: String,
    val itensProcessados: List<ItemProcessadoAgricultor>,
    val totalVendidoBrutoAgricultor: Double,
    val valorCooperativa: Double,
    val valorLiquidoAgricultor: Double
)

@Serializable
data class ResultadoGeralFeira(
    val fairDetails: FairDetails,
    val resultadosPorAgricultor: List<ResultadoAgricultorFeira>,
    val totalGeralVendido: Double,
    val totalGeralCooperativa: Double,
    val totalGeralAgricultores: Double
)

@Serializable
data class DadosCompletosFeira(
    val fairDetails: FairDetails,
    val entradasTodosAgricultores: Map<String, List<EntradaItemAgricultor>>,
    val perdasTotaisDaFeira: List<PerdaItemFeira>,
    val despesasDaFeira: List<DespesaFeiraUiItem>,
    val resultadoGeralCalculado: ResultadoGeralFeira?
)
