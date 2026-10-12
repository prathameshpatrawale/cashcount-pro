package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.local.entity.SettingsEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.model.Currency

object ReceiptPrinter {

    fun printTransactionReceipt(
        context: Context,
        tx: TransactionEntity,
        items: List<TransactionItemEntity>,
        currency: Currency,
        settings: SettingsEntity?
    ) {
        val htmlContent = generateReceiptHtml(tx, items, currency, settings)

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager != null) {
                    val jobName = "CashCount_Receipt_${tx.transactionNumber}"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    val attributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("res1", "default", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                    printManager.print(jobName, printAdapter, attributes)
                }
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun generateReceiptHtml(
        tx: TransactionEntity,
        items: List<TransactionItemEntity>,
        currency: Currency,
        settings: SettingsEntity?
    ): String {
        val bizName = settings?.businessName ?: "CashCount Pro"
        val address = settings?.address ?: ""
        val phone = settings?.phone ?: ""
        val gstin = settings?.gstin ?: ""
        val footer = settings?.receiptFooter ?: "Certified Cash Statement"

        val rows = StringBuilder()
        for (item in items) {
            if (item.quantity > 0) {
                val denomStr = CurrencyFormatter.formatDenominationOnly(item.denominationValue, currency)
                val amtStr = CurrencyFormatter.format(item.amount, currency)
                rows.append("""
                    <tr>
                        <td style="padding: 6px 8px; border-bottom: 1px solid #e2e8f0; font-weight: bold;">$denomStr</td>
                        <td style="padding: 6px 8px; border-bottom: 1px solid #e2e8f0; color: #64748b; font-size: 11px;">${item.denominationType}</td>
                        <td style="padding: 6px 8px; border-bottom: 1px solid #e2e8f0; text-align: center;">${item.quantity}</td>
                        <td style="padding: 6px 8px; border-bottom: 1px solid #e2e8f0; text-align: right; font-weight: bold;">$amtStr</td>
                    </tr>
                """.trimIndent())
            }
        }

        val diffClass = when (tx.status) {
            "EXCESS" -> "color: #16a34a;"
            "SHORTAGE" -> "color: #dc2626;"
            else -> "color: #0284c7;"
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Receipt ${tx.transactionNumber}</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        color: #1e293b;
                        margin: 0;
                        padding: 24px;
                        background: #ffffff;
                    }
                    .receipt-card {
                        max-width: 480px;
                        margin: 0 auto;
                        border: 1px solid #cbd5e1;
                        border-radius: 8px;
                        padding: 24px;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                    }
                    .header {
                        text-align: center;
                        border-bottom: 2px dashed #94a3b8;
                        padding-bottom: 16px;
                        margin-bottom: 16px;
                    }
                    .biz-title {
                        font-size: 20px;
                        font-weight: 800;
                        color: #0f172a;
                        margin: 0 0 4px 0;
                    }
                    .biz-sub {
                        font-size: 12px;
                        color: #64748b;
                        margin: 2px 0;
                    }
                    .tag {
                        display: inline-block;
                        padding: 2px 8px;
                        border-radius: 4px;
                        font-size: 11px;
                        font-weight: bold;
                        text-transform: uppercase;
                        margin-top: 8px;
                    }
                    .tag-balanced { background: #e0f2fe; color: #0284c7; }
                    .tag-excess { background: #dcfce7; color: #16a34a; }
                    .tag-shortage { background: #fee2e2; color: #dc2626; }
                    .info-grid {
                        display: grid;
                        grid-template-columns: 1fr 1fr;
                        gap: 8px;
                        font-size: 12px;
                        margin-bottom: 16px;
                    }
                    .info-grid div { margin-bottom: 4px; }
                    .info-label { color: #64748b; }
                    .info-val { font-weight: 600; }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        font-size: 13px;
                        margin-bottom: 16px;
                    }
                    th {
                        background: #f8fafc;
                        padding: 8px;
                        text-align: left;
                        border-bottom: 2px solid #cbd5e1;
                        font-size: 11px;
                        color: #475569;
                        text-transform: uppercase;
                    }
                    .total-box {
                        border-top: 2px dashed #94a3b8;
                        padding-top: 12px;
                        margin-top: 8px;
                    }
                    .total-row {
                        display: flex;
                        justify-content: space-between;
                        padding: 4px 0;
                        font-size: 13px;
                    }
                    .grand-total {
                        font-size: 18px;
                        font-weight: 800;
                        color: #0f172a;
                        border-top: 1px solid #e2e8f0;
                        border-bottom: 1px solid #e2e8f0;
                        padding: 8px 0;
                        margin: 8px 0;
                    }
                    .footer {
                        text-align: center;
                        font-size: 11px;
                        color: #94a3b8;
                        margin-top: 24px;
                        border-top: 1px solid #e2e8f0;
                        padding-top: 12px;
                    }
                    .signature-box {
                        margin-top: 32px;
                        display: flex;
                        justify-content: space-between;
                        font-size: 12px;
                        color: #64748b;
                    }
                    .signature-line {
                        border-top: 1px solid #94a3b8;
                        width: 140px;
                        text-align: center;
                        padding-top: 4px;
                    }
                </style>
            </head>
            <body>
                <div class="receipt-card">
                    <div class="header">
                        <div class="biz-title">$bizName</div>
                        ${if (address.isNotBlank()) "<div class=\"biz-sub\">$address</div>" else ""}
                        ${if (phone.isNotBlank()) "<div class=\"biz-sub\">Tel: $phone</div>" else ""}
                        ${if (gstin.isNotBlank()) "<div class=\"biz-sub\">GSTIN: $gstin</div>" else ""}
                        <div class="tag ${when (tx.status) { "EXCESS" -> "tag-excess"; "SHORTAGE" -> "tag-shortage"; else -> "tag-balanced" }}">${tx.status}</div>
                    </div>

                    <div class="info-grid">
                        <div><span class="info-label">Statement No:</span><br><span class="info-val">${tx.transactionNumber}</span></div>
                        <div><span class="info-label">Date & Time:</span><br><span class="info-val">${DateUtils.formatFullDateTime(tx.timestamp)}</span></div>
                        <div><span class="info-label">Register/Till:</span><br><span class="info-val">${tx.locationName}</span></div>
                    </div>

                    <table>
                        <thead>
                            <tr>
                                <th>Denom</th>
                                <th>Type</th>
                                <th style="text-align: center;">Qty</th>
                                <th style="text-align: right;">Amount</th>
                            </tr>
                        </thead>
                        <tbody>
                            $rows
                        </tbody>
                    </table>

                    <div class="total-box">
                        <div class="total-row">
                            <span>Opening Cash:</span>
                            <span>${CurrencyFormatter.format(tx.openingCash, currency)}</span>
                        </div>
                        <div class="total-row">
                            <span>Expected Cash:</span>
                            <span>${CurrencyFormatter.format(tx.expectedCash, currency)}</span>
                        </div>
                        <div class="total-row grand-total">
                            <span>ACTUAL CASH COUNTED:</span>
                            <span>${CurrencyFormatter.format(tx.actualCash, currency)}</span>
                        </div>
                        <div class="total-row" style="$diffClass font-weight: bold; font-size: 14px;">
                            <span>Difference (${tx.status}):</span>
                            <span>${CurrencyFormatter.format(tx.difference, currency)}</span>
                        </div>
                    </div>

                    ${if (tx.notes.isNotBlank()) """
                        <div style="margin-top: 12px; font-size: 12px; background: #f8fafc; padding: 8px; border-radius: 4px; border-left: 3px solid #cbd5e1;">
                            <strong>Remarks:</strong> ${tx.notes}
                        </div>
                    """.trimIndent() else ""}

                    <div class="signature-box">
                        <div class="signature-line">Cashier Signature</div>
                        <div class="signature-line">Manager Verified</div>
                    </div>

                    <div class="footer">
                        $footer<br>
                        Generated on ${DateUtils.formatFullDateTime(System.currentTimeMillis())}
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
