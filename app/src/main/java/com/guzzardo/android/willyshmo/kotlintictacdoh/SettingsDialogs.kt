/*
 * Copyright (C) 2007 The Android Open Source Project
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
 */
package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AlertDialog
import com.guzzardo.android.willyshmo.kotlintictacdoh.MainActivity.UserPreferences
import com.guzzardo.android.willyshmo.kotlintictacdoh.SettingsDialogs

//import android.support.v7.app.AlertDialog;
class SettingsDialogs : Activity() {
    private var mPlayer1Name: String? = null
    private var mPlayer2Name: String? = null
    private var mButtonPlayer1: Button? = null
    private var mButtonPlayer2: Button? = null
    private var mSeekBar: SeekBar? = null
    private var mTokenSize = 0
    private var mTokenColor = 0
    private var mTokenColor1 = 0
    private var mTokenColor2 = 0
    /*
    HandlerThread handlerThread;
    private Looper looper;
    private Handler mProgressHandler;
    private static final int MAX_PROGRESS = 100;
    private ProgressDialog mProgressDialog;
    private int mProgress;
    private final Handler mHandler = new Handler(Looper.getMainLooper(), (Handler.Callback) new MyHandlerCallback());
    private static class MyHandlerCallback {  }
    */

    /**
     * Initialization of the Activity after it is first created.  Must at least
     * call [android.app.Activity.setContentView] to
     * describe what is to be displayed in the screen.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPlayer1Name = intent.getStringExtra(GameActivity.PLAYER1_NAME)
        mPlayer2Name = intent.getStringExtra(GameActivity.PLAYER2_NAME)
        setContentView(R.layout.settings_dialog)
        mPlayer1Name = if (mPlayer1Name == null) " " else mPlayer1Name
        mPlayer2Name = if (mPlayer2Name == null) " " else mPlayer2Name
        mButtonPlayer1 = findViewById<View>(R.id.text_entry_button_player1_name) as Button
        mButtonPlayer1!!.text = "Player 1 Name: $mPlayer1Name"
        mButtonPlayer2 = findViewById<View>(R.id.text_entry_button_player2_name) as Button
        mButtonPlayer2!!.text = "Player 2 Name: $mPlayer2Name"
        val settings = getSharedPreferences(UserPreferences.PREFS_NAME, MODE_PRIVATE)
        mMoveModeTouch = settings.getBoolean(GameActivity.MOVE_MODE, false)
        mTokenSize = settings.getInt(GameActivity.TOKEN_SIZE, 50)
        mTokenColor1 = settings.getInt(GameActivity.TOKEN_COLOR_1, Color.RED)
        mTokenColor2 = settings.getInt(GameActivity.TOKEN_COLOR_2, Color.BLUE)
        mMoveModeChecked = if (!mMoveModeTouch) 0 else 1
        mSoundModeChecked = if (mSoundMode) 0 else 1

        /* Display a text message with yes/no buttons and handle each message as well as the cancel action */
        val twoButtonsTitle = findViewById<View>(R.id.reset_scores) as Button
        twoButtonsTitle.setOnClickListener { //                showDialog(RESET_SCORES);
            val resetScoresDialog = showResetScoresDialog()
            resetScoresDialog.show()
        }

        /* Display a text entry dialog for entry of player 1 name */
        mButtonPlayer1!!.setOnClickListener {
            val playerNameDialog = showPlayerNameDialog(1)
            playerNameDialog.show()
        }

        /* Display a text entry dialog for entry of player 2 name */
        mButtonPlayer2!!.setOnClickListener {
            val playerNameDialog = showPlayerNameDialog(2)
            playerNameDialog.show()
        }

        /*
        handlerThread = new HandlerThread("MyHandlerThread");
        handlerThread.start();
        looper = handlerThread.getLooper();
        
        mProgressHandler =  new Handler(looper) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (mProgress >= MAX_PROGRESS) {
                    mProgressDialog.dismiss();
                } else {
                    mProgress++;
                    mProgressDialog.incrementProgressBy(1);
                    mProgressHandler.sendEmptyMessageDelayed(0, 100);
                }
            }
        };
        */

        /* Display a radio button group */
        var radioButton = findViewById<View>(R.id.move_mode) as Button
        radioButton.setOnClickListener { //              showDialog(DIALOG_MOVE_MODE);
            val moveModeDialog = showMoveModeDialog()
            moveModeDialog.show()
        }

