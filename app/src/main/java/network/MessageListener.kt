package network
/*
Interface for message receiver
 */
interface MessageListener {
    fun onMessage(data: String)
}