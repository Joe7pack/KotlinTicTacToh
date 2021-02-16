package com.guzzardo.android.willyshmo.kotlintictacdoh;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.Toast;

import com.guzzardo.android.willyshmo.kotlintictacdoh.MainActivity.UserPreferences;

public class PrizesAvailableActivity extends Activity implements ToastMessage {
	
    public static ErrorHandler errorHandler;   
    private static Resources mResources;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mResources = getResources();
        errorHandler = new ErrorHandler();
        final boolean customTitleSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        
        try {
            setContentView(R.layout.prize_frame);        
            if (customTitleSupported) {
            	getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.prizes_title);
            }    
        } catch (Exception e) {
        	sendToastMessage(e.getMessage());
        }
    }

    /**
     * This is the "top-level" fragment, showing a list of items that the
     * user can pick.  Upon picking an item, it takes care of displaying the
     * data to the user as appropriate based on the currrent UI layout.
     */

    public static class PrizesAvailableFragment extends ListFragment {
        boolean mDualPane;
        int mCurCheckPosition = 0;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            /* uncomment this when we figure out why getActivity is of the wrong type */
            LazyAdapter adapter = new LazyAdapter(getActivity(), WillyShmoApplication.getPrizeNames(),
            	WillyShmoApplication.getBitmapImages(), WillyShmoApplication.getImageWidths(), WillyShmoApplication.getImageHeights(), 
            	WillyShmoApplication.getPrizeDistances(), WillyShmoApplication.getPrizeLocations(), mResources);            
    		setListAdapter(adapter);

            // Check to see if we have a frame in which to embed the details
            // fragment directly in the containing UI.
            //View detailsFrame = getActivity().findViewById(R.id.details);
            //mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

            if (mDualPane) {
                // In dual-pane mode, the list view highlights the selected item.
                getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                // Make sure our UI is in the correct state.
                showDetails(mCurCheckPosition);
            }
        }
        
        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt("curChoice", mCurCheckPosition);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
        	String url = WillyShmoApplication.getPrizeUrls()[position];
        	if (url != null && url.length() > 4) {
        		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + url));
        		startActivity(browserIntent);
        	}
        }
        
        /**
         * Helper function to show the details of a selected item, either by
         * displaying a fragment in-place in the current UI, or starting a
         * whole new activity in which it is displayed.
         */
        private void showDetails(int index) {
            mCurCheckPosition = index;

//            if (mDualPane) {
//                // We can display everything in-place with fragments, so update
//                // the list to highlight the selected item and show the data.
//                getListView().setItemChecked(index, true);
//
//                // Check what fragment is currently shown, replace if needed.
//                DetailsFragment details = (DetailsFragment)
//                        getFragmentManager().findFragmentById(R.id.details);
//                if (details == null || details.getShownIndex() != index) {
//                    // Make new fragment to show this selection.
//                    details = DetailsFragment.newInstance(index);
//
//                    // Execute a transaction, replacing any existing fragment
//                    // with this one inside the frame.
//                    FragmentTransaction ft = getFragmentManager().beginTransaction();
//                    if (index == 0) {
//                        ft.replace(R.id.details, details);
//                    } else {
//                        ft.replace(R.id.a_item, details);
//                    }
//                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
//                    ft.commit();
//                }
//
//            } else {
//                // Otherwise we need to launch a new activity to display
//                // the dialog fragment with selected text.
//                Intent intent = new Intent();
//                intent.setClass(getActivity(), DetailsActivity.class);
//                intent.putExtra("index", index);
//                startActivity(intent);
//            }
        }
    }
    
    private void getSharedPreferences() {
        SharedPreferences settings = getSharedPreferences(UserPreferences.PREFS_NAME, MODE_PRIVATE);
        //mPrizesAvailable = settings.getString("ga_prizes_available", null);
        //mPlayer1Id = settings.getInt(GameActivity.PLAYER1_ID, 0); 
        //mPlayer1Name = settings.getString(GameActivity.PLAYER1_NAME, null); 
    }
    
    private class ErrorHandler extends Handler {
        @Override
        public void handleMessage(Message msg)
        {
    		Toast.makeText(getApplicationContext(), (String)msg.obj, Toast.LENGTH_LONG).show();
        }
    }
    
    private static void writeToLog(String filter, String msg) {
    	if ("true".equalsIgnoreCase(mResources.getString(R.string.debug))) {
    		Log.d(filter, msg);
    	}
    }
    
	@Override
	public void sendToastMessage(String message) {
    	Message msg = PrizesAvailableActivity.errorHandler.obtainMessage();
    	msg.obj = message;
    	PrizesAvailableActivity.errorHandler.sendMessage(msg);	
	}

}

