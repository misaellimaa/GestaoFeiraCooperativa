package com.example.gestaofeiracooperativa // Seu package

import java.util.Locale

/**
 * Formata um valor Double para exibição.
 * Retorna "-" para 0.0.
 * Retorna um número inteiro (ex: "5") se não houver casas decimais.
 * Retorna um número com duas casas decimais e vírgula (ex: "5,50") caso contrário.
 */
fun formatQuantity(value: Double): String {
    return if (value == 0.0) "-" else if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale("pt", "BR"), "%.2f", value)
}

/**
 * Formata um valor Double como moeda (R$).
 */
fun formatCurrency(value: Double, incluirSimbolo: Boolean = true): String {
    val prefix = if (incluirSimbolo) "R$ " else ""
    return prefix + String.format(Locale("pt", "BR"), "%.2f", value)
}