package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.res.Resources
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApi
//import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.*

/** THIS CLASS IS NO LONGER USED
 * An AsyncTask that will be used to find other players currently online
 */
class LoadPrizesTask : AsyncTask<Any?, Void?, Int>(),
    //GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
    LocationListener {
    private var mCallerActivity: SplashScreen? = null
    private var mApplicationContext: Context? = null
    //private var mGoogleApiClient: GoogleApiClient? = null
    private val mLocationRequest: LocationRequest? = null

    // Global variable to hold the current location
    var mCurrentLocation: Location? = null
    var mPlayErrorMessage: String? = null
    override fun doInBackground(vararg params: Any?): Int? {
        mCallerActivity =
            params[0] as SplashScreen
        mApplicationContext = params[1] as Context
        mResources = params[2] as Resources
        WillyShmoApplication.callerActivity = mCallerActivity
        writeToLog(
            "LoadPrizesTask",
            "doInBackground called  at: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(Date())
        )
        /*
        mGoogleApiClient = GoogleApiClient.Builder(mApplicationContext!!)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()

        */
        val api = GoogleApiAvailability.getInstance()
        val isPlayAvailable = api.isGooglePlayServicesAvailable(mApplicationContext)
        when (isPlayAvailable) {
            ConnectionResult.DEVELOPER_ERROR -> mPlayErrorMessage =
                "The application is misconfigured."
            ConnectionResult.INTERNAL_ERROR -> mPlayErrorMessage = "Internal Error"
            ConnectionResult.INVALID_ACCOUNT -> mPlayErrorMessage =
                "Your Google Play account is invalid."
            ConnectionResult.LICENSE_CHECK_FAILED -> mPlayErrorMessage =
                "Google Play is not licensed to this user."
            ConnectionResult.NETWORK_ERROR -> mPlayErrorMessage =
                "A network error has occurred. Please try again later."
            ConnectionResult.RESOLUTION_REQUIRED -> mPlayErrorMessage =
                "Completing the connection requires some form of resolution."
            ConnectionResult.SERVICE_DISABLED -> mPlayErrorMessage =
                "The installed version of Google Play services has been disabled on this device."
            ConnectionResult.SERVICE_INVALID -> mPlayErrorMessage =
                "The version of the Google Play services installed on this device is not authentic."
            ConnectionResult.SERVICE_MISSING -> mPlayErrorMessage =
                "Google Play services is missing on this device."
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> mPlayErrorMessage =
                "Please update Google Play Services"
            ConnectionResult.SIGN_IN_REQUIRED -> mPlayErrorMessage = "Please sign in to Google Play"
        }
        if (mPlayErrorMessage != null) {
            return isPlayAvailable
        }
        if (isPlayAvailable == ConnectionResult.SUCCESS) {
            try {
                writeToLog(
                    "LoadPrizesTask",
                    "isPlayAvailable successful, about to connect at: " + SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss"
                    ).format(Date())
                )
                //mGoogleApiClient.connect()
                //WillyShmoApplication.setGoogleApiClient(mGoogleApiClient)
                WillyShmoApplication.setMainStarted(true)
            } catch (e: Exception) {
                mCallerActivity!!.sendToastMessage(e.message)
            }
        }
        writeToLog(
            "LoadPrizesTask",
            "doInBackground completed at: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(Date())
        )
        return Integer.valueOf(0)
    }

    override fun onPostExecute(isPlayAvailable: Int) {
        try {
            if (isPlayAvailable != ConnectionResult.SUCCESS) {
                mCallerActivity!!.showGooglePlayError(isPlayAvailable, mPlayErrorMessage)
                writeToLog(
                    "LoadPrizesTask",
                    "onPostExecute called play error: $mPlayErrorMessage"
                )
            } else {
                //mCallerActivity.startWaitForPrizesPopup();
                //mCallerActivity.setAsyncMessage2();
                writeToLog(
                    "LoadPrizesTask",
                    "onPostExecute called, waiting for prizes to load from server at: " + SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss"
                    ).format(Date())
                )
            }
        } catch (e: Exception) {
            writeToLog(
                "LoadPrizesTask",
                "onPostExecute exception called " + e.message
            )
            mCallerActivity!!.sendToastMessage(e.message)
        }
        writeToLog(
            "LoadPrizesTask",
            "onPostExecute completed at: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(Date())
        )
    }

    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    fun onConnected(dataBundle: Bundle?) {
        // mCallerActivity.sendToastMessage("Connected to Google Play");
        // Register the listener with the Location Manager to receive location updates
        try {
            val locationManager =
                mCallerActivity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, WillyShmoApplication.getLocationListener());
//    		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } catch (e: SecurityException) {
            writeToLog("LoadPrizesTask", "onConnected error: " + e.message)
            mCallerActivity!!.sendToastMessage(e.message)
        }
        writeToLog(
            "LoadPrizesTask",
            "onConnected completed at: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(Date())
        )
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    //@Override
    // public void onDisconnected() {
    //	mCallerActivity.sendToastMessage("Disconnected from Google Play. Please re-connect.");
    //}
    fun onConnectionSuspended(cause: Int) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    fun onConnectionFailed(connectionResult: ConnectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                    mCallerActivity,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST
                )
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (e: SendIntentException) {
                // Log the error
                //e.printStackTrace();
                mCallerActivity!!.sendToastMessage("onConnectionFailed exception: " + e.message)
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            //FIXME - add showErrorDialog
            //showErrorDialog(connectionResult.getErrorCode());
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        writeToLog("LoadPrizesTask", "onActivityResult called")
    }

    override fun onStatusChanged(
        status: String,
        value: Int,
        bundle: Bundle
    ) {
        writeToLog(
            "LoadPrizesTask",
            "onStatusChanged called status: $status"
        )
    }

    override fun onProviderDisabled(status: String) {
        writeToLog(
            "LoadPrizesTask",
            "onProviderDisabled called status: $status"
        )
    }

    override fun onProviderEnabled(status: String) {
        writeToLog(
            "LoadPrizesTask",
            "onProviderEnabled called status: $status"
        )
    }

    override fun onLocationChanged(location: Location) {
        writeToLog(
            "LoadPrizesTask",
            "onLocationChanged called, new location latitude: " + location.latitude + " longitude: " + location.longitude
        )
    }

    companion object {
        private var mResources: Resources? = null
        private const val CONNECTION_FAILURE_RESOLUTION_REQUEST = 1
        private fun writeToLog(filter: String, msg: String) {
            if ("true".equals(
                    mResources!!.getString(R.string.debug),
                    ignoreCase = true
                )
            ) {
                Log.d(filter, msg)
            }
        }
    }
}