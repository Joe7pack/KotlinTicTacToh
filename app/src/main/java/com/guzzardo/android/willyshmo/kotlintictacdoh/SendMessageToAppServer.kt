package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.res.Resources
import android.util.Log
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object SendMessageToAppServer {
    private var mToastMessage: ToastMessage? = null
    private var mResources: Resources? = null

    fun main(urlData: String, stringToEncode: String?, callerActivity: ToastMessage,  resources: Resources, finishActivity: Boolean): String {
        mToastMessage = callerActivity
        mResources = resources
        val url = mResources!!.getString(R.string.domainName) + urlData
        var `is`: InputStream? = null
        var result: String? = null
        var errorAt: String? = null
        var urlConnection: HttpURLConnection? = null
        var exceptionMessage:String ? = null

        try {
            val myURL = if (stringToEncode == null) {
                URL(url)
            } else {
                val encodedUrl = URLEncoder.encode(stringToEncode, "UTF-8")
                URL(url + encodedUrl)
            }
            errorAt = "openConnection"
            urlConnection = myURL.openConnection() as HttpURLConnection
            /* Define InputStreams to read from the URLConnection. */
            errorAt = "getInputStream"
            `is` = urlConnection.getInputStream()
            errorAt = "convertStreamToString"
            result = convertStreamToString(`is`)
        } catch (e: Exception) {
            writeToLog("SendMessageToAppServer", "error: " + e.message + " error at: " + errorAt)
            mToastMessage!!.sendToastMessage("SendMessageToAppServer error: " + e.message + " at $errorAt")
            exceptionMessage = e.message
        } finally {
            try {
                `is`!!.close()
                if (urlConnection != null) {
                    urlConnection.disconnect()
                }
            } catch (e: Exception) {
                //nothing to do here
                writeToLog(
                    "SendMessageToAppServer",
                    "finally error: " + e.message
                )
            }
        onPostExecute(finishActivity)
        return "Hi there Joseph" //exceptionMessage // result
        }
    }

    fun onPostExecute(finishActivity: Boolean) {
        try {
            if (finishActivity) {
                mToastMessage?.finish()
            }
        } catch (e: Exception) {
            mToastMessage?.sendToastMessage(e.message)
        }
    }

    //@JvmStatic
    private fun convertStreamToString(`is`: InputStream?): String {
        val reader = BufferedReader(InputStreamReader(`is`))
        val sb = StringBuilder()
        try {
            val allText = `is`?.bufferedReader()?.readText() //. .use(BufferedReader::readText)
            sb.append(allText)
        } catch (e: IOException) {
            writeToLog("SendMessageToAppServer", "IOException: " + e.message)
            mToastMessage!!.sendToastMessage(e.message)
        } catch (e: Exception) {
            writeToLog("SendMessageToAppServer", "Exception: " + e.message)
            mToastMessage!!.sendToastMessage(e.message)
        } finally {
            try {
                reader.close()
            } catch (e: IOException) {
                writeToLog("SendMessageToAppServer", "is close IOException: " + e.message)
                mToastMessage!!.sendToastMessage(e.message)
            }
        }
        return sb.toString()
    }

    private fun writeToLog(filter: String, msg: String) {
        if ("true".equals(mResources!!.getString(R.string.debug), ignoreCase = true)) {
            Log.d(filter, msg)
        }
    }

}