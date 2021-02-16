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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.guzzardo.android.willyshmo.kotlintictacdoh.GameActivity.ClientThread;
import com.guzzardo.android.willyshmo.kotlintictacdoh.MainActivity.UserPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

//-----------------------------------------------

public class GameView extends View {
	
    public static final long FPS_MS = 1000/2;

    public enum State {
        UNKNOWN(-3),
        WIN(-2),
        EMPTY(0),
        PLAYER1(1),
        PLAYER2(2),
        PLAYERBOTH(3); // xo token 

        private int mValue;

        private State(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static State fromInt(int i) {
            for (State s : values()) {
                if (s.getValue() == i) {
                    return s;
                }
            }
            return EMPTY;
        }
    }

    //constants related to gameBoard drawing:
    private static int MARGIN = 5; // was 4 was 2 // was 4 // had set it to zero for some reason?
    private static int GRIDLINEWIDTH = 4; // was 5
    
    //TODO - calculate these 3 values in onMeasure
    private static int TOKENSIZE; // bitmap pixel size of X or O on board
    private static int mTokenRadius = 40;  
    private static final int PORTRAITOFFSETX = 5; // X offset to board grid in portrait mode
    private static final int PORTRAITOFFSETY = 5;
    private static final int PORTRAITWIDTHHEIGHT = 300; // portrait width and height of view square
    
    private static int BoardLowerLimit;
    private static int mDisplayMode; // portrait or landscape
    private static final int portraitComputerLiteralOffset = 245; // was 210;
    private static final int portraitHumanTokenSelectedOffsetX = 20;
    private static int landscapeRightMoveXLimitPlayer1;
    private static int landscapeLeftMoveXLimitPlayer2;   
    
    private static int landscapeRightMoveXLimitPlayer2;
    private static int landscapeLeftMoveXLimitPlayer1;  
    private boolean mViewDisabled;
    
    private static final int landscapeHumanTokenSelectedOffsetX = 25;
    private static final int portraitIncrementXPlayer1 = 50;
    private static final int portraitIncrementYPlayer1 = 0;
    private static final int portraitStartingXPlayer1 = 50;
    private static final int portraitStartingYPlayer1 = 260;

    private static final int portraitIncrementXPlayer2 = 0;
    private static final int portraitIncrementYPlayer2 = 50;
    private static final int portraitStartingXPlayer2 = 260;
    private static final int portraitStartingYPlayer2 = 60;

    private static final int landscapeIncrementXPlayer1 = 0;
    private static int landscapeIncrementYPlayer;
    private static final int landscapeStartingXPlayer1 = 50;
    private static final int landscapeStartingYPlayer1 = 25;  // was 50
    private static final int landscapeIncrementXPlayer2 = 0;
    private static int landscapeStartingXPlayer2; // = 410; // calculated dynamically in onMeasure based upon screen width
    private static final int landscapeStartingYPlayer2 = 25;  // was 50
    private Context mContext;
    private static final int MSG_BLINK = 1;
    private static final int MSG_BLINK_TOKEN = 2;
    private static final int MSG_BLINK_SQUARE = 3;    
    private static final boolean SPACENOTAVAILABLE  = false;
    private static boolean INITIALIZATIONCOMPLETED  = false;
    private static final int NUMBEROFTOKENS = 9;
    private static int mTokenSize, mTokenColor1, mTokenColor2;
    private static int computerMove; //temporary value to assign token to computer move
    private static int mPlayer1TokenChoice, mPlayer2TokenChoice;
    private static final int[][] mStartingPlayerTokenPositions = 
//		{	{50, startingPlayer1YOffset}, 
//    		{100, startingPlayer1YOffset}, 
//    		{150, startingPlayer1YOffset},
//    		{200, startingPlayer1YOffset}
//		};
    	{
    		{0, 0}, 
    		{0, 0}, 
    		{0, 0},
    		{0, 0}
    	};

    private static final int[] startingGameTokenCard = 
    	{	BoardSpaceValues.CIRCLE, BoardSpaceValues.CROSS, BoardSpaceValues.CIRCLE, BoardSpaceValues.CROSS,
    		BoardSpaceValues.CIRCLE, BoardSpaceValues.CROSS, BoardSpaceValues.CIRCLE, BoardSpaceValues.CROSS,
    		BoardSpaceValues.CIRCLECROSS
    	};
    
    public static int[] mGameTokenCard = 
    	{	BoardSpaceValues.EMPTY, BoardSpaceValues.EMPTY, BoardSpaceValues.EMPTY, BoardSpaceValues.EMPTY,
    		BoardSpaceValues.EMPTY, BoardSpaceValues.EMPTY, BoardSpaceValues.EMPTY, BoardSpaceValues.EMPTY,
    		BoardSpaceValues.EMPTY
    	};

    private final Handler mHandler = new Handler(new MyHandler());

    private final Rect mSrcRect = new Rect();
    private final Rect mDstRect = new Rect();
    private final Rect mTakenRect = new Rect();

    private static int mSxy;
    private int mOffsetX;
    private int mOffsetY;
    private Paint mWinPaint;
    private Paint mLinePaint;
    private Paint mBmpPaint;
    private Bitmap mBmpCrossCenter, mBmpCrossPlayer1,  mBmpCrossPlayer2;
    private Bitmap mBmpCircleCenter, mBmpCirclePlayer1, mBmpCirclePlayer2;
    private Bitmap mBmpCircleCrossCenter, mBmpCircleCrossPlayer1, mBmpCircleCrossPlayer2;
    private Bitmap mBmpAvailableMove;
    private Paint mTextPaint;  
    private Bitmap mBmpTakenMove; 
    private Bitmap mBmpPrize; 
    private static int mPrizeLocation = -1;
    private static int[] mPrizeXBoardLocationArray = {0, 1, 2, 3, 4, 0, 1, 2, 3, 4, 0, 1, 2, 3, 4, 0, 1, 2, 3, 4, 0, 1, 2, 3, 4,};
    private static int[] mPrizeYBoardLocationArray = {0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4,};
    private static int mPrizeXBoardLocation;
    private static int mPrizeYBoardLocation;
    

    private Random mRandom = new Random();
    private ICellListener mCellListener;

    /** Contains one of {@link State#EMPTY}, {@link State#PLAYER1} or {@link State#PLAYER2}. */
    private final State[] mData = new State[BoardSpaceValues.BOARDSIZE];
    private static final int[] mBoardSpaceValue = new int[BoardSpaceValues.BOARDSIZE]; // -1 = empty, 0 = circle, 1 = cross 2 = circleCross
    private final boolean[] mBoardSpaceAvailable = new boolean[BoardSpaceValues.BOARDSIZE]; // false = not available

    private int mSelectedCell = -1;
    private State mSelectedValue = State.EMPTY;
    private State mCurrentPlayer = State.UNKNOWN;
    private State mWinner = State.EMPTY;

    private int mWinCol = -1;
    private int mWinRow = -1;
    private int mWinDiag = -1;

    private boolean mBlinkDisplayOff;
    private static boolean HUMAN_VS_HUMAN;

    private final Rect mBlinkRect = new Rect();

    //private ColorBall[] mColorBall = new ColorBall[ColorBall.getMaxBalls()]; // array that holds the balls
    private ColorBall[] mColorBall = new ColorBall[ColorBall.MAXBALLS]; // array that holds the balls
    private int mBallId = -1; // variable to know what ball is being dragged
    
    private static ClientThread mClientThread;
    private static GameActivity mGameActivity;
    
    private static int mPrevSelectedBall; //save value for touch selection
    private static int mPrevSelectedCell; //save value for touch selection
    
    private static Resources resources;
    
	public interface ICellListener {
        abstract void onCellSelected();
    }
	
    public interface BoardSpaceValues {
    	static final int EMPTY = -1;
        static final int CIRCLE = 0;
        static final int CROSS = 1;
        static final int CIRCLECROSS = 2;
        static final int BOARDSIZE = 25;
        static final int BOARDCENTER = 12;
	}
    
    public interface ScreenOrientation {
    	static final int PORTRAIT = 0;
    	static final int LANDSCAPE = 1;
    }
	
    public void setViewDisabled(boolean disabled) {
    	mViewDisabled = disabled;
    }
    
    public void setHumanState(boolean humanState) {
    	HUMAN_VS_HUMAN = humanState;
    }
    
	public void setBoardSpaceValue(int offset, int token) {
		mBoardSpaceValue[offset] = token;
		mBoardSpaceAvailable[offset] = false; 
	}
	
	public void setBoardSpaceValueCenter(int tokenType) {
		mBoardSpaceValue[BoardSpaceValues.BOARDCENTER] = tokenType;
		mBoardSpaceAvailable[BoardSpaceValues.BOARDCENTER] = false;  
	}
	

	public void setBoardSpaceValue(int offset) {
		if (mBallId > -1) {
			mBoardSpaceValue[offset] = mColorBall[mBallId].getType();
			mBoardSpaceAvailable[offset] = false; 
		}
	}
	
