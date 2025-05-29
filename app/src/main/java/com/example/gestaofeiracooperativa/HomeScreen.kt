package com.example.gestaofeiracooperativa // <<--- ATENÇÃO: MUDE PARA O SEU PACKAGE REAL

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToNovaFeira: () -> Unit,
    onNavigateToFeirasSalvas: () -> Unit,
    onNavigateToCadastroProdutos: () -> Unit, // <<--- NOVO CALLBACK
    onNavigateToCadastroAgricultores: () -> Unit // <<--- NOVO CALLBACK
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Gestão de Feiras da Cooperativa") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onNavigateToNovaFeira,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(50.dp)
            ) {
                Text("Iniciar/Continuar Feira", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNavigateToFeirasSalvas,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(50.dp)
            ) {
                Text("Ver Feiras Salvas", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // NOVOS BOTÕES DE CADASTRO
            Button(
                onClick = onNavigateToCadastroProdutos,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(50.dp)
            ) {
                Text("Gerenciar Produtos (CAD PROD)", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNavigateToCadastroAgricultores,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(50.dp)
            ) {
                Text("Gerenciar Agricultores", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
