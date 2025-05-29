package com.example.gestaofeiracooperativa // <<--- MUDE PARA O SEU PACKAGE REAL

import kotlinx.coroutines.flow.Flow // Import Flow

class ProdutoRepository(private val produtoDao: ProdutoDao) {

    fun getAllProducts(): Flow<List<Produto>> = produtoDao.getAllProducts()

    fun searchProducts(query: String): Flow<List<Produto>> = produtoDao.searchProducts(query)

    suspend fun getProductByNumber(numero: String): Produto? = produtoDao.getProductByNumber(numero)

    suspend fun insert(produto: Produto) {
        produtoDao.insert(produto)
    }

    suspend fun update(produto: Produto) {
        produtoDao.update(produto)
    }

    suspend fun delete(produto: Produto) {
        produtoDao.delete(produto)
    }

    suspend fun deleteAllProducts() { // Adicionado para limpeza, se necessário
        produtoDao.deleteAllProducts()
    }
}
