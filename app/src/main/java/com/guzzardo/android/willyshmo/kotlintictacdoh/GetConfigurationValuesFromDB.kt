package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.Context
import android.content.res.Resources
import android.os.AsyncTask
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.*

/**
 * An AsyncTask that will be used to load Configuration values from the DB
 */
class GetConfigurationValuesFromDB :
    AsyncTask<Any?, Void?, String?>() {
    //private GetConfigurationActivity mCallerActivity;
    private var mCallerActivity: SplashScreen? = null
    private var applicationContext: Context? = null
    override fun doInBackground(vararg params: Any?): String? {
        var configValues: String? = null
        //mCallerActivity = (GetConfigurationActivity)params[0];
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

    override fun onPostExecute(configValues: String?) {
        try {
            writeToLog("GetConfigurationValuesFromDB", "onPostExecute called configValues: $configValues")
            val objectMapper = ObjectMapper()
            /* original code that was converted to Kotlin by the built in Android Studio Kotlin converter
            val result2: List<HashMap<String, Any>> =
                objectMapper.readValue<List<*>>(
                    configValues,
                    MutableList::class.java
                )
            */

           //suggestions from Stack Overflow: https://stackoverflow.com/questions/52238211/kotlin-objectmapper-readvalue-with-typereferencehashmapstring-string-can
           // val msg = objectMapper.readValue<HashMap<String, String>>(message.payload, typeRef)
           // val msg: HashMap<String, String> = objectMapper.readValue(message.payload, typeRef)
           //and after applying recommendations from code correction suggestions:

            val result: MutableList<*>? = objectMapper.readValue(configValues, MutableList::class.java)

            if (result != null) {
                for (x in result.indices) {
                    val myHashMap: HashMap<*, *> = result?.get(x) as HashMap<*, *>
                    val key = myHashMap["key"] as String?
                    val value = myHashMap["value"] as String?
                    WillyShmoApplication.setConfigMap(key, value)
                }
            }

            //mCallerActivity.setAsyncMessage();
        } catch (e: Exception) {
            e.printStackTrace()
            writeToLog(
                "GetConfigurationValuesFromDB",
                "onPostExecute exception called " + e.message
            )
            mCallerActivity!!.sendToastMessage(e.message)
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