        /* Display a radio button group */
        radioButton = findViewById<View>(R.id.sound_mode) as Button
        radioButton.setOnClickListener {
            val soundModeDialog = showSoundModeDialog()
            soundModeDialog.show()
        }
        val tokenSizeTitle = findViewById<View>(R.id.seeker_entry_token_size) as Button
        tokenSizeTitle.setOnClickListener {
            val tokenSizeDialog = showTokenSizeDialog()
            tokenSizeDialog.show()
        }
        val tokenColorTitle = findViewById<View>(R.id.seeker_entry_token_color_1) as Button
        tokenColorTitle.setOnClickListener {
            val tokenColorDialog = showTokenColorDialog(1)
            tokenColorDialog.show()
        }
        val tokenColorTitle2 = findViewById<View>(R.id.seeker_entry_token_color_2) as Button
        tokenColorTitle2.setOnClickListener {
            val tokenColorDialog2 = showTokenColorDialog(2)
            tokenColorDialog2.show()
        }
    }

    private fun showMoveModeDialog(): AlertDialog {
        return AlertDialog.Builder(this@SettingsDialogs)
            .setIcon(R.drawable.willy_shmo_small_icon)
            .setTitle(R.string.move_mode)
            .setSingleChoiceItems(R.array.select_move_mode, mMoveModeChecked) { dialog, whichButton -> setMoveModeSelection(whichButton) }
            .setPositiveButton(R.string.alert_dialog_ok) { dialog, whichButton -> setMoveMode() }
            .setNegativeButton(R.string.alert_dialog_cancel) { dialog, whichButton -> dialog.cancel() }
            .create()
    }

    private fun showSoundModeDialog(): AlertDialog {
        return AlertDialog.Builder(this@SettingsDialogs)
            .setIcon(R.drawable.willy_shmo_small_icon)
            .setTitle(R.string.sound_mode)
            .setSingleChoiceItems(R.array.select_sound_mode, mSoundModeChecked) { dialog, whichButton -> setSoundModeSelection(whichButton) }
            .setPositiveButton(R.string.alert_dialog_ok) { dialog, whichButton -> setSoundMode() }
            .setNegativeButton(R.string.alert_dialog_cancel) { dialog, whichButton -> dialog.cancel() }
            .create()
    }

    private fun showResetScoresDialog(): AlertDialog {
        return AlertDialog.Builder(this@SettingsDialogs)
            .setIcon(R.drawable.willy_shmo_small_icon)
            .setTitle(R.string.alert_dialog_reset_scores)
            .setPositiveButton(R.string.alert_dialog_ok) { dialog, whichButton -> /* User clicked OK so do some stuff */
                resetScores()
            }
            .setNegativeButton(R.string.alert_dialog_cancel) { dialog, whichButton -> /* User clicked Cancel so do some stuff */ }
            .create()
    }

    private fun showPlayerNameDialog(playerId: Int): AlertDialog {
        // This example shows how to add a custom layout to an AlertDialog
        var titleId = R.string.alert_dialog_text_entry_player1_name
        if (playerId == 2) {
            titleId = R.string.alert_dialog_text_entry_player2_name
        }
        val factory = LayoutInflater.from(this)
        val textEntryViewPlayer = factory.inflate(R.layout.name_dialog_text_entry, null)
        return AlertDialog.Builder(this@SettingsDialogs)
            .setIcon(R.drawable.willy_shmo_small_icon)
            .setTitle(titleId)
            .setView(textEntryViewPlayer)
            .setCancelable(false)
            .setPositiveButton(R.string.alert_dialog_ok) { dialog, whichButton -> /* User clicked OK so do some stuff */
                val userName = textEntryViewPlayer.findViewById<View>(R.id.username_edit) as EditText
                val userNameText = userName.text
                val userNameLength = if (userNameText.length > 15) 15 else userNameText.length
                val userNameChars = CharArray(userNameLength)
                userNameText.getChars(0, userNameLength, userNameChars, 0)
                val intent = Intent(applicationContext, SettingsDialogs::class.java)
                if (playerId == 1) {
                    mPlayer1Name = String(userNameChars)
                    intent.putExtra(GameActivity.PLAYER1_ID, 0)
                } else {
                    mPlayer2Name = String(userNameChars)
                }
                intent.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name)
                intent.putExtra(GameActivity.PLAYER2_NAME, mPlayer2Name)
                startActivityForResult(intent, 1)
                finish()
            }
            .setNegativeButton(R.string.alert_dialog_cancel) { dialog, whichButton -> /* User clicked cancel so do some stuff */ }
            .create()
    }

    private fun showTokenSizeDialog(): AlertDialog {
        val tokenSizeDialog = AlertDialog.Builder(this)
        val titleId = R.string.alert_dialog_seeker_entry_token_size
        val factory = LayoutInflater.from(this)
        val tokenSizeEntryView = factory.inflate(R.layout.token_size_dialog_entry, null)
        tokenSizeDialog.setView(tokenSizeEntryView)
        mSeekBar = tokenSizeEntryView.findViewById<View>(R.id.seekBar) as SeekBar
        mSeekBar!!.progress = mTokenSize
        mSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mTokenSize = progress
            }
        })
        tokenSizeDialog.setIcon(R.drawable.willy_shmo_small_icon)
        tokenSizeDialog.setTitle(titleId)
        tokenSizeDialog.setCancelable(false)
        tokenSizeDialog.setPositiveButton(R.string.alert_dialog_ok) { dialog, whichButton -> /* User clicked OK so do some stuff */
            setTokenSize()
            val intent = Intent(applicationContext, SettingsDialogs::class.java)
            startActivityForResult(intent, 1)
            finish()
        }
        tokenSizeDialog.setNegativeButton(R.string.alert_dialog_cancel) { dialog, whichButton -> /* User clicked cancel so do some stuff */ }
        return tokenSizeDialog.create()
    }

    fun setTokenColorFromDialog(newTokenColor: Int) {
        mTokenColor = newTokenColor
    }

    private fun showTokenColorDialog(playerNumber: Int): AlertDialog {
        val tokenColorDialog = AlertDialog.Builder(this)
        val titleId =
            if (playerNumber == 1) R.string.alert_dialog_seeker_entry_token_color_1 else R.string.alert_dialog_seeker_entry_token_color_2
        val newColor = if (playerNumber == 1) mTokenColor1 else mTokenColor2
        val tokenColorPickerView = TokenColorPickerView(this, this@SettingsDialogs, newColor)
        tokenColorDialog.setView(tokenColorPickerView)
        tokenColorDialog.setIcon(R.drawable.willy_shmo_small_icon)
        tokenColorDialog.setTitle(titleId)
        tokenColorDialog.setCancelable(false)
        tokenColorDialog.setPositiveButton(R.string.alert_dialog_ok) { dialog, whichButton -> /* User clicked OK so do some stuff */
            setTokenColor(playerNumber)
            val intent = Intent(applicationContext, SettingsDialogs::class.java)
            startActivityForResult(intent, 1)
            finish()
        }
        tokenColorDialog.setNegativeButton(R.string.alert_dialog_cancel) { dialog, whichButton -> /* User clicked cancel so do some stuff */ }
        return tokenColorDialog.create()
    }

    override fun onResume() {
        super.onResume()
        mPlayer1Name = intent.getStringExtra(GameActivity.PLAYER1_NAME)
        mPlayer2Name = intent.getStringExtra(GameActivity.PLAYER2_NAME)
    }

    override fun onPause() {
        super.onPause()
        intent.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name)
        intent.putExtra(GameActivity.PLAYER2_NAME, mPlayer2Name)
    }

    override fun onStop() {
        super.onStop()

        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        val settings = getSharedPreferences(UserPreferences.PREFS_NAME, 0)
        val editor = settings.edit()
        editor.putString(GameActivity.PLAYER1_NAME, mPlayer1Name)
        editor.putString(GameActivity.PLAYER2_NAME, mPlayer2Name)

        // Commit the edits!
        editor.commit()
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putBoolean("gDialog_move_mode", mMoveModeTouch)
        savedInstanceState.putBoolean("gDialog_sound_mode", mSoundMode)
        savedInstanceState.putInt("gDialog_token_size", mTokenSize)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.	
        mMoveModeTouch = savedInstanceState.getBoolean("gDialog_move_mode")
        mSoundMode = savedInstanceState.getBoolean("gDialog_sound_mode")
        mTokenSize = savedInstanceState.getInt("gDialog_token_size")
    }

    private fun resetScores() {
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        val settings = getSharedPreferences(UserPreferences.PREFS_NAME, 0)
        val editor = settings.edit()
        editor.putInt(GameActivity.PLAYER1_SCORE, 0)
        editor.putInt(GameActivity.PLAYER2_SCORE, 0)
        editor.putInt(GameActivity.WILLY_SCORE, 0)
        // Commit the edits!
        editor.commit()
    }

    private fun setMoveModeSelection(moveMode: Int) {
        mMoveModeTouch = if (moveMode == 0) false else true
    }

    private fun setMoveMode() {
        val settings = getSharedPreferences(UserPreferences.PREFS_NAME, 0)
        val editor = settings.edit()
        editor.putBoolean(GameActivity.MOVE_MODE, mMoveModeTouch)
        // Commit the edits!
        editor.commit()
    }

    private fun setTokenSize() {
        val settings = getSharedPreferences(UserPreferences.PREFS_NAME, 0)
        val editor = settings.edit()
        editor.putInt(GameActivity.TOKEN_SIZE, mTokenSize)
        // Commit the edits!
        editor.commit()
    }

    private fun setTokenColor(playerNumber: Int) {
        val settings = getSharedPreferences(UserPreferences.PREFS_NAME, 0)
        val editor = settings.edit()
        if (playerNumber == 1) {
            editor.putInt(GameActivity.TOKEN_COLOR_1, mTokenColor)
        } else {
            editor.putInt(GameActivity.TOKEN_COLOR_2, mTokenColor)
        }
        // Commit the edits!
        editor.commit()
    }

    private fun setSoundModeSelection(soundMode: Int) {
        mSoundMode = if (soundMode == 0) true else false
    }

    private fun setSoundMode() {
        val settings = getSharedPreferences(UserPreferences.PREFS_NAME, 0)
        val editor = settings.edit()
        editor.putBoolean(GameActivity.SOUND_MODE, mSoundMode)
        // Commit the edits!
        editor.commit()
    }

    companion object {
        private var mMoveModeTouch = false //false = drag move mode; true = touch move mode
        private var mMoveModeChecked = 0 // 0 = drag move mode; 1 = touch move mode
        private var mSoundModeChecked = 0 // 0 = sound on; 1 = sound off
        private var mSoundMode = false //false = no sound; true = sound
    }
}