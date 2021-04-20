package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.widget.Toast
import com.guzzardo.android.willyshmo.kotlintictacdoh.MainActivity.UserPreferences
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.androidId
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.latitude
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.longitude

class PlayOverNetwork : Activity(), ToastMessage {
    interface NetworkValues { //    	static final String domainName = "http://ww2.guzzardo.com:8081"; // for production
        //    	static final String domainName = "http://localhost:8092";
        //    	static final String domainName = "http://10.0.2.2:8092"; // this works ok when using the emulator but when using
        // the G1 phone i need to use an external ip address
        // e.g. ww2.guzzardo.com or testandroid.guzzardo.com
        //    	static final String domainName = "http://testandroid.guzzardo.com:8082"; // for test 
        //    	static final String domainName = "http://216.80.121.243:6999"; // for test 
        //    	static final String domainName = "http://willyshmotest.guzzardo.com"; // for test - link up to Grails - set to port 6260       	
        //    	static final String domainName = "http://willyshmoprod.guzzardo.com"; // for Prod - link up to Grails - set to port 6360     	
    }

    private var mPlayer1Name: String? = null

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        errorHandler = ErrorHandler()
        Companion.resources = resources
        sharedPreferences
        if (mPlayer1Name == null) {
            mPlayer1Name = intent.getStringExtra(GameActivity.PLAYER1_NAME)
        }
        if (mPlayer1Id == 0) {
            setSharedPreferences()
            addMyselfToPlayerList()
        } else {
            val webServerInterfaceUsersOnlineTask = WebServerInterfaceUsersOnlineTask()
            webServerInterfaceUsersOnlineTask.execute(
                this,
                applicationContext,
                mPlayer1Name,
                Companion.resources,
                Integer.valueOf(mPlayer1Id)
            )
        }
        finish()
    }

    private fun addMyselfToPlayerList() {
        // add a new entry to the GamePlayer table
        val androidId = "?deviceId=$androidId"
        val latitude = "&latitude=$latitude"
        val longitude = "&longitude=$longitude"
        val trackingInfo = androidId + latitude + longitude
        val url =
            Companion.resources!!.getString(R.string.domainName) + "/gamePlayer/createAndroid/" + trackingInfo + "&userName="
        val webServerInterfaceNewPlayerTask =
            WebServerInterfaceNewPlayerTask()
        val playOverNetwork = this
        webServerInterfaceNewPlayerTask.execute(
            playOverNetwork,
            url,
            mPlayer1Name,
            applicationContext,
            Companion.resources
        )
    }

    private val sharedPreferences: Unit
        private get() {
            val settings = getSharedPreferences(UserPreferences.PREFS_NAME, Context.MODE_PRIVATE)
            mPlayer1Id = settings.getInt(GameActivity.PLAYER1_ID, 0)
            mPlayer1Name = settings.getString(GameActivity.PLAYER1_NAME, null)
        }

    private fun setSharedPreferences() {
        val settings = getSharedPreferences(UserPreferences.PREFS_NAME, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString(GameActivity.PLAYER1_NAME, mPlayer1Name)
        // Commit the edits!
        editor.commit()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mPlayer1Name = intent.getStringExtra(GameActivity.PLAYER1_NAME)
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
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

    inner class ErrorHandler : Handler() {
        override fun handleMessage(msg: Message) {
            Toast.makeText(applicationContext, msg.obj as String, Toast.LENGTH_LONG)
                .show()
        }
    }

    override fun sendToastMessage(message: String?) {
        val msg = errorHandler!!.obtainMessage()
        msg.obj = message
        errorHandler!!.sendMessage(msg)
    }

    companion object {
        private var mPlayer1Id = 0
        private var resources: Resources? = null
        var errorHandler: ErrorHandler? =
            null
    }
}