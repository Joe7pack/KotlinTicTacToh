package com.guzzardo.android.willyshmo.kotlintictacdoh;

/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.Policy;
import com.google.android.vending.licensing.ServerManagedPolicy;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Actions;
import com.google.firebase.appindexing.builders.Indexables;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends Activity implements ToastMessage { //--, ConnectionCallbacks,
    private String mPlayer1Name, mPlayer2Name;
    private static double mLongitude, mLatitude;
    private static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 1;
    public static ErrorHandler mErrorHandler;
    private Button mPrizeButton;

    private String mText = "Joes text here";
    private String mUrl = "Joes url here";

    private static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt6bYx4PqPNnRxsW9DuBAOarpGA6ds7v866szk3e28yIF5LjV/EValnRMLsRylX8FP+BEYeGZvB6THbiQ5Gm7H8i+S2tUv6sngc894hBWZnQKAmwrwgl0Zm+vtYo8fnI6jppIxX4A9+4TrzW+Onl4LeW3kafJ9nIa3P73xSLhtFoxbGjBlEVhUQDVkRl27RXC5LuyULWzsYaUOCI9Yyf06DeDlahl2SwkRoTyB0+LdYsmp0fmw49OsW6P4FkLKvo3UGl75EZyTm3vd8oze4NXNy9GiSxpfD12jhtToKDub/qd7EMJrFadUkuGoTg/qQtmDk4YVoWJvLb26KcUH51PdQIDAQAB";

    // Generate your own 20 random bytes, and put them here.
    private static final byte[] SALT = new byte[] {
            -26, 85, 30, -128, -112, -57, 74, -64, 32, 88, -90, -45, 88, -117, -36, -113, -11, 32, -61,
            89
    };

    private AdView mAdView;

    private TextView mStatusText;
    private Button mCheckLicenseButton;

    private LicenseCheckerCallback mLicenseCheckerCallback;
    private LicenseChecker mChecker;

    // A handler on the UI thread.
    private Handler mHandler;

    @Override
    public void onStart() {
        super.onStart();

        ArrayList<Indexable> indexableNotes = new ArrayList<>();

        Indexable noteToIndex = Indexables.noteDigitalDocumentBuilder()
            .setName("Joes name Note")
            .setText("Joes text here")
            .setUrl("joe.guzzardo.com")
            .build();

        indexableNotes.add(noteToIndex);
        Indexable[] notesArr = new Indexable[indexableNotes.size()];
        notesArr = indexableNotes.toArray(notesArr);

        FirebaseApp.initializeApp(this);

        FirebaseAppIndex.getInstance().update(notesArr);
        FirebaseUserActions.getInstance().start(getAction());
        Log.d("MainActivity", "onStart called at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    }

    // After
    public Action getAction() {
        return Actions.newView(mText, mUrl);
    }

    @Override
    public void onStop() {
        FirebaseUserActions.getInstance().end(getAction());
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mChecker.onDestroy();
    }

    // Acquire a reference to the system Location Manager
    public interface UserPreferences {
        static final String PREFS_NAME = "TicTacDohPrefsFile";
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.main);
        mErrorHandler = new ErrorHandler();

        findViewById(R.id.rules).setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        showRules();
                    }
                });

        findViewById(R.id.about).setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        showAbout();
                    }
                });

        findViewById(R.id.two_player).setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        showTwoPlayers();
                    }
                });

        findViewById(R.id.one_player).setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        showOnePlayer();
                    }
                });

        findViewById(R.id.settings_dialog).setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        showDialogs();
                    }
                });

        mPrizeButton = (Button) findViewById(R.id.prizes_dialog);
        mPrizeButton.setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        showPrizes();
                    }
                });

        mStatusText = (TextView) findViewById(R.id.status_text);

        mCheckLicenseButton = (Button) findViewById(R.id.check_license_button);
        mCheckLicenseButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                doCheck();
            }
        });

        // FIXME - set animation for Prizes button only if we are connected to the network
        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(500); //You can manage the time of the blink with this parameter
        anim.setStartOffset(20);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        mPrizeButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.backwithgreenborder));
        mPrizeButton.startAnimation(anim);

        if (WillyShmoApplication.Companion.isNetworkAvailable()) {
            mPrizeButton.setVisibility(View.VISIBLE);
        } else {
            mPrizeButton.setVisibility(View.GONE);
        }

        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                //.addTestDevice("EE90BD2A7578BC19014DE8617761F10B") //Samsung Galaxy Note
                // Create an ad request. Check your logcat output for the hashed device ID to
                // get test ads on a physical device. e.g.
                // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
                //.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build(); //mAdView.loadAd(adRequest);

        // Start loading the ad in the background.
        mAdView.loadAd(adRequest);

        //adRequest.addTestDevice(AdRequest.TEST_EMULATOR);             // Android emulator
        //adRequest.addTestDevice("5F310740585B99B1179370AC1B4490C4"); // My T-Mobile G1 Test Phone
        //adRequest.addTestDevice("EE90BD2A7578BC19014DE8617761F10B");  // My Samsung Note

        mHandler = new Handler();

        // Try to use more data here. ANDROID_ID is a single point of attack.
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Library calls this when it's done.
        mLicenseCheckerCallback = new MyLicenseCheckerCallback();
        // Construct the LicenseChecker with a policy.
        mChecker = new LicenseChecker(
                this, new ServerManagedPolicy(this,
                new AESObfuscator(SALT, getPackageName(), deviceId)),
                BASE64_PUBLIC_KEY);
        //doCheck();

    }

    private void showRules() {
        Intent i = new Intent(this, RulesActivity.class);
        startActivity(i);
    }

    private void showAbout() {
        Intent i = new Intent(this, AboutActivity.class);
        startActivity(i);
    }

    private void showOnePlayer() {
        Intent i = new Intent(this, OnePlayerActivity.class);
        i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name);
        i.putExtra(GameActivity.PLAYER2_NAME, mPlayer2Name);
        startActivity(i);
    }

    private void showTwoPlayers() {
        Intent i = new Intent(this, TwoPlayerActivity.class);
        i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name);
        i.putExtra(GameActivity.PLAYER2_NAME, mPlayer2Name);
        startActivity(i);
    }

    private void showDialogs() {
        Intent i = new Intent(this, SettingsDialogs.class);
        i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name);
        i.putExtra(GameActivity.PLAYER2_NAME, mPlayer2Name);
        //FIXME - why startActivityForResult?
        startActivityForResult(i, 1);
    }

    private void showPrizes() {
        Intent i = new Intent(this, PrizesAvailableActivity.class);
        startActivity(i);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("gm_player1_name", mPlayer1Name);
        savedInstanceState.putString("gm_player2_name", mPlayer2Name);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        mPlayer1Name = savedInstanceState.getString("gm_player1_name");
        mPlayer2Name = savedInstanceState.getString("gm_player2_name");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restore preferences
        SharedPreferences settings = getSharedPreferences(UserPreferences.PREFS_NAME, MODE_PRIVATE);
        mPlayer1Name = settings.getString(GameActivity.PLAYER1_NAME, "Player 1");
        mPlayer2Name = settings.getString(GameActivity.PLAYER2_NAME, "Player 2");
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private class ErrorHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
        }
    }

    public void sendToastMessage(String message) {
        Message msg = MainActivity.mErrorHandler.obtainMessage();
        msg.obj = message;
        MainActivity.mErrorHandler.sendMessage(msg);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    protected Dialog onCreateDialog(int id) {
        final boolean bRetry = id == 1;
        return new AlertDialog.Builder(this)
                .setTitle(R.string.unlicensed_dialog_title)
                .setMessage(bRetry ? R.string.unlicensed_dialog_retry_body : R.string.unlicensed_dialog_body)
                .setPositiveButton(bRetry ? R.string.retry_button : R.string.buy_button, new DialogInterface.OnClickListener() {
                    boolean mRetry = bRetry;
                    public void onClick(DialogInterface dialog, int which) {
                        if ( mRetry ) {
                            doCheck();
                        } else {
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                                    "http://market.android.com/details?id=" + getPackageName()));
                            startActivity(marketIntent);
                        }
                    }
                })
                .setNegativeButton(R.string.quit_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).create();
    }

    private void doCheck() {
        mCheckLicenseButton.setEnabled(false);
        setProgressBarIndeterminateVisibility(true);
        mStatusText.setText(R.string.checking_license);
        mChecker.checkAccess(mLicenseCheckerCallback);
    }

    private void displayResult(final String result) {
        mHandler.post(new Runnable() {
            public void run() {
                mStatusText.setText(result);
                setProgressBarIndeterminateVisibility(false);
                mCheckLicenseButton.setEnabled(true);
            }
        });
    }

    private void displayDialog(final boolean showRetry) {
        mHandler.post(new Runnable() {
            public void run() {
                setProgressBarIndeterminateVisibility(false);
                showDialog(showRetry ? 1 : 0);
                mCheckLicenseButton.setEnabled(true);
            }
        });
    }

    private class MyLicenseCheckerCallback implements LicenseCheckerCallback {
        public void allow(int policyReason) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            // Should allow user access.
            displayResult(getString(R.string.allow));
        }

        public void dontAllow(int policyReason) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            displayResult(getString(R.string.dont_allow));
            // Should not allow access. In most cases, the app should assume
            // the user has access unless it encounters this. If it does,
            // the app should inform the user of their unlicensed ways
            // and then either shut down the app or limit the user to a
            // restricted set of features.
            // In this example, we show a dialog that takes the user to Market.
            // If the reason for the lack of license is that the service is
            // unavailable or there is another problem, we display a
            // retry button on the dialog and a different message.
            displayDialog(policyReason == Policy.RETRY);
        }

        public void applicationError(int errorCode) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            // This is a polite way of saying the developer made a mistake
            // while setting up or calling the license checker library.
            // Please examine the error code and fix the error.
            //String result = String.format(getString(R.string.application_error, errorCode);
            String result = " applicationError: " + errorCode;
            displayResult(result);
        }
    }

}