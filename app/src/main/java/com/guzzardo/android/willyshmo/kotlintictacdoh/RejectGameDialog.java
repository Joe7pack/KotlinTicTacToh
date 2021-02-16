package com.guzzardo.android.willyshmo.kotlintictacdoh;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

//import android.support.v7.app.AlertDialog;

public class RejectGameDialog extends DialogFragment implements ToastMessage {
	
	private static String mOpposingPlayerName;
	private static String mOpposingPlayerId;
	private static Context mApplicationContext;
	private static String mPlayerName;
	private static Integer mPlayerId;
	private static Resources mResources;
	public static ErrorHandler errorHandler;	

    /*
	public RejectGameDialog(String opposingPlayerName, String opposingPlayerId, String playerName, Integer playerId, Context applicationContext, Resources resources) {
		mOpposingPlayerName = opposingPlayerName;
		mOpposingPlayerId = opposingPlayerId;
		mApplicationContext = applicationContext;
		mPlayerName = playerName;
		mPlayerId = playerId;
		mResources = resources;
	}
	*/

    public RejectGameDialog() {
        super();
    }

    @Override
    public void setArguments(Bundle myBundle) {
        mOpposingPlayerName = myBundle.getString("opponentName");
        mOpposingPlayerId = myBundle.getString("opponentId");
        mPlayerName = myBundle.getString("playerName");
        mPlayerId = myBundle.getInt("player1Id");
}

    public void setContext(Context applicationContext) {
        mApplicationContext = applicationContext;
    }

    public void setResources(Resources resources) {
        mResources = resources;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
        	.setTitle("Sorry, " + mOpposingPlayerName + " doesn't want to play now")
            .setIcon(R.drawable.willy_shmo_small_icon)
            .setPositiveButton(android.R.string.yes,  new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
//                	acknowledgeRejection();
                }
            })
            .create();
    }
    
    public void setOpposingPlayerName(String name) {
    	mOpposingPlayerName = name;
    }
    
    private void acknowledgeRejection() {
    	//String hostName = mResources.getString(R.string.RabbitMQHostName);
        String hostName = (String)WillyShmoApplication.getConfigMap("RabbitMQIpAddress");
        String queuePrefix = (String)WillyShmoApplication.getConfigMap("RabbitMQQueuePrefix");
		String qName = queuePrefix + "-" + "startGame"  + "-" + mOpposingPlayerId;
		String messageToOpponent = "refused," + mPlayerName + ","  + mPlayerId;
		new SendMessageToRabbitMQTask().execute(hostName, qName, null, messageToOpponent, this, mResources);

    }
    
    private static void writeToLog(String filter, String msg) {
    	if ("true".equalsIgnoreCase(mResources.getString(R.string.debug))) {
    		Log.d(filter, msg);
    	}
    }
    
    public void finish() { }
    
    public void sendToastMessage(String message) {
    	Message msg = RejectGameDialog.errorHandler.obtainMessage();
    	msg.obj = message;
    	RejectGameDialog.errorHandler.sendMessage(msg);	
    }
    
    private class ErrorHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
    		Toast.makeText(mApplicationContext, (String)msg.obj, Toast.LENGTH_LONG).show();
        }
    }
    
    public void setContentView(int view) { }
    
}

