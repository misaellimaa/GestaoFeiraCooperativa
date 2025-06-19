package com.example.gestaofeiracooperativa

// Seus imports para as data classes (FairDetails, EntradaItemAgricultor, etc.)
// devem estar aqui se não estiverem no mesmo pacote.

fun calcularResultadosFeira(
    fairDetails: FairDetails,
    entradasTodosAgricultores: Map<String, List<EntradaItemAgricultor>>,
    perdasTotaisDaFeira: List<PerdaItemFeira>,
    catalogoProdutos: List<Produto> // Lembre-se que para ter preços atualizados, esta lista deve vir do banco
): ResultadoGeralFeira {

    val resultadosPorAgricultor = mutableListOf<ResultadoAgricultorFeira>()
    var acumuladoTotalGeralVendido = 0.0
    var acumuladoTotalGeralCooperativa = 0.0
    var acumuladoTotalGeralAgricultores = 0.0

    // 1. Criar um mapa de perdas totais por ID de produto
    val mapaPerdasSemanaPorProdutoId = perdasTotaisDaFeira.associate { perdaItem ->
        // <<< CORREÇÃO: Acesso seguro à propriedade 'numero' >>>
        perdaItem.produto?.numero to perdaItem.getTotalPerdidoNaSemana()
    }

    // 2. Calcular a contribuição TOTAL (sobra + entradas da semana) de cada produto
    val mapaContribuicaoTotalPorProdutoId = mutableMapOf<String, Double>()
    entradasTodosAgricultores.values.flatten().forEach { entradaItem ->
        entradaItem.produto?.let { produto -> // Garante que o produto não seja nulo
            val totalAtualProduto = mapaContribuicaoTotalPorProdutoId.getOrDefault(produto.numero, 0.0)
            // <<< CORREÇÃO: Usa a função getContribuicaoTotalParaFeira() >>>
            mapaContribuicaoTotalPorProdutoId[produto.numero] = totalAtualProduto + entradaItem.getContribuicaoTotalParaFeira()
        }
    }

    // 3. Processar cada agricultor
    entradasTodosAgricultores.forEach { (agricultorId, listaDeEntradasDoAgricultor) ->
        if (listaDeEntradasDoAgricultor.isNotEmpty()) {
            val itensProcessadosParaEsteAgricultor = mutableListOf<ItemProcessadoAgricultor>()
            var subTotalVendidoBrutoAgricultor = 0.0

            listaDeEntradasDoAgricultor.forEach { entradaItemDoAgricultor ->
                entradaItemDoAgricultor.produto?.let { produto -> // Garante que o produto não seja nulo
                    // <<< CORREÇÃO: Usa a função getContribuicaoTotalParaFeira() >>>
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

                    // Para ter certeza que o valor unitário é o mais atual, buscamos do catálogo geral
                    val valorUnitario = catalogoProdutos.find { it.numero == produto.numero }?.valorUnidade ?: produto.valorUnidade
                    val valorTotalVendidoItem = quantidadeVendida * valorUnitario

                    itensProcessadosParaEsteAgricultor.add(
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