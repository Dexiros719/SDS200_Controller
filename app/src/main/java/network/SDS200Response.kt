package network

sealed interface SDS200Response

data class StatusResponse(val status: ScannerStatus) : SDS200Response
data class ButtonResponse(val success: Boolean) : SDS200Response
data class GltResponse(val favoriteLists: List<FavoriteList>) : SDS200Response
data class ErrorResponse(val message: String) : SDS200Response

// Data class for GLT (Global List)
data class FavoriteList(
    val name: String,
    val index: Long,
    val monitor: Boolean = false,
    val avoid: Boolean = false
)
