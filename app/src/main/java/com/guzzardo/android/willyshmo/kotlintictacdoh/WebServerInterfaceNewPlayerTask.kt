package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.guzzardo.android.willyshmo.kotlintictacdoh.MainActivity.UserPreferences
import com.guzzardo.android.willyshmo.kotlintictacdoh.WebServerInterface.converseWithWebServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONException
import org.json.JSONObject

class WebServerInterfaceNewPlayerTask {
    private var mCallerActivity: Context? = null
    private lateinit var mToastMessage: ToastMessage //? = mCallerActivity as ToastMessage
    private var mPlayer1Name: String? = null
    private var mPlayer1Id: Int? = null

    fun main(callerActivity: Context, url: String, player1Name: String?, resources: Resources) = runBlocking {
        mCallerActivity = callerActivity
        mToastMessage = callerActivity as ToastMessage
        mPlayer1Name = player1Name
        mResources = resources
        writeToLog("WebServerInterfaceNewPlayerTask", "doInBackground called")
        try {
            val newUser = converseWithWebServer(url, mPlayer1Name, mToastMessage, mResources!!)
            mPlayer1Id = getNewUserId(newUser)
        } catch (e: Exception) {
            writeToLog("WebServerInterfaceNewPlayerTask", "doInBackground exception called " + e.message)
            mToastMessage!!.sendToastMessage(e.message)
        }
        findOtherPlayersCurrentlyOnline()
    }

    private fun findOtherPlayersCurrentlyOnline() {
        try {
            writeToLog("WebServerInterfaceNewPlayerTask", "onPostExecute called player1Id: $mPlayer1Id")
            if (mPlayer1Id == null) {
                return
            }
            val settings = mCallerActivity!!.getSharedPreferences(UserPreferences.PREFS_NAME, 0)
            val editor = settings.edit()
            editor.putInt(GameActivity.PLAYER1_ID, mPlayer1Id!!)
            // Commit the edits!
            editor.apply()
            CoroutineScope( Dispatchers.Default).launch {
                val webServerInterfaceUsersOnlineTask = WebServerInterfaceUsersOnlineTask()
                webServerInterfaceUsersOnlineTask.main(mCallerActivity, mPlayer1Name, mResources, mPlayer1Id!!)
            }
        } catch (e: Exception) {
            writeToLog("WebServerInterfaceNewPlayerTask", "onPostExecute exception called " + e.message)
            mToastMessage!!.sendToastMessage(e.message)
        }
    }

    private fun getNewUserId(newUser: String?): Int {
        try {
            val jsonObject = JSONObject(newUser)
            val userObject = jsonObject.getJSONObject("User")
            val userId = userObject.getString("id")
            return userId.toInt()
        } catch (e: JSONException) {
            writeToLog("WebServerInterfaceNewPlayerTask", "getNewUserId exception called " + e.message)
            mToastMessage!!.sendToastMessage(e.message)
        }
        return 0
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