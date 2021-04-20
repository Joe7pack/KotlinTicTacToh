package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.res.Resources
import android.os.AsyncTask
import android.util.Log
import java.io.*
import java.net.URL
import java.net.URLEncoder

class SendMessageToWillyShmoServer :
    AsyncTask<Any?, Void?, String?>() {
    protected override fun doInBackground(vararg params: Any?): String? {
        val urlToEncode = params[1] as String
        mCallerActivity = params[2] as ToastMessage
        mResources = params[3] as Resources
        mFinishActivity = params[4] as Boolean
        val url = mResources!!.getString(R.string.domainName) +params[0] as String
        var bis: BufferedInputStream? = null
        var `is`: InputStream? = null
        var result: String? = null
        var errorAt: String? = null
        try {
            var myURL: URL? = null
            myURL = if (urlToEncode == null) {
                URL(url)
            } else {
                val encodedUrl = URLEncoder.encode(urlToEncode, "UTF-8")
                URL(url + encodedUrl)
            }
            errorAt = "openConnection"
            val ucon = myURL.openConnection()
            /* Define InputStreams to read 
			 * from the URLConnection. */errorAt = "getInputStream"
            `is` = ucon.getInputStream()
            errorAt = "bufferedInputStream"
            bis = BufferedInputStream(`is`)
            errorAt = "convertStreamToString"
            result = convertStreamToString(`is`)
            /* Convert the Bytes read to a String. */
        } catch (e: Exception) {
            writeToLog("SendMessageToWillyShmoServer","error: " + e.message + " error at: " + errorAt)
            mCallerActivity!!.sendToastMessage("Sorry, Willy Shmo server is not available now. Please try again later")
        } finally {
            try {
                bis!!.close()
                `is`!!.close()
            } catch (e: Exception) {
                //nothing to do here
                writeToLog(
                    "SendMessageToWillyShmoServer",
                    "finally error: " + e.message
                )
            }
        }
        return result
    }

    override fun onPostExecute(res: String?) {
        try {
            if (mFinishActivity!!) {
                mCallerActivity!!.finish()
            }
        } catch (e: Exception) {
            mCallerActivity!!.sendToastMessage(e.message)
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
            var line: String? = null
            try {
                while (reader.readLine().also { line = it } != null) {
                    sb.append(""" $line """.trimIndent())
                }
            } catch (e: IOException) {
                writeToLog("SendMessageToWillyShmoServer","IOException: " + e.message)
                mCallerActivity!!.sendToastMessage(e.message)
            } catch (e: Exception) {
                writeToLog("SendMessageToWillyShmoServer", "Exception: " + e.message)
                mCallerActivity!!.sendToastMessage(e.message)
            } finally {
                try {
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