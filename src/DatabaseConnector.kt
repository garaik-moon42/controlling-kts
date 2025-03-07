import java.math.BigDecimal
import java.sql.*
import java.time.LocalDate
import java.util.HashSet

const val DATABASE_TABLE_NAME = "BANK_TRANSACTIONS"

object DatabaseConnector: AutoCloseable {
    private val connection: Connection by lazy {
        val jdbcUrl = "jdbc:mysql://${Config.data.db.host}:${Config.data.db.port}/${Config.data.db.name}?useSSL=false&allowPublicKeyRetrieval=true"
        DriverManager.getConnection(jdbcUrl, Config.data.db.user, Config.data.db.password)
    }

    private fun prepareUpdateStatement(fieldName: String) = connection.prepareStatement("update $DATABASE_TABLE_NAME set $fieldName = ? where id = ?")

    private val insertStatement: PreparedStatement = connection.prepareStatement("""
        insert into $DATABASE_TABLE_NAME (
            ID, ACCOUNT_NUMBER, ACCOUNT_NAME, ENTRY_DATE, VALUE_DATE, MONTH, COUNTER_ACCOUNT_NUMBER, PARTNER,
            AMOUNT, CURRENCY, CTRL_CATEGORY, CTRL_MONTH, CTRL_INCLUDE, CTRL_VAT, CTRL_AMOUNT,
            NOTICE, TRANSACTION_BANK_ID, TRANSACTION_TYPE_CODE, TRANSACTION_TYPE_NAME, CTRL_INVOICE_URL
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
    """.trimIndent())
    private val updateCategoryStatement = prepareUpdateStatement("CTRL_CATEGORY")
    private val updateMonthStatement = prepareUpdateStatement("CTRL_MONTH")
    private val updateIncludeStatement = prepareUpdateStatement("CTRL_INCLUDE")
    private val updateVatStatement = prepareUpdateStatement("CTRL_VAT")
    private val updateAmountStatement = prepareUpdateStatement("CTRL_AMOUNT")
    private val updateInvoiceUrlStatement = prepareUpdateStatement("CTRL_INVOICE_URL")

    var insertCount = 0
    var skipCount = 0
    var updateCount = 0

    private val storedItemIds: MutableSet<Int> by lazy {
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
        updateCategoryStatement.close()
        updateMonthStatement.close()
        updateIncludeStatement.close()
        updateVatStatement.close()
        updateAmountStatement.close()
        updateInvoiceUrlStatement.close()
        connection.close()
    }

    fun insert(tli: TransactionLogItem) {
        if (storedItemIds.contains(tli.id)) {
            skipCount++
        } else {
            fun booleanToInt(b: Boolean?) = when (b) {
                null -> -1
                false -> 0
                true -> 1
            }
            println("Inserting $tli...")
            insertStatement.setInt(1, tli.id)
            insertStatement.setString(2, tli.accountNumber)
            insertStatement.setString(3, tli.accountName)
            insertStatement.setTimestamp(4, Timestamp.valueOf(tli.entryDate))
            insertStatement.setDate(5, Date.valueOf(tli.valueDate))
            insertStatement.setDate(6, Date.valueOf(tli.month))
            insertStatement.setString(7, tli.counterAccountNumber)
            insertStatement.setString(8, tli.partner)
            insertStatement.setBigDecimal(9, tli.amount)
            insertStatement.setString(10, tli.currency)
            insertStatement.setString(11, tli.ctrlCategory)
            insertStatement.setDate(12, tli.ctrlMonth?.let(Date::valueOf))
            insertStatement.setInt(13, booleanToInt(tli.ctrlInclude))
            insertStatement.setInt(14, booleanToInt(tli.ctrlVat))
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

    fun update(tli: TransactionLogItem) {
        fun executeUpdateIfNotNull(statement: PreparedStatement, value: Any?): Int {
            if (value != null) {
                when (value) {
                    is String -> statement.setString(1, value)
                    is LocalDate -> statement.setDate(1, Date.valueOf(value))
                    is BigDecimal -> statement.setBigDecimal(1, value)
                    is Boolean -> statement.setInt(1, if (value) 1 else 0)
                    else -> error("Unsupported value type: ${value::class.java.name}")
                }
                statement.setInt(2, tli.id)
                return statement.executeUpdate()
            }
            return 0
        }

        with (connection) {
            try {
                autoCommit = false
                val updateResults = listOf(
                    executeUpdateIfNotNull(updateCategoryStatement, tli.ctrlCategory),
                    executeUpdateIfNotNull(updateMonthStatement, tli.ctrlMonth),
                    executeUpdateIfNotNull(updateIncludeStatement, tli.ctrlInclude),
                    executeUpdateIfNotNull(updateVatStatement, tli.ctrlVat),
                    executeUpdateIfNotNull(updateAmountStatement, tli.ctrlAmount),
                    executeUpdateIfNotNull(updateInvoiceUrlStatement, tli.ctrlInvoiceUrl)
                )
                if (updateResults.any { it > 1 }) {
                    error("Unexpected update result count: $updateResults for $tli")
                }
                if (updateResults.any { it == 1 }) {
                    commit()
                    updateCount++
                    println("Updated $tli")
                }
                autoCommit = true
            } catch (e: Exception) {
                rollback()
                throw e
            }
        }
    }
}