package com.swordfish.lemuroid.app.shared.support

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class SupportSubmissionTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun stateLabelsMatchThePlayerFacingWording() {
        assertEquals("Draft", SupportSubmissionState.DRAFT.label)
        assertEquals("Pending submission", SupportSubmissionState.PENDING_SUBMISSION.label)
        assertEquals("Submitting", SupportSubmissionState.SUBMITTING.label)
        assertEquals("Submitted", SupportSubmissionState.SUBMITTED.label)
        assertEquals("Submission failed", SupportSubmissionState.SUBMISSION_FAILED.label)
    }

    @Test
    fun unknownReportStartsAsDraftAndTransitionsPersist() {
        val store = SupportSubmissionStore(context)
        val report = SupportReport.create("settings", "Other", null)
        assertEquals(SupportSubmissionState.DRAFT, store.load(report.id).state)

        store.transition(report.id, SupportSubmissionState.PENDING_SUBMISSION)
        assertEquals(SupportSubmissionState.PENDING_SUBMISSION, store.load(report.id).state)

        store.transition(report.id, SupportSubmissionState.SUBMITTING)
        val submitting = store.load(report.id)
        assertEquals(SupportSubmissionState.PENDING_SUBMISSION, submitting.state)
        assertEquals(1, submitting.attempts)

        store.transition(report.id, SupportSubmissionState.SUBMITTED, reference = "CD-58")
        val submitted = store.load(report.id)
        assertEquals(SupportSubmissionState.SUBMITTED, submitted.state)
        assertEquals("CD-58", submitted.reference)
        assertNull(submitted.lastError)
        assertTrue(submitted.updatedAtMillis > 0)

        store.delete(report.id)
        assertEquals(SupportSubmissionState.DRAFT, store.load(report.id).state)
    }

    @Test
    fun staleSubmittingRecordResolvesToPendingOnLoad() {
        val report = SupportReport.create("settings", "Other", null)
        val stale =
            SupportSubmission(
                reportId = report.id,
                state = SupportSubmissionState.SUBMITTING,
                attempts = 2,
                updatedAtMillis = 123L,
            )
        assertEquals(SupportSubmissionState.SUBMITTING, JSONObject(stale.toJson()).getString("state").let {
            SupportSubmissionState.valueOf(it)
        })
        val resolved = SupportSubmission.parse(stale.toJson())
        assertEquals(SupportSubmissionState.PENDING_SUBMISSION, resolved.state)
        assertEquals(2, resolved.attempts)
    }

    @Test
    fun failureRecordsTheErrorAndKeepsTheReferenceSlotEmpty() {
        val store = SupportSubmissionStore(context)
        val report = SupportReport.create("settings", "Other", null)
        store.transition(report.id, SupportSubmissionState.SUBMITTING)
        store.transition(report.id, SupportSubmissionState.SUBMISSION_FAILED, lastError = "HTTP 503")
        val failed = store.load(report.id)
        assertEquals(SupportSubmissionState.SUBMISSION_FAILED, failed.state)
        assertEquals("HTTP 503", failed.lastError)
        assertNull(failed.reference)
    }

    @Test
    fun deletingAReportAlsoDeletesItsSubmissionRecord() {
        val reports = SupportReportStore(context)
        val submissions = SupportSubmissionStore(context)
        val report = SupportReport.create("settings", "Other", null)
        reports.save(report)
        submissions.transition(report.id, SupportSubmissionState.SUBMITTED, reference = "CD-99")
        assertEquals(SupportSubmissionState.SUBMITTED, submissions.load(report.id).state)
        reports.delete(report.id)
        assertEquals(SupportSubmissionState.DRAFT, submissions.load(report.id).state)
        assertNull(submissions.load(report.id).reference)
    }

    @Test
    fun forkJsonCreatesANewDraftIdentityAndCopiesTheFields() {
        val original = SupportReport.create("settings", "Crash or freeze", "stage=CORE_FRAME", gameTitle = "Fixture")
        val newId = "123e4567-e89b-42d3-a456-426614174000"
        val forked =
            SupportReport.parse(
                SupportSubmission.forkJson(original.json, newId, 999_000L, "2026-11-28T00:00:00.000Z"),
            )
        assertEquals(newId, forked.id)
        assertNotEquals(original.id, forked.id)
        assertEquals(999_000L, forked.createdAtMillis)
        assertEquals("2026-11-28T00:00:00.000Z", forked["createdAt"])
        assertEquals(original["category"], forked["category"])
        assertEquals(original["game"], forked["game"])
        assertEquals(original["diagnostic"], forked["diagnostic"])
        // The fork starts life as an unsubmitted draft.
        val submissions = SupportSubmissionStore(context)
        assertEquals(SupportSubmissionState.DRAFT, submissions.load(forked.id).state)
    }
}
