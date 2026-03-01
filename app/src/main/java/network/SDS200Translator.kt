package network

import java.util.regex.Pattern

class SDS200Translator {

    /*
    Parse the raw response string into a typed SDS200Response.
    */
    fun parse(response: String): SDS200Response? {
        val type = response.substringBefore(",")
        val content = response.substringAfter(",")

        return when (type) {
            "GSI" -> StatusResponse(parseGsiXml(content))
            "KEY" -> ButtonResponse(content.startsWith("OK"))
            else -> {
                ErrorResponse("Unknown response type: $type")
            }
        }
    }

    private fun parseGsiXml(xml: String): ScannerStatus {
        val scannerInfoTag = Regex("<ScannerInfo\\s+([^>]*)>").find(xml)?.groupValues?.get(1) ?: ""
        
        val systems = Regex("<System\\s+([^>]*)/>").findAll(xml).map { match ->
            val attrs = match.groupValues[1]
            SystemInfo(
                name = extractAttribute(attrs, "Name"),
                index = extractAttribute(attrs, "Index").toIntOrNull() ?: 0,
                hold = extractAttribute(attrs, "Hold") == "On"
            )
        }.toList()

        val departments = Regex("<Department\\s+([^>]*)/>").findAll(xml).map { match ->
            val attrs = match.groupValues[1]
            DepartmentInfo(
                name = extractAttribute(attrs, "Name"),
                index = extractAttribute(attrs, "Index").toIntOrNull() ?: 0,
                hold = extractAttribute(attrs, "Hold") == "On"
            )
        }.toList()

        val convFrequencies = Regex("<ConvFrequency\\s+([^>]*)/>").findAll(xml).map { match ->
            val attrs = match.groupValues[1]
            ConvFrequencyInfo(
                name = extractAttribute(attrs, "Name"),
                freq = extractAttribute(attrs, "Freq").replace("MHz", "").trim(),
                mod = extractAttribute(attrs, "Mod"),
                hold = extractAttribute(attrs, "Hold") == "On",
                svcType = extractAttribute(attrs, "SvcType")
            )
        }.toList()

        val property = Regex("<Property\\s+([^>]*)/>").find(xml)?.let { match ->
            val attrs = match.groupValues[1]
            PropertyInfo(
                vol = extractAttribute(attrs, "VOL").toIntOrNull() ?: 0,
                backlight = extractAttribute(attrs, "Backlight").toIntOrNull() ?: 0,
                mute = extractAttribute(attrs, "Mute")
            )
        } ?: PropertyInfo()

        val info1 = extractTagAttribute(xml, "InfoArea1", "Text")
        val info2 = extractTagAttribute(xml, "InfoArea2", "Text")
        val popup = extractTagAttribute(xml, "PopupScreen", "Text")

        val sLevelMatch = Pattern.compile("S(\\d+):").matcher(info2)
        val sLevel = if (sLevelMatch.find()) sLevelMatch.group(1) else ""

        val vScreen = extractAttribute(scannerInfoTag, "V_Screen")

        return ScannerStatus(
            mode = extractAttribute(scannerInfoTag, "Mode"),
            vScreen = vScreen,
            systems = systems,
            departments = departments,
            convFrequencies = convFrequencies,
            properties = property,
            viewTexts = ViewDescription(info1, info2, popup),
            sLevel = sLevel,
            vScreenDisplay = vScreen.substringAfterLast('_')
        )
    }

    private fun extractAttribute(tagContent: String, attribute: String): String {
        return Regex("$attribute=\"([^\"]*)\"").find(tagContent)?.groupValues?.get(1) ?: ""
    }

    private fun extractTagAttribute(xml: String, tagName: String, attribute: String): String {
        val tagRegex = Regex("<$tagName\\s+([^>]*)/>|<$tagName\\s+([^>]*)>.*?</$tagName>")
        val match = tagRegex.find(xml) ?: return ""
        val attrs = match.groupValues[1].ifEmpty { match.groupValues[2] }
        return extractAttribute(attrs, attribute)
    }
}
