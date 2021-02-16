package com.guzzardo.android.willyshmo.kotlintictacdoh;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;


public class SendMessageToWillyShmoServer extends AsyncTask<Object, Void, String> {
	
	private static ToastMessage mCallerActivity;
	private static Resources mResources;
	private static Boolean mFinishActivity;

	@Override
	protected String doInBackground(Object... values) {

		String urlToEncode = (String)values[1];
		mCallerActivity = (ToastMessage)values[2];
    	mResources = (Resources)values[3];
    	mFinishActivity = (Boolean)values[4];
		String url = mResources.getString(R.string.domainName) + (String)values[0];

		BufferedInputStream bis = null; 
		InputStream is = null;
		String result = null;
		String errorAt = null;

		try {    	
			URL myURL = null;
			if (urlToEncode == null) {
				myURL = new URL(url);
			} else {
				String encodedUrl = URLEncoder.encode(urlToEncode, "UTF-8");
				myURL = new URL(url+encodedUrl);
			}
			errorAt = "openConnection";
			URLConnection ucon = myURL.openConnection();
			/* Define InputStreams to read 
			 * from the URLConnection. */
			errorAt = "getInputStream";
			is = ucon.getInputStream();
			errorAt = "bufferedInputStream";
			bis = new BufferedInputStream(is);
			errorAt = "convertStreamToString";
			result = convertStreamToString(is);
			/* Convert the Bytes read to a String. */
		} catch (Exception e) {
			//e.printStackTrace(); 
			writeToLog("SendMessageToWillyShmoServer", "error: " + e.getMessage() + " error at: " + errorAt); 
			mCallerActivity.sendToastMessage("Sorry, Willy Shmo server is not available now. Please try again later");
		} finally {
			try {
				bis.close();
				is.close();
			} catch (Exception e) {
				//nothing to do here
				writeToLog("SendMessageToWillyShmoServer", "finally error: " + e.getMessage());				
			}
		}
		return result;
	}
	
    private static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
 
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
//            e.printStackTrace();
        	writeToLog("SendMessageToWillyShmoServer", "IOException: " + e.getMessage());
        	mCallerActivity.sendToastMessage(e.getMessage());
        } catch (Exception e) {
  //      	e.printStackTrace();
        	writeToLog("SendMessageToWillyShmoServer", "Exception: " + e.getMessage());        	
        	mCallerActivity.sendToastMessage(e.getMessage());
        } finally {
            try {
                is.close();
            } catch (IOException e) {
//                e.printStackTrace();
            	writeToLog("SendMessageToWillyShmoServer", "is close IOException: " + e.getMessage());            	
            	mCallerActivity.sendToastMessage(e.getMessage());
            }
        }
        return sb.toString();
    }
    
    @Override
	protected void onPostExecute(String res) {
		try {
			if (mFinishActivity) {
				mCallerActivity.finish();
			}
		} catch (Exception e) {
			mCallerActivity.sendToastMessage(e.getMessage());
		}
	}
    
    private static void writeToLog(String filter, String msg) {
    	if ("true".equalsIgnoreCase(mResources.getString(R.string.debug))) {
    		Log.d(filter, msg);
    	}
    }
    
}

