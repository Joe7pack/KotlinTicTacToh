package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.Intent

interface ToastMessage {
    fun sendToastMessage(message: String?)
    fun finish()
    fun startActivity(i: Intent?)
}