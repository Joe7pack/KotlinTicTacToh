package com.guzzardo.android.willyshmo.kotlintictacdoh;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

import com.guzzardo.android.willyshmo.kotlintictacdoh.MainActivity.UserPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public class WebServerInterfaceNewPlayerTask extends AsyncTask<Object, Void, Integer> {

	private PlayOverNetwork mCallerActivity;
	private Context mApplicationContext;
	private String mPlayer1Name;
	private static Resources mResources;

	@Override
	protected Integer doInBackground(Object... params) {
		int player1Id = 0;
    	mCallerActivity = (PlayOverNetwork)params[0];
    	String url = (String)params[1];
    	mPlayer1Name = (String)params[2];
    	mApplicationContext = (Context)params[3]; 
    	mResources = (Resources)params[4];
		writeToLog("WebServerInterfaceNewPlayerTask", "doInBackground called"); 
		try {
			String newUser = WebServerInterface.converseWithWebServer(url, mPlayer1Name, mCallerActivity, mResources);
			if (newUser == null) {
				return null;
			}
			player1Id = getNewUserId(newUser);
		} catch (Exception e) { 
//			System.out.println(e.getMessage());
			writeToLog("WebServerInterfaceNewPlayerTask", "doInBackground exception called " + e.getMessage()); 
	        mCallerActivity.sendToastMessage(e.getMessage());
		}
		return player1Id;
	}	
	
	protected void onPostExecute(Integer player1Id) {
		try {
			writeToLog("WebServerInterfaceNewPlayerTask", "onPostExecute called player1Id " + player1Id); 
			if (player1Id == null) {
				return;
			}
	        SharedPreferences settings = mApplicationContext.getSharedPreferences(UserPreferences.PREFS_NAME, 0);
	        SharedPreferences.Editor editor = settings.edit();
	        editor.putInt(GameActivity.PLAYER1_ID, player1Id);
	        // Commit the edits!
	        editor.apply();
        	WebServerInterfaceUsersOnlineTask webServerInterfaceUsersOnlineTask = new WebServerInterfaceUsersOnlineTask();
        	webServerInterfaceUsersOnlineTask.execute(mCallerActivity, mApplicationContext, mPlayer1Name, mResources, player1Id);
		} catch (Exception e) {
			writeToLog("WebServerInterfaceNewPlayerTask", "onPostExecute exception called " + e.getMessage()); 
			mCallerActivity.sendToastMessage(e.getMessage());			
    	}
	}
	
    private int getNewUserId(String newUser) {
    	try {
    		JSONObject jsonObject = new JSONObject(newUser);
    		JSONObject userObject = jsonObject.getJSONObject("User"); 
       		String userId = userObject.getString("id");
       		if (null != userId)
       			return Integer.parseInt(userId);
        } catch (JSONException e) {
        	writeToLog("WebServerInterfaceNewPlayerTask", "getNewUserId exception called " + e.getMessage());        	
			mCallerActivity.sendToastMessage(e.getMessage());			
        }
        return 0;
    }
    
    private static void writeToLog(String filter, String msg) {
    	if ("true".equalsIgnoreCase(mResources.getString(R.string.debug))) {
    		Log.d(filter, msg);
    	}
    }
	
}


