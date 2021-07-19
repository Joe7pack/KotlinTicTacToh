package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.res.Resources
import android.util.Log
import kotlinx.coroutines.runBlocking
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SendMessageToWillyShmoServer {
    private lateinit var mCallerActivity: ToastMessage

    fun main(urlData: String, urlToEncode: String?, callerActivity: ToastMessage,  resources: Resources, finishActivity: Boolean)  = runBlocking {
        mCallerActivity = callerActivity
        mResources = resources
        mFinishActivity = finishActivity
        val url = mResources!!.getString(R.string.domainName) + urlData
        var bis: BufferedInputStream? = null
        var `is`: InputStream? = null
        var result: String? = null
        var errorAt: String? = null
        var urlConnection: HttpURLConnection? = null
        try {
            val myURL = if (urlToEncode == null) {
                URL(url)
            } else {
                val encodedUrl = URLEncoder.encode(urlToEncode, "UTF-8")
                URL(url + encodedUrl)
            }
            errorAt = "openConnection"
            urlConnection = myURL.openConnection() as HttpURLConnection
            /* Define InputStreams to read from the URLConnection. */
            errorAt = "getInputStream"
            `is` = urlConnection.getInputStream()
            errorAt = "bufferedInputStream"
            bis = BufferedInputStream(`is`)
            errorAt = "convertStreamToString"
            result = convertStreamToString(`is`)
            /* Convert the Bytes read to a String. */
        } catch (e: Exception) {
            writeToLog("SendMessageToWillyShmoServer","error: " + e.message + " error at: " + errorAt)
            mCallerActivity.sendToastMessage("SendMessageToWillyShmoServer error at $errorAt")
        } finally {
            try {
                if (urlConnection != null) {
                    urlConnection.disconnect()
                }
                bis!!.close()
                `is`!!.close()
            } catch (e: Exception) {
                //nothing to do here
                writeToLog("SendMessageToWillyShmoServer", "finally error: " + e.message)
            }
        }
        onPostExecute(finishActivity)
        //return result
    }

    fun onPostExecute(finishActivity: Boolean) {
        try {
            if (finishActivity) {
                mCallerActivity.finish()
            }
        } catch (e: Exception) {
            mCallerActivity.sendToastMessage(e.message)
        }
    }

    companion object {
         private var mCallerActivity: ToastMessage? = null
         private var mResources: Resources? = null
         private var mFinishActivity: Boolean? = null
         private fun convertStreamToString(`is`: InputStream?): String {
            /*
             * To convert the InputStream to String we use the BufferedReader.readLine()
             * method. We iterate until the BufferedReader return null which means
             * there's no more data to read. Each line will appended to a StringBuilder
             * and returned as String.
             */
         val reader = BufferedReader(InputStreamReader(`is`))
         val sb = StringBuilder()
         try {
             val allText = `is`?.bufferedReader()?.readText() //. .use(BufferedReader::readText)
             sb.append(allText)
             /*
             while (reader.readLine().also { line = it } != null) {
                 sb.append(""" $line """.trimIndent())
             }
             */
         } catch (e: IOException) {
             writeToLog("SendMessageToWillyShmoServer","IOException: " + e.message)
             mCallerActivity!!.sendToastMessage(e.message)
         } catch (e: Exception) {
             writeToLog("SendMessageToWillyShmoServer", "Exception: " + e.message)
             mCallerActivity!!.sendToastMessage(e.message)
         } finally {
             try {
                 reader.close()
                 `is`!!.close()
             } catch (e: IOException) {
                 writeToLog("SendMessageToWillyShmoServer", "is close IOException: " + e.message)
                 mCallerActivity!!.sendToastMessage(e.message)
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
}