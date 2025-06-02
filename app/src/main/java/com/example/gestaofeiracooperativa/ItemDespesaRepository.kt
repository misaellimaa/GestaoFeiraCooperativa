package com.example.gestaofeiracooperativa

import kotlinx.coroutines.flow.Flow

// Importe ItemDespesaEntity e ItemDespesaDao
// (Certifique-se que os paths estão corretos)
import com.example.gestaofeiracooperativa.ItemDespesaEntity
import com.example.gestaofeiracooperativa.ItemDespesaDao

class ItemDespesaRepository(private val itemDespesaDao: ItemDespesaDao) {

    /**
     * Retorna um Flow com a lista de todos os itens de despesa, ordenados por nome.
     * Este Flow será atualizado automaticamente quando houver mudanças na tabela.
     */
    fun getAllItensDespesa(): Flow<List<ItemDespesaEntity>> = itemDespesaDao.getAllItensDespesa()

    /**
     * Busca itens de despesa pelo nome.
     */
    fun searchItensDespesa(query: String): Flow<List<ItemDespesaEntity>> = itemDespesaDao.searchItensDespesa(query)

    /**
     * Busca um item de despesa específico pelo seu ID.
     * Função suspend para ser chamada de uma coroutine.
     */
    suspend fun getItemDespesaById(id: Long): ItemDespesaEntity? {
        return itemDespesaDao.getItemDespesaById(id)
    }

    /**
     * Busca um item de despesa específico pelo seu nome.
     * Útil para verificar se um nome de item já existe antes de tentar inserir um novo.
     * Função suspend para ser chamada de uma coroutine.
     */
    suspend fun getItemDespesaByNome(nome: String): ItemDespesaEntity? {
        return itemDespesaDao.getItemDespesaByNome(nome)
    }

    /**
     * Insere um novo item de despesa no banco de dados.
     * O DAO está configurado com OnConflictStrategy.ABORT, então uma exceção será lançada
     * se o nome do item já existir (devido ao índice único no nome na ItemDespesaEntity).
     * Função suspend para ser chamada de uma coroutine.
     */
    suspend fun insertItemDespesa(itemDespesa: ItemDespesaEntity) {
        itemDespesaDao.insertItemDespesa(itemDespesa)
    }

    /**
     * Atualiza um item de despesa existente no banco de dados.
     * Função suspend para ser chamada de uma coroutine.
     */
    suspend fun updateItemDespesa(itemDespesa: ItemDespesaEntity) {
        itemDespesaDao.updateItemDespesa(itemDespesa)
    }

    /**
     * Deleta um item de despesa do banco de dados.
     * Função suspend para ser chamada de uma coroutine.
     */
    suspend fun deleteItemDespesa(itemDespesa: ItemDespesaEntity) {
        itemDespesaDao.deleteItemDespesa(itemDespesa)
    }
}