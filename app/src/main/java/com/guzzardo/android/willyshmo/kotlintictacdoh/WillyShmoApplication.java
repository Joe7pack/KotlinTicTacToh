package com.guzzardo.android.willyshmo.kotlintictacdoh;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.location.LocationListener;
import android.provider.Settings;
import android.util.Log;

import androidx.multidex.MultiDexApplication;

import com.google.android.gms.common.api.GoogleApiClient;

import java.util.HashMap;

//import android.support.multidex.MultiDexApplication;
//import com.google.android.gms.common.GooglePlayServicesClient;
//import com.google.android.gms.location.LocationClient;


public class WillyShmoApplication extends MultiDexApplication /* implements ConnectionCallbacks,
	OnConnectionFailedListener, LocationListener */ {
	
//    private static String [] mPrizeImages;
    private static String [] mPrizeNames;  
    private static String [] mPrizeIds;
    private static Bitmap [] mBitmapImages;
    private static String [] mImageWidths;
    private static String [] mImageHeights;
    private static String [] mPrizeDistances;
    private static String [] mPrizeUrls;
    private static String [] mPrizeLocations;

    private static HashMap<String, String> mConfigMap;
    
    private static double mLongitude;
    private static double mLatitude;
    private static boolean mNetworkAvailable; 
    private static LocationListener mLocationListener;  
    private static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 1;
	private static ToastMessage mCallerActivity;
    private static Resources mResources;	
    private static boolean mStartMainActivity;
    private static String mAndroidId;

	public static void setWillyShmoApplicationContext(Context context) {
		mApplicationContext = context;
	}

	public static Context getWillyShmoApplicationContext() {
		return mApplicationContext;
	}

	private static Context mApplicationContext;
	//private static LocationClient mLocationClient; // = new LocationClient(getApplicationContext(), this, this);
    private static GoogleApiClient mGoogleApiClient;
    
    @Override
    public void onCreate() {
    	super.onCreate();
        //mLocationClient = new LocationClient(getApplicationContext(), this, this);
        mAndroidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        mResources = getResources();
        mConfigMap = new HashMap();
    }    

//	public static String[] getPrizeImages() {
//		return mPrizeImages;
//	}
//
//	public static void setPrizeImages(String[] mPrizeImages) {
//		WillyShmoApplication.mPrizeImages = mPrizeImages;
//	}

	public static String[] getPrizeNames() {
		return mPrizeNames;
	}

	public static void setPrizeNames(String[] mPrizeNames) {
		WillyShmoApplication.mPrizeNames = mPrizeNames;
	}

	public static String[] getPrizeIds() {
		return mPrizeIds;
	}

	public static void setPrizeIds(String[] mPrizeIds) {
		WillyShmoApplication.mPrizeIds = mPrizeIds;
	}

	public static Bitmap[] getBitmapImages() {
		return mBitmapImages;
	}

	public static void setBitmapImages(Bitmap[] mBitmapImages) {
		WillyShmoApplication.mBitmapImages = mBitmapImages;
	}

	public static double getLatitude() {
		return mLatitude;
	}

	public static void setLatitude(double latitude) {
		mLatitude = latitude;
	}    

	public static double getLongitude() {
		return mLongitude;
	}

	public static void setLongitude(double longitude) {
		mLongitude = longitude;
	}

	public static String[] getPrizeDistances() {
		return mPrizeDistances;
	}

	public static void setPrizeDistances(String[] prizeDistances) {
		WillyShmoApplication.mPrizeDistances = prizeDistances;
	}
	
	public static String[] getImageWidths() {
		return mImageWidths;
	}

	public static void setImageWidths(String[] imageWidths) {
		WillyShmoApplication.mImageWidths = imageWidths;
	}

	public static String[] getImageHeights() {
		return mImageHeights;
	}

	public static void setImageHeights(String[] imageHeights) {
		WillyShmoApplication.mImageHeights = imageHeights;
	}
	
	public static boolean isNetworkAvailable() {
		return mNetworkAvailable;
	}

	public static void setNetworkAvailable(boolean networkAvailable) {
		mNetworkAvailable = networkAvailable;
        writeToLog("WillyShmoApplication", "setNetworkAvailable(): "  + mNetworkAvailable);
	}    

    // Acquire a reference to the system Location Manager
    // Define a listener that responds to location updates


	/*
	public static LocationListener getLocationListener() {

        writeToLog("WillyShmoApplication", "getLocationListener() called at: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		if (null == mLocationListener) {
			setLocationListener();
		}
		writeToLog("WillyShmoApplication", "getLocationListener() returned at: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		return mLocationListener;	
	}
	*/

	public static void setCallerActivity(ToastMessage activity) {
		mCallerActivity = activity;
	}

	public static ToastMessage getCallerActivity() {
		return mCallerActivity;
	}


//	public static LocationClient getLocationClient(boolean startMainActivity) {
//		mStartMainActivity = startMainActivity;
//		return mLocationClient;
//	}

    public static void setMainStarted(boolean startMainActivity) {
        mStartMainActivity = startMainActivity;
    }

	/*
    @Override
    public void onConnected(Bundle dataBundle) {
    	// mCallerActivity.sendToastMessage("Connected to Google Play");
        // Register the listener with the Location Manager to receive location updates
		writeToLog("WillyShmoApplication", "onConnected called at: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    	try {
    		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, WillyShmoApplication.getLocationListener());

			Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
			setLatitude(lastKnownLocation.getLatitude());
			setLongitude(lastKnownLocation.getLongitude());
			writeToLog("WillyShmoApplication", "new location - longitude: " + getLongitude() + " latitude: " + getLatitude());
			if (mStartMainActivity) {
				mStartMainActivity = false;
				GetPrizeListTask getPrizeListTask = new GetPrizeListTask();
				getPrizeListTask.execute(mCallerActivity, mResources, "true");
			}
			writeToLog("WillyShmoApplication", "onConnected completed at: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
//    		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
//    		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    	} catch (SecurityException se) {
    		writeToLog("WillyShmoApplication", "requestLocationUpdates SecurityException encountered");
		}

    	catch (Exception e) {
    		writeToLog("WillyShmoApplication", "onConnected error: " + e.getMessage());
    	}
    }
    */

	/*
	@Override
	public void onConnectionSuspended(int cause) {
		// The connection has been interrupted.
		// Disable any UI components that depend on Google APIs
		// until onConnected() is called.
	}
    
    /*
     * Called by Location Services if the attempt to
     * Location Services fails.

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.

        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult((Activity)mCallerActivity,
				    CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent

            } catch (IntentSender.SendIntentException e) {
                // Log the error
                //e.printStackTrace();
//            	mCallerActivity.sendToastMessage("onConnectionFailed exception: " + e.getMessage());            	
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.

        	//FIXME - add showErrorDialog
            //showErrorDialog(connectionResult.getErrorCode());
        }
    }
    */
    
    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.

    @Override
    public void onDisconnected() {
    	//mCallerActivity.sendToastMessage("Disconnected from Google Play. Please re-connect.");
    }
     */

    /*
	private static void setLocationListener() {
		mLocationListener = new LocationListener() {

			public void onLocationChanged(Location location) {
                writeToLog("WillyShmoApplication", "onLocationChanged() called at: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
				// Called when a new location is found by the network location provider.
				//        	mCurrentLocation = mLocationClient.getLastLocation();
//				if (latitude == 0 && longitude == 0) {


				if (location.getLatitude() != getLatitude() && location.getLongitude() != getLongitude()) {
					setLatitude(location.getLatitude());
					setLongitude(location.getLongitude());
					writeToLog("WillyShmoApplication", "new location - longitude: " + getLongitude() + " latitude: " + getLatitude());
					if (mStartMainActivity) {
						mStartMainActivity = false;
						GetPrizeListTask getPrizeListTask = new GetPrizeListTask();
						getPrizeListTask.execute(mCallerActivity, mResources, "true");
					}
				}
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {
				writeToLog("WillyShmoApplication", "onStatusChanged");
			}

			public void onProviderEnabled(String provider) {
				writeToLog("WillyShmoApplication", "onProviderEnabled");
			}

			public void onProviderDisabled(String provider) {
				writeToLog("WillyShmoApplication", "onProviderDisabled called");
			}

		};
	}
	*/

    private static void writeToLog(String filter, String msg) {
    	if ("true".equalsIgnoreCase(mResources.getString(R.string.debug))) {
    		Log.d(filter, msg);
    	}
    }
    
    public static String getAndroidId() {
    	return mAndroidId;
    }

	public static String[] getPrizeUrls() {
		return mPrizeUrls;
	}

	public static void setPrizeUrls(String[] prizeUrls) {
		WillyShmoApplication.mPrizeUrls = prizeUrls;
	}

	public static void setConfigMap(String key, String value) {
		mConfigMap.put(key, value);
	}

	public static String getConfigMap(String key) {
		return (String)mConfigMap.get(key);
	}

	public static String[] getPrizeLocations() {
		return mPrizeLocations;
	}

	public static void setPrizeLocations(String[] prizeLocations) {
		WillyShmoApplication.mPrizeLocations = prizeLocations;
	}

	/*
	@Override
	public void onStatusChanged(String status, int value, Bundle bundle) {
		writeToLog("WillyShmoApplication", "onStatusChanged called status: " + status);
	}

	@Override
	public void onProviderDisabled(String status) {
		writeToLog("WillyShmoApplication", "onProviderDisabled called status: " + status);
	}

	@Override
	public void onProviderEnabled(String status) {
		writeToLog("WillyShmoApplication", "onProviderEnabled called status: " + status);
	}

	@Override
	public void onLocationChanged(Location location) {
		writeToLog("WillyShmoApplication", "onLocationChanged called, new location latitude: " + location.getLatitude() + " longitude: " + location.getLongitude());
	}
    */

    static public void setGoogleApiClient(GoogleApiClient googleApiClient) {
        mGoogleApiClient = googleApiClient;
    }
}
