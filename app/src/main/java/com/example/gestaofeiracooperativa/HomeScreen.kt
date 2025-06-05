package com.example.gestaofeiracooperativa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Importa todos os ícones 'filled'
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToNovaFeira: () -> Unit,
    onNavigateToFeirasSalvas: () -> Unit,
    onNavigateToCadastroProdutos: () -> Unit,
    onNavigateToCadastroAgricultores: () -> Unit,
    onNavigateToCadastroItensDespesa: () -> Unit,
    onNavigateToRelatorioDespesas: () -> Unit // <<< NOVO CALLBACK
) {
    Scaffold(
        topBar = {
            StandardTopAppBar( // Assumindo que você já aplicou a TopAppBar Padrão aqui
                title = "Gestão de Feiras da Cooperativa",
                canNavigateBack = false // A tela inicial não tem botão de voltar
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top)
        ) {
            // Seção de Feiras
            NavigationCard(
                text = "Iniciar / Gerenciar Feira",
                icon = Icons.Filled.EditNote,
                contentDescription = "Iniciar ou gerenciar uma feira",
                onClick = onNavigateToNovaFeira
            )
            NavigationCard(
                text = "Ver Feiras Salvas",
                icon = Icons.Filled.ListAlt,
                contentDescription = "Visualizar feiras salvas",
                onClick = onNavigateToFeirasSalvas
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Seção de Cadastros
            NavigationCard(
                text = "Gerenciar Produtos",
                icon = Icons.Filled.Inventory2,
                contentDescription = "Cadastrar e gerenciar produtos",
                onClick = onNavigateToCadastroProdutos
            )
            NavigationCard(
                text = "Gerenciar Agricultores",
                icon = Icons.Filled.People,
                contentDescription = "Cadastrar e gerenciar agricultores",
                onClick = onNavigateToCadastroAgricultores
            )
            NavigationCard(
                text = "Gerenciar Itens de Despesa",
                icon = Icons.Filled.Summarize, // Ícone diferente para despesas
                contentDescription = "Cadastrar e gerenciar itens de despesa",
                onClick = onNavigateToCadastroItensDespesa
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // <<< NOVO: Card para Relatório Mensal de Despesas >>>
            NavigationCard(
                text = "Relatório Mensal de Despesas",
                icon = Icons.Filled.RequestQuote, // Ícone para relatórios/resumos
                contentDescription = "Gerar relatório mensal consolidado de despesas",
                onClick = onNavigateToRelatorioDespesas
            )
        }
    }
}

// <<< ATENÇÃO: NavigationCard Atualizado >>>
// Este é o NavigationCard que inclui o parâmetro 'enabled'.
// É uma ótima ideia mover este Composable para um arquivo comum (ex: AppUiComponents.kt),
// remover o 'private' e importá-lo tanto aqui quanto na GerenciarFeiraScreen para evitar código duplicado.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationCard(
    text: String,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true // Parâmetro que adicionamos para GerenciarFeiraScreen
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