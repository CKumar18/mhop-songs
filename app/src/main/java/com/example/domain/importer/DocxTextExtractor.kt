package com.example.domain.importer

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

data class DocxRawExtraction(
    val text: String,
    val links: List<String>,
    val warnings: List<String>,
)

object DocxTextExtractor {

    fun extract(inputStream: InputStream): DocxRawExtraction {
        var documentXmlBytes: ByteArray? = null
        var relsXmlBytes: ByteArray? = null

        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when (entry.name) {
                    "word/document.xml" -> documentXmlBytes = zip.readBytes()
                    "word/_rels/document.xml.rels" -> relsXmlBytes = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        if (documentXmlBytes == null) {
            throw IllegalArgumentException("The selected file is not a valid DOCX document (missing word/document.xml).")
        }

        val relMap = relsXmlBytes?.let { parseRelationships(it) } ?: emptyMap()
        val parsed = parseDocumentXml(documentXmlBytes!!, relMap)
        return parsed
    }

    private fun parseRelationships(bytes: ByteArray): Map<String, String> {
        val links = mutableMapOf<String, String>()
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "Relationship") {
                val id = parser.getAttributeValue(null, "Id")
                val target = parser.getAttributeValue(null, "Target")
                val targetMode = parser.getAttributeValue(null, "TargetMode")
                if (!id.isNullOrBlank() && !target.isNullOrBlank() &&
                    (targetMode.equals("External", ignoreCase = true) || target.startsWith("http"))
                ) {
                    links[id] = target
                }
            }
            eventType = parser.next()
        }
        return links
    }

    private fun parseDocumentXml(bytes: ByteArray, relMap: Map<String, String>): DocxRawExtraction {
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        val outputBuilder = StringBuilder()
        val collectedLinks = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        var inParagraph = false
        var isHeadingParagraph = false
        val paragraphBuilder = StringBuilder()
        var hasTable = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "w:tbl" -> hasTable = true
                        "w:p" -> {
                            inParagraph = true
                            isHeadingParagraph = false
                            paragraphBuilder.setLength(0)
                        }
                        "w:pStyle" -> {
                            val styleVal = parser.getAttributeValue(null, "w:val") ?: ""
                            if (styleVal.startsWith("Heading", ignoreCase = true) ||
                                styleVal.equals("Title", ignoreCase = true)
                            ) {
                                isHeadingParagraph = true
                            }
                        }
                        "w:hyperlink" -> {
                            val rId = parser.getAttributeValue(null, "r:id")
                            val url = rId?.let { relMap[it] }
                            if (!url.isNullOrBlank()) {
                                collectedLinks.add(url)
                            }
                        }
                        "w:br" -> {
                            val brType = parser.getAttributeValue(null, "w:type")
                            if (brType.equals("page", ignoreCase = true)) {
                                paragraphBuilder.append("\u000C")
                            } else {
                                paragraphBuilder.append("\n")
                            }
                        }
                        "w:tab" -> {
                            paragraphBuilder.append("    ")
                        }
                        "w:t" -> {
                            if (inParagraph) {
                                paragraphBuilder.append(parser.nextText())
                            }
                        }
                        "w:tc" -> {
                            // Table cell boundary handled by paragraphs inside cell
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "w:p" -> {
                            inParagraph = false
                            val lineText = paragraphBuilder.toString()
                            if (isHeadingParagraph && lineText.isNotBlank()) {
                                outputBuilder.append("[[HEADING]]").append(lineText.trim()).append("\n")
                            } else {
                                outputBuilder.append(lineText).append("\n")
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        if (hasTable) {
            warnings.add("Document contained tables; verify line order and section alignment.")
        }

        return DocxRawExtraction(
            text = outputBuilder.toString().trim(),
            links = collectedLinks.distinct(),
            warnings = warnings,
        )
    }
}
