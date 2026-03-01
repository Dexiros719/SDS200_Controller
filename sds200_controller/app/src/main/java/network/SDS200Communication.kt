package network

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

class SDS200Communication(
    private var ip: String = "192.168.0.91",
    private var port: Int = 50536,
    private var isEnabled: Boolean = true
) : MessageListener {

    private val TAG = "SDS200Communication"

    // UDP communication initialized with correct port
    private val udp: UdpCommunication = UdpCommunication(port, listener = this)

    // Latest response from UDP
    private var pendingResponse: CompletableDeferred<String>? = null
    
    // Track connection state
    var isLastStatusSuccess: Boolean = false
        private set

    init {
        if (isEnabled) {
            Log.d(TAG, "Initializing communication: IP=$ip, Port=$port")
            udp.start()
        }
    }

    fun isCommunicationPossible(): Boolean {
        return isEnabled && isLastStatusSuccess
    }

    fun updateSettings(newIp: String, newPort: Int, enabled: Boolean) {
        if (this.ip == newIp && this.port == newPort && this.isEnabled == enabled) return
        
        Log.d(TAG, "Updating settings: IP=$newIp, Port=$newPort, Enabled=$enabled")
        this.ip = newIp
        this.port = newPort
        this.isEnabled = enabled

        udp.updatePort(newPort)

        if (enabled) {
            udp.start()
        } else {
            udp.stop()
            isLastStatusSuccess = false
        }
    }

    // Implementation for callback of MessageListener interface
    override fun onMessage(data: String) {
        pendingResponse?.complete(data)
        pendingResponse = null
    }

    // Status getter which suspends until response is received
    suspend fun getStatus(): String? {
        if (!isEnabled) return null
        
        return try {
            val result = withTimeout( 2000) {
                val deferred = CompletableDeferred<String>()
                pendingResponse = deferred
                udp.sendTo("GSI\r", ip)
                deferred.await()
            }
            isLastStatusSuccess = result != null
            result
        } catch (e: Exception) {
            isLastStatusSuccess = false
            null
        }
    }

    // Button push which suspends until response is received
    suspend fun sendButton(keyCode: String, keyMode: Int = 0): String? {
        if (!isEnabled) return null

        return try {
            // Reduced timeout to 500ms to match status responsiveness
            withTimeout(500) {
                val deferred = CompletableDeferred<String>()
                pendingResponse = deferred
                udp.sendTo("KEY,$keyCode,$keyMode\r", ip)
                deferred.await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Button press timeout or error: ${e.message}")
            null
        }
    }

    // Stops the UDP communication
    fun stopCommunication() {
        udp.stop()
    }
}
