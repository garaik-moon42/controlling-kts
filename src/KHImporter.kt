import java.math.BigDecimal
import java.nio.charset.Charset
import java.nio.file.Path
import java.sql.*
import java.sql.Date as SqlDate
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import kotlin.io.path.bufferedReader
import kotlin.io.path.listDirectoryEntries

const val DATABASE_TABLE_NAME = "BANK_TRANSACTIONS_TMP"
const val INPUT_DIR_PATH = "input/"

data class TransactionLogItem(
    val accountNumber: String,
    val accountName: String,
    val entryDate: ZonedDateTime,
    val valueDate: LocalDate,
    val counterAccountNumber: String,
    val partner: String,
    val amount: BigDecimal,
    val currency: String,
    val ctrlCategory: String? = null,
    val ctrlMonth: LocalDate? = null,
    val ctrlInclude: Boolean? = null,
    val ctrlVat: Boolean? = null,
    val ctrlAmount: BigDecimal? = null,
    val notice: String,
    val transactionBankId: String,
    val transactionTypeCode: String,
    val transactionTypeName: String,
    val ctrlInvoiceUrl: String? = null
) {
    val id: Int by lazy {
        Objects.hash(accountNumber, entryDate, partner, amount, transactionBankId)
    }
    val month: LocalDate by lazy { valueDate.withDayOfMonth(1) }

    companion object {
        fun createOf(exportCSVLine: String) : TransactionLogItem {
            val i = exportCSVLine.split(";(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.substring(1, it.length - 1) }.iterator()
            return TransactionLogItem(
                accountNumber = i.next(),
                accountName = i.next(),
                entryDate = ZonedDateTime.parse(i.next()),
                valueDate = LocalDate.parse(i.next()),
                counterAccountNumber = i.next(),
                partner = i.next(),
                amount = BigDecimal(i.next().trim().replace(',', '.')),
                currency = i.next().uppercase(),
                notice = i.next().trim().replace(" {2,}".toRegex(), " "),
                transactionBankId = i.next(),
                transactionTypeCode = i.next(),
                transactionTypeName = i.next(),
            )
        }
    }
}

object DatabaseConnector: AutoCloseable {
    private val connection: Connection by lazy {
        val jdbcUrl = "jdbc:mysql://${Config.DB.host}:${Config.DB.port}/${Config.DB.name}?useSSL=false&allowPublicKeyRetrieval=true"
        DriverManager.getConnection(jdbcUrl, Config.DB.user, Config.DB.password)
    }

    private val insertStatement: PreparedStatement = connection.prepareStatement("""
        insert into $DATABASE_TABLE_NAME (
            ID, ACCOUNT_NUMBER, ACCOUNT_NAME, ENTRY_DATE, VALUE_DATE, MONTH, COUNTER_ACCOUNT_NUMBER, PARTNER,
            AMOUNT, CURRENCY, CTRL_CATEGORY, CTRL_MONTH, CTRL_INCLUDE, CTRL_VAT, CTRL_AMOUNT,
            NOTICE, TRANSACTION_BANK_ID, TRANSACTION_TYPE_CODE, TRANSACTION_TYPE_NAME, CTRL_INVOICE_URL
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
    """.trimIndent())

    var insertCount = 0
    var skipCount = 0

    val storedItemIds: MutableSet<Int> by lazy {
        HashSet<Int>().apply {
            connection.createStatement().executeQuery("select id from $DATABASE_TABLE_NAME;").use { rs ->
                while (rs.next()) {
                    add(rs.getInt(1))
                }
            }
        }
    }

    override fun close() {
        insertStatement.close()
        connection.close()
    }

    fun insert(tli: TransactionLogItem) {
        if (storedItemIds.contains(tli.id)) {
            skipCount++
        } else {
            println("Inserting $tli...")
            insertStatement.setInt(1, tli.id)
            insertStatement.setString(2, tli.accountNumber)
            insertStatement.setString(3, tli.accountName)
            insertStatement.setTimestamp(4, Timestamp.from(tli.entryDate.toInstant()))
            insertStatement.setDate(5, SqlDate.valueOf(tli.valueDate))
            insertStatement.setDate(6, SqlDate.valueOf(tli.month))
            insertStatement.setString(7, tli.counterAccountNumber)
            insertStatement.setString(8, tli.partner)
            insertStatement.setBigDecimal(9, tli.amount)
            insertStatement.setString(10, tli.currency)
            insertStatement.setString(11, tli.ctrlCategory)
            insertStatement.setDate(12, tli.ctrlMonth?.let(SqlDate::valueOf))
            insertStatement.setInt(13, when (tli.ctrlInclude) {
                null -> -1
                false -> 0
                true -> 1
            })
            insertStatement.setInt(14, when (tli.ctrlVat) {
                null -> -1
                false -> 0
                true -> 1
            })
            insertStatement.setBigDecimal(15, tli.ctrlAmount)
            insertStatement.setString(16, tli.notice)
            insertStatement.setString(17, tli.transactionBankId)
            insertStatement.setString(18, tli.transactionTypeCode)
            insertStatement.setString(19, tli.transactionTypeName)
            insertStatement.setString(20, tli.ctrlInvoiceUrl)
            insertStatement.executeUpdate()
            storedItemIds.add(tli.id)
            insertCount++
        }
    }
}

private fun importFile(filePath: Path) {
    println("Processing [${filePath.toAbsolutePath()}]...")
    val cset = Charset.forName("ISO-8859-2")
    val header: String = filePath.bufferedReader(cset).useLines { it.firstOrNull() }
        ?: error("Import file not found or it is empty: $filePath")
    filePath.bufferedReader(cset).useLines { lines ->
        lines
            .filter { line -> line != header }
            .map(TransactionLogItem::createOf)
            .forEach(DatabaseConnector::insert)
    }
}

fun main() {
    Path.of(INPUT_DIR_PATH).listDirectoryEntries("*.csv").forEach(::importFile)
    println("${DatabaseConnector.insertCount} items inserted, ${DatabaseConnector.skipCount} items skipped.")
}