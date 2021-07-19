package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.Context
import android.content.Intent
import android.content.res.Resources
//import android.os.AsyncTask
import android.util.Log
import com.guzzardo.android.willyshmo.kotlintictacdoh.WebServerInterface.converseWithWebServer
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.androidId
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.latitude
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.longitude
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Boolean.FALSE

/**
 * An AsyncTask that will be used to find other players currently online
 */
class WebServerInterfaceUsersOnlineTask {
    private var mCallerActivity: Context? = null
    private var mToastMessage: ToastMessage? = null
    private var mPlayer1Name: String? = null
    private var mPlayer1Id: Int? = null
    private var mUsersOnline: String? = null

    fun main(callerActivity: Context?, player1Name: String?, resources: Resources, player1Id: Int) {
        mCallerActivity = callerActivity
        mPlayer1Name =  player1Name
        mResources = resources
        mPlayer1Id = player1Id
        val url = mResources.getString(R.string.domainName) + "/gamePlayer/listUsers"
        try {
            mUsersOnline = converseWithWebServer(url,null, mCallerActivity as ToastMessage, mResources)
            // consider replacing above call with this?
            //val sendMessageToWillyShmoServer = SendMessageToWillyShmoServer()
            //sendMessageToWillyShmoServer.main(url, null, mCallerActivity  as ToastMessage, mResources, java.lang.Boolean.valueOf(false))
        } catch (e: Exception) {
            writeToLog("WebServerInterfaceUsersOnlineTask", "doInBackground: " + e.message)
            mToastMessage!!.sendToastMessage(e.message)
        }
        writeToLog("WebServerInterfaceUsersOnline", "WebServerInterfaceUsersOnlineTask doInBackground called usersOnline: $mUsersOnline")
        setOnlineNow()
    }

     private fun setOnlineNow() {
        try {
            if (mUsersOnline == null) {
                return
            }
            writeToLog("WebServerInterfaceUsersOnlineTask","setPlayingNow called usersOnline: $mUsersOnline")
            val androidId = "&deviceId=$androidId"
            val latitude = "&latitude=$latitude"
            val longitude = "&longitude=$longitude"
            val trackingInfo = androidId + latitude + longitude
            val urlData = "/gamePlayer/update/?id=$mPlayer1Id$trackingInfo&onlineNow=true&playingNow=false&opponentId=0&userName="
            CoroutineScope( Dispatchers.Default).launch {
                val sendMessageToWillyShmoServer = SendMessageToWillyShmoServer()
                sendMessageToWillyShmoServer.main(urlData, mPlayer1Name, mCallerActivity as ToastMessage, mResources, FALSE)
            }
            val settings = mCallerActivity!!.getSharedPreferences(MainActivity.UserPreferences.PREFS_NAME,0)
            val editor = settings.edit()
            editor.putString("ga_users_online", mUsersOnline)
            // Commit the edits!
            editor.apply()
            val i = Intent(mCallerActivity, PlayersOnlineActivity::class.java)
            i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_DEBUG_LOG_RESOLUTION or Intent.FLAG_FROM_BACKGROUND)
            mCallerActivity!!.startActivity(i) // control is picked up in onCreate method
        } catch (e: Exception) {
            writeToLog(
                "WebServerInterfaceUsersOnlineTask",
                "onPostExecute exception called " + e.message
            )
            mToastMessage!!.sendToastMessage(e.message)
        }
    }

    companion object {
        private lateinit var mResources: Resources
        private fun writeToLog(filter: String, msg: String) {
            if ("true".equals(mResources.getString(R.string.debug), ignoreCase = true)) {
                Log.d(filter, msg)
            }
        }
    }
}