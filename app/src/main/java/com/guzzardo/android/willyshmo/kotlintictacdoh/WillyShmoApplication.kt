package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.provider.Settings
import android.util.Log
import androidx.multidex.MultiDexApplication
import com.google.android.gms.common.api.GoogleApiClient
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class WillyShmoApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        mResources = resources
        mConfigMap = HashMap<String?, String?>()
    }

    companion object {
        private lateinit var mPrizeNames: Array<String?>
        //private lateinit var mPrizeNames: Array<String>? = null


            private lateinit var mPrizeIds: Array<String?>
            private lateinit  var mBitmapImages: Array<Bitmap?>
            lateinit var imageWidths: Array<String?>
            lateinit var imageHeights: Array<String?>
            lateinit var prizeDistances: Array<String?>
            lateinit var prizeUrls: Array<String?>
            lateinit var prizeLocations: Array<String?>
            private var mConfigMap: HashMap<String?, String?>? = null
            var longitude = 0.0
            var latitude = 0.0
            private var mNetworkAvailable = false
            var callerActivity: ToastMessage? = null
            private var mResources: Resources? = null
            private var mStartMainActivity = false
            var androidId: String? = null
            private set

            var willyShmoApplicationContext: Context? = null
            private var mGoogleApiClient: GoogleApiClient? = null
            @JvmStatic

            /*
            fun getPrizeNames(prizeNames: Array<String>? ) {
                mPrizeNames = prizeNames
            }

             */
            var prizeNames: Array<String?>
            get() = mPrizeNames
            set(prizeNames) {
                mPrizeNames = prizeNames
            }

            var prizeIds: Array<String?>
            get() = mPrizeIds
            set(prizeIds) {
                mPrizeIds = prizeIds
            }

            var bitmapImages: Array<Bitmap?>
            get() = mBitmapImages
            set(bitmapImages) {
                mBitmapImages = bitmapImages
            }

            var isNetworkAvailable: Boolean
            get() = mNetworkAvailable
            set(networkAvailable) {
                mNetworkAvailable = networkAvailable
                writeToLog("WillyShmoApplication","setNetworkAvailable(): $mNetworkAvailable")
            }

            fun setMainStarted(startMainActivity: Boolean) {
                mStartMainActivity = startMainActivity
            }

            private fun writeToLog(filter: String, msg: String) {
                if ("true".equals(mResources!!.getString(R.string.debug), ignoreCase = true)) {
                    Log.d(filter, msg)
                }
            }

            fun setConfigMap(key: String?, value: String?) {
                mConfigMap!![key] = value
            }

            @JvmStatic
            fun getConfigMap(key: String?): String? {
                return mConfigMap!![key]
            }

            fun setGoogleApiClient(googleApiClient: GoogleApiClient?) {
                mGoogleApiClient = googleApiClient
            }
        }

}