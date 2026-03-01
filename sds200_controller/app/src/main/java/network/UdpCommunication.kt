package network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException

/*
Class for UDP communication and setting up XMLs using XmlAssembler class
*/
class UdpCommunication(
    private var port: Int,                //Port for communication
    private val listener: MessageListener //Listener to send the callback message to
) {
    companion object {
        const val BUFF_SIZE = 65535
        const val TAG = "UdpCommunication"
    }
    private val assembler = XmlAssembler()  //Object of XML assembler
    private var socket: DatagramSocket? = null  //Socket for communication
    private var job: Job? = null    //Coroutine for getting data
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isRunning = false
    
    // Track the type of the XML being currently assembled (e.g., GSI, GLT)
    private var currentXmlType: String? = null

    /*
    Starts communication
    */
    fun start() {
        if (isRunning) return
        isRunning = true
        
        // Coroutine for listening to UDP
        job = scope.launch {
            while (isActive && isRunning) {
                try {
                    Log.d(TAG, "Attempting to open socket on port $port")
                    socket = DatagramSocket(port)
                    socket?.soTimeout = 5000 // 5 seconds timeout for receive

                    val buffer = ByteArray(BUFF_SIZE)
                    while (isActive && isRunning) {
                        try {
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket?.receive(packet)
                            val msg = String(packet.data, 0, packet.length)
                            processIncomingMessage(msg)
                        } catch (e: Exception) {
                            // Timeout or other receive error - just continue the loop
                            if (isRunning) delay(100)
                        }
                    }
                } catch (e: SocketException) {
                    Log.e(TAG, "SocketException: ${e.message}. Retrying in 2s...")
                    delay(2000)
                } catch (e: Exception) {
                    Log.e(TAG, "General Exception in UDP: ${e.message}")
                    delay(2000)
                } finally {
                    socket?.close()
                    socket = null
                }
            }
        }
    }

    /*
    Stops UDP listening
    */
    fun stop() {
        isRunning = false
        job?.cancel()
        socket?.close()
        socket = null
    }

    /*
    Updates the listening port and restarts if needed
    */
    fun updatePort(newPort: Int) {
        if (this.port != newPort) {
            this.port = newPort
            if (isRunning) {
                stop()
                start()
            }
        }
    }

    /*
    Processes one received segment
    If format is XML, sends to XmlAssembler
    */
    private fun processIncomingMessage(msg: String) {
        val xmlIndex = msg.indexOf(",<XML>,")

        if (xmlIndex != -1) {
            val type = msg.substring(0, xmlIndex)
            val xmlPart = msg.substring(xmlIndex + ",<XML>,".length)
            
            // Remember the type for multi-part messages
            currentXmlType = type
            
            val fullXml = assembler.addSegment(xmlPart)

            if (fullXml != null) {
                // Once assembled, prepend the type back so the Translator knows what it is
                listener.onMessage("${currentXmlType},$fullXml")
                currentXmlType = null
            }
        } else {
            listener.onMessage(msg)
        }
    }

    /*
    Sends data to desired endpoint using the established socket
    */
    fun sendTo(message: String, ip: String) {
        val currentSocket = socket
        if (currentSocket == null) {
            Log.w(TAG, "Send aborted: Socket not initialized.")
            return
        }

        scope.launch {
            try {
                val data = message.toByteArray()
                val address = InetAddress.getByName(ip)
                val packet = DatagramPacket(data, data.size, address, port)
                
                // Use the main socket so the remote end knows which port to reply to
                currentSocket.send(packet)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending UDP: ${e.message}")
            }
        }
    }
}
