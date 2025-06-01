package com.example.gestaofeiracooperativa

// As data classes Produto, EntradaItemAgricultor, PerdaItemFeira,
// ItemProcessadoAgricultor, ResultadoAgricultorFeira, ResultadoGeralFeira
// e FairDetails
// devem estar definidas em DataModels.kt

fun calcularResultadosFeira(
    fairDetails: FairDetails,
    entradasTodosAgricultores: Map<String, List<EntradaItemAgricultor>>,
    perdasTotaisDaFeira: List<PerdaItemFeira>,
    catalogoProdutos: List<Produto> // Lembre-se que esta lista pode precisar vir do banco para ter valores atualizados
): ResultadoGeralFeira {

    val resultadosPorAgricultor = mutableListOf<ResultadoAgricultorFeira>()
    var acumuladoTotalGeralVendido = 0.0
    var acumuladoTotalGeralCooperativa = 0.0
    var acumuladoTotalGeralAgricultores = 0.0

    val mapaPerdasSemanaPorProdutoId = perdasTotaisDaFeira.associate {
        it.produto.numero to it.getTotalPerdidoNaSemana()
    }

    val mapaTotalEntregueSemanaPorProdutoId = mutableMapOf<String, Double>()
    entradasTodosAgricultores.values.flatten().forEach { entradaItem ->
        val totalAtualProduto = mapaTotalEntregueSemanaPorProdutoId.getOrDefault(entradaItem.produto.numero, 0.0)
        mapaTotalEntregueSemanaPorProdutoId[entradaItem.produto.numero] = totalAtualProduto + entradaItem.getTotalEntregueNaSemana()
    }

    entradasTodosAgricultores.forEach { (agricultorId, listaDeEntradasDoAgricultor) ->
        // <<< ALTERAÇÃO AQUI: Processa o agricultor apenas se ele tiver entradas >>>
        if (listaDeEntradasDoAgricultor.isNotEmpty()) {
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
                val valorUnitario = produto.valorUnidade // ATENÇÃO: Este valor virá da lista 'catalogoProdutos'
                // que pode estar usando o CSV original, não o valor atualizado do produto no banco.
                // Para usar o valor atualizado, 'catalogoProdutos' deveria ser 'todosOsProdutosState'
                // e você buscaria o produto daqui para pegar o valorUnitario mais recente.
                val valorTotalVendidoItem = quantidadeVendida * valorUnitario

                itensProcessadosParaEsteAgricultor.add(
                    ItemProcessadoAgricultor(
                        produto = produto, // Este objeto Produto vem das entradas, pode ter valorUnitario desatualizado
                        quantidadeEntregueTotalSemana = quantidadeEntreguePeloAgricultorParaEsteProdutoSemana,
                        quantidadePerdaAlocada = perdaAlocadaParaEsteItem,
                        quantidadeVendida = quantidadeVendida,
                        valorTotalVendido = valorTotalVendidoItem
                    )
                )
                subTotalVendidoBrutoAgricultor += valorTotalVendidoItem
            }

            val valorCooperativa = subTotalVendidoBrutoAgricultor * 0.30 // Exemplo: 30% para cooperativa
            val valorLiquidoAgricultor = subTotalVendidoBrutoAgricultor * 0.70 // Exemplo: 70% para agricultor

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
    }

    return ResultadoGeralFeira(
        fairDetails = fairDetails,
        resultadosPorAgricultor = resultadosPorAgricultor,
        totalGeralVendido = acumuladoTotalGeralVendido,
        totalGeralCooperativa = acumuladoTotalGeralCooperativa,
        totalGeralAgricultores = acumuladoTotalGeralAgricultores
    )
}