# controlling-kts
Controlling related tools written in Kotlin.

This project uses Intellij IDEA's build system. Open this folder as a Kotlin project, rebuild it, and you can use it.

Before using it make a copy of `controlling-config.json.sample` as `controlling-config.json` and fill the missing
credential information.

Currently the project consists of the following runnable files:
  * `KHImporter.kt` - Import the bank account history from the CSV file exported from the K&H online banking interface.
  * `KHTransferExport.kt` - Exports items to be transferred from a Google sheets containing invoices to pay.
  * `TransactionLogUpdater.kt` - Connects to the Google sheets configured and updates transaction log according to their 
                                 content.
