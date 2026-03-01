package com.example.sds200_communicator

import androidx.lifecycle.ViewModel
import network.RepositoryWriter
import network.SDS200Communication
import network.SDS200Controller
import network.SDS200Translator
import network.ScannerRepository

class Sds200ViewModel : ViewModel() {
    val repository = ScannerRepository()
    private val writer = RepositoryWriter(repository)
    
    lateinit var communicator: SDS200Communication
    lateinit var controller: SDS200Controller
    
    var isLastStatusSuccess: Boolean = false
    private var isInitialized = false

    fun init(ip: String, port: Int, enabled: Boolean) {
        if (isInitialized) return
        
        communicator = SDS200Communication(ip, port, enabled)
        controller = SDS200Controller(
            communicator = communicator,
            translator = SDS200Translator(),
            writer = writer
        )
        
        controller.onStatusCompleted = { success ->
            isLastStatusSuccess = success
            onStatusUpdated?.invoke(success)
        }
        
        // Use 500ms for a more responsive "live" feel
        controller.startAutoStatus(500)
        isInitialized = true
    }

    var onStatusUpdated: ((Boolean) -> Unit)? = null

    override fun onCleared() {
        super.onCleared()
        if (::controller.isInitialized) {
            controller.stop()
        }
    }
}
