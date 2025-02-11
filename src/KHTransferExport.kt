const val APPLICATION_NAME = "KH Transfer Export"
const val SPREADSHEET_ID = "1xuojTv-_jiu6itu7kVxNOSD27lY499d1HNCE9vTyzsw"
const val SHEET_NAME = "SZÁMLÁK"
const val COLUMN_STATE = "státusz"
const val COLUMN_TRANSFER_DATE = "utalás napja"

fun export() {
    val gst = GoogleSheetsTools.connectAs(APPLICATION_NAME, Config.Google.apiClientSecret)
    val content = gst.getSheetContentAsMap(SPREADSHEET_ID, SHEET_NAME).filter { it[COLUMN_STATE] == "Rögzíthető" }.groupBy { it[COLUMN_TRANSFER_DATE] }
    content.keys.forEach { transferDate ->
        println("$transferDate:")
        content[transferDate]?.forEach(::println)
    }
}

fun main() {
    export()
}
