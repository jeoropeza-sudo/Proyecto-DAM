package cl.duocuc.Induztek.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.duocuc.Induztek.R

@Composable
fun MarcaCompacta(
    subtitulo: String,
    sobreFondoOscuro: Boolean,
    modifier: Modifier = Modifier,
    colorTexto: Color = if (sobreFondoOscuro) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    },
    tamanoIcono: Dp = 34.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(
                if (sobreFondoOscuro) R.drawable.logo_sin_texto_blanco else R.drawable.logo_sin_texto_negro
            ),
            contentDescription = null,
            modifier = Modifier.size(tamanoIcono)
        )
        Column {
            Text(
                text = "Induztek",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorTexto
            )
            if (subtitulo.isNotBlank()) {
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorTexto.copy(alpha = 0.82f)
                )
            }
        }
    }
}
