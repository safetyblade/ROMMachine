package com.swordfish.lemuroid.app.shared.support

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class SupportActivity : ComponentActivity() {
    private val store by lazy { SupportReportStore(this) }
    private val submissions by lazy { SupportSubmissionStore(this) }
    private val screenshots by lazy { SupportScreenshot(this) }
    private var connectivityCallback: ConnectivityManager.NetworkCallback? = null
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
    private var pendingExport: SupportReport? = null
    private val exportZip =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) {
            finishExport(it, "zip")
        }
    private val exportJson =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
            finishExport(it, "json")
        }
    private val exportText =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) {
            finishExport(it, "txt")
        }

    private fun startExport(
        report: SupportReport,
        name: String,
        launcher: androidx.activity.result.ActivityResultLauncher<String>,
    ) {
        if (pendingExport != null) return
        pendingExport = report
        runCatching { launcher.launch(name) }.onFailure {
            pendingExport = null
            message("Could not open device storage. You can still copy or share the report.")
        }
    }

    private fun finishExport(
        uri: android.net.Uri?,
        extension: String,
    ) {
        val report = pendingExport
        pendingExport = null
        if (uri == null || report == null) return
        runIo("Report exported to the selected location.", "Could not export the selected report.") {
            checkNotNull(contentResolver.openOutputStream(uri, "wt")).use { output ->
                when (extension) {
                    "json" -> output.write(report.json.toByteArray())
                    "txt" -> output.write(report.text.toByteArray())
                    else -> store.packageReport(report).inputStream().use { it.copyTo(output) }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat
            .setDecorFitsSystemWindows(window, false)
        onBackPressedDispatcher.addCallback(this) { navigateBack() }
        attachmentFromEditor = savedInstanceState?.getBoolean("attachmentFromEditor") ?: false
        pendingExport =
            savedInstanceState?.getString("export")?.let { runCatching { SupportReport.parse(it) }.getOrNull() }
        val report =
            (savedInstanceState?.getString(EXTRA_REPORT) ?: intent.getStringExtra(EXTRA_REPORT))
                ?.let { runCatching { SupportReport.parse(it) }.getOrNull() }
        when {
            report == null -> showHome()
            savedInstanceState?.getBoolean("preview") == true -> showPreview(report)
            else -> showReport(report)
        }
        registerConnectivityRetry()
        retryPendingSubmissions()
    }

    override fun onDestroy() {
        connectivityCallback?.let { callback ->
            runCatching {
                getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(callback)
            }
        }
        connectivityCallback = null
        super.onDestroy()
    }

    private fun hasActiveNetwork(): Boolean {
        val manager = getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork ?: return false
        return manager
            .getNetworkCapabilities(network)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    /** Silent retry for reports the player already explicitly submitted (ticket §8: never auto-send a fresh report). */
    private fun registerConnectivityRetry() {
        val manager = getSystemService(ConnectivityManager::class.java)
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    retryPendingSubmissions()
                }
            }
        connectivityCallback = callback
        runCatching { manager.registerDefaultNetworkCallback(callback) }
    }

    private fun retryPendingSubmissions() {
        if (!SupportTicketTransport.enabled || !hasActiveNetwork()) return
        lifecycleScope.launch {
            val pending =
                withContext(Dispatchers.IO) {
                    runCatching {
                        store.list().filter { report ->
                            submissions.load(report.id).state in
                                setOf(
                                    SupportSubmissionState.PENDING_SUBMISSION,
                                    SupportSubmissionState.SUBMISSION_FAILED,
                                )
                        }
                    }.getOrDefault(emptyList())
                }
            pending.forEach { attemptSubmission(it, silent = true) }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("attachmentFromEditor", attachmentFromEditor)
        outState.putBoolean("preview", preview)
        editorSnapshot?.invoke()?.let { currentReport = it }
        pendingExport?.let { outState.putString("export", it.json) }
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
            val reports = withContext(Dispatchers.IO) { runCatching { store.list() }.getOrNull() }
            if (reports == null) {
                reportsCard.label("Could not read saved reports.", color = SupportDesign.muted)
                return@launch
            }
            if (reports.isEmpty()) reportsCard.label("No saved reports.", color = SupportDesign.muted)
            reports.forEach { report ->
                val subject = report["game"].takeUnless { it == SupportReport.NOT_CAPTURED } ?: report["category"]
                val submission =
                    withContext(Dispatchers.IO) { runCatching { submissions.load(report.id) }.getOrNull() }
                val stateSuffix =
                    when {
                        submission == null -> ""
                        submission.state == SupportSubmissionState.SUBMITTED && submission.reference != null ->
                            " · ${submission.state.label} · ${submission.reference}"
                        else -> " · ${submission.state.label}"
                    }
                val identity = "${report["createdAt"]} · ${report["versionCode"]} · ${report.id.take(8)}"
                reportsCard.action("$subject · $identity$stateSuffix") {
                    showPreview(report, saved = true)
                }
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
        // C04-B: one-tap diagnostic copying is first-class on the editor
        // itself, not only behind Review — it copies the current snapshot,
        // including the player's edits, exactly as preview/export would.
        column.action("Copy diagnostic report") { snapshot()?.let { copy(it) } }
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
                            val size = this@SupportActivity.dp(18)
                            val drawable = context.getDrawable(icon)?.mutate()
                            drawable?.setBounds(0, 0, size, size)
                            setCompoundDrawablesRelative(drawable, null, null, null)
                            compoundDrawablePadding = this@SupportActivity.dp(8)
                            compoundDrawableTintList =
                                android.content.res.ColorStateList
                                    .valueOf(SupportDesign.cyan)
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
        val artwork =
            intent
                .getStringExtra(EXTRA_ARTWORK)
                ?.takeIf { intent.getStringExtra(EXTRA_ARTWORK_REPORT) == report.id }
        card.addView(
            android.widget.ImageView(this).apply {
                contentDescription = "$game artwork"
                scaleType = android.widget.ImageView.ScaleType.FIT_START
                load(artwork) {
                    fallback(R.drawable.cartdeck_pink_front_base)
                    error(R.drawable.cartdeck_pink_front_base)
                }
            },
            LinearLayout.LayoutParams(dp(74), dp(90)),
        )
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
        AlertDialog
            .Builder(this)
            .setTitle("Attach a screenshot")
            .setMessage("Review your image for personal information before sharing.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Choose image") { _, _ -> chooseScreenshot.launch("image/*") }
            .show()
            .also(SupportDesign::style)
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
        val draft = report.id !in activeSubmissions && submissions.load(report.id).state == SupportSubmissionState.DRAFT
        if (report.screenshotDigest == null) {
            if (draft) {
                column.action("Attach a screenshot") {
                    chooseAttachment(report, fromEditor = false)
                }
            }
        } else {
            if (draft) column.action("Remove screenshot from report") { showPreview(report.withScreenshot(null)) }
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
        column.action("Save report in CARTDECK") {
            runIo("Report saved in CARTDECK. Find it in Previous reports.", "Could not save the report.") {
                store.save(report)
            }
        }
        column.action("Copy diagnostic report") { copy(report) }
        column.action("Export diagnostic package") {
            startExport(report, report.packageName, exportZip)
        }
        if (SupportTicketTransport.enabled) {
            val stored = submissions.load(report.id)
            val submission =
                if (report.id in activeSubmissions && stored.state != SupportSubmissionState.SUBMITTED) {
                    stored.copy(state = SupportSubmissionState.SUBMITTING)
                } else {
                    stored
                }
            column.card().apply {
                label("Submission", 18, SupportDesign.cyan, bold = true)
                label("Status: ${submission.state.label}")
                if (submission.state == SupportSubmissionState.SUBMITTED && submission.reference != null) {
                    label("Support reference ${submission.reference}", color = SupportDesign.cyan)
                }
                submission.lastError
                    ?.takeIf { submission.state == SupportSubmissionState.SUBMISSION_FAILED }
                    ?.let { label(it, color = SupportDesign.muted) }
            }
            when (submission.state) {
                SupportSubmissionState.SUBMITTING ->
                    column.action("Submitting…") {}.apply { isEnabled = false }
                SupportSubmissionState.SUBMITTED -> Unit
                SupportSubmissionState.PENDING_SUBMISSION, SupportSubmissionState.SUBMISSION_FAILED ->
                    column.action("Retry submission", primary = true) { confirmSubmit(report) }
                else -> column.action("Submit support ticket", primary = true) { confirmSubmit(report) }
            }
        }
        if (report.id !in activeSubmissions) {
            column.action("Delete local report") {
                AlertDialog
                    .Builder(this)
                    .setTitle("Delete this local report?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch {
                            val deleted =
                                withContext(Dispatchers.IO) { runCatching { store.delete(report.id) }.isSuccess }
                            if (deleted) showHome() else message("Could not delete the local report.")
                        }
                    }.show()
                    .also(SupportDesign::style)
            }
        }
        column.action("Edit report") { editReport(report) }
        column.action("Previous reports") { showHistory() }
        val advanced = column.card()
        advanced.label("Advanced diagnostics", 18, SupportDesign.cyan, bold = true)
        var advancedExpanded = false
        lateinit var advancedToggle: android.widget.Button
        advancedToggle =
            advanced.action("Show advanced diagnostics") {
                advancedExpanded = !advancedExpanded
                if (advancedExpanded) {
                    advancedToggle.text = "Hide advanced diagnostics"
                    advanced.action("Export JSON report") {
                        startExport(report, "CARTDECK-CD-${report.id}.json", exportJson)
                    }
                    advanced.action("Export text report") {
                        startExport(report, "CARTDECK-CD-${report.id}.txt", exportText)
                    }
                    advanced.action("Share diagnostic package") {
                        packageFor(report) { SupportTransport.share(this@SupportActivity, report, it, email = false) }
                    }
                    advanced.action("Send by email") {
                        packageFor(report) { SupportTransport.share(this@SupportActivity, report, it, email = true) }
                    }
                    advanced.action("Open text-only email draft") {
                        runCatching { SupportTransport.textEmail(this@SupportActivity, report) }
                            .onFailure {
                                message("No compatible email app. You can copy, export or share the report.")
                            }
                    }
                    advanced.label(report.text, 13).setTextIsSelectable(true)
                } else {
                    showPreview(report, saved)
                }
            }
        privacy(column)
    }

    private fun confirmSubmit(report: SupportReport) {
        AlertDialog
            .Builder(this)
            .setTitle("Submit this diagnostic report?")
            .setMessage("This sends the reviewed report and its selected attachments to CARTDECK Support.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Submit") { _, _ -> submitTicket(report) }
            .show()
            .also(SupportDesign::style)
    }

    /** Editing a submitted report forks a new draft; the submitted report stays on record untouched. */
    private fun editReport(report: SupportReport) {
        if (report.id !in activeSubmissions && submissions.load(report.id).state == SupportSubmissionState.DRAFT) {
            showReport(report)
            return
        }
        lifecycleScope.launch {
            val fork =
                withContext(Dispatchers.IO) {
                    runCatching {
                        val now = System.currentTimeMillis()
                        val createdAt =
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT)
                                .apply {
                                    timeZone = TimeZone.getTimeZone("UTC")
                                }.format(Date(now))
                        val forked =
                            SupportReport.parse(
                                SupportSubmission.forkJson(report.json, UUID.randomUUID().toString(), now, createdAt),
                            )
                        report.screenshotDigest?.let { digest ->
                            screenshots.copyFor(report.id, digest, forked.id)
                        }
                        store.save(forked)
                        forked
                    }.getOrNull()
                }
            if (fork == null) {
                message("Could not start a new draft from this report.")
            } else {
                message("Editing a copy. The original report stays on record.")
                showReport(fork)
            }
        }
    }

    private fun submitTicket(report: SupportReport) {
        if (!hasActiveNetwork()) {
            lifecycleScope.launch {
                val queued =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            store.save(report)
                            submissions.transition(report.id, SupportSubmissionState.PENDING_SUBMISSION)
                        }.isSuccess
                    }
                if (queued) {
                    message("Report saved in CARTDECK and queued for submission.")
                    if (currentReport?.id == report.id) showPreview(report, saved = true)
                } else {
                    message("Could not save or queue the report. You can still copy or export it.")
                }
            }
            return
        }
        attemptSubmission(report, silent = false)
    }

    private fun attemptSubmission(
        report: SupportReport,
        silent: Boolean,
    ) {
        lifecycleScope.launch {
            if (!activeSubmissions.add(report.id)) return@launch
            var savedLocally = false
            try {
                val state = withContext(Dispatchers.IO) { submissions.load(report.id).state }
                if (state == SupportSubmissionState.SUBMITTED ||
                    (silent && state == SupportSubmissionState.DRAFT)
                ) {
                    return@launch
                }
                if (!silent && currentReport?.id == report.id) showPreview(report)
                savedLocally = withContext(Dispatchers.IO) { runCatching { store.save(report) }.isSuccess }
                if (!savedLocally) {
                    if (!silent) message("Could not save the report. You can still copy or export it.")
                    return@launch
                }
                val outcome =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            submissions.transition(report.id, SupportSubmissionState.SUBMITTING)
                            SupportTicketTransport.send(report, store.packageReport(report))
                        }
                    }
                outcome.fold(
                    onSuccess = { reference ->
                        // SUBMITTED is only ever set from a receipt the transport has already validated.
                        submissions.transition(
                            report.id,
                            SupportSubmissionState.SUBMITTED,
                            reference = reference,
                        )
                        if (silent) {
                            message("Report submitted. Support reference $reference")
                        } else {
                            AlertDialog
                                .Builder(this@SupportActivity)
                                .setTitle("Report submitted.")
                                .setMessage("Support reference $reference")
                                .setPositiveButton("Close", null)
                                .show()
                                .also(SupportDesign::style)
                        }
                    },
                    onFailure = { error ->
                        submissions.transition(
                            report.id,
                            SupportSubmissionState.SUBMISSION_FAILED,
                            lastError =
                                when (error) {
                                    is java.net.UnknownHostException -> "Could not resolve the support server."
                                    is javax.net.ssl.SSLException ->
                                        "Could not establish a secure connection to support."
                                    is java.net.SocketTimeoutException -> "The support server did not respond in time."
                                    else -> "Submission could not be confirmed."
                                },
                        )
                        if (!silent) {
                            message("Submission could not be confirmed. Retry, email, copy or export the report.")
                        }
                    },
                )
            } finally {
                activeSubmissions.remove(report.id)
                if (!silent && currentReport?.id == report.id && preview) showPreview(report, saved = savedLocally)
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
        success: String,
        error: String,
        action: () -> Unit,
    ) {
        lifecycleScope.launch {
            val completed = withContext(Dispatchers.IO) { runCatching(action).isSuccess }
            message(if (completed) success else error)
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
        private val activeSubmissions =
            java.util.concurrent.ConcurrentHashMap
                .newKeySet<String>()
        private const val EXTRA_REPORT = "support.report"
        private const val EXTRA_ARTWORK = "support.artwork"
        private const val EXTRA_ARTWORK_REPORT = "support.artworkReport"

        fun intent(
            context: Context,
            report: SupportReport? = null,
            game: Game? = null,
        ): Intent =
            Intent(context, SupportActivity::class.java).apply {
                report?.let { putExtra(EXTRA_REPORT, it.json) }
                game?.displayCoverUrl()?.let {
                    putExtra(EXTRA_ARTWORK, it)
                    putExtra(EXTRA_ARTWORK_REPORT, report?.id)
                }
            }
    }
}
