package com.guzzardo.android.willyshmo.kotlintictacdoh;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//import static com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.companion; . .Companion.getWillyShmoApplicationContext;


/*
Splash screen starts:

calls FusedLocationActivity via intent
FusedLocationActivity gets Location permissions and then gets the Location
FusedLocationActivity then calls GetPrizeListTask via an async call
GetPrizeListTask then calls MainActivity which displays the screen showing the load prizes button
LoadPrizesTask is no longer used
*/

public class SplashScreen extends Activity implements ToastMessage {
    protected boolean mActive = true;
    private static boolean mSkipWaitCheck;
    private static int mSplashTime = 2500;
    public static ErrorHandler mErrorHandler;
    private static Resources mResources;
    String MSG_KEY = "message key";
    /**
     * perform the action in `handleMessage` when the thread calls
     * `mHandler.sendMessage(msg)`
     */
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String string = bundle.getString(MSG_KEY);
            final TextView myTextView = (TextView)findViewById(R.id.textView);
            myTextView.setText(string);
        }
    };

    private final Runnable mMessageSender = new Runnable() {
        public void run() {
            Message msg = mHandler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString(MSG_KEY, getCurrentTime());
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    };

    private String getCurrentTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy", Locale.US);
        return dateFormat.format(new Date());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        mResources = getResources();
        mErrorHandler = new ErrorHandler();
        boolean mPrizesAvailable = false;
        if ("true".equalsIgnoreCase(mResources.getString(R.string.prizesAvailable))) {
            mPrizesAvailable = true;
        }

        WillyShmoApplication.Companion.setLatitude(0);
        WillyShmoApplication.Companion.setLongitude(0);

        WillyShmoApplication.Companion.setCallerActivity(SplashScreen.this);
        WillyShmoApplication.Companion.setWillyShmoApplicationContext(this.getApplicationContext());
        //WillyShmoApplication.Companion.setPrizeNames(null);

        if (mPrizesAvailable) {
            //new LoadPrizesTask().execute(SplashScreen.this, getApplicationContext(), getResources());
            //mSkipWaitCheck = true;
        }

        //Context willyShmoApplicationContext = getWillyShmoApplicationContext();
        Context willyShmoApplicationContext = WillyShmoApplication.Companion.getWillyShmoApplicationContext();

        Intent myIntent = new Intent(willyShmoApplicationContext, FusedLocationActivity.class);
        startActivity(myIntent);
    }

    @Override
    public void onStart() {
        super.onStart();
        GetConfigurationValuesFromDB getConfigurationValuesFromDB = new GetConfigurationValuesFromDB();
        getConfigurationValuesFromDB.execute(this, getApplicationContext(), getResources());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            setSplashActive(false);
        }
        return true;
    }

    private class ErrorHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(getApplicationContext(), (String)msg.obj, Toast.LENGTH_LONG).show();
        }
    }

    public void sendToastMessage(String message) {
        Message msg = SplashScreen.mErrorHandler.obtainMessage();
        msg.obj = message;
        SplashScreen.mErrorHandler.sendMessage(msg);
    }

    public void showGooglePlayError(final Integer isPlayAvailable, final String playErrorMessage) {
        try {
            AlertDialog dialog = createGooglePlayErrorDialog(isPlayAvailable, playErrorMessage);
            dialog.show();
        } catch (Exception e) {
            sendToastMessage(e.getMessage());
        }
    }

    public AlertDialog createGooglePlayErrorDialog(final Integer isPlayAvailable, final String playErrorMessage) {
        return new AlertDialog.Builder(SplashScreen.this)
                .setIcon(R.drawable.willy_shmo_small_icon)
                .setTitle(R.string.google_play_service_error)
                .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* User clicked OK so do some stuff */
                        callGooglePlayServicesUtil(isPlayAvailable);
                        setSplashActive(false);
                    }
                })
                .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* User clicked Cancel so do some stuff */
                        setSplashActive(false);
                    }
                })
                .setMessage(playErrorMessage)
                .create();
    }

    private void callGooglePlayServicesUtil(int isPlayAvailable) {
        GooglePlayServicesUtil.getErrorDialog(isPlayAvailable, SplashScreen.this, 99);
    }

    private void setSplashActive(boolean active) {
        mActive = active;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        setSplashActive(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}