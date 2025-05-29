package com.example.gestaofeiracooperativa

import kotlinx.serialization.Serializable

val diasDaSemanaFeira = listOf("TER", "QUA", "QUI", "SEX", "SAB", "DOM")

@Serializable
data class FairDetails(
    val feiraId: String,
    val startDate: String,
    val endDate: String
)

@Serializable
data class Produto(
    val numero: String,
    val item: String,
    val unidade: String,
    val valorUnidade: Double
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
    val resultadoGeralCalculado: ResultadoGeralFeira?
)