	public int getBallMoved() {
		return mBallId;
	}

	public int getBoardSpaceValue(int offset) {
			return mBoardSpaceValue[offset];
	}
	
	public boolean testBoardFull(int howFull) {
		int tokenCount = 0;
		for (int x = 0; x < mBoardSpaceValue.length; x++) {
			if (mBoardSpaceValue[x] != BoardSpaceValues.EMPTY)
				tokenCount++;
		}
		if (tokenCount == howFull)
			return true;
		return false;
	}
	
	public void setPlayer1TokenChoice(int player1TokenChoice) {
		mPlayer1TokenChoice = player1TokenChoice;
	}

	public void setPlayer2TokenChoice(int player2TokenChoice) {
		mPlayer2TokenChoice = player2TokenChoice;
	}
	
    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true); //necessary for getting the touch events
        requestFocus();
        resources = getResources();
        mContext = context;
        getSharedPreferences();
        
        mBmpPrize = getResBitmap(R.drawable.prize_token);

        mBmpCrossPlayer1 = getResBitmap(R.drawable.lib_crossred);
        ColorBall.setTokenColor(mBmpCrossPlayer1, mTokenColor1);
        
        mBmpCrossPlayer2 = getResBitmap(R.drawable.lib_crossblue);
        ColorBall.setTokenColor(mBmpCrossPlayer2, mTokenColor2);
        
        mBmpCrossCenter = getResBitmap(R.drawable.lib_crossgreen);
        
        mBmpCirclePlayer1 = getResBitmap(R.drawable.lib_circlered);
        ColorBall.setTokenColor(mBmpCirclePlayer1, mTokenColor1);
        
        mBmpCirclePlayer2 = getResBitmap(R.drawable.lib_circleblue);
        ColorBall.setTokenColor(mBmpCirclePlayer2, mTokenColor2);
        
        mBmpCircleCenter = getResBitmap(R.drawable.lib_circlegreen); 
        
        mBmpCircleCrossPlayer1 = getResBitmap(R.drawable.lib_circlecrossred);
        ColorBall.setTokenColor(mBmpCircleCrossPlayer1, mTokenColor1);
        
        mBmpCircleCrossPlayer2 = getResBitmap(R.drawable.lib_circlecrossblue);
        ColorBall.setTokenColor(mBmpCircleCrossPlayer2, mTokenColor2);
        
        mBmpCircleCrossCenter = getResBitmap(R.drawable.lib_circlecrossgreen);         
        
        mBmpAvailableMove = getResBitmap(R.drawable.allowed_move);
        mBmpTakenMove = getResBitmap(R.drawable.taken_move);        
        
        if (mBmpCrossPlayer1 != null) {
            mSrcRect.set(0, 0, mBmpCrossPlayer1.getWidth() - 1, mBmpCrossPlayer1.getHeight() - 1);
        }
        if (mBmpAvailableMove != null) {
            mTakenRect.set(0, 0, mBmpAvailableMove.getWidth() - 1, mBmpAvailableMove.getHeight() - 1);
        }

        mBmpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mLinePaint = new Paint();
        mLinePaint.setColor(0xFFFFFF00);
        mLinePaint.setStrokeWidth(GRIDLINEWIDTH);
        mLinePaint.setStyle(Style.STROKE);

        mWinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWinPaint.setColor(0xFFFF0000);
        mWinPaint.setStrokeWidth(5); // was 10
        mWinPaint.setStyle(Style.STROKE);

        mTextPaint = new Paint();
        mTextPaint.setColor(0xFFFF0000);
        mTextPaint.setStrokeWidth(1);
        mTextPaint.setStyle(Style.STROKE);
        
        initalizeGameValues();
        
        if (isInEditMode()) {
            // In edit mode (e.g. in the Eclipse ADT graphical layout editor)
            // we'll use some random data to display the state.

            for (int i = 0; i < mData.length; i++) {
                mData[i] = State.fromInt(mRandom.nextInt(3));
            }
        }
        
        computerMove = 4; //temporary value for determining next token for computer to move 
        mPlayer1TokenChoice = -1;
        mPlayer2TokenChoice = -1;
        
