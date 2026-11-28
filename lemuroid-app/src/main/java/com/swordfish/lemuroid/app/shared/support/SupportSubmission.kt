package com.swordfish.lemuroid.app.shared.support

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Submission lifecycle for one report. SUBMITTED is only ever reached from a validated server receipt. */
enum class SupportSubmissionState(val label: String) {
    DRAFT("Draft"),
    PENDING_SUBMISSION("Pending submission"),
    SUBMITTING("Submitting"),
    SUBMITTED("Submitted"),
    SUBMISSION_FAILED("Submission failed"),
}

/** Persistent per-report submission record, stored next to the report it describes. */
data class SupportSubmission(
    val reportId: String,
    val state: SupportSubmissionState,
    val reference: String? = null,
    val attempts: Int = 0,
    val updatedAtMillis: Long = 0L,
    val lastError: String? = null,
) {
    fun toJson(): String =
        JSONObject()
            .put("reportId", reportId)
            .put("state", state.name)
            .put("reference", reference ?: JSONObject.NULL)
            .put("attempts", attempts)
            .put("updatedAtMillis", updatedAtMillis)
            .put("lastError", lastError ?: JSONObject.NULL)
            .toString(2)

    companion object {
        fun parse(json: String): SupportSubmission {
            val values = JSONObject(json)
            val reportId = values.getString("reportId")
            UUID.fromString(reportId)
            val state = SupportSubmissionState.valueOf(values.getString("state"))
            return SupportSubmission(
                reportId = reportId,
                // A stored SUBMITTING means the process may have died mid-submit; the send
                // never completed, so the report is pending again rather than submitted.
                state =
                    if (state == SupportSubmissionState.SUBMITTING) {
                        SupportSubmissionState.PENDING_SUBMISSION
                    } else {
                        state
                    },
                reference = values.optString("reference").takeIf { !values.isNull("reference") && it.isNotBlank() },
                attempts = values.optInt("attempts"),
                updatedAtMillis = values.optLong("updatedAtMillis"),
                lastError = values.optString("lastError").takeIf { !values.isNull("lastError") && it.isNotBlank() },
            )
        }

        /** Fork semantics: a submitted report edited by the player becomes a brand-new draft. */
        fun forkJson(
            submittedJson: String,
            newId: String,
            newCreatedAtMillis: Long,
            newCreatedAt: String,
        ): String =
            JSONObject(submittedJson)
                .put("id", newId)
                .put("createdAtMillis", newCreatedAtMillis)
                .put("createdAt", newCreatedAt)
                .toString(2)
    }
}

class SupportSubmissionStore(context: Context) {
    private val directory = File(context.noBackupFilesDir, "support-submissions")

    fun load(reportId: String): SupportSubmission =
        synchronized(lock) {
            val file = submissionFile(reportId)
            if (!file.isFile) return@synchronized SupportSubmission(reportId, SupportSubmissionState.DRAFT)
            runCatching {
                require(file.length() in 1..MAX_BYTES.toLong())
                SupportSubmission.parse(file.readText()).also { require(it.reportId == reportId) }
            }.getOrElse { SupportSubmission(reportId, SupportSubmissionState.DRAFT) }
        }

    fun save(submission: SupportSubmission) =
        synchronized(lock) {
            val bytes = submission.toJson().toByteArray()
            require(bytes.size <= MAX_BYTES)
            check(directory.isDirectory || directory.mkdirs())
            val file = AtomicFile(submissionFile(submission.reportId))
            val output = file.startWrite()
            try {
                output.write(bytes)
                file.finishWrite(output)
            } catch (error: Exception) {
                file.failWrite(output)
                throw error
            }
            Unit
        }

    /** Records one transition, stamping the update time. Attempts only grow on an explicit transition into SUBMITTING. */
    fun transition(
        reportId: String,
        state: SupportSubmissionState,
        reference: String? = null,
        lastError: String? = null,
    ): SupportSubmission =
        synchronized(lock) {
            val current = load(reportId)
            val updated =
                current.copy(
                    state = state,
                    reference = reference ?: current.reference,
                    attempts = current.attempts + if (state == SupportSubmissionState.SUBMITTING) 1 else 0,
                    updatedAtMillis = System.currentTimeMillis(),
                    lastError = lastError,
                )
            save(updated)
            updated
        }

    fun delete(reportId: String) =
        synchronized(lock) {
            AtomicFile(submissionFile(reportId)).delete()
            Unit
        }

    private fun submissionFile(reportId: String): File = File(directory, "${UUID.fromString(reportId)}.json")

    companion object {
        private val lock = Any()
        private const val MAX_BYTES = 16_000
    }
}
