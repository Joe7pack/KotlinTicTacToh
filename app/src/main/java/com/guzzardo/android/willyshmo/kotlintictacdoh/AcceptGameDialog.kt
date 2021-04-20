package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.app.Dialog
import android.app.DialogFragment
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.guzzardo.android.willyshmo.kotlintictacdoh.MainActivity.UserPreferences

//This class is no longer used, GameActivity.acceptIncomingGameRequestFromClient() is used instead
//import android.support.v7.app.AlertDialog;
class AcceptGameDialog : DialogFragment(), ToastMessage {
    override fun setArguments(myBundle: Bundle) {
        mOpposingPlayerName = myBundle.getString("opponentName")
        mOpposingPlayerId = myBundle.getString("opponentId")
        mPlayerName = myBundle.getString("playerName")
        mPlayerId = myBundle.getInt("player1Id")
    }

    fun setContext(applicationContext: Context) {
        mApplicationContext = applicationContext
    }

    fun setResources(resources: Resources?) {
        mResources = resources
    }

    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        return AlertDialog.Builder(activity)
            .setTitle(mOpposingPlayerName + R.string.would_like_to_play)
            .setIcon(R.drawable.willy_shmo_small_icon)
            .setNegativeButton(android.R.string.no) { dialog, which -> rejectGame() }
            .setPositiveButton(android.R.string.yes) { dialog, which -> acceptGame() }
            .create()
    }

    fun setOpposingPlayerName(name: String?) {
        mOpposingPlayerName = name
    }

    private fun rejectGame() { //this is what the server side will see
        val hostName = WillyShmoApplication.getConfigMap("RabbitMQIpAddress") as String
        val queuePrefix = WillyShmoApplication.getConfigMap("RabbitMQQueuePrefix") as String
        val qName = "$queuePrefix-client-$mOpposingPlayerId"
        val messageToOpponent = "noPlay,$mPlayerName,$mPlayerId"
        SendMessageToRabbitMQTask().execute(hostName, qName, null, messageToOpponent, this, mResources)
    }

    private fun acceptGame() { //this is what the server side will see
        writeToLog("AcceptGameDialog", "at start of acceptGame method")
        val settings = mApplicationContext!!.getSharedPreferences(UserPreferences.PREFS_NAME,0)
        val editor = settings.edit()
        editor.putString("ga_opponent_screenName", mOpposingPlayerName)
        editor.commit()
        val i = Intent(mApplicationContext, GameActivity::class.java)
        i.putExtra(GameActivity.START_SERVER, "true")
        i.putExtra(GameActivity.PLAYER1_ID, mPlayerId)
        i.putExtra(GameActivity.PLAYER1_NAME, mPlayerName)
        i.putExtra(GameActivity.START_CLIENT_OPPONENT_ID, mOpposingPlayerId)
        i.putExtra(GameActivity.PLAYER2_NAME, mOpposingPlayerName)
        i.putExtra(GameActivity.HAVE_OPPONENT, "true")
        writeToLog("AcceptGameDialog", "starting server only")
        startActivity(i)
    }

    override fun sendToastMessage(message: String?) {
        TODO("Not yet implemented")
    }

    override fun finish() { // to fulfill contract with ToastMessage
    }

    inner class ErrorHandler : Handler() {
        override fun handleMessage(msg: Message) {
            Toast.makeText(mApplicationContext, msg.obj as String, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private var mOpposingPlayerName: String? = null
        private var mOpposingPlayerId: String? = null
        private var mApplicationContext: Context? = null
        private var mPlayerName: String? = null
        private var mPlayerId: Int? = null
        private var mResources: Resources? = null
        var errorHandler: ErrorHandler? = null

        private fun writeToLog(filter: String, msg: String) {
            if ("true".equals(mResources!!.getString(R.string.debug), ignoreCase = true)) {
                Log.d(filter, msg)
            }
        }
    }
}