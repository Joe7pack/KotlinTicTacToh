package com.guzzardo.android.willyshmo.kotlintictacdoh

import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.latitude
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.longitude
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.callerActivity
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.willyShmoApplicationContext
import android.app.Activity
import com.guzzardo.android.willyshmo.kotlintictacdoh.ToastMessage
import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import com.guzzardo.android.willyshmo.kotlintictacdoh.R
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.google.android.gms.ads.initialization.InitializationStatus
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication
import android.content.Intent
import com.guzzardo.android.willyshmo.kotlintictacdoh.FusedLocationActivity
import com.guzzardo.android.willyshmo.kotlintictacdoh.GetConfigurationValuesFromDB
import android.view.MotionEvent
import android.widget.Toast
import android.content.DialogInterface
import android.content.res.Resources
import android.os.Handler
import android.os.Message
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.common.GooglePlayServicesUtil
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

//import static com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.companion; . .Companion.getWillyShmoApplicationContext;
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

    /**
     * perform the action in `handleMessage` when the thread calls
     * `mHandler.sendMessage(msg)`
     */
    @SuppressLint("HandlerLeak")
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            val bundle = msg.data
            val string = bundle.getString(MSG_KEY)
            val myTextView = findViewById<View>(R.id.textView) as TextView
            myTextView.text = string
        }
    }
    private val mMessageSender = Runnable {
        val msg = mHandler.obtainMessage()
        val bundle = Bundle()
        bundle.putString(MSG_KEY, currentTime)
        msg.data = bundle
        mHandler.sendMessage(msg)
    }
    private val currentTime: String
        private get() {
            val dateFormat = SimpleDateFormat("HH:mm:ss MM/dd/yyyy", Locale.US)
            return dateFormat.format(Date())
        }

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) { }
        mResources = resources
        mErrorHandler = ErrorHandler()
        var mPrizesAvailable = false
        if ("true".equals(mResources?.getString(R.string.prizesAvailable), ignoreCase = true)) {
            mPrizesAvailable = true
        }
        latitude = 0.0
        longitude = 0.0
        callerActivity = this@SplashScreen
        willyShmoApplicationContext = this.applicationContext
        //WillyShmoApplication.Companion.setPrizeNames(null);
        if (mPrizesAvailable) {
            //new LoadPrizesTask().execute(SplashScreen.this, getApplicationContext(), getResources());
            //mSkipWaitCheck = true;
        }

        //Context willyShmoApplicationContext = getWillyShmoApplicationContext();
        val willyShmoApplicationContext = willyShmoApplicationContext
        val myIntent = Intent(willyShmoApplicationContext, FusedLocationActivity::class.java)
        startActivity(myIntent)
    }

    public override fun onStart() {
        super.onStart()
        val getConfigurationValuesFromDB = GetConfigurationValuesFromDB()
        getConfigurationValuesFromDB.execute(this, applicationContext, resources)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            setSplashActive(false)
        }
        return true
    }

    inner class ErrorHandler : Handler() {
        override fun handleMessage(msg: Message) {
            Toast.makeText(applicationContext, msg.obj as String, Toast.LENGTH_LONG).show()
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

    fun createGooglePlayErrorDialog(isPlayAvailable: Int, playErrorMessage: String?): AlertDialog {
        return AlertDialog.Builder(this@SplashScreen)
            .setIcon(R.drawable.willy_shmo_small_icon)
            .setTitle(R.string.google_play_service_error)
            .setPositiveButton(R.string.alert_dialog_ok) { dialog, whichButton -> /* User clicked OK so do some stuff */
                callGooglePlayServicesUtil(isPlayAvailable)
                setSplashActive(false)
            }
            .setNegativeButton(R.string.alert_dialog_cancel) { dialog, whichButton -> /* User clicked Cancel so do some stuff */
                setSplashActive(false)
            }
            .setMessage(playErrorMessage)
            .create()
    }

    private fun callGooglePlayServicesUtil(isPlayAvailable: Int) {
        GooglePlayServicesUtil.getErrorDialog(isPlayAvailable, this@SplashScreen, 99)
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
        private const val mSkipWaitCheck = false
        private const val mSplashTime = 2500
        var mErrorHandler: ErrorHandler? = null
        private var mResources: Resources? = null
    }
}