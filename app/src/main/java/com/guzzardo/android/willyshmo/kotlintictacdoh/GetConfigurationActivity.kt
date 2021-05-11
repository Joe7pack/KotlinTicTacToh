package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.widget.Toast

//this class is no longer used

class GetConfigurationActivity : Activity(), ToastMessage {
    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //val getConfigurationValuesFromDB = GetConfigurationValuesFromDB()
        //getConfigurationValuesFromDB.execute(this, applicationContext, resources)

        //getConfigurationValuesFromDB.main(this, applicationContext, resources)



    }

    override fun sendToastMessage(message: String?) {
        val msg = mErrorHandler!!.obtainMessage()
        msg.obj = message
        mErrorHandler!!.sendMessage(msg)
    }

    inner class ErrorHandler : Handler() {
        override fun handleMessage(msg: Message) {
            Toast.makeText(applicationContext, msg.obj as String, Toast.LENGTH_LONG)
                .show()
        }
    }

    companion object {
        var mErrorHandler: ErrorHandler? = null
    }
}