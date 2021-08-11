package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.res.Resources
import android.util.Log
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object SendMessageToAppServer {
    private var mToastMessage: ToastMessage? = null
    private var mResources: Resources? = null

    fun main(urlData: String, toastMessage: ToastMessage, resources: Resources, finishActivity: Boolean): String {
        mToastMessage = toastMessage
        mResources = resources
        val url = mResources!!.getString(R.string.domainName) + urlData
        val inputStream: InputStream?
        var result = "noResultReturned"
        var errorAt: String? = null
        var httpUrlConnection: HttpURLConnection? = null
        var networkAvailable = false
        var responseCode = 0
        val beginTime = System.currentTimeMillis()

        try {
            val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
            //writeToLog("SendMessageToAppServer", "main() called at time: $dateTime with message: $urlData")
            val myURL = URL(url)
            errorAt = "openConnection"
            httpUrlConnection = myURL.openConnection() as HttpURLConnection
            httpUrlConnection.requestMethod = "POST"
            httpUrlConnection.connectTimeout = 3000
            responseCode = httpUrlConnection.responseCode
            errorAt = "getInputStream"
            inputStream = httpUrlConnection.inputStream
            errorAt = "convertStreamToString"
            result = convertStreamToString(inputStream) // convert the Bytes read to a String.
            networkAvailable = true
        } catch (e: Exception) {
            writeToLog("SendMessageToAppServer", "response code: $responseCode error: ${e.message}  message error at: $errorAt url: $url")
            mToastMessage!!.sendToastMessage("SendMessageToAppServer error: " + e.message + " at $errorAt")
        } finally {
            try {
                httpUrlConnection?.disconnect()
            } catch (e: Exception) {
                //nothing to do here
                writeToLog("SendMessageToAppServer", "finally error: " + e.message)
            }
            onPostExecute(finishActivity)
            WillyShmoApplication.isNetworkAvailable = networkAvailable
        }
        val endTime = System.currentTimeMillis()
        writeToLog("SendMessageToAppServer", "main() url: $url elapsed time: ${endTime-beginTime} result: $result")
        return result
    }

    private fun onPostExecute(finishActivity: Boolean) {
        try {
            if (finishActivity) {
                mToastMessage?.finish()
            }
        } catch (e: Exception) {
            mToastMessage?.sendToastMessage(e.message)
        }
    }

    private fun convertStreamToString(inputStream: InputStream?): String {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val sb = StringBuilder()
        try {
            val allText = inputStream?.bufferedReader()?.readText()
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
                inputStream!!.close()
            } catch (e: Exception) {
                writeToLog("SendMessageToAppServer", "is close Exception: " + e.message)
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