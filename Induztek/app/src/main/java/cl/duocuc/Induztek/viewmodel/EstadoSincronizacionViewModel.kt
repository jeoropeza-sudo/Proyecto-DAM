package cl.duocuc.Induztek.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cl.duocuc.Induztek.data.remote.InduztekRepository
import cl.duocuc.Induztek.data.remote.SyncQueueEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class EstadoSincronizacionViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = InduztekRepository(application)
    private val _tareas = MutableStateFlow<List<SyncQueueEntry>>(emptyList())
    val tareas = _tareas.asStateFlow()

    fun actualizar() {
        _tareas.value = repositorio.tareasDeSincronizacion()
    }

    fun reintentar(id: String) {
        repositorio.resolverConflicto(id, true)
        actualizar()
    }

    fun descartar(id: String) {
        repositorio.resolverConflicto(id, false)
        actualizar()
    }
}
