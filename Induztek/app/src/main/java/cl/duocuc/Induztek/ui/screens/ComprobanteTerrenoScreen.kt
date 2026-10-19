package cl.duocuc.Induztek.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duocuc.Induztek.R
import cl.duocuc.Induztek.ui.components.BotonPrincipal
import cl.duocuc.Induztek.ui.components.TarjetaSeccion
import cl.duocuc.Induztek.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprobanteTerrenoScreen(
    idProtocolo: Long,
    onVolverInicio: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comprobante de Visita") },
                navigationIcon = {
                    IconButton(onClick = onVolverInicio) {
                        Icon(
                            painter = painterResource(R.drawable.ic_volver),
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Dimens.espacioPantalla)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
        ) {
            TarjetaSeccion(titulo = "ACTA DE VISITA EN TERRENO") {
                Text(text = "Induztek Ingeniería SpA", fontWeight = FontWeight.Bold)
                Text(text = "Cliente: Envasadora del Pacífico S.A.")
                Text(text = "Planta: Lampa")
                Text(text = "Equipo: Transformador (TAG: TR-01)")
                Text(text = "Técnico a Cargo: Matías Godoy Herrera")
                Text(text = "Fecha: 18/10/2026 - 10:00 hrs")
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Dimens.espacioMedio)) {
                    Text(
                        text = "ESTADO DEL DOCUMENTO",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "REGISTRADO EN TERRENO (PENDIENTE DE FORMALIZACIÓN)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.espacioGrande))

            BotonPrincipal(
                texto = "Volver al Inicio",
                onClick = onVolverInicio,
                icono = R.drawable.ic_check,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}