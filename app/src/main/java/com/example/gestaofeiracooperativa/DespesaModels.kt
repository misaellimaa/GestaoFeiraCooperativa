package com.example.gestaofeiracooperativa

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "itens_despesa",
    indices = [Index(value = ["nome"], unique = true)] // Garante que o nome do item de despesa seja único
)
data class ItemDespesaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Chave primária autogerada

    @ColumnInfo(name = "nome")
    val nome: String, // Ex: "Gasolina", "Refeição", "Salários", "Sacolas"

    @ColumnInfo(name = "descricao")
    val descricao: String? = null // Opcional: uma breve descrição do item
)
@Entity(
    tableName = "lancamentos_despesas_mensais",
    // Garante que só haja um registro de um item de despesa para um determinado mês/ano
    indices = [Index(value = ["itemDespesaId", "ano", "mes"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = ItemDespesaEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemDespesaId"],
            onDelete = ForeignKey.CASCADE // Se o item de despesa for excluído, seus lançamentos também são
        )
    ]
)
data class LancamentoMensalDespesaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "itemDespesaId", index = true) // Chave estrangeira para ItemDespesaEntity
    val itemDespesaId: Long,

    @ColumnInfo(name = "ano")
    val ano: Int, // Ex: 2024

    @ColumnInfo(name = "mes")
    val mes: Int, // Ex: 4 (para Abril)

    @ColumnInfo(name = "valor_semana1")
    val valorSemana1: Double? = null, // Valores podem ser nulos se não houver despesa

    @ColumnInfo(name = "valor_semana2")
    val valorSemana2: Double? = null,

    @ColumnInfo(name = "valor_semana3")
    val valorSemana3: Double? = null,

    @ColumnInfo(name = "valor_semana4")
    val valorSemana4: Double? = null,

    @ColumnInfo(name = "valor_semana5")
    val valorSemana5: Double? = null, // Para meses com uma 5ª semana parcial

    @ColumnInfo(name = "observacoes")
    val observacoes: String? = null
) {
    // Função para calcular o total mensal (pode ser útil)
    fun getTotalMensal(): Double {
        return (valorSemana1 ?: 0.0) +
                (valorSemana2 ?: 0.0) +
                (valorSemana3 ?: 0.0) +
                (valorSemana4 ?: 0.0) +
                (valorSemana5 ?: 0.0)
    }
}