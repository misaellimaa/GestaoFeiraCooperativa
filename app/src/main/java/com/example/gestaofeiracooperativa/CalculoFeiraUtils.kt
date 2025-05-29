package com.example.gestaofeiracooperativa

// As data classes Produto, EntradaItemAgricultor, PerdaItemFeira,
// ItemProcessadoAgricultor, ResultadoAgricultorFeira, ResultadoGeralFeira
// e FairDetails
// devem estar definidas em DataModels.kt

fun calcularResultadosFeira(
    // MUDANÇA AQUI: Recebe o objeto FairDetails completo
    fairDetails: FairDetails,
    entradasTodosAgricultores: Map<String, List<EntradaItemAgricultor>>,
    perdasTotaisDaFeira: List<PerdaItemFeira>,
    catalogoProdutos: List<Produto>
): ResultadoGeralFeira {

    val resultadosPorAgricultor = mutableListOf<ResultadoAgricultorFeira>()
    var acumuladoTotalGeralVendido = 0.0
    var acumuladoTotalGeralCooperativa = 0.0
    var acumuladoTotalGeralAgricultores = 0.0

    // 1. Criar um mapa de perdas TOTAIS NA SEMANA por ID de produto para fácil acesso
    val mapaPerdasSemanaPorProdutoId = perdasTotaisDaFeira.associate {
        it.produto.numero to it.getTotalPerdidoNaSemana()
    }

    // 2. Calcular a quantidade total entregue NA SEMANA de cada produto por TODOS os agricultores
    val mapaTotalEntregueSemanaPorProdutoId = mutableMapOf<String, Double>()
    entradasTodosAgricultores.values.flatten().forEach { entradaItem ->
        val totalAtualProduto = mapaTotalEntregueSemanaPorProdutoId.getOrDefault(entradaItem.produto.numero, 0.0)
        mapaTotalEntregueSemanaPorProdutoId[entradaItem.produto.numero] = totalAtualProduto + entradaItem.getTotalEntregueNaSemana()
    }

    // 3. Processar cada agricultor
    entradasTodosAgricultores.forEach { (agricultorId, listaDeEntradasDoAgricultor) ->
        val itensProcessadosParaEsteAgricultor = mutableListOf<ItemProcessadoAgricultor>()
        var subTotalVendidoBrutoAgricultor = 0.0

        listaDeEntradasDoAgricultor.forEach { entradaItemDoAgricultor ->
            val produto = entradaItemDoAgricultor.produto
            val quantidadeEntreguePeloAgricultorParaEsteProdutoSemana = entradaItemDoAgricultor.getTotalEntregueNaSemana()

            val perdaTotalDoProdutoNaFeiraSemana = mapaPerdasSemanaPorProdutoId[produto.numero] ?: 0.0
            val quantidadeTotalEntregueDoProdutoPorTodosSemana = mapaTotalEntregueSemanaPorProdutoId[produto.numero] ?: 0.0

            var perdaAlocadaParaEsteItem = 0.0
            if (quantidadeTotalEntregueDoProdutoPorTodosSemana > 0 && quantidadeEntreguePeloAgricultorParaEsteProdutoSemana > 0) {
                perdaAlocadaParaEsteItem = (quantidadeEntreguePeloAgricultorParaEsteProdutoSemana / quantidadeTotalEntregueDoProdutoPorTodosSemana) * perdaTotalDoProdutoNaFeiraSemana
            }

            perdaAlocadaParaEsteItem = perdaAlocadaParaEsteItem.coerceAtMost(quantidadeEntreguePeloAgricultorParaEsteProdutoSemana)

            val quantidadeVendida = (quantidadeEntreguePeloAgricultorParaEsteProdutoSemana - perdaAlocadaParaEsteItem).coerceAtLeast(0.0)
            val valorUnitario = produto.valorUnidade
            val valorTotalVendidoItem = quantidadeVendida * valorUnitario

            itensProcessadosParaEsteAgricultor.add(
                ItemProcessadoAgricultor(
                    produto = produto,
                    quantidadeEntregueTotalSemana = quantidadeEntreguePeloAgricultorParaEsteProdutoSemana,
                    quantidadePerdaAlocada = perdaAlocadaParaEsteItem,
                    quantidadeVendida = quantidadeVendida,
                    valorTotalVendido = valorTotalVendidoItem
                )
            )
            subTotalVendidoBrutoAgricultor += valorTotalVendidoItem
        }

        val valorCooperativa = subTotalVendidoBrutoAgricultor * 0.30
        val valorLiquidoAgricultor = subTotalVendidoBrutoAgricultor * 0.70

        resultadosPorAgricultor.add(
            ResultadoAgricultorFeira(
                agricultorId = agricultorId,
                itensProcessados = itensProcessadosParaEsteAgricultor,
                totalVendidoBrutoAgricultor = subTotalVendidoBrutoAgricultor,
                valorCooperativa = valorCooperativa,
                valorLiquidoAgricultor = valorLiquidoAgricultor
            )
        )

        acumuladoTotalGeralVendido += subTotalVendidoBrutoAgricultor
        acumuladoTotalGeralCooperativa += valorCooperativa
        acumuladoTotalGeralAgricultores += valorLiquidoAgricultor
    }

    return ResultadoGeralFeira(
        fairDetails = fairDetails, // <<--- MUDANÇA AQUI: Passa o objeto FairDetails
        resultadosPorAgricultor = resultadosPorAgricultor,
        totalGeralVendido = acumuladoTotalGeralVendido,
        totalGeralCooperativa = acumuladoTotalGeralCooperativa,
        totalGeralAgricultores = acumuladoTotalGeralAgricultores
    )
}