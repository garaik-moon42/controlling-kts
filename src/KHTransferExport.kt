import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter

const val COLUMN_STATE = "státusz"
const val COLUMN_TRANSFER_DATE = "utalás napja"
const val COLUMN_TARGET_ACCOUNT_GIRO = "Számlaszám"
const val COLUMN_BENEFICIARY  = "Kedvezményezett"
const val COLUMN_AMOUNT = "bruttó"
const val COLUMN_NOTICE = "Közlemény"
const val COLUMN_CURRENCY = "pénznem"

val myConfig = Config.data.transferExport

val EXPORT_FILE_HEADER = listOf(
    "Forrás számlaszám-max.28",
    "Partner számlaszáma-max.28",
    "Partner neve-max.75",
    "Átutalandó összeg-max.18",
    "Átutalandó deviza-max.3",
    "Közlemény-max.140",
    "Átutalás egyedi azonosítója-max.35",
    "Értéknap-max.10"
).joinToString(";")

val inputDateFormatter: DateTimeFormatter  = DateTimeFormatter.ofPattern("yyyy.MM.dd.")
val outputDateFormatter: DateTimeFormatter  = DateTimeFormatter.ofPattern("yyyy.MM.dd")
val fileNameDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

data class Transaction(
    val targetAccountNumber: String,
    val beneficiary: String,
    val amount: BigDecimal,
    val currency: String,
    val notice: String,
    val transferDate: LocalDate,
) {
    companion object {
        fun of(row: Map<String, String>):Transaction {
            return Transaction(
                targetAccountNumber = row[COLUMN_TARGET_ACCOUNT_GIRO]?.replace("-", "")?.trim() ?: error("Target account number is missing."),
                beneficiary = row[COLUMN_BENEFICIARY]?.replace(myConfig.valueSeparator, " ")?.trim() ?: error("Beneficiary is missing."),
                amount = row[COLUMN_AMOUNT]?.substringBefore(',')?.replace("\u00A0", "")?.trim()?.toBigDecimal() ?: error("Amount is missing."),
                currency = row[COLUMN_CURRENCY] ?: error("Currency is missing."),
                notice = row[COLUMN_NOTICE]?.replace(" ", "")?.trim()?.ifBlank { null } ?: error("Notice is missing."),
                transferDate = row[COLUMN_TRANSFER_DATE]?.ifBlank { null }?.let { LocalDate.parse(it, inputDateFormatter) } ?: error("Transfer date is missing.")
            )
        }
    }
}

fun export() {
    File(myConfig.targetDir).also(File::deleteRecursively).mkdirs() // init and empty target dir
    val gst = GoogleSheetsTools.connectAs(Config.data.applicationName)
    val content = gst.getSheetContentAsMap(myConfig.sourceSpreadsheetId, myConfig.sourceSheetName)
        .filter { it[COLUMN_STATE] == "Rögzíthető" && it[COLUMN_CURRENCY] == "HUF" }
        .map { Transaction.of(it)}
        .groupBy { it.transferDate }
        .map { (transferDate, transactions) -> transferDate to groupTransactionsByPartner(transactions) }
        .toMap()
    content.keys.forEach { transferDate -> createFile(transferDate, content[transferDate]!!) }
}

private fun joinWithLengthLimit(s1: String, s2: String, limit: Int = 140): String {
    val s = "$s1,$s2"
    return if (s.length <= limit) {
        s
    } else {
        error("Notice length limit exceeded: ${s.length} > $limit when adding notice: `$s2`")
    }
}

private fun groupTransactionsByPartner(transactions: List<Transaction>):List<Transaction> {
    return transactions.groupBy { it.targetAccountNumber }.values.map { partnerTransactions ->
        partnerTransactions.reduce {
           t1, t2 -> Transaction(
               t1.targetAccountNumber,
               t1.beneficiary,
               t1.amount + t2.amount,
               t1.currency,
               joinWithLengthLimit(t1.notice, t2.notice),
               t1.transferDate
           )
        }
    }
}

private fun createFile(transferDate: LocalDate, content: List<Transaction>) {
    FileOutputStream("${myConfig.targetDir}${File.separator}kh-utalandok-${fileNameDateFormatter.format(transferDate)}.HUF.csv")
        .bufferedWriter(Charset.forName(myConfig.charset))
        .use { out ->
            out.write(EXPORT_FILE_HEADER)
            out.newLine()
            content.forEach { row ->
                out.write(generateExportLine(row))
                out.newLine()
            }
        }
}

private fun generateExportLine(row: Transaction):String {
    return listOf(
        myConfig.sourceAccountGiro,
        row.targetAccountNumber,
        row.beneficiary,
        row.amount.toString(),
        row.currency,
        row.notice,
        "",
        outputDateFormatter.format(row.transferDate),
    ).joinToString(myConfig.valueSeparator)
}

fun main() {
    export()
}
