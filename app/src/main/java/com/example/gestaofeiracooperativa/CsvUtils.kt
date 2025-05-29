package com.example.gestaofeiracooperativa

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
// import java.text.NumberFormat // Se necessário para formatos de moeda complexos
// import java.util.Locale

fun loadProductsFromAssets(context: Context, fileName: String): List<Produto> {
    val products = mutableListOf<Produto>()
    val assetManager = context.assets
    try {
        assetManager.open(fileName).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                var line = reader.readLine() // Pula o cabeçalho
                if (line == null) {
                    println("LOG_CSV: Arquivo CSV '$fileName' está vazio ou não tem cabeçalho.")
                    return emptyList()
                }

                while (true) {
                    line = reader.readLine() ?: break
                    val tokens = line.split(';') // Delimitador é PONTO E VÍRGULA

                    if (tokens.size >= 4) {
                        try {
                            val numero = tokens[0].trim()
                            val item = tokens[1].trim()
                            val unidade = tokens[2].trim()

                            val valorStr = tokens[3].trim()
                                .replace("R$", "")
                                .replace(".", "") // Remove separador de milhar se existir (ex: 1.234,50 -> 1234,50)
                                .replace(",", ".") // Substitui vírgula decimal por ponto (ex: 1234,50 -> 1234.50)
                                .trim()

                            val valorUnidade = valorStr.toDoubleOrNull() ?: 0.0

                            products.add(Produto(numero, item, unidade, valorUnidade))
                        } catch (e: Exception) {
                            println("LOG_CSV_ERROR: Erro ao parsear linha do produto: '$line' em '$fileName' - ${e.message}")
                        }
                    } else {
                        println("LOG_CSV_WARN: Linha ignorada (número de colunas < 4): '$line' em '$fileName'")
                    }
                }
            }
        }
    } catch (fnf: java.io.FileNotFoundException) {
        println("LOG_CSV_ERROR: ARQUIVO NÃO ENCONTRADO: '$fileName'. Verifique 'app/src/main/assets/' e o nome.")
        fnf.printStackTrace()
    } catch (e: Exception) {
        println("LOG_CSV_ERROR: Erro crítico ao carregar arquivo CSV '$fileName': ${e.message}")
        e.printStackTrace()
    }

    if (products.isEmpty()) {
        println("LOG_CSV: Nenhum produto foi carregado de '$fileName'. Verifique o arquivo e os logs.")
    } else {
        println("LOG_CSV: ${products.size} produtos carregados de '$fileName'.")
    }
    return products
}