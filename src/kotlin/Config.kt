import com.google.gson.Gson
import java.io.InputStreamReader

object Config {
    val data: ConfigData

    init {
        val configFileName = "controlling-config.json"
        val configFileUrl = this::class.java.classLoader.getResource(configFileName)
            ?: error("Config file not found: $configFileName")
        data = InputStreamReader(configFileUrl.openStream()).use {
            Gson().fromJson(it, ConfigData::class.java)
        }
    }
}

data class ImportSheetConfig (
    val remark: String,
    val active: Boolean,
    val id: String,
    val sheetName: String
)

data class ConfigData (
    val applicationName: String,
    val db: DBConfig,
    val google: GoogleConfig,
    val csvImport: CsvImportConfig,
    val transferExport: TransferExportConfig,
    val transactionUpdater: TransactionUpdaterConfig
)

data class DBConfig (
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: String
)

data class GoogleConfig (
    val clientSecret: String,
    val tokenDir: String
)

data class CsvImportConfig (
    val dir: String,
    val charset: String
)

data class TransferExportConfig (
    val sourceSpreadsheetId: String,
    val sourceSheetName: String,
    val sourceAccountGiro: String,
    val valueSeparator: String,
    val charset: String,
    val targetDir: String
)

data class TransactionUpdaterConfig (
    val importSheets: List<ImportSheetConfig>
)