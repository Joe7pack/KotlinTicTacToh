package com.guzzardo.android.willyshmo.kotlintictacdoh

import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.mLatitude
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.mLongitude
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.willyShmoApplicationContext
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import com.google.android.gms.ads.MobileAds
import android.content.Intent
import android.view.MotionEvent
import android.widget.Toast
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.common.GoogleApiAvailability
import java.lang.Exception
import java.util.*
import kotlinx.coroutines.*

/*
Splash screen starts:

calls FusedLocationActivity via intent
FusedLocationActivity gets Location permissions and then gets the Location
FusedLocationActivity then calls GetPrizeListTask via an async call
GetPrizeListTask then calls MainActivity which displays the screen showing the load prizes button
LoadPrizesTask is no longer used
*/

class SplashScreen : Activity(), ToastMessage {
    protected var mActive = true
    var MSG_KEY = "message key"
    lateinit var mCallerActivity: Activity //= null //= super.getCallingActivity()
    /**
     * perform the action in `handleMessage` when the thread calls
     * `mHandler.sendMessage(msg)`
     */
    //@SuppressLint("HandlerLeak")
    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val bundle = msg.data
            val string = bundle.getString(MSG_KEY)
            val myTextView = findViewById<View>(R.id.textView) as TextView
            myTextView.text = string
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mApplicationContext = applicationContext
        MobileAds.initialize(this) { }
        mErrorHandler = ErrorHandler()
        var mPrizesAvailable = false
        if ("true".equals(mResources?.getString(R.string.prizesAvailable), ignoreCase = true)) {
            mPrizesAvailable = true
        }
        mLatitude = 0.0
        mLongitude = 0.0
        mCallerActivity = this
        willyShmoApplicationContext = this.applicationContext
        WillyShmoApplication.prizesAreAvailable = false
        val willyShmoApplicationContext = willyShmoApplicationContext
        val myIntent = Intent(willyShmoApplicationContext, FusedLocationActivity::class.java)
        startActivity(myIntent)
        writeToLog("SplashScreen", "onCreate finished")
    }

    public override fun onStart() {
        super.onStart()
        CoroutineScope( Dispatchers.Default).launch {
            val getConfigurationValuesFromDB = GetConfigurationValuesFromDB()
            getConfigurationValuesFromDB.main(mCallerActivity as ToastMessage, resources)
        }
    }

    public override fun onResume() {
        super.onResume()
        finish()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            setSplashActive(false)
        }
        return true
    }

    class ErrorHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            Toast.makeText(mApplicationContext, msg.obj as String, Toast.LENGTH_LONG).show()
        }
    }

    override fun sendToastMessage(message: String?) {
        val msg = mErrorHandler!!.obtainMessage()
        msg.obj = message
        mErrorHandler!!.sendMessage(msg)
    }

    fun showGooglePlayError(isPlayAvailable: Int, playErrorMessage: String?) {
        try {
            val dialog = createGooglePlayErrorDialog(isPlayAvailable, playErrorMessage)
            dialog.show()
        } catch (e: Exception) {
            sendToastMessage(e.message)
        }
    }

    private fun createGooglePlayErrorDialog(isPlayAvailable: Int, playErrorMessage: String?): AlertDialog {
        return AlertDialog.Builder(this@SplashScreen)
            .setIcon(R.drawable.willy_shmo_small_icon)
            .setTitle(R.string.google_play_service_error)
            .setPositiveButton(R.string.ok) { _, _ ->
                callGooglePlayServicesUtil(isPlayAvailable)
                setSplashActive(false)
            }
            .setNegativeButton(R.string.alert_dialog_cancel) { _, _ ->
                setSplashActive(false)
            }
            .setMessage(playErrorMessage)
            .create()
    }

    private fun callGooglePlayServicesUtil(isPlayAvailable: Int) {
        //GooglePlayServicesUtil.getErrorDialog(isPlayAvailable, this@SplashScreen, 99)
        GoogleApiAvailability.getInstance().getErrorDialog(this, isPlayAvailable, 777)
    }

    private fun setSplashActive(active: Boolean) {
        mActive = active
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onStop() {
        super.onStop()
        setSplashActive(false)
    }

    public override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        private var mApplicationContext: Context? = null
        var mErrorHandler: ErrorHandler? = null
        private var mResources: Resources? = null

        private fun writeToLog(filter: String, msg: String) {
            if ("true".equals(mResources?.getString(R.string.debug), ignoreCase = true))
            { Log.d(filter, msg) }
        }
    }
}