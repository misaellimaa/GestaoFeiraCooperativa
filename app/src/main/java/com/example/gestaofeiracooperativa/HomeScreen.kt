package com.example.gestaofeiracooperativa // <<--- ATENÇÃO: MUDE PARA O SEU PACKAGE REAL

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote // Para Iniciar/Continuar Feira
import androidx.compose.material.icons.filled.ListAlt // Para Ver Feiras Salvas
import androidx.compose.material.icons.filled.Inventory2 // Para Gerenciar Produtos
import androidx.compose.material.icons.filled.People // Para Gerenciar Agricultores
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector // Necessário para o parâmetro do ícone
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToNovaFeira: () -> Unit,
    onNavigateToFeirasSalvas: () -> Unit,
    onNavigateToCadastroProdutos: () -> Unit,
    onNavigateToCadastroAgricultores: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestão de Feiras da Cooperativa") },
                colors = TopAppBarDefaults.topAppBarColors( // Opcional: cores customizadas para a TopAppBar
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp), // Padding geral da tela
            horizontalAlignment = Alignment.CenterHorizontally,
            // <<< ALTERAÇÃO: Arrangement.Top para começar do topo, com espaçamento >>>
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top)
        ) {
            // Chamada a um Composable auxiliar para criar os cards
            NavigationCard(
                text = "Iniciar Feira",
                icon = Icons.Filled.EditNote,
                contentDescription = "Iniciar ou continuar o gerenciamento de uma feira",
                onClick = onNavigateToNovaFeira
            )

            NavigationCard(
                text = "Ver Feiras Salvas",
                icon = Icons.Filled.ListAlt,
                contentDescription = "Visualizar feiras que foram salvas anteriormente",
                onClick = onNavigateToFeirasSalvas
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp)) // Divisor opcional

            NavigationCard(
                text = "Gerenciar Produtos",
                icon = Icons.Filled.Inventory2,
                contentDescription = "Cadastrar e gerenciar produtos (CAD PROD)",
                onClick = onNavigateToCadastroProdutos
            )

            NavigationCard(
                text = "Gerenciar Agricultores",
                icon = Icons.Filled.People,
                contentDescription = "Cadastrar e gerenciar agricultores",
                onClick = onNavigateToCadastroAgricultores
            )
        }
    }
}

// <<< NOVO: Composable auxiliar para criar os Cards de Navegação >>>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationCard(
    text: String,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp), // Altura mínima para o card
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium // Bordas arredondadas
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp) // Padding interno do Card
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp) // Espaço entre o ícone e o texto
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(36.dp), // Tamanho do ícone
                tint = MaterialTheme.colorScheme.primary // Cor do ícone
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium, // Estilo do texto
                fontWeight = FontWeight.Medium // Peso da fonte
            )
        }
    }
}