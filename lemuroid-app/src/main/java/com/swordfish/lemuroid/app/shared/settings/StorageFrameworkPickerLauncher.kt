package com.swordfish.lemuroid.app.shared.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.app.utils.android.displayErrorDialog
import com.swordfish.lemuroid.lib.android.RetrogradeActivity
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import javax.inject.Inject

class StorageFrameworkPickerLauncher : RetrogradeActivity() {
    @Inject
    lateinit var directoriesManager: DirectoriesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val intent =
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    this.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    this.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    this.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                    this.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                }
            try {
                startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER)
            } catch (e: Exception) {
                showStorageAccessFrameworkNotSupportedDialog()
            }
        }
    }

    private fun showStorageAccessFrameworkNotSupportedDialog() {
        val message = getString(R.string.dialog_saf_not_found, directoriesManager.getInternalRomsDirectory())
        val actionLabel = getString(R.string.ok)
        displayErrorDialog(message, actionLabel) { finish() }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        resultData: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, resultData)

        val newFolder = resultData?.data
        if (requestCode == REQUEST_CODE_PICK_FOLDER && resultCode == Activity.RESULT_OK && newFolder != null) {
            val preferences = SharedPreferencesHelper.getLegacySharedPreferences(this)
            val key = getString(com.swordfish.lemuroid.lib.R.string.pref_key_extenral_folder)
            val previous = preferences.getString(key, null)?.let(Uri::parse)
            val accepted =
                acceptLibraryFolder(
                    previous = previous,
                    selected = newFolder,
                    acquire = {
                        contentResolver.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    },
                    save = { preferences.edit().putString(key, it.toString()).apply() },
                )
            if (accepted) {
                startLibraryIndexWork()
            } else {
                displayErrorDialog(
                    "Read access to the selected folder could not be kept.",
                    getString(R.string.ok),
                ) { finish() }
                return
            }
        }
        finish()
    }

    private fun startLibraryIndexWork() {
        LibraryIndexScheduler.scheduleLibrarySync(applicationContext)
    }

    companion object {
        private const val REQUEST_CODE_PICK_FOLDER = 1

        fun pickFolder(context: Context) {
            context.startActivity(Intent(context, StorageFrameworkPickerLauncher::class.java))
        }
    }
}

/** Keep existing provider grants, which may also be used by existing cartridges or other app features. */
internal fun acceptLibraryFolder(
    previous: Uri?,
    selected: Uri,
    acquire: (Uri) -> Unit,
    save: (Uri) -> Unit,
): Boolean {
    try {
        acquire(selected)
    } catch (_: SecurityException) {
        return false
    }
    if (selected != previous) save(selected)
    return true
}
