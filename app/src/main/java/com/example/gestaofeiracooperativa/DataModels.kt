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
    val feiraId: String,
    val startDate: String,
    val endDate: String
)

@Serializable
@Entity(tableName = "produtos")
data class Produto(
    @PrimaryKey
    val numero: String,
    val item: String,
    val unidade: String,
    val valorUnidade: Double
)

@Serializable
@Entity(tableName = "agricultores")
data class Agricultor(
    @PrimaryKey
    val id: String,
    val nome: String
)

@Entity(tableName = "feiras")
data class FeiraEntity(
    @PrimaryKey
    val feiraId: String,
    val startDate: String,
    val endDate: String,
    val resultadoGeralJson: String? = null
)

// <<< ALTERAÇÃO 1: Adicionado novo campo 'quantidadeSobra' >>>
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
    val feiraId: String,
    val agricultorId: String,
    val produtoNumero: String,
    // Este é o "crédito" de sobras da feira anterior que é distribuído
    @ColumnInfo(name = "quantidade_sobra", defaultValue = "0.0") // defaultValue para migrações mais fáceis
    val quantidadeSobra: Double = 0.0,
    // Este armazena as novas entradas da semana atual
    val quantidadesPorDiaJson: String
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
    val feiraId: String,
    val produtoNumero: String,
    val perdasPorDiaJson: String, // Representa o "S PLAN" (perda total)
    // Este é o valor que o usuário digita como a "SOBRA REAL"
    @ColumnInfo(name = "quantidade_sobra", defaultValue = "0.0") // defaultValue para migrações mais fáceis
    val quantidadeSobra: Double = 0.0
)
data class SobraUiItem(
    val produto: Produto,
    val perdaTotalCalculada: Double,
    var sobraRealInput: String, // O valor que o usuário digita
    val perdaEntityOriginal: PerdaEntity
)


// --- As data classes abaixo não são entidades de banco, mas modelos para UI/Lógica ---

@Serializable
data class EntradaItemAgricultor(
    val produto: Produto,
    val quantidadeSobraDaSemanaAnterior: Double = 0.0,
    val quantidadesPorDia: Map<String, Double>
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
    val quantidadeSobraAnterior: Double, // <<< NOVO: Guarda apenas a sobra da feira anterior
    val quantidadeEntradaSemana: Double, // <<< NOVO: Guarda apenas as entradas da semana atual
    val contribuicaoTotal: Double,       // <<< NOVO: Soma dos dois acima (substitui o antigo 'quantidadeEntregueTotalSemana')
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
