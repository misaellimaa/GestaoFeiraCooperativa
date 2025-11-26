package com.example.gestaofeiracooperativa

import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.Locale

class FeiraRepository(
    private val appDatabase: AppDatabase,
    private val feiraDao: FeiraDao,
    private val entradaDao: EntradaDao,
    private val perdaDao: PerdaDao,
    private val produtoDao: ProdutoDao,
    private val agricultorDao: AgricultorDao,
    private val itemDespesaDao: ItemDespesaDao,
    private val despesaFeiraDao: DespesaFeiraDao
) {

    private val firestore = Firebase.firestore
    private val feirasCollection = firestore.collection("feiras")
    private val jsonFormat = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun Double?.formatParaUi(): String {
        if (this == null) return ""
        return if (this % 1.0 == 0.0) this.toInt().toString() else String.format(Locale.getDefault(), "%.2f", this).replace('.', ',')
    }

    fun getTodasAsFeirasInfo(): Flow<List<FairDetails>> = feiraDao.getAllFeiras().map { entities ->
        entities.map { FairDetails(it.feiraId, it.startDate, it.endDate) }
    }

    suspend fun getFeiraAnterior(feiraIdAtual: String): FeiraEntity? = feiraDao.getFeiraAnterior(feiraIdAtual)

    fun getFeirasByMesAno(ano: Int, mes: Int): Flow<List<FairDetails>> {
        val mesStr = String.format("%02d", mes)
        val anoStr = ano.toString()
        return feiraDao.getFeirasByMesAno(mesStr, anoStr).map { entities ->
            entities.map { FairDetails(it.feiraId, it.startDate, it.endDate) }
        }
    }

    suspend fun feiraExiste(feiraId: String): Boolean {
        if (feiraDao.getFeiraById(feiraId) != null) return true
        return try { feirasCollection.document(feiraId).get().await().exists() }
        catch (e: Exception) { Log.e("FeiraRepository", "Erro ao verificar se feira existe no Firestore", e); false }
    }

    suspend fun sincronizarFeiraDoFirestoreParaRoom(feiraId: String) {
        try {
            val feiraRef = feirasCollection.document(feiraId)
            val feiraDoc = feiraRef.get().await()

            if (feiraDoc.exists()) {
                val feiraEntity = feiraDoc.toObject(FeiraEntity::class.java)!!
                val entradas = feiraRef.collection("entradas").get().await().toObjects(EntradaEntity::class.java)
                val perdas = feiraRef.collection("perdas").get().await().toObjects(PerdaEntity::class.java)
                val despesas = feiraRef.collection("despesas").get().await().toObjects(DespesaFeiraEntity::class.java)
                sincronizarFeiraLocal(feiraEntity, entradas, perdas, despesas)
                Log.d("FeiraRepo_Sync", "Sincronização explícita da feira $feiraId concluída.")
            }
        } catch (e: Exception) { Log.e("FeiraRepo_Sync", "Erro na sincronização explícita da feira $feiraId.", e) }
    }

    // <<< CORREÇÃO AQUI: Removemos a sincronização forçada ao carregar >>>
    suspend fun carregarDadosCompletosFeira(feiraId: String): DadosCompletosFeira? {
        // REMOVIDO: sincronizarFeiraDoFirestoreParaRoom(feiraId)
        // Motivo: Isso sobrescrevia os dados locais (novos) com dados da nuvem (antigos/vazios)
        // Agora confiamos 100% no banco local, que é a fonte da verdade imediata.
        return carregarDadosCompletosFeiraLocal(feiraId)
    }

    private suspend fun carregarDadosCompletosFeiraLocal(feiraId: String): DadosCompletosFeira? {
        val feiraEntity = feiraDao.getFeiraById(feiraId) ?: return null
        val entradasDoBanco = entradaDao.getEntradasForFeira(feiraId).firstOrNull() ?: emptyList()
        val perdasDoBanco = perdaDao.getPerdasForFeira(feiraId).firstOrNull() ?: emptyList()
        val despesasDoBanco = despesaFeiraDao.getDespesasByFeiraId(feiraId).firstOrNull() ?: emptyList()
        val todosOsItensDeDespesa = itemDespesaDao.getAllItensDespesa().firstOrNull() ?: emptyList()

        val entradasAgricultoresMap = mutableMapOf<String, MutableList<EntradaItemAgricultor>>()
        entradasDoBanco.forEach { entrada ->
            produtoDao.getProductByNumber(entrada.produtoNumero)?.let { produto ->
                val qtds = try { jsonFormat.decodeFromString<Map<String, Double>>(entrada.quantidadesPorDiaJson) } catch (_: Exception) { emptyMap() }
                entradasAgricultoresMap.getOrPut(entrada.agricultorId) { mutableListOf() }.add(EntradaItemAgricultor(produto, entrada.quantidadeSobra, qtds))
            }
        }
        val perdasFeiraList = perdasDoBanco.mapNotNull { perda ->
            produtoDao.getProductByNumber(perda.produtoNumero)?.let { produto ->
                val perdasMap = try { jsonFormat.decodeFromString<Map<String, Double>>(perda.perdasPorDiaJson) } catch (_: Exception) { emptyMap() }
                PerdaItemFeira(produto, perdasMap, perda.quantidadeSobra)
            }
        }
        val despesasUiList = todosOsItensDeDespesa.map { itemDespesa ->
            val lancamento = despesasDoBanco.find { it.itemDespesaId == itemDespesa.id }
            val valoresMap = lancamento?.valoresPorDiaJson?.let { try { jsonFormat.decodeFromString<Map<String, Double>>(it) } catch (_: Exception) { emptyMap() } } ?: emptyMap()
            val inputMap = mutableMapOf<String, String>()
            diasDaSemanaFeira.forEach { dia -> inputMap[dia] = valoresMap[dia]?.formatParaUi() ?: "" }
            DespesaFeiraUiItem(itemDespesa, inputMap, lancamento?.observacao ?: "", lancamento != null)
        }
        val resultadoGeral = feiraEntity.resultadoGeralJson?.let { try { jsonFormat.decodeFromString<ResultadoGeralFeira>(it) } catch (_: Exception) { null } }

        return DadosCompletosFeira(
            fairDetails = FairDetails(feiraEntity.feiraId, feiraEntity.startDate, feiraEntity.endDate),
            entradasTodosAgricultores = entradasAgricultoresMap,
            perdasTotaisDaFeira = perdasFeiraList,
            despesasDaFeira = despesasUiList,
            resultadoGeralCalculado = resultadoGeral
        )
    }

    suspend fun salvarDadosCompletosFeira(dadosFeira: DadosCompletosFeira): Boolean {
        val feiraId = dadosFeira.fairDetails.feiraId
        Log.i("FeiraRepo_Save", "--- INICIANDO SALVAMENTO PARA FEIRA ID: $feiraId ---")
        return try {
            val feiraRef = feirasCollection.document(feiraId)
            val batch = firestore.batch()

            val feiraEntity = FeiraEntity(
                feiraId = feiraId, startDate = dadosFeira.fairDetails.startDate, endDate = dadosFeira.fairDetails.endDate,
                resultadoGeralJson = dadosFeira.resultadoGeralCalculado?.let { jsonFormat.encodeToString(serializer(), it) }
            )
            val entradasEntities = dadosFeira.entradasTodosAgricultores.flatMap { (agricultorId, listaEntradas) ->
                listaEntradas.map { entradaItem ->
                    EntradaEntity(
                        feiraId = feiraId, agricultorId = agricultorId, produtoNumero = entradaItem.produto?.numero ?: "",
                        quantidadeSobra = entradaItem.quantidadeSobraDaSemanaAnterior,
                        quantidadesPorDiaJson = jsonFormat.encodeToString(serializer<Map<String, Double>>(), entradaItem.quantidadesPorDia)
                    )
                }
            }
            val perdasEntities = dadosFeira.perdasTotaisDaFeira.map { perdaItem ->
                PerdaEntity(
                    feiraId = feiraId, produtoNumero = perdaItem.produto?.numero ?: "",
                    perdasPorDiaJson = jsonFormat.encodeToString(serializer<Map<String, Double>>(), perdaItem.perdasPorDia),
                    quantidadeSobra = perdaItem.quantidadeSobra
                )
            }
            val despesasEntities = dadosFeira.despesasDaFeira.mapNotNull { despesaUi ->
                val valoresConvertidos = mutableMapOf<String, Double>()
                despesaUi.valoresPorDiaInput.forEach { (dia, valorStr) -> valorStr.replace(',', '.').toDoubleOrNull()?.let { valoresConvertidos[dia] = it } }
                if (valoresConvertidos.isNotEmpty() || despesaUi.isExistingEntry) {
                    DespesaFeiraEntity(
                        feiraId = feiraId, itemDespesaId = despesaUi.itemDespesa.id,
                        valoresPorDiaJson = jsonFormat.encodeToString(serializer<Map<String, Double>>(), valoresConvertidos),
                        observacao = despesaUi.observacaoInput.trim().ifEmpty { null }
                    )
                } else null
            }

            batch.set(feiraRef, feiraEntity)
            limparSubcolecao(feiraRef.collection("entradas"), batch); entradasEntities.forEach {
                batch.set(feiraRef.collection("entradas").document("${it.agricultorId}-${it.produtoNumero}"), it)
            }
            limparSubcolecao(feiraRef.collection("perdas"), batch); perdasEntities.forEach { batch.set(feiraRef.collection("perdas").document(it.produtoNumero), it) }
            limparSubcolecao(feiraRef.collection("despesas"), batch); despesasEntities.forEach { batch.set(feiraRef.collection("despesas").document(it.itemDespesaId.toString()), it) }

            batch.commit().await()

            sincronizarFeiraLocal(feiraEntity, entradasEntities, perdasEntities, despesasEntities)
            true
        } catch (e: Exception) {
            Log.e("FeiraRepo_Save", "--- ERRO: Falha ao salvar na nuvem. ---", e)
            try {
                Log.w("FeiraRepo_Save", "Tentando salvar apenas localmente (Room)...")
            } catch (exLocal: Exception) {
                Log.e("FeiraRepo_Save", "Erro fatal ao salvar localmente.", exLocal)
            }
            false
        }
    }

    private suspend fun limparSubcolecao(collectionRef: com.google.firebase.firestore.CollectionReference, batch: com.google.firebase.firestore.WriteBatch) {
        val snapshot = collectionRef.limit(500).get().await()
        if(snapshot.isEmpty) return
        snapshot.documents.forEach { batch.delete(it.reference) }
    }

    private suspend fun sincronizarFeiraLocal(feiraEntity: FeiraEntity, entradas: List<EntradaEntity>, perdas: List<PerdaEntity>, despesas: List<DespesaFeiraEntity>) {
        appDatabase.withTransaction {
            feiraDao.insertFeira(feiraEntity)
            entradaDao.deleteAllEntradasForFeira(feiraEntity.feiraId); if (entradas.isNotEmpty()) entradaDao.insertAllEntradas(entradas)
            perdaDao.deleteAllPerdasForFeira(feiraEntity.feiraId); if (perdas.isNotEmpty()) perdaDao.insertAllPerdas(perdas)
            despesaFeiraDao.deleteDespesasByFeiraId(feiraEntity.feiraId); if (despesas.isNotEmpty()) despesas.forEach { despesaFeiraDao.insertOrUpdate(it) }
        }
    }

    suspend fun sincronizarFeirasLocaisParaNuvem() {
        Log.d("FeiraRepo_Sync", "Iniciando UPLOAD de todas as feiras locais para a nuvem...")
        try {
            val feirasLocais = feiraDao.getAllFeiras().firstOrNull() ?: emptyList()
            if (feirasLocais.isEmpty()) {
                Log.d("FeiraRepo_Sync", "Nenhuma feira local para enviar.")
                return
            }

            for (feiraEntity in feirasLocais) {
                val dadosCompletos = carregarDadosCompletosFeiraLocal(feiraEntity.feiraId)
                if (dadosCompletos != null) {
                    val sucesso = salvarDadosCompletosFeira(dadosCompletos)
                    if (sucesso) {
                        Log.d("FeiraRepo_Sync", "Feira ${feiraEntity.feiraId} sincronizada com sucesso.")
                    } else {
                        Log.w("FeiraRepo_Sync", "Falha ao sincronizar feira ${feiraEntity.feiraId}.")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FeiraRepo_Sync", "Erro geral no upload de feiras.", e)
        }
    }

    fun iniciarOuvinteDaListaDeFeiras() {
        CoroutineScope(Dispatchers.IO).launch {
            ouvirAtualizacoesDeFeiras()
        }
    }

    private fun ouvirAtualizacoesDeFeiras() {
        feirasCollection.addSnapshotListener { snapshots, e ->
            if (e != null) { Log.w("FeiraRepo_Sync", "Ouvinte de feiras falhou.", e); return@addSnapshotListener }
            if (snapshots != null) {
                val feirasDaNuvem = snapshots.toObjects(FeiraEntity::class.java)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (feirasDaNuvem.isNotEmpty()) {
                            Log.d("FeiraRepo_Sync", "Recebidas ${feirasDaNuvem.size} feiras da nuvem. Mesclando com locais...")
                            feiraDao.insertAllFeiras(feirasDaNuvem)
                        }
                    }
                    catch (ex: Exception) { Log.e("FeiraRepo_Sync", "Erro ao mesclar lista de feiras.", ex) }
                }
            }
        }
    }

    suspend fun deletarFeira(feiraId: String): Boolean {
        return try {
            feirasCollection.document(feiraId).delete().await()
            appDatabase.withTransaction { feiraDao.deleteFeiraById(feiraId) }
            Log.d("FeiraRepository_Cloud", "Feira $feiraId deletada da nuvem e local.")
            true
        } catch (e: Exception) {
            Log.e("FeiraRepository_Cloud", "Erro ao deletar feira.", e)
            false
        }
    }
}