package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.guzzardo.android.willyshmo.kotlintictacdoh.PlayersOnlineActivity.Companion.getContext

/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ //import android.support.v7.app.AlertDialog;
class TwoPlayerActivity : Activity() {
    private var mPlayer1Name = "Player 1"
    private var mPlayer2Name = "Player 2"
    private var mButtonPlayer1: Button? = null
    private var mButtonPlayer2: Button? = null
    private var mButtonPlayOverNetwork: Button? = null

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.two_player)
        val player1Name = intent.getStringExtra(GameActivity.PLAYER1_NAME)
        if (player1Name != null) mPlayer1Name = player1Name
        val player2Name = intent.getStringExtra(GameActivity.PLAYER2_NAME)
        if (player2Name != null) mPlayer2Name = player2Name
        mButtonPlayer1 = findViewById<View>(R.id.player_1) as Button
        mButtonPlayer1!!.text = "$mPlayer1Name moves first?"
        mButtonPlayer2 = findViewById<View>(R.id.player_2) as Button
        mButtonPlayer2!!.text = "$mPlayer2Name moves first?"
        findViewById<View>(R.id.player_1).setOnClickListener {
            startGame(1)
        }
        findViewById<View>(R.id.player_2).setOnClickListener {
            startGame(2)
        }
        mButtonPlayOverNetwork = findViewById<View>(R.id.play_over_network) as Button
        mButtonPlayOverNetwork!!.setOnClickListener { playOverNetwork() }

        /*
        if (WillyShmoApplication.isNetworkAvailable) {
            mButtonPlayOverNetwork!!.visibility = View.VISIBLE
        } else {
            mButtonPlayOverNetwork!!.visibility = View.GONE
        }
        */
    }

    private fun startGame(player: Int) {
        val i = Intent(this, GameActivity::class.java)
        i.putExtra(
            GameActivity.START_PLAYER_HUMAN,
            if (player == 1) GameView.State.PLAYER1.value else GameView.State.PLAYER2.value
        )
        i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name)
        i.putExtra(GameActivity.PLAYER2_NAME, mPlayer2Name)
        startActivity(i)
        finish()
    }

    private fun playOverNetwork() {
        if (mPlayer1Name == "" || mPlayer1Name.equals("Player 1", ignoreCase = true)) {
            displayNameRequiredAlert()
            return
        }
        val i = Intent(this, PlayOverNetwork::class.java)
        i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name)
        startActivity(i)
        finish()
    }

    private fun displayNameRequiredAlert() {
        try {
            AlertDialog.Builder(this@TwoPlayerActivity)
                .setTitle("Please enter your name in the settings menu")
                .setNeutralButton("OK") { _, which -> finish() }
                .setIcon(R.drawable.willy_shmo_small_icon)
                .show()
        } catch (e: Exception) {
            sendToastMessage(e.message)
        }
    }

    override fun onPause() {
        super.onPause()
        writeToLog("TwoPlayerActivity", "onPause called for TwoPlayerActivity")
    }

    override fun onDestroy() {
        super.onDestroy()
        writeToLog("TwoPlayerActivity", "onDestroy called for TwoPlayerActivity")
    }

    private fun writeToLog(filter: String, msg: String) {
        if ("true".equals(resources.getString(R.string.debug), ignoreCase = true)) {
            Log.d(filter, msg)
        }
    }

    private fun sendToastMessage(message: String?) {
        val msg = errorHandler!!.obtainMessage()
        msg.obj = message
        errorHandler!!.sendMessage(msg)
    }

    class ErrorHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            Toast.makeText(getContext(), msg.obj as String, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        var errorHandler: ErrorHandler? = null
    }
}