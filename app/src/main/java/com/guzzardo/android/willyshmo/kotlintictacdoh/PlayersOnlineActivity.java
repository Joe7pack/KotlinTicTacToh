package com.guzzardo.android.willyshmo.kotlintictacdoh;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.guzzardo.android.willyshmo.kotlintictacdoh.MainActivity.UserPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

//import android.support.v4.app.FragmentManager;
//import android.support.v4.app.FragmentTransaction;

//import PlayersOnlineActivity.PlayersOnlineFragment.ListThread;

public class PlayersOnlineActivity extends Activity implements ToastMessage {
	
    private String mUsersOnline;
    private static String [] mUserNames;
    private static String [] mUserIds;  
    private static Integer mPlayer1Id;
    private static String mPlayer1Name;
    private static Context mApplicationContext;
    public static ErrorHandler errorHandler;   
    private static Resources mResources;
    private static PlayersOnlineActivity mPlayersOnlineActivity;
    private static int mSelectedPosition = -1;
    private static RabbitMQMessageConsumer mMessageConsumer;
    //private static boolean mWaitingForOpponent = true;
    private static RabbitMQPlayerResponseHandler mRabbitMQPlayerResponseHandler;
    private String mRabbitMQPlayerResponse;
    //private static String mOpponentName;
    //private static String mOpponentId;
    //private static ListThread mWaitForOpponent;

    //TODO - consider saving mUserNames and mUserIds in savedInstanceState and changing AndroidManifest.PlayersOnlineActivity 
    // android:noHistory to false so that we can restore prior list when user presses back button in GameActivity
    // This will make replaying simpler since the user will see the prior list of users online when leaving a game instead of 
    // being sent back to the 2 Players screen.
    // The downside is that this list will become increasingly inaccurate as other players enter and leave the online list.
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mResources = getResources();
        errorHandler = new ErrorHandler();
        mApplicationContext = getApplicationContext();
        getSharedPreferences();
        mPlayersOnlineActivity = this;
        mPlayer1Name = getIntent().getStringExtra(GameActivity.PLAYER1_NAME);
        getPlayersOnline();
        resetPlayersOnline();
        	
        if (mUserNames.length == 0) {
        	writeToLog("PlayersOnlineActivity", "starting server only");
           	Intent i = new Intent(mApplicationContext, GameActivity.class);
           	i.putExtra(GameActivity.START_SERVER, "true");
           	i.putExtra(GameActivity.PLAYER1_ID, mPlayer1Id); 
           	i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name);
           	startActivity(i);
           	finish();
        } else {
            mMessageConsumer = new RabbitMQMessageConsumer(this, mResources);
            mRabbitMQPlayerResponseHandler = new RabbitMQPlayerResponseHandler();
            setContentView(R.layout.players_online); //this starts up the list view
        }
    }
    
    private class RabbitMQPlayerResponseHandler implements RabbitMQResponseHandler {
    	public void setRabbitMQResponse(String rabbitMQResponse) {
  	  		mRabbitMQPlayerResponse = rabbitMQResponse;
  	  	}
		
  	  	public String getRabbitMQResponse() {
  	  		return mRabbitMQPlayerResponse;
  	  	}
    }
    
  	private static void setUpMessageConsumer(RabbitMQMessageConsumer rabbitMQMessageConsumer, final String qNameQualifier, final RabbitMQResponseHandler rabbitMQResponseHandler) { 
  		//String hostName = mResources.getString(R.string.RabbitMQHostName);
        String hostName = (String)WillyShmoApplication.getConfigMap("RabbitMQIpAddress");
        String queuePrefix = (String)WillyShmoApplication.getConfigMap("RabbitMQQueuePrefix");
  		String qName = queuePrefix + "-" + qNameQualifier + "-" + mPlayer1Id;
  		new ConsumerConnectTask().execute(hostName, rabbitMQMessageConsumer, qName, mPlayersOnlineActivity, mResources, "fromPlayersOnlineActivity");
  		writeToLog("PlayersOnlineActivity", qNameQualifier +" message consumer listening on queue: " + qName);		
		
  		// register for messages
  		rabbitMQMessageConsumer.setOnReceiveMessageHandler(new RabbitMQMessageConsumer.OnReceiveMessageHandler() {
  			public void onReceiveMessage(byte[] message) {
  				String text = "";
  				try {
  					text = new String(message, "UTF8");
  				} catch (UnsupportedEncodingException e) {
  					mPlayersOnlineActivity.sendToastMessage(e.getMessage());
  				}
                rabbitMQResponseHandler.setRabbitMQResponse(text);
  				writeToLog("PlayersOnlineActivity", qNameQualifier + " OnReceiveMessageHandler received message: " + text);	
  			}
  		});
  	}
    
