package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.res.Resources
import android.util.Log
import kotlinx.coroutines.*

class SendMessageToRabbitMQ {
    private var mCallingActivity: ToastMessage? = null
    fun main(rabbitMQConnection: RabbitMQConnection?, qName: String, message: String, callerActivity: ToastMessage, resources: Resources?) = runBlocking {
        try {
            mCallingActivity = callerActivity
            mResources = resources
            //val connection = rabbitMQConnection?.connection
            val channel = rabbitMQConnection?.channel
            channel?.queueDeclare(qName, false, false, false, null)
//			channel.basicPublish(EXCHANGE_NAME, QUEUE_NAME, null, tempstr.getBytes());
            channel?.basicPublish("", qName, null, message.toByteArray())
            writeToLog("SendMessageToRabbitMQTask", "message: $message to queue: $qName")
            //channel.close()
            //connection.close()
        } catch (e: Exception) {
            writeToLog("SendMessageToRabbitMQTask", "Exception: " + e.message)
            mCallingActivity!!.sendToastMessage(e.message)
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