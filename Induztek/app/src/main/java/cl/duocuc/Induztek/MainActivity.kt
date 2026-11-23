package cl.duocuc.Induztek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.duocuc.Induztek.navigation.AppNavegacion
import cl.duocuc.Induztek.ui.theme.InduztekTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InduztekTheme {
                AppNavegacion()
            }
        }
    }
}