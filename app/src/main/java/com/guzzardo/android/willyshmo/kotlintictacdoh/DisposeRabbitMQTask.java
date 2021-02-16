package com.guzzardo.android.willyshmo.kotlintictacdoh;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

public class DisposeRabbitMQTask  extends AsyncTask<Object, Void, Void> {
	ToastMessage mActivity;
	static Resources mResources;

	@Override
	protected Void doInBackground(Object... values) {
		try {
			RabbitMQMessageConsumer rabbitMQMessageConsumer= (RabbitMQMessageConsumer)values[0];
			mResources = (Resources)values[1];
			mActivity = (ToastMessage)values[2];	
//			rabbitMQMessageConsumer.setConsumeRunning(false);
			rabbitMQMessageConsumer.dispose();

		} catch (Exception e) {
			writeToLog("DisposeRabbitMQTask", e.getMessage());
			mActivity.sendToastMessage(e.getMessage());
		}
		return null;
	}
	
    private static void writeToLog(String filter, String msg) {
    	if ("true".equalsIgnoreCase(mResources.getString(R.string.debug))) {
    		Log.d(filter, msg);
    	}
    }
	
}