//    @Override
//    public void onBackPressed()
//    {
//        super.onBackPressed();
//        PlayersOnlineFragment.onBackPressed();
//    }    
    
    @Override
    public void onPause() {
    	super.onPause();
    	if (mSelectedPosition == -1) {
    		String urlData = "/gamePlayer/update/?id=" + mPlayer1Id + "&onlineNow=false&opponentId=0&userName=";
    		new SendMessageToWillyShmoServer().execute(urlData, mPlayer1Name, this, mResources, false);
    	}
    }
    
    /**
     * This is the "top-level" fragment, showing a list of items that the
     * user can pick.  Upon picking an item, it takes care of displaying the
     * data to the user as appropriate based on the currrent UI layout.
     */

    public static class PlayersOnlineFragment extends ListFragment {
        private boolean mDualPane;
        private int mCurCheckPosition = 0;
//        private ListThread mWaitForOpponent;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // Populate list with our static array of titles.
            setListAdapter(new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_list_item_activated_1, mUserNames));

            // Check to see if we have a frame in which to embed the details
            // fragment directly in the containing UI.
            //View detailsFrame = getActivity().findViewById(R.id.details);
            //mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

            if (savedInstanceState != null) {
                // Restore last state for checked position.
                mCurCheckPosition = savedInstanceState.getInt("curChoice", 0);
            }

            if (mDualPane) {
                // In dual-pane mode, the list view highlights the selected item.
                getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                // Make sure our UI is in the correct state.
                showDetails(mCurCheckPosition);
            }

