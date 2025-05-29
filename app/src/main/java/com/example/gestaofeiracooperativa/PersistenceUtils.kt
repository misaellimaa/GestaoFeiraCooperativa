package com.example.gestaofeiracooperativa

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private val jsonFormat = Json {
    prettyPrint = true
    isLenient = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private const val DIRETOREO_FEIRAS = "feiras_salvas"
private const val EXTENSAO_ARQUIVO = ".json"

fun getNomeArquivoFeira(feiraId: String): String {
    return "feira_${feiraId.replace(Regex("[^A-Za-z0-9_]"), "_")}$EXTENSAO_ARQUIVO"
}

fun salvarDadosFeira(context: Context, dadosFeira: DadosCompletosFeira): Boolean {
    return try {
        val jsonString = jsonFormat.encodeToString(dadosFeira)
        val outputDir = File(context.filesDir, DIRETOREO_FEIRAS)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val file = File(outputDir, getNomeArquivoFeira(dadosFeira.fairDetails.feiraId))
        file.writeText(jsonString, Charsets.UTF_8)
        println("LOG_PERSISTENCE: Feira ${dadosFeira.fairDetails.feiraId} salva em ${file.absolutePath}")
        true
    } catch (e: IOException) {
        println("LOG_PERSISTENCE_ERROR: Erro de I/O ao salvar feira ${dadosFeira.fairDetails.feiraId}: ${e.message}")
        e.printStackTrace()
        false
    } catch (e: kotlinx.serialization.SerializationException) {
        println("LOG_PERSISTENCE_ERROR: Erro de serialização ao salvar feira ${dadosFeira.fairDetails.feiraId}: ${e.message}")
        e.printStackTrace()
        false
    } catch (e: Exception) {
        println("LOG_PERSISTENCE_ERROR: Erro genérico ao salvar feira ${dadosFeira.fairDetails.feiraId}: ${e.message}")
        e.printStackTrace()
        false
    }
}

fun carregarDadosFeira(context: Context, feiraId: String): DadosCompletosFeira? {
    return try {
        val inputDir = File(context.filesDir, DIRETOREO_FEIRAS)
        val file = File(inputDir, getNomeArquivoFeira(feiraId))
        if (!file.exists() || !file.canRead()) {
            println("LOG_PERSISTENCE: Arquivo da feira $feiraId não encontrado ou não pode ser lido.")
            return null
        }
        val jsonString = file.readText(Charsets.UTF_8)
        val dadosFeira = jsonFormat.decodeFromString<DadosCompletosFeira>(jsonString)
        println("LOG_PERSISTENCE: Feira $feiraId carregada de ${file.absolutePath}")
        dadosFeira
    } catch (e: IOException) {
        println("LOG_PERSISTENCE_ERROR: Erro de I/O ao carregar feira $feiraId: ${e.message}")
        e.printStackTrace()
        null
    } catch (e: kotlinx.serialization.SerializationException) {
        println("LOG_PERSISTENCE_ERROR: Erro de desserialização ao carregar feira $feiraId: ${e.message}")
        e.printStackTrace()
        null
    } catch (e: IllegalArgumentException) {
        println("LOG_PERSISTENCE_ERROR: Erro de argumento (JSON corrompido?) ao carregar feira $feiraId: ${e.message}")
        e.printStackTrace()
        null
    } catch (e: Exception) {
        println("LOG_PERSISTENCE_ERROR: Erro genérico ao carregar feira $feiraId: ${e.message}")
        e.printStackTrace()
        null
    }
}

fun listarFeirasSalvas(context: Context): List<String> {
    val feirasDir = File(context.filesDir, DIRETOREO_FEIRAS)
    if (!feirasDir.exists() || !feirasDir.isDirectory) {
        return emptyList()
    }
    return feirasDir.listFiles { _, name -> name.startsWith("feira_") && name.endsWith(EXTENSAO_ARQUIVO) }
        ?.mapNotNull { file ->
            file.nameWithoutExtension.removePrefix("feira_").takeIf { it.isNotBlank() }
        }?.sorted() ?: emptyList()
}

fun deletarDadosFeira(context: Context, feiraId: String): Boolean {
    return try {
        val inputDir = File(context.filesDir, DIRETOREO_FEIRAS)
        val file = File(inputDir, getNomeArquivoFeira(feiraId))
        if (file.exists()) {
            val deleted = file.delete()
            if (deleted) {
                println("LOG_PERSISTENCE: Feira $feiraId deletada.")
            } else {
                println("LOG_PERSISTENCE_ERROR: Falha ao deletar feira $feiraId.")
            }
            return deleted
        } else {
            println("LOG_PERSISTENCE_WARN: Tentativa de deletar feira $feiraId, mas o arquivo não existe.")
            return false
        }
    } catch (e: Exception) {
        println("LOG_PERSISTENCE_ERROR: Erro ao tentar deletar feira $feiraId: ${e.message}")
        e.printStackTrace()
        false
    }
}
