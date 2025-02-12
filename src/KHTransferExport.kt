const val APPLICATION_NAME = "KH Transfer Export"
const val SPREADSHEET_ID = "1xuojTv-_jiu6itu7kVxNOSD27lY499d1HNCE9vTyzsw"
const val SHEET_NAME = "SZÁMLÁK"
const val COLUMN_STATE = "státusz"
const val COLUMN_TRANSFER_DATE = "utalás napja"
const val COLUMN_TARGET_ACCOUNT_GIRO = "Számlaszám"
const val COLUMN_BENEFICIARY  = "Kedvezményezett"
const val COLUMN_AMOUNT = "bruttó"
const val COLUMN_CURRENCY = "pénznem"
const val SOURCE_ACCOUNT_GIRO = "104010005052678450831006"
const val VALUE_SEPARATOR = ";"

fun export() {
    val gst = GoogleSheetsTools.connectAs(APPLICATION_NAME, Config.Google.apiClientSecret)
    val content = gst.getSheetContentAsMap(SPREADSHEET_ID, SHEET_NAME)
        .filter { it[COLUMN_STATE] == "Rögzíthető" }
    content.forEach { row ->
        println(generateExportLine(row))
    }
}

private fun generateExportLine(row: Map<String?, String>):String {
    return listOf(
        SOURCE_ACCOUNT_GIRO,
        row[COLUMN_TARGET_ACCOUNT_GIRO]?.replace("-", "")?.trim(),
        row[COLUMN_BENEFICIARY]?.replace(VALUE_SEPARATOR, " ")?.trim(),
        
        row[COLUMN_AMOUNT]?.replace(" ", "")?.trim(),
        row[COLUMN_CURRENCY],
        row[COLUMN_TRANSFER_DATE]
    ).joinToString(VALUE_SEPARATOR)
}

fun main() {
    export()
}
