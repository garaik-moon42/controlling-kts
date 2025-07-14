import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.bufferedReader
import kotlin.io.path.listDirectoryEntries

private fun importFile(filePath: Path) {
    println("Processing [${filePath.toAbsolutePath()}]...")
    val cset = Charset.forName(Config.data.csvImport.charset)
    val header: String = filePath.bufferedReader(cset).useLines { it.firstOrNull() }
        ?: error("Import file not found or it is empty: $filePath")
    filePath.bufferedReader(cset).useLines { lines ->
        lines
            .filter { line -> line != header }
            .map(TransactionLogItem::of)
            .map(TransactionLogItem::detectCtrlCategory) // I am not really sure that the detection mechanism is good at this place
            .forEach(DatabaseConnector::insert)
    }
}

fun main() {
    Path.of(Config.data.csvImport.dir).listDirectoryEntries("HISTORY_2025.csv").forEach(::importFile)
    println("${DatabaseConnector.insertCount} items inserted, ${DatabaseConnector.skipCount} items skipped.")
}