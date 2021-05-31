package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.res.Resources
import android.util.Log

class DisposeRabbitMQTask {
    private lateinit var mCallerActivity: ToastMessage
    fun main(rabbitMQMessageConsumer: RabbitMQMessageConsumer?, resources: Resources, activity: ToastMessage) {
        mCallerActivity = activity
        try {
            mResources = resources
            writeToLog("DisposeRabbitMQTask", "about to call rabbitMQMessageConsumer?.dispose()")
            rabbitMQMessageConsumer?.dispose()
            //FIXME - maybe add some logic here to get rid of any old messages in Queue?
        } catch (e: Exception) {
            writeToLog("DisposeRabbitMQTask", e.message)
            activity.sendToastMessage(e.message)
        } finally {
            writeToLog("DisposeRabbitMQTask", "main function all done!")
        }
    }

    companion object {
        var mResources: Resources? = null
        private fun writeToLog(filter: String, msg: String?) {
            if ("true".equals(mResources!!.getString(R.string.debug), ignoreCase = true)) {
                Log.d(filter, msg!!)
            }
        }
    }
}