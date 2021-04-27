package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.res.Resources
import android.os.AsyncTask
import android.util.Log

class DisposeRabbitMQTask : AsyncTask<Any?, Void?, Void?>() {
    var mActivity: ToastMessage? = null
    protected override fun doInBackground(vararg values: Any?): Void? {
        try {
            val rabbitMQMessageConsumer = values[0] as RabbitMQMessageConsumer
            mResources = values[1] as Resources
            mActivity = values[2] as ToastMessage
            //rabbitMQMessageConsumer.setConsumeRunning(false);
            rabbitMQMessageConsumer.dispose()
        } catch (e: Exception) {
            writeToLog("DisposeRabbitMQTask", e.message)
            mActivity!!.sendToastMessage(e.message)
        }
        return null
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