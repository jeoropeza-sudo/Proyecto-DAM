package cl.duocuc.Induztek.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import cl.duocuc.Induztek.ui.theme.InduztekTheme
import cl.duocuc.Induztek.R
import cl.duocuc.Induztek.ui.theme.Dimens

@Composable
fun ImagenEncabezado(
    @DrawableRes imagen: Int,
    descripcion: String,
    titulo: String,
    subtitulo: String,
    modifier: Modifier = Modifier
) {
    // Sombra oscura progresiva (Slate 900 con alpha) para garantizar contraste del texto sobre el banner
    val colorSombra = Color(0xCC0F172A)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.altoBanner)
    ) {
        Image(
            painter = painterResource(id = imagen),
            contentDescription = descripcion,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Degradado vertical transparente a oscuro para lectura clara en Terreno
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, colorSombra)))
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Dimens.espacioPantalla)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ImagenEncabezadoPreview() {
    InduztekTheme {
        ImagenEncabezado(
            imagen = R.drawable.banner_registro,
            descripcion = "Banner Induztek",
            titulo = "Protocolos de Terreno",
            subtitulo = "Planta Lampa - Envasadora del Pacífico"
        )
    }
}