package com.swordfish.lemuroid.app.shared.support

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SupportActivity : ComponentActivity() {
    private val store by lazy { SupportReportStore(this) }
    private var exportFile: File? = null
    private val createDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            val file = exportFile
            exportFile = null
            if (uri == null || file == null) return@registerForActivityResult
            runIo("Could not export the diagnostic package.") {
                checkNotNull(contentResolver.openOutputStream(uri)).use { output ->
                    file.inputStream().use { it.copyTo(output) }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exportFile = savedInstanceState?.getString("export")?.let(::File)
        val report =
            (savedInstanceState?.getString(EXTRA_REPORT) ?: intent.getStringExtra(EXTRA_REPORT))
                ?.let { runCatching { SupportReport.parse(it) }.getOrNull() }
        if (report != null) showReport(report) else showHome()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        exportFile?.let { outState.putString("export", it.path) }
        currentReport?.let { outState.putString(EXTRA_REPORT, it.json) }
        super.onSaveInstanceState(outState)
    }

    private var currentReport: SupportReport? = null

    private fun showHome() {
        currentReport = null
        val column = screen("CARTDECK Support")
        column.action("Report an application problem") {
            showReport(SupportReport.create("settings", "Application problem", null))
        }
        column.action("Report a game problem") {
            showReport(SupportReport.create("settings", "Game problem", null))
        }
        column.action("Copy support email address") {
            getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText("CARTDECK support email", SupportTransport.EMAIL))
            message("Support email address copied.")
        }
        column.addView(TextView(this).apply { text = "Previous reports" })
        lifecycleScope.launch {
            val reports = withContext(Dispatchers.IO) { runCatching { store.list() }.getOrDefault(emptyList()) }
            if (currentReport != null) return@launch
            if (reports.isEmpty()) column.addView(TextView(this@SupportActivity).apply { text = "No saved reports." })
            reports.forEach { report ->
                val subject = report["game"].takeUnless { it == SupportReport.NOT_CAPTURED } ?: report["category"]
                column.action("$subject · ${report["createdAt"].take(10)}") { showReport(report) }
            }
        }
    }

    private fun showReport(report: SupportReport) {
        currentReport = report
        val column = screen("Report a problem")
        val description =
            EditText(this).apply {
                hint = "Tell us what happened (optional)"
                setText(report["description"].takeUnless { it == SupportReport.NOT_CAPTURED }.orEmpty())
                filters = arrayOf(android.text.InputFilter.LengthFilter(2_000))
            }
        column.addView(description)
        val category =
            android.widget.Spinner(this).apply {
                adapter =
                    choices(
                        listOf(
                            report["category"], "Crash or freeze", "Game will not start", "Slow performance",
                            "Graphics or display", "Sound", "Controller or input", "Save or loading",
                            "Library or shelf",
                            "CARTMOD", "General application problem", "Other",
                        ).distinct(),
                    )
            }
        val frequency =
            android.widget.Spinner(this).apply {
                adapter = choices(listOf("Not sure", "Every time", "Sometimes", "Happened once"))
                setSelection((0 until count).firstOrNull { getItemAtPosition(it) == report["frequency"] } ?: 0)
            }
        column.addView(TextView(this).apply { text = "What went wrong?" })
        column.addView(category)
        column.addView(TextView(this).apply { text = "How often does it happen?" })
        column.addView(frequency)
        column.action("Review report") {
            val snapshot =
                report.withPlayerDetails(
                    description.text.toString(),
                    category.selectedItem.toString(),
                    frequency.selectedItem.toString(),
                )
            showPreview(snapshot)
        }
        column.action("Copy diagnostic report") { copy(report) }
        column.action("Previous reports") { showHome() }
    }

    private fun showPreview(report: SupportReport) {
        currentReport = report
        val column = screen("Diagnostic report")
        column.action("Copy diagnostic report") { copy(report) }
        column.action("Save report on this device") {
            runIo("Could not save the report.") { store.save(report) }
        }
        column.action("Send by email") { packageFor(report) { SupportTransport.share(this, report, it, email = true) } }
        column.action("Open text-only email draft") {
            runCatching { SupportTransport.textEmail(this, report) }
                .onFailure { message("No compatible email app. You can copy, export or share the report.") }
        }
        column.action("Export diagnostic package") {
            packageFor(report) {
                exportFile = it
                createDocument.launch(report.packageName)
            }
        }
        column.action("Share diagnostic package") {
            packageFor(report) { SupportTransport.share(this, report, it, email = false) }
        }
        column.action("Delete local report") {
            AlertDialog.Builder(this).setTitle("Delete this local report?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        val deleted = withContext(Dispatchers.IO) { runCatching { store.delete(report.id) }.isSuccess }
                        if (deleted) showHome() else message("Could not delete the local report.")
                    }
                }.show()
        }
        column.action("Edit report") { showReport(report) }
        column.addView(
            TextView(this).apply {
                text = report.text
                setTextIsSelectable(true)
            },
        )
    }

    private fun packageFor(
        report: SupportReport,
        action: (File) -> Unit,
    ) {
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) { runCatching { store.packageReport(report) }.getOrNull() }
            if (file == null) {
                message("Could not create the diagnostic package. You can still copy the report.")
            } else {
                runCatching { action(file) }.onFailure {
                    message("No compatible app opened. You can still copy or export the report.")
                }
            }
        }
    }

    private fun runIo(
        error: String,
        action: () -> Unit,
    ) {
        lifecycleScope.launch {
            val completed = withContext(Dispatchers.IO) { runCatching(action).isSuccess }
            message(if (completed) "Report saved." else error)
        }
    }

    private fun copy(report: SupportReport) {
        runCatching { SupportTransport.copy(this, report) }
            .onSuccess { message("Diagnostic report copied.") }
            .onFailure { message("Could not copy the diagnostic report.") }
    }

    private fun message(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    private fun choices(items: List<String>) =
        android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)

    private fun screen(title: String): LinearLayout {
        val inset = (24 * resources.displayMetrics.density).toInt()
        val column =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(inset, inset, inset, inset)
                addView(
                    TextView(context).apply {
                        text = title
                        textSize = 24f
                    },
                )
            }
        setContentView(ScrollView(this).apply { addView(column) })
        return column
    }

    private fun LinearLayout.action(
        label: String,
        onClick: () -> Unit,
    ) {
        addView(
            Button(context).apply {
                text = label
                setOnClickListener { onClick() }
            },
        )
    }

    companion object {
        private const val EXTRA_REPORT = "support.report"

        fun intent(
            context: Context,
            report: SupportReport? = null,
        ): Intent =
            Intent(context, SupportActivity::class.java).apply { report?.let { putExtra(EXTRA_REPORT, it.json) } }
    }
}
