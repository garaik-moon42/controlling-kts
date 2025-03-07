fun main() {
    val gst = GoogleSheetsTools.connectAs("Transaction Log Updater")
    Config.data.transactionUpdater.importSheets.filter { it.active }.forEach { importSheetConfig ->
        println("Processing [${importSheetConfig.remark}]...")
        val content: List<Map<String, String>> = gst.getSheetContentAsMap(importSheetConfig.id, importSheetConfig.sheetName)
        content.map(TransactionLogItem::of).forEach(DatabaseConnector::update)
    }
    println("${DatabaseConnector.updateCount} transaction log item updated.")
}
