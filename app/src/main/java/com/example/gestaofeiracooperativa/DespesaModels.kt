package com.example.gestaofeiracooperativa

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.File // Necessário para UiState.Success

// Entidade para os TIPOS de despesa (ex: Gasolina, Alimentação)
@Entity(
    tableName = "itens_despesa",
    indices = [Index(value = ["nome"], unique = true)] // Garante que o nome do item de despesa seja único
)
data class ItemDespesaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "nome")
    val nome: String = "",

    @ColumnInfo(name = "descricao")
    val descricao: String? = null
)

// Entidade para os LANÇAMENTOS de despesa, agora vinculados a uma feira específica
@Entity(
    tableName = "despesas_feira",
    primaryKeys = ["feiraId", "itemDespesaId"], // Chave primária composta
    foreignKeys = [
        ForeignKey(
            entity = FeiraEntity::class,
            parentColumns = ["feiraId"],
            childColumns = ["feiraId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ItemDespesaEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemDespesaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["feiraId"]), Index(value = ["itemDespesaId"])]
)
data class DespesaFeiraEntity(
    @ColumnInfo(name = "feiraId")
    val feiraId: String = "", // <<< CORREÇÃO: Adicionado valor padrão

    @ColumnInfo(name = "itemDespesaId")
    val itemDespesaId: Long = 0, // <<< CORREÇÃO: Adicionado valor padrão

    @ColumnInfo(name = "valores_por_dia_json")
    val valoresPorDiaJson: String = "{}",

    @ColumnInfo(name = "observacao")
    val observacao: String? = null
)

// Classe de dados para ajudar a gerenciar o estado na tela de lançamento (UI)
data class DespesaFeiraUiItem(
    val itemDespesa: ItemDespesaEntity = ItemDespesaEntity(), // <<< CORREÇÃO: Adicionado valor padrão
    val valoresPorDiaInput: MutableMap<String, String> = mutableMapOf(), // <<< CORREÇÃO: Adicionado valor padrão
    var observacaoInput: String = "",
    val isExistingEntry: Boolean = false
)

// Classe de dados para agrupar as informações para gerar o PDF mensal consolidado
data class DadosPdfDespesasMensais(
    val ano: Int = 0,
    val mes: Int = 0,
    val feirasDasSemanasDoMes: List<FairDetails> = emptyList(),
    val despesasDeCadaFeiraDoMes: Map<String, List<DespesaFeiraEntity>> = emptyMap(),
    val todosOsItensDeDespesa: List<ItemDespesaEntity> = emptyList()
)

// Classe de estado para controlar a UI durante processos assíncronos (como gerar PDF)
sealed class UiState<out T> {
    object Idle : UiState<Nothing>() // Estado inicial ou ocioso
    object Loading : UiState<Nothing>() // Estado de carregamento
    data class Success<T>(val data: T) : UiState<T>() // Sucesso, contém os dados (ex: o arquivo PDF)
    data class Error(val message: String) : UiState<Nothing>() // Falha, contém uma mensagem de erro
}