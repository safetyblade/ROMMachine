package com.swordfish.lemuroid.app.shared.gamecrash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.core.view.isVisible
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.app.shared.game.ps2next.Armsx2BiosRecovery

class GameCrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash)

        val message = intent.getStringExtra(EXTRA_MESSAGE)
        val messageDetail = intent.getStringExtra(EXTRA_MESSAGE_DETAIL)

        findViewById<TextView>(R.id.text1).apply {
            isVisible = !message.isNullOrEmpty()
            text = message
        }

        findViewById<TextView>(R.id.text2).apply {
            isVisible = !messageDetail.isNullOrEmpty()
            text = messageDetail
            setTextIsSelectable(true)
        }
        val recovery = Armsx2BiosRecovery.validate(this, intent.getParcelableExtra(EXTRA_BIOS_RECOVERY))
        findViewById<android.widget.Button>(R.id.importPs2Bios).apply {
            isVisible = recovery != null
            setOnClickListener { recovery?.let { startActivityForResult(it, REQUEST_BIOS) } }
        }
        findViewById<android.widget.Button>(R.id.copyReport).setOnClickListener {
            getSystemService(android.content.ClipboardManager::class.java).setPrimaryClip(
                android.content.ClipData.newPlainText(
                    "CARTDECK engine report",
                    "${findViewById<TextView>(R.id.text1).text}\n${findViewById<TextView>(R.id.text2).text}",
                ),
            )
            android.widget.Toast.makeText(this, "Engine report copied", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_BIOS) return
        if (resultCode == RESULT_OK) {
            android.widget.Toast.makeText(
                this,
                "PS2 BIOS imported. Reopen your cartridge from the shelf.",
                android.widget.Toast.LENGTH_LONG,
            ).show()
            finish()
        } else if (resultCode == BaseGameActivity.RESULT_ERROR) {
            data?.getStringExtra(BaseGameActivity.PLAY_GAME_RESULT_ERROR)?.let {
                findViewById<TextView>(R.id.text2).text = it
            }
        }
    }

    companion object {
        private const val REQUEST_BIOS = 617
        const val EXTRA_BIOS_RECOVERY = "EXTRA_PS2_BIOS_RECOVERY"
        private const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
        private const val EXTRA_MESSAGE_DETAIL = "EXTRA_MESSAGE_DETAIL"

        fun launch(
            activity: Activity,
            message: String,
            messageDetail: String?,
            biosRecovery: Intent? = null,
        ) {
            val intent =
                Intent(activity, GameCrashActivity::class.java).apply {
                    putExtra(EXTRA_MESSAGE, message)
                    putExtra(EXTRA_MESSAGE_DETAIL, messageDetail)
                    putExtra(EXTRA_BIOS_RECOVERY, biosRecovery)
                }
            activity.startActivity(intent)
        }
    }
}
