package com.shapeshed.aerial.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.data.StationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class StationEditViewModel(
    private val repository: StationRepository,
    private val stationId: Long?,
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _streamUrl = MutableStateFlow("")
    val streamUrl: StateFlow<String> = _streamUrl.asStateFlow()

    private val _logoPath = MutableStateFlow("")
    val logoPath: StateFlow<String> = _logoPath.asStateFlow()

    val isEditing: Boolean = stationId != null

    private var logoCopyJob: Job? = null
    private var existingStation: Station? = null

    init {
        if (stationId != null) {
            viewModelScope.launch {
                repository.getById(stationId)?.let { station ->
                    existingStation = station
                    _name.value = station.name
                    _streamUrl.value = station.streamUrl
                    val path = station.logoPath
                    _logoPath.value = when {
                        path.startsWith("http") -> path
                        path.isNotEmpty() && File(path).exists() -> path
                        else -> ""
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) { _name.value = value }
    fun onStreamUrlChange(value: String) { _streamUrl.value = value }

    fun onLogoPicked(context: Context, uri: Uri) {
        logoCopyJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = File(context.filesDir, "logos").also { it.mkdirs() }
                val dest = copyLogoFromUri(context, uri, dir)
                if (dest != null) withContext(Dispatchers.Main) { _logoPath.value = dest.absolutePath }
            } catch (_: Exception) {}
        }
    }

    fun removeLogo() {
        logoCopyJob?.cancel()
        _logoPath.value = ""
    }

    fun save(onDone: () -> Unit) {
        if (_name.value.isBlank() || _streamUrl.value.isBlank()) return
        viewModelScope.launch {
            logoCopyJob?.join()  // wait for any in-progress copy before reading the path
            val station = (existingStation ?: Station(
                name = "",
                streamUrl = "",
                isFavorite = true,
            )).copy(
                id = stationId ?: 0,
                name = _name.value.trim(),
                streamUrl = _streamUrl.value.trim(),
                logoPath = _logoPath.value,
            )
            if (stationId == null) repository.insert(station) else repository.update(station)
            withContext(Dispatchers.Main) { onDone() }
        }
    }
}

class StationEditViewModelFactory(
    private val repository: StationRepository,
    private val stationId: Long?,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return StationEditViewModel(repository, stationId) as T
    }
}
