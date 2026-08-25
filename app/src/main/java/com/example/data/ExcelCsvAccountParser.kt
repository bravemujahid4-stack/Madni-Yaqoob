package com.example.data

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

object ExcelCsvAccountParser {

    /**
     * Reads a file from a Uri (XLSX or CSV) and parses it into an ImportPreviewResult.
     */
    fun parseFile(
        context: Context,
        uri: Uri,
        existingAccounts: List<PartyAccount>
    ): ImportPreviewResult {
        val fileName = getFileName(context, uri)
        val isXlsx = fileName.endsWith(".xlsx", ignoreCase = true) || isZipFormat(context, uri)

        val rawRows: List<List<String>> = try {
            if (isXlsx) {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    parseXlsxStream(inputStream)
                } ?: emptyList()
            } else {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    parseCsvStream(inputStream)
                } ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }

        return processRawRows(fileName, rawRows, existingAccounts)
    }

    /**
     * Parses raw table matrix (header + data) into validated preview rows.
     */
    fun processRawRows(
        fileName: String,
        rawRows: List<List<String>>,
        existingAccounts: List<PartyAccount>
    ): ImportPreviewResult {
        if (rawRows.isEmpty()) {
            return ImportPreviewResult(
                fileName = fileName,
                totalRows = 0,
                readyCount = 0,
                duplicateCount = 0,
                invalidCount = 0,
                rows = emptyList()
            )
        }

        // Find header row or default mapping
        val (headerIndex, colMap) = detectHeaders(rawRows)
        val dataRows = if (headerIndex >= 0 && headerIndex < rawRows.size) {
            rawRows.subList(headerIndex + 1, rawRows.size)
        } else {
            rawRows
        }

        val parsedRows = mutableListOf<ImportedAccountRow>()
        val existingAndBatchCodes = existingAccounts.map { it.code.trim().uppercase() }.toMutableSet()
        val generatedCounters = mutableMapOf<PartyAccountType, Int>()

        // Pre-compute existing max counters for each type
        PartyAccountType.values().forEach { type ->
            var maxNum = 0
            existingAccounts.filter { it.accountType == type }.forEach { acc ->
                val code = acc.code.trim()
                if (code.startsWith(type.codePrefix, ignoreCase = true)) {
                    val numPart = code.removePrefix(type.codePrefix).trim('-', '_', ' ')
                    numPart.toIntOrNull()?.let { num ->
                        if (num > maxNum) maxNum = num
                    }
                }
            }
            generatedCounters[type] = maxNum
        }

        fun nextCodeFor(type: PartyAccountType): String {
            var curr = (generatedCounters[type] ?: 0) + 1
            var candidate = String.format("%s-%03d", type.codePrefix, curr)
            while (existingAndBatchCodes.contains(candidate.uppercase())) {
                curr++
                candidate = String.format("%s-%03d", type.codePrefix, curr)
            }
            generatedCounters[type] = curr
            existingAndBatchCodes.add(candidate.uppercase())
            return candidate
        }

        dataRows.forEachIndexed { index, row ->
            // Skip purely empty lines
            if (row.all { it.isBlank() }) return@forEachIndexed

            val rawCode = getCell(row, colMap["code"])
            val name = getCell(row, colMap["name"])
            val categoryStr = getCell(row, colMap["category"])
            val openingStr = getCell(row, colMap["opening"])
            val balanceTypeStr = getCell(row, colMap["balance_type"])
            val phone = getCell(row, colMap["phone"])
            val address = getCell(row, colMap["address"])
            val notes = getCell(row, colMap["notes"])

            val resolvedType = PartyAccountType.fromString(categoryStr)
            val openingBalance = parseAmount(openingStr)
            val balanceType = resolveBalanceType(balanceTypeStr, resolvedType)

            val assignedCode = when {
                rawCode.isNotBlank() -> rawCode.trim()
                resolvedType != null -> nextCodeFor(resolvedType)
                else -> ""
            }

            // Duplicate detection check
            var duplicateMatch: PartyAccount? = null
            var duplicateReason: String? = null

            if (resolvedType != null && name.isNotBlank()) {
                // Rule 8: Check duplicate with SAME account type
                duplicateMatch = existingAccounts.firstOrNull { existing ->
                    val codeMatch = existing.code.equals(assignedCode, ignoreCase = true)
                    val nameAndTypeMatch = existing.name.equals(name, ignoreCase = true) && existing.accountType == resolvedType
                    val phoneAndTypeMatch = phone.isNotBlank() && existing.phone.isNotBlank() && existing.phone == phone && existing.accountType == resolvedType
                    codeMatch || nameAndTypeMatch || phoneAndTypeMatch
                }

                if (duplicateMatch != null) {
                    duplicateReason = when {
                        duplicateMatch.code.equals(assignedCode, ignoreCase = true) -> "Code ${duplicateMatch.code} already exists"
                        duplicateMatch.name.equals(name, ignoreCase = true) -> "Name '$name' already exists under ${resolvedType.displayName}"
                        else -> "Phone $phone matches ${duplicateMatch.name}"
                    }
                }
            }

            val status = when {
                name.isBlank() -> "Missing Name"
                resolvedType == null -> "Missing Category"
                duplicateMatch != null -> "Duplicate"
                else -> "Ready"
            }

            parsedRows.add(
                ImportedAccountRow(
                    rowIndex = index + 1,
                    rawCode = rawCode,
                    assignedCode = assignedCode,
                    name = name,
                    categoryString = categoryStr,
                    resolvedType = resolvedType,
                    openingBalance = openingBalance,
                    balanceType = balanceType,
                    phone = phone,
                    address = address,
                    notes = notes,
                    status = status,
                    duplicateReason = duplicateReason,
                    existingAccountId = duplicateMatch?.id
                )
            )
        }

        val readyCount = parsedRows.count { it.status == "Ready" }
        val duplicateCount = parsedRows.count { it.status == "Duplicate" }
        val invalidCount = parsedRows.count { it.status != "Ready" && it.status != "Duplicate" }

        return ImportPreviewResult(
            fileName = fileName,
            totalRows = parsedRows.size,
            readyCount = readyCount,
            duplicateCount = duplicateCount,
            invalidCount = invalidCount,
            rows = parsedRows
        )
    }

    private fun detectHeaders(rows: List<List<String>>): Pair<Int, Map<String, Int>> {
        for (i in 0 until minOf(5, rows.size)) {
            val row = rows[i].map { it.trim().lowercase() }
            val map = mutableMapOf<String, Int>()

            row.forEachIndexed { colIdx, text ->
                when {
                    text.contains("code") || text == "id" || text == "acc code" || text == "account code" -> map["code"] = colIdx
                    text.contains("name") || text == "party" || text == "account" || text == "title" || text == "party name" || text == "account name" -> if (!map.containsKey("name")) map["name"] = colIdx
                    text.contains("category") || text.contains("type") || text == "acc type" || text == "group" || text == "account type" -> if (!map.containsKey("category")) map["category"] = colIdx
                    text.contains("opening") || text.contains("balance") || text == "opening bal" || text == "op bal" || text == "amount" -> if (!map.containsKey("opening")) map["opening"] = colIdx
                    text.contains("dr/cr") || text.contains("dr / cr") || text.contains("nature") || text.contains("get/give") || text == "balance type" || text == "type (dr/cr)" -> map["balance_type"] = colIdx
                    text.contains("phone") || text.contains("mobile") || text.contains("contact") || text.contains("cell") || text.contains("whatsapp") -> map["phone"] = colIdx
                    text.contains("address") || text.contains("city") || text.contains("location") -> map["address"] = colIdx
                    text.contains("note") || text.contains("remark") || text.contains("detail") || text.contains("desc") -> map["notes"] = colIdx
                }
            }

            if (map.containsKey("name") || map.containsKey("category") || map.containsKey("code")) {
                return Pair(i, map)
            }
        }

        // Fallback default column order: 0: Code, 1: Name, 2: Category, 3: Opening Balance, 4: Balance Type, 5: Phone, 6: Address, 7: Notes
        val defaultMap = mapOf(
            "code" to 0,
            "name" to 1,
            "category" to 2,
            "opening" to 3,
            "balance_type" to 4,
            "phone" to 5,
            "address" to 6,
            "notes" to 7
        )
        return Pair(-1, defaultMap)
    }

    private fun getCell(row: List<String>, colIdx: Int?): String {
        if (colIdx == null || colIdx < 0 || colIdx >= row.size) return ""
        return row[colIdx].trim()
    }

    private fun parseAmount(str: String): Double {
        if (str.isBlank()) return 0.0
        val clean = str.replace("Rs", "", ignoreCase = true)
            .replace("PKR", "", ignoreCase = true)
            .replace("$", "")
            .replace(",", "")
            .trim()
        return clean.toDoubleOrNull() ?: 0.0
    }

    private fun resolveBalanceType(typeStr: String, accountType: PartyAccountType?): String {
        val s = typeStr.trim().lowercase()
        return when {
            s.startsWith("cr") || s.contains("credit") || s.contains("give") || s.contains("jama") || s.contains("payable") -> "Credit"
            s.startsWith("dr") || s.contains("debit") || s.contains("get") || s.contains("baqaya") || s.contains("receivable") -> "Debit"
            accountType != null -> accountType.defaultNature
            else -> "Debit"
        }
    }

    // ========================================================================
    // CSV Parser
    // ========================================================================
    fun parseCsvStream(inputStream: InputStream): List<List<String>> {
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val rows = mutableListOf<List<String>>()

        var line: String? = reader.readLine()
        if (line == null) return emptyList()

        // Detect separator
        val sep = when {
            line.contains("\t") -> '\t'
            line.contains(";") -> ';'
            else -> ','
        }

        while (line != null) {
            val parsedRow = parseCsvLine(line, sep)
            if (parsedRow.isNotEmpty()) {
                rows.add(parsedRow)
            }
            line = reader.readLine()
        }

        return rows
    }

    private fun parseCsvLine(line: String, separator: Char): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        sb.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == separator && !inQuotes -> {
                    result.add(sb.toString().trim())
                    sb.setLength(0)
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        result.add(sb.toString().trim())
        return result
    }

    // ========================================================================
    // Pure Kotlin XLSX Parser (Streaming Zip + XML)
    // ========================================================================
    fun parseXlsxStream(inputStream: InputStream): List<List<String>> {
        val bytes = inputStream.readBytes()
        val sharedStrings = extractSharedStrings(bytes)
        return extractWorksheetRows(bytes, sharedStrings)
    }

    private fun extractSharedStrings(zipBytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name.equals("xl/sharedStrings.xml", ignoreCase = true)) {
                    val factory = XmlPullParserFactory.newInstance()
                    factory.isNamespaceAware = true
                    val parser = factory.newPullParser()
                    parser.setInput(zis, "UTF-8")

                    var eventType = parser.eventType
                    var inText = false
                    val currentStr = StringBuilder()

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (parser.name.equals("t", ignoreCase = true)) {
                                    inText = true
                                }
                            }
                            XmlPullParser.TEXT -> {
                                if (inText) {
                                    currentStr.append(parser.text)
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (parser.name.equals("t", ignoreCase = true)) {
                                    inText = false
                                } else if (parser.name.equals("si", ignoreCase = true)) {
                                    strings.add(currentStr.toString())
                                    currentStr.setLength(0)
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    break
                }
                entry = zis.nextEntry
            }
        }
        return strings
    }

    private fun extractWorksheetRows(zipBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                // Read sheet1.xml or first worksheet
                if (entry.name.startsWith("xl/worksheets/sheet", ignoreCase = true) && entry.name.endsWith(".xml", ignoreCase = true)) {
                    val factory = XmlPullParserFactory.newInstance()
                    factory.isNamespaceAware = true
                    val parser = factory.newPullParser()
                    parser.setInput(zis, "UTF-8")

                    var eventType = parser.eventType
                    val currentRow = mutableMapOf<Int, String>()
                    var currentCellCol = 0
                    var cellType = ""
                    var inValue = false
                    var inInlineText = false
                    val cellValue = StringBuilder()

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                when (parser.name.lowercase()) {
                                    "row" -> {
                                        currentRow.clear()
                                    }
                                    "c" -> {
                                        val cellRef = parser.getAttributeValue(null, "r") ?: ""
                                        currentCellCol = columnRefToIndex(cellRef)
                                        cellType = parser.getAttributeValue(null, "t") ?: ""
                                        cellValue.setLength(0)
                                    }
                                    "v" -> inValue = true
                                    "t" -> inInlineText = true
                                }
                            }
                            XmlPullParser.TEXT -> {
                                if (inValue || inInlineText) {
                                    cellValue.append(parser.text)
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                when (parser.name.lowercase()) {
                                    "v" -> inValue = false
                                    "t" -> inInlineText = false
                                    "c" -> {
                                        val text = when (cellType) {
                                            "s" -> {
                                                val sIdx = cellValue.toString().toIntOrNull()
                                                if (sIdx != null && sIdx in sharedStrings.indices) sharedStrings[sIdx] else ""
                                            }
                                            else -> cellValue.toString()
                                        }
                                        currentRow[currentCellCol] = text
                                    }
                                    "row" -> {
                                        if (currentRow.isNotEmpty()) {
                                            val maxCol = (currentRow.keys.maxOrNull() ?: 0)
                                            val rowList = (0..maxCol).map { col -> currentRow[col] ?: "" }
                                            rows.add(rowList)
                                        }
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    break
                }
                entry = zis.nextEntry
            }
        }
        return rows
    }

    private fun columnRefToIndex(cellRef: String): Int {
        var col = 0
        for (ch in cellRef.uppercase()) {
            if (ch in 'A'..'Z') {
                col = col * 26 + (ch - 'A' + 1)
            } else {
                break
            }
        }
        return (col - 1).coerceAtLeast(0)
    }

    private fun isZipFormat(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(4)
                val read = stream.read(header)
                read == 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "accounts_import.xlsx"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx != -1) {
                    name = it.getString(idx) ?: name
                }
            }
        }
        return name
    }

    /**
     * Generates a sample CSV format string for quick testing or user download.
     */
    fun generateSampleCsv(): String {
        return buildString {
            appendLine("Account Code,Account Name,Category,Opening Balance,Balance Type,Phone Number,Address,Notes")
            appendLine("OWN-001,Partner A Capital,Owner,500000,Credit,+92 300 1111111,Head Office,Founding partner capital")
            appendLine("INS-001,Investor Alpha,Investor,1000000,Credit,+92 300 2222222,Main Branch,Working capital investment")
            appendLine("FAC-001,Main Scrap Factory,Factory,0,Debit,+92 300 3333333,Site Area,Factory operations account")
            appendLine("LAB-001,Plant Operator Lead,Labour & Employee,0,Debit,+92 300 4444444,Factory Quarter,Monthly salary labour")
            appendLine("CUS-001,National Steel Mills,Customer,75000,Debit,+92 300 5555555,Industrial Zone,Wholesale customer")
            appendLine("CASH-001,Primary Factory Cash,Cash In Hand,250000,Debit,+92 300 6666666,Cash Vault,Vault cash in hand")
        }
    }
}
