package network

class XmlAssembler {

    private val parts = mutableMapOf<Int, String>()
    private var lastPartNumber: Int? = null

    /*
    Adds an XML segment and checks for completion.
    Handles single-packet XMLs and multi-part sequences.
    */
    fun addSegment(segment: String): String? {
        val footerRegex = Regex("<Footer\\s+No=\"(\\d+)\"\\s+EOT=\"(\\d+)\"\\s*/>")
        val match = footerRegex.find(segment)

        if (match == null) {
            // No footer found. 
            // Check if this is a complete single-packet XML by looking for its closing root tag.
            val rootTagName = extractRootTagName(segment)
            if (parts.isEmpty() && rootTagName != null && segment.contains("</$rootTagName>")) {
                parts[1] = segment.trim()
                val fullXml = buildFullXml()
                clear()
                return fullXml
            }

            // Otherwise, it's a fragment. 
            // If the first packet of a multi-part message has no footer, we assume it's part 1.
            val partNumber = if (parts.isEmpty()) 1 else (parts.keys.maxOrNull() ?: 0) + 1

            parts[partNumber] = segment.trim()
            return null
        }

        // Standard multi-part footer handling
        val partNumber = match.groupValues[1].toInt()
        val isLastPart = match.groupValues[2] == "1"
        if (isLastPart) lastPartNumber = partNumber

        // Strip the footer tag from the content
        val cleanSegment = segment.substring(0, match.range.first) + segment.substring(match.range.last + 1)
        parts[partNumber] = cleanSegment.trim()

        // Return the full message if all parts are present and we've seen the end (EOT=1)
        if (lastPartNumber != null && parts.size == lastPartNumber) {
            val fullXml = buildFullXml()
            clear() 
            return fullXml
        }

        return null
    }

    /*
    Extracts the root tag name from an XML segment.
    */
    private fun extractRootTagName(segment: String): String? {
        val xmlHeaderRegex = Regex("<\\?xml.*?\\?>")
        val content = segment.replace(xmlHeaderRegex, "").trim()
        val match = Regex("<([a-zA-Z0-9]+)").find(content)
        return match?.groupValues?.get(1)
    }

    /*
    Merges fragments into a single valid XML document.
    Preserves the full opening root tag from the first part.
    */
    private fun buildFullXml(): String {
        val sortedKeys = parts.keys.sorted()
        val sb = StringBuilder()
        
        val firstPart = parts[sortedKeys.first()] ?: ""
        val xmlHeaderRegex = Regex("<\\?xml.*?\\?>")
        val headerMatch = xmlHeaderRegex.find(firstPart)
        val header = headerMatch?.value ?: "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
        
        // Find the full opening root tag
        val contentWithoutHeader = firstPart.replace(xmlHeaderRegex, "").trim()
        val rootTagMatch = Regex("<([a-zA-Z0-9]+)(\\s[^>]*)?>").find(contentWithoutHeader)
        val rootTagName = rootTagMatch?.groupValues?.get(1) ?: "XML"
        val fullOpeningTag = rootTagMatch?.value ?: "<$rootTagName>"
        
        sb.append(header).append("\n")
        sb.append(fullOpeningTag).append("\n")

        for (key in sortedKeys) {
            var partContent = parts[key] ?: ""
            partContent = partContent.replace(xmlHeaderRegex, "")
            
            // Strip the opening and closing root tags from every part to merge inner content
            partContent = partContent.replace(Regex("<$rootTagName(\\s[^>]*)?>"), "")
            partContent = partContent.replace("</$rootTagName>", "")
            
            val trimmed = partContent.trim()
            if (trimmed.isNotEmpty()) {
                sb.append(trimmed).append("\n")
            }
        }

        sb.append("</").append(rootTagName).append(">")
        return sb.toString()
    }

    private fun clear() {
        parts.clear()
        lastPartNumber = null
    }
}
