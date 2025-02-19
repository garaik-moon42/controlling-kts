fun main() {
    val gst = GoogleSheetsTools.connectAs("Transaction Log Updater", Config.Google.apiClientSecret)
    val content: List<Map<String, String>> = gst.getSheetContentAsMap("1OW-XxFt984tqmEqx6AJtH5Lqh-NVWyg-WDHb5TGSbi0", "TRANSACTIONS")
    content.map(TransactionLogItem::of).forEach(DatabaseConnector::update)
    println("${DatabaseConnector.updateCount} transaction log item updated.")
}
