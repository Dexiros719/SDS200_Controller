package network

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select

class SDS200Controller(
    // Objects for communication
    private val communicator: SDS200Communication,
    private val translator: SDS200Translator,
    private val writer: RepositoryWriter
) {
    // Callbacks for higher classes, for UI mainly
    var onStatusCompleted: ((Boolean) -> Unit)? = null
    var onButtonCompleted: ((Boolean) -> Unit)? = null

    // Coroutine scope for communication
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Priority channel for status
    private val statusChannel = Channel<suspend () -> Unit>(Channel.CONFLATED)

    // Standard channel for commands
    private val commandChannel = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    // Initializes the controller and starts the communication loop
    init {
        scope.launch {
            while (isActive) {
                try {
                    select<Unit> {
                        // Priority for status updates
                        statusChannel.onReceive { cmd ->
                            cmd()
                        }

                        commandChannel.onReceive { cmd ->
                            cmd()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Enqueues a command for execution
    fun enqueueCommand(command: suspend () -> Unit) {
        // Only enqueue if communication is likely to succeed
        if (communicator.isCommunicationPossible()) {
            commandChannel.trySend(command)
        }
    }

    // Enqueues a status update
    fun enqueueStatus(command: suspend () -> Unit) {
        statusChannel.trySend(command)
    }


    // Stops the controller and all coroutines
    fun stop() {
        scope.cancel()
        communicator.stopCommunication()
    }

    // Sends a button command
    fun sendButton(keyCode: String, keyMode: Int = 1) {
        enqueueCommand {
            val rawResponse = communicator.sendButton(keyCode, keyMode)
            if (rawResponse != null) {
                val response = translator.parse(rawResponse)
                val success = (response as? ButtonResponse)?.success ?: false
                onButtonCompleted?.invoke(success)
            } else {
                onButtonCompleted?.invoke(false)
            }
        }
    }

    // Enqueues a status update with a specified interval
    fun startAutoStatus(intervalMs: Long = 1000) {
        scope.launch {
            while (isActive) {
                enqueueStatus {
                    val rawStatus = communicator.getStatus()
                    if (rawStatus != null) {
                        val response = translator.parse(rawStatus)

                        when (response) {
                            is StatusResponse -> {
                                writer.writeStatus(response.status)
                                onStatusCompleted?.invoke(true)
                            }
                            is ErrorResponse -> {
                                onStatusCompleted?.invoke(false)
                            }
                            else -> {
                                onStatusCompleted?.invoke(false)
                            }
                        }
                    } else {
                        onStatusCompleted?.invoke(false)
                    }
                }

                delay(intervalMs)
            }
        }
    }
}
