package cl.duocuc.Induztek.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import cl.duocuc.Induztek.R

@Composable
fun LogoInduztek(
    sobreFondoOscuro: Boolean,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(
            if (sobreFondoOscuro) R.drawable.logo_fondo_negro else R.drawable.logo_fondo_blanco
        ),
        contentDescription = "Induztek Ingeniería SpA",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}
