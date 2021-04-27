package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.app.Dialog
import android.app.DialogFragment
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.getConfigMap

class RejectGameDialog : DialogFragment(), ToastMessage {
    override fun setArguments(myBundle: Bundle) {
        mOpposingPlayerName = myBundle.getString("opponentName")
        mOpposingPlayerId = myBundle.getString("opponentId")
        mPlayerName = myBundle.getString("playerName")
        mPlayerId = myBundle.getInt("player1Id")
    }

    fun setContext(applicationContext: Context?) {
        mApplicationContext = applicationContext
    }

    fun setResources(resources: Resources?) {
        mResources = resources
    }

    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        return AlertDialog.Builder(activity)
                .setTitle("Sorry, " + mOpposingPlayerName + " doesn't want to play now")
                .setIcon(R.drawable.willy_shmo_small_icon)
                .setPositiveButton(android.R.string.yes) { dialog, which ->
                //acknowledgeRejection();
                }
                .create()
    }

    fun setOpposingPlayerName(name: String?) {
        mOpposingPlayerName = name
    }

    private fun acknowledgeRejection() {
        //String hostName = mResources.getString(R.string.RabbitMQHostName);
        val qName = getConfigMap("RabbitMQQueuePrefix") + "-" + "startGame" + "-" + mOpposingPlayerId
        val messageToOpponent = "refused," + mPlayerName + "," + mPlayerId
        SendMessageToRabbitMQTask().execute(getConfigMap("RabbitMQIpAddress"), qName, null, messageToOpponent, this, mResources)
    }

    override fun finish() {}
    override fun sendToastMessage(message: String?) {
        val msg = errorHandler!!.obtainMessage()
        msg.obj = message
        errorHandler!!.sendMessage(msg)
    }

    inner class ErrorHandler : Handler() {
        override fun handleMessage(msg: Message) {
            Toast.makeText(mApplicationContext, msg.obj as String, Toast.LENGTH_LONG).show()
        }
    }

    fun setContentView(view: Int) {}

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