//            mSelectedPosition = -1;
//            mRabbitMQPlayerResponseHandler.getRabbitMQResponse(); // get rid of any old game requests            
//            mRabbitMQPlayerResponseHandler.setRabbitMQResponse("null"); // get rid of any old game requests 
//           	mWaitForOpponent = new ListThread();
//           	mWaitForOpponent.start();
       }
        
        private void showAcceptDialog(String opponentName, String opponentId) {
            FragmentManager manager = this.getFragmentManager();
            FragmentTransaction ft = manager.beginTransaction();

            Bundle myBundle = new Bundle();
            myBundle.putString("opponentName", opponentName);
            myBundle.putString("opponentId", opponentId);
            myBundle.putString("playerName", mPlayer1Name);
            myBundle.putInt("player1Id", mPlayer1Id);
            AcceptGameDialog acceptGameDialog = new AcceptGameDialog();
            //AcceptGameDialog acceptGameDialog = new AcceptGameDialog(opponentName, opponentId, mPlayer1Name, mPlayer1Id, mApplicationContext, mResources);
            acceptGameDialog.setArguments(myBundle);
            acceptGameDialog.setContext(mApplicationContext);
            acceptGameDialog.setResources(mResources);
            acceptGameDialog.show(ft, "dialog");
        }
        
        private void showRejectDialog(String opponentName, String opponentId) {
            FragmentManager manager = this.getFragmentManager();
            FragmentTransaction ft = manager.beginTransaction();

            Bundle myBundle = new Bundle();
            myBundle.putString("opponentName", opponentName);
            myBundle.putString("opponentId", opponentId);
            myBundle.putString("playerName", mPlayer1Name);
            myBundle.putInt("player1Id", mPlayer1Id);
            RejectGameDialog rejectGameDialog = new RejectGameDialog();
            //RejectGameDialog rejectGameDialog = new RejectGameDialog(opponentName, opponentId, mPlayer1Name, mPlayer1Id, mApplicationContext, mResources);
            rejectGameDialog.setArguments(myBundle);
            rejectGameDialog.setContext(mApplicationContext);
            rejectGameDialog.setResources(mResources);
            rejectGameDialog.show(ft, "dialog");
        }
        
        @Override
        public void onResume() { //only called when at least one opponent is online to select 
            super.onResume();
            startGame();

//          This doesnt seen to work, the intent is, if there is only one selection available, then select it automatically
//            if (mUserNames.length == 1)
//                getListView().setSelection(0);


//         	mWaitForOpponent = new ListThread();
//         	mWaitForOpponent.start();
        }

        @Override
        public void onPause() {
        	super.onPause();
        	//mWaitingForOpponent = false;
         	new DisposeRabbitMQTask().execute(mMessageConsumer, mResources, mPlayersOnlineActivity);      
        }

        /*
        public class ListThread extends Thread { 
        	@Override
        	public void run() {
        		try {
                    mWaitingForOpponent = true;
                    
        			while (mWaitingForOpponent) {
        				sleep(1000);
        				if (null != mRabbitMQPlayerResponseHandler.getRabbitMQResponse()) { 
        					writeToLog("PlayersOnlineActivity", "Retrieving command: " + mRabbitMQPlayerResponseHandler.getRabbitMQResponse()); 
        					String [] responseValues = mRabbitMQPlayerResponseHandler.getRabbitMQResponse().split(",");
    						mRabbitMQPlayerResponseHandler.setRabbitMQResponse(null);
        					if (responseValues.length > 2) {
        						mOpponentName = responseValues[1];
        						mOpponentId = responseValues[2];
        						if ("noPlay".equals(responseValues[0])) {
        							showRejectDialog(mOpponentName, mOpponentId); //never called
        						}
        						if ("letsPlay".equals(responseValues[0])) {
        							showAcceptDialog(mOpponentName, mOpponentId);
        						}
        					}
        					//display pop up saying opposing player would like to play
        					//if this players accepts request from opponent then 
        					// perform logic similar to setUpClientAndServer()
        				}
        			}
        		} catch (Exception e) {
  					mPlayersOnlineActivity.sendToastMessage(e.getMessage());
        		}
        	}
        }
        */
        
        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt("curChoice", mCurCheckPosition);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
        	//mWaitingForOpponent = false;
            setUpClientAndServer(position);

            String hostName = (String)WillyShmoApplication.getConfigMap("RabbitMQIpAddress");
   			//String hostName = mResources.getString(R.string.RabbitMQHostName);
            String queuePrefix = (String)WillyShmoApplication.getConfigMap("RabbitMQQueuePrefix");
   			String qName = queuePrefix + "-" + "startGame"  + "-" +  mUserIds[position];
   			
   			String messageToOpponent = "letsPlay," + mPlayer1Name + ","  + mPlayer1Id; //mUserIds[position];
   			new SendMessageToRabbitMQTask().execute(hostName, qName, null, messageToOpponent, mPlayersOnlineActivity, mResources);  
            mSelectedPosition = position;
        }
        
        private void setUpClientAndServer(int which) {
	        SharedPreferences settings = mApplicationContext.getSharedPreferences(UserPreferences.PREFS_NAME, 0);
	        SharedPreferences.Editor editor = settings.edit();
	        editor.putString("ga_opponent_screenName", mUserNames[which]);
	        //editor.commit();
            editor.apply();
        	
        	Intent i = new Intent(mApplicationContext, GameActivity.class);
        	i.putExtra(GameActivity.START_SERVER, "true");
            i.putExtra(GameActivity.START_CLIENT, "true"); //this will send the new game to the client
        	i.putExtra(GameActivity.PLAYER1_ID, mPlayer1Id); 
        	i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name);
        	i.putExtra(GameActivity.START_CLIENT_OPPONENT_ID, mUserIds[which]);
            i.putExtra(GameActivity.PLAYER2_NAME, mUserNames[which]);
            i.putExtra(GameActivity.START_FROM_PLAYER_LIST, "true"); 
            writeToLog("PlayersOnlineActivity", "starting client and server");
        	startActivity(i);
        }
        
        /**
         * Helper function to show the details of a selected item, either by
         * displaying a fragment in-place in the current UI, or starting a
         * whole new activity in which it is displayed.
         */
        void showDetails(int index) {
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
        
//        static void onBackPressed() {
//        	System.out.println("on back pressed");
//        }

    }

    static private void startGame() {
        setUpMessageConsumer(mMessageConsumer, "startGame", mRabbitMQPlayerResponseHandler);
        mSelectedPosition = -1;
        mRabbitMQPlayerResponseHandler.setRabbitMQResponse("null"); // get rid of any old game requests
    }
    
    private void getPlayersOnline() {
    	TreeMap<String, HashMap<String, String>> users = parseUserList(mUsersOnline);

    	TreeMap<String, HashMap<String, String>> usersClone = (TreeMap<String, HashMap<String, String>>) users.clone();    	
    	//we're creating a clone because removing an entry from the original TreeMap causes a problem for the iterator
    	Set<String> userKeySet = usersClone.keySet(); // this is where the keys (userNames) gets sorted
    	Iterator<String> keySetIterator = userKeySet.iterator();

    	while (keySetIterator.hasNext()) {
    		String key = keySetIterator.next();
    		HashMap<String, String> userValues = users.get(key);
    		String userId = userValues.get("userId");
    		if (userId.equals(Integer.toString(mPlayer1Id))) //not going to play against myself on the network
    			users.remove(key);
    	}
    	
    	Object[] objectArray = users.keySet().toArray();
    	mUserNames = new String[objectArray.length];
    	mUserIds = new String[objectArray.length];
    	for (int x = 0; x < objectArray.length; x++) {
    		mUserNames[x] = (String)objectArray[x];
    		
            HashMap<String, String> userValues = users.get(objectArray[x]);
            mUserIds[x] =  userValues.get("userId");
            //System.out.println("user id: " + mUserIds[x] + " for: " + mUserNames[x]);
    	}
    }
    
    private TreeMap<String, HashMap<String, String>> parseUserList(String usersLoggedOn) {
    	TreeMap<String, HashMap<String, String>> userTreeMap = new TreeMap<String, HashMap<String, String>>();
    	
    	try {
    		JSONObject jsonObject = new JSONObject(usersLoggedOn);
    		JSONArray userArray = jsonObject.getJSONArray("UserList"); 
        	
        	for (int y = 0; y < userArray.length(); y++) {
        		HashMap<String, String> userMapValues = new HashMap<String, String>();
        		JSONObject userValues = userArray.getJSONObject(y);
        		String userId = userValues.getString("id");
        		String userName = userValues.getString("userName");
        		userMapValues.put("userId", userId);
        		userTreeMap.put(userName, userMapValues);  
        	}
        } catch (JSONException e) {
//            e.printStackTrace();	
        	writeToLog("PlayersOnlineActivity", "parseUserList: " + e.getMessage());
			sendToastMessage(e.getMessage());			
        }
        return userTreeMap;
    }
    
    private void getSharedPreferences() {
        SharedPreferences settings = getSharedPreferences(UserPreferences.PREFS_NAME, MODE_PRIVATE);
        mUsersOnline = settings.getString("ga_users_online", null);
        mPlayer1Id = settings.getInt(GameActivity.PLAYER1_ID, 0); 
        //mPlayer1Name = settings.getString(GameActivity.PLAYER1_NAME, null); 
    }
    
    private void resetPlayersOnline() {
        SharedPreferences settings = getSharedPreferences(UserPreferences.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("ga_users_online", null);
        // Commit the edits!
        //editor.commit();
        editor.apply();
    }
    
    
//    @Override
//    public void onBackPressed() {
//    	Fragment fragment = getFragmentManager().findFragmentById(R.id.players_online);
//   	   if (fragment instanceOf YourFragmet) {
//    	   super.onBackPressed();
    	
//  	          finish();
//   	          return;
//   	   }
//   	}    
    
	/**
     * A simple utility Handler to display an error message as a Toast popup
     */
    
    private class ErrorHandler extends Handler {
        @Override
        public void handleMessage(Message msg)
        {
    		Toast.makeText(getApplicationContext(), (String)msg.obj, Toast.LENGTH_LONG).show();
        }
    }
    
    public void sendToastMessage(String message) {
    	Message msg = PlayersOnlineActivity.errorHandler.obtainMessage();
    	msg.obj = message;
    	PlayersOnlineActivity.errorHandler.sendMessage(msg);	
    }
    
    private static void writeToLog(String filter, String msg) {
    	if ("true".equalsIgnoreCase(mResources.getString(R.string.debug))) {
    		Log.d(filter, msg);
    	}
    }
    
}
