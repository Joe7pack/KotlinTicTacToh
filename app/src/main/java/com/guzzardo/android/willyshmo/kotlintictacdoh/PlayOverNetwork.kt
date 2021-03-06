package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import com.guzzardo.android.willyshmo.kotlintictacdoh.MainActivity.UserPreferences
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.androidId
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.latitude
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.longitude
import kotlinx.coroutines.*

class PlayOverNetwork : Activity(), ToastMessage {

    private var mPlayer1Name: String? = null
    private lateinit var mCallerActivity: PlayOverNetwork

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCallerActivity = this
        mApplicationContext = applicationContext
        mErrorHandler = ErrorHandler()
        mResources = resources
        sharedPreferences
        if (mPlayer1Name == null) {
            mPlayer1Name = intent.getStringExtra(GameActivity.PLAYER1_NAME)
        }
        if (mPlayer1Id == 0) {
            setSharedPreferences()
            addMyselfToPlayerList()
        } else {
            CoroutineScope( Dispatchers.Default).launch { //consider changing this to an async task?
                val webServerInterfaceUsersOnlineTask = WebServerInterfaceUsersOnlineTask()
                webServerInterfaceUsersOnlineTask.main(mCallerActivity, mPlayer1Name, resources, Integer.valueOf(mPlayer1Id))
            }
        }
        finish()
    }

    private fun addMyselfToPlayerList() {
        // add a new entry to the GamePlayer table
        val androidId = "?deviceId=$androidId"
        val latitude = "&latitude=$latitude"
        val longitude = "&longitude=$longitude"
        val trackingInfo = androidId + latitude + longitude
        val url = mResources!!.getString(R.string.domainName) + "/gamePlayer/createAndroid/" + trackingInfo + "&userName="
        CoroutineScope( Dispatchers.Default).launch {
            val webServerInterfaceNewPlayerTask =  WebServerInterfaceNewPlayerTask()
            webServerInterfaceNewPlayerTask.main(mCallerActivity as Context, url, mPlayer1Name, resources)
        }
    }

    private val sharedPreferences: Unit
        get() {
            val settings = getSharedPreferences(UserPreferences.PREFS_NAME, Context.MODE_PRIVATE)
            mPlayer1Id = settings.getInt(GameActivity.PLAYER1_ID, 0)
            mPlayer1Name = settings.getString(GameActivity.PLAYER1_NAME, null)
        }

    private fun setSharedPreferences() {
        val settings = getSharedPreferences(UserPreferences.PREFS_NAME, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString(GameActivity.PLAYER1_NAME, mPlayer1Name)
        // Commit the edits!
        editor.apply()
    }

    override fun onResume() {
        super.onResume()
        mPlayer1Name = intent.getStringExtra(GameActivity.PLAYER1_NAME)
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putInt("gn_player1_Id", mPlayer1Id)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mPlayer1Id = savedInstanceState.getInt("gn_player1_Id")
    }

    class ErrorHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            Toast.makeText(mApplicationContext, msg.obj as String, Toast.LENGTH_LONG).show()
        }
    }

    override fun sendToastMessage(message: String?) {
        val msg = mErrorHandler!!.obtainMessage()
        msg.obj = message
        mErrorHandler!!.sendMessage(msg)
    }

    companion object {
        private var mApplicationContext: Context? = null
        private var mPlayer1Id = 0
        private var mResources: Resources? = null
        var mErrorHandler: ErrorHandler? = null
        private fun writeToLog(filter: String, msg: String) {
            if ("true".equals(mResources!!.getString(R.string.debug), ignoreCase = true)) {
                Log.d(filter, msg)
            }
        }
    }
}