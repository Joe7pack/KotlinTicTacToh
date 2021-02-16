package com.guzzardo.android.willyshmo.kotlintictacdoh;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

/**
 * An AsyncTask that will be used to find other players currently online
 */
public class WebServerInterfaceUsersOnlineTask extends AsyncTask<Object, Void, String> {

	private PlayOverNetwork mCallerActivity;
	private Context applicationContext;
	private String mPlayer1Name;
	private Integer mPlayer1Id;
	private static Resources mResources;

	@Override
	protected String doInBackground(Object... params) {
		
		String usersOnline = null;
    	mCallerActivity = (PlayOverNetwork)params[0];
    	applicationContext = (Context)params[1]; 
    	mPlayer1Name = (String)params[2];
    	mResources = (Resources)params[3]; 
    	mPlayer1Id = (Integer)params[4];
    	String url = mResources.getString(R.string.domainName) + "/gamePlayer/listUsers";
    	
		try {
			usersOnline = WebServerInterface.converseWithWebServer(url, null, mCallerActivity, mResources);
		} catch (Exception e) { 
			writeToLog("WebServerInterfaceUsersOnlineTask", "doInBackground: " + e.getMessage());
			mCallerActivity.sendToastMessage(e.getMessage());		
		}
		writeToLog("WServerInterfaceUsersOnline", "WebServerInterfaceUsersOnlineTask doInBackground called usersOnline: " + usersOnline);  		
		return usersOnline;
	}	
	
	protected void onPostExecute(String usersOnline) {
		try {
			writeToLog("WebServerInterfaceUsersOnlineTask", "onPostExecute called usersOnline: " + usersOnline);
			
	    	String androidId = "&deviceId=" + WillyShmoApplication.getAndroidId(); 
	    	String latitude = "&latitude=" + WillyShmoApplication.getLatitude();
	    	String longitude = "&longitude=" + WillyShmoApplication.getLongitude();
	    	String trackingInfo = androidId + latitude + longitude;

    		String urlData = "/gamePlayer/update/?id=" + mPlayer1Id + trackingInfo + "&onlineNow=true&opponentId=0&userName=";
    		new SendMessageToWillyShmoServer().execute(urlData, mPlayer1Name, mCallerActivity, mResources, Boolean.FALSE);
			if (usersOnline == null) {
				return;
			}
	        SharedPreferences settings = applicationContext.getSharedPreferences(MainActivity.UserPreferences.PREFS_NAME, 0);
	        SharedPreferences.Editor editor = settings.edit();
	        editor.putString("ga_users_online", usersOnline);
	        // Commit the edits!
	        editor.apply();
	        Intent i = new Intent(mCallerActivity, PlayersOnlineActivity.class);
	        i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name);	        
	        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_DEBUG_LOG_RESOLUTION | Intent.FLAG_FROM_BACKGROUND );	        
	        applicationContext.startActivity(i); // control is picked up in onCreate method 	        
		} catch(Exception e) {
			writeToLog("WebServerInterfaceUsersOnlineTask", "onPostExecute exception called " + e.getMessage());
			mCallerActivity.sendToastMessage(e.getMessage());			
    	}
	}
	
    private static void writeToLog(String filter, String msg) {
    	if ("true".equalsIgnoreCase(mResources.getString(R.string.debug))) {
    		Log.d(filter, msg);
    	}
    }
}