        INITIALIZATIONCOMPLETED  = false;
    }
    
    public void setGamePrize() {
    	mPrizeLocation = -1;
    	//FIXME also need to think about human vs network
    	if (WillyShmoApplication.getPrizeNames() != null && !HUMAN_VS_HUMAN) {
//      if (WillyShmoApplication.getPrizeNames() != null) { uncomment this line to test prizes with another player
    		mPrizeLocation = mRandom.nextInt(BoardSpaceValues.BOARDSIZE);
//    		mPrizeLocation = 11; //set prize to a fixed location
    		mPrizeXBoardLocation = mPrizeXBoardLocationArray[mPrizeLocation]; 
    		mPrizeYBoardLocation = mPrizeYBoardLocationArray[mPrizeLocation];
    	}
    }
    
    protected void initalizeGameValues() {
        for (int x = 0; x < mData.length; x++) {
            mData[x] = State.EMPTY;
            mBoardSpaceValue[x] = BoardSpaceValues.EMPTY;
            mBoardSpaceAvailable[x] = SPACENOTAVAILABLE;
        }

        mBoardSpaceAvailable[7] = true;
        mBoardSpaceAvailable[11] = true;
        mBoardSpaceAvailable[13] = true;
        mBoardSpaceAvailable[17] = true;        
        
        mData[BoardSpaceValues.BOARDCENTER] = State.PLAYERBOTH;
        
        mWinCol = -1;
        mWinRow = -1;
        mWinDiag = -1;
        mBallId = -1;
    }
    
    public void setTokenCards() {
    	for (int x = 0; x < mGameTokenCard.length; x++) {
    		mGameTokenCard[x] = BoardSpaceValues.EMPTY;
    	}
    	
        for (int x = 0; x < startingGameTokenCard.length; x++) {
        	boolean positionFilled = false;
        	while (!positionFilled) {
        		int randomCard = mRandom.nextInt(NUMBEROFTOKENS);
        		if (mGameTokenCard[randomCard] == BoardSpaceValues.EMPTY) {
        			mGameTokenCard[randomCard] = startingGameTokenCard[x];
        			positionFilled = true;
        		}
        	}
        }
    }
    
    //TODO- this method should be combined with initializePlayerTokens
    private void initializeBallPositions() {
    	int incrementX = portraitIncrementXPlayer1;
    	int startX = portraitStartingXPlayer1;
    	int incrementY = portraitIncrementYPlayer1;
    	int startY = portraitStartingYPlayer1;
    	
    	if (mDisplayMode == ScreenOrientation.LANDSCAPE) {
        	incrementX = landscapeIncrementXPlayer1;
        	startX = landscapeStartingXPlayer1;
        	incrementY = landscapeIncrementYPlayer;
        	startY = landscapeStartingXPlayer1;
    	}
    	
    	for (int x = 0; x < 4; x++) {
    		mStartingPlayerTokenPositions[x][0] = startX;
    		mStartingPlayerTokenPositions[x][1] = startY;
    		startX += incrementX;
    		startY += incrementY;
    	}
    }
    
    private void initializePlayerTokens(Context context) {
    	int portraitLocationXPlayer1 = portraitStartingXPlayer1; 
    	int portraitLocationYPlayer1 = portraitStartingYPlayer1; 
    	int portraitLocationXPlayer2 = portraitStartingXPlayer2; 
    	int portraitLocationYPlayer2 = portraitStartingYPlayer2; 
    	
    	int landscapeLocationXPlayer1 = landscapeStartingXPlayer1; 
    	int landscapeLocationYPlayer1 = landscapeStartingYPlayer1; 
    	int landscapeLocationXPlayer2 = landscapeStartingXPlayer2; 
    	int landscapeLocationYPlayer2 = landscapeStartingYPlayer2; 
    	
    	setTokenCards();
 	
//    	for (int x = 0; x < mGameTokenCard.length; x++) {
//    		mGameTokenCard[x] = BoardSpaceValues.EMPTY;
//    	}
//    	
//        for (int x = 0; x < startingGameTokenCard.length; x++) {
//        	boolean positionFilled = false;
//        	while (!positionFilled) {
//        		int randomCard = mRandom.nextInt(NUMBEROFTOKENS);
//        		if (mGameTokenCard[randomCard] == BoardSpaceValues.EMPTY) {
//        			mGameTokenCard[randomCard] = startingGameTokenCard[x];
//        			positionFilled = true;
//        		}
//        	}
//        }
        
        setBoardSpaceValue(BoardSpaceValues.BOARDCENTER, mGameTokenCard[NUMBEROFTOKENS - 1]);
     

// initialize board to specific test values        
//    	for (int x = 0; x < gameTokenCard.length; x++) {
//    		gameTokenCard[x] = startingGameTokenCard[x];
//    	} 
//      setBoardSpaceValue(BOARDCENTER, BoardSpaceValues.CIRCLE);             

    	Point tokenPointLandscape = new Point();
    	Point tokenPointPortrait = new Point();
    	
        for (int x = 0; x < 4; x++) {
        	tokenPointLandscape.set(landscapeLocationXPlayer1, landscapeLocationYPlayer1);
        	tokenPointPortrait.set(portraitLocationXPlayer1, portraitLocationYPlayer1);
        	
        	landscapeLocationXPlayer1 += landscapeIncrementXPlayer1;
        	portraitLocationXPlayer1 += portraitIncrementXPlayer1;
        	landscapeLocationYPlayer1 += landscapeIncrementYPlayer;
        	portraitLocationYPlayer1 += portraitIncrementYPlayer1;

        	int resource = R.drawable.lib_circlered;
        	if (mGameTokenCard[x] == ColorBall.CROSS)
        		resource = R.drawable.lib_crossred;
        	else
            if (mGameTokenCard[x] == ColorBall.CIRCLECROSS)
            	resource = R.drawable.lib_circlecrossred;

        	mColorBall[x] = new ColorBall(context, resource, tokenPointLandscape, tokenPointPortrait, mDisplayMode, mGameTokenCard[x], mTokenColor1);
        }
        
        for (int x = 4; x < NUMBEROFTOKENS - 1; x++) {
        	tokenPointLandscape.set(landscapeLocationXPlayer2, landscapeLocationYPlayer2);
        	tokenPointPortrait.set(portraitLocationXPlayer2, portraitLocationYPlayer2);
        	
        	landscapeLocationXPlayer2 += landscapeIncrementXPlayer2;
        	portraitLocationXPlayer2 += portraitIncrementXPlayer2;
        	landscapeLocationYPlayer2 += landscapeIncrementYPlayer;
        	portraitLocationYPlayer2 += portraitIncrementYPlayer2;
        	
        	int resource = R.drawable.lib_circleblue;
        	if (mGameTokenCard[x] == ColorBall.CROSS)
        		resource = R.drawable.lib_crossblue;
        	else
            if (mGameTokenCard[x] == ColorBall.CIRCLECROSS)
            	resource = R.drawable.lib_circlecrossblue;
        	
        	mColorBall[x] = new ColorBall(context, resource, tokenPointLandscape, tokenPointPortrait, mDisplayMode, mGameTokenCard[x], mTokenColor2);
//        	colorBall[x].setDisabled(true);
        }
        
//      if (mClientThread != null) {
        if (isClientRunning()) {        	
        	sendTokensToServer();
        }
    }
    
    public void updatePlayerToken(int id, int tokenType) {
    	Bitmap bitmap = null;
//    	int tokenColor = 0;
    	if (id < 4) { 
//    		tokenColor = mTokenColor1;
    		bitmap = mBmpCirclePlayer1;
    		//resource = R.drawable.lib_circlered;
    		if (tokenType == ColorBall.CROSS) {
    			//resource = R.drawable.lib_crossred;
    			bitmap = mBmpCrossPlayer1;
    		} else {
    			if (tokenType == ColorBall.CIRCLECROSS) {
    				//resource = R.drawable.lib_circlecrossred;
    				bitmap =  mBmpCircleCrossPlayer1;
    			}
    		}
    	} else {
//    		tokenColor = mTokenColor2;
        	//resource = R.drawable.lib_circleblue;
    		bitmap = mBmpCirclePlayer2;
        	if (tokenType == ColorBall.CROSS) {
        		//resource = R.drawable.lib_crossblue;
        		bitmap =  mBmpCrossPlayer2;
        	} else {
        		if (tokenType == ColorBall.CIRCLECROSS) {
        			//resource = R.drawable.lib_circlecrossblue;
        			bitmap =  mBmpCircleCrossPlayer2;
        		}
        	}
    	}

    	ColorBall ball = mColorBall[id];
    	ball.updateBall(mContext, tokenType, bitmap); 
    }
    
    public void sendTokensToServer() {
    	try {
    		JSONObject tokenList = new JSONObject();
    		JSONArray tokenArray = new JSONArray();
    		for (int x = 0; x < NUMBEROFTOKENS; x++) {
    			JSONObject tokenValues = new JSONObject();
    			tokenValues.put("tokenId", x);
    			tokenValues.put("tokenType", mGameTokenCard[x]);
    			tokenArray.put(tokenValues);
    		}
    		tokenList.put("tokenList",tokenArray);
    		String player1Name = mClientThread.getPlayer1Name();
    		String player1Id = mClientThread.getPlayer1Id();
    		tokenList.put("player1Name", player1Name);
    		tokenList.put("player1Id", player1Id);
    		String tokenListString = tokenList.toString();
    		
//    		mClientThread.setMessageToServer(tokenListString);
//    		String player2Id = mGameActivity.getPlayer2Id();
    		
    		//TODO send this list to player 2 via Twitter
    		mGameActivity.sendMessageToServerHost(tokenListString);
    		
    		setViewDisabled(true);
    		mGameActivity.highlightCurrentPlayer(State.PLAYER2);
    		mClientThread.setGameStarted(true);    		
        } catch (JSONException e) {
            //e.printStackTrace();
            mGameActivity.sendToastMessage(e.getMessage());
        } 
    }
    
