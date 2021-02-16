package com.guzzardo.android.willyshmo.kotlintictacdoh;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.multidex.BuildConfig;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

public class FusedLocationActivity extends Activity implements ToastMessage {
    /*
      Using location settings.
      <p/>
      Uses the {@link com.google.android.gms.location.SettingsApi} to ensure that the device's system
      settings are properly configured for the app's location needs. When making a request to
      Location services, the device's system settings may be in a state that prevents the app from
      obtaining the location data that it needs. For example, GPS or Wi-Fi scanning may be switched
      off. The {@code SettingsApi} makes it possible to determine if a device's system settings are
      adequate for the location request, and to optionally invoke a dialog that allows the user to
      enable the necessary settings.
      <p/>
      This sample allows the user to request location updates using the ACCESS_FINE_LOCATION setting
      (as specified in AndroidManifest.xml).
     */

    /**
     * Code used in requesting runtime permissions.
     */
    // private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 454;

    /**
     * Constant used in the location settings dialog.
     */
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    // Keys for storing activity state in the Bundle.
    private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private final static String KEY_LOCATION = "location";
    private final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Callback for Location events.
     */
    private LocationCallback mLocationCallback;

    /**
     * Represents a geographical location.
     */
    private Location mCurrentLocation;
    private String mLatitudeLabel;
    private String mLongitudeLabel;
    private String mLastUpdateTimeLabel;

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    private Boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a String.
     */
    private String mLastUpdateTime;

    private double myLatitude, myLongitude;

    public static ErrorHandler mErrorHandler;

    private ProgressBar pgsBar;
    private int progressIndex = 0;

