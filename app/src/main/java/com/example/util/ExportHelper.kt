package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.SettingsEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.model.Currency
import java.io.File
import java.io.FileWriter

object ExportHelper {

    fun generateTransactionsCsv(
        context: Context,
        transactions: List<TransactionEntity>,
        currency: Currency,
        title: String = "CashCount_Pro_Transactions"
    ): File? {
        return try {
            val fileName = "${title}_${DateUtils.formatForFileName()}.csv"
            val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(cacheDir, fileName)

            FileWriter(file).use { writer ->
                // Header block
                writer.append("CashCount Pro - Cash Denomination Transactions Report\n")
                writer.append("Generated Date,${DateUtils.formatFullDateTime(System.currentTimeMillis())}\n")
                writer.append("Total Records,${transactions.size}\n\n")

                // Column Headers
                writer.append("Txn Number,Date & Time,Location,Opening Cash (${currency.symbol}),Expected Cash (${currency.symbol}),Actual Cash (${currency.symbol}),Difference (${currency.symbol}),Status,Notes\n")

                for (tx in transactions) {
                    val dt = DateUtils.formatFullDateTime(tx.timestamp).replace(",", " ")
                    val notesClean = tx.notes.replace(",", ";").replace("\n", " ")
                    writer.append("${tx.transactionNumber},")
                    writer.append("$dt,")
                    writer.append("\"${tx.locationName}\",")
                    writer.append("${tx.openingCash},")
                    writer.append("${tx.expectedCash},")
                    writer.append("${tx.actualCash},")
                    writer.append("${tx.difference},")
                    writer.append("${tx.status},")
                    writer.append("\"$notesClean\"\n")
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateSingleReceiptCsv(
        context: Context,
        tx: TransactionEntity,
        items: List<TransactionItemEntity>,
        currency: Currency,
        settings: SettingsEntity?
    ): File? {
        return try {
            val fileName = "Receipt_${tx.transactionNumber}_${DateUtils.formatForFileName()}.csv"
            val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(cacheDir, fileName)

            FileWriter(file).use { writer ->
                writer.append("${settings?.businessName ?: "CashCount Pro"}\n")
                writer.append("Address,${settings?.address ?: ""}\n")
                writer.append("Phone,${settings?.phone ?: ""}\n")
                writer.append("GSTIN,${settings?.gstin ?: ""}\n\n")

                writer.append("Transaction ID,${tx.transactionNumber}\n")
                writer.append("Date & Time,${DateUtils.formatFullDateTime(tx.timestamp)}\n")
                writer.append("Location,${tx.locationName}\n\n")

                writer.append("Denomination,Type,Quantity,Amount (${currency.symbol})\n")
                for (item in items) {
                    val denomLabel = CurrencyFormatter.formatDenominationOnly(item.denominationValue, currency)
                    writer.append("$denomLabel,${item.denominationType},${item.quantity},${item.amount}\n")
                }

                writer.append("\nOpening Cash,${tx.openingCash}\n")
                writer.append("Expected Cash,${tx.expectedCash}\n")
                writer.append("Actual Cash,${tx.actualCash}\n")
                writer.append("Difference,${tx.difference} (${tx.status})\n")
                if (tx.notes.isNotBlank()) {
                    writer.append("Notes,\"${tx.notes.replace("\"", "\"\"")}\"\n")
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String = "text/csv", chooserTitle: String = "Export Report") {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun formatReceiptText(
        tx: TransactionEntity,
        items: List<TransactionItemEntity>,
        currency: Currency,
        settings: SettingsEntity?
    ): String {
        val sb = StringBuilder()
        sb.appendLine("================================")
        sb.appendLine(settings?.businessName ?: "CashCount Pro")
        if (!settings?.address.isNullOrBlank()) sb.appendLine(settings?.address)
        if (!settings?.phone.isNullOrBlank()) sb.appendLine("Tel: ${settings?.phone}")
        if (!settings?.gstin.isNullOrBlank()) sb.appendLine("GSTIN: ${settings?.gstin}")
        sb.appendLine("================================")
        sb.appendLine("CASH DENOMINATION STATEMENT")
        sb.appendLine("Txn No: ${tx.transactionNumber}")
        sb.appendLine("Date: ${DateUtils.formatFullDateTime(tx.timestamp)}")
        sb.appendLine("Counter: ${tx.locationName}")
        sb.appendLine("--------------------------------")
        sb.appendLine("DENOMINATION BREAKDOWN:")
        for (item in items) {
            if (item.quantity > 0) {
                val denomLabel = CurrencyFormatter.formatDenominationOnly(item.denominationValue, currency)
                val amtLabel = CurrencyFormatter.format(item.amount, currency)
                sb.appendLine("$denomLabel x ${item.quantity} = $amtLabel")
            }
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("TOTAL ACTUAL CASH: ${CurrencyFormatter.format(tx.actualCash, currency)}")
        sb.appendLine("Expected Cash:     ${CurrencyFormatter.format(tx.expectedCash, currency)}")
        val diffLabel = CurrencyFormatter.format(tx.difference, currency)
        sb.appendLine("Difference:        $diffLabel (${tx.status})")
        if (tx.notes.isNotBlank()) {
            sb.appendLine("Notes: ${tx.notes}")
        }
        sb.appendLine("================================")
        sb.appendLine(settings?.receiptFooter ?: "Certified Cash Statement")
        return sb.toString()
    }

    fun shareText(context: Context, text: String, title: String = "Share Statement") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Receipt Statement") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied statement to clipboard", Toast.LENGTH_SHORT).show()
    }
}
