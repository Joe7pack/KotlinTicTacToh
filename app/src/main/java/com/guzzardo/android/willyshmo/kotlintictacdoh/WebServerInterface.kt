package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.res.Resources
import android.util.Log
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object WebServerInterface {
    private var mToastMessage: ToastMessage? = null
    private var mResources: Resources? = null
    @JvmStatic
	fun converseWithWebServer(url: String, urlToEncode: String?, toastMessage: ToastMessage?, resources: Resources ): String? {
        var bis: BufferedInputStream? = null
        var `is`: InputStream? = null
        var result: String? = null
        mResources = resources
        mToastMessage = toastMessage
        var errorAt: String? = null
        var responseCode = 0
        var networkAvailable = false
        try {
            writeToLog("WebServerInterface", "converseWithWebServer() called")
            val myURL = if (urlToEncode == null) {
                URL(url)
            } else {
                val encodedUrl = URLEncoder.encode(urlToEncode, "UTF-8")
                URL(url + encodedUrl)
            }
            errorAt = "openConnection"
            val httpUrlConnection = myURL.openConnection() as HttpURLConnection
            httpUrlConnection.requestMethod = "POST"
            httpUrlConnection.connectTimeout = 3000
            responseCode = httpUrlConnection.responseCode
            errorAt = "getInputStream"
            `is` = httpUrlConnection.inputStream // define InputStreams to read from the URLConnection.
            errorAt = "bufferedInputStream"
            bis = BufferedInputStream(`is`)
            errorAt = "convertStreamToString"
            result = convertStreamToString(`is`) // convert the Bytes read to a String.
            networkAvailable = true
        } catch (e: Exception) {
            writeToLog("WebServerInterface", "response code: $responseCode error: $e.message error at: $errorAt")
            val networkNotAvailable = resources.getString(R.string.network_not_available)
            mToastMessage!!.sendToastMessage(networkNotAvailable)
        } finally {
            try {
                bis!!.close()
                `is`!!.close()
            } catch (e: Exception) {
                //nothing to do here
                writeToLog("WebServerInterface", "finally exception: $e.message")
            }
        }
        WillyShmoApplication.isNetworkAvailable = networkAvailable
        return result
    }

    private fun convertStreamToString(`is`: InputStream?): String {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        val reader = BufferedReader(InputStreamReader(`is`))
        val sb = StringBuilder()
        var line: String
        try {
            while (reader.readLine().also { line = it } != null) {
                sb.append(""" $line """.trimIndent())
            }
        } catch (e: IOException) {
            writeToLog( "WebServerInterface", "convertStreamToString IOException: " + e.message)
            if (e.message?.indexOf("it must not be null", 0)!! == -1)
                mToastMessage!!.sendToastMessage("convertStreamToString IOException: " + e.message)
        } catch (e: Exception) {
            writeToLog( "WebServerInterface", "convertStreamToString Exception: " + e.message)
            if (e.message?.indexOf("it must not be null", 0)!! == -1)
                mToastMessage!!.sendToastMessage("convertStreamToString Exception: " + e.message)
        } finally {
            try {
                `is`!!.close()
            } catch (e: IOException) {
                writeToLog("WebServerInterface", "is close IOException:: " + e.message)
                mToastMessage!!.sendToastMessage("is close IOException: " + e.message)
            }
        }
        return sb.toString()
    }

    private fun writeToLog(filter: String, msg: String) {
        if ("true".equals(mResources!!.getString(R.string.debug), ignoreCase = true))
            Log.d(filter, msg)
    }
}