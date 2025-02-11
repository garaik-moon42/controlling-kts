# controlling-kts
Controlling related tools written in Kotlin.

This project uses Intellij IDEA's build system. Open this folder as a Kotlin project, rebuild it, and you can use it.

Currently the project consists of two runable files:
  * `KHImporter.kt` - Import the bank account history from the CSV file exported from the K&H online banking interface.
  * `KHTransferExport.kt` - Exports items to be transferred from a google sheets containing invoices to pay.