    HandlerThread handlerThread;
    private Looper looper;
    private Handler looperHandler;
    final int START_LOCATION_CHECK_ACTION = 0;
    final int MAIN_ACTIVITY_ACTION = 1;
    final int COMPLETED_LOCATION_CHECK_ACTION = 2;
    final int START_GET_PRIZES_FROM_SERVER = 3;
    final int FORMATTING_PRIZE_DATA = 4;
    final int PRIZES_LOADED = 5;
    final int PRIZE_LOAD_IN_PROGRESS = 6;
    final int PRIZES_READY_TO_DISPLAY = 7;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.splash2);
        setContentView(R.layout.splash_with_guidelines);
        pgsBar = (ProgressBar) findViewById(R.id.progressBar);
        pgsBar.setProgress(10);

        // Set labels.
        mLatitudeLabel = getResources().getString(R.string.latitude_label);
        mLongitudeLabel = getResources().getString(R.string.longitude_label);
        mLastUpdateTimeLabel = getResources().getString(R.string.last_update_time_label);

        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        //mSettingsClient = LocationServices.getSettingsClient(this);

        buildLocationSettingsRequest();
        mErrorHandler = new ErrorHandler();

        startMyThread();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions();
        } else {
            getLocation();
        }
    }

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        KEY_REQUESTING_LOCATION_UPDATES);
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING)) {
                mLastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING);
            }
            // updateUI();
        }
    }

     /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        writeToLog("FusedLocationActivity", "User agreed to make required location settings changes.");
                        //Log.i(TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        writeToLog("FusedLocationActivity", "User chose not to make required location settings changes.");
                        //Log.i(TAG, "User chose not to make required location settings changes.");
                        mRequestingLocationUpdates = false;
                        //updateUI();
                        break;
                }
                break;
        }
    }

    /**
     * Handles the Start Updates button and requests start of location updates. Does nothing if
     * updates have already been requested.
     */

    //note - this handler method is coded right in the view xml file!
    public void startUpdatesButtonHandler(View view) {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            //setButtonsEnabledState();
            //startLocationUpdates();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermissions() {
        PermissionUtil.checkPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION,
                new PermissionUtil.PermissionAskListener() {
                    @Override
                    public void onPermissionAsk() {
                        writeToLog("FusedLocationActivity", "onPermissionAsk() called.");
                        ActivityCompat.requestPermissions(
                                FusedLocationActivity.this,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION
                        );
                    }

                    @Override
                    public void onPermissionPreviouslyDenied() {
                        //show a dialog explaining permission and then request permission
                        writeToLog("FusedLocationActivity", "requestPermissions() called.");
                        requestPermissions();
                    }

                    @Override
                    public void onPermissionDisabled() {
                        //TODO - may want to call requestPermissions() again?
                        writeToLog("FusedLocationActivity", "onPermissionDisabled() called.");
                        sendToastMessage("Permission Disabled");
                        requestPermissions();
                    }

                    @Override
                    public void onPermissionGranted() {
                        writeToLog("FusedLocationActivity", "onPermissionGranted() called.");
                        getLocation();
                        //readContacts();
                        //startLocationUpdates(); //for now, may not actually need this
                    }
                });
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
           // return;
            writeToLog("FusedLocationActivity", "Permission not granted");
        }

        //mCallerActivity.setStartLocationLookup();
        // we need to set mCallerActivity to Splashscreen to use the message Handler code to set the progress bar

        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            setStartLocationLookup();
            //if (location != null) {
                //myLatitude = location.getLatitude();
                //myLongitude = location.getLongitude();
                writeToLog("FusedLocationActivity", "My latitude: " + myLatitude + " my Longitude: " + myLongitude);
                WillyShmoApplication.setLatitude(myLatitude);
                WillyShmoApplication.setLongitude(myLongitude);

                //SplashScreen callerActivity = (SplashScreen)WillyShmoApplication.getCallerActivity();

                setStartLocationLookupCompleted();
                GetPrizeListTask getPrizeListTask = new GetPrizeListTask();
                getPrizeListTask.execute(this, getResources(), "true");

            //}
        });
        }

        @Override
        public void onResume() {
            super.onResume();
        }

        @Override
        protected void onPause() {
            super.onPause();
            // Remove location updates to save battery.
            //stopLocationUpdates();
        }

        /**
         * Stores activity data in the Bundle.
         */
        public void onSaveInstanceState(Bundle savedInstanceState) {
            savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
            savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation);
            savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime);
            super.onSaveInstanceState(savedInstanceState);
        }

        /**
         * Shows a {@link Snackbar}.
         *
         * @param mainTextStringId The id for the string resource for the Snackbar text.
         * @param actionStringId   The text of the action item.
         * @param listener         The listener associated with the Snackbar action.
         */
        private void showSnackbar(final int mainTextStringId, final int actionStringId,
                                  View.OnClickListener listener) {
            Snackbar.make(
                    findViewById(android.R.id.content),
                    getString(mainTextStringId),
                    Snackbar.LENGTH_INDEFINITE)
                    .setBackgroundTint(Color.WHITE)
                    .setActionTextColor(Color.BLACK)
                    .setAction(getString(actionStringId), listener).show();

            View snackBarView = findViewById(android.R.id.content);
            //snackBarView.setBackgroundColor(Color.GREEN);
        }

        private void requestPermissions() {
            boolean shouldProvideRationale =
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION);

            // Provide an additional rationale to the user. This would happen if the user denied the
            // request previously, but didn't check the "Don't ask again" checkbox.
            if (shouldProvideRationale) {
                writeToLog("FusedLocationActivity", "Displaying permission rationale to provide additional context.");
                //Log.i(TAG, "Displaying permission rationale to provide additional context.");
                showSnackbar(R.string.permission_rationale,
                        android.R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Request permission
                                ActivityCompat.requestPermissions(FusedLocationActivity.this,
                                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
                            }
                        });
            } else {
                writeToLog("FusedLocationActivity", "Requesting permission.");
                //Log.i(TAG, "Requesting permission");
                // Request permission. It's possible this can be auto answered if device policy
                // sets the permission in a given state or the user denied the permission
                // previously and checked "Never ask again".
                ActivityCompat.requestPermissions(FusedLocationActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        //new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            }
        }

        /**
         * Callback received when a permissions request has been completed.
         */
        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

            try {
                //writeToLog("FusedLocationActivity", "onRequestPermissionResult called");
                writeToLog("FusedLocationActivity", "at start of onRequestPermissionsResult request code = " + requestCode + ", grantResults[0] = " + grantResults[0]);
                if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION) {
                    writeToLog("FusedLocationActivity", "inside onRequestPermissionsResult request code = " + requestCode + ", grantResults[0] = " + grantResults[0]);
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        getLocation();
                    } else {
                        // Permission denied.

                        // Notify the user via a SnackBar that they have rejected a core permission for the
                        // app, which makes the Activity useless. In a real app, core permissions would
                        // typically be best requested during a welcome-screen flow.

                        // Additionally, it is important to remember that a permission might have been
                        // rejected without asking the user for permission (device policy or "Never ask
                        // again" prompts). Therefore, a user interface affordance is typically implemented
                        // when permissions are denied. Otherwise, your app could appear unresponsive to
                        // touches or interactions which have required permissions.

                        showSnackbar(R.string.permission_denied_explanation);
                    }
                }
            } catch (Exception e) {
                System.out.println(("onRequestPermissionsResult exception: " + e));
            }
        }

        private void showSnackbar(int displayStringResource) {
            showSnackbar(displayStringResource,
                    R.string.settings, view -> {
                        // Build intent that displays the App settings screen.
                        Intent intent = new Intent();
                        intent.setAction(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package",
                                BuildConfig.APPLICATION_ID, null);
                        intent.setData(uri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    });
        }

    private static class ErrorHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(WillyShmoApplication.getWillyShmoApplicationContext(), (String)msg.obj, Toast.LENGTH_LONG).show();
        }
    }

    public void sendToastMessage(String message) {
        Message msg = mErrorHandler.obtainMessage();
        msg.obj = message;
        mErrorHandler.sendMessage(msg);
    }

    public void startMyThread() {
        progressIndex = pgsBar.getProgress();
        handlerThread = new HandlerThread("MyHandlerThread");
        handlerThread.start();
        looper = handlerThread.getLooper();
        looperHandler = new Handler(looper)  {
            @Override
            public void handleMessage(Message msg) {
                //TODO - consolidate this code a little better
                switch (msg.what) {
                    case START_LOCATION_CHECK_ACTION: {
                        setProgressBar(20);
                        break;
                    }
                    case COMPLETED_LOCATION_CHECK_ACTION: {
                        setProgressBar(30);
                        break;
                    }
                    case START_GET_PRIZES_FROM_SERVER: {  //most of the waiting is here
                        setProgressBar(40);
                        break;
                    }
                    case FORMATTING_PRIZE_DATA: {
                        setProgressBar(60);
                        break;
                    }
                    case PRIZE_LOAD_IN_PROGRESS: {
                        setProgressBar(70);
                        break;
                    }
                    case PRIZES_LOADED: {
                        setProgressBar(80);
                        break;
                    }
                    case PRIZES_READY_TO_DISPLAY: {
                        setProgressBar(90);
                        break;
                    }
                    case MAIN_ACTIVITY_ACTION: {
                        setProgressBar(100);
                        break;
                    }
                    default:
                        break;
                }
            }
        };
    }

    public void setStartLocationLookup() {
        Message msg = looperHandler.obtainMessage(START_LOCATION_CHECK_ACTION);
        looperHandler.sendMessage(msg);
    }

    public void setStartLocationLookupCompleted() {
        Message msg = looperHandler.obtainMessage(COMPLETED_LOCATION_CHECK_ACTION);
        looperHandler.sendMessage(msg);
    }

    public void setMainActivityCalled() {
        Message msg = looperHandler.obtainMessage(MAIN_ACTIVITY_ACTION);
        looperHandler.sendMessage(msg);
    }

    public void setGettingPrizesCalled() {
        Message msg = looperHandler.obtainMessage(START_GET_PRIZES_FROM_SERVER);
        looperHandler.sendMessage(msg);
    }

    public void prizeLoadInProgress() {
        Message msg = looperHandler.obtainMessage(PRIZE_LOAD_IN_PROGRESS);
        looperHandler.sendMessage(msg);
    }

    public void setPrizesRetrievedFromServer() {
        Message msg = looperHandler.obtainMessage(FORMATTING_PRIZE_DATA);
        looperHandler.sendMessage(msg);
    }

    public void setPrizesLoadIntoObjects() {
        Message msg = looperHandler.obtainMessage(PRIZES_LOADED);
        looperHandler.sendMessage(msg);
    }

    public void setPrizesLoadedAllDone() {
        Message msg = looperHandler.obtainMessage(PRIZES_READY_TO_DISPLAY);
        looperHandler.sendMessage(msg);
    }

    private void  setProgressBar(int progress) {
        pgsBar.setProgress(progress);
        writeToLog("FusedLocationActivity", "progress bar set to: " + progress);
        //handlerThread.quit();
    }

    private static void writeToLog(String filter, String msg) {
        if ("true".equalsIgnoreCase(WillyShmoApplication.getWillyShmoApplicationContext().getResources().getString(R.string.debug))) {
            Log.d(filter, msg);
        }
    }

}


