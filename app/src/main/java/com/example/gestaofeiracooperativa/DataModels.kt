package com.example.gestaofeiracooperativa

import kotlinx.serialization.Serializable
import androidx.room.Entity // ADICIONAR IMPORT
import androidx.room.PrimaryKey // ADICIONAR IMPORT
import androidx.room.ForeignKey // ADICIONAR IMPORT
import androidx.room.Index
import androidx.room.ColumnInfo
val diasDaSemanaFeira = listOf("TER", "QUA", "QUI", "SEX", "SAB", "DOM")

@Serializable
data class FairDetails(
    val feiraId: String = "",
    val startDate: String = "",
    val endDate: String = ""
)

@Serializable
@Entity(tableName = "produtos")
data class Produto(
    @PrimaryKey
    val numero: String = "", // <<< Adicione = ""
    val item: String = "",       // <<< Adicione = ""
    val unidade: String = "",    // <<< Adicione = ""
    val valorUnidade: Double = 0.0 // <<< Adicione = 0.0
)

@Serializable
@Entity(tableName = "agricultores")
data class Agricultor(
    @PrimaryKey
    val id: String = "",
    val nome: String = ""
)

@Entity(tableName = "feiras")
data class FeiraEntity(
    @PrimaryKey
    val feiraId: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val resultadoGeralJson: String? = null
)


@Entity(
    tableName = "entradas_feira",
    primaryKeys = ["feiraId", "agricultorId", "produtoNumero"],
    foreignKeys = [
        ForeignKey(entity = FeiraEntity::class, parentColumns = ["feiraId"], childColumns = ["feiraId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Agricultor::class, parentColumns = ["id"], childColumns = ["agricultorId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Produto::class, parentColumns = ["numero"], childColumns = ["produtoNumero"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["agricultorId"]), Index(value = ["produtoNumero"])]
)
data class EntradaEntity(
    val feiraId: String = "",
    val agricultorId: String = "",
    val produtoNumero: String = "",
    @ColumnInfo(name = "quantidade_sobra", defaultValue = "0.0")
    val quantidadeSobra: Double = 0.0,
    val quantidadesPorDiaJson: String = "{}"
)

// <<< ALTERAÇÃO 2: Adicionado novo campo 'quantidadeSobra' >>>
@Entity(
    tableName = "perdas_feira",
    primaryKeys = ["feiraId", "produtoNumero"],
    foreignKeys = [
        ForeignKey(entity = FeiraEntity::class, parentColumns = ["feiraId"], childColumns = ["feiraId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Produto::class, parentColumns = ["numero"], childColumns = ["produtoNumero"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["produtoNumero"])]
)
data class PerdaEntity(
    val feiraId: String = "",
    val produtoNumero: String = "",
    val perdasPorDiaJson: String = "{}",
    @ColumnInfo(name = "quantidade_sobra", defaultValue = "0.0")
    val quantidadeSobra: Double = 0.0
)
data class SobraUiItem(
    val produto: Produto = Produto(), // Usa o construtor padrão de Produto
    val perdaTotalCalculada: Double = 0.0,
    var sobraRealInput: String = "",
    val perdaEntityOriginal: PerdaEntity = PerdaEntity()
)


// --- As data classes abaixo não são entidades de banco, mas modelos para UI/Lógica ---

@Serializable
data class EntradaItemAgricultor(
    val produto: Produto? = null,
    val quantidadeSobraDaSemanaAnterior: Double = 0.0,
    val quantidadesPorDia: Map<String, Double> = emptyMap()
) {
    fun getTotalEntradasDaSemana(): Double {
        return quantidadesPorDia.values.sum()
    }
    fun getContribuicaoTotalParaFeira(): Double {
        return quantidadeSobraDaSemanaAnterior + getTotalEntradasDaSemana()
    }
}

@Serializable
data class PerdaItemFeira(
    val produto: Produto? = null, // Nulável
    var perdasPorDia: Map<String, Double> = emptyMap(),
    val quantidadeSobra: Double = 0.0
) {
    fun getTotalPerdidoNaSemana(): Double {
        return perdasPorDia.values.sum()
    }
}

@Serializable
data class ItemProcessadoAgricultor(
    val produto: Produto? = null, // Nulável
    val quantidadeSobraAnterior: Double = 0.0,
    val quantidadeEntradaSemana: Double = 0.0,
    val contribuicaoTotal: Double = 0.0,
    val quantidadePerdaAlocada: Double = 0.0,
    val quantidadeVendida: Double = 0.0,
    val valorTotalVendido: Double = 0.0
)

@Serializable
data class ResultadoAgricultorFeira(
    val agricultorId: String = "",
    val itensProcessados: List<ItemProcessadoAgricultor> = emptyList(),
    val totalVendidoBrutoAgricultor: Double = 0.0,
    val valorCooperativa: Double = 0.0,
    val valorLiquidoAgricultor: Double = 0.0
)

@Serializable
data class ResultadoGeralFeira(
    val fairDetails: FairDetails = FairDetails(),
    val resultadosPorAgricultor: List<ResultadoAgricultorFeira> = emptyList(),
    val totalGeralVendido: Double = 0.0,
    val totalGeralCooperativa: Double = 0.0,
    val totalGeralAgricultores: Double = 0.0
)

@Serializable
data class DadosCompletosFeira(
    val fairDetails: FairDetails = FairDetails(),
    val entradasTodosAgricultores: Map<String, List<EntradaItemAgricultor>> = emptyMap(),
    val perdasTotaisDaFeira: List<PerdaItemFeira> = emptyList(),
    val despesasDaFeira: List<DespesaFeiraUiItem> = emptyList(),
    val resultadoGeralCalculado: ResultadoGeralFeira? = null
)
