package cl.duocuc.Induztek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.duocuc.Induztek.navigation.AppNavegacion
import cl.duocuc.Induztek.ui.theme.InduztekTheme
import cl.duocuc.Induztek.data.remote.SyncScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SyncScheduler.programarPeriodico(this)
        enableEdgeToEdge()
        setContent {
            InduztekTheme {
                AppNavegacion()
            }
        }
    }
}