import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.util.*


data class TransactionLogItem (
    val id:Int,
    val accountNumber: String,
    val accountName: String,
    val entryDate: LocalDateTime,
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
    val month: LocalDate by lazy { valueDate.withDayOfMonth(1) }

    companion object {
        private val GOOGLE_SHEET_DATETIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private val GOOGLE_SHEET_MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
        private val GOOGLE_SHEET_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        fun of(exportCSVLine: String) : TransactionLogItem {
            val i = exportCSVLine.split(";(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.substring(1, it.length - 1) }
            val accountNumber = i[0]
            val entryDate = ZonedDateTime.parse(i[2])
            val amount = BigDecimal(i[6].trim().replace(',', '.'))
            val partner = i[5]
            val transactionBankId = i[9]
            return TransactionLogItem(
                id = Objects.hash(accountNumber, entryDate, partner, amount, transactionBankId),
                accountNumber = accountNumber,
                accountName = i[1],
                entryDate = entryDate.toLocalDateTime(),
                valueDate = LocalDate.parse(i[3]),
                counterAccountNumber = i[4],
                partner = partner,
                amount = amount,
                currency = i[7].uppercase(),
                notice = i[8].trim().replace(" {2,}".toRegex(), " "),
                transactionBankId = transactionBankId,
                transactionTypeCode = i[10],
                transactionTypeName = i[11],
            )
        }
        
        fun of(transactionSheetRow: Map<String, String>) : TransactionLogItem {
            fun booleanValueOf(cellValue: String): Boolean? {
                if (cellValue.isBlank()) {
                    return null
                }
                return try {
                    when (cellValue.toInt()) {
                        0 -> false
                        1 -> true
                        else -> null
                    }
                } catch (e: NumberFormatException) {
                    null
                }
            }

            fun bigDecimalValueOf(cellValue: String?): BigDecimal? {
                if (cellValue.isNullOrBlank()) {
                    return null
                }
                return try {
                    BigDecimal(cellValue.replace(",",""))
                } catch (e: NumberFormatException) {
                    null
                }
            }

            fun dateValueOf(monthReference: String): LocalDate? {
                if (monthReference.isBlank()) {
                    return null
                }
                return try {
                    // Parse the year and month, and add the first day of the month
                    val parsedYearMonth = GOOGLE_SHEET_MONTH_FORMAT.parse(monthReference)
                    LocalDate.of(parsedYearMonth[ChronoField.YEAR], parsedYearMonth[ChronoField.MONTH_OF_YEAR], 1)
                } catch (e: DateTimeParseException) {
                    null
                }
            }

            return TransactionLogItem(
                id = transactionSheetRow["ID"]?.toInt()!!,
                accountNumber = transactionSheetRow["ACCOUNT_NUMBER"]!!,
                accountName = transactionSheetRow["ACCOUNT_NAME"]!!,
                entryDate = GOOGLE_SHEET_DATETIME_FORMAT.parse(transactionSheetRow["ENTRY_DATE"]!!, LocalDateTime::from),
                valueDate = GOOGLE_SHEET_DATE_FORMAT.parse(transactionSheetRow["VALUE_DATE"]!!, LocalDate::from),
                counterAccountNumber = transactionSheetRow["COUNTER_ACCOUNT_NUMBER"]!!,
                partner = transactionSheetRow["PARTNER"]!!,
                amount =  bigDecimalValueOf(transactionSheetRow["AMOUNT"])!!,
                currency = transactionSheetRow["CURRENCY"]!!,
                ctrlCategory = transactionSheetRow["CTRL_CATEGORY"],
                ctrlMonth = dateValueOf(transactionSheetRow["CTRL_MONTH"]!!),
                ctrlInclude = booleanValueOf(transactionSheetRow["CTRL_INCLUDE"]!!),
                ctrlVat = booleanValueOf(transactionSheetRow["CTRL_VAT"]!!),
                ctrlAmount = bigDecimalValueOf(transactionSheetRow["CTRL_AMOUNT"]),
                notice = transactionSheetRow["NOTICE"]!!,
                transactionBankId = transactionSheetRow["TRANSACTION_BANK_ID"]!!,
                transactionTypeCode = transactionSheetRow["TRANSACTION_TYPE_CODE"]!!,
                transactionTypeName = transactionSheetRow["TRANSACTION_TYPE_NAME"]!!,
                ctrlInvoiceUrl = transactionSheetRow["CTRL_INVOICE_URL"]?.trim()
            )
        }

        // ToDo: consider moving this method to a better place
        fun detectCtrlCategory(tli: TransactionLogItem): TransactionLogItem {
            val onlineServicePartners = setOf(
                "Adobe Systems Software", "OPENAI  CHATGPT SUBSCR", "ATLASSIAN", "GOOGLE GSUITE MOON42.C",
                "AWS EMEA", "REMARKABLE", "2CO.COM!HP INC.", "ZAPIER.COM/CHARGE", "SLACK T0225UG4P9C",
                "SLACK TQK3B5K8A", "DEEPL  SUB 2654037 CUS")
            val bankCostPartners = setOf("Kp.felvét tranzakciós jutalék", "Könyvelési díj - deviza",
                "Könyvelési díj - hitel", "Kamat", "Könyvelési díj", "Rendelkezésre tartási jutalék",
                "Hitelkamat törlesztés")
            val subcontractorPartners = setOf("Proszenyák Norbert ev.", "Tóth Bence Dániel", "Hawat Consulting Bt.",
                "Identity Hungary Kft.", "Build Kft. Tóth Bence", "Robár Róbert")
            return when {
                tli.notice.contains("Kártyafedezeti rendelkezés alapján", ignoreCase = true)
                        || tli.transactionTypeName.contains("azonnali ft átvezetés", ignoreCase = true)
                        || tli.transactionTypeName.contains("murex interfész könyvelései", ignoreCase = true)
                        -> tli.copy(ctrlCategory = "átvezetés", ctrlInclude = false, ctrlVat = false)
                tli.transactionTypeName.contains("készpénz felvétel - atm", ignoreCase = true)
                        -> tli.copy(ctrlCategory = "készpénz", ctrlInclude = false, ctrlVat = false)
                tli.transactionTypeName.contains("Bankkártyával kapcsolatos jutalék", ignoreCase = true)
                        || tli.transactionTypeName.contains("jutalék, díj", ignoreCase = true)
                        || tli.transactionTypeName.contains("átutalás jutalék - elektronikus", ignoreCase = true)
                        || bankCostPartners.contains(tli.partner)
                        -> tli.copy(ctrlCategory = "bankköltség", ctrlInclude = true, ctrlVat = false)
                onlineServicePartners.contains(tli.partner)
                        -> tli.copy(ctrlCategory = "online szolgáltatás")
                tli.partner.contains("cleverant", ignoreCase = true)
                        -> tli.copy(ctrlCategory = "cleverant")
                subcontractorPartners.contains(tli.partner)
                        -> tli.copy(ctrlCategory = "alvállalkozó", ctrlInclude = true, ctrlVat = true)
                else -> tli
            }
            
        }
    }
}