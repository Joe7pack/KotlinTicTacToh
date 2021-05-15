package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.vending.licensing.*
import com.google.firebase.FirebaseApp
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.appindexing.Indexable
import com.google.firebase.appindexing.builders.Actions
import com.google.firebase.appindexing.builders.Indexables
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.prizesAreAvailable
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity(), ToastMessage {
    private var mPlayer1Name: String? = null
    private var mPlayer2Name: String? = null
    private var mPrizeButton: Button? = null
    private val mText = "Joes text here"
    private val mUrl = "Joes url here"
    private var mAdView: AdView? = null
    private var mStatusText: TextView? = null
    private var mCheckLicenseButton: Button? = null
    private var mLicenseCheckerCallback: LicenseCheckerCallback? = null
    private var mChecker: LicenseChecker? = null

    // A handler on the UI thread.
    private var mHandler: Handler? = null
    public override fun onStart() {
        super.onStart()
        val indexableNotes = ArrayList<Indexable>()
        val noteToIndex = Indexables.noteDigitalDocumentBuilder()
            .setName("Joes name Note")
            .setText("Joes text here")
            .setUrl("joe.guzzardo.com")
            .build()
        indexableNotes.add(noteToIndex)
        var notesArr = arrayOfNulls<Indexable>(indexableNotes.size)
        notesArr = indexableNotes.toArray(notesArr)
        FirebaseApp.initializeApp(this)
        FirebaseAppIndex.getInstance(this).update(*notesArr)
        FirebaseUserActions.getInstance(this).start(action)
        Log.d("MainActivity", "onStart called at " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()))
    }

    // After
    val action: Action
        get() = Actions.newView(mText, mUrl)

    public override fun onStop() {
        FirebaseUserActions.getInstance(this).end(action)
        super.onStop()
    }

    public override fun onDestroy() {
        super.onDestroy()
        mChecker!!.onDestroy()
    }

    // Acquire a reference to the system Location Manager
    interface UserPreferences {
        companion object {
            const val PREFS_NAME = "TicTacDohPrefsFile"
        }
    }

    // Called when the activity is first created.
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        setContentView(R.layout.main)
        mErrorHandler = ErrorHandler()
        findViewById<View>(R.id.rules).setOnClickListener { showRules() }
        findViewById<View>(R.id.about).setOnClickListener { showAbout() }
        findViewById<View>(R.id.two_player).setOnClickListener { showTwoPlayers() }
        findViewById<View>(R.id.one_player).setOnClickListener { showOnePlayer() }
        findViewById<View>(R.id.settings_dialog).setOnClickListener { showDialogs() }
        mPrizeButton = findViewById<View>(R.id.prizes_dialog) as Button
        mPrizeButton!!.setOnClickListener { showPrizes() }
        mStatusText = findViewById<View>(R.id.status_text) as TextView
        mCheckLicenseButton = findViewById<View>(R.id.check_license_button) as Button
        mCheckLicenseButton!!.setOnClickListener { doCheck() }

        //FIXME - set animation for Prizes button only if we are connected to the network
        val anim: Animation = AlphaAnimation(0.0f, 1.0f)
        anim.duration = 500 //You can manage the time of the blink with this parameter
        anim.startOffset = 20
        anim.repeatMode = Animation.REVERSE
        anim.repeatCount = Animation.INFINITE
        mPrizeButton!!.setBackground(AppCompatResources.getDrawable(getApplicationContext(), R.drawable.backwithgreenborder))
        mPrizeButton!!.startAnimation(anim)
        if (prizesAreAvailable) {
            mPrizeButton!!.visibility = View.VISIBLE
        } else {
            mPrizeButton!!.visibility = View.GONE
        }
        mAdView = findViewById<View>(R.id.adView) as AdView
        val adRequest =
            AdRequest.Builder() //.addTestDevice("EE90BD2A7578BC19014DE8617761F10B") //Samsung Galaxy Note
                // Create an ad request. Check your logcat output for the hashed device ID to
                // get test ads on a physical device. e.g.
                // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
                //.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build() //mAdView.loadAd(adRequest);

        // Start loading the ad in the background.
        mAdView!!.loadAd(adRequest)

        //adRequest.addTestDevice(AdRequest.TEST_EMULATOR);             // Android emulator
        //adRequest.addTestDevice("5F310740585B99B1179370AC1B4490C4"); // My T-Mobile G1 Test Phone
        //adRequest.addTestDevice("EE90BD2A7578BC19014DE8617761F10B");  // My Samsung Note
        mHandler = Handler(Looper.getMainLooper())

        // Try to use more data here. ANDROID_ID is a single point of attack.
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        // Library calls this when it's done.
        mLicenseCheckerCallback = MyLicenseCheckerCallback()
        // Construct the LicenseChecker with a policy.
        mChecker = LicenseChecker(
            this, ServerManagedPolicy(
                this,
                AESObfuscator(SALT, packageName, deviceId)
            ),
            BASE64_PUBLIC_KEY
        )
    }

    private fun showRules() {
        val i = Intent(this, RulesActivity::class.java)
        startActivity(i)
    }

    private fun showAbout() {
        val i = Intent(this, AboutActivity::class.java)
        startActivity(i)
    }

    private fun showOnePlayer() {
        val i = Intent(this, OnePlayerActivity::class.java)
        i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name)
        i.putExtra(GameActivity.PLAYER2_NAME, mPlayer2Name)
        startActivity(i)
    }

    private fun showTwoPlayers() {
        val i = Intent(this, TwoPlayerActivity::class.java)
        i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name)
        i.putExtra(GameActivity.PLAYER2_NAME, mPlayer2Name)
        startActivity(i)
    }

    private fun showDialogs() {
        val i = Intent(this, SettingsDialogs::class.java)
        i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name)
        i.putExtra(GameActivity.PLAYER2_NAME, mPlayer2Name)
        startActivity(i)
    }

    private fun showPrizes() {
        val i = Intent(this, PrizesAvailableActivity::class.java)
        startActivity(i)
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putString("gm_player1_name", mPlayer1Name)
        savedInstanceState.putString("gm_player2_name", mPlayer2Name)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        mPlayer1Name = savedInstanceState.getString("gm_player1_name")
        mPlayer2Name = savedInstanceState.getString("gm_player2_name")
    }

    override fun onResume() {
        super.onResume()
        // Restore preferences
        val settings = getSharedPreferences(UserPreferences.PREFS_NAME, MODE_PRIVATE)
        mPlayer1Name = settings.getString(GameActivity.PLAYER1_NAME, "Player 1")
        mPlayer2Name = settings.getString(GameActivity.PLAYER2_NAME, "Player 2")
    }

    override fun onPause() {
        super.onPause()
    }

    inner class ErrorHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            Toast.makeText(applicationContext, msg.obj as String, Toast.LENGTH_LONG).show()
        }
    }

    override fun sendToastMessage(message: String?) {
        val msg = mErrorHandler!!.obtainMessage()
        msg.obj = message
        mErrorHandler!!.sendMessage(msg)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        writeToLog("MainActivity", "MainActivity onActivityResult")

    }

    private fun writeToLog(filter: String, msg: String) {
        if ("true".equals(resources!!.getString(R.string.debug), ignoreCase = true)) {
            Log.d(filter, msg)
        }
    }

    override fun onCreateDialog(id: Int): Dialog {
        val bRetry = id == 1
        return AlertDialog.Builder(this)
            .setTitle(R.string.unlicensed_dialog_title)
            .setMessage(if (bRetry) R.string.unlicensed_dialog_retry_body else R.string.unlicensed_dialog_body)
            .setPositiveButton(
                if (bRetry) R.string.retry_button else R.string.buy_button,
                object : DialogInterface.OnClickListener {
                    var mRetry = bRetry
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        if (mRetry) {
                            doCheck()
                        } else {
                            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://market.android.com/details?id=$packageName"))
                            startActivity(marketIntent)
                        }
                    }
                })
            .setNegativeButton(R.string.quit_button) { dialog, which -> finish() }.create()
    }

    private fun doCheck() {
        mCheckLicenseButton!!.isEnabled = false
        //setProgressBarIndeterminateVisibility(true)
        //setSupportProgressBarIndeterminateVisibility(true) //see requestWindowFeature call above
        mStatusText!!.setText(R.string.checking_license)
        mChecker!!.checkAccess(mLicenseCheckerCallback)
    }

    private fun displayResult(result: String) {
        mHandler!!.post {
            mStatusText!!.text = result
            //setProgressBarIndeterminateVisibility(false)
            mCheckLicenseButton!!.isEnabled = true
        }
    }

    private fun displayDialog(showRetry: Boolean) {
        mHandler!!.post {
            //setProgressBarIndeterminateVisibility(false)
            //showDialog(if (showRetry) 1 else 0)
            mCheckLicenseButton!!.isEnabled = true
        }
    }

    private inner class MyLicenseCheckerCallback : LicenseCheckerCallback {
        override fun allow(policyReason: Int) {
            if (isFinishing) {
                // Don't update UI if Activity is finishing.
                return
            }
            // Should allow user access.
            displayResult(getString(R.string.allow))
        }

        override fun dontAllow(policyReason: Int) {
            if (isFinishing) {
                // Don't update UI if Activity is finishing.
                return
            }
            displayResult(getString(R.string.dont_allow))
            // Should not allow access. In most cases, the app should assume
            // the user has access unless it encounters this. If it does,
            // the app should inform the user of their unlicensed ways
            // and then either shut down the app or limit the user to a
            // restricted set of features.
            // In this example, we show a dialog that takes the user to Market.
            // If the reason for the lack of license is that the service is
            // unavailable or there is another problem, we display a
            // retry button on the dialog and a different message.
            displayDialog(policyReason == Policy.RETRY)
        }

        override fun applicationError(errorCode: Int) {
            if (isFinishing) {
                // Don't update UI if Activity is finishing.
                return
            }
            // This is a polite way of saying the developer made a mistake
            // while setting up or calling the license checker library.
            // Please examine the error code and fix the error.
            //String result = String.format(getString(R.string.application_error, errorCode);
            val result = " applicationError: $errorCode"
            displayResult(result)
        }
    }

    companion object {
        private lateinit var mResources: Resources
        private const val mLongitude = 0.0
        private const val mLatitude = 0.0
        private const val CONNECTION_FAILURE_RESOLUTION_REQUEST = 1
        var mErrorHandler: ErrorHandler? = null
        private const val BASE64_PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt6bYx4PqPNnRxsW9DuBAOarpGA6ds7v866szk3e28yIF5LjV/EValnRMLsRylX8FP+BEYeGZvB6THbiQ5Gm7H8i+S2tUv6sngc894hBWZnQKAmwrwgl0Zm+vtYo8fnI6jppIxX4A9+4TrzW+Onl4LeW3kafJ9nIa3P73xSLhtFoxbGjBlEVhUQDVkRl27RXC5LuyULWzsYaUOCI9Yyf06DeDlahl2SwkRoTyB0+LdYsmp0fmw49OsW6P4FkLKvo3UGl75EZyTm3vd8oze4NXNy9GiSxpfD12jhtToKDub/qd7EMJrFadUkuGoTg/qQtmDk4YVoWJvLb26KcUH51PdQIDAQAB"

        // Generate your own 20 random bytes, and put them here.
        private val SALT = byteArrayOf(
            -26, 85, 30, -128, -112, -57, 74, -64, 32, 88, -90, -45, 88, -117, -36, -113, -11, 32, -61, 89
        )
    }
}