import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import java.io.File
import java.io.InputStreamReader

class GoogleSheetsTools private constructor(private val service: Sheets) {

    fun getSheetContentAsMap(spreadsheetId: String, sheetName: String): List<Map<String, String>> {
        fun columnIndexToA1(index: Int): String {
            return buildString {
                var i = index
                while (i >= 0) {
                    insert(0, ('A' + (i % 26)))
                    i = (i / 26) - 1
                }
            }
        }

        val spreadsheet = service.spreadsheets().get(spreadsheetId).execute()
        val sheet = spreadsheet.sheets.firstOrNull { it.properties.title == sheetName }
            ?: error("Sheet $sheetName not found.")
        val lastRow = sheet.properties.gridProperties.rowCount ?: 0
        val lastColumn = sheet.properties.gridProperties.columnCount ?: 0
        val range = "$sheetName!A1:${columnIndexToA1(lastColumn - 1)}$lastRow"
        val response = service.spreadsheets().values().get(spreadsheetId, range).execute()
        val content = response.getValues()?.filterNotNull() ?: emptyList()
        val header = content.first()
        if (header.any { it == null || it.toString().isBlank() }) {
            error("Sheet header contains null or empty values: $header")
        }
        val headerMap = header.mapIndexed { i, columnName -> i to columnName.toString() }.toMap()

        return content.drop(1).map { row ->
            row.filterNotNull().mapIndexed { i, value -> headerMap[i]!! to value.toString() }.toMap()
        }
    }

    companion object {

        fun connectAs(applicationName: String):GoogleSheetsTools {
            val credentialsFilePath = Config.data.google.clientSecret
            val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
            val service = Sheets.Builder(httpTransport, GsonFactory.getDefaultInstance(), getCredentials(httpTransport, credentialsFilePath))
                .setApplicationName(applicationName)
                .build()
            return GoogleSheetsTools(service)
        }

        private fun getCredentials(httpTransport: NetHttpTransport, credentialsFilePath: String): Credential {
            val csStream = object {}.javaClass.getResourceAsStream(credentialsFilePath) ?: error("Credentials file not found: $credentialsFilePath")
            val clientSecret = GoogleClientSecrets.load(GsonFactory.getDefaultInstance(), InputStreamReader(csStream))
            val flow = GoogleAuthorizationCodeFlow.Builder(
                httpTransport, GsonFactory.getDefaultInstance(), clientSecret, listOf(SheetsScopes.SPREADSHEETS_READONLY))
                .setDataStoreFactory(FileDataStoreFactory(File(Config.data.google.tokenDir)))
                .setAccessType("offline")
                .build()
            return AuthorizationCodeInstalledApp(flow, LocalServerReceiver()).authorize("user")
        }
    }
}