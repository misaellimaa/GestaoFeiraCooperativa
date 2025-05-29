package com.example.gestaofeiracooperativa

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString // Importe decodeFromString diretamente

class MapStringDoubleConverter {
    private val json = Json {
        ignoreUnknownKeys = true // Útil se o JSON tiver campos extras no futuro
        prettyPrint = false      // Não precisa de pretty print para armazenamento interno
        isLenient = true
        encodeDefaults = true    // Garante que mesmo valores padrão sejam serializados
    }

    @TypeConverter
    fun fromMapStringDouble(map: Map<String, Double>?): String? {
        return map?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toMapStringDouble(jsonString: String?): Map<String, Double>? {
        return jsonString?.let { json.decodeFromString<Map<String, Double>>(it) }
    }
}

// Se você decidir armazenar ResultadoGeralFeira como JSON na FeiraEntity,
// você precisaria de um conversor similar para ele:
// class ResultadoGeralFeiraConverter {
//     private val json = Json { /* ... sua configuração json ... */ }
//
//     @TypeConverter
//     fun fromResultadoGeralFeira(resultado: ResultadoGeralFeira?): String? {
//         return resultado?.let { json.encodeToString(it) }
//     }
//
//     @TypeConverter
//     fun toResultadoGeralFeira(jsonString: String?): ResultadoGeralFeira? {
//         return jsonString?.let { json.decodeFromString<ResultadoGeralFeira>(it) }
//     }
// }