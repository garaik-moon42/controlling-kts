import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset

const val APPLICATION_NAME = "KH Transfer Export"
const val SPREADSHEET_ID = "1xuojTv-_jiu6itu7kVxNOSD27lY499d1HNCE9vTyzsw"
const val SHEET_NAME = "SZÁMLÁK"
const val COLUMN_STATE = "státusz"
const val COLUMN_TRANSFER_DATE = "utalás napja"
const val COLUMN_TARGET_ACCOUNT_GIRO = "Számlaszám"
const val COLUMN_BENEFICIARY  = "Kedvezményezett"
const val COLUMN_AMOUNT = "bruttó"
const val COLUMN_NOTICE = "Közlemény"
const val COLUMN_CURRENCY = "pénznem"
const val SOURCE_ACCOUNT_GIRO = "104010005052678450831006"
const val VALUE_SEPARATOR = ";"
const val TARGET_CHARSET = "ISO-8859-2"
val EXPORT_FILE_HEADER = listOf(
    "Forrás számlaszám-max.28",
    "Partner számlaszáma-max.28",
    "Partner neve-max.75",
    "Átutalandó összeg-max.18",
    "Átutalandó deviza-max.3",
    "Közlemény-max.140",
    "Átutalás egyedi azonosítója-max.35",
    "Értéknap-max.10"
).joinToString(":")

fun export() {
    File(Config.targetDir).also(File::deleteRecursively).mkdirs() // init and empty target dir
    val gst = GoogleSheetsTools.connectAs(APPLICATION_NAME, Config.Google.apiClientSecret)
    val content = gst.getSheetContentAsMap(SPREADSHEET_ID, SHEET_NAME)
        .filter { it[COLUMN_STATE] == "Rögzíthető" }.groupBy { it[COLUMN_TRANSFER_DATE] ?: "" }
    content.keys.forEach { transferDate -> createFile(transferDate, content[transferDate]!!) }
}

private fun createFile(transferDateStr: String, content: List<Map<String, String>>) {
    val fileNameDate = transferDateStr.replace(".", "")
    FileOutputStream("${Config.targetDir}${File.separator}kh-utalandok-$fileNameDate.HUF.csv")
        .bufferedWriter(Charset.forName(TARGET_CHARSET))
        .use { out ->
            out.write(EXPORT_FILE_HEADER)
            out.newLine()
            content.forEach { row ->
                out.write(generateExportLine(row))
                out.newLine()
            }
        }
}

private fun generateExportLine(row: Map<String, String>):String {
    return listOf(
        SOURCE_ACCOUNT_GIRO,
        row[COLUMN_TARGET_ACCOUNT_GIRO]?.replace("-", "")?.trim(),
        row[COLUMN_BENEFICIARY]?.replace(VALUE_SEPARATOR, " ")?.trim(),
        row[COLUMN_AMOUNT]?.substringBefore(',')?.replace("\u00A0", "")?.trim(),
        row[COLUMN_CURRENCY],
        row[COLUMN_NOTICE] ?: "",
        "",
        row[COLUMN_TRANSFER_DATE]?.substringBeforeLast('.')
    ).joinToString(VALUE_SEPARATOR)
}

fun main() {
    export()
}
