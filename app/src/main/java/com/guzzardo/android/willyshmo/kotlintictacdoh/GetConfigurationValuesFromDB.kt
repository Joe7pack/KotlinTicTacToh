package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.*
import kotlinx.coroutines.*

// An AsyncTask that will be used to load Configuration values from the DB

class GetConfigurationValuesFromDB { // : AsyncTask<Any?, Void?, String?>() {
    private lateinit var mCallerActivity: ToastMessage
    private var applicationContext: Context? = null


/*
    override fun doInBackground(vararg params: Any?): String? {
        var configValues: String? = null
        mCallerActivity = params[0] as SplashScreen
        applicationContext = params[1] as Context
        mResources = params[2] as Resources
        val url = mResources!!.getString(R.string.domainName) + "/config/getConfigValues"
        try { configValues = WebServerInterface.converseWithWebServer(url, null, mCallerActivity, mResources!!)
        } catch (e: Exception) {
            writeToLog("GetConfigurationValuesFromDB", "doInBackground: " + e.message)
            mCallerActivity!!.sendToastMessage(e.message)
        }
        writeToLog("GetConfigurationValuesFromDB", "GetConfigurationValuesFromDB doInBackground called usersOnline: $configValues")
        return configValues
    }

 */

    fun main(callerActivity: ToastMessage, resources: Resources) = runBlocking {
        var configValues: String? = null
        mCallerActivity = callerActivity
        mResources = resources
        val url = mResources!!.getString(R.string.domainName) + "/config/getConfigValues"
        try {
            configValues = WebServerInterface.converseWithWebServer(url, null, mCallerActivity, mResources!!)
        } catch (e: Exception) {
            writeToLog("GetConfigurationValuesFromDB", "doInBackground: " + e.message)
            mCallerActivity.sendToastMessage(e.message)
        }
        writeToLog("GetConfigurationValuesFromDB", "GetConfigurationValuesFromDB doInBackground return values: $configValues")
        setConfigValues(configValues);
    }

    fun setConfigValues(configValues: String?) {
        try {
            writeToLog("GetConfigurationValuesFromDB", "onPostExecute called configValues: $configValues")
            val objectMapper = ObjectMapper()
            val result: MutableList<*>? = objectMapper.readValue(configValues, MutableList::class.java)

            if (result != null) {
                for (x in result.indices) {
                    val myHashMap: HashMap<*, *> = result[x] as HashMap<*, *>
                    val key = myHashMap["key"] as String?
                    val value = myHashMap["value"] as String?
                    WillyShmoApplication.setConfigMap(key, value)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            writeToLog("GetConfigurationValuesFromDB", "onPostExecute exception called " + e.message)
            mCallerActivity.sendToastMessage(e.message)
        }
    }

    companion object {
        private var mResources: Resources? = null
        private fun writeToLog(filter: String, msg: String) {
            if ("true".equals(mResources!!.getString(R.string.debug), ignoreCase = true))
                { Log.d(filter, msg) }
        }
    }
}