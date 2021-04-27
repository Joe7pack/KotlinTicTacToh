package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.Context
import android.content.res.Resources
import android.os.AsyncTask
import android.util.Log
import com.guzzardo.android.willyshmo.kotlintictacdoh.MainActivity.UserPreferences
import com.guzzardo.android.willyshmo.kotlintictacdoh.WebServerInterface.converseWithWebServer
import org.json.JSONException
import org.json.JSONObject

class WebServerInterfaceNewPlayerTask : AsyncTask<Any?, Void?, Int?>() {
    private var mCallerActivity: PlayOverNetwork? = null
    private var mApplicationContext: Context? = null
    private var mPlayer1Name: String? = null
    protected override fun doInBackground(vararg params: Any?): Int? {
        var player1Id = 0
        mCallerActivity = params[0] as PlayOverNetwork
        val url = params[1] as String
        mPlayer1Name = params[2] as String
        mApplicationContext = params[3] as Context
        mResources = params[4] as Resources
        writeToLog("WebServerInterfaceNewPlayerTask", "doInBackground called")
        try {
            val newUser = converseWithWebServer(url, mPlayer1Name, mCallerActivity, mResources!!) ?: return null
            player1Id = getNewUserId(newUser)
        } catch (e: Exception) {
//			System.out.println(e.getMessage());
            writeToLog("WebServerInterfaceNewPlayerTask", "doInBackground exception called " + e.message)
            mCallerActivity!!.sendToastMessage(e.message)
        }
        return player1Id
    }

    override fun onPostExecute(player1Id: Int?) {
        try {
            writeToLog("WebServerInterfaceNewPlayerTask", "onPostExecute called player1Id $player1Id")
            if (player1Id == null) {
                return
            }
            val settings = mApplicationContext!!.getSharedPreferences(UserPreferences.PREFS_NAME, 0)
            val editor = settings.edit()
            editor.putInt(GameActivity.PLAYER1_ID, player1Id)
            // Commit the edits!
            editor.apply()
            val webServerInterfaceUsersOnlineTask = WebServerInterfaceUsersOnlineTask()
            webServerInterfaceUsersOnlineTask.execute(mCallerActivity, mApplicationContext, mPlayer1Name, mResources, player1Id)
        } catch (e: Exception) {
            writeToLog("WebServerInterfaceNewPlayerTask", "onPostExecute exception called " + e.message)
            mCallerActivity!!.sendToastMessage(e.message)
        }
    }

    private fun getNewUserId(newUser: String): Int {
        try {
            val jsonObject = JSONObject(newUser)
            val userObject = jsonObject.getJSONObject("User")
            val userId = userObject.getString("id")
            if (null != userId) return userId.toInt()
        } catch (e: JSONException) {
            writeToLog("WebServerInterfaceNewPlayerTask", "getNewUserId exception called " + e.message)
            mCallerActivity!!.sendToastMessage(e.message)
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