//    public void sendMessageToTwitter(String twitterMessage) {
//    	try {
//    		final TembooSession tembooSession = new TembooSession(AuthenticationValues.getTembooAccountName(), 
//    				AuthenticationValues.getTembooAppKeyName(), AuthenticationValues.getTembooAppKeyValue());
//
//    		SendTwitterDirectMessage sendTwitterDirectMessage = new SendTwitterDirectMessage();
//    		sendTwitterDirectMessage.execute(this, tembooSession, mContext, twitterMessage, mTwitterOpponentScreenName);
//    	} catch (TembooException e) {
//    		//FIXME - do something with these exceptions!
//    		e.printStackTrace();
//    	}
//    }
    
    public void setTwitterApplicationResponse(String twitterResponse) {
    	System.out.println("twitter response: " + twitterResponse);
    }
    
    private void resetUnusedTokens() {
        for (ColorBall ball : mColorBall) {
       		if (ball.isDisabled())         	
       			continue;
       		if (ball.getID() == mBallId)
       			continue;
       		ball.resetPosition(mDisplayMode);
       	}
    }
    
    public State[] getData() {
        return mData;
    }
    
	public int[] getBoardSpaceValues() { 
		return mBoardSpaceValue;
	}

	public boolean[] getBoardSpaceAvailableValues() { 
		return mBoardSpaceAvailable;
	}
	
    public void setCell(int cellIndex, State value) {
        mData[cellIndex] = value;
        invalidate();
    }

    public void setCellListener(ICellListener cellListener) {
        mCellListener = cellListener;
    }

    public int getSelection() {
        if (mSelectedValue == mCurrentPlayer) {
            return mSelectedCell;
        }
        return -1;
    }

    public int getTokenAtCell(int cell) {
    	return mBoardSpaceValue[cell];
    }
    
    public State getCurrentPlayer() {
        return mCurrentPlayer;
    }

    public void setCurrentPlayer(State player) {
        mCurrentPlayer = player;
        mSelectedCell = -1;
    }

    public State getWinner() {
        return mWinner;
    }

    public void setWinner(State winner) {
        mWinner = winner;
    }

    /** Sets winning mark on specified column or row (0..2) or diagonal (0..1). */
    public void setFinished(int col, int row, int diagonal) {
        mWinCol = col;
        mWinRow = row;
        mWinDiag = diagonal;
    }
    
    private Bitmap getTokenToDraw(int location) {
    	Bitmap cross = mBmpCrossPlayer1;
    	Bitmap circle = mBmpCirclePlayer1;
    	Bitmap circleCross = mBmpCircleCrossPlayer1;
    	
    	if (mData[location] == State.PLAYER2) {
    		cross = mBmpCrossPlayer2;
        	circle = mBmpCirclePlayer2;
        	circleCross = mBmpCircleCrossPlayer2;
    	}
    	
    	Bitmap tokenToDraw = circle;
        if (mBoardSpaceValue[location] == BoardSpaceValues.CROSS)
        	tokenToDraw = cross;
        else 
        if (mBoardSpaceValue[location] == BoardSpaceValues.CIRCLECROSS)
        	tokenToDraw = circleCross;
        return tokenToDraw;
    }

    private Bitmap getTokenToDrawCenter() {
    	Bitmap tokenToDraw = mBmpCircleCenter;
        if (mBoardSpaceValue[BoardSpaceValues.BOARDCENTER] == BoardSpaceValues.CROSS)
        	tokenToDraw = mBmpCrossCenter;
        else 
        if (mBoardSpaceValue[BoardSpaceValues.BOARDCENTER] == BoardSpaceValues.CIRCLECROSS)
        	tokenToDraw = mBmpCircleCrossCenter;
        return tokenToDraw;
    }
    
    private void drawPlayerToken(Canvas canvas, int location) {
    	if (mBoardSpaceValue[location] != BoardSpaceValues.EMPTY) {
       		canvas.drawBitmap(mBmpTakenMove, mTakenRect, mDstRect, mBmpPaint); //revert background to black
       		Bitmap tokenToDraw = getTokenToDraw(location);
       		canvas.drawBitmap(tokenToDraw, mSrcRect, mDstRect, mBmpPaint);
       	}
    }
   
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
//        System.out.println("onDraw called with margin set to: "+MARGIN+" landscape offset: "+mOffsetX);
//        System.out.println("mSxy: "+mSxy+" mOffsetX: "+mOffsetX+" mOffsetY: "+mOffsetY);
        int s3  = mSxy * 5;
        
        int x7 = mOffsetX;
        int y7 = mOffsetY;
        Bitmap tokenToDraw = null;
       
        for (int i = 0, k = mSxy; i < 4; i++, k += mSxy) {
            canvas.drawLine(x7    , y7 + k, x7 + s3 - 1, y7 + k     , mLinePaint); // draw horizontal rows 
            canvas.drawLine(x7 + k, y7    , x7 + k     , y7 + s3 - 1, mLinePaint); // draw vertical columns
        }
        
        setAvailableMoves(canvas, mSelectedCell, mBoardSpaceValue, mBoardSpaceAvailable);
        
        boolean prizeDrawn = false;
        if (GameActivity.getMoveModeTouch()) {
        	if (mSelectedCell > -1) {
        		int xValue = mSelectedCell % 5;
        		int yValue = calculateYValue(mSelectedCell);
    			mDstRect.offsetTo(MARGIN + mOffsetX + mSxy * (xValue), MARGIN + mOffsetY + mSxy * yValue);
        		if (mBlinkDisplayOff) {
        			canvas.drawBitmap(mBmpAvailableMove, mTakenRect, mDstRect, null);
        		} else { 
        			canvas.drawBitmap(mBmpTakenMove, mTakenRect, mDstRect, null);
        		}
        	}
        }
        
        mDstRect.offsetTo(MARGIN + mOffsetX + mSxy * 2, MARGIN + mOffsetY + mSxy * 2);
        tokenToDraw = getTokenToDrawCenter();
        canvas.drawBitmap(tokenToDraw, mSrcRect, mDstRect, mBmpPaint);
        
        if (mPrizeLocation > -1 && !prizeDrawn) {
        	mDstRect.offsetTo(MARGIN + mOffsetX + mSxy * mPrizeXBoardLocation, MARGIN + mOffsetY + mSxy * mPrizeYBoardLocation); 
        	canvas.drawBitmap(mBmpPrize, mSrcRect, mDstRect, mBmpPaint);
        }
        
        for (int j = 0, k = 0, y = y7; j < 5; j++, y += mSxy) {
            for (int i = 0, x = x7; i < 5; i++, k++, x += mSxy) {
                mDstRect.offsetTo(MARGIN + x, MARGIN + y);

                State v;
                if (mSelectedCell == k) {
                    if (mBlinkDisplayOff) {
                        continue;
                    }
                    v = mSelectedValue;
                } else {
                    v = mData[k];
                }

                //TODO - add some logic to handle mBmpCross, mBmpCircle and mBmpTakenMove == null
//                if (HUMAN_VS_HUMAN) {
                if (HUMAN_VS_HUMAN && (v == State.PLAYER1 || v == State.PLAYER2)) {
                	if (mSelectedCell == k && mBoardSpaceAvailable[k]) {
                		//this is required to allow blinking
            			canvas.drawBitmap(mBmpAvailableMove, mTakenRect, mDstRect, null);     
                	} else {
                		if (k != BoardSpaceValues.BOARDCENTER) {
                			drawPlayerToken(canvas, k);    
                		}
                	}
                } else {
                	switch(v) {
                	case PLAYER1:
                		if (mSelectedCell == k && mBoardSpaceAvailable[k]) {
                			//this is required to allow blinking
                			canvas.drawBitmap(mBmpAvailableMove, mTakenRect, mDstRect, null);     
                		} else { 
                			drawPlayerToken(canvas, k);
                		}
                		break;
                	case PLAYER2:
                		drawPlayerToken(canvas, k);
                		break;
                	}
                }
            }
        }

        if (mWinRow >= 0) {
            int y = y7 + mWinRow * mSxy + mSxy / 2;
            canvas.drawLine(x7 + MARGIN, y, x7 + s3 - 1 - MARGIN, y, mWinPaint);

        } else if (mWinCol >= 0) {
            int x = x7 + mWinCol * mSxy + mSxy / 2;
            canvas.drawLine(x, y7 + MARGIN, x, y7 + s3 - 1 - MARGIN, mWinPaint);

        } else if (mWinDiag == 0) {
            // diagonal 0 is from (0,0) to (2,2)
            canvas.drawLine(x7 + MARGIN, y7 + MARGIN,
                    x7 + s3 - 1 - MARGIN, y7 + s3 - 1 - MARGIN, mWinPaint);
        } else if (mWinDiag == 2) {
            // diagonal 0 is from (0,0) to (2,2)
            canvas.drawLine(x7 + MARGIN + mSxy, y7 + MARGIN,
                    x7 + s3 - 1 - MARGIN, y7 + s3 - 1 - MARGIN - mSxy, mWinPaint);
        } else if (mWinDiag == 3) {
            canvas.drawLine(x7 + MARGIN + 2 * mSxy, y7 + MARGIN,
                    x7 + s3 - 1 - MARGIN, y7 + s3 - 1 - MARGIN - 2 * mSxy, mWinPaint);
        } else if (mWinDiag == 5) {
            canvas.drawLine(x7 + MARGIN, y7 + MARGIN + 2 * mSxy,
                    x7 + s3 - 1 - MARGIN  - 2 * mSxy, y7 + s3 - 1 - MARGIN, mWinPaint);
        } else if (mWinDiag == 4) {
            canvas.drawLine(x7 + MARGIN, y7 + MARGIN + mSxy,
                    x7 + s3 - 1 - MARGIN  - mSxy, y7 + s3 - 1 - MARGIN, mWinPaint);
        } else if (mWinDiag == 6) {
            // diagonal 6 is from (0,3) to (15,0)
            canvas.drawLine(x7 + MARGIN  - 0 * mSxy, y7 + s3 - 1 - MARGIN - 1 * mSxy,
                    x7 + s3 + 2 - MARGIN - 1 * mSxy, y7 + MARGIN, mWinPaint);
        } else if (mWinDiag == 7) {
            // diagonal 7 is from (0,4) to (20,0)
            canvas.drawLine(x7 + MARGIN, y7 + s3 - 1 - MARGIN,
                    x7 + s3 - 1 - MARGIN, y7 + MARGIN, mWinPaint);
        } else if (mWinDiag == 1) {
            // diagonal 1 is from (0,2) to (10,0)
            canvas.drawLine(x7 + MARGIN  - 0 * mSxy, y7 + s3 - 1 - MARGIN - 2 * mSxy,
                    x7 + s3 + 2 - MARGIN - 2 * mSxy, y7 + MARGIN, mWinPaint);
        } else if (mWinDiag == 8) {
        // diagonal 8 is from (1,4) to (4,1)
        	canvas.drawLine(x7 + MARGIN  + 1 * mSxy, y7 + s3 - 1 - MARGIN - 0 * mSxy,
                x7 + s3 + 2 - MARGIN + 0 * mSxy, y7 + MARGIN  + 1 * mSxy, mWinPaint);
        } else if (mWinDiag == 9) {
            // diagonal 9 is from (2,4) to (4,2)
            canvas.drawLine(x7 + MARGIN  + 2 * mSxy, y7 + s3 - 1 - MARGIN - 0 * mSxy,
                    x7 + s3 + 2 - MARGIN + 0 * mSxy, y7 + MARGIN  + 2 * mSxy, mWinPaint);
        }

    	//draw the balls on the canvas
        if (GameActivity.getMoveModeTouch()) {
            for (int x = 0; x < mColorBall.length; x++) {
        		if (!mColorBall[x].isDisabled()) {
        			
            		if (mBallId == x && mBlinkDisplayOff) 
            			continue;
        			
        			mDstRect.offsetTo(mColorBall[x].getCoordX(), mColorBall[x].getCoordY());
        			canvas.drawBitmap(mColorBall[x].getBitmap(), mSrcRect, mDstRect, mBmpPaint);
        		}
            }
        } else {
        	for (ColorBall ball : mColorBall) {
        		if (!ball.isDisabled()) {
        			mDstRect.offsetTo(ball.getCoordX(), ball.getCoordY());
        			canvas.drawBitmap(ball.getBitmap(), mSrcRect, mDstRect, mBmpPaint);
        		}
    		}
        }
    }
    
    public int getFirstAvailableSpace() {
    	int x = 0;
    	 for (x = 0; x < mBoardSpaceAvailable.length; x++) {
    		 if (mBoardSpaceAvailable[x] == true)
    			 return x;
    	 }
    	 return -1;
    }

    private int calculateLeftLimit(int[] boardSpaceValue) {
    	for (int x = 4; x < 25; x+=5)
    		if (boardSpaceValue[x] !=  BoardSpaceValues.EMPTY) {
    			return 2;
    		}
    	for (int x = 3; x < 24; x+=5)
    		if (boardSpaceValue[x] !=  BoardSpaceValues.EMPTY) {
    			return 1;
    		}
    	return 0;
    }

    private int calculateRightLimit(int[] boardSpaceValue) {
    	for (int x = 0; x < 21; x+=5)
    		if (boardSpaceValue[x] !=  BoardSpaceValues.EMPTY) {
    			return 2;
    		}
    	for (int x = 1; x < 22; x+=5)
    		if (boardSpaceValue[x] !=  BoardSpaceValues.EMPTY) {
    			return 3;
    		}
    	return 4;
    }

    private int calculateTopLimit(int[] boardSpaceValue) {
    	for (int x = 20; x < 25; x++)
    		if (boardSpaceValue[x] !=  BoardSpaceValues.EMPTY) {
    			return 2;
    		}
    	for (int x = 15; x < 20; x++)
    		if (boardSpaceValue[x] !=  BoardSpaceValues.EMPTY) {
    			return 1;
    		}
    	return 0;
    }
    
    private int calculateBottomLimit(int[] boardSpaceValue) {
    	for (int x = 0; x < 5; x++)
    		if (boardSpaceValue[x] !=  BoardSpaceValues.EMPTY) {
    			return 2;
    		}
    	for (int x = 5; x < 10; x++)
    		if (boardSpaceValue[x] !=  BoardSpaceValues.EMPTY) {
    			return 3;
    		}
    	return 4;
    }

    private void resetAvailableMoves(Canvas canvas, boolean[] boardSpaceAvailable, int selectedCell) {
    	for (int x = 0; x < boardSpaceAvailable.length; x++) {
    		int xValue = x % 5;
    		int yValue = calculateYValue(x);
    		if (selectedCell == x)
    			continue;
    		boardSpaceAvailable[x] = SPACENOTAVAILABLE;
    		if (canvas != null) {
    			mDstRect.offsetTo(MARGIN + mOffsetX + mSxy * xValue, MARGIN + mOffsetY + mSxy * yValue);
    			canvas.drawBitmap(mBmpTakenMove, mTakenRect, mDstRect, null);
    		} 
    	}
    }

    public void setAvailableMoves(int selectedCell, int[] boardSpaceValue, 
    		boolean[] boardSpaceAvailable) {
    	setAvailableMoves(null, selectedCell, boardSpaceValue, boardSpaceAvailable);
    }
    
