package com.swordfish.lemuroid.app.shared.support

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import coil.load
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.support.SupportDesign.action
import com.swordfish.lemuroid.app.shared.support.SupportDesign.card
import com.swordfish.lemuroid.app.shared.support.SupportDesign.dp
import com.swordfish.lemuroid.app.shared.support.SupportDesign.header
import com.swordfish.lemuroid.app.shared.support.SupportDesign.label
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SupportActivity : ComponentActivity() {
    private val store by lazy { SupportReportStore(this) }
    private val screenshots by lazy { SupportScreenshot(this) }
    private val chooseScreenshot =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            val report = currentReport
            if (uri == null || report == null) return@registerForActivityResult
            lifecycleScope.launch {
                val digest =
                    withContext(Dispatchers.IO) {
                        runCatching { screenshots.importImage(report, uri) }.getOrNull()
                    }
                if (currentReport?.id != report.id) return@launch
                if (digest == null) {
                    message("Could not attach this image.")
                } else {
                    val updated = report.withScreenshot(digest)
                    if (attachmentFromEditor) showReport(updated) else showPreview(updated)
                }
            }
        }
    private var attachmentFromEditor = false
    private var navigateBack: () -> Unit = { finish() }
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
        onBackPressedDispatcher.addCallback(this) { navigateBack() }
        attachmentFromEditor = savedInstanceState?.getBoolean("attachmentFromEditor") ?: false
        exportFile = savedInstanceState?.getString("export")?.let(::File)
        val report =
            (savedInstanceState?.getString(EXTRA_REPORT) ?: intent.getStringExtra(EXTRA_REPORT))
                ?.let { runCatching { SupportReport.parse(it) }.getOrNull() }
        when {
            report == null -> showHome()
            savedInstanceState?.getBoolean("preview") == true -> showPreview(report)
            else -> showReport(report)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("attachmentFromEditor", attachmentFromEditor)
        outState.putBoolean("preview", preview)
        editorSnapshot?.invoke()?.let { currentReport = it }
        exportFile?.let { outState.putString("export", it.path) }
        currentReport?.let { outState.putString(EXTRA_REPORT, it.json) }
        super.onSaveInstanceState(outState)
    }

    private var currentReport: SupportReport? = null
    private var editorSnapshot: (() -> SupportReport?)? = null
    private var preview = false

    private fun showHome() {
        currentReport = null
        navigateBack = { finish() }
        val column = screen("Support", "Get help with a game or CARTDECK.")
        column.card().apply {
            action("Report an application problem", primary = true) {
                showReport(SupportReport.create("settings", "Application problem", null))
            }
            action("Report a game problem") {
                showReport(SupportReport.create("settings", "Game problem", null))
            }
            action("Previous reports") { showHistory() }
        }
        column.card().apply {
            label("Contact", 18, SupportDesign.cyan, bold = true)
            label(SupportTransport.EMAIL)
            action("Copy support email address") {
                getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText("CARTDECK support email", SupportTransport.EMAIL))
                message("Support email address copied.")
            }
        }
        privacy(column)
    }

    private fun showHistory(returnTo: () -> Unit = ::showHome) {
        currentReport = null
        navigateBack = returnTo
        val column = screen("Previous reports", "Reports saved on this device.")
        column.action("Back", onClick = returnTo)
        val reportsCard = column.card()
        lifecycleScope.launch {
            val reports = withContext(Dispatchers.IO) { runCatching { store.list() }.getOrDefault(emptyList()) }
            if (reports.isEmpty()) reportsCard.label("No saved reports.", color = SupportDesign.muted)
            reports.forEach { report ->
                val subject = report["game"].takeUnless { it == SupportReport.NOT_CAPTURED } ?: report["category"]
                reportsCard.action("$subject · ${report["createdAt"].take(10)}") { showPreview(report, saved = true) }
            }
        }
    }

    private fun showReport(report: SupportReport) {
        currentReport = report
        navigateBack = ::showHome
        val column = screen("Report a problem", "Review your report before you share it.")
        cartridge(column, report)
        val quick = column.card()
        quick.label(
            if (report["diagnostic"] == SupportReport.NOT_CAPTURED) {
                "App and device information ready"
            } else {
                "Engine diagnostics collected"
            },
            16,
            SupportDesign.cyan,
            bold = true,
        )
        quick.action("Copy diagnostic report") { copy(report) }

        val details = column.card()
        details.label("What went wrong?", 19, bold = true)
        val categories =
            listOf(
                "Crash or freeze" to R.drawable.ic_menu_restart,
                "Game will not start" to R.drawable.ic_menu_disk,
                "Slow performance" to R.drawable.ic_menu_fast_forward,
                "Graphics or display" to R.drawable.ic_menu_image,
                "Sound" to R.drawable.ic_menu_mute,
                "Controller or input" to R.drawable.ic_menu_controls,
                "Save or loading" to R.drawable.ic_menu_save,
                "Library or shelf" to R.drawable.ic_menu_load,
                "CARTMOD" to R.drawable.ic_menu_settings,
                "General application problem" to R.drawable.ic_menu,
                "Other" to R.drawable.ic_menu,
            )
        val options =
            (listOf(report["category"] to R.drawable.ic_menu) + categories)
                .let { if (categories.any { category -> category.first == report["category"] }) categories else it }
        val category = choices(details, options, report["category"])
        val description =
            field(details, "Describe what happened (optional)", report["description"]).apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                minLines = 4
                filters = arrayOf(android.text.InputFilter.LengthFilter(2_000))
            }
        details.label("How often does it happen?", 16, bold = true)
        val frequency =
            choices(
                details,
                listOf("Not sure", "Every time", "Sometimes", "Happened once").map { it to 0 },
                report["frequency"].takeUnless { it == SupportReport.NOT_CAPTURED } ?: "Not sure",
            )
        val reply =
            if (SupportTicketTransport.enabled) {
                field(details, "Reply email address (optional)", report["replyAddress"]).apply {
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                }
            } else {
                null
            }
        val snapshot = {
            val draft = report.withPlayerDetails(description.text.toString(), category(), frequency())
            if (reply == null) {
                draft
            } else {
                runCatching { draft.withReplyAddress(reply.text.toString().trim()) }.getOrElse {
                    reply.error = "Enter a valid reply email address."
                    null
                }
            }
        }
        editorSnapshot = snapshot
        val attachments = column.card()
        attachments.label("Screenshot", 16, bold = true)
        if (report.screenshotDigest == null) {
            attachments.action("Attach a screenshot") { snapshot()?.let { chooseAttachment(it, fromEditor = true) } }
        } else {
            attachments.label("Screenshot attached", color = SupportDesign.cyan)
            attachments.action("Remove screenshot") { snapshot()?.let { showReport(it.withScreenshot(null)) } }
        }
        column.action("Review report", primary = true) { snapshot()?.let { showPreview(it) } }
        column.action("Previous reports") { snapshot()?.let { draft -> showHistory { showReport(draft) } } }
        column.action("Support home") { showHome() }
        privacy(column)
    }

    private fun field(
        parent: LinearLayout,
        hint: String,
        value: String,
    ): EditText =
        EditText(this).also {
            it.hint = hint
            it.setText(value.takeUnless { text -> text == SupportReport.NOT_CAPTURED }.orEmpty())
            SupportDesign.input(it)
            parent.addView(
                it,
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = this@SupportActivity.dp(10) },
            )
        }

    private fun choices(
        parent: LinearLayout,
        options: List<Pair<String, Int>>,
        initial: String,
    ): () -> String {
        var selected = initial.takeIf { value -> options.any { it.first == value } } ?: options.first().first
        val buttons = mutableMapOf<String, android.widget.Button>()
        val columns = if (resources.configuration.screenWidthDp >= 360) 2 else 1
        for (group in options.chunked(columns)) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            parent.addView(row)
            for ((label, icon) in group) {
                val button =
                    android.widget.Button(this).apply {
                        text = label
                        isSelected = label == selected
                        SupportDesign.style(this)
                        if (icon != 0) {
                            setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
                            compoundDrawablePadding = this@SupportActivity.dp(8)
                            compoundDrawableTintList = android.content.res.ColorStateList.valueOf(SupportDesign.cyan)
                        }
                        setOnClickListener {
                            buttons[selected]?.isSelected = false
                            selected = label
                            isSelected = true
                        }
                    }
                buttons[label] = button
                row.addView(
                    button,
                    LinearLayout.LayoutParams(0, -1, 1f).apply {
                        marginEnd = this@SupportActivity.dp(4)
                        bottomMargin = this@SupportActivity.dp(5)
                    },
                )
            }
        }
        return { selected }
    }

    private fun cartridge(
        column: LinearLayout,
        report: SupportReport,
    ) {
        val game = report["game"].takeUnless { it == SupportReport.NOT_CAPTURED } ?: return
        val card = column.card()
        val artwork = intent.getStringExtra(EXTRA_ARTWORK)
        if (artwork != null) {
            card.addView(
                android.widget.ImageView(this).apply {
                    contentDescription = "$game cover"
                    scaleType = android.widget.ImageView.ScaleType.FIT_START
                    load(artwork)
                },
                LinearLayout.LayoutParams(dp(88), dp(110)),
            )
        }
        card.label(game, 20, bold = true)
        val metadata = listOf(report["console"], report["engine"]).filterNot { it == SupportReport.NOT_CAPTURED }
        if (metadata.isNotEmpty()) card.label(metadata.joinToString(" · "), color = SupportDesign.muted)
    }

    private fun privacy(column: LinearLayout) {
        column.card().apply {
            label("Privacy", 16, SupportDesign.cyan, bold = true)
            label(
                "Review your diagnostics and image before sharing. Content stays on this device until you send it.",
            )
        }
    }

    private fun chooseAttachment(
        report: SupportReport,
        fromEditor: Boolean,
    ) {
        currentReport = report
        attachmentFromEditor = fromEditor
        AlertDialog.Builder(this)
            .setTitle("Attach a screenshot")
            .setMessage("Review your image for personal information before sharing.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Choose image") { _, _ -> chooseScreenshot.launch("image/*") }
            .show().also(SupportDesign::style)
    }

    private fun showPreview(
        report: SupportReport,
        saved: Boolean = false,
    ) {
        currentReport = report
        val column = screen(if (saved) "Report details" else "Review report", "Diagnostic report")
        preview = true
        navigateBack = if (saved) ({ showHistory() }) else ({ showReport(report) })
        column.action("Back", onClick = navigateBack)
        cartridge(column, report)
        column.action("Copy diagnostic report") { copy(report) }
        if (report.screenshotDigest == null) {
            column.action("Attach a screenshot") {
                chooseAttachment(report, fromEditor = false)
            }
        } else {
            column.action("Remove screenshot from report") { showPreview(report.withScreenshot(null)) }
            val image =
                android.widget.ImageView(this).apply {
                    adjustViewBounds = true
                    maxHeight = (240 * resources.displayMetrics.density).toInt()
                    contentDescription = "Selected screenshot"
                }
            column.addView(image)
            lifecycleScope.launch {
                val preview =
                    withContext(Dispatchers.IO) {
                        screenshots.attachment(report)?.let { android.graphics.BitmapFactory.decodeFile(it.path) }
                    }
                if (preview != null) image.setImageBitmap(preview)
            }
        }
        column.action("Save report on this device") {
            runIo("Could not save the report.") { store.save(report) }
        }
        if (SupportTicketTransport.enabled) {
            column.action("Submit support ticket") {
                AlertDialog.Builder(this)
                    .setTitle("Submit this diagnostic report?")
                    .setMessage("This sends the reviewed report and its selected attachments to CARTDECK Support.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Submit") { _, _ -> submitTicket(report) }
                    .show().also(SupportDesign::style)
            }
        }
        column.card().apply {
            label("Email preparation", 18, SupportDesign.cyan, bold = true)
            label("To ${SupportTransport.EMAIL}", color = SupportDesign.muted)
            action("Send by email", primary = !SupportTicketTransport.enabled) {
                packageFor(report) { SupportTransport.share(this@SupportActivity, report, it, email = true) }
            }
        }
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
                }.show().also(SupportDesign::style)
        }
        column.action("Edit report") { showReport(report) }
        column.action("Previous reports") { showHistory() }
        column.card().apply {
            label("Advanced diagnostics", 18, SupportDesign.cyan, bold = true)
            label(report.text, 13).setTextIsSelectable(true)
        }
        privacy(column)
    }

    private fun submitTicket(report: SupportReport) {
        lifecycleScope.launch {
            val reference =
                withContext(Dispatchers.IO) {
                    runCatching {
                        store.save(report)
                        SupportTicketTransport.send(report, store.packageReport(report))
                    }.getOrNull()
                }
            if (reference == null) {
                message("Submission could not be confirmed. Retry, email, copy or export the report.")
            } else {
                AlertDialog.Builder(this@SupportActivity)
                    .setTitle("Report submitted.")
                    .setMessage("Support reference $reference")
                    .setPositiveButton("Close", null)
                    .show().also(SupportDesign::style)
            }
        }
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

    private fun screen(
        title: String,
        subtitle: String? = null,
    ): LinearLayout {
        editorSnapshot = null
        preview = false
        return SupportDesign.column(this).also {
            it.header(title, subtitle)
            setContentView(SupportDesign.frame(this, it))
        }
    }

    companion object {
        private const val EXTRA_REPORT = "support.report"
        private const val EXTRA_ARTWORK = "support.artwork"

        fun intent(
            context: Context,
            report: SupportReport? = null,
            game: Game? = null,
        ): Intent =
            Intent(context, SupportActivity::class.java).apply {
                report?.let { putExtra(EXTRA_REPORT, it.json) }
                game?.displayCoverUrl()?.let { putExtra(EXTRA_ARTWORK, it) }
            }
    }
}
