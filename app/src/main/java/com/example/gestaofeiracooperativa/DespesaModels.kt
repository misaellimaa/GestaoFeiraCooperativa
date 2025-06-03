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
data class DespesaMensalParaUi(
    val itemDespesa: ItemDespesaEntity, // O tipo de despesa (ex: Gasolina)
    var valorSemana1: String = "", // Valores como String para os TextFields
    var valorSemana2: String = "",
    var valorSemana3: String = "",
    var valorSemana4: String = "",
    var valorSemana5: String = "",
    val idLancamentoExistente: Long? = null // ID do LancamentoMensalDespesaEntity se já existir
)
@Entity(
    tableName = "despesas_feira",
    // Chave primária composta para garantir que um item de despesa só seja lançado uma vez por feira
    // (se você permitir múltiplos lançamentos do mesmo item na mesma feira, use um id autogerado simples)
    primaryKeys = ["feiraId", "itemDespesaId"],
    foreignKeys = [
        ForeignKey(
            entity = FeiraEntity::class,
            parentColumns = ["feiraId"],
            childColumns = ["feiraId"],
            onDelete = ForeignKey.CASCADE // Se a feira for deletada, suas despesas associadas também são
        ),
        ForeignKey(
            entity = ItemDespesaEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemDespesaId"],
            onDelete = ForeignKey.CASCADE // Se o tipo de despesa for deletado, seus lançamentos também são
        )
    ],
    indices = [Index(value = ["feiraId"]), Index(value = ["itemDespesaId"])]
)
data class DespesaFeiraEntity(
    @ColumnInfo(name = "feiraId")
    val feiraId: String,

    @ColumnInfo(name = "itemDespesaId")
    val itemDespesaId: Long,

    // Armazena um Map<String, Double> ("TER" to valor, "QUA" to valor, etc.)
    // para os dias da semana da feira. Usará o mesmo MapStringDoubleConverter.
    @ColumnInfo(name = "valores_por_dia_json")
    val valoresPorDiaJson: String,

    @ColumnInfo(name = "observacao")
    val observacao: String? = null
) {
    // Função auxiliar para obter o total da despesa para esta feira/item
    // (precisaria desserializar o JSON aqui, ou fazer isso no Repository/ViewModel)
}
data class DespesaFeiraUiItem(
    val itemDespesa: ItemDespesaEntity,
    // Usaremos um MutableMap para que a UI possa alterar os valores diretamente
    // As chaves serão os dias da semana (ex: "TER", "QUA")
    val valoresPorDiaInput: MutableMap<String, String>,
    var observacaoInput: String,
    val isExistingEntry: Boolean
)