//	if the position checked is occupied determine if left, right, up and down exist
//	for each direction that exists check if its already occupied
//	if its not, then its available
    private void setAvailableMoves(Canvas canvas, int selectedCell, int[] boardSpaceValue, 
    		boolean[] boardSpaceAvailable) {
    	resetAvailableMoves(canvas, boardSpaceAvailable, selectedCell);
    	int leftLimit = calculateLeftLimit(boardSpaceValue);
    	int rightLimit = calculateRightLimit(boardSpaceValue);
    	int topLimit = calculateTopLimit(boardSpaceValue);
    	int bottomLimit = calculateBottomLimit(boardSpaceValue);
//    	calculate leftLimit, rightLimit, topLimit and bottomLimit
//    	if xValue < leftLimit then position is not available, similarly for the remaining directions
    	
    	for (int x = 0; x < boardSpaceValue.length; x++) {
    		boolean leftExists = false;
    		boolean rightExists = false;
    		boolean upExists = false;
    		boolean downExists = false;
    		if (boardSpaceValue[x] == -1)
    			continue;
    		if (x % 5 > 0)
    			leftExists = true;
    		if (x != 4 && x != 9 && x != 14 && x != 19 && x != 24)
    			rightExists = true;
    		if (x > 4)
    			upExists = true;
    		if (x < 20)
    			downExists = true;
    		
    		int xValue = x % 5;
    		int yValue = calculateYValue(x);
    		
    		if (leftExists) {
    			if (xValue > leftLimit)
    				if (boardSpaceValue[x - 1] == -1 && selectedCell != x - 1) {
    					if (canvas != null) {
    						drawAvailableSquare(canvas, xValue - 1, yValue);
//    						mDstRect.offsetTo(MARGIN + mOffsetX + mSxy * (xValue - 1), MARGIN + mOffsetY + mSxy * yValue);
//    						canvas.drawBitmap(mBmpAvailableMove, mTakenRect, mDstRect, null);
    					}
    					boardSpaceAvailable[x - 1] = true;
    				}
    		}
    		
    		if (rightExists) {
    			if (xValue < rightLimit)
    				if (boardSpaceValue[x + 1] == -1 && selectedCell != x + 1) {
    					if (canvas != null) {
    						drawAvailableSquare(canvas, xValue + 1, yValue);
//    						mDstRect.offsetTo(MARGIN + mOffsetX + mSxy * (xValue + 1), MARGIN + mOffsetY + mSxy * yValue);    						
//    						canvas.drawBitmap(mBmpAvailableMove, mTakenRect, mDstRect, null);
    					}
    					boardSpaceAvailable[x + 1] = true;
    				}
    		}
    		
    		if (upExists) {
    			if (yValue > topLimit) {
    				if (boardSpaceValue[x - 5] == -1 && selectedCell != x - 5) {
    					if (canvas != null) {
    						drawAvailableSquare(canvas, xValue, yValue - 1);
//    						mDstRect.offsetTo(MARGIN + mOffsetX + mSxy * xValue, MARGIN + mOffsetY + mSxy * (yValue - 1));    						
//    		    			canvas.drawBitmap(mBmpAvailableMove, mTakenRect, mDstRect, null);
    					}
    					boardSpaceAvailable[x - 5] = true;
    				}
    			}
    		}

    		if (downExists) {
    			if (yValue < bottomLimit) {
    				if (boardSpaceValue[x + 5] == -1 && selectedCell != x + 5) {
    					if (canvas != null) {
    						drawAvailableSquare(canvas, xValue, yValue + 1);
//    						mDstRect.offsetTo(MARGIN + mOffsetX + mSxy * xValue, MARGIN + mOffsetY + mSxy * (yValue + 1));    						
//    		    			canvas.drawBitmap(mBmpAvailableMove, mTakenRect, mDstRect, null);
    					}
    					boardSpaceAvailable[x + 5] = true;
    				}
    			}
    		}
    	}
    }
    
    private void drawAvailableSquare(Canvas canvas, int xValue, int yValue) {
		mDstRect.offsetTo(MARGIN + mOffsetX + mSxy * xValue, MARGIN + mOffsetY + mSxy * yValue);
		canvas.drawBitmap(mBmpAvailableMove, mTakenRect, mDstRect, null);
    	
//		mDstRect.offsetTo(MARGIN + mOffsetX + mSxy * (xValue + 1), MARGIN + mOffsetY + mSxy * yValue);    						
//		canvas.drawBitmap(mBmpAvailableMove, mTakenRect, mDstRect, null);
//		
//		mDstRect.offsetTo(MARGIN + mOffsetX + mSxy * xValue, MARGIN + mOffsetY + mSxy * (yValue - 1));    						
//		canvas.drawBitmap(mBmpAvailableMove, mTakenRect, mDstRect, null);
//		
//		mDstRect.offsetTo(MARGIN + mOffsetX + mSxy * xValue, MARGIN + mOffsetY + mSxy * (yValue + 1));    						
//		canvas.drawBitmap(mBmpAvailableMove, mTakenRect, mDstRect, null);
    }

    //calculate the Y offset given the cell position on the game board
    private int calculateYValue(int cellNumber) {
    	int yValue = 0;
		if (cellNumber < 5)
			yValue = 0;
			else if (cellNumber < 10)
				yValue = 1;
			else if (cellNumber < 15)
				yValue = 2;
			else if (cellNumber < 20)
				yValue = 3;
			else yValue = 4;
		return yValue;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) { 
        // Keep the view squared
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        int d = w == 0 ? h : h == 0 ? w : w < h ? w : h;
//        setMeasuredDimension(d, d);
        
        int modeW = MeasureSpec.getMode(widthMeasureSpec);
        int modeH = MeasureSpec.getMode(heightMeasureSpec);
        
       	if (modeH == MeasureSpec.EXACTLY &&  modeW == MeasureSpec.EXACTLY) {
       		mDisplayMode = ScreenOrientation.LANDSCAPE; 
       		int screenBoardSize = w < h ? w : h;
       		
       		if (screenBoardSize < 400) { //LG G1 screenBoardSize = 320
       			double result = mTokenSize * 3 / 150.0 * 40.0; 
       			TOKENSIZE = (int)result;
       			mOffsetY = 20; // offset of board grid from top of screen
       			landscapeIncrementYPlayer = 50;
       			mTokenRadius = 40;
       		} else if (screenBoardSize < 500) { //WVGA800 screenBoardSize = 442
       			double result = mTokenSize * 3 / 150.0 * 50.0;
       			TOKENSIZE = (int)result;
       			mOffsetY = 40; 
       			landscapeIncrementYPlayer = 80;
       			mTokenRadius = 45;
       		} else if (screenBoardSize < 800) {  // on my Galaxy Note screenBoardSize = 720, on my Nexus 6 screenBoardSize = 661
       			double result = mTokenSize * 3 / 150.0 * 90.0;
       			TOKENSIZE = (int)result;
       			mOffsetY = 60; //was 120
       			landscapeIncrementYPlayer = 100;
       			mTokenRadius = 55;
       		} else { // my LG V10
                double result = mTokenSize * 3 / 150.0 * 130.0;
                TOKENSIZE = (int)result;
                mOffsetY = 80;
                landscapeIncrementYPlayer = TOKENSIZE;// was 130;
                double workRadius = TOKENSIZE * .75;
                mTokenRadius = (int)workRadius; // was hard coded at 65 then changed to TOKENSIZE / 2
            }
       		
        	mSxy = TOKENSIZE + 2;
        	//mOffsetX = (w - (landscapeIncrementYPlayer * 4))/2;
            mOffsetX = (w - (landscapeIncrementYPlayer * 5))/2;
   			
//        	BoardLowerLimit = TOKENSIZE / 2;
        	BoardLowerLimit = mOffsetY + mSxy * 5;        	
//            mOffsetY = mOffsetXY; // 5;
            //h = LANDSCAPEHEIGHT; // 222;
            
            //landscapeComputerTokenSelectedOffsetX = w - landscapeHumanTokenSelectedOffsetX - mSxy;
            
            int playingBoardWidth = (mSxy + GRIDLINEWIDTH) * 5;
            landscapeRightMoveXLimitPlayer1 = w / 2 + playingBoardWidth / 2;
            landscapeLeftMoveXLimitPlayer2 = w / 2 - playingBoardWidth / 2;   
            
            landscapeRightMoveXLimitPlayer2 =  w - mSxy;
            landscapeLeftMoveXLimitPlayer1 = mSxy * 2;
            
            landscapeStartingXPlayer2 = w - landscapeHumanTokenSelectedOffsetX - mSxy;
            mDstRect.set(MARGIN, MARGIN, mSxy - MARGIN - 1, mSxy - MARGIN - 1);
       		
            setMeasuredDimension(w, h);
       	} else if (modeH == MeasureSpec.EXACTLY) {
       		mDisplayMode = ScreenOrientation.PORTRAIT; 
       		mOffsetX = 0;
       		mOffsetX = 0;
            
            mOffsetX = PORTRAITOFFSETX; // 5;
            mOffsetY = PORTRAITOFFSETY; // 5;
            d = PORTRAITWIDTHHEIGHT; // 300;
            
            mDstRect.set(MARGIN, MARGIN, mSxy - MARGIN - 1, mSxy - MARGIN - 1);
            
        	setMeasuredDimension(d, d);
       	} else {
       		setMeasuredDimension(d, d);
       	}
       	
       	if (!INITIALIZATIONCOMPLETED) {  
       		initializeBallPositions();
       		initializePlayerTokens(mContext);
       		INITIALIZATIONCOMPLETED  = true;
       	}
        resetUnusedTokens();
    }
    
    protected void onSizeChangedOrig(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        int boardNumber = 7;
        if (mDisplayMode == ScreenOrientation.LANDSCAPE) {
        	boardNumber = 5;
        }
        
        int sx = (w - 2 * MARGIN) / boardNumber; //5 // was 3 in original Tic Tac Toe game
        int sy = (h - 2 * MARGIN) / boardNumber; //5 // was 3 in original Tic Tac Toe game

        int size = sx < sy ? sx : sy;

        mSxy = size;
        if (mDisplayMode == ScreenOrientation.LANDSCAPE)
        	mOffsetX = ((w - boardNumber * size) / 2) + 20; // was w - 3 in original Tic Tac Toe game 
        else
        	mOffsetX = (w - boardNumber * size) / 2; // was w - 3 in original Tic Tac Toe game 
        mOffsetY = (h - boardNumber * size) / 2; // was h - 3 in original Tic Tac Toe game 
        
//        mDstRect.set(MARGIN, MARGIN, size - MARGIN, size - MARGIN);
        mDstRect.set(MARGIN, MARGIN, size - MARGIN - 1, size - MARGIN - 1);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	if (mViewDisabled)
    		return false;
    	
        if (GameActivity.getMoveModeTouch())
        	return moveModeTouch(event);
    	
    	//mBallId = -1; //nope, can't do that here, it won't allow you to move any token
    	
        int action = event.getAction();
        int X = (int)event.getX();
        int Y = (int)event.getY();
        
        int ballStart = 0;
        int ballLimit = 4;
        int rightLimit = landscapeRightMoveXLimitPlayer1; // default to 1 player landscape
        int leftLimit = landscapeLeftMoveXLimitPlayer1; // default to 1 player landscape
        
        if (HUMAN_VS_HUMAN) { 
        	if (mCurrentPlayer == State.PLAYER2) {
                ballStart = 4;
                ballLimit = 8;
                if (mDisplayMode == ScreenOrientation.LANDSCAPE) {
                	rightLimit = landscapeRightMoveXLimitPlayer2;
                	leftLimit = landscapeLeftMoveXLimitPlayer2;   
                } else {
                	rightLimit = portraitComputerLiteralOffset;
                	leftLimit = portraitHumanTokenSelectedOffsetX;   
                }
        	}
        } else
        	if (mDisplayMode == ScreenOrientation.PORTRAIT) { // 1 player portrait 
        		rightLimit = portraitComputerLiteralOffset;
        		leftLimit = portraitHumanTokenSelectedOffsetX;
        }
        
        if (action == MotionEvent.ACTION_DOWN) {
        	// touch down so check if the finger is on a ball
        	mBallId = -1;
        	for (int x = ballStart; x < ballLimit; x++) {
        		ColorBall ball = mColorBall[x];
        		if (ball.isDisabled())
        			continue;
        		// check if inside the bounds of the ball (circle)
        		// get the center for the ball
        		int centerX = ball.getCoordX() + 25;
        		int centerY = ball.getCoordY() + 25;
        		// calculate the radius from the touch to the center of the ball
        		double radCircle  = Math.sqrt( (double) (((centerX-X)*(centerX-X)) + (centerY-Y)*(centerY-Y)));
        		// if the radius is smaller then 23 (radius of a ball is 22), then it must be on the ball
//        		if (radCircle < 40) { // was 23
           		if (radCircle < mTokenRadius) { // was 23 
        			mBallId = ball.getID(); 
                    break;
        		}
        		// check all the bounds of the ball (square)
        		//if (X > ball.getCoordX() && X < ball.getCoordX()+50 && Y > ball.getCoordY() && Y < ball.getCoordY()+50){
                //	balID = ball.getID();
                //	break;
                //}
              }
        	invalidate();
            return true;
        } else if (action == MotionEvent.ACTION_MOVE) {  
            X = (int)event.getX();
            Y = (int)event.getY();
        	// move the balls the same as the finger
            
            if (mBallId > -1) {
               	if (X > leftLimit && X < rightLimit)
            		mColorBall[mBallId].setCoordX(X - 25);
            	if (Y > 25 && Y < BoardLowerLimit)
            		mColorBall[mBallId].setCoordY(Y - 25);
            }
            invalidate();
        	return true;
        } else if (action == MotionEvent.ACTION_UP) {
        	X = (int) event.getX();
        	Y = (int) event.getY();
            
        	int xPos = (X - mOffsetX) / mSxy;
        	int yPos = (Y - mOffsetY) / mSxy;            

            if (xPos >= 0 && xPos < 5 && yPos >= 0 & yPos < 5) {
                int cell = xPos + 5 * yPos;

                State state = cell == mSelectedCell ? mSelectedValue : mData[cell];
                
                writeToLog("ClientService", "xPos: "+xPos+ " yPos: "+yPos+" calculated cell: "+cell);
                writeToLog("ClientService", "state: "+state+ " mSelectedCell: "+mSelectedCell+" mSelectedValue: "+mSelectedValue+" mData[cell]: "+mData[cell]);
                
                //state = state == State.EMPTY ? mCurrentPlayer : State.EMPTY;
                if (state == State.EMPTY)
                	state = mCurrentPlayer;

                //stopBlink(); // not sure why they were stopping it here

//                mSelectedCell = cell;
//                mSelectedValue = state;
                //mBlinkDisplayOff = false; //this value is turned off in stopBlink()
//                mBlinkRect.set(MARGIN + mOffsetX + xPos * sxy, MARGIN + yPos * sxy,
//                               MARGIN + mOffsetX + (xPos + 1) * sxy, MARGIN + (yPos + 1) * sxy);
                
                
                writeToLog("ClientService", "cell calculated: "+cell);
                
                if (mBallId > -1) {
                	if (mBoardSpaceAvailable[cell] == true) { 
                		mDstRect.offsetTo(MARGIN + mOffsetX + mSxy * xPos, MARGIN + mOffsetY + mSxy * yPos);
                		
                		mColorBall[mBallId].setCoordX(MARGIN + mOffsetX + mSxy * xPos);
                		mColorBall[mBallId].setCoordY(MARGIN + mOffsetY + mSxy * yPos);
                    	
                        mBlinkRect.set(MARGIN + mOffsetX + xPos * mSxy, 
                        			   MARGIN + mOffsetY + yPos * mSxy,
                        			   MARGIN + mOffsetX + (xPos + 1) * mSxy, 
                        			   MARGIN + mOffsetY + (yPos + 1) * mSxy);
                        
                        mSelectedCell = cell;
                        mSelectedValue = state;
                        
                        writeToLog("ClientService", "ball id: "+mBallId+ " cell calculated: "+cell);
                	} 
                	else {
                		mBallId = -1;
                		stopBlink();
                		writeToLog("ClientService", "ball id: "+mBallId+ " cell calculated: "+cell+" space not available");
//                		    mSelectedValue = State.EMPTY; 
//                		    mCellListener.onCellSelected();
//                		    mSelectedCell = -1;
                	}
                }
                
                if (mBallId > -1 && !mColorBall[mBallId].isDisabled() && state != State.EMPTY) {
                    // Start the blinker
                    mHandler.sendEmptyMessageDelayed(MSG_BLINK, FPS_MS);
                    writeToLog("ClientService", "blinker started");
                }

                if (mCellListener != null) {
                	writeToLog("ClientService", "onCellSelected called for ballId: "+mBallId);
                	if (mBallId > -1)
                		mCellListener.onCellSelected(); //activates / de-activates next button 
                	else {
                		mSelectedCell = -1;
                		mCellListener.onCellSelected();
                	}
                }
                
            } else {
            	mBallId = -1;
            	mBlinkRect.setEmpty();
            	mSelectedCell = -1;
//            	mSelectedValue = State.EMPTY; 
            	mCellListener.onCellSelected();
            	writeToLog("ClientService", "outside of board selected");
            }
            
            for (int x = ballStart; x < ballLimit; x++) { // only reset 1 side token positions
            	ColorBall ball = mColorBall[x];
        		if (ball.isDisabled()) {
        			continue;
        		}
        		if (mBallId == ball.getID()) {
        			continue;
        		}
        		writeToLog("ClientService", "ball reset: "+x);
        		ball.resetPosition(mDisplayMode);
        	}
            invalidate();
            return true;
        }
        return false;
    }
    
    
    private boolean moveModeTouch(MotionEvent event) {
        int action = event.getAction();
        int X = (int)event.getX();
        int Y = (int)event.getY();
        
        int ballStart = 0;
        int ballLimit = 4;
        
        if (HUMAN_VS_HUMAN && mCurrentPlayer == State.PLAYER2) {
        	ballStart = 4;
            ballLimit = 8;
        } 
     // if token is blinking and we've selected another valid token then turn blinking off original token and blink new token
        // if nothing is blinking then make either a valid selected token or a valid selected move to position blink
        // if position is blinking and we've selected a valid token then move selected token to blinking position
        // if token is blinking and we've selected a valid position then move blinking selected token to valid position
        
        // if position is blinking and we've selected another valid position then turn blinking off original position and blink new position
        // if blink position has been filled with a selected token and user selects another token then swap tokens in blinking position
        // if blink position has been filled with a selected token and user selects another valid position then swap positions with same token
        // if anything else is touched then don't change anything that is currently blinking (selected)
        
        if (action == MotionEvent.ACTION_DOWN) {
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
        	// touch up so check if the finger is on a ball
        	// if we've touched a valid ball or a valid square on the board then make it blink
        	//mBallId = -1;
        	for (int x = ballStart; x < ballLimit; x++) {
        		ColorBall ball = mColorBall[x];
        		if (ball.isDisabled()) {
        			continue;
        		}
        		// check if inside the bounds of the ball (circle)
        		// get the center for the ball
        		int centerX = ball.getCoordX() + 25;
        		int centerY = ball.getCoordY() + 25;
        		// calculate the radius from the touch to the center of the ball
        		double radCircle  = Math.sqrt( (double) (((centerX-X)*(centerX-X)) + (centerY-Y)*(centerY-Y)));
        		// if the radius is smaller then 23 (radius of a ball is 22), then it must be on the ball
//        		if (radCircle < 40){ // was 23 xx
               	if (radCircle < mTokenRadius) { // was 23 
        			mBallId = ball.getID(); 
                    break;
        		}
        	}

            for (int x = ballStart; x < ballLimit; x++) { // only reset 1 side token positions
            	ColorBall ball = mColorBall[x];
        		if (ball.isDisabled()) {
        			continue;
        		}
        		if (mBallId == ball.getID()) {
        			continue;
        		}
        		writeToLog("ClientService", "ball reset: "+x);
        		ball.resetPosition(mDisplayMode);
        	}
        	
        	int xPos = (X - mOffsetX) / mSxy;
        	int yPos = (Y - mOffsetY) / mSxy;

        	if (xPos >= 0 && xPos < 5 && yPos >= 0 & yPos < 5) {
        		int cell = xPos + 5 * yPos;

        		State state = cell == mSelectedCell ? mSelectedValue : mData[cell];
              
        		writeToLog("ClientService", "xPos: "+xPos+ " yPos: "+yPos+" calculated cell: "+cell);
        		writeToLog("ClientService", "state: "+state+ " mSelectedCell: "+mSelectedCell+" mSelectedValue: "+mSelectedValue+" mData[cell]: "+mData[cell]);
              
        		if (state == State.EMPTY) {
        			state = mCurrentPlayer;
        		}
              
        		writeToLog("ClientService", "cell calculated: "+cell);
              
        		if (mBallId > -1) {
        			if (mBoardSpaceAvailable[cell] == true) { 
        				mDstRect.offsetTo(MARGIN + mOffsetX + mSxy * xPos, MARGIN + mOffsetY + mSxy * yPos);
        				mColorBall[mBallId].setCoordX(MARGIN + mOffsetX + mSxy * xPos);
        				mColorBall[mBallId].setCoordY(MARGIN + mOffsetY + mSxy * yPos);
        				stopBlinkTouchMode();
        				mBlinkRect.set(MARGIN + mOffsetX + xPos * mSxy, 
        						       MARGIN + mOffsetY + yPos * mSxy,
        						       MARGIN + mOffsetX + (xPos + 1) * mSxy, 
        						       MARGIN + mOffsetY + (yPos + 1) * mSxy);
                      
        				mSelectedCell = cell;
        				mSelectedValue = state;
        				
        				writeToLog("ClientService", "ball id: "+mBallId+ " cell calculated: "+cell);
                		mHandler.sendEmptyMessageDelayed(MSG_BLINK, FPS_MS);
                		
                		if (!(mPrevSelectedBall == mBallId && mPrevSelectedCell == mSelectedCell)) {
                			mCellListener.onCellSelected();
                			mPrevSelectedBall = mBallId;
                			mPrevSelectedCell = mSelectedCell;
                			invalidate();
                		}
        				return true;
        			}
        		}
            //if we've touched a valid square on the board then make it blink
        		if (mBoardSpaceAvailable[cell] == true) {
        			stopBlinkTouchMode();
        			mBlinkRect.set(MARGIN + mOffsetX + xPos * mSxy, 
        					       MARGIN + mOffsetY + yPos * mSxy,
    					           MARGIN + mOffsetX + (xPos + 1) * mSxy, 
    					           MARGIN + mOffsetY + (yPos + 1) * mSxy);
        			mHandler.sendEmptyMessageDelayed(MSG_BLINK_SQUARE, FPS_MS);
        			mSelectedCell = cell;
        			invalidate();
        			return true;
        		}
        	}
        	
        	if (mBallId > -1 && mSelectedCell > -1) {
        		int xValue = mSelectedCell % 5;
        		int yValue = calculateYValue(mSelectedCell);
				mColorBall[mBallId].setCoordX(MARGIN + mOffsetX + mSxy * xValue);
				mColorBall[mBallId].setCoordY(MARGIN + mOffsetY + mSxy * yValue);
				mSelectedValue = mCurrentPlayer;
				stopBlinkTouchMode();
				mBlinkRect.set(MARGIN + mOffsetX + xValue * mSxy, 
							   MARGIN + mOffsetY + yValue * mSxy,
							   MARGIN + mOffsetX + (xValue + 1) * mSxy, 
							   MARGIN + mOffsetY + (yValue + 1) * mSxy);
        		mHandler.sendEmptyMessageDelayed(MSG_BLINK, FPS_MS);
        		if (!(mPrevSelectedBall == mBallId && mPrevSelectedCell == mSelectedCell)) {
        			mCellListener.onCellSelected();
        			mPrevSelectedBall = mBallId;
        			mPrevSelectedCell = mSelectedCell;
        			invalidate();
        		}
				return true;
        	}

        	if (mBallId > -1) {
        		Rect rect = mColorBall[mBallId].getRect();
        		stopBlinkTouchMode();
        		mBlinkRect.set(rect.left, rect.top, rect.left + TOKENSIZE, rect.top + TOKENSIZE);
        		mHandler.sendEmptyMessageDelayed(MSG_BLINK_TOKEN, FPS_MS);
        		invalidate();
        	}
        }
    	return false;
    }

    public int selectSpecificComputerToken(int type, boolean offense) {
    	for (int x = 0; x < 4; x++ ) {
    		if (offense) {
    			if ((mColorBall[computerMove+x].getType() == type || mColorBall[computerMove+x].getType() == BoardSpaceValues.CIRCLECROSS)
   					&& !mColorBall[computerMove+x].isDisabled()) 
    				return computerMove+x;
    			} else {
        			if ((mColorBall[computerMove+x].getType() == type) 
           				&& !mColorBall[computerMove+x].isDisabled()) 
            				return computerMove+x;
        		}
   			}
   		return -1;
    }
    
    public int selectSpecificHumanToken(int type) {
    	for (int x = 0; x < 4; x++ ) {
    		if ((mColorBall[x].getType() == type || mColorBall[x].getType() == BoardSpaceValues.CIRCLECROSS) && !mColorBall[x].isDisabled()) 
    			return x;
    	}
    	return -1;
    }
    
    public int selectLastComputerToken() {
    	for (int x = 0; x < 4; x++ ) {
    		if (mColorBall[computerMove+x].isDisabled()) {
    			continue;
    		}
    		return computerMove+x;
    	}
    	return -1;
    }

    public int selectLastHumanToken() {
    	for (int x = 0; x < 4; x++ ) {
    		if (mColorBall[x].isDisabled()) {
    			continue;
    		}
    		return x;
    	}
    	return -1;
    }
    
    public int selectRandomComputerToken() {
    	int randomNumber = mRandom.nextInt(4);
    	
    	int tokenAvailable = 0;
    	int totalAvailable = 0;
    	for (int x = 4; x < 8; x++) {
    		if (mColorBall[x].isDisabled()) {
    			continue;
    		}
    		tokenAvailable = x;
    		totalAvailable++;
    	}
    	
    	if (totalAvailable == 1) {
    		return tokenAvailable;
    	}
    	
    	for (int x = 0; x < 4; x++ ) {
    		if (mColorBall[computerMove+randomNumber].isDisabled() || mColorBall[computerMove+randomNumber].getType() == BoardSpaceValues.CIRCLECROSS) {
    			randomNumber++;
    		}
    		if (randomNumber > 3) {
    			randomNumber = 0;
    			continue;
    		}
//    		if (mColorBall[computerMove+randomNumber].getType() == BoardSpaceValues.CIRCLECROSS) {
//    			randomNumber++;
//    			continue;
//    		}
//    		if (mColorBall[computerMove+randomNumber].isDisabled()) {
//    			randomNumber++;
//    			continue;
//    		}
    		break;
    	}
//		colorBall[computerMove+randomNumber].setDisabled(true);
		return computerMove+randomNumber;
    }
    
    public int selectRandomAvailableBoardSpace() {
    	
//    	build array of avail cells
    	int numberAvailable = 0;
    	for (int x = 0; x < mBoardSpaceAvailable.length; x++) {
    		if (mBoardSpaceAvailable[x] == true)
    			numberAvailable++;
    	}
    	int [] boardCellsAvailable = new int[numberAvailable];
    	int boardCellNumber = 0; 
    	for (int x = 0; x < mBoardSpaceAvailable.length; x++) {
    		if (mBoardSpaceAvailable[x] == true)
    			boardCellsAvailable[boardCellNumber++] = x;
    	}
    	
    	int randomNumber = mRandom.nextInt(boardCellsAvailable.length);
    	return boardCellsAvailable[randomNumber];
    }
    
    public int moveComputerToken(int boardLocation, int ballSelected) {
    	setBoardSpaceValue(boardLocation, mColorBall[ballSelected].getType());
    	return mColorBall[ballSelected].getType();     	
    }
    
    public void stopBlink() {
        boolean hadSelection = mSelectedCell != -1 && mSelectedValue != State.EMPTY;
        mSelectedCell = -1;
        mSelectedValue = State.EMPTY;
        if (!mBlinkRect.isEmpty()) {
            invalidate(mBlinkRect);
        }
        mBlinkDisplayOff = false;
        mBlinkRect.setEmpty();
        mHandler.removeMessages(MSG_BLINK);
        mHandler.removeMessages(MSG_BLINK_TOKEN);
        mHandler.removeMessages(MSG_BLINK_SQUARE);        
        if (hadSelection && mCellListener != null) {
            mCellListener.onCellSelected(); //enables I'm done button
        }
    }
    
    private void stopBlinkTouchMode() {
        mBlinkDisplayOff = false;
        mBlinkRect.setEmpty();
        mHandler.removeMessages(MSG_BLINK);
        mHandler.removeMessages(MSG_BLINK_TOKEN);
        mHandler.removeMessages(MSG_BLINK_SQUARE);        
    }

    private class MyHandler implements Callback {
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_BLINK) {
                if (mSelectedCell >= 0 && mSelectedValue != State.EMPTY) {
            	
                    mBlinkDisplayOff = !mBlinkDisplayOff;
                    invalidate(mBlinkRect);

                    if (!mHandler.hasMessages(MSG_BLINK)) {
                        mHandler.sendEmptyMessageDelayed(MSG_BLINK, FPS_MS);
                    }
                }
                return true;
            }
            
            if (msg.what == MSG_BLINK_TOKEN) {
                mBlinkDisplayOff = !mBlinkDisplayOff;
                invalidate(mBlinkRect);

                if (!mHandler.hasMessages(MSG_BLINK_TOKEN)) {
                    mHandler.sendEmptyMessageDelayed(MSG_BLINK_TOKEN, FPS_MS);
                }
                return true;
            }

            if (msg.what == MSG_BLINK_SQUARE) {
                mBlinkDisplayOff = !mBlinkDisplayOff;
                invalidate(mBlinkRect);

                if (!mHandler.hasMessages(MSG_BLINK_SQUARE)) {
                    mHandler.sendEmptyMessageDelayed(MSG_BLINK_SQUARE, FPS_MS);
                }
                return true;
            }
            return false;
        }
    }

    private Bitmap getResBitmap(int bmpResId) {
        Options opts = new Options();
        opts.inDither = false;
        opts.inMutable = true;

        Resources res = getResources(); 
        Bitmap bmp = BitmapFactory.decodeResource(res, bmpResId, opts);

        if (bmp == null && isInEditMode()) {
            // BitmapFactory.decodeResource doesn't work from the rendering
            // library in Eclipse's Graphical Layout Editor. Use this workaround instead.

            Drawable d = res.getDrawable(bmpResId);
            int w = d.getIntrinsicWidth();
            int h = d.getIntrinsicHeight();
            bmp = Bitmap.createBitmap(w, h, Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            d.setBounds(0, 0, w - 1, h - 1);
            d.draw(c);
        }

        return bmp;
    }
    
    public void disableBall() {
    	if (mBallId > -1) {
    		mColorBall[mBallId].setDisabled(true);
    	}
    	mBallId = -1;
    }
    
    public void disableBall(int ballId) {
    	mColorBall[ballId].setDisabled(true);
    }
 
    public void setClient(ClientThread clientThread) {
    	mClientThread = clientThread;
    }
    
    private boolean isClientRunning() {
    	return mGameActivity.isClientRunning();
    }
    
    public void setGameActivity(GameActivity gameActivity) {
    	mGameActivity = gameActivity;
    }
    
    private static void writeToLog(String filter, String msg) {
    	if ("true".equalsIgnoreCase(resources.getString(R.string.debug))) {
    		Log.d(filter, msg);
    	}
    }
    
    private void getSharedPreferences() {
        SharedPreferences settings = mContext.getSharedPreferences(UserPreferences.PREFS_NAME, Context.MODE_PRIVATE);
        mTokenSize = settings.getInt(GameActivity.TOKEN_SIZE, 50);
        mTokenColor1 = settings.getInt(GameActivity.TOKEN_COLOR_1, Color.RED);
        mTokenColor2 = settings.getInt(GameActivity.TOKEN_COLOR_2, Color.BLUE);
        
    }

	public static int getPrizeLocation() {
		return mPrizeLocation;
	}

}


