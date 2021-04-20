package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.AsyncTask
import android.util.Log
import com.guzzardo.android.willyshmo.kotlintictacdoh.WebServerInterface.converseWithWebServer
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.androidId
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.latitude
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.longitude
import java.lang.Boolean.FALSE

/**
 * An AsyncTask that will be used to find other players currently online
 */
class WebServerInterfaceUsersOnlineTask :
    AsyncTask<Any?, Void?, String?>() {
    private var mCallerActivity: PlayOverNetwork? = null
    private var applicationContext: Context? = null
    private var mPlayer1Name: String? = null
    private var mPlayer1Id: Int? = null
    protected override fun doInBackground(vararg params: Any?): String? {
        var usersOnline: String? = null
        mCallerActivity = params[0] as PlayOverNetwork
        applicationContext = params[1] as Context
        mPlayer1Name = params[2] as String
        mResources = params[3] as Resources
        mPlayer1Id = params[4] as Int
        val url = mResources!!.getString(R.string.domainName) + "/gamePlayer/listUsers"
        try {
            usersOnline = converseWithWebServer(url,null, mCallerActivity, mResources!!)
        } catch (e: Exception) {
            writeToLog("WebServerInterfaceUsersOnlineTask", "doInBackground: " + e.message)
            mCallerActivity!!.sendToastMessage(e.message)
        }
        writeToLog("WServerInterfaceUsersOnline", "WebServerInterfaceUsersOnlineTask doInBackground called usersOnline: $usersOnline")
        return usersOnline
    }

    override fun onPostExecute(usersOnline: String?) {
        try {
            writeToLog("WebServerInterfaceUsersOnlineTask","onPostExecute called usersOnline: $usersOnline")
            val androidId = "&deviceId=$androidId"
            val latitude = "&latitude=$latitude"
            val longitude = "&longitude=$longitude"
            val trackingInfo = androidId + latitude + longitude
            val urlData = "/gamePlayer/update/?id=$mPlayer1Id$trackingInfo&onlineNow=true&opponentId=0&userName="
            SendMessageToWillyShmoServer().execute(urlData, mPlayer1Name, mCallerActivity, mResources, FALSE)
            if (usersOnline == null) {
                return
            }
            val settings = applicationContext!!.getSharedPreferences(MainActivity.UserPreferences.PREFS_NAME,0)
            val editor = settings.edit()
            editor.putString("ga_users_online", usersOnline)
            // Commit the edits!
            editor.apply()
            val i = Intent(mCallerActivity, PlayersOnlineActivity::class.java)
            i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_DEBUG_LOG_RESOLUTION or Intent.FLAG_FROM_BACKGROUND)
            applicationContext!!.startActivity(i) // control is picked up in onCreate method 	        
        } catch (e: Exception) {
            writeToLog(
                "WebServerInterfaceUsersOnlineTask",
                "onPostExecute exception called " + e.message
            )
            mCallerActivity!!.sendToastMessage(e.message)
        }
    }

    companion object {
        private var mResources: Resources? = null
        private fun writeToLog(filter: String, msg: String) {
            if ("true".equals(mResources!!.getString(R.string.debug), ignoreCase = true)) {
                Log.d(filter, msg)
            }
        }
    }
}