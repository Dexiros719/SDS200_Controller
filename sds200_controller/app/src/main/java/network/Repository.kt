package network

// Repository for data
class ScannerRepository {

    // Current status
    private var currentStatus: ScannerStatus? = null

    // Update status
    fun updateStatus(newStatus: ScannerStatus) {
        currentStatus = newStatus
    }

    // Get status
    fun getStatus(): ScannerStatus? {
        return currentStatus
    }
}

// Status data class
data class ScannerStatus(
    val mode: String = "",
    val vScreen: String = "",
    val systems: List<SystemInfo> = emptyList(),
    val departments: List<DepartmentInfo> = emptyList(),
    val convFrequencies: List<ConvFrequencyInfo> = emptyList(),
    val properties: PropertyInfo = PropertyInfo(),
    val viewTexts: ViewDescription = ViewDescription(),
    val sLevel: String = "",
    val vScreenDisplay: String = ""
)

data class SystemInfo(val name: String = "", val index: Int = 0, val hold: Boolean = false)
data class DepartmentInfo(val name: String = "", val index: Int = 0, val hold: Boolean = false)
data class ConvFrequencyInfo(val name: String = "", val freq: String = "", val mod: String = "", val hold: Boolean = false, val svcType: String = "")
data class PropertyInfo(val vol: Int = 0, val backlight: Int = 0, val mute: String = "")
data class ViewDescription(val info1: String = "", val info2: String = "", val popup: String = "")
