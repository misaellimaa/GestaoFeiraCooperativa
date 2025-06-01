package com.example.gestaofeiracooperativa

import android.widget.Toast // Não esqueça de importar Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext

// Importe Agricultor e FairDetails
import com.example.gestaofeiracooperativa.Agricultor
import com.example.gestaofeiracooperativa.FairDetails
// Importe StandardTopAppBar e NavigationCard
// import com.example.gestaofeiracooperativa.StandardTopAppBar
// import com.example.gestaofeiracooperativa.NavigationCard


// Se o NavigationCard não estiver em um arquivo comum, mantenha-o aqui ou mova-o.
// Vou assumir que ele pode ser movido para AppUiComponents.kt e importado.
// Se não, descomente esta seção:

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationCard(
    text: String,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        onClick = { if (enabled) onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 4.dp else 1.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
            contentColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(36.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenciarFeiraScreen(
    navController: NavHostController,
    feiraDetails: FairDetails,
    listaDeAgricultores: List<Agricultor>,
    onNavigateToLancamentos: (agricultorId: String) -> Unit,
    onNavigateToPerdasTotais: () -> Unit,
    onNavigateToResultados: () -> Unit,
    onSalvarFeira: () -> Unit
) {
    var agricultorIdSelecionado by remember { mutableStateOf("") }
    var expandedAgricultorDropdown by remember { mutableStateOf(false) }

    val nomeDisplayAgricultorSelecionado = remember(agricultorIdSelecionado, listaDeAgricultores) {
        if (agricultorIdSelecionado.isBlank()) {
            "Selecione o Agricultor"
        } else {
            val agricultorEncontrado = listaDeAgricultores.find { it.id == agricultorIdSelecionado }
            if (agricultorEncontrado != null) {
                "ID: ${agricultorEncontrado.id} - ${agricultorEncontrado.nome}"
            } else {
                "Agricultor ID: $agricultorIdSelecionado (Nome não encontrado)"
            }
        }
    }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = "Gerenciar Feira", // Título simplificado
                canNavigateBack = true,
                onNavigateBack = { navController.navigateUp() },
                actions = {
                    IconButton(onClick = onSalvarFeira) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Salvar Feira")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // Espaçamento entre os cards/seções principais
        ) {
            // Título da tela e detalhes da feira no topo
            Text(
                "Feira Nº ${feiraDetails.feiraId}",
                style = MaterialTheme.typography.headlineSmall, // Destaque maior
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Período: ${feiraDetails.startDate} a ${feiraDetails.endDate}",
                style = MaterialTheme.typography.titleSmall, // Um pouco menor que o ID
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(bottom = 16.dp) // Espaço antes do primeiro card
            )

            // <<< INÍCIO DA ALTERAÇÃO: Card Agrupado para Lançamento de Entradas >>>
            Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp) // Espaçamento interno do card
                ) {
                    Text(
                        "Entradas de Produtos por Agricultor", // Título da Seção dentro do Card
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Dropdown para selecionar agricultor
                    ExposedDropdownMenuBox(
                        expanded = expandedAgricultorDropdown,
                        onExpandedChange = { expandedAgricultorDropdown = !expandedAgricultorDropdown },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            readOnly = true,
                            value = nomeDisplayAgricultorSelecionado,
                            onValueChange = {},
                            label = { Text("Selecionar Agricultor") }, // Label simplificado
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAgricultorDropdown) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedAgricultorDropdown,
                            onDismissRequest = { expandedAgricultorDropdown = false }
                        ) {
                            if (listaDeAgricultores.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Nenhum agricultor cadastrado") },
                                    onClick = { expandedAgricultorDropdown = false },
                                    enabled = false
                                )
                            } else {
                                listaDeAgricultores.forEach { agricultor ->
                                    DropdownMenuItem(
                                        text = { Text("ID: ${agricultor.id} - ${agricultor.nome}") },
                                        onClick = {
                                            agricultorIdSelecionado = agricultor.id
                                            expandedAgricultorDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Botão para Lançar/Editar Entradas (agora um Button normal dentro do Card)
                    Button(
                        onClick = {
                            if (agricultorIdSelecionado.isNotBlank()) {
                                onNavigateToLancamentos(agricultorIdSelecionado)
                            } else {
                                Toast.makeText(context, "Por favor, selecione um agricultor.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = agricultorIdSelecionado.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.PostAdd, contentDescription = null) // Ícone no botão
                            Text("Lançar/Editar Entradas")
                        }
                    }
                }
            }
            // <<< FIM DA ALTERAÇÃO: Card Agrupado para Lançamento de Entradas >>>


            // Os outros NavigationCards para Perdas e Resultados permanecem como estavam,
            // pois são ações gerais da feira e não dependem da seleção de agricultor nesta tela.
            // Podemos adicionar um título de seção para eles também, se desejar.

            Text(
                "Outras Ações da Feira:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start).padding(top = 8.dp) // Espaço acima do título da seção
            )

            NavigationCard( // Certifique-se que NavigationCard está acessível (ex: importado de AppUiComponents.kt)
                text = "Lançar/Editar Perdas Totais",
                icon = Icons.Filled.Inventory,
                contentDescription = "Lançar ou editar as perdas totais de produtos da feira",
                onClick = onNavigateToPerdasTotais
            )

            NavigationCard( // Certifique-se que NavigationCard está acessível
                text = "Processar e Ver Resultados",
                icon = Icons.Filled.Assessment,
                contentDescription = "Processar os dados e visualizar os resultados da feira",
                onClick = onNavigateToResultados
            )
        }
    }
}