package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.res.Resources
import android.util.Log
import kotlinx.coroutines.*

class CloseRabbitMQConnection {
    private var mCallingActivity: ToastMessage? = null

    fun main(rabbitMQConnection: RabbitMQConnection, callerActivity: ToastMessage, resources: Resources?) = runBlocking {
        try {
            mCallingActivity = callerActivity
            mResources = resources
            rabbitMQConnection.channel?.close()
            rabbitMQConnection.connection?.close()
        } catch (e: Exception) {
            writeToLog("CloseRabbitMQConnection", "Exception: " + e.message)
            mCallingActivity!!.sendToastMessage(e.message)
        }
        finally {
            writeToLog("CloseRabbitMQConnection", "finally executed")
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