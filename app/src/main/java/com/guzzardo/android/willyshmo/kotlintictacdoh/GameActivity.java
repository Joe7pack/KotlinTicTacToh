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

package com.guzzardo.android.willyshmo.kotlintictacdoh;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.guzzardo.android.willyshmo.kotlintictacdoh.MainActivity.UserPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameActivity extends Activity implements ToastMessage {

    public static ErrorHandler errorHandler;
    private static final String packageName = "com.guzzardo.android.willyshmo.kotlintictacdoh";

	/** Start player. Must be 1 or 2. Default is 1. */
    public static final String EXTRA_START_PLAYER =
            packageName + ".GameActivity.EXTRA_START_PLAYER";
    public static final String START_PLAYER_HUMAN =
            packageName + ".GameActivity.START_PLAYER_HUMAN";
    public static final String PLAYER1_NAME =
            packageName + ".GameActivity.PLAYER1_NAME";
    public static final String PLAYER2_NAME =
            packageName + ".GameActivity.PLAYER2_NAME";
    public static final String PLAYER1_SCORE =
            packageName + ".GameActivity.PLAYER1_SCORE";
    public static final String PLAYER2_SCORE =
            packageName + ".GameActivity.PLAYER2_SCORE";
    public static final String WILLY_SCORE =
            packageName + ".GameActivity.WILLY_SCORE";
    public static final String START_SERVER =
            packageName + ".GameActivity.START_SERVER";
    public static final String START_CLIENT =
            packageName + ".GameActivity.START_CLIENT";
    public static final String START_CLIENT_OPPONENT_ID =
            packageName + ".GameActivity.START_CLIENT_OPPONENT_ID";
    public static final String PLAYER1_ID =
            packageName + ".GameActivity.PLAYER1_ID";
    public static final String START_OVER_LAN =
            packageName + ".GameActivity.START_OVER_LAN";
    public static final String MOVE_MODE =
            packageName + ".GameActivity.MOVE_MODE";
    public static final String SOUND_MODE =
            packageName + ".GameActivity.SOUND_MODE";
    public static final String TOKEN_SIZE =
            packageName + ".GameActivity.TOKEN_SIZE";
    public static final String TOKEN_COLOR_1 =
            packageName + ".GameActivity.TOKEN_COLOR_1";
    public static final String TOKEN_COLOR_2 =
            packageName + ".GameActivity.TOKEN_COLOR_2";
    public static final String HAVE_OPPONENT =
            packageName + ".GameActivity.HAVE_OPPONENT";
    public static final String START_FROM_PLAYER_LIST =
            packageName + ".GameActivity.START_FROM_PLAYER_LIST";
    
    private static final int MSG_COMPUTER_TURN = 1;
    private static final int NEW_GAME_FROM_CLIENT = 2;
    private static final int MSG_NETWORK_CLIENT_TURN = 3; 
    private static final int MSG_NETWORK_SERVER_TURN = 4; 
    private static final int MSG_NETWORK_SET_TOKEN_CHOICE = 5;
    private static final int DISMISS_WAIT_FOR_NEW_GAME_FROM_CLIENT = 6;
    private static final int DISMISS_WAIT_FOR_NEW_GAME_FROM_HOST = 7;
    private static final int ACCEPT_INCOMING_GAME_REQUEST_FROM_CLIENT = 8;
    private static final int MSG_NETWORK_CLIENT_MAKE_FIRST_MOVE = 9;
    private static final int MSG_HOST_UNAVAILABLE = 10;
    private static final int MSG_NETWORK_SERVER_REFUSED_GAME = 11;
    private static final int MSG_NETWORK_SERVER_LEFT_GAME = 12;

    private static final long COMPUTER_DELAY_MS = 500;
    private static final int THREAD_SLEEP_INTERVAL = 300; //milliseconds
    private static final int mRegularWin = 10;
    private static final int mSuperWin = 30;
    private boolean mServer, mClient;
    private static int mPlayer1Id;
    private static String mPlayer2Id;

    private Handler mHandler = new Handler(new MyHandlerCallback());
    private static GameView mGameView;
    private Button mButtonNext;
    private int mPlayer1TokenChoice = GameView.BoardSpaceValues.EMPTY;
    private int mPlayer2TokenChoice = GameView.BoardSpaceValues.EMPTY; // computer or opponent
    private TextView mPlayer1ScoreTextValue, mPlayer2ScoreTextValue;
    private EditText mPlayer1NameTextValue, mPlayer2NameTextValue;
    private ImageView mGameTokenPlayer1, mGameTokenPlayer2;
    private Map<Integer, Integer> humanWinningHashMap = new HashMap<Integer, Integer>();
    private static int mPlayer1Score, mPlayer2Score, mWillyScore;  
    private static int mPlayer1NetworkScore, mPlayer2NetworkScore;
    private static String mPlayer1Name, mPlayer2Name;
   
    private static boolean mMoveModeTouch; //false = drag move mode; true = touch move mode
    private static boolean mSoundMode; //false = no sound; true = sound
    private static boolean HUMAN_VS_HUMAN;
    private static boolean HUMAN_VS_NETWORK;
    private static int mSavedCell; //hack for saving cell selected when XO token is chosen as first move
    private static CharSequence mButtonStartText;
    
    private static boolean mServerRunning, mClientRunning;
    private ClientThread mClientThread;
    private ServerThread mServerThread;
    private static boolean imServing;

    private List<int[]> mTokensFromClient;  
    private Random mRandom = new Random();
    private static int mBallMoved; //hack for correcting problem with resetting mBallId to -1 in mGameView.disableBall()
    private static Resources resources;
    private static ProgressDialog mHostWaitDialog, mClientWaitDialog;
    private static AlertDialog mChooseTokenDialog;
    
	private RabbitMQMessageConsumer mMessageClientConsumer, mMessageServerConsumer, mMessageStartGameConsumer;
	private String mRabbitMQClientResponse, mRabbitMQServerResponse, mRabbitMQStartGameResponse;
	private static String mNetworkOpponentPlayerName;
	private static int mLastCellSelected;
	
	private RabbitMQServerResponseHandler mRabbitMQServerResponseHandler;
	private RabbitMQClientResponseHandler mRabbitMQClientResponseHandler;
	private RabbitMQStartGameResponseHandler mRabbitMQStartGameResponseHandler;
	
	private String mServerHasOpponent;
//	private boolean mGameStartedFromPlayerList;
    private static String mHostName;
    private static String mQueuePrefix;
	
    public interface PrizeValue {
    	static final int SHMOGRANDPRIZE = 4; //player wins with a Shmo and shmo card was placed on prize card
    	static final int SHMOPRIZE = 2; //player wins with a shmo
    	static final int GRANDPRIZE = 3; //player wins by placing winning card on prize token
    	static final int REGULARPRIZE = 1; //player wins with any card covering the prize
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        resources = getResources();
        setContentView(R.layout.lib_game);
        mGameView = (GameView) findViewById(R.id.game_view);
        mButtonNext = (Button) findViewById(R.id.next_turn);
        mButtonStartText = mButtonNext.getText();
        mPlayer1ScoreTextValue = (TextView)findViewById(R.id.player1_score);
        mPlayer2ScoreTextValue = (TextView)findViewById(R.id.player2_score);        
        mPlayer1NameTextValue = (EditText)findViewById(R.id.player1_name);
        mPlayer2NameTextValue = (EditText)findViewById(R.id.player2_name);     
        mGameTokenPlayer1 = (ImageView) findViewById(R.id.player1_token);
        mGameTokenPlayer2 = (ImageView) findViewById(R.id.player2_token);        
        
        mGameView.setFocusable(true);
        mGameView.setFocusableInTouchMode(true);
        mGameView.setCellListener(new MyCellListener());

        mButtonNext.setOnClickListener(new MyButtonListener());
        
        HUMAN_VS_HUMAN = false;
        HUMAN_VS_NETWORK = false;
        
        SharedPreferences settings = getSharedPreferences(UserPreferences.PREFS_NAME, MODE_PRIVATE); 
        mPlayer1Score = settings.getInt(GameActivity.PLAYER1_SCORE, 0);
        mPlayer2Score = settings.getInt(GameActivity.PLAYER2_SCORE, 0);
        mWillyScore = settings.getInt(GameActivity.WILLY_SCORE, 0); 
        mMoveModeTouch = settings.getBoolean(GameActivity.MOVE_MODE, false);
        mSoundMode = settings.getBoolean(GameActivity.SOUND_MODE, false);
        
        mGameView.setViewDisabled(false);

        mHostName = (String)WillyShmoApplication.getConfigMap("RabbitMQIpAddress");
        mQueuePrefix = (String)WillyShmoApplication.getConfigMap("RabbitMQQueuePrefix");
    }
    
	private ProgressDialog showHostWaitDialog() {
		String opponentName = mPlayer2Name == null ? "Waiting for player to connect..." : "Waiting for " + mPlayer2Name + " to connect...";
		String hostingDescription = mPlayer2Name == null ? "Hosting... (Ask a friend to install Willy Shmo\'s Tic Tac Toe)" : "Hosting...";
        mGameView.setGamePrize();

		return ProgressDialog.show(GameActivity.this, hostingDescription, opponentName, true, true,
				new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				GameActivity.this.finish();
			}
		});
	}

	private ProgressDialog showClientWaitDialog() {
        final ProgressDialog show = ProgressDialog.show(GameActivity.this, "Connecting...", "to " + mPlayer2Name, true, true,
                new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        GameActivity.this.finish();
                    }
                });
        return show;
	}

    @Override
    protected void onResume() {
        super.onResume();
        
        mGameView.setGameActivity(this);
        mGameView.setClient(null);
        mServer = Boolean.valueOf(getIntent().getStringExtra(START_SERVER));
        
        if (mServer && !mServerRunning) {
        	mPlayer1Id = getIntent().getIntExtra(PLAYER1_ID, 0);
            mServerThread = new ServerThread(); 
            mMessageServerConsumer = new RabbitMQMessageConsumer(GameActivity.this, resources);
            mRabbitMQServerResponseHandler = new RabbitMQServerResponseHandler();
            mRabbitMQServerResponseHandler.setRabbitMQResponse("null");

            setUpMessageConsumer(mMessageServerConsumer, "server", mRabbitMQServerResponseHandler); 
            mPlayer1Name = getIntent().getStringExtra(PLAYER1_NAME);
            mServerRunning = true;             
            mServerThread.start();
            HUMAN_VS_NETWORK = true;
            mServerHasOpponent = getIntent().getStringExtra(HAVE_OPPONENT);
        }  

        mClient = Boolean.valueOf(getIntent().getStringExtra(START_CLIENT));
        if (mClient && !mClientRunning) {
        	mPlayer1Id = getIntent().getIntExtra(PLAYER1_ID, 0);
        	String clientOpponentId = getIntent().getStringExtra(START_CLIENT_OPPONENT_ID);
        	mPlayer2Id = clientOpponentId;
        	mClientThread = new ClientThread();
            mMessageClientConsumer = new RabbitMQMessageConsumer(GameActivity.this, resources);
            mRabbitMQClientResponseHandler = new RabbitMQClientResponseHandler();    
            mRabbitMQClientResponseHandler.setRabbitMQResponse("null");
            
        	setUpMessageConsumer(mMessageClientConsumer, "client", mRabbitMQClientResponseHandler);        	
            mClientThread.start();
            mClientRunning = true;
            HUMAN_VS_NETWORK = true;            
            mGameView.setClient(mClientThread); //this is where we inform GameView to send game tokens to network opponent when the GameView is created
            mPlayer2Name = getIntent().getStringExtra(PLAYER2_NAME);
            mClientWaitDialog = showClientWaitDialog();
            mClientWaitDialog.show();
        }    
        
        if (mServer && !mClient) {
            mPlayer1NetworkScore = mPlayer2NetworkScore = 0;
            mPlayer2Name = null;
            displayScores();
            mHostWaitDialog = showHostWaitDialog();
            mHostWaitDialog.show();
            
        	String androidId = "&deviceId=" + WillyShmoApplication.getAndroidId(); 
        	String latitude = "&latitude=" + WillyShmoApplication.getLatitude();
        	String longitude = "&longitude=" + WillyShmoApplication.getLongitude();
        	String trackingInfo = androidId + latitude + longitude;

            String urlData = "/gamePlayer/update/?onlineNow=true&playingNow=false&opponentId=0" + trackingInfo + "&id="
				+ mPlayer1Id + "&userName=";
    		new SendMessageToWillyShmoServer().execute(urlData, mPlayer1Name, GameActivity.this, resources, Boolean.valueOf(false));    		
        	return;
        }
        
        GameView.State player = mGameView.getCurrentPlayer();
        if (player == GameView.State.UNKNOWN) {
        	player = GameView.State.fromInt(getIntent().getIntExtra(START_PLAYER_HUMAN, -3));
        	if (player == GameView.State.UNKNOWN) {
        		player = GameView.State.fromInt(getIntent().getIntExtra(EXTRA_START_PLAYER, 1));
        	} else { 
        		HUMAN_VS_HUMAN = true;
        	}
        	
        	mGameView.setHumanState(HUMAN_VS_HUMAN);
            mPlayer1Name = getIntent().getStringExtra(PLAYER1_NAME);
            mPlayer2Name = getIntent().getStringExtra(PLAYER2_NAME);
            if (!checkGameFinished(player, false)) {
                selectTurn(player);
            }
        }
        
        mGameView.setGamePrize(); //works only from client side but server side never call onResume when starting a game
        //but if we just play against Willy then onResume is called
        
        mPlayer1NetworkScore = mPlayer2NetworkScore = 0;
        displayScores();
        highlightCurrentPlayer(player);
        showPlayerTokenChoice();
        
        if (player == GameView.State.PLAYER2 && !(HUMAN_VS_HUMAN | HUMAN_VS_NETWORK)) {
            mHandler.sendEmptyMessageDelayed(MSG_COMPUTER_TURN, COMPUTER_DELAY_MS);
        }
        
        if (player == GameView.State.WIN) {
            setWinState(mGameView.getWinner());
        }
        
        mGameView.setViewDisabled(false);
    }
    
    private GameView.State selectTurn(GameView.State player) {
        mGameView.setCurrentPlayer(player);
        mButtonNext.setEnabled(false);

        if (player == GameView.State.PLAYER1) {
            mGameView.setEnabled(true);

        } else if (player == GameView.State.PLAYER2) {
            mGameView.setEnabled(false);
        }
        return player;
    }

    private class MyCellListener implements GameView.ICellListener {
    	public void onCellSelected() {
    		int cell = mGameView.getSelection();
    		mButtonNext.setEnabled(cell >= 0);
    		if (cell >= 0) {
    			playHumanMoveSound();
    			mLastCellSelected = cell;
    		}
    	}
    }
    
    private class MyButtonListener implements OnClickListener {
    	
    	AlertDialog showChooseTokenDialog() {
            return new AlertDialog.Builder(GameActivity.this)
            .setIcon(R.drawable.willy_shmo_small_icon)
            .setTitle(R.string.alert_dialog_starting_token_value)
            .setSingleChoiceItems(R.array.select_starting_token, 0, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	switch(whichButton) { 
                	case (0) :  
                		if (HUMAN_VS_HUMAN) {
                			if (mGameView.getCurrentPlayer() == GameView.State.PLAYER1) {
                				mPlayer1TokenChoice = GameView.BoardSpaceValues.CIRCLE;
                				mPlayer2TokenChoice = GameView.BoardSpaceValues.CROSS;
                			} else { 
                				mPlayer1TokenChoice = GameView.BoardSpaceValues.CROSS; // else we're looking at player 2
                				mPlayer2TokenChoice = GameView.BoardSpaceValues.CIRCLE;
                			}
                			break; // so we set opposing player to opposing token choice
                		}
               			mPlayer1TokenChoice = GameView.BoardSpaceValues.CIRCLE;
               			mPlayer2TokenChoice = GameView.BoardSpaceValues.CROSS;
                		break;
                	case (1) :
                		if (HUMAN_VS_HUMAN) {
                			if (mGameView.getCurrentPlayer() == GameView.State.PLAYER1) {
                				mPlayer1TokenChoice = GameView.BoardSpaceValues.CROSS;
                				mPlayer2TokenChoice = GameView.BoardSpaceValues.CIRCLE;
                			} else { 
                				mPlayer1TokenChoice = GameView.BoardSpaceValues.CIRCLE;
                				mPlayer2TokenChoice = GameView.BoardSpaceValues.CROSS;
                			}
                			break;
                		}
               			mPlayer1TokenChoice = GameView.BoardSpaceValues.CROSS;
               			mPlayer2TokenChoice = GameView.BoardSpaceValues.CIRCLE;
                		break; 
                	} 
                	setGameTokenFromDialog();
                }
            })
           .create();
    	}
    	
    	private void sendNewGameToServer() {
    		mGameView.initalizeGameValues();
    		mGameView.setCurrentPlayer(GameView.State.PLAYER1);
            mButtonNext.setText(mButtonStartText);
            mButtonNext.setEnabled(false);
            mPlayer1TokenChoice = GameView.BoardSpaceValues.EMPTY;
            mPlayer2TokenChoice = GameView.BoardSpaceValues.EMPTY;
    		showPlayerTokenChoice();
    		mGameView.setTokenCards(); //generate new random list of tokens in mGameView.mGameTokenCard[x]
    		
    		for (int x = 0; x < GameView.mGameTokenCard.length; x++) {
    			if (x < 8) {
    				mGameView.updatePlayerToken(x, GameView.mGameTokenCard[x]); //update ball array
    			} else {
    				mGameView.setBoardSpaceValueCenter(GameView.mGameTokenCard[x]);
    			}
    		}
    		mGameView.sendTokensToServer(); 
    		mGameView.invalidate();
    		
            mClientWaitDialog = showClientWaitDialog();
            mClientWaitDialog.show();
    	}
    	
        public void onClick(View v) {
            GameView.State player = mGameView.getCurrentPlayer();
            
            String testText = mButtonNext.getText().toString();
            if (testText.endsWith("Play Again?")) { //game is over
            	if (imServing) {
                     mHostWaitDialog = showHostWaitDialog();
                     mHostWaitDialog.show();
            	} else if (mClientRunning) { //reset values on client side
//TODO - may want to consider moving this to a more appropriate location when a new game has actually started
            		sendNewGameToServer();
            	} else {
            		GameActivity.this.finish();
            	}

            } else if (player == GameView.State.PLAYER1 || player == GameView.State.PLAYER2) {
            	playFinishMoveSound();
                int cell = mGameView.getSelection();
                boolean okToFinish = true;
                if (cell >= 0) {
                	mSavedCell = cell;
                	int gameTokenPlayer1 = -1;
                    mGameView.stopBlink();
                    mGameView.setCell(cell, player);
                    mGameView.setBoardSpaceValue(cell);
                    if (mPlayer1TokenChoice == GameView.BoardSpaceValues.EMPTY) {
                        int tokenSelected = mGameView.getBoardSpaceValue(cell);
                        if (tokenSelected == GameView.BoardSpaceValues.CIRCLECROSS) {
                        	mChooseTokenDialog = showChooseTokenDialog();
                        	mChooseTokenDialog.show();
                        	okToFinish = false;
                        } else {
                        	if (player == GameView.State.PLAYER1) {
                        		mPlayer1TokenChoice = tokenSelected;
                        		mPlayer2TokenChoice = 
                        			mPlayer1TokenChoice ==  GameView.BoardSpaceValues.CIRCLE ? GameView.BoardSpaceValues.CROSS : GameView.BoardSpaceValues.CIRCLE;
                        	} else {
                        		mPlayer2TokenChoice = tokenSelected;
                        		mPlayer1TokenChoice = 
                        			mPlayer2TokenChoice ==  GameView.BoardSpaceValues.CIRCLE ? GameView.BoardSpaceValues.CROSS : GameView.BoardSpaceValues.CIRCLE;
                        	}
                        	mGameView.setPlayer1TokenChoice(mPlayer1TokenChoice);
                        	mGameView.setPlayer2TokenChoice(mPlayer2TokenChoice);
                        	gameTokenPlayer1 = mPlayer1TokenChoice;
                        	showPlayerTokenChoice();
                        }
                    }
                    if (okToFinish) {
                    	if (HUMAN_VS_NETWORK) {
                    		String movedMessage = "moved, " + mGameView.getBallMoved() + ", " + cell + ", " + gameTokenPlayer1;                    	
                    		if (imServing) {
                    			mServerThread.setMessageToClient(movedMessage);
                    		} else {
                    			mClientThread.setMessageToServer(movedMessage);
                    		}
                    		finishTurn(false, false, false); //don't send message to make computer move don't switch the player don't use player 2 for win testing
                    		GameView.State currentPlayer = mGameView.getCurrentPlayer();
                    		highlightCurrentPlayer(getOtherPlayer(currentPlayer));
                    		mGameView.setViewDisabled(true);
                    	} else {
                    		finishTurn(true, true, false); //send message to make computer move switch the player don't use player 2 for win testing
                    	}
                    } else {
                    	mBallMoved = mGameView.getBallMoved();
                    	mGameView.disableBall(); //disableBall sets ball moved to -1, so we need to get it first :-(
                    }
                }
            }
        }
    }
    
    private void setGameTokenFromDialog() {  // when player has chosen value for wildcard token
    	mChooseTokenDialog.dismiss();
    	if (HUMAN_VS_NETWORK) {
    		mGameView.setCurrentPlayer(GameView.State.PLAYER1);
//    		mPlayer2TokenChoice = mPlayer1TokenChoice ==  BoardSpaceValues.CIRCLE ? BoardSpaceValues.CROSS : BoardSpaceValues.CIRCLE;
    	}
    	if (!(HUMAN_VS_HUMAN | HUMAN_VS_NETWORK)) { //playing against Willy
    		setComputerMove();
    		finishTurn(false, false, false); //added to test if Willy wins 
    		mGameView.disableBall();
    	} else if (HUMAN_VS_HUMAN) {
    		finishTurn(false, true, false); //don't send message to make computer move but switch the player don't use player 2 for win testing
    		highlightCurrentPlayer(mGameView.getCurrentPlayer());
    	} else {
    		mGameView.disableBall();
        	highlightCurrentPlayer(GameView.State.PLAYER2);
    	}
    	showPlayerTokenChoice();
    	
    	String movedMessage = "moved, " + mBallMoved + ", " + mSavedCell + ", " +mPlayer1TokenChoice;                    	
    	
        if (HUMAN_VS_NETWORK) {
        	if (imServing) {
        		mServerThread.setMessageToClient(movedMessage);
        	} else {
        		mClientThread.setMessageToServer(movedMessage);
        	}
        	mGameView.setViewDisabled(true);
        }
    }   
    
    private void saveHumanWinner(int winningPositionOnBoard, int positionStatus) {
    	humanWinningHashMap.put(winningPositionOnBoard, positionStatus);
    	
    	//second value indicates position is available for use after comparing against other 
    	//entries in this map
    	//second value: initialized to -1 upon creation 
    	// set to 0 if not available
    	// set to 1 if available
    }
    
    private int[] selectBestMove() { //this is the heart and soul of Willy Shmo - at least as far as his skill at playing this game
    	int [] selectionArray = new int[2];
    	
    	int tokenSelected = -1;
    	int boardSpaceSelected = -1;
    	humanWinningHashMap.clear();
        
        if (mPlayer2TokenChoice == GameView.BoardSpaceValues.EMPTY) { //computer makes first move of game
        	tokenSelected = mGameView.selectRandomComputerToken();
        	//TODO - just for testing 1 specific case
        	//tokenSelected = mGameView.selectSpecificComputerToken(BoardSpaceValues.CROSS, true);        	
        	boardSpaceSelected = mGameView.selectRandomAvailableBoardSpace();
        } else {
        	// populate array with available moves        
    		int availableSpaceCount = 0;
    		boolean[] availableValues =  mGameView.getBoardSpaceAvailableValues();
    		int[] normalizedBoardPlayer1 = new int[GameView.BoardSpaceValues.BOARDSIZE];
    		int[] normalizedBoardPlayer2 = new int[GameView.BoardSpaceValues.BOARDSIZE];
    		
    		boolean[] testAvailableValues = new boolean[GameView.BoardSpaceValues.BOARDSIZE]; // false = not available
        	int tokenChoice = mGameView.selectSpecificComputerToken(mPlayer2TokenChoice, true); 
        	//true = playing offensively which means we can select the xo card if we have one
    		
            int[] boardSpaceValues = mGameView.getBoardSpaceValues();

            //populate test board with xo token changed to player 2 token
            for (int x = 0; x < normalizedBoardPlayer1.length; x++) {
            	normalizedBoardPlayer1[x] = boardSpaceValues[x];
            	normalizedBoardPlayer2[x] = boardSpaceValues[x];
            	
            	if (normalizedBoardPlayer1[x] == GameView.BoardSpaceValues.CIRCLECROSS) {
            		normalizedBoardPlayer1[x] = mPlayer1TokenChoice;
            		normalizedBoardPlayer2[x] = mPlayer2TokenChoice;
            	}
            }
            
			int trialBoardSpaceSelected1 = -1;
			int trialBoardSpaceSelected2 = -1;
			int trialBoardSpaceSelected3 = -1;  
			
			for (int x = 0; x < availableValues.length; x++) {
				if (availableValues[x]) {
					availableSpaceCount++;
					if (trialBoardSpaceSelected1 == -1) {
						trialBoardSpaceSelected1 = x;
					} else {
						if (trialBoardSpaceSelected2 == -1) {
							trialBoardSpaceSelected2 = x;
						} else {
							if (trialBoardSpaceSelected3 == -1) {
								trialBoardSpaceSelected3 = x;
							}
						}
					}
				}
			}
            
    		if (availableSpaceCount == 1) { //last move!
				if (tokenChoice == -1) {
					tokenChoice = mGameView.selectLastComputerToken();
				}
    			tokenSelected = tokenChoice;
    			boardSpaceSelected = trialBoardSpaceSelected1;
    		}
        	
        	if (tokenChoice > -1) {
        		if (tokenChoice == GameView.BoardSpaceValues.CIRCLECROSS) {
        			tokenChoice = mPlayer2TokenChoice;
        		}
                for (int x = 0; x < availableValues.length; x++) {
                	if (availableValues[x]) {
                    	int[] testBoard = new int[GameView.BoardSpaceValues.BOARDSIZE];
                    	//copy normalizedBoard to testBoard
                        for (int y = 0; y < testBoard.length; y++) {
                        	testBoard[y] = normalizedBoardPlayer2[y];
                        }
                		testBoard[x] = mPlayer2TokenChoice;
                		
                		int[] winnerFound = checkWinningPosition(testBoard);
                		if (winnerFound[0] > -1 || winnerFound[1] > -1 || winnerFound[2] > -1) {
                			tokenSelected = tokenChoice;
                			boardSpaceSelected = x;
                			break;
                		}
                	}
                	// if we reach here then the computer cannot win on this move
                }
        	}
        	// try to block the human win on the next move here
/*
 * There is a possibility that the human will have more than 1 winning move. So, lets save each
 * winning outcome in a HashMap and re-test them with successive available moves until we find one
 * that results in no winning next available move for human.         	
 */

        	
        	if (tokenSelected == -1) { //try again with human selected token
            	tokenChoice = mGameView.selectSpecificComputerToken(mPlayer2TokenChoice, false);
            	if (tokenChoice > -1) {
//            		int computerBlockingMove = -1;
            		for (int x = 0; x < availableValues.length; x++) {
            			if (availableValues[x]) {
            				int[] testBoard = new int[GameView.BoardSpaceValues.BOARDSIZE];
            				//copy normalizedBoard to testBoard
            				for (int y = 0; y < testBoard.length; y++) {
            					testBoard[y] = normalizedBoardPlayer1[y];
            				}
            				testBoard[x] = mPlayer1TokenChoice; 
// since there can be multiple winning moves available for the human 
// move computer token to boardSpaceSelected
// reset available and re-test for winner using mPlayer1TokenChoice
// if winner not found then set tokenSelected to tokenChoice and set boardSpaceSelected to x                				
            				int[] winnerFound = checkWinningPosition(testBoard);
            				if (winnerFound[0] > -1 || winnerFound[1] > -1 || winnerFound[2] > -1) {
            					saveHumanWinner(x, -1);
            				}
            			}
            		}
//               		System.out.println("human winner list size: "+humanWinningHashMap.size());
               		
               		if (humanWinningHashMap.size() == 1) {
//               			Integer[] onlyWinningPosition = (Integer[])humanWinningHashMap.keySet().toArray();
               			Object[] onlyWinningPosition = humanWinningHashMap.keySet().toArray();
               			
               			int testMove = (Integer)onlyWinningPosition[0];
               			tokenSelected = tokenChoice;
               			boardSpaceSelected = testMove;
               		} else if (humanWinningHashMap.size() > 1){
               			Iterator<Integer> it = humanWinningHashMap.keySet().iterator();
               			while(it.hasNext()) {
               				int winningPosition  = (Integer)it.next();
//               				System.out.println("winning position: "+winningPosition);
               				int[] testBoard = new int[GameView.BoardSpaceValues.BOARDSIZE];

               				for (int y = 0; y < testBoard.length; y++) {
               					testBoard[y] = normalizedBoardPlayer1[y];
               				}
               				testBoard[winningPosition] = mPlayer2TokenChoice;
               				mGameView.setAvailableMoves(winningPosition, testBoard, testAvailableValues);

               				Iterator<Integer> it2 = humanWinningHashMap.keySet().iterator();
               				while(it2.hasNext()) {
               					int testMove = (Integer)it2.next();
               					if (winningPosition == testMove) {
               						continue; // no point in testing against same value
               					}
               					int spaceOkToUse = (Integer)humanWinningHashMap.get(testMove);
//                   				System.out.println("testing "+testMove+ " against winning position: "+ winningPosition);
               					// testMove = a winning move human
               					if (testAvailableValues[testMove]) {
//               						computerBlockingMove = winningPosition;
//               						break;
          							saveHumanWinner(testMove, 0); // space cannot be used
//                					System.out.println("reset value at "+testMove+ " to unavailable(false) for "+ winningPosition);
               					} else {
               						if (spaceOkToUse != 0)
               							saveHumanWinner(testMove, 1); //space is ok to use
//                					System.out.println("reset value at "+testMove+ " to ok to use for "+ winningPosition);
               					}
               				}
               			}
               			Iterator<Integer> it3 = humanWinningHashMap.keySet().iterator();
               			while(it3.hasNext()) {   
               				int computerBlockingMove = (Integer)it3.next();
               				int spaceAvailable = (Integer)humanWinningHashMap.get(computerBlockingMove);
               				if (spaceAvailable == 1) {
               					boardSpaceSelected = computerBlockingMove;
               					tokenSelected = tokenChoice;
//                   				System.out.println("found good move for computer at "+boardSpaceSelected);
               				}
               			}
               		}
           		}
        	}
        	// if we reach here then the computer cannot win on this move and the human
        	// cannot win on the next 
        	// so we'll select a position that at least doesn't give the human a win and move there
        	if (tokenSelected == -1) {
//            	tokenChoice = mGameView.selectSpecificComputerToken(mPlayer2TokenChoice, false);
//            	if (tokenChoice == -1)
                	tokenChoice = mGameView.selectSpecificComputerToken(mPlayer1TokenChoice, false);
                	
            	if (tokenChoice > -1) {
            		for (int x = 0; x < availableValues.length; x++) {
            			if (availableValues[x]) {
            				int[] testBoard = new int[GameView.BoardSpaceValues.BOARDSIZE];
            				//copy normalizedBoard to testBoard
            				for (int y = 0; y < testBoard.length; y++) {
            					testBoard[y] = normalizedBoardPlayer1[y];
            				}
            				testBoard[x] = mPlayer1TokenChoice; 
            				int[] winnerFound = checkWinningPosition(testBoard);
            				//test to see if human can't win if he were to move here
            				// if human cannot win then this is a candidate move for computer
            				if (winnerFound[0] == -1 && winnerFound[1] == -1 && winnerFound[2] == -1) {
            					int boardSpaceSelected2 = x;
//take it one step further and see if moving to this position gives the human a win in the next move,
// if it does then try next available board position
            					int humanToken =  mGameView.selectSpecificHumanToken(mPlayer1TokenChoice);
            					int[] testBoard2 = new int[GameView.BoardSpaceValues.BOARDSIZE];
            					if (humanToken > -1) {
            						boolean computerCanUseMove = true;
            						for (int z = 0; z < availableValues.length; z++) {
            							//copy the board with trial move from above to another test board
                    					for (int y = 0; y < testBoard.length; y++) {
                        					testBoard2[y] = testBoard[y];
                        				}
                    					//set available moves for new test board
                    					mGameView.setAvailableMoves(boardSpaceSelected2, testBoard2, testAvailableValues);
            							if (testAvailableValues[z] == true && z != boardSpaceSelected2) {
            								testBoard2[z] = mPlayer1TokenChoice; 
            								int[] winnerFound2 = checkWinningPosition(testBoard2);
            								if (winnerFound2[0] > -1 || winnerFound2[1] > -1 || winnerFound2[2] > -1) {
            									computerCanUseMove = false;
            									break;
            								}
            							}
            						}
//            	               		System.out.println("test case 2 selection made, computerCanUseMove = "+computerCanUseMove+" boardSpaceSelected: "+x);
            						
            						if (computerCanUseMove) {
    	            					tokenSelected = tokenChoice;
    									boardSpaceSelected = x;
    									break;
            						}
            					}
            				}
            			}
            		} 
            	}
        	}
        	if (tokenSelected == -1) {
        		int humanTokenChoice = mGameView.selectSpecificHumanToken(mPlayer1TokenChoice);
        		tokenChoice = mGameView.selectSpecificComputerToken(mPlayer2TokenChoice, false);
        		
        		if (availableSpaceCount == 2) { //we're down to our last 2 possible moves
        			//if we get here we're on the last move and we know we can't win with it.
        			//so let's see if the human could make the computer win 
//        			System.out.println("testing with 2 possibilities available");
        			int[] testBoard = new int[GameView.BoardSpaceValues.BOARDSIZE];
        			//copy normalizedBoard to testBoard
        			for (int y = 0; y < testBoard.length; y++) {
        				testBoard[y] = normalizedBoardPlayer1[y];
        			}
        			if (humanTokenChoice > -1) {
        				testBoard[trialBoardSpaceSelected1] = mPlayer1TokenChoice;
        				int[] winnerFound = checkWinningPosition(testBoard);
        				if (winnerFound[0] > -1 || winnerFound[1] > -1 || winnerFound[2] > -1) { 
        					boardSpaceSelected = trialBoardSpaceSelected2;
            				if (tokenChoice == -1) {
            					tokenChoice = mGameView.selectLastComputerToken();
            				}
        					tokenSelected = tokenChoice;
//        					System.out.println("human choice found, moving computer token to "+trialBoardSpaceSelected2);
        				} else {
        					tokenSelected = mGameView.selectLastComputerToken();
        					boardSpaceSelected = trialBoardSpaceSelected1;
//        					System.out.println("human choice found, no winning move found for human, moving computer token to "+trialBoardSpaceSelected2);
        				}
        			} else { 
        				if (tokenChoice == -1)
        					tokenChoice = mGameView.selectLastComputerToken();
    					tokenSelected = tokenChoice;
        				testBoard[trialBoardSpaceSelected2] = mPlayer1TokenChoice;
        				int[] winnerFound = checkWinningPosition(testBoard);
        				if (winnerFound[0] > -1 || winnerFound[1] > -1 || winnerFound[2] > -1) {
        					boardSpaceSelected = trialBoardSpaceSelected1;
//        					System.out.println("winning move found for human, moving computer token to "+trialBoardSpaceSelected1);
        				} else {
        					boardSpaceSelected = trialBoardSpaceSelected2;
//        					System.out.println("moving computer token to "+trialBoardSpaceSelected2);
       					}
        			}
        		}
    			
    			if (availableSpaceCount >= 3) {
    			// 3 or more spaces still open on board
//    				System.out.println("3 or more spaces open");
//    				tokenChoice = mGameView.selectLastComputerToken();
//    				tokenChoice = mGameView.selectSpecificComputerToken(mPlayer2TokenChoice, false);
    				tokenChoice = mGameView.selectSpecificHumanToken(mPlayer1TokenChoice);
    				if (tokenChoice > -1) {
    					int[] testBoard = new int[GameView.BoardSpaceValues.BOARDSIZE];
    					for (int x = 0; x < availableValues.length; x++) {
    						for (int y = 0; y < testBoard.length; y++) {
    							testBoard[y] = normalizedBoardPlayer1[y];
    						}
    						if (availableValues[x]) {
    							testBoard[x] = mPlayer1TokenChoice;
    							int[] winnerFound = checkWinningPosition(testBoard);
    							if (winnerFound[0] > -1 || winnerFound[1] > -1 || winnerFound[2] > -1) {
    								tokenChoice = mGameView.selectSpecificComputerToken(mPlayer2TokenChoice, false);
    								if (tokenChoice > -1) {
    									tokenSelected = tokenChoice;
    								} else {
    									tokenSelected = mGameView.selectLastComputerToken(); //no choice here
    								}
    								boardSpaceSelected = x;
    								break;
    							}
            				}
    					}
    					//if we get here then there is no winning move available for human player
    					// for us to block so we'll just select the next available position and move there
    					
    					if (tokenSelected == -1) {
							tokenChoice = mGameView.selectSpecificComputerToken(mPlayer2TokenChoice, false);
//							System.out.println("3 or more spaces open and still no choice made yet");
    					}
						if (tokenChoice > -1) {
							tokenSelected = tokenChoice;
						} else {
							tokenSelected = mGameView.selectLastComputerToken(); //no choice here
						}
    					for (int x = 0; x < availableValues.length; x++) {
    						if (availableValues[x]) {
    							boardSpaceSelected = x;
    							break;
    						}
    					}
    				} else {
    					tokenChoice = mGameView.selectSpecificComputerToken(mPlayer2TokenChoice, false);
    					if (tokenChoice == -1) {
    						tokenChoice = mGameView.selectLastComputerToken(); //no choice here
    					}
    					int[] testBoard = new int[GameView.BoardSpaceValues.BOARDSIZE];
//    					System.out.println("3 or more spaces open last attempt made");
    					for (int x = 0; x < availableValues.length; x++) {
    						for (int y = 0; y < testBoard.length; y++) {
    							testBoard[y] = normalizedBoardPlayer2[y];
    						}
    						if (availableValues[x]) {
    							testBoard[x] = mPlayer1TokenChoice;
    							int[] winnerFound = checkWinningPosition(testBoard);
    							if (winnerFound[0] == -1 && winnerFound[1] == -1 && winnerFound[2] == -1) {
   									tokenSelected = tokenChoice;
    								boardSpaceSelected = x;
    								break;
    							}
            				}
    					}
    					if (tokenSelected == -1) {
    						tokenSelected = tokenChoice;
    						for (int x = 0; x < availableValues.length; x++) {
    							if (availableValues[x]) {
    								boardSpaceSelected = x;
    								break;
    							}
    						}
    					}
    				}
    			}
        	}
        }
        	
        mGameView.disableBall(tokenSelected);
    	selectionArray[0] = boardSpaceSelected; //selectionArray[0] = boardPosition 
    	selectionArray[1] = tokenSelected; //selectionArray[1] = computerToken
    	return selectionArray;
    }
    
    private void setNetworkMove(int boardPosition, int tokenMoved) {
		int resultValue = -1;
		if (tokenMoved > 3) {
			resultValue = tokenMoved - 4;
		} else {
			resultValue = tokenMoved + 4;
		}
        mGameView.moveComputerToken(boardPosition, resultValue); //move token selected to location on board
        
        mLastCellSelected = boardPosition;
        playFinishMoveSound();
        mGameView.disableBall(resultValue);
        mGameView.setCell(boardPosition, GameView.State.PLAYER2); //set State table
    }
    
    private int setComputerMove() {
    	int computerToken = GameView.BoardSpaceValues.EMPTY;
    	int index[] = selectBestMove(); //0 = boardSpaceSelected, 1 = tokenSelected
        
    	if (index[0] != -1) {
    		playComputerMoveSound();
            mGameView.setCell(index[0], GameView.State.PLAYER2); // set State table - the computer (Willy) is always PLAYER2
            computerToken = mGameView.moveComputerToken(index[0],index[1]); //move computer token to location on board
    	}
    	return computerToken;
    }
    
    private void networkCallBackFinish() {
    	finishTurn(false, false, true); //don't send message to make computer move don't switch the player don't use player 2 for win testing 
    	
        String testText = mButtonNext.getText().toString();
        if (testText.endsWith("Play Again?")) {
        	highlightCurrentPlayer(GameView.State.EMPTY);
        	return;
        }
    	
    	GameView.State currentPlayer = mGameView.getCurrentPlayer();
    	highlightCurrentPlayer(currentPlayer);
    	mGameView.setViewDisabled(false);
    }
    
    private class MyHandlerCallback implements Callback {
        public boolean handleMessage(Message msg) {
        	
        	if (msg.what == DISMISS_WAIT_FOR_NEW_GAME_FROM_CLIENT) {
        		if (mHostWaitDialog != null) {
        			mHostWaitDialog.dismiss(); 
        			mHostWaitDialog = null;
        		}
                return true;
        	}
        	
        	if (msg.what == DISMISS_WAIT_FOR_NEW_GAME_FROM_HOST) {
        		
        		String urlData = "/gamePlayer/update/?id=" + mPlayer1Id + "&playingNow=true&opponentId="
    				+ mPlayer2Id + "&userName=";
        		new SendMessageToWillyShmoServer().execute(urlData, mPlayer1Name, GameActivity.this, resources, Boolean.FALSE);
        		
                mClientWaitDialog.dismiss();
                mClientWaitDialog = null;
                return true;
        	}
        	
        	if (msg.what == ACCEPT_INCOMING_GAME_REQUEST_FROM_CLIENT) { 
        		if (mServerHasOpponent != null) {
        			if ("true".equals(mServerHasOpponent)) {
        				setGameRequestFromClient(true);
        			} else {
        				setGameRequestFromClient(false);
        			}
        		} else {
        			acceptIncomingGameRequestFromClient(); 
        		}
        		return true;
        	}
        	
        	if (msg.what == MSG_HOST_UNAVAILABLE) {
        		displayHostNotAvailableAlert();
        	}
        	
        	if (msg.what == MSG_NETWORK_SERVER_REFUSED_GAME) {
       			displayServerRefusedGameAlert(mNetworkOpponentPlayerName);
        	}

        	if (msg.what == MSG_NETWORK_SERVER_LEFT_GAME) {
       			displayOpponentLeftGameAlert(mPlayer2Name);
       			mPlayer2Name = null;
        	}
        	
//        	if (msg.what == ACCEPT_INCOMING_GAME_REQUEST_FROM_CLIENT) {
        	if (msg.what == NEW_GAME_FROM_CLIENT) {
        		mGameView.initalizeGameValues();
            	mPlayer2NameTextValue.setText(mPlayer2Name); 
                mButtonNext.setText(mButtonStartText);
                mButtonNext.setEnabled(false);
        		showPlayerTokenChoice();
        		
        		for (int x = 0; x < mTokensFromClient.size(); x++) {
        			int [] tokenArray = (int[])mTokensFromClient.get(x);
        			if (x < 8) {
        				mGameView.updatePlayerToken(tokenArray[0], tokenArray[1]);
        			} else {
        				mGameView.setBoardSpaceValueCenter(tokenArray[1]);
        			}
        		}
        		
        		boolean moveFirst = mRandom.nextBoolean();
        		if (moveFirst) {
            		mGameView.setCurrentPlayer(GameView.State.PLAYER1);
            		highlightCurrentPlayer(GameView.State.PLAYER1);
            		mGameView.setViewDisabled(false);
        		} else {
            		mGameView.setCurrentPlayer(GameView.State.PLAYER1); //this value will be switched in onClick method
            		highlightCurrentPlayer(GameView.State.PLAYER2);
            		mGameView.setViewDisabled(true);
            		if (mServerThread != null)
            			mServerThread.setMessageToClient("moveFirst");
        		}
        		
        		mGameView.invalidate();
        		return true; 
        	}
        	
            if (msg.what == MSG_NETWORK_CLIENT_TURN) {
            	if (mClientThread != null) {
            		int boardPosition = mClientThread.getBoardPosition();
            		int tokenMoved = mClientThread.getTokenMoved();
            		setNetworkMove(boardPosition, tokenMoved);
            		networkCallBackFinish();
            		mGameView.invalidate();
            	}
            	return true;
            }
            
            if (msg.what == MSG_NETWORK_SERVER_TURN) {
            	if (mServerThread != null) { 
            		int boardPosition = mServerThread.getBoardPosition();
            		int tokenMoved = mServerThread.getTokenMoved();
            		setNetworkMove(boardPosition, tokenMoved);
            		networkCallBackFinish();
            		String testText = mButtonNext.getText().toString();
            		if (testText.endsWith("Play Again?")) {
                    	if (imServing) { // if win came from client side we need to send back a message to give client the 
                    		mServerThread.setMessageToClient("game over"); // ability to respond
                    	}
                    }
                }
            	return true;
            }      
            
            if (msg.what == MSG_NETWORK_SET_TOKEN_CHOICE) {
            	showPlayerTokenChoice();
            }
        	
            if (msg.what == MSG_NETWORK_CLIENT_MAKE_FIRST_MOVE) {
            	mGameView.setCurrentPlayer(GameView.State.PLAYER1);
//            	State currentPlayer = mGameView.getCurrentPlayer();
            	highlightCurrentPlayer(GameView.State.PLAYER1);
            	mGameView.setViewDisabled(false);
            }
            
            if (msg.what == MSG_COMPUTER_TURN) {
//            	int computerToken = BoardSpaceValues.EMPTY;
// consider setting a difficulty level            	
            	
/* 
 * trivial cases:
 * 		if only 1 token on board then just put token anywhere  but don't select xo token    
 * 		if only 1 space is available then just put last card there
 * 
 * test cases using mPlayer2TokenChoice:
 * look for available computer token that matches mPlayer2TokenChoice
 * if found test for win for computer player using mPlayer2TokenChoice value
 * testing each available position
 * 		if (testForWin == true) we are done  
 * test for human player win with opposing token
 * 		if found move token there 
 * 
 * test cases using mPlayer2TokenChoice:
 * 		loop thru available board positions
 * 		put token anywhere where result doesn't cause human player to win
 *
 * else choose random position and place random token there
 * 
 * test for win possibility changing xo card on board to computer token
 * test for block possibility changing xo card on board to player 1 token
 * else just put down token randomly for now
 *        	
 */
            	
//            	int index = mGameView.getFirstAvailableSpace();
//            	int index[] = selectBestMove(); //0 = boardSpaceSelected, 1 = tokenSelected
//            
//            
//            	if (index[0] != -1) {
//                    mSoundComputerMove.start();
//                    mGameView.setCell(index[0], mGameView.getCurrentPlayer()); //set State table
//                    computerToken = mGameView.moveComputerToken(index[0],index[1]); //move computer token to location on board
//                    mGameView.setBoardSpaceValue(index[0], computerToken);//set internal Board correspondingly
//            	}
            	
            	int computerToken = setComputerMove();

// for now, the computer will never select the xo token for its opening move but we may change this in 
// the future. As of 07/10/2010, the computer will select the xo token only on a winning move or for the last
// move possible.
            	if (mPlayer2TokenChoice == GameView.BoardSpaceValues.EMPTY) {
            		if (computerToken != GameView.BoardSpaceValues.EMPTY)
            			mPlayer2TokenChoice = computerToken;
            		if (mPlayer2TokenChoice == GameView.BoardSpaceValues.CIRCLECROSS ||
            			mPlayer2TokenChoice == GameView.BoardSpaceValues.CROSS) { //computer will always choose X if it selects the XO card
            			mPlayer1TokenChoice = GameView.BoardSpaceValues.CIRCLE; // on its first move, we may want to change this behavior
            			// see comments above
            		} else {
            			mPlayer1TokenChoice = GameView.BoardSpaceValues.CROSS;
            		}
                	mGameView.setPlayer1TokenChoice(mPlayer1TokenChoice);
                	mGameView.setPlayer2TokenChoice(mPlayer2TokenChoice);
                	
                	showPlayerTokenChoice();
                	
            	}
           		finishTurn(false, true, false); //don't send message to make computer move but do switch the player and don't use player 2 for win testing
                return true;
            }
            return false;
        }
    }

    private GameView.State getOtherPlayer(GameView.State player) {
        return player == GameView.State.PLAYER1 ? GameView.State.PLAYER2 : GameView.State.PLAYER1;
    }
    
    //FIXME - consider highlighting the border of the enclosing rectangle around the player's name instead
    protected void highlightCurrentPlayer(GameView.State player) {
    	
//    	System.out.println("entering highlightCurrentPlayer");

		Animation anim = new AlphaAnimation(0.0f, 1.0f);
		anim.setDuration(500); //You can manage the time of the blink with this parameter
		anim.setStartOffset(20);
		anim.setRepeatMode(Animation.REVERSE);
		anim.setRepeatCount(Animation.INFINITE);
		
		Animation anim2 = new AlphaAnimation(0.0f, 1.0f);
		anim2.setDuration(500); //You can manage the time of the blink with this parameter
		anim2.setStartOffset(20);
		anim2.setRepeatMode(Animation.REVERSE);
		anim2.setRepeatCount(0);
    	
		if (player == GameView.State.PLAYER1) {
//			if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
				mPlayer1NameTextValue.setBackgroundDrawable(getResources().getDrawable(R.drawable.backwithgreenborder));
				mPlayer2NameTextValue.setBackgroundDrawable(getResources().getDrawable(R.drawable.backwithwhiteborder));
				mPlayer1NameTextValue.startAnimation(anim);
				mPlayer2NameTextValue.startAnimation(anim2);
//			} else {
//				mPlayer1NameTextValue.setBackground(getResources().getDrawable(R.drawable.backwithgreenborder));
//				mPlayer2NameTextValue.setBackground(getResources().getDrawable(R.drawable.backwithwhiteborder));
//			}
		} else if (player == GameView.State.PLAYER2) {
//			if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
				mPlayer2NameTextValue.setBackgroundDrawable(getResources().getDrawable(R.drawable.backwithgreenborder));
				mPlayer2NameTextValue.startAnimation(anim);
				mPlayer1NameTextValue.setBackgroundDrawable(getResources().getDrawable(R.drawable.backwithwhiteborder));
				mPlayer1NameTextValue.startAnimation(anim2);
//			} else {
//				mPlayer2NameTextValue.setBackground(getResources().getDrawable(R.drawable.backwithgreenborder));
//				mPlayer1NameTextValue.setBackground(getResources().getDrawable(R.drawable.backwithwhiteborder));
//			}
		} else {
//			if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
				mPlayer1NameTextValue.setBackgroundDrawable(getResources().getDrawable(R.drawable.backwithwhiteborder));
				mPlayer2NameTextValue.setBackgroundDrawable(getResources().getDrawable(R.drawable.backwithwhiteborder));
				mPlayer1NameTextValue.startAnimation(anim2);
				mPlayer2NameTextValue.startAnimation(anim2);
//			} else {
//				mPlayer1NameTextValue.setBackground(getResources().getDrawable(R.drawable.backwithwhiteborder));
//				mPlayer2NameTextValue.setBackground(getResources().getDrawable(R.drawable.backwithwhiteborder));
//			}
		}
    }

    private void finishTurn(boolean makeComputerMove, boolean switchPlayer, boolean usePlayer2) {
        GameView.State player = mGameView.getCurrentPlayer();
        if (usePlayer2) { // if we're playing over a network then current player is always player 1 
        	player = GameView.State.PLAYER2; // so we need to add some extra logic to test winner on player 2 over the network
        }
        mGameView.disableBall();
        if (!checkGameFinished(player, usePlayer2)) {
        	if (switchPlayer) {
        		player = selectTurn(getOtherPlayer(player));
        		if (player == GameView.State.PLAYER2 && makeComputerMove && !(HUMAN_VS_HUMAN | HUMAN_VS_NETWORK)) {
        			mHandler.sendEmptyMessageDelayed(MSG_COMPUTER_TURN, COMPUTER_DELAY_MS);
        		}
            	highlightCurrentPlayer(player);
        	}
        }
    }

// Given the existence of the xo token, there is a possibility that both players could be winners.
// in this case we will give precedence to the token type of the player that made the winning move.    
    private boolean checkGameFinished(GameView.State player, boolean usePlayer2) {
        int[] boardSpaceValues = mGameView.getBoardSpaceValues();
        
        int[] data = new int[GameView.BoardSpaceValues.BOARDSIZE];
        
        int wildCardValue = mPlayer1TokenChoice;
        if (player == GameView.State.PLAYER2) {
        	wildCardValue = mPlayer2TokenChoice;
        }
        for (int x = 0; x < data.length; x++) {
    		data[x] = boardSpaceValues[x];
        	if (data[x] == GameView.BoardSpaceValues.CIRCLECROSS) {
        		data[x] = wildCardValue; //BoardSpaceValues.CROSS;
        	}
        }
        if (testForWinner(data, usePlayer2)) {
        	return true;
        }
        
        wildCardValue = wildCardValue == mPlayer1TokenChoice ? mPlayer2TokenChoice : mPlayer1TokenChoice;
        data = new int[GameView.BoardSpaceValues.BOARDSIZE];
        for (int x = 0; x < data.length; x++) {
    		data[x] = boardSpaceValues[x];
        	if (data[x] == GameView.BoardSpaceValues.CIRCLECROSS) {
        		data[x] = wildCardValue; //BoardSpaceValues.CIRCLE;
        	}
        }
        if (testForWinner(data, usePlayer2)) {
        	return true;
        }
        
        if (mGameView.testBoardFull(9)) {
            setFinished(GameView.State.EMPTY, -1, -1, -1);
            if (HUMAN_VS_NETWORK) {
            	updateWebServerScore();
            }
            return true;
        }
        return false;
    }
    
    private int[] checkWinningPosition(int[] data) {
        int col = -1;
        int row = -1;
        int diag = -1;
        int winningToken = -1;
        int winningPosition1 = -1;
        int winningPosition2 = -1;
        int winningPosition3 = -1;
        
        // check rows
        for (int j = 0, k = 0; j < 5; j++, k += 5) {
            if (data[k] != GameView.BoardSpaceValues.EMPTY && data[k] == data[k+1] && data[k] == data[k+2]) {
            	winningToken = data[k];
                row = j;
                winningPosition1 = k;
                winningPosition2 = k+1;
                winningPosition3 = k+2;
                break;
            }
            if (data[k+1] != GameView.BoardSpaceValues.EMPTY && data[k+1] == data[k+2] && data[k+2] == data[k+3]) {
            	winningToken = data[k+1];
                row = j;
                winningPosition1 = k+1;
                winningPosition2 = k+2;
                winningPosition3 = k+3;
                break;
            }
            if (data[k+2] != GameView.BoardSpaceValues.EMPTY && data[k+2] == data[k+3] && data[k+2] == data[k+4]) {
            	winningToken = data[k+2];
                row = j;
                winningPosition1 = k+2;
                winningPosition2 = k+3;
                winningPosition3 = k+4;
                break;
            }
        }

        // check columns
        if (row == -1) {
        	for (int i = 0; i < 5; i++) {
        		if (data[i] != GameView.BoardSpaceValues.EMPTY && data[i] == data[i+5] && data[i] == data[i+10]) {
        			winningToken = data[i];
        			col = i;
                    winningPosition1 = i;
                    winningPosition2 = i+5;
                    winningPosition3 = i+10;
        			break;
        		}
        		if (data[i+5] != GameView.BoardSpaceValues.EMPTY && data[i+5] == data[i+10] && data[i+5] == data[i+15]) {
        			winningToken = data[i+5];
                    winningPosition1 = i+5;
                    winningPosition2 = i+10;
                    winningPosition3 = i+15;
        			col = i;
        			break;
        		}
        		if (data[i+10] != GameView.BoardSpaceValues.EMPTY && data[i+10] == data[i+15] && data[i+10] == data[i+20]) {
        			winningToken = data[i+10];
        			col = i;
                    winningPosition1 = i+10;
                    winningPosition2 = i+15;
                    winningPosition3 = i+20;
        			break;
        		}
        	}
        }

        // check diagonals
        //upper left to lower right diagonals:    
        if (row == -1 && col == -1) {
        	if (data[0] != GameView.BoardSpaceValues.EMPTY && data[0] == data[6] && data[0] == data[12]) {
        		winningToken = data[0];
        		diag = 0;
                winningPosition1 = 0;
                winningPosition2 = 6;
                winningPosition3 = 12;
        	} else  if (data[1] != GameView.BoardSpaceValues.EMPTY && data[1] == data[7] && data[1] == data[13]) {
        		winningToken = data[1];
        		diag = 2;
                winningPosition1 = 1;
                winningPosition2 = 7;
                winningPosition3 = 13;
        	} else  if (data[2] != GameView.BoardSpaceValues.EMPTY && data[2] == data[8] && data[2] == data[14]) {
        		winningToken = data[2];
        		diag = 3;
                winningPosition1 = 2;
                winningPosition2 = 8;
                winningPosition3 = 14;
        	} else  if (data[5] != GameView.BoardSpaceValues.EMPTY && data[5] == data[11] && data[5] == data[17]) {
        		winningToken = data[5];
        		diag = 4;
                winningPosition1 = 5;
                winningPosition2 = 11;
                winningPosition3 = 17;
        	} else  if (data[6] != GameView.BoardSpaceValues.EMPTY && data[6] == data[12] && data[6] == data[18]) {
        		winningToken = data[6];
        		diag = 0;
                winningPosition1 = 6;
                winningPosition2 = 12;
                winningPosition3 = 18;
        	} else  if (data[7] != GameView.BoardSpaceValues.EMPTY && data[7] == data[13] && data[7] == data[19]) {
        		winningToken = data[7];
        		diag = 2;
                winningPosition1 = 7;
                winningPosition2 = 13;
                winningPosition3 = 19;
        	} else  if (data[10] != GameView.BoardSpaceValues.EMPTY && data[10] == data[16] && data[10] == data[22]) {
        		winningToken = data[10];
        		diag = 5;
                winningPosition1 = 10;
                winningPosition2 = 16;
                winningPosition3 = 22;
        	} else  if (data[11] != GameView.BoardSpaceValues.EMPTY && data[11] == data[17] && data[11] == data[23]) {
        		winningToken = data[11];
        		diag = 4;
                winningPosition1 = 11;
                winningPosition2 = 17;
                winningPosition3 = 23;
        	} else  if (data[12] != GameView.BoardSpaceValues.EMPTY && data[12] == data[18] && data[12] == data[24]) {
        		winningToken = data[12];
        		diag = 0;
                winningPosition1 = 12;
                winningPosition2 = 18;
                winningPosition3 = 24;

        	//check diagonals running from lower left to upper right
        	} else  if (data[2] != GameView.BoardSpaceValues.EMPTY && data[2] == data[6] && data[2] == data[10]) {
        		winningToken = data[2];
        		diag = 1;
                winningPosition1 = 2;
                winningPosition2 = 6;
                winningPosition3 = 10;
        	} else  if (data[3] != GameView.BoardSpaceValues.EMPTY && data[3] == data[7] && data[3] == data[11]) {
        		winningToken = data[3];
        		diag = 6;
                winningPosition1 = 3;
                winningPosition2 = 7;
                winningPosition3 = 11;
        	} else  if (data[4] != GameView.BoardSpaceValues.EMPTY && data[4] == data[8] && data[4] == data[12]) {
        		winningToken = data[4];
        		diag = 7;
                winningPosition1 = 4;
                winningPosition2 = 8;
                winningPosition3 = 12;
        	} else  if (data[9] != GameView.BoardSpaceValues.EMPTY && data[9] == data[13] && data[9] == data[17]) {
        		winningToken = data[9];
        		diag = 8;
                winningPosition1 = 9;
                winningPosition2 = 13;
                winningPosition3 = 17;
        	} else  if (data[14] != GameView.BoardSpaceValues.EMPTY && data[14] == data[18] && data[14] == data[22]) {
        		winningToken = data[14];
        		diag = 9;
                winningPosition1 = 14;
                winningPosition2 = 18;
                winningPosition3 = 22;
        	} else  if (data[7] != GameView.BoardSpaceValues.EMPTY && data[7] == data[11] && data[7] == data[15]) {
        		winningToken = data[7];
        		diag = 6;
                winningPosition1 = 7;
                winningPosition2 = 11;
                winningPosition3 = 15;
        	} else  if (data[8] != GameView.BoardSpaceValues.EMPTY && data[8] == data[12] && data[8] == data[16]) {
        		winningToken = data[8];
        		diag = 7;
                winningPosition1 = 8;
                winningPosition2 = 12;
                winningPosition3 = 16;
        	} else  if (data[12] != GameView.BoardSpaceValues.EMPTY && data[12] == data[16] && data[12] == data[20]) {
        		winningToken = data[12];
        		diag = 7;
                winningPosition1 = 12;
                winningPosition2 = 16;
                winningPosition3 = 20;
        	} else  if (data[13] != GameView.BoardSpaceValues.EMPTY && data[13] == data[17] && data[13] == data[21]) {
        		winningToken = data[13];
        		diag = 8;
                winningPosition1 = 13;
                winningPosition2 = 17;
                winningPosition3 = 21;
        	}     
        }

        int[] returnValue = new int[7];
        returnValue[0] = col;
        returnValue[1] = row;
        returnValue[2] = diag;
        returnValue[3] = winningToken;
        returnValue[4] = winningPosition1;
        returnValue[5] = winningPosition2;
        returnValue[6] = winningPosition3;
        
        return returnValue; 
    }
        
    private boolean testForWinner(int[] data, boolean usePlayer2) {
    	int[] winnerFound = checkWinningPosition(data);
        
// For scoring purposes we will need to determine if the current player is the winner when the last card 
// was placed or if the opposing player is the winner.
// if the opposing player wins then more points are awarded to the opponent          
        GameView.State player = null;
        GameView.State currentPlayer = mGameView.getCurrentPlayer();
        if (usePlayer2) // if we're playing over a network then current player is always player 1 
        	currentPlayer = GameView.State.PLAYER2; // so we need to add some extra logic to test winner on player 2 over the network
        if (winnerFound[3] > -1) {
        	if (winnerFound[3] == mPlayer1TokenChoice) {
        		player = GameView.State.PLAYER1;
        		int player1Score = mPlayer1Score;
        		if (HUMAN_VS_NETWORK) {
        			player1Score = mPlayer1NetworkScore;
        		}
        		if (currentPlayer == GameView.State.PLAYER1) {
        			player1Score += mRegularWin;
        			playHumanWinSound();
        			checkForPrizeWin(winnerFound[4], winnerFound[5], winnerFound[6], PrizeValue.REGULARPRIZE);
        		} else {
        			player1Score += mSuperWin;
        			playHumanWinShmoSound();
        			checkForPrizeWin(winnerFound[4], winnerFound[5], winnerFound[6], PrizeValue.SHMOPRIZE);
        		}
        		if (HUMAN_VS_NETWORK) {
        			mPlayer1NetworkScore = player1Score;
        		} else {
        			mPlayer1Score = player1Score;
        		}
        	} else {
        		player = GameView.State.PLAYER2;
        		
        		int player2Score = mPlayer2Score;
        		if (HUMAN_VS_NETWORK) {
        			player2Score = mPlayer2NetworkScore;
        		}
        		if (currentPlayer == GameView.State.PLAYER2) {
        			if (HUMAN_VS_HUMAN || HUMAN_VS_NETWORK) {
        				player2Score += mRegularWin;
            			playHumanWinSound(); 
//            			if (HUMAN_VS_NETWORK) {
//            				checkForPrizeWin(winnerFound[4], winnerFound[5], winnerFound[6], PrizeValue.REGULARPRIZE);
//            			}
        			} else {
        				mWillyScore += mRegularWin;
            			playWillyWinSound();
        			}
        		} else {
        			if (HUMAN_VS_HUMAN || HUMAN_VS_NETWORK) {
        				player2Score += mSuperWin;
            			playHumanWinShmoSound(); 
//            			if (HUMAN_VS_NETWORK) {
//            				checkForPrizeWin(winnerFound[4], winnerFound[5], winnerFound[6], PrizeValue.SHMOPRIZE); 
//            			}
        			} else { 
        				mWillyScore += mSuperWin;
        				playWillyWinShmoSound();
        			}
        		}
        		if (HUMAN_VS_HUMAN) {
        			mPlayer2Score = player2Score;
        		}
        		if (HUMAN_VS_NETWORK) {
        			mPlayer2NetworkScore = player2Score;
        		}
        	}
        }
        
        if (winnerFound[0] != -1 || winnerFound[1] != -1 || winnerFound[2] != -1) {
        	setFinished(player, winnerFound[0], winnerFound[1], winnerFound[2]);
            return true;
        }
        return false;
    }
    
    
    private void checkForPrizeWin(int winningPosition1, int winningPosition2, int winningPosition3, int winType) {
    	if (mLastCellSelected == GameView.getPrizeLocation()) {
    		if (winType == PrizeValue.SHMOPRIZE) {
    			showPrizeWon(PrizeValue.SHMOGRANDPRIZE); 
    		} else {
    			showPrizeWon(PrizeValue.GRANDPRIZE); 
    		}
    	} else if (GameView.getPrizeLocation() == winningPosition1 || GameView.getPrizeLocation() == winningPosition2 ||
    		GameView.getPrizeLocation() == winningPosition3) {
    		if (winType == PrizeValue.SHMOPRIZE) {
    			showPrizeWon(PrizeValue.SHMOPRIZE); 
    		} else {
        		showPrizeWon(PrizeValue.REGULARPRIZE); 
    		}
    	}
    }

    private void setFinished(GameView.State player, int col, int row, int diagonal) {
//TODO - not sure how to handle this yet?    	
        mGameView.setCurrentPlayer(GameView.State.WIN);
        mGameView.setWinner(player);
        mGameView.setEnabled(false);
        mGameView.setFinished(col, row, diagonal);
        setWinState(player);
        if (player == GameView.State.PLAYER2) {
        	mGameView.invalidate();
        }
        displayScores();
    }
    
    private void setWinState(GameView.State player) {
        mButtonNext.setEnabled(true);
        String text;
        String player1Name = "Player 1";
        String player2Name = "Player 2";
        if (mPlayer1Name != null) {
        	player1Name = mPlayer1Name;
        }
        if (mPlayer2Name != null) {
        	player2Name = mPlayer2Name;
        }
        if (player == GameView.State.EMPTY) {
            text = getString(R.string.tie);
        } else if (player == GameView.State.PLAYER1) {
//            text = getString(R.string.player1_win);
        	text = player1Name+" wins! Play Again?";
        } else {
//            text = getString(R.string.player2_win);
        	text = player2Name+" wins! Play Again?";
        }
        mButtonNext.setText(text); 
        
    	if (HUMAN_VS_NETWORK) {
    		updateWebServerScore();
    	}
        
        highlightCurrentPlayer(GameView.State.EMPTY);
        mGameView.setViewDisabled(true);
    }

//FIXME - there's got to be some way to consolidate these sound methods into a single callable method
// with a switch/case ?    
    private void playFinishMoveSound() {
    	if (!getSoundMode()) {
    		return;
    	}
    	
        MediaPlayer soundFinishMove = MediaPlayer.create(getApplicationContext(), R.raw.finish_move);
        if (soundFinishMove != null) {
        	soundFinishMove.setOnCompletionListener(new OnCompletionListener() {
        		@Override
        		public void onCompletion(MediaPlayer mp) {
        			mp.release();
        		}
        	});
        	soundFinishMove.start();
        }
        soundFinishMove = null;
    }    
    
    private void playHumanMoveSound() {
    	if (!getSoundMode()) {
    		return;
    	}
    	
        MediaPlayer soundHumanMove = MediaPlayer.create(getApplicationContext(), R.raw.human_token_move_sound);
        if (soundHumanMove != null) {
        	soundHumanMove.setOnCompletionListener(new OnCompletionListener() {
        		@Override
        		public void onCompletion(MediaPlayer mp) {
        			mp.release();
        		}
        	});
        	soundHumanMove.start();
        }
    	soundHumanMove = null;
    }    

    private void playComputerMoveSound() {
    	if (!getSoundMode()) {
    		return;
    	}
    	
        MediaPlayer soundComputerMove = MediaPlayer.create(getApplicationContext(), R.raw.computer_token_move_sound);
        if (soundComputerMove != null) {
        	soundComputerMove.setOnCompletionListener(new OnCompletionListener() {
        		@Override
        		public void onCompletion(MediaPlayer mp) {
        			mp.release();
        		}
        	});
        	soundComputerMove.start();
        }
    	soundComputerMove = null;
    }
    
    private void playHumanWinSound() {
    	if (!getSoundMode()) {
    		return;
    	}
    	
    	MediaPlayer soundHumanWin = MediaPlayer.create(getApplicationContext(), R.raw.player_win);
        if (soundHumanWin != null) {
        	soundHumanWin.setOnCompletionListener(new OnCompletionListener() {
        		@Override
        		public void onCompletion(MediaPlayer mp) {
        			mp.release();
        		}
        	});
        	soundHumanWin.start();
        }
        soundHumanWin = null;
    }
    
    private void playHumanWinShmoSound() {
    	if (!getSoundMode()) {
    		return;
    	}
    	
    	MediaPlayer soundHumanWinShmo = MediaPlayer.create(getApplicationContext(), R.raw.player_win_shmo);
        if (soundHumanWinShmo != null) {
        	soundHumanWinShmo.setOnCompletionListener(new OnCompletionListener() {
        		@Override
        		public void onCompletion(MediaPlayer mp) {
        			mp.release();
        		}
        	});
        	soundHumanWinShmo.start();
        }
        soundHumanWinShmo = null;
    }
    
    private void playWillyWinSound() {
    	if (!getSoundMode()) {
    		return;
    	}
    	
    	MediaPlayer soundWillyWin  = MediaPlayer.create(getApplicationContext(), R.raw.willy_win);
        if (soundWillyWin != null) {
        	soundWillyWin.setOnCompletionListener(new OnCompletionListener() {
        		@Override
        		public void onCompletion(MediaPlayer mp) {
        			mp.release();
        		}
        	});
        	soundWillyWin.start();
        }
        soundWillyWin = null;
    }
    
    private void playWillyWinShmoSound() {
    	if (!getSoundMode()) {
    		return;
    	}
    	
    	MediaPlayer soundWillyWinShmo  = MediaPlayer.create(getApplicationContext(), R.raw.willy_win_shmo);
    	if (soundWillyWinShmo != null) {
    		soundWillyWinShmo.setOnCompletionListener(new OnCompletionListener() {
        		@Override
        		public void onCompletion(MediaPlayer mp) {
        			mp.release();
        		}
        	});
    		soundWillyWinShmo.start();
        }
    	soundWillyWinShmo = null;
    }
    
    private String editScore(int score) {
    	DecimalFormat formatter = new DecimalFormat("0000");
    	StringBuilder formatScore = new StringBuilder(formatter.format(score));

    	for (int x = 0; x < formatScore.length(); x++) {
    		String testString = formatScore.substring(x, x+1);
    		if (testString.equals("0")) {
    			formatScore.replace(x, x+1, " ");
    		} else {
    			break;
    		}
    	}
    	return formatScore.toString();
    }
    	
	private void displayScores() {
		if (HUMAN_VS_NETWORK) { 
			mPlayer1ScoreTextValue.setText(editScore(mPlayer1NetworkScore));
			mPlayer2ScoreTextValue.setText(editScore(mPlayer2NetworkScore));
		} else if (HUMAN_VS_HUMAN) {
			mPlayer1ScoreTextValue.setText(editScore(mPlayer1Score));
			mPlayer2ScoreTextValue.setText(editScore(mPlayer2Score));
		} else {
			mPlayer1ScoreTextValue.setText(editScore(mPlayer1Score));
			mPlayer2ScoreTextValue.setText(editScore(mWillyScore));
		}
		
        mPlayer1NameTextValue.setText(mPlayer1Name);
        mPlayer2NameTextValue.setText(mPlayer2Name);     
    }
	
	
	private void showPlayerTokenChoice() {
        if (mPlayer1TokenChoice == GameView.BoardSpaceValues.CROSS) {
        	mGameTokenPlayer1.setImageResource(R.drawable.cross_small);
        } else if (mPlayer1TokenChoice == GameView.BoardSpaceValues.CIRCLE) {
        	mGameTokenPlayer1.setImageResource(R.drawable.circle_small);
        } else {
        	mGameTokenPlayer1.setImageResource(R.drawable.reset_token_selection);
        }
        
        if (mPlayer2TokenChoice == GameView.BoardSpaceValues.CROSS) {
        	mGameTokenPlayer2.setImageResource(R.drawable.cross_small); 
		} else if (mPlayer2TokenChoice == GameView.BoardSpaceValues.CIRCLE) {
        	mGameTokenPlayer2.setImageResource(R.drawable.circle_small);  
		} else {
        	mGameTokenPlayer2.setImageResource(R.drawable.reset_token_selection);
		}
	}
	
	@Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
	  // Save UI state changes to the savedInstanceState.
	  // This bundle will be passed to onCreate if the process is
	  // killed and restarted.
		super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("ga_player1_token_choice", mPlayer1TokenChoice);
        savedInstanceState.putInt("ga_player2_token_choice", mPlayer2TokenChoice);
        savedInstanceState.putInt("ga_player1_score", mPlayer1Score);
        savedInstanceState.putInt("ga_player2_score", mPlayer2Score);
        savedInstanceState.putInt("ga_willy_score", mWillyScore);
        
//        savedInstanceState.putBoolean("ga_human_vs_human", HUMAN_VS_HUMAN);        
        savedInstanceState.putString("ga_button", mButtonNext.getText().toString());
        savedInstanceState.putBoolean("ga_move_mode", mMoveModeTouch);  
        savedInstanceState.putBoolean("ga_sound_mode", mSoundMode); 
    }        
	
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// Restore UI state from the savedInstanceState.
		// This bundle has also been passed to onCreate.	
		mPlayer1TokenChoice = savedInstanceState.getInt("ga_player1_token_choice");
		mPlayer2TokenChoice = savedInstanceState.getInt("ga_player2_token_choice");
		
		mPlayer1Score = savedInstanceState.getInt("ga_player1_score");
		mPlayer2Score = savedInstanceState.getInt("ga_player2_score");
		mWillyScore = savedInstanceState.getInt("ga_willy_score");

//		HUMAN_VS_HUMAN = savedInstanceState.getBoolean("ga_human_vs_human");
		
		String workString = savedInstanceState.getString("ga_info");
		workString = savedInstanceState.getString("ga_button");
		mButtonNext.setText(workString);
		if (!mButtonNext.getText().toString().endsWith("Play Again?")) {
			 mButtonNext.setEnabled(false);
		}
		mMoveModeTouch = savedInstanceState.getBoolean("ga_move_mode");
		mSoundMode = savedInstanceState.getBoolean("ga_sound_mode"); 
	}
    
    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences settings = getSharedPreferences(UserPreferences.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(GameActivity.PLAYER1_SCORE, mPlayer1Score);
        editor.putInt(GameActivity.PLAYER2_SCORE, mPlayer2Score);
        editor.putInt(GameActivity.WILLY_SCORE, mWillyScore);
        editor.apply();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        writeToLog("ClientService", "GameActivity onDestroy processed");        
    }

	protected class ServerThread extends Thread {
    	private String mMessageToClient;
        private int boardPosition;
        private int tokenMoved;  
        private boolean mGameStarted;
        
    	public int getBoardPosition() {
    		return boardPosition;
    	}
    	
    	public int getTokenMoved() {
    		return tokenMoved;
    	}
    	
    	public void setMessageToClient(String newMessage) { 
    		mMessageToClient = newMessage; 
        }     
    	
    	private void parseLine(String line) {
    		String [] moveValues = line.split(",");
         	if (moveValues[1] != null) {
         		tokenMoved = Integer.parseInt(moveValues[1].trim());
         	}
         	if (moveValues[2] != null) {
         		boardPosition = Integer.parseInt(moveValues[2].trim());
         	}
        	if (moveValues[3] != null) {
        		int player1TokenChoice = Integer.parseInt(moveValues[3].trim());
        		if (player1TokenChoice > -1) {
        			mPlayer2TokenChoice = player1TokenChoice;
        			mPlayer1TokenChoice = mPlayer2TokenChoice == GameView.BoardSpaceValues.CIRCLE ? GameView.BoardSpaceValues.CROSS : GameView.BoardSpaceValues.CIRCLE;
        			mHandler.sendEmptyMessage(MSG_NETWORK_SET_TOKEN_CHOICE);
        		}
        	}
    	}
    	
    	ServerThread() {
    		super();
    	}
    	
        public void run() { 
            try {
    			mPlayer1NetworkScore = mPlayer2NetworkScore = 0;
    			mGameStarted = false;
            	while (mServerRunning) {
                    if (mRabbitMQServerResponseHandler.getRabbitMQResponse() != null) {
                    	writeToLog("ServerThread", "Retrieving command: " + mRabbitMQServerResponseHandler.getRabbitMQResponse()); 
                    	if (mRabbitMQServerResponseHandler.getRabbitMQResponse().contains("tokenList")) {
                    		getGameSetUpFromClient(mRabbitMQServerResponseHandler.getRabbitMQResponse());
                    		mHandler.sendEmptyMessage(DISMISS_WAIT_FOR_NEW_GAME_FROM_CLIENT);
                    		mHandler.sendEmptyMessage(ACCEPT_INCOMING_GAME_REQUEST_FROM_CLIENT); 
                    		mGameStarted = true;
                    	}
                    	if (mRabbitMQServerResponseHandler.getRabbitMQResponse().startsWith("moved")) {
                    		parseLine(mRabbitMQServerResponseHandler.getRabbitMQResponse());
                    		mHandler.sendEmptyMessage(MSG_NETWORK_SERVER_TURN);
                    	}
        				if (mRabbitMQServerResponseHandler.getRabbitMQResponse().startsWith("leftGame") && mGameStarted) { 
        					playerNotPlaying(mRabbitMQServerResponseHandler.getRabbitMQResponse(), 1);
        					mGameStarted = false;
        					mServerRunning = false;
        				}
        				mRabbitMQServerResponseHandler.setRabbitMQResponse(null);
                    }
                    
                    if (mMessageToClient != null) {
                    	writeToLog("ServerThread", "Server about to respond to client: "+mMessageToClient); 
               			//String hostName = resources.getString(R.string.RabbitMQHostName);
                        //String hostName = (String)WillyShmoApplication.getConfigMap("RabbitMQIpAddress");
                        //String queuePrefix = (String)WillyShmoApplication.getConfigMap("RabbitMQQueuePrefix");
               			String qName = mQueuePrefix + "-" + "client"  + "-" + mPlayer2Id;
               			new SendMessageToRabbitMQTask().execute(mHostName, qName, null, mMessageToClient, GameActivity.this, resources);
               			writeToLog("ServerThread", "Server responded to client completed: "+mMessageToClient + " queue: " + qName); 
                        if (mMessageToClient.startsWith("leftGame") || mMessageToClient.startsWith("noPlay")) {
                     		mServerRunning = false;
                       		imServing = false;
                        }
                       	mMessageToClient = null;
                    }
                    Thread.sleep(THREAD_SLEEP_INTERVAL);
            	} // while end
            	mPlayer1NetworkScore = mPlayer2NetworkScore = 0;
                imServing = false;
            } catch (Exception e) {
                //e.printStackTrace();
            	writeToLog("ServerThread", "error in Server Thread: "+e.getMessage());             	
                GameActivity.this.sendToastMessage(e.getMessage());
            } finally {
            	mServerRunning = false;
            	imServing = false;
            	
            	mPlayer1NetworkScore = mPlayer2NetworkScore = 0;
            	mServerThread = null;

//        		String onlineNowIndicator = "";
//        		if (!mClient) {
//        			onlineNowIndicator = "&onlineNow=false";
//        		}

        		String urlData = "/gamePlayer/update/?id=" + mPlayer1Id + "&onlineNow=false&playingNow=false&opponentId=0";
    			new SendMessageToWillyShmoServer().execute(urlData, null, GameActivity.this, resources, Boolean.FALSE);
            	new DisposeRabbitMQTask().execute(mMessageServerConsumer, resources, GameActivity.this);      
            	writeToLog("ServerThread", "server finished");
//            	finish(); 
            }
        }
    }
	
    public void playerNotPlaying(String line, int reason) {
    	String [] playerName = line.split(",");
    	if (playerName[1] != null) {
    		mNetworkOpponentPlayerName = playerName[1];
    	}
    	switch (reason) {
    	case 0:
    		mHandler.sendEmptyMessage(MSG_NETWORK_SERVER_REFUSED_GAME);
    		break;
    	case 1:
    		mHandler.sendEmptyMessage(MSG_NETWORK_SERVER_LEFT_GAME);
    		break;
    	}
    }
    
    protected class ClientThread extends Thread {
        private String mMessageToServer;
        private int boardPosition;
        private int tokenMoved;
//        public List<int[]> mTokensFromClient;
        private boolean mGameStarted;

    	public int getBoardPosition() {
    		return boardPosition;
    	}
    	
    	public int getTokenMoved() {
    		return tokenMoved;
    	}
    	
    	public void setGameStarted(boolean gameStarted) {
    		mGameStarted = gameStarted;
    	}

    	ClientThread() {
    		super();
    	}
    	
    	public String getPlayer1Name() {
    		return mPlayer1Name;
    	}
    	
    	public String getPlayer1Id() {
    		return Integer.toString(mPlayer1Id);
    	}    	
    	
        public void setMessageToServer(String newMessage) {
        	mMessageToServer = newMessage;
        }  
        
//        public void setMessageFromServer(String newMessage) { // called from Twitter get direct messages async task
//        	mMessageFromServer = newMessage;
//        }  
        
        private void parseMove(String line) {
        	String [] moveValues = line.split(",");
        	if (moveValues[1] != null)
        		tokenMoved = Integer.parseInt(moveValues[1].trim());
        	if (moveValues[2] != null)
        		boardPosition = Integer.parseInt(moveValues[2].trim());
        	if (moveValues[3] != null) {
        		int player1TokenChoice = Integer.parseInt(moveValues[3].trim());
        		if (player1TokenChoice > -1) {
        			mPlayer2TokenChoice = player1TokenChoice;
        			mPlayer1TokenChoice = mPlayer2TokenChoice == GameView.BoardSpaceValues.CIRCLE ? GameView.BoardSpaceValues.CROSS : GameView.BoardSpaceValues.CIRCLE;
        			mHandler.sendEmptyMessage(MSG_NETWORK_SET_TOKEN_CHOICE);
        		}
        	}
        }

        public void run() {
        	try {
        		writeToLog("ClientService", "client run method entered");

        		while (mClientRunning) {
        			if (mMessageToServer != null) {
                        //String hostName = (String)WillyShmoApplication.getConfigMap("RabbitMQIpAddress");
                        //String queuePrefix = (String)WillyShmoApplication.getConfigMap("RabbitMQQueuePrefix");
        				//String hostName = resources.getString(R.string.RabbitMQHostName);
        				String qName = mQueuePrefix + "-" + "server"  + "-" + mPlayer2Id;
        				new SendMessageToRabbitMQTask().execute(mHostName, qName, null, mMessageToServer, GameActivity.this, resources);
        				writeToLog("ClientThread", "Sending command: " + mMessageToServer + " queue: " + qName); 
                        if (mMessageToServer.startsWith("leftGame")) {
                        	mClientRunning = false;
                        } 
            			mMessageToServer = null;
        			}
        			
        			if (mRabbitMQClientResponseHandler.getRabbitMQResponse() != null) {
        				writeToLog("ClientThread", "read response: " + mRabbitMQClientResponseHandler.getRabbitMQResponse());        				
        				
        				if (mClientWaitDialog != null) {
        					mHandler.sendEmptyMessage(DISMISS_WAIT_FOR_NEW_GAME_FROM_HOST);                  
        				}
        				if (mRabbitMQClientResponseHandler.getRabbitMQResponse().startsWith("moved")) {
        					parseMove(mRabbitMQClientResponseHandler.getRabbitMQResponse());
        					mHandler.sendEmptyMessage(MSG_NETWORK_CLIENT_TURN);
        					mGameStarted = true;
        				}
        				if (mRabbitMQClientResponseHandler.getRabbitMQResponse().startsWith("moveFirst")) {
        					mHandler.sendEmptyMessage(MSG_NETWORK_CLIENT_MAKE_FIRST_MOVE);
        					mGameStarted = true;
        				}
        				if (mRabbitMQClientResponseHandler.getRabbitMQResponse().startsWith("noPlay")) {
        					playerNotPlaying(mRabbitMQClientResponseHandler.getRabbitMQResponse(), 0);
        					mGameStarted = false;
        				}
        				if (mRabbitMQClientResponseHandler.getRabbitMQResponse().startsWith("leftGame") && mGameStarted) { 
        					playerNotPlaying(mRabbitMQClientResponseHandler.getRabbitMQResponse(), 1);
        					mGameStarted = false;
        				}
        				mRabbitMQClientResponseHandler.setRabbitMQResponse(null);
        			}
        			Thread.sleep(THREAD_SLEEP_INTERVAL);
        		}
        		writeToLog("ClientThread", "client run method finished"); 
        	} catch (Exception e) {
        		//writeToLog("ClientService", "Client error 2: "+e);
				GameActivity.this.sendToastMessage(e.getMessage());
        	}
        	finally {
        		
//        		String onlineNowIndicator = "";
//        		if (!mGameStartedFromPlayerList) {
//        			onlineNowIndicator = "&onlineNow=false";
//        		}

        		String urlData = "/gamePlayer/update/?id=" + mPlayer1Id + "&playingNow=false&onlineNow=false&opponentId=0";
				new SendMessageToWillyShmoServer().execute(urlData, null, GameActivity.this, resources, Boolean.FALSE);
        		mPlayer1NetworkScore = mPlayer2NetworkScore = 0;
        		mClientRunning = false;
        		mClientThread = null;
        		new DisposeRabbitMQTask().execute(mMessageClientConsumer, resources, GameActivity.this); 
        		writeToLog("ClientThread", "client run method finally done"); 
        	}
        }
    }    
    
    @Override
    protected void onPause() { 
        super.onPause();  
        if (mClientRunning) {
			mClientThread.setMessageToServer("leftGame, " + mPlayer1Name);
        }        	
        if (imServing) {
			mServerThread.setMessageToClient("leftGame, " + mPlayer1Name);  
        } else if (mServerRunning) {
        	mServerRunning = false;
        }
        
        writeToLog("GameActivity", "onPause called"); 
    }   
    
    private static void writeToLog(String filter, String msg) {
    	if ("true".equalsIgnoreCase(resources.getString(R.string.debug))) {
    		Log.d(filter, msg);
    	}
    }
    
    private void getGameSetUpFromClient(String gameSetUp) {
    	try {
    	    mPlayer1TokenChoice = GameView.BoardSpaceValues.EMPTY;
    	    mPlayer2TokenChoice = GameView.BoardSpaceValues.EMPTY; // computer or opponent
    	    mTokensFromClient = new ArrayList<int[]>();
    		
    		JSONObject jsonObject = new JSONObject(gameSetUp);
    		JSONArray tokenArray = jsonObject.getJSONArray("tokenList"); 
        	
        	for (int y = 0; y < tokenArray.length(); y++) {
        		JSONObject tokenValues = tokenArray.getJSONObject(y);
        		String tokenId = tokenValues.getString("tokenId");
        		String tokenType = tokenValues.getString("tokenType");
        		
        		int tokenIntValue = Integer.parseInt(tokenId); 
        		int tokenIntType = Integer.parseInt(tokenType);
        		
        		if (tokenIntValue < 8) {
        			int resultValue = -1;
        			if (tokenIntValue > 3) {
        				resultValue = tokenIntValue - 4;
        			} else {
        				resultValue = tokenIntValue + 4;
        			}
        			mTokensFromClient.add(new int[] {resultValue, tokenIntType});
        		} else {
        			mTokensFromClient.add(new int[] {GameView.BoardSpaceValues.BOARDCENTER, tokenIntType});
        		}
        	}
        	
        	mPlayer2Name = jsonObject.getString("player1Name");
        	mPlayer2Id = jsonObject.getString("player1Id");
        } catch (JSONException e) {
             sendToastMessage(e.getMessage());
        }
    }
    
    private final static int ACCEPT_GAME = 1;
    private final static int REJECT_GAME = 2;
    
    private void setGameRequestFromClient(boolean start) {
		mServerHasOpponent = null;
		
    	String urlData = "/gamePlayer/update/?playingNow=true&id=" + mPlayer1Id +"&opponentId=" + mPlayer2Id;
    	if (start) {
			mHandler.sendEmptyMessage(NEW_GAME_FROM_CLIENT);
			imServing = true;
			mServerThread.setMessageToClient("serverAccepted");
    	} else {
			urlData = "/gamePlayer/update/?id=" + mPlayer1Id + "&playingNow=false&onlineNow=false&opponentId=0";
			mPlayer1NetworkScore = mPlayer2NetworkScore = 0;
			mPlayer2Name = null;
			if (mServerThread != null) {
				mServerThread.setMessageToClient("noPlay, " + mPlayer1Name); 
			}
		}

        //TODO - replace GameActivity.this with a static reference to getContext() set at class instantiation
		new SendMessageToWillyShmoServer().execute(urlData, null, GameActivity.this, resources, !start);
    } 

    private Handler newNetworkGameHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ACCEPT_GAME:
                	setGameRequestFromClient(true);
                break;
                case REJECT_GAME:
                	setGameRequestFromClient(false);
                break;
            }
        }
    };

    private void acceptIncomingGameRequestFromClient() {
        mMessageStartGameConsumer = new RabbitMQMessageConsumer(GameActivity.this, resources);
        mRabbitMQStartGameResponseHandler = new RabbitMQStartGameResponseHandler();
        mRabbitMQStartGameResponseHandler.setRabbitMQResponse("null"); 
        setUpMessageConsumer(mMessageStartGameConsumer, "startGame", mRabbitMQStartGameResponseHandler); 
        mRabbitMQStartGameResponseHandler.getRabbitMQResponse(); // get rid of "startGame" RabbitMQ message
		new DisposeRabbitMQTask().execute(mMessageStartGameConsumer, resources, GameActivity.this);
        // "Accept Game" call back.
        final Message acceptMsg = Message.obtain();
        acceptMsg.setTarget(newNetworkGameHandler);
        acceptMsg.what = ACCEPT_GAME;
        final Message rejectMsg = Message.obtain();
        rejectMsg.setTarget(newNetworkGameHandler);
        rejectMsg.what = REJECT_GAME;
        
        try {
        	new AlertDialog.Builder(GameActivity.this)
        	.setTitle(mPlayer2Name + " would like to play")
          
        	.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
        		@Override
        		public void onClick(DialogInterface dialog, int which) {
        			acceptMsg.sendToTarget();
        			}
        		}
        	)
        	.setCancelable(true)
        	.setIcon(R.drawable.willy_shmo_small_icon)
        	.setNegativeButton("Reject", new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int which) {
        			rejectMsg.sendToTarget();  
        			}
        		}
        	)
        	.show();
        } catch (Exception e) {
        	sendToastMessage(e.getMessage());
        }
    }        
    
    private void displayHostNotAvailableAlert() {
        try {
        	new AlertDialog.Builder(GameActivity.this)
        	.setTitle(mPlayer2Name + " has left the game")

        	.setNeutralButton("OK", new DialogInterface.OnClickListener() {
        		@Override
        			public void onClick(DialogInterface dialog, int which) {
        				GameActivity.this.finish(); 
        			}
        		}
        	)
        	.setIcon(R.drawable.willy_shmo_small_icon)
        	.show();
        } catch (Exception e) {
        	sendToastMessage(e.getMessage());
        }
    }
    
    private void displayServerRefusedGameAlert(String serverPlayerName) {
        try {
        	new AlertDialog.Builder(GameActivity.this)
        	.setIcon(R.drawable.willy_shmo_small_icon)
        	.setTitle("Sorry, " + serverPlayerName + " doesn't want to play now")
        	.setNeutralButton("OK", new DialogInterface.OnClickListener() {
        		@Override
        			public void onClick(DialogInterface dialog, int which) {
        				GameActivity.this.finish();
        			}
        		}
        	)
        	.show();
        } catch (Exception e) {
        	sendToastMessage(e.getMessage());
        }
    }

    private void displayOpponentLeftGameAlert(String serverPlayerName) {
        try {
        	new AlertDialog.Builder(GameActivity.this)
        	.setIcon(R.drawable.willy_shmo_small_icon)
        	.setTitle("Sorry, " + serverPlayerName + " has left the game")
        	.setNeutralButton("OK", new DialogInterface.OnClickListener() {
        		@Override
        			public void onClick(DialogInterface dialog, int which) {
        				GameActivity.this.finish();
        			}
        		}
        	)
        	.show();
        } catch (Exception e) {
        	sendToastMessage(e.getMessage());
        }
    }
    
    private void updateWebServerScore() {
		String urlData = "/gamePlayer/updateGamesPlayed/?id=" + mPlayer1Id + "&score="+mPlayer1NetworkScore;
		new SendMessageToWillyShmoServer().execute(urlData, null, GameActivity.this, resources, Boolean.FALSE);
    }
    
    public static boolean getMoveModeTouch() {
    	return mMoveModeTouch;
    }

    public static boolean getSoundMode() {
    	return mSoundMode;
    }
    
    public String getPlayer2Id() {
    	return mPlayer2Id;
    }
    
    private class ErrorHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
    		Toast.makeText(getApplicationContext(), (String)msg.obj, Toast.LENGTH_LONG).show();
        }
    }
    
    public void sendToastMessage(String message) {
    	Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
    	toast.show();    	
    }
    
    public boolean isClientRunning() {
    	return mClientRunning;
    }
    
	private class RabbitMQServerResponseHandler implements RabbitMQResponseHandler {
		public void setRabbitMQResponse(String rabbitMQResponse) {
			mRabbitMQServerResponse = rabbitMQResponse;
		}
		
		public String getRabbitMQResponse() {
			return mRabbitMQServerResponse;
		}
	}

	private class RabbitMQClientResponseHandler implements RabbitMQResponseHandler {
		public void setRabbitMQResponse(String rabbitMQResponse) {
			mRabbitMQClientResponse = rabbitMQResponse;
		}
		
		public String getRabbitMQResponse() {
			return mRabbitMQClientResponse;
		}
	}

	private class RabbitMQStartGameResponseHandler implements RabbitMQResponseHandler {
		public void setRabbitMQResponse(String rabbitMQResponse) {
			mRabbitMQStartGameResponse = rabbitMQResponse;
		}
		
		public String getRabbitMQResponse() {
			return mRabbitMQStartGameResponse;
		}
	}

	private void setUpMessageConsumer(RabbitMQMessageConsumer rabbitMQMessageConsumer, final String qNameQualifier, final RabbitMQResponseHandler rabbitMQResponseHandler) {
        //String hostName = (String)WillyShmoApplication.getConfigMap("RabbitMQIpAddress");
        //String queuePrefix = (String)WillyShmoApplication.getConfigMap("RabbitMQQueuePrefix");
		//String hostName = resources.getString(R.string.RabbitMQHostName);
		String qName = mQueuePrefix + "-" + qNameQualifier + "-" + mPlayer1Id;
		new ConsumerConnectTask().execute(mHostName, rabbitMQMessageConsumer, qName, GameActivity.this, resources, "GameActivity");
		writeToLog("GameActivity", qNameQualifier +" message consumer listening on queue: " + qName);		
		
		// register for messages
		rabbitMQMessageConsumer.setOnReceiveMessageHandler(new RabbitMQMessageConsumer.OnReceiveMessageHandler() {
			public void onReceiveMessage(byte[] message) {
				String text = "";
                text = new String(message, StandardCharsets.UTF_8);
                rabbitMQResponseHandler.setRabbitMQResponse(text);
				writeToLog("GameActivity", qNameQualifier + " OnReceiveMessageHandler received message: " + text);	
			}
		});
	}

    public void sendMessageToServerHost(String message) {
		//String hostName = resources.getString(R.string.RabbitMQHostName);
		String qName = mQueuePrefix + "-" + "server"  + "-" + mPlayer2Id;
		new SendMessageToRabbitMQTask().execute(mHostName, qName, null, message, GameActivity.this, resources);
		writeToLog("GameActivity", "sendMessageToServerHost: " + message + " queue: " + qName);		
    }

// to update the game count:
// http://ww2.guzzardo.com:8081/WillyShmoGrails/gamePlayer/updateGamesPlayed/?id=1

    public void showPrizeWon(int prizeType) {
    	try {
    		new AlertDialog.Builder(GameActivity.this)
    		.setTitle("Congratulations, you won a prize!")

    		.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
    			@Override
    			public void onClick(DialogInterface dialog, int which) {
    				Intent i  = new Intent(GameActivity.this, PrizesAvailableActivity.class);
    				startActivity(i);
    			}
    		})
    		.setCancelable(true)
    		.setIcon(R.drawable.willy_shmo_small_icon)
    		.setNegativeButton("Reject", new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int which) {
    			}
    		})
    		.show();
    	} catch (Exception e) {
    		//e.printStackTrace();
    		sendToastMessage(e.getMessage());
    	}
    }
    
    public ServerThread getServerThread() {
    	return mServerThread;
    }
    
    public String getPlayer1Name() {
    	return mPlayer1Name;
    }

}
