package cl.duocuc.Induztek.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.duocuc.Induztek.ui.styles.EstilosTarjeta
import cl.duocuc.Induztek.ui.theme.Dimens
import cl.duocuc.Induztek.ui.theme.InduztekTheme

@Composable
fun TarjetaSeccion(
    titulo: String,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = EstilosTarjeta.forma,
        colors = EstilosTarjeta.colores(),
        elevation = EstilosTarjeta.elevacion()
    ) {
        Column(
            modifier = Modifier.padding(Dimens.espacioMedio),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacioChico)
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.secondary)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            contenido()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TarjetaSeccionPreview() {
    InduztekTheme {
        TarjetaSeccion(titulo = "Sobre mí", modifier = Modifier.padding(16.dp)) {
            Text("Cualquier contenido puede ir aquí dentro.")
        }
    }
}
