package com.example.gestaofeiracooperativa


// Em CalculoFeiraUtils.kt
fun calcularResultadosFeira(
    fairDetails: FairDetails,
    entradasTodosAgricultores: Map<String, List<EntradaItemAgricultor>>,
    perdasTotaisDaFeira: List<PerdaItemFeira>,
    catalogoProdutos: List<Produto>
): ResultadoGeralFeira {

    val resultadosPorAgricultor = mutableListOf<ResultadoAgricultorFeira>()
    var acumuladoTotalGeralVendido = 0.0
    var acumuladoTotalGeralCooperativa = 0.0
    var acumuladoTotalGeralAgricultores = 0.0

    val mapaPerdasSemanaPorProdutoId = perdasTotaisDaFeira.associate {
        it.produto.numero to it.getTotalPerdidoNaSemana()
    }

    // Calcula o mapa de contribuição TOTAL (sobra + entradas) por produto
    val mapaContribuicaoTotalPorProdutoId = mutableMapOf<String, Double>()
    entradasTodosAgricultores.values.flatten().forEach { entradaItem ->
        val totalAtualProduto = mapaContribuicaoTotalPorProdutoId.getOrDefault(entradaItem.produto.numero, 0.0)
        // <<< CORREÇÃO: Usa a contribuição total >>>
        mapaContribuicaoTotalPorProdutoId[entradaItem.produto.numero] = totalAtualProduto + entradaItem.getContribuicaoTotalParaFeira()
    }

    entradasTodosAgricultores.forEach { (agricultorId, listaDeEntradasDoAgricultor) ->
        if (listaDeEntradasDoAgricultor.isNotEmpty()) {
            val itensProcessadosParaEsteAgricultor = mutableListOf<ItemProcessadoAgricultor>()
            var subTotalVendidoBrutoAgricultor = 0.0

            listaDeEntradasDoAgricultor.forEach { entradaItemDoAgricultor ->
                val produto = entradaItemDoAgricultor.produto
                // <<< CORREÇÃO: Usa a contribuição total >>>
                val contribuicaoTotalDoAgricultor = entradaItemDoAgricultor.getContribuicaoTotalParaFeira()
                val sobraAnterior = entradaItemDoAgricultor.quantidadeSobraDaSemanaAnterior
                val entradasDaSemana = entradaItemDoAgricultor.getTotalEntradasDaSemana()

                val perdaTotalDoProdutoNaFeira = mapaPerdasSemanaPorProdutoId[produto.numero] ?: 0.0
                val contribuicaoTotalDoProduto = mapaContribuicaoTotalPorProdutoId[produto.numero] ?: 0.0
                var perdaAlocadaParaEsteItem = 0.0

                if (contribuicaoTotalDoProduto > 0 && contribuicaoTotalDoAgricultor > 0) {
                    perdaAlocadaParaEsteItem = (contribuicaoTotalDoAgricultor / contribuicaoTotalDoProduto) * perdaTotalDoProdutoNaFeira
                }

                perdaAlocadaParaEsteItem = perdaAlocadaParaEsteItem.coerceAtMost(contribuicaoTotalDoAgricultor)

                val quantidadeVendida = (contribuicaoTotalDoAgricultor - perdaAlocadaParaEsteItem).coerceAtLeast(0.0)
                val valorUnitario = produto.valorUnidade
                val valorTotalVendidoItem = quantidadeVendida * valorUnitario

                itensProcessadosParaEsteAgricultor.add(
                    // <<< ALTERAÇÃO: Preenche os novos campos >>>
                    ItemProcessadoAgricultor(
                        produto = produto,
                        quantidadeSobraAnterior = sobraAnterior,
                        quantidadeEntradaSemana = entradasDaSemana,
                        contribuicaoTotal = contribuicaoTotalDoAgricultor,
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
    }

    return ResultadoGeralFeira(
        fairDetails = fairDetails,
        resultadosPorAgricultor = resultadosPorAgricultor,
        totalGeralVendido = acumuladoTotalGeralVendido,
        totalGeralCooperativa = acumuladoTotalGeralCooperativa,
        totalGeralAgricultores = acumuladoTotalGeralAgricultores
    )
}