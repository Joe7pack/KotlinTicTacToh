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
package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import com.guzzardo.android.willyshmo.kotlintictacdoh.GameView.ICellListener
import com.guzzardo.android.willyshmo.kotlintictacdoh.MainActivity.UserPreferences
import com.guzzardo.android.willyshmo.kotlintictacdoh.RabbitMQMessageConsumer.OnReceiveMessageHandler
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.androidId
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.getConfigMap
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.latitude
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.longitude
import kotlinx.android.synthetic.main.players_online.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.util.*

class GameActivity : Activity(), ToastMessage {
    private var mServer = false
    private var mClient = false
    private val mHandler = Handler(Looper.getMainLooper(), MyHandlerCallback())
    private var mButtonNext: Button? = null
    private var mPlayer1TokenChoice = GameView.BoardSpaceValues.EMPTY
    private var mPlayer2TokenChoice = GameView.BoardSpaceValues.EMPTY // computer or opponent
    private var mPlayer1ScoreTextValue: TextView? = null
    private var mPlayer2ScoreTextValue: TextView? = null
    private var mPlayer1NameTextValue: EditText? = null
    private var mPlayer2NameTextValue: EditText? = null
    private var mGameTokenPlayer1: ImageView? = null
    private var mGameTokenPlayer2: ImageView? = null
    private val humanWinningHashMap: MutableMap<Int, Int> = HashMap()
    private var mClientThread: ClientThread? = null
    private var mServerThread: ServerThread? = null
    private var mTokensFromClient: MutableList<IntArray>? = null
    private val mRandom = Random()
    private var mMessageClientConsumer: RabbitMQMessageConsumer? = null
    private var mMessageServerConsumer: RabbitMQMessageConsumer? = null
    private var mMessageStartGameConsumer: RabbitMQMessageConsumer? = null
    private var mRabbitMQClientResponse: String? = null
    private var mRabbitMQServerResponse: String? = null
    private var mRabbitMQStartGameResponse: String? = null
    private var mRabbitMQServerResponseHandler: RabbitMQServerResponseHandler? = null
    private var mRabbitMQClientResponseHandler: RabbitMQClientResponseHandler? = null
    private var mRabbitMQStartGameResponseHandler: RabbitMQStartGameResponseHandler? = null
    private var mServerHasOpponent: String? = null

    interface PrizeValue {
        companion object {
            const val SHMOGRANDPRIZE = 4 //player wins with a Shmo and shmo card was placed on prize card
            const val SHMOPRIZE = 2 //player wins with a shmo
            const val GRANDPRIZE = 3 //player wins by placing winning card on prize token
            const val REGULARPRIZE = 1 //player wins with any card covering the prize
        }
    }

    /** Called when the activity is first created.  */
    public override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        Companion.resources = resources
        setContentView(R.layout.lib_game)
        mGameView = findViewById<View>(R.id.game_view) as GameView
        mButtonNext = findViewById<View>(R.id.next_turn) as Button
        mButtonStartText = mButtonNext!!.text
        mPlayer1ScoreTextValue = findViewById<View>(R.id.player1_score) as TextView
        mPlayer2ScoreTextValue = findViewById<View>(R.id.player2_score) as TextView
        mPlayer1NameTextValue = findViewById<View>(R.id.player1_name) as EditText
        mPlayer2NameTextValue = findViewById<View>(R.id.player2_name) as EditText
        mGameTokenPlayer1 = findViewById<View>(R.id.player1_token) as ImageView
        mGameTokenPlayer2 = findViewById<View>(R.id.player2_token) as ImageView
        mGameView!!.isFocusable = true
        mGameView!!.isFocusableInTouchMode = true
        mGameView!!.setCellListener(MyCellListener())
        mButtonNext!!.setOnClickListener(MyButtonListener())
        HUMAN_VS_HUMAN = false
        HUMAN_VS_NETWORK = false
        val settings = getSharedPreferences(UserPreferences.PREFS_NAME, MODE_PRIVATE)
        mPlayer1Score = settings.getInt(PLAYER1_SCORE, 0)
        mPlayer2Score = settings.getInt(PLAYER2_SCORE, 0)
        mWillyScore = settings.getInt(WILLY_SCORE, 0)
        moveModeTouch = settings.getBoolean(MOVE_MODE, false)
        soundMode = settings.getBoolean(SOUND_MODE, false)
        mGameView!!.setViewDisabled(false)
        mHostName = getConfigMap("RabbitMQIpAddress")
        mQueuePrefix = getConfigMap("RabbitMQQueuePrefix")
        writeToLog("GameActivity", "onCreate() Completed")
    }

    private fun showHostWaitDialog(): AlertDialog { //ProgressDialog {
        val opponentName = if (mPlayer2Name == null) "Waiting for player to connect..." else "Waiting for " + mPlayer2Name + " to connect..."
        val hostingDescription = if (mPlayer2Name == null) "Hosting... (Ask a friend to install Willy Shmo\'s Tic Tac Toe)" else "Hosting..."
        mGameView!!.setGamePrize()
        //return ProgressDialog.show(this@GameActivity, hostingDescription, opponentName, true, true) { finish() }
        return AlertDialog.Builder(this@GameActivity)
            .setIcon(R.drawable.willy_shmo_small_icon)
            .setTitle(hostingDescription)
            .setMessage(opponentName)
            .setCancelable(true)
            .create()
    }

    private fun showClientWaitDialog(): AlertDialog { //ProgressDialog {
        //return ProgressDialog.show(this@GameActivity, "Connecting...", "to $mPlayer2Name", true, true) { finish() }
        return AlertDialog.Builder(this@GameActivity)
            .setIcon(R.drawable.willy_shmo_small_icon)
            .setTitle("Connecting...")
            .setMessage(mPlayer2Name)
            .setCancelable(true)
            .create()
    }

    override fun onResume() {
        super.onResume()
        mGameView!!.setGameActivity(this)
        mGameView!!.setClient(null)
        mServer = java.lang.Boolean.valueOf(intent.getStringExtra(START_SERVER))
        if (mServer && !mServerRunning) {
            mPlayer1Id = intent.getIntExtra(PLAYER1_ID, 0)
            mServerThread = ServerThread()
            mMessageServerConsumer = RabbitMQMessageConsumer(this@GameActivity, Companion.resources)
            mRabbitMQServerResponseHandler = RabbitMQServerResponseHandler()
            mRabbitMQServerResponseHandler!!.rabbitMQResponse = "server starting"
            setUpMessageConsumer(mMessageServerConsumer!!, "server", mRabbitMQServerResponseHandler!!)
            mPlayer1Name = intent.getStringExtra(PLAYER1_NAME)
            mServerRunning = true
            mServerThread!!.start()
            HUMAN_VS_NETWORK = true
            mServerHasOpponent = intent.getStringExtra(HAVE_OPPONENT)
            writeToLog("GameActivity", "onResume - we are serving but server is not running")
        }
        mClient = java.lang.Boolean.valueOf(intent.getStringExtra(START_CLIENT))
        if (mClient && !mClientRunning) {
            mPlayer1Id = intent.getIntExtra(PLAYER1_ID, 0)
            //val clientOpponentId = intent.getStringExtra(START_CLIENT_OPPONENT_ID)
            player2Id = intent.getStringExtra(START_CLIENT_OPPONENT_ID) //clientOpponentId
            mClientThread = ClientThread()
            mMessageClientConsumer = RabbitMQMessageConsumer(this@GameActivity, Companion.resources)
            mRabbitMQClientResponseHandler = RabbitMQClientResponseHandler()
            mRabbitMQClientResponseHandler!!.rabbitMQResponse = "clientStarting"
            setUpMessageConsumer(mMessageClientConsumer!!, "client", mRabbitMQClientResponseHandler!!)
            mClientThread!!.start()
            mClientRunning = true
            HUMAN_VS_NETWORK = true
            mGameView!!.setClient(mClientThread) //this is where we inform GameView to send game tokens to network opponent when the GameView is created
            mGameView!!.setOpposingPlayerId(player2Id)
            mPlayer2Name = intent.getStringExtra(PLAYER2_NAME)
            mClientWaitDialog = showClientWaitDialog()
            mClientWaitDialog!!.show()
            writeToLog("GameActivity", "onResume - we are client but client is not running")
        }
        if (mServer && !mClient) {
            mPlayer2NetworkScore = 0
            mPlayer1NetworkScore = mPlayer2NetworkScore
            mPlayer2Name = null
            displayScores()
            mHostWaitDialog = showHostWaitDialog()
            mHostWaitDialog!!.show()
            val androidId = "&deviceId=" + androidId
            val latitude = "&latitude=" + latitude
            val longitude = "&longitude=" + longitude
            val trackingInfo = androidId + latitude + longitude
            val urlData = ("/gamePlayer/update/?onlineNow=true&playingNow=false&opponentId=0" + trackingInfo + "&id="
                    + mPlayer1Id + "&userName=")
            SendMessageToWillyShmoServer().execute(urlData, mPlayer1Name, this@GameActivity, Companion.resources, java.lang.Boolean.valueOf(false))
            writeToLog("GameActivity", "onResume - we are serving but we're not a client")
            return
        }
        var player = mGameView!!.currentPlayer
        if (player == GameView.State.UNKNOWN) {
            player = GameView.State.fromInt(intent.getIntExtra(START_PLAYER_HUMAN, -3))
            if (player == GameView.State.UNKNOWN) {
                player = GameView.State.fromInt(intent.getIntExtra(EXTRA_START_PLAYER, 1))
            } else {
                HUMAN_VS_HUMAN = true
            }
            mGameView!!.setHumanState(HUMAN_VS_HUMAN)
            mPlayer1Name = intent.getStringExtra(PLAYER1_NAME)
            mPlayer2Name = intent.getStringExtra(PLAYER2_NAME)
            if (!checkGameFinished(player, false)) {
                selectTurn(player)
            }
        }
        mGameView!!.setGamePrize() //works only from client side but server side never call onResume when starting a game
        //but if we just play against Willy then onResume is called
        mPlayer2NetworkScore = 0
        mPlayer1NetworkScore = mPlayer2NetworkScore
        displayScores()
        highlightCurrentPlayer(player)
        showPlayerTokenChoice()
        if (player == GameView.State.PLAYER2 && !(HUMAN_VS_HUMAN or HUMAN_VS_NETWORK)) {
            mHandler.sendEmptyMessageDelayed(MSG_COMPUTER_TURN, COMPUTER_DELAY_MS)
        }
        if (player == GameView.State.WIN) {
            setWinState(mGameView!!.winner)
        }
        mGameView!!.setViewDisabled(false)
        writeToLog("GameActivity", "onResume() Completed")
    }

    private fun selectTurn(player: GameView.State): GameView.State {
        mGameView!!.currentPlayer = player
        mButtonNext!!.isEnabled = false
        if (player == GameView.State.PLAYER1) {
            mGameView!!.isEnabled = true
        } else if (player == GameView.State.PLAYER2) {
            mGameView!!.isEnabled = false
        }
        return player
    }

    private inner class MyCellListener : ICellListener {
        override fun onCellSelected() {
            val cell = mGameView!!.selection
            mButtonNext!!.isEnabled = cell >= 0
            if (cell >= 0) {
                playHumanMoveSound()
                mLastCellSelected = cell
            }
        }
    }

    private inner class MyButtonListener : View.OnClickListener {
        fun showChooseTokenDialog(): AlertDialog {
            return AlertDialog.Builder(this@GameActivity)
                    .setIcon(R.drawable.willy_shmo_small_icon)
                    .setTitle(R.string.alert_dialog_starting_token_value)
                    .setSingleChoiceItems(R.array.select_starting_token, 0) { dialog, whichButton ->
                        when (whichButton) {
                            0 -> {
                                if (HUMAN_VS_HUMAN) {
                                    if (mGameView!!.currentPlayer == GameView.State.PLAYER1) {
                                        mPlayer1TokenChoice = GameView.BoardSpaceValues.CIRCLE
                                        mPlayer2TokenChoice = GameView.BoardSpaceValues.CROSS
                                    } else {
                                        mPlayer1TokenChoice = GameView.BoardSpaceValues.CROSS // else we're looking at player 2
                                        mPlayer2TokenChoice = GameView.BoardSpaceValues.CIRCLE
                                    }
                                } else {
                                    mPlayer1TokenChoice = GameView.BoardSpaceValues.CIRCLE
                                    mPlayer2TokenChoice = GameView.BoardSpaceValues.CROSS
                                }
                            }
                            1 -> {
                                if (HUMAN_VS_HUMAN) {
                                    if (mGameView!!.currentPlayer == GameView.State.PLAYER1) {
                                        mPlayer1TokenChoice = GameView.BoardSpaceValues.CROSS
                                        mPlayer2TokenChoice = GameView.BoardSpaceValues.CIRCLE
                                    } else {
                                        mPlayer1TokenChoice = GameView.BoardSpaceValues.CIRCLE
                                        mPlayer2TokenChoice = GameView.BoardSpaceValues.CROSS
                                    }
                                } else {
                                    mPlayer1TokenChoice = GameView.BoardSpaceValues.CROSS
                                    mPlayer2TokenChoice = GameView.BoardSpaceValues.CIRCLE
                                }
                            }
                        }
                        setGameTokenFromDialog()
                    }
                    .create()
        }

        private fun sendNewGameToServer() {
            mGameView!!.initalizeGameValues()
            mGameView!!.currentPlayer = GameView.State.PLAYER1
            mButtonNext!!.text = mButtonStartText
            mButtonNext!!.isEnabled = false
            mPlayer1TokenChoice = GameView.BoardSpaceValues.EMPTY
            mPlayer2TokenChoice = GameView.BoardSpaceValues.EMPTY
            showPlayerTokenChoice()
            mGameView!!.setTokenCards() //generate new random list of tokens in mGameView.mGameTokenCard[x]
            for (x in GameView.mGameTokenCard.indices) {
                if (x < 8) {
                    mGameView!!.updatePlayerToken(x, GameView.mGameTokenCard[x]) //update ball array
                } else {
                    mGameView!!.setBoardSpaceValueCenter(GameView.mGameTokenCard[x])
                }
            }
            mGameView!!.sendTokensToServer()
            mGameView!!.invalidate()
            mClientWaitDialog = showClientWaitDialog()
            mClientWaitDialog!!.show()
        }

        override fun onClick(v: View) {
            val player = mGameView!!.currentPlayer
            val testText = mButtonNext!!.text.toString()
            if (testText.endsWith("Play Again?")) { //game is over
                if (imServing) {
                    mHostWaitDialog = showHostWaitDialog()
                    mHostWaitDialog!!.show()
                } else if (mClientRunning) { //reset values on client side
                    sendNewGameToServer()
                } else {
                    finish()
                }
            } else if (player == GameView.State.PLAYER1 || player == GameView.State.PLAYER2) {
                playFinishMoveSound()
                val cell = mGameView!!.selection
                var okToFinish = true
                if (cell >= 0) {
                    mSavedCell = cell
                    var gameTokenPlayer1 = -1
                    mGameView!!.stopBlink()
                    mGameView!!.setCell(cell, player)
                    mGameView!!.setBoardSpaceValue(cell)
                    if (mPlayer1TokenChoice == GameView.BoardSpaceValues.EMPTY) {
                        val tokenSelected = mGameView!!.getBoardSpaceValue(cell)
                        if (tokenSelected == GameView.BoardSpaceValues.CIRCLECROSS) {
                            mChooseTokenDialog = showChooseTokenDialog()
                            mChooseTokenDialog!!.show()
                            okToFinish = false
                        } else {
                            if (player == GameView.State.PLAYER1) {
                                mPlayer1TokenChoice = tokenSelected
                                mPlayer2TokenChoice = if (mPlayer1TokenChoice == GameView.BoardSpaceValues.CIRCLE) GameView.BoardSpaceValues.CROSS else GameView.BoardSpaceValues.CIRCLE
                            } else {
                                mPlayer2TokenChoice = tokenSelected
                                mPlayer1TokenChoice = if (mPlayer2TokenChoice == GameView.BoardSpaceValues.CIRCLE) GameView.BoardSpaceValues.CROSS else GameView.BoardSpaceValues.CIRCLE
                            }
                            mGameView!!.setPlayer1TokenChoice(mPlayer1TokenChoice)
                            mGameView!!.setPlayer2TokenChoice(mPlayer2TokenChoice)
                            gameTokenPlayer1 = mPlayer1TokenChoice
                            showPlayerTokenChoice()
                        }
                    }
                    if (okToFinish) {
                        if (HUMAN_VS_NETWORK) {
                            val movedMessage = "moved, " + mGameView!!.ballMoved + ", " + cell + ", " + gameTokenPlayer1
                            if (imServing) {
                                mServerThread!!.setMessageToClient(movedMessage)
                            } else {
                                mClientThread!!.setMessageToServer(movedMessage)
                            }
                            finishTurn(false, false, false) //don't send message to make computer move don't switch the player don't use player 2 for win testing
                            val currentPlayer = mGameView!!.currentPlayer
                            highlightCurrentPlayer(getOtherPlayer(currentPlayer))
                            mGameView!!.setViewDisabled(true)
                        } else {
                            finishTurn(true, true, false) //send message to make computer move switch the player don't use player 2 for win testing
                        }
                    } else {
                        mBallMoved = mGameView!!.ballMoved
                        mGameView!!.disableBall() //disableBall sets ball moved to -1, so we need to get it first :-(
                    }
                }
            }
        }
    }

    private fun setGameTokenFromDialog() {  // when player has chosen value for wildcard token
        mChooseTokenDialog!!.dismiss()
        if (HUMAN_VS_NETWORK) {
            mGameView!!.currentPlayer = GameView.State.PLAYER1
        }
        if (!(HUMAN_VS_HUMAN or HUMAN_VS_NETWORK)) { //playing against Willy
            setComputerMove()
            finishTurn(false, false, false) //added to test if Willy wins 
            mGameView!!.disableBall()
        } else if (HUMAN_VS_HUMAN) {
            finishTurn(false, true, false) //don't send message to make computer move but switch the player don't use player 2 for win testing
            highlightCurrentPlayer(mGameView!!.currentPlayer)
        } else {
            mGameView!!.disableBall()
            highlightCurrentPlayer(GameView.State.PLAYER2)
        }
        showPlayerTokenChoice()
        val movedMessage = "moved, " + mBallMoved + ", " + mSavedCell + ", " + mPlayer1TokenChoice
        if (HUMAN_VS_NETWORK) {
            if (imServing) {
                mServerThread!!.setMessageToClient(movedMessage)
            } else {
                mClientThread!!.setMessageToServer(movedMessage)
            }
            mGameView!!.setViewDisabled(true)
        }
    }

    private fun saveHumanWinner(winningPositionOnBoard: Int, positionStatus: Int) {
        humanWinningHashMap[winningPositionOnBoard] = positionStatus

        //second value indicates position is available for use after comparing against other 
        //entries in this map
        //second value: initialized to -1 upon creation 
        // set to 0 if not available
        // set to 1 if available
    }

    private fun selectBestMove(): IntArray { //this is the heart and soul of Willy Shmo - at least as far as his skill at playing this game
        val selectionArray = IntArray(2)
        var tokenSelected = -1
        var boardSpaceSelected = -1
        humanWinningHashMap.clear()
        if (mPlayer2TokenChoice == GameView.BoardSpaceValues.EMPTY) { //computer makes first move of game
            tokenSelected = mGameView!!.selectRandomComputerToken()
            //TODO - just for testing 1 specific case
            //tokenSelected = mGameView.selectSpecificComputerToken(BoardSpaceValues.CROSS, true);        	
            boardSpaceSelected = mGameView!!.selectRandomAvailableBoardSpace()
        } else {
            // populate array with available moves        
            var availableSpaceCount = 0
            val availableValues = mGameView!!.boardSpaceAvailableValues
            val normalizedBoardPlayer1 = IntArray(GameView.BoardSpaceValues.BOARDSIZE)
            val normalizedBoardPlayer2 = IntArray(GameView.BoardSpaceValues.BOARDSIZE)
            val testAvailableValues = BooleanArray(GameView.BoardSpaceValues.BOARDSIZE) // false = not available
            var tokenChoice = mGameView!!.selectSpecificComputerToken(mPlayer2TokenChoice, true)
            //true = playing offensively which means we can select the xo card if we have one
            val boardSpaceValues = mGameView!!.boardSpaceValues

            //populate test board with xo token changed to player 2 token
            for (x in normalizedBoardPlayer1.indices) {
                normalizedBoardPlayer1[x] = boardSpaceValues[x]
                normalizedBoardPlayer2[x] = boardSpaceValues[x]
                if (normalizedBoardPlayer1[x] == GameView.BoardSpaceValues.CIRCLECROSS) {
                    normalizedBoardPlayer1[x] = mPlayer1TokenChoice
                    normalizedBoardPlayer2[x] = mPlayer2TokenChoice
                }
            }
            var trialBoardSpaceSelected1 = -1
            var trialBoardSpaceSelected2 = -1
            var trialBoardSpaceSelected3 = -1
            for (x in availableValues.indices) {
                if (availableValues[x]) {
                    availableSpaceCount++
                    if (trialBoardSpaceSelected1 == -1) {
                        trialBoardSpaceSelected1 = x
                    } else {
                        if (trialBoardSpaceSelected2 == -1) {
                            trialBoardSpaceSelected2 = x
                        } else {
                            if (trialBoardSpaceSelected3 == -1) {
                                trialBoardSpaceSelected3 = x
                            }
                        }
                    }
                }
            }
            if (availableSpaceCount == 1) { //last move!
                if (tokenChoice == -1) {
                    tokenChoice = mGameView!!.selectLastComputerToken()
                }
                tokenSelected = tokenChoice
                boardSpaceSelected = trialBoardSpaceSelected1
            }
            if (tokenChoice > -1) {
                if (tokenChoice == GameView.BoardSpaceValues.CIRCLECROSS) {
                    tokenChoice = mPlayer2TokenChoice
                }
                for (x in availableValues.indices) {
                    if (availableValues[x]) {
                        val testBoard = IntArray(GameView.BoardSpaceValues.BOARDSIZE)
                        //copy normalizedBoard to testBoard
                        for (y in testBoard.indices) {
                            testBoard[y] = normalizedBoardPlayer2[y]
                        }
                        testBoard[x] = mPlayer2TokenChoice
                        val winnerFound = checkWinningPosition(testBoard)
                        if (winnerFound[0] > -1 || winnerFound[1] > -1 || winnerFound[2] > -1) {
                            tokenSelected = tokenChoice
                            boardSpaceSelected = x
                            break
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
 */         if (tokenSelected == -1) { //try again with human selected token
                tokenChoice = mGameView!!.selectSpecificComputerToken(mPlayer2TokenChoice, false)
                if (tokenChoice > -1) {
//            		int computerBlockingMove = -1;
                    for (x in availableValues.indices) {
                        if (availableValues[x]) {
                            val testBoard = IntArray(GameView.BoardSpaceValues.BOARDSIZE)
                            //copy normalizedBoard to testBoard
                            for (y in testBoard.indices) {
                                testBoard[y] = normalizedBoardPlayer1[y]
                            }
                            testBoard[x] = mPlayer1TokenChoice
                            // since there can be multiple winning moves available for the human 
// move computer token to boardSpaceSelected
// reset available and re-test for winner using mPlayer1TokenChoice
// if winner not found then set tokenSelected to tokenChoice and set boardSpaceSelected to x                				
                            val winnerFound = checkWinningPosition(testBoard)
                            if (winnerFound[0] > -1 || winnerFound[1] > -1 || winnerFound[2] > -1) {
                                saveHumanWinner(x, -1)
                            }
                        }
                    }
                    //System.out.println("human winner list size: "+humanWinningHashMap.size());
                    if (humanWinningHashMap.size == 1) {
                        val onlyWinningPosition: Array<Any> = humanWinningHashMap.keys.toTypedArray()
                        val testMove = onlyWinningPosition[0] as Int
                        tokenSelected = tokenChoice
                        boardSpaceSelected = testMove
                    } else if (humanWinningHashMap.size > 1) {
                        val it: Iterator<Int> = humanWinningHashMap.keys.iterator()
                        while (it.hasNext()) {
                            val winningPosition = it.next()
                            //System.out.println("winning position: "+winningPosition);
                            val testBoard = IntArray(GameView.BoardSpaceValues.BOARDSIZE)
                            for (y in testBoard.indices) {
                                testBoard[y] = normalizedBoardPlayer1[y]
                            }
                            testBoard[winningPosition] = mPlayer2TokenChoice
                            mGameView!!.setAvailableMoves(winningPosition, testBoard, testAvailableValues)
                            val it2: Iterator<Int> = humanWinningHashMap.keys.iterator()
                            while (it2.hasNext()) {
                                val testMove = it2.next()
                                if (winningPosition == testMove) {
                                    continue  // no point in testing against same value
                                }
                                val spaceOkToUse = humanWinningHashMap[testMove] as Int
                                //System.out.println("testing "+testMove+ " against winning position: "+ winningPosition);
                                // testMove = a winning move human
                                if (testAvailableValues[testMove]) {
                                    //computerBlockingMove = winningPosition;
                                    //break;
                                    saveHumanWinner(testMove, 0) // space cannot be used
                                    //System.out.println("reset value at "+testMove+ " to unavailable(false) for "+ winningPosition);
                                } else {
                                    if (spaceOkToUse != 0) saveHumanWinner(testMove, 1) //space is ok to use
                                    //System.out.println("reset value at "+testMove+ " to ok to use for "+ winningPosition);
                                }
                            }
                        }
                        val it3: Iterator<Int> = humanWinningHashMap.keys.iterator()
                        while (it3.hasNext()) {
                            val computerBlockingMove = it3.next()
                            val spaceAvailable = humanWinningHashMap[computerBlockingMove] as Int
                            if (spaceAvailable == 1) {
                                boardSpaceSelected = computerBlockingMove
                                tokenSelected = tokenChoice
                                //System.out.println("found good move for computer at "+boardSpaceSelected);
                            }
                        }
                    }
                }
            }
            // if we reach here then the computer cannot win on this move and the human
            // cannot win on the next 
            // so we'll select a position that at least doesn't give the human a win and move there
            if (tokenSelected == -1) {
                tokenChoice = mGameView!!.selectSpecificComputerToken(mPlayer1TokenChoice, false)
                if (tokenChoice > -1) {
                    for (x in availableValues.indices) {
                        if (availableValues[x]) {
                            val testBoard = IntArray(GameView.BoardSpaceValues.BOARDSIZE)
                            //copy normalizedBoard to testBoard
                            for (y in testBoard.indices) {
                                testBoard[y] = normalizedBoardPlayer1[y]
                            }
                            testBoard[x] = mPlayer1TokenChoice
                            val winnerFound = checkWinningPosition(testBoard)
                            //test to see if human can't win if he were to move here
                            // if human cannot win then this is a candidate move for computer
                            if (winnerFound[0] == -1 && winnerFound[1] == -1 && winnerFound[2] == -1) {
                                //take it one step further and see if moving to this position gives the human a win in the next move,
                                // if it does then try next available board position
                                val humanToken = mGameView!!.selectSpecificHumanToken(mPlayer1TokenChoice)
                                val testBoard2 = IntArray(GameView.BoardSpaceValues.BOARDSIZE)
                                if (humanToken > -1) {
                                    var computerCanUseMove = true
                                    for (z in availableValues.indices) {
                                        //copy the board with trial move from above to another test board
                                        for (y in testBoard.indices) {
                                            testBoard2[y] = testBoard[y]
                                        }
                                        //set available moves for new test board
                                        mGameView!!.setAvailableMoves(x, testBoard2, testAvailableValues)
                                        if (testAvailableValues[z] == true && z != x) {
                                            testBoard2[z] = mPlayer1TokenChoice
                                            val winnerFound2 = checkWinningPosition(testBoard2)
                                            if (winnerFound2[0] > -1 || winnerFound2[1] > -1 || winnerFound2[2] > -1) {
                                                computerCanUseMove = false
                                                break
                                            }
                                        }
                                    }
                                    //System.out.println("test case 2 selection made, computerCanUseMove = "+computerCanUseMove+" boardSpaceSelected: "+x);
                                    if (computerCanUseMove) {
                                        tokenSelected = tokenChoice
                                        boardSpaceSelected = x
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (tokenSelected == -1) {
                val humanTokenChoice = mGameView!!.selectSpecificHumanToken(mPlayer1TokenChoice)
                tokenChoice = mGameView!!.selectSpecificComputerToken(mPlayer2TokenChoice, false)
                if (availableSpaceCount == 2) { //we're down to our last 2 possible moves
                    //if we get here we're on the last move and we know we can't win with it.
                    //so let's see if the human could make the computer win 
                    val testBoard = IntArray(GameView.BoardSpaceValues.BOARDSIZE)
                    //copy normalizedBoard to testBoard
                    for (y in testBoard.indices) {
                        testBoard[y] = normalizedBoardPlayer1[y]
                    }
                    if (humanTokenChoice > -1) {
                        testBoard[trialBoardSpaceSelected1] = mPlayer1TokenChoice
                        val winnerFound = checkWinningPosition(testBoard)
                        if (winnerFound[0] > -1 || winnerFound[1] > -1 || winnerFound[2] > -1) {
                            boardSpaceSelected = trialBoardSpaceSelected2
                            if (tokenChoice == -1) {
                                tokenChoice = mGameView!!.selectLastComputerToken()
                            }
                            tokenSelected = tokenChoice
                        } else {
                            tokenSelected = mGameView!!.selectLastComputerToken()
                            boardSpaceSelected = trialBoardSpaceSelected1
                        }
                    } else {
                        if (tokenChoice == -1) tokenChoice = mGameView!!.selectLastComputerToken()
                        tokenSelected = tokenChoice
                        testBoard[trialBoardSpaceSelected2] = mPlayer1TokenChoice
                        val winnerFound = checkWinningPosition(testBoard)
                        boardSpaceSelected = if (winnerFound[0] > -1 || winnerFound[1] > -1 || winnerFound[2] > -1) {
                            trialBoardSpaceSelected1
                            //System.out.println("winning move found for human, moving computer token to "+trialBoardSpaceSelected1);
                        } else {
                            trialBoardSpaceSelected2
                            //System.out.println("moving computer token to "+trialBoardSpaceSelected2);
                        }
                    }
                }
                if (availableSpaceCount >= 3) {
                    tokenChoice = mGameView!!.selectSpecificHumanToken(mPlayer1TokenChoice)
                    if (tokenChoice > -1) {
                        val testBoard = IntArray(GameView.BoardSpaceValues.BOARDSIZE)
                        for (x in availableValues.indices) {
                            for (y in testBoard.indices) {
                                testBoard[y] = normalizedBoardPlayer1[y]
                            }
                            if (availableValues[x]) {
                                testBoard[x] = mPlayer1TokenChoice
                                val winnerFound = checkWinningPosition(testBoard)
                                if (winnerFound[0] > -1 || winnerFound[1] > -1 || winnerFound[2] > -1) {
                                    tokenChoice = mGameView!!.selectSpecificComputerToken(mPlayer2TokenChoice, false)
                                    tokenSelected = if (tokenChoice > -1) {
                                        tokenChoice
                                    } else {
                                        mGameView!!.selectLastComputerToken() //no choice here
                                    }
                                    boardSpaceSelected = x
                                    break
                                }
                            }
                        }
                        //if we get here then there is no winning move available for human player
                        // for us to block so we'll just select the next available position and move there
                        if (tokenSelected == -1) {
                            tokenChoice = mGameView!!.selectSpecificComputerToken(mPlayer2TokenChoice, false)
                            //System.out.println("3 or more spaces open and still no choice made yet");
                        }
                        tokenSelected = if (tokenChoice > -1) {
                            tokenChoice
                        } else {
                            mGameView!!.selectLastComputerToken() //no choice here
                        }
                        for (x in availableValues.indices) {
                            if (availableValues[x]) {
                                boardSpaceSelected = x
                                break
                            }
                        }
                    } else {
                        tokenChoice = mGameView!!.selectSpecificComputerToken(mPlayer2TokenChoice, false)
                        if (tokenChoice == -1) {
                            tokenChoice = mGameView!!.selectLastComputerToken() //no choice here
                        }
                        val testBoard = IntArray(GameView.BoardSpaceValues.BOARDSIZE)
                        //System.out.println("3 or more spaces open last attempt made");
                        for (x in availableValues.indices) {
                            for (y in testBoard.indices) {
                                testBoard[y] = normalizedBoardPlayer2[y]
                            }
                            if (availableValues[x]) {
                                testBoard[x] = mPlayer1TokenChoice
                                val winnerFound = checkWinningPosition(testBoard)
                                if (winnerFound[0] == -1 && winnerFound[1] == -1 && winnerFound[2] == -1) {
                                    tokenSelected = tokenChoice
                                    boardSpaceSelected = x
                                    break
                                }
                            }
                        }
                        if (tokenSelected == -1) {
                            tokenSelected = tokenChoice
                            for (x in availableValues.indices) {
                                if (availableValues[x]) {
                                    boardSpaceSelected = x
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }
        mGameView!!.disableBall(tokenSelected)
        selectionArray[0] = boardSpaceSelected
        selectionArray[1] = tokenSelected
        return selectionArray
    }

    private fun setNetworkMove(boardPosition: Int, tokenMoved: Int) {
        var resultValue = if (tokenMoved > 3) {
            tokenMoved - 4
        } else {
            tokenMoved + 4
        }
        mGameView!!.moveComputerToken(boardPosition, resultValue) //move token selected to location on board
        mLastCellSelected = boardPosition
        playFinishMoveSound()
        mGameView!!.disableBall(resultValue)
        mGameView!!.setCell(boardPosition, GameView.State.PLAYER2) //set State table
    }

    private fun setComputerMove(): Int {
        var computerToken = GameView.BoardSpaceValues.EMPTY
        val index = selectBestMove() //0 = boardSpaceSelected, 1 = tokenSelected
        if (index[0] != -1) {
            playComputerMoveSound()
            mGameView!!.setCell(index[0], GameView.State.PLAYER2) // set State table - the computer (Willy) is always PLAYER2
            computerToken = mGameView!!.moveComputerToken(index[0], index[1]) //move computer token to location on board
        }
        return computerToken
    }

    private fun networkCallBackFinish() {
        finishTurn(false, false, true) //don't send message to make computer move don't switch the player don't use player 2 for win testing 
        val testText = mButtonNext!!.text.toString()
        if (testText.endsWith("Play Again?")) {
            highlightCurrentPlayer(GameView.State.EMPTY)
            return
        }
        val currentPlayer = mGameView!!.currentPlayer
        highlightCurrentPlayer(currentPlayer)
        mGameView!!.setViewDisabled(false)
    }

    private inner class MyHandlerCallback : Handler.Callback {
        override fun handleMessage(msg: Message): Boolean {
            writeToLog("MyHandlerCallback", "msg.what value: " + msg.what)
            if (msg.what == DISMISS_WAIT_FOR_NEW_GAME_FROM_CLIENT) {
                if (mHostWaitDialog != null) {
                    mHostWaitDialog!!.dismiss()
                    writeToLog("MyHandlerCallback", "host wait dialog dismissed")
                    mHostWaitDialog = null
                }
                return true
            }
            if (msg.what == DISMISS_WAIT_FOR_NEW_GAME_FROM_SERVER) {
                val urlData = ("/gamePlayer/update/?id=" + mPlayer1Id + "&playingNow=true&opponentId=" + player2Id + "&userName=")
                SendMessageToWillyShmoServer().execute(urlData, mPlayer1Name, this@GameActivity, Companion.resources, java.lang.Boolean.FALSE)
                mClientWaitDialog!!.dismiss()
                writeToLog("MyHandlerCallback", "client wait dialog dismissed")
                mClientWaitDialog = null
                return true
            }
            if (msg.what == ACCEPT_INCOMING_GAME_REQUEST_FROM_CLIENT) {
                if (mServerHasOpponent != null) {
                    if ("true" == mServerHasOpponent) {
                        setGameRequestFromClient(true)
                    } else {
                        setGameRequestFromClient(false)
                    }
                } else {
                    acceptIncomingGameRequestFromClient()
                }
                return true
            }
            if (msg.what == MSG_HOST_UNAVAILABLE) {
                displayHostNotAvailableAlert()
            }
            if (msg.what == MSG_NETWORK_SERVER_REFUSED_GAME) {
                displayServerRefusedGameAlert(mNetworkOpponentPlayerName)
            }
            if (msg.what == MSG_NETWORK_SERVER_LEFT_GAME) {
                displayOpponentLeftGameAlert("server", mPlayer2Name)
                mPlayer2Name = null
            }
            if (msg.what == MSG_NETWORK_CLIENT_LEFT_GAME) {
                displayOpponentLeftGameAlert("client", mPlayer2Name)
                mPlayer2Name = null
            }
            if (msg.what == NEW_GAME_FROM_CLIENT) {
                mGameView!!.initalizeGameValues()
                mPlayer2NameTextValue!!.setText(mPlayer2Name)
                mButtonNext!!.text = mButtonStartText
                mButtonNext!!.isEnabled = false
                showPlayerTokenChoice()
                for (x in mTokensFromClient!!.indices) {
                    val tokenArray = mTokensFromClient!![x]
                    if (x < 8) {
                        mGameView!!.updatePlayerToken(tokenArray[0], tokenArray[1])
                    } else {
                        mGameView!!.setBoardSpaceValueCenter(tokenArray[1])
                    }
                }
                val moveFirst = mRandom.nextBoolean()
                if (moveFirst) {
                    mGameView!!.currentPlayer = GameView.State.PLAYER1
                    highlightCurrentPlayer(GameView.State.PLAYER1)
                    mGameView!!.setViewDisabled(false)
                } else {
                    mGameView!!.currentPlayer = GameView.State.PLAYER1 //this value will be switched in onClick method
                    highlightCurrentPlayer(GameView.State.PLAYER2)
                    mGameView!!.setViewDisabled(true)
                    if (mServerThread != null)
                        mServerThread!!.setMessageToClient("moveFirst")
                }
                mGameView!!.invalidate()
                return true
            }
            if (msg.what == MSG_NETWORK_CLIENT_TURN) {
                if (mClientThread != null) {
                    val boardPosition = mClientThread!!.boardPosition
                    val tokenMoved = mClientThread!!.tokenMoved
                    setNetworkMove(boardPosition, tokenMoved)
                    networkCallBackFinish()
                    mGameView!!.invalidate()
                }
                return true
            }
            if (msg.what == MSG_NETWORK_SERVER_TURN) {
                if (mServerThread != null) {
                    val boardPosition = mServerThread!!.boardPosition
                    val tokenMoved = mServerThread!!.tokenMoved
                    setNetworkMove(boardPosition, tokenMoved)
                    networkCallBackFinish()
                    val testText = mButtonNext!!.text.toString()
                    if (testText.endsWith("Play Again?")) {
                        if (imServing) { // if win came from client side we need to send back a message to give client the 
                            mServerThread!!.setMessageToClient("game over") // ability to respond
                        }
                    }
                }
                return true
            }
            if (msg.what == MSG_NETWORK_SET_TOKEN_CHOICE) {
                showPlayerTokenChoice()
            }
            if (msg.what == MSG_NETWORK_CLIENT_MAKE_FIRST_MOVE) {
                mGameView!!.currentPlayer = GameView.State.PLAYER1
                highlightCurrentPlayer(GameView.State.PLAYER1)
                mGameView!!.setViewDisabled(false)
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
                val computerToken = setComputerMove()

// for now, the computer will never select the xo token for its opening move but we may change this in 
// the future. As of 07/10/2010, the computer will select the xo token only on a winning move or for the last
// move possible.
                if (mPlayer2TokenChoice == GameView.BoardSpaceValues.EMPTY) {
                    if (computerToken != GameView.BoardSpaceValues.EMPTY) mPlayer2TokenChoice = computerToken
                    mPlayer1TokenChoice = if (mPlayer2TokenChoice == GameView.BoardSpaceValues.CIRCLECROSS ||
                            mPlayer2TokenChoice == GameView.BoardSpaceValues.CROSS) { //computer will always choose X if it selects the XO card
                        GameView.BoardSpaceValues.CIRCLE // on its first move, we may want to change this behavior
                        // see comments above
                    } else {
                        GameView.BoardSpaceValues.CROSS
                    }
                    mGameView!!.setPlayer1TokenChoice(mPlayer1TokenChoice)
                    mGameView!!.setPlayer2TokenChoice(mPlayer2TokenChoice)
                    showPlayerTokenChoice()
                }
                finishTurn(false, true, false) //don't send message to make computer move but do switch the player and don't use player 2 for win testing
                return true
            }
            return false
        }
    }

    private fun getOtherPlayer(player: GameView.State): GameView.State {
        return if (player == GameView.State.PLAYER1) GameView.State.PLAYER2 else GameView.State.PLAYER1
    }

    //FIXME - consider highlighting the border of the enclosing rectangle around the player's name instead
    fun highlightCurrentPlayer(player: GameView.State) {
        val anim: Animation = AlphaAnimation(0.0f, 1.0f)
        anim.duration = 500 //You can manage the time of the blink with this parameter
        anim.startOffset = 20
        anim.repeatMode = Animation.REVERSE
        anim.repeatCount = Animation.INFINITE
        val anim2: Animation = AlphaAnimation(0.0f, 1.0f)
        anim2.duration = 500 //You can manage the time of the blink with this parameter
        anim2.startOffset = 20
        anim2.repeatMode = Animation.REVERSE
        anim2.repeatCount = 0
        if (player == GameView.State.PLAYER1) {
            mPlayer1NameTextValue!!.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.backwithgreenborder, null))
            mPlayer2NameTextValue!!.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.backwithwhiteborder, null))
            mPlayer1NameTextValue!!.startAnimation(anim)
            mPlayer2NameTextValue!!.startAnimation(anim2)
        } else if (player == GameView.State.PLAYER2) {
            mPlayer2NameTextValue!!.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.backwithgreenborder, null))
            mPlayer2NameTextValue!!.startAnimation(anim)
            mPlayer1NameTextValue!!.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.backwithwhiteborder, null))
            mPlayer1NameTextValue!!.startAnimation(anim2)
        } else {
            mPlayer1NameTextValue!!.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.backwithwhiteborder, null))
            mPlayer2NameTextValue!!.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.backwithwhiteborder, null))
            mPlayer1NameTextValue!!.startAnimation(anim2)
            mPlayer2NameTextValue!!.startAnimation(anim2)
        }
    }

    private fun finishTurn(makeComputerMove: Boolean, switchPlayer: Boolean, usePlayer2: Boolean) {
        writeToLog("finishTurn", "called with switchPLayer " + switchPlayer + " usePlayer2: " + usePlayer2)
        var player = mGameView!!.currentPlayer
        if (usePlayer2) { // if we're playing over a network then current player is always player 1 
            player = GameView.State.PLAYER2 // so we need to add some extra logic to test winner on player 2 over the network
        }
        mGameView!!.disableBall()
        if (!checkGameFinished(player, usePlayer2)) {
            if (switchPlayer) {
                player = selectTurn(getOtherPlayer(player))
                if (player == GameView.State.PLAYER2 && makeComputerMove && !(HUMAN_VS_HUMAN or HUMAN_VS_NETWORK)) {
                    writeToLog("finishTurn", "gonna send computer_turn message")
                    mHandler.sendEmptyMessageDelayed(MSG_COMPUTER_TURN, COMPUTER_DELAY_MS)
                }
                highlightCurrentPlayer(player)
            }
        }
    }

    // Given the existence of the xo token, there is a possibility that both players could be winners.
    // in this case we will give precedence to the token type of the player that made the winning move.    
    private fun checkGameFinished(player: GameView.State, usePlayer2: Boolean): Boolean {
        val boardSpaceValues = mGameView!!.boardSpaceValues
        var data = IntArray(GameView.BoardSpaceValues.BOARDSIZE)
        var wildCardValue = mPlayer1TokenChoice
        if (player == GameView.State.PLAYER2) {
            wildCardValue = mPlayer2TokenChoice
        }
        for (x in data.indices) {
            data[x] = boardSpaceValues[x]
            if (data[x] == GameView.BoardSpaceValues.CIRCLECROSS) {
                data[x] = wildCardValue //BoardSpaceValues.CROSS;
            }
        }
        if (testForWinner(data, usePlayer2)) {
            return true
        }
        wildCardValue = if (wildCardValue == mPlayer1TokenChoice) mPlayer2TokenChoice else mPlayer1TokenChoice
        data = IntArray(GameView.BoardSpaceValues.BOARDSIZE)
        for (x in data.indices) {
            data[x] = boardSpaceValues[x]
            if (data[x] == GameView.BoardSpaceValues.CIRCLECROSS) {
                data[x] = wildCardValue
            }
        }
        if (testForWinner(data, usePlayer2)) {
            return true
        }
        if (mGameView!!.testBoardFull(9)) {
            setFinished(GameView.State.EMPTY, -1, -1, -1)
            if (HUMAN_VS_NETWORK) {
                updateWebServerScore()
            }
            return true
        }
        return false
    }

    private fun checkWinningPosition(data: IntArray): IntArray {
        var col = -1
        var row = -1
        var diag = -1
        var winningToken = -1
        var winningPosition1 = -1
        var winningPosition2 = -1
        var winningPosition3 = -1

        // check rows
        var j = 0
        var k = 0
        while (j < 5) {
            if (data[k] != GameView.BoardSpaceValues.EMPTY && data[k] == data[k + 1] && data[k] == data[k + 2]) {
                winningToken = data[k]
                row = j
                winningPosition1 = k
                winningPosition2 = k + 1
                winningPosition3 = k + 2
                break
            }
            if (data[k + 1] != GameView.BoardSpaceValues.EMPTY && data[k + 1] == data[k + 2] && data[k + 2] == data[k + 3]) {
                winningToken = data[k + 1]
                row = j
                winningPosition1 = k + 1
                winningPosition2 = k + 2
                winningPosition3 = k + 3
                break
            }
            if (data[k + 2] != GameView.BoardSpaceValues.EMPTY && data[k + 2] == data[k + 3] && data[k + 2] == data[k + 4]) {
                winningToken = data[k + 2]
                row = j
                winningPosition1 = k + 2
                winningPosition2 = k + 3
                winningPosition3 = k + 4
                break
            }
            j++
            k += 5
        }

        // check columns
        if (row == -1) {
            for (i in 0..4) {
                if (data[i] != GameView.BoardSpaceValues.EMPTY && data[i] == data[i + 5] && data[i] == data[i + 10]) {
                    winningToken = data[i]
                    col = i
                    winningPosition1 = i
                    winningPosition2 = i + 5
                    winningPosition3 = i + 10
                    break
                }
                if (data[i + 5] != GameView.BoardSpaceValues.EMPTY && data[i + 5] == data[i + 10] && data[i + 5] == data[i + 15]) {
                    winningToken = data[i + 5]
                    winningPosition1 = i + 5
                    winningPosition2 = i + 10
                    winningPosition3 = i + 15
                    col = i
                    break
                }
                if (data[i + 10] != GameView.BoardSpaceValues.EMPTY && data[i + 10] == data[i + 15] && data[i + 10] == data[i + 20]) {
                    winningToken = data[i + 10]
                    col = i
                    winningPosition1 = i + 10
                    winningPosition2 = i + 15
                    winningPosition3 = i + 20
                    break
                }
            }
        }

        // check diagonals
        //upper left to lower right diagonals:    
        if (row == -1 && col == -1) {
            if (data[0] != GameView.BoardSpaceValues.EMPTY && data[0] == data[6] && data[0] == data[12]) {
                winningToken = data[0]
                diag = 0
                winningPosition1 = 0
                winningPosition2 = 6
                winningPosition3 = 12
            } else if (data[1] != GameView.BoardSpaceValues.EMPTY && data[1] == data[7] && data[1] == data[13]) {
                winningToken = data[1]
                diag = 2
                winningPosition1 = 1
                winningPosition2 = 7
                winningPosition3 = 13
            } else if (data[2] != GameView.BoardSpaceValues.EMPTY && data[2] == data[8] && data[2] == data[14]) {
                winningToken = data[2]
                diag = 3
                winningPosition1 = 2
                winningPosition2 = 8
                winningPosition3 = 14
            } else if (data[5] != GameView.BoardSpaceValues.EMPTY && data[5] == data[11] && data[5] == data[17]) {
                winningToken = data[5]
                diag = 4
                winningPosition1 = 5
                winningPosition2 = 11
                winningPosition3 = 17
            } else if (data[6] != GameView.BoardSpaceValues.EMPTY && data[6] == data[12] && data[6] == data[18]) {
                winningToken = data[6]
                diag = 0
                winningPosition1 = 6
                winningPosition2 = 12
                winningPosition3 = 18
            } else if (data[7] != GameView.BoardSpaceValues.EMPTY && data[7] == data[13] && data[7] == data[19]) {
                winningToken = data[7]
                diag = 2
                winningPosition1 = 7
                winningPosition2 = 13
                winningPosition3 = 19
            } else if (data[10] != GameView.BoardSpaceValues.EMPTY && data[10] == data[16] && data[10] == data[22]) {
                winningToken = data[10]
                diag = 5
                winningPosition1 = 10
                winningPosition2 = 16
                winningPosition3 = 22
            } else if (data[11] != GameView.BoardSpaceValues.EMPTY && data[11] == data[17] && data[11] == data[23]) {
                winningToken = data[11]
                diag = 4
                winningPosition1 = 11
                winningPosition2 = 17
                winningPosition3 = 23
            } else if (data[12] != GameView.BoardSpaceValues.EMPTY && data[12] == data[18] && data[12] == data[24]) {
                winningToken = data[12]
                diag = 0
                winningPosition1 = 12
                winningPosition2 = 18
                winningPosition3 = 24

                //check diagonals running from lower left to upper right
            } else if (data[2] != GameView.BoardSpaceValues.EMPTY && data[2] == data[6] && data[2] == data[10]) {
                winningToken = data[2]
                diag = 1
                winningPosition1 = 2
                winningPosition2 = 6
                winningPosition3 = 10
            } else if (data[3] != GameView.BoardSpaceValues.EMPTY && data[3] == data[7] && data[3] == data[11]) {
                winningToken = data[3]
                diag = 6
                winningPosition1 = 3
                winningPosition2 = 7
                winningPosition3 = 11
            } else if (data[4] != GameView.BoardSpaceValues.EMPTY && data[4] == data[8] && data[4] == data[12]) {
                winningToken = data[4]
                diag = 7
                winningPosition1 = 4
                winningPosition2 = 8
                winningPosition3 = 12
            } else if (data[9] != GameView.BoardSpaceValues.EMPTY && data[9] == data[13] && data[9] == data[17]) {
                winningToken = data[9]
                diag = 8
                winningPosition1 = 9
                winningPosition2 = 13
                winningPosition3 = 17
            } else if (data[14] != GameView.BoardSpaceValues.EMPTY && data[14] == data[18] && data[14] == data[22]) {
                winningToken = data[14]
                diag = 9
                winningPosition1 = 14
                winningPosition2 = 18
                winningPosition3 = 22
            } else if (data[7] != GameView.BoardSpaceValues.EMPTY && data[7] == data[11] && data[7] == data[15]) {
                winningToken = data[7]
                diag = 6
                winningPosition1 = 7
                winningPosition2 = 11
                winningPosition3 = 15
            } else if (data[8] != GameView.BoardSpaceValues.EMPTY && data[8] == data[12] && data[8] == data[16]) {
                winningToken = data[8]
                diag = 7
                winningPosition1 = 8
                winningPosition2 = 12
                winningPosition3 = 16
            } else if (data[12] != GameView.BoardSpaceValues.EMPTY && data[12] == data[16] && data[12] == data[20]) {
                winningToken = data[12]
                diag = 7
                winningPosition1 = 12
                winningPosition2 = 16
                winningPosition3 = 20
            } else if (data[13] != GameView.BoardSpaceValues.EMPTY && data[13] == data[17] && data[13] == data[21]) {
                winningToken = data[13]
                diag = 8
                winningPosition1 = 13
                winningPosition2 = 17
                winningPosition3 = 21
            }
        }
        val returnValue = IntArray(7)
        returnValue[0] = col
        returnValue[1] = row
        returnValue[2] = diag
        returnValue[3] = winningToken
        returnValue[4] = winningPosition1
        returnValue[5] = winningPosition2
        returnValue[6] = winningPosition3
        return returnValue
    }

    private fun testForWinner(data: IntArray, usePlayer2: Boolean): Boolean {
        val winnerFound = checkWinningPosition(data)

// For scoring purposes we will need to determine if the current player is the winner when the last card 
// was placed or if the opposing player is the winner.
// if the opposing player wins then more points are awarded to the opponent          
        var player: GameView.State? = null
        var currentPlayer = mGameView!!.currentPlayer
        if (usePlayer2) // if we're playing over a network then current player is always player 1 
            currentPlayer = GameView.State.PLAYER2 // so we need to add some extra logic to test winner on player 2 over the network
        if (winnerFound[3] > -1) {
            if (winnerFound[3] == mPlayer1TokenChoice) {
                player = GameView.State.PLAYER1
                var player1Score = mPlayer1Score
                if (HUMAN_VS_NETWORK) {
                    player1Score = mPlayer1NetworkScore
                }
                if (currentPlayer == GameView.State.PLAYER1) {
                    player1Score += mRegularWin
                    playHumanWinSound()
                    checkForPrizeWin(winnerFound[4], winnerFound[5], winnerFound[6], PrizeValue.REGULARPRIZE)
                } else {
                    player1Score += mSuperWin
                    playHumanWinShmoSound()
                    checkForPrizeWin(winnerFound[4], winnerFound[5], winnerFound[6], PrizeValue.SHMOPRIZE)
                }
                if (HUMAN_VS_NETWORK) {
                    mPlayer1NetworkScore = player1Score
                } else {
                    mPlayer1Score = player1Score
                }
            } else {
                player = GameView.State.PLAYER2
                var player2Score = mPlayer2Score
                if (HUMAN_VS_NETWORK) {
                    player2Score = mPlayer2NetworkScore
                }
                if (currentPlayer == GameView.State.PLAYER2) {
                    if (HUMAN_VS_HUMAN || HUMAN_VS_NETWORK) {
                        player2Score += mRegularWin
                        playHumanWinSound()
                    } else {
                        mWillyScore += mRegularWin
                        playWillyWinSound()
                    }
                } else {
                    if (HUMAN_VS_HUMAN || HUMAN_VS_NETWORK) {
                        player2Score += mSuperWin
                        playHumanWinShmoSound()
                    } else {
                        mWillyScore += mSuperWin
                        playWillyWinShmoSound()
                    }
                }
                if (HUMAN_VS_HUMAN) {
                    mPlayer2Score = player2Score
                }
                if (HUMAN_VS_NETWORK) {
                    mPlayer2NetworkScore = player2Score
                }
            }
        }
        if (winnerFound[0] != -1 || winnerFound[1] != -1 || winnerFound[2] != -1) {
            if (player != null) {
                setFinished(player, winnerFound[0], winnerFound[1], winnerFound[2])
            }
            return true
        }
        return false
    }

    private fun checkForPrizeWin(winningPosition1: Int, winningPosition2: Int, winningPosition3: Int, winType: Int) {
        if (mLastCellSelected == GameView.prizeLocation) {
            if (winType == PrizeValue.SHMOPRIZE) {
                showPrizeWon(PrizeValue.SHMOGRANDPRIZE)
            } else {
                showPrizeWon(PrizeValue.GRANDPRIZE)
            }
        } else if (GameView.prizeLocation == winningPosition1 || GameView.prizeLocation == winningPosition2 || GameView.prizeLocation == winningPosition3) {
            if (winType == PrizeValue.SHMOPRIZE) {
                showPrizeWon(PrizeValue.SHMOPRIZE)
            } else {
                showPrizeWon(PrizeValue.REGULARPRIZE)
            }
        }
    }

    private fun setFinished(player: GameView.State, col: Int, row: Int, diagonal: Int) {
        mGameView!!.currentPlayer = GameView.State.WIN
        mGameView!!.winner = player
        mGameView!!.isEnabled = false
        mGameView!!.setFinished(col, row, diagonal)
        setWinState(player)
        if (player == GameView.State.PLAYER2) {
            mGameView!!.invalidate()
        }
        displayScores()
    }

    private fun setWinState(player: GameView.State?) {
        mButtonNext!!.isEnabled = true
        val text: String
        var player1Name: String? = "Player 1"
        var player2Name: String? = "Player 2"
        if (mPlayer1Name != null) {
            player1Name = mPlayer1Name
        }
        if (mPlayer2Name != null) {
            player2Name = mPlayer2Name
        }
        text = if (player == GameView.State.EMPTY) {
            getString(R.string.tie)
        } else if (player == GameView.State.PLAYER1) {
            "$player1Name wins! Play Again?"
        } else {
            "$player2Name wins! Play Again?"
        }
        mButtonNext!!.text = text
        if (HUMAN_VS_NETWORK) {
            updateWebServerScore()
        }
        highlightCurrentPlayer(GameView.State.EMPTY)
        mGameView!!.setViewDisabled(true)
    }

    //FIXME - there's got to be some way to consolidate these sound methods into a single callable method
    // with a switch/case ?    
    private fun playFinishMoveSound() {
        if (!soundMode) {
            return
        }
        var soundFinishMove = MediaPlayer.create(applicationContext, R.raw.finish_move)
        if (soundFinishMove != null) {
            soundFinishMove.setOnCompletionListener { mp -> mp.release() }
            soundFinishMove.start()
        }
        soundFinishMove = null
    }

    private fun playHumanMoveSound() {
        if (!soundMode) {
            return
        }
        var soundHumanMove = MediaPlayer.create(applicationContext, R.raw.human_token_move_sound)
        if (soundHumanMove != null) {
            soundHumanMove.setOnCompletionListener { mp -> mp.release() }
            soundHumanMove.start()
        }
        soundHumanMove = null
    }

    private fun playComputerMoveSound() {
        if (!soundMode) {
            return
        }
        var soundComputerMove = MediaPlayer.create(applicationContext, R.raw.computer_token_move_sound)
        if (soundComputerMove != null) {
            soundComputerMove.setOnCompletionListener { mp -> mp.release() }
            soundComputerMove.start()
        }
        soundComputerMove = null
    }

    private fun playHumanWinSound() {
        if (!soundMode) {
            return
        }
        var soundHumanWin = MediaPlayer.create(applicationContext, R.raw.player_win)
        if (soundHumanWin != null) {
            soundHumanWin.setOnCompletionListener { mp -> mp.release() }
            soundHumanWin.start()
        }
        soundHumanWin = null
    }

    private fun playHumanWinShmoSound() {
        if (!soundMode) {
            return
        }
        var soundHumanWinShmo = MediaPlayer.create(applicationContext, R.raw.player_win_shmo)
        if (soundHumanWinShmo != null) {
            soundHumanWinShmo.setOnCompletionListener { mp -> mp.release() }
            soundHumanWinShmo.start()
        }
        soundHumanWinShmo = null
    }

    private fun playWillyWinSound() {
        if (!soundMode) {
            return
        }
        var soundWillyWin = MediaPlayer.create(applicationContext, R.raw.willy_win)
        if (soundWillyWin != null) {
            soundWillyWin.setOnCompletionListener { mp -> mp.release() }
            soundWillyWin.start()
        }
        soundWillyWin = null
    }

    private fun playWillyWinShmoSound() {
        if (!soundMode) {
            return
        }
        var soundWillyWinShmo = MediaPlayer.create(applicationContext, R.raw.willy_win_shmo)
        if (soundWillyWinShmo != null) {
            soundWillyWinShmo.setOnCompletionListener { mp -> mp.release() }
            soundWillyWinShmo.start()
        }
        soundWillyWinShmo = null
    }

    private fun editScore(score: Int): String {
        val formatter = DecimalFormat("0000")
        val formatScore = StringBuilder(formatter.format(score.toLong()))
        for (x in 0 until formatScore.length) {
            val testString = formatScore.substring(x, x + 1)
            if (testString == "0") {
                formatScore.replace(x, x + 1, " ")
            } else {
                break
            }
        }
        return formatScore.toString()
    }

    private fun displayScores() {
        if (HUMAN_VS_NETWORK) {
            mPlayer1ScoreTextValue!!.text = editScore(mPlayer1NetworkScore)
            mPlayer2ScoreTextValue!!.text = editScore(mPlayer2NetworkScore)
        } else if (HUMAN_VS_HUMAN) {
            mPlayer1ScoreTextValue!!.text = editScore(mPlayer1Score)
            mPlayer2ScoreTextValue!!.text = editScore(mPlayer2Score)
        } else {
            mPlayer1ScoreTextValue!!.text = editScore(mPlayer1Score)
            mPlayer2ScoreTextValue!!.text = editScore(mWillyScore)
        }
        mPlayer1NameTextValue!!.setText(mPlayer1Name)
        mPlayer2NameTextValue!!.setText(mPlayer2Name)
    }

    private fun showPlayerTokenChoice() {
        if (mPlayer1TokenChoice == GameView.BoardSpaceValues.CROSS) {
            mGameTokenPlayer1!!.setImageResource(R.drawable.cross_small)
        } else if (mPlayer1TokenChoice == GameView.BoardSpaceValues.CIRCLE) {
            mGameTokenPlayer1!!.setImageResource(R.drawable.circle_small)
        } else {
            mGameTokenPlayer1!!.setImageResource(R.drawable.reset_token_selection)
        }
        if (mPlayer2TokenChoice == GameView.BoardSpaceValues.CROSS) {
            mGameTokenPlayer2!!.setImageResource(R.drawable.cross_small)
        } else if (mPlayer2TokenChoice == GameView.BoardSpaceValues.CIRCLE) {
            mGameTokenPlayer2!!.setImageResource(R.drawable.circle_small)
        } else {
            mGameTokenPlayer2!!.setImageResource(R.drawable.reset_token_selection)
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putInt("ga_player1_token_choice", mPlayer1TokenChoice)
        savedInstanceState.putInt("ga_player2_token_choice", mPlayer2TokenChoice)
        savedInstanceState.putInt("ga_player1_score", mPlayer1Score)
        savedInstanceState.putInt("ga_player2_score", mPlayer2Score)
        savedInstanceState.putInt("ga_willy_score", mWillyScore)
        savedInstanceState.putString("ga_button", mButtonNext!!.text.toString())
        savedInstanceState.putBoolean("ga_move_mode", moveModeTouch)
        savedInstanceState.putBoolean("ga_sound_mode", soundMode)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.	
        mPlayer1TokenChoice = savedInstanceState.getInt("ga_player1_token_choice")
        mPlayer2TokenChoice = savedInstanceState.getInt("ga_player2_token_choice")
        mPlayer1Score = savedInstanceState.getInt("ga_player1_score")
        mPlayer2Score = savedInstanceState.getInt("ga_player2_score")
        mWillyScore = savedInstanceState.getInt("ga_willy_score")
        var workString = savedInstanceState.getString("ga_button")
        mButtonNext!!.text = workString
        if (!mButtonNext!!.text.toString().endsWith("Play Again?")) {
            mButtonNext!!.isEnabled = false
        }
        moveModeTouch = savedInstanceState.getBoolean("ga_move_mode")
        soundMode = savedInstanceState.getBoolean("ga_sound_mode")
    }

    override fun onStop() {
        super.onStop()
        val settings = getSharedPreferences(UserPreferences.PREFS_NAME, 0)
        val editor = settings.edit()
        editor.putInt(PLAYER1_SCORE, mPlayer1Score)
        editor.putInt(PLAYER2_SCORE, mPlayer2Score)
        editor.putInt(WILLY_SCORE, mWillyScore)
        editor.apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        writeToLog("GameActivity", "GameActivity onDestroy() called")
    }

    protected inner class ServerThread internal constructor() : Thread() {
        private var mMessageToClient: String? = null
        var boardPosition = 0
            private set
        var tokenMoved = 0
            private set
        //var mGameStarted = false

        fun setMessageToClient(newMessage: String?) {
            mMessageToClient = newMessage
        }

        private fun parseLine(line: String) {
            val moveValues: Array<String?> = line.split(",".toRegex()).toTypedArray()
            if (moveValues[1] != null) {
                tokenMoved = moveValues[1]!!.trim { it <= ' ' }.toInt()
            }
            if (moveValues[2] != null) {
                boardPosition = moveValues[2]!!.trim { it <= ' ' }.toInt()
            }
            if (moveValues[3] != null) {
                val player1TokenChoice = moveValues[3]!!.trim { it <= ' ' }.toInt()
                if (player1TokenChoice > -1) {
                    mPlayer2TokenChoice = player1TokenChoice
                    mPlayer1TokenChoice = if (mPlayer2TokenChoice == GameView.BoardSpaceValues.CIRCLE) GameView.BoardSpaceValues.CROSS else GameView.BoardSpaceValues.CIRCLE
                    mHandler.sendEmptyMessage(MSG_NETWORK_SET_TOKEN_CHOICE)
                }
            }
        }

        override fun run() {
            try {
                writeToLog("ServerThread", "server run method started")
                mPlayer2NetworkScore = 0
                mPlayer1NetworkScore = mPlayer2NetworkScore
                mGameStarted = false
                while (mServerRunning) {
                    if (mRabbitMQServerResponseHandler!!.rabbitMQResponse != null) {
                        writeToLog("ServerThread", "Retrieving command: " + mRabbitMQServerResponseHandler!!.rabbitMQResponse)
                        if (mRabbitMQServerResponseHandler!!.rabbitMQResponse!!.contains("tokenList")) {
                            getGameSetUpFromClient(mRabbitMQServerResponseHandler!!.rabbitMQResponse!!)
                            mHandler.sendEmptyMessage(DISMISS_WAIT_FOR_NEW_GAME_FROM_CLIENT)
                            mHandler.sendEmptyMessage(ACCEPT_INCOMING_GAME_REQUEST_FROM_CLIENT)
                            mGameStarted = true
                        }
                        if (mRabbitMQServerResponseHandler!!.rabbitMQResponse!!.startsWith("moved")) {
                            parseLine(mRabbitMQServerResponseHandler!!.rabbitMQResponse!!)
                            mHandler.sendEmptyMessage(MSG_NETWORK_SERVER_TURN)
                        }
                        if (mRabbitMQServerResponseHandler!!.rabbitMQResponse!!.startsWith("leftGame") && mGameStarted) {
                            playerNotPlaying("client", mRabbitMQServerResponseHandler!!.rabbitMQResponse!!, 1)
                            mGameStarted = false
                            //mServerRunning = false
                        }
                        mRabbitMQServerResponseHandler!!.rabbitMQResponse = null
                    }
                    if (mMessageToClient != null) {
                        var messageToBeSent = mMessageToClient //circumvent sending a null message to sendMessageToRabbitMQTask
                        writeToLog("ServerThread", "Server about to respond to client: $messageToBeSent")
                        val qName = mQueuePrefix + "-" + "client" + "-" + player2Id
                        CoroutineScope( Dispatchers.Default).launch {
                            val sendMessageToRabbitMQTask = SendMessageToRabbitMQTask()
                            sendMessageToRabbitMQTask.main(mHostName, qName,
                                messageToBeSent!!, this@GameActivity as ToastMessage, Companion.resources)
                        }
                        writeToLog("ServerThread", "Server responded to client completed: messageToBeSent queue: $qName")
                        if (messageToBeSent!!.startsWith("leftGame") || messageToBeSent!!.startsWith("noPlay")) {
                            mServerRunning = false
                            imServing = false
                        }
                        mMessageToClient = null
                    }
                    sleep(THREAD_SLEEP_INTERVAL.toLong())
                } // while end
                mPlayer2NetworkScore = 0
                mPlayer1NetworkScore = mPlayer2NetworkScore
                imServing = false
            } catch (e: Exception) {
                //e.printStackTrace();
                writeToLog("ServerThread", "error in Server Thread: " + e.message)
                sendToastMessage(e.message)
            } finally {
                mServerRunning = false
                imServing = false
                mPlayer2NetworkScore = 0
                mPlayer1NetworkScore = mPlayer2NetworkScore
                mServerThread = null

                val urlData = "/gamePlayer/update/?id=" + mPlayer1Id + "&onlineNow=false&playingNow=false&opponentId=0"
                SendMessageToWillyShmoServer().execute(urlData, null, this@GameActivity, Companion.resources, java.lang.Boolean.FALSE)
                writeToLog("ServerThread", "Server about to call DisposeRabbitMQTask()")
                CoroutineScope(Dispatchers.Default).launch {
                    val disposeRabbitMQTask = DisposeRabbitMQTask()
                    disposeRabbitMQTask.main(mMessageServerConsumer, resources, this@GameActivity as ToastMessage)
                }
                writeToLog("ServerThread", "server finished")
            }
        }
    }

    fun playerNotPlaying(clientOrServer: String, line: String, reason: Int) {
        val playerName: Array<String?> = line.split(",".toRegex()).toTypedArray()
        if (playerName[1] != null) {
            mNetworkOpponentPlayerName = playerName[1]
        }
        if (clientOrServer.startsWith("server")) {
            when (reason) {
                0 -> mHandler.sendEmptyMessage(MSG_NETWORK_SERVER_REFUSED_GAME)
                1 -> mHandler.sendEmptyMessage(MSG_NETWORK_SERVER_LEFT_GAME)
            }
        } else {
            when (reason) {
                0 -> mHandler.sendEmptyMessage(MSG_NETWORK_CLIENT_REFUSED_GAME)
                1 -> mHandler.sendEmptyMessage(MSG_NETWORK_CLIENT_LEFT_GAME)
            }
        }
    }

    inner class ClientThread internal constructor() : Thread() {
        private var mMessageToServer: String? = null
        var boardPosition = 0
            //private set
        var tokenMoved = 0
            //private set

        //        public List<int[]> mTokensFromClient;
        private var mGameStarted = false
        fun setGameStarted(gameStarted: Boolean) {
            mGameStarted = gameStarted
        }

        val player1Id: String
            get() = Integer.toString(mPlayer1Id)

        val player1Name: String?
            get() = mPlayer1Name

        fun setMessageToServer(newMessage: String?) {
            mMessageToServer = newMessage
        }

        private fun parseMove(line: String) {
            val moveValues: Array<String?> = line.split(",".toRegex()).toTypedArray()
            if (moveValues[1] != null) tokenMoved = moveValues[1]!!.trim { it <= ' ' }.toInt()
            if (moveValues[2] != null) boardPosition = moveValues[2]!!.trim { it <= ' ' }.toInt()
            if (moveValues[3] != null) {
                val player1TokenChoice = moveValues[3]!!.trim { it <= ' ' }.toInt()
                if (player1TokenChoice > -1) {
                    mPlayer2TokenChoice = player1TokenChoice
                    mPlayer1TokenChoice = if (mPlayer2TokenChoice == GameView.BoardSpaceValues.CIRCLE) GameView.BoardSpaceValues.CROSS else GameView.BoardSpaceValues.CIRCLE
                    mHandler.sendEmptyMessage(MSG_NETWORK_SET_TOKEN_CHOICE)
                }
            }
        }

        override fun run() {
            try {
                writeToLog("ClientService", "client run method started")
                while (mClientRunning) {
                    if (mMessageToServer != null) {
                        var messageToBeSent = mMessageToServer //circumvent sending a null message to sendMessageToRabbitMQTask
                        val qName = mQueuePrefix + "-" + "server" + "-" + player2Id
                        CoroutineScope( Dispatchers.Default).launch {
                            val sendMessageToRabbitMQTask = SendMessageToRabbitMQTask()
                            sendMessageToRabbitMQTask.main(mHostName, qName,
                                messageToBeSent!!, this@GameActivity as ToastMessage, Companion.resources)
                        }
                        writeToLog("ClientThread", "Sending command: $messageToBeSent queue: $qName")
                        if (messageToBeSent!!.startsWith("leftGame")) {
                            mClientRunning = false
                        }
                        mMessageToServer = null
                    }
                    if (mRabbitMQClientResponseHandler!!.rabbitMQResponse == null)
                        continue
                     else {
                        writeToLog("ClientThread", "read response: " + mRabbitMQClientResponseHandler!!.rabbitMQResponse)
                        if (mClientWaitDialog != null ) { //&&  mRabbitMQClientResponseHandler!!.rabbitMQResponse!!.startsWith("clientStarting")) {
                            mHandler.sendEmptyMessage(DISMISS_WAIT_FOR_NEW_GAME_FROM_SERVER)
                        }
                        if (mRabbitMQClientResponseHandler!!.rabbitMQResponse!!.startsWith("moved")) {
                            parseMove(mRabbitMQClientResponseHandler!!.rabbitMQResponse!!)
                            mHandler.sendEmptyMessage(MSG_NETWORK_CLIENT_TURN)
                            mGameStarted = true
                        }
                        if (mRabbitMQClientResponseHandler!!.rabbitMQResponse!!.startsWith("moveFirst")) {
                            mHandler.sendEmptyMessage(MSG_NETWORK_CLIENT_MAKE_FIRST_MOVE)
                            mGameStarted = true
                        }
                        if (mRabbitMQClientResponseHandler!!.rabbitMQResponse!!.startsWith("noPlay")) {
                            playerNotPlaying("server", mRabbitMQClientResponseHandler!!.rabbitMQResponse!!, 0)
                            mGameStarted = false
                        }
                        if (mRabbitMQClientResponseHandler!!.rabbitMQResponse!!.startsWith("leftGame") && mGameStarted) {
                            playerNotPlaying("server", mRabbitMQClientResponseHandler!!.rabbitMQResponse!!, 1)
                            mGameStarted = false
                        }
                        mRabbitMQClientResponseHandler!!.rabbitMQResponse = null // .rabbitMQResponse(null)
                    }
                    sleep(THREAD_SLEEP_INTERVAL.toLong())
                }
                writeToLog("ClientThread", "client run method finished")
            } catch (e: Exception) {
                //writeToLog("ClientService", "Client error 2: "+e);
                sendToastMessage(e.message)
            } finally {
                val urlData = "/gamePlayer/update/?id=" + mPlayer1Id + "&playingNow=false&onlineNow=false&opponentId=0"
                SendMessageToWillyShmoServer().execute(urlData, null, this@GameActivity, Companion.resources, java.lang.Boolean.FALSE)
                mPlayer2NetworkScore = 0
                mPlayer1NetworkScore = mPlayer2NetworkScore
                mClientRunning = false
                mClientThread = null
                writeToLog("ClientThread", "Client about to call DisposeRabbitMQTask()")
                CoroutineScope(Dispatchers.Default).launch {
                    val disposeRabbitMQTask = DisposeRabbitMQTask()
                    disposeRabbitMQTask.main(mMessageClientConsumer, resources, this@GameActivity as ToastMessage)
                }
                writeToLog("ClientThread", "client run method finally done")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (mClientRunning) {
            mClientThread!!.setMessageToServer("leftGame, " + mPlayer1Name)
        }
        if (imServing) {
            mServerThread!!.setMessageToClient("leftGame, " + mPlayer1Name)
        } else if (mServerRunning) {
            mServerRunning = false
        }
        writeToLog("GameActivity", "onPause called")
    }

    private fun getGameSetUpFromClient(gameSetUp: String) {
        try {
            mPlayer1TokenChoice = GameView.BoardSpaceValues.EMPTY
            mPlayer2TokenChoice = GameView.BoardSpaceValues.EMPTY // computer or opponent
            mTokensFromClient = ArrayList()
            val jsonObject = JSONObject(gameSetUp)
            val tokenArray = jsonObject.getJSONArray("tokenList")
            for (y in 0 until tokenArray.length()) {
                val tokenValues = tokenArray.getJSONObject(y)
                val tokenId = tokenValues.getString("tokenId")
                val tokenType = tokenValues.getString("tokenType")
                val tokenIntValue = tokenId.toInt()
                val tokenIntType = tokenType.toInt()
                if (tokenIntValue < 8) {
                    var resultValue = -1
                    resultValue = if (tokenIntValue > 3) {
                        tokenIntValue - 4
                    } else {
                        tokenIntValue + 4
                    }
                    (mTokensFromClient as ArrayList<IntArray>).add(intArrayOf(resultValue, tokenIntType))
                } else {
                    (mTokensFromClient as ArrayList<IntArray>).add(intArrayOf(GameView.BoardSpaceValues.BOARDCENTER, tokenIntType))
                }
            }
            mPlayer2Name = jsonObject.getString("player1Name")
            player2Id = jsonObject.getString("player1Id")
        } catch (e: JSONException) {
            sendToastMessage(e.message)
        }
    }

    private fun setGameRequestFromClient(start: Boolean) {
        mServerHasOpponent = null
        var urlData = "/gamePlayer/update/?playingNow=true&id=" + mPlayer1Id + "&opponentId=" + player2Id
        if (start) {
            mHandler.sendEmptyMessage(NEW_GAME_FROM_CLIENT)
            imServing = true
            mServerThread!!.setMessageToClient("serverAccepted")
        } else {
            urlData = "/gamePlayer/update/?id=" + mPlayer1Id + "&playingNow=false&onlineNow=false&opponentId=0"
            mPlayer2NetworkScore = 0
            mPlayer1NetworkScore = mPlayer2NetworkScore
            mPlayer2Name = null
            if (mServerThread != null) {
                mServerThread!!.setMessageToClient("noPlay, " + mPlayer1Name)
            }
        }

        //TODO - replace GameActivity.this with a static reference to getContext() set at class instantiation
        SendMessageToWillyShmoServer().execute(urlData, null, this@GameActivity, Companion.resources, !start)
    }

    private val newNetworkGameHandler = Handler(Looper.getMainLooper()) { msg -> // Your code logic goes here.
        when (msg.what) {
            ACCEPT_GAME -> setGameRequestFromClient(true)
            REJECT_GAME -> setGameRequestFromClient(false)
        }
        true
    }

    private fun acceptIncomingGameRequestFromClient() {
        if (this@GameActivity == null) { //I'm hoping we can get rid of this
            writeToLog("GameActivity", "acceptIncomingGameRequestFromClient() GameActivity is null!!!")
            return
        }
        val acceptMsg = Message.obtain()
        acceptMsg.target = newNetworkGameHandler
        acceptMsg.what = ACCEPT_GAME
        val rejectMsg = Message.obtain()
        rejectMsg.target = newNetworkGameHandler
        rejectMsg.what = REJECT_GAME
        try {
            AlertDialog.Builder(this@GameActivity)
                .setTitle(mPlayer2Name + " would like to play")
                .setPositiveButton("Accept") { dialog, which -> acceptMsg.sendToTarget() }
                .setCancelable(true)
                .setIcon(R.drawable.willy_shmo_small_icon)
                .setNegativeButton("Reject") { dialog, which -> rejectMsg.sendToTarget() }
                .show()
        } catch (e: Exception) {
            sendToastMessage(e.message)
        }
    }

    private fun displayHostNotAvailableAlert() {
        try {
            AlertDialog.Builder(this@GameActivity)
                .setTitle(mPlayer2Name + " client side has left the game")
                .setNeutralButton("OK") { dialog, which -> finish() }
                .setIcon(R.drawable.willy_shmo_small_icon)
                .show()
        } catch (e: Exception) {
            sendToastMessage(e.message)
        }
    }

    private fun displayServerRefusedGameAlert(serverPlayerName: String?) {
        try {
            AlertDialog.Builder(this@GameActivity)
                .setIcon(R.drawable.willy_shmo_small_icon)
                .setTitle("Sorry, $serverPlayerName server side doesn't want to play now")
                .setNeutralButton("OK") { dialog, which -> finish() }
                .show()
        } catch (e: Exception) {
            sendToastMessage(e.message)
        }
    }

    private fun displayOpponentLeftGameAlert(clientOrServer: String, serverPlayerName: String?) {
        try {
            AlertDialog.Builder(this@GameActivity)
                .setIcon(R.drawable.willy_shmo_small_icon)
                .setTitle("Sorry, $serverPlayerName $clientOrServer side has left the game")
                .setNeutralButton("OK") { dialog, which -> finish() }
                .show()
        } catch (e: Exception) {
            sendToastMessage(e.message)
        }
    }

    private fun updateWebServerScore() {
        val urlData = "/gamePlayer/updateGamesPlayed/?id=" + mPlayer1Id + "&score=" + mPlayer1NetworkScore
        SendMessageToWillyShmoServer().execute(urlData, null, this@GameActivity, Companion.resources, java.lang.Boolean.FALSE)
    }

    inner class ErrorHandler : Handler() {
        override fun handleMessage(msg: Message) {
            Toast.makeText(applicationContext, msg.obj as String, Toast.LENGTH_LONG).show()
        }
    }

    override fun sendToastMessage(message: String?) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    }

    private inner class RabbitMQServerResponseHandler : RabbitMQResponseHandler() { }
    private inner class RabbitMQClientResponseHandler : RabbitMQResponseHandler() { }
    private inner class RabbitMQStartGameResponseHandler : RabbitMQResponseHandler() { }

    private fun setUpMessageConsumer(rabbitMQMessageConsumer: RabbitMQMessageConsumer, qNameQualifier: String, rabbitMQResponseHandler: RabbitMQResponseHandler) {
        val qName = mQueuePrefix + "-" + qNameQualifier + "-" + mPlayer1Id
        //ConsumerConnectTask().execute(mHostName, rabbitMQMessageConsumer, qName, this@GameActivity, Companion.resources, "GameActivity")
        CoroutineScope(Dispatchers.Default).launch {
            val consumerConnectTask = ConsumerConnectTask()
            consumerConnectTask.main(
                getConfigMap("RabbitMQIpAddress"),
                rabbitMQMessageConsumer,
                qName,
                this@GameActivity,
                resources,
                "GameActivity"
            )
        }
        writeToLog("GameActivity", "$qNameQualifier message consumer listening on queue: $qName")
        // register for messages
        rabbitMQMessageConsumer.setOnReceiveMessageHandler(object : OnReceiveMessageHandler {
            override fun onReceiveMessage(message: ByteArray?) {
                var text = String(message!!, StandardCharsets.UTF_8)
                rabbitMQResponseHandler.rabbitMQResponse = text
                writeToLog("GameActivity", "$qNameQualifier OnReceiveMessageHandler received message: $text")
                if (text.startsWith("letsPlay")) { //we should never see a letsPlay message here!!!!!
                    writeToLog("GameActivity", "About to stopService on PlayersOnlineActivity")
                }
            }
        })
    }

    fun sendMessageToServerHost(message: String) {
        val qName = mQueuePrefix + "-" + "server" + "-" + player2Id
        CoroutineScope(Dispatchers.Default).launch {
            val sendMessageToRabbitMQTask = SendMessageToRabbitMQTask()
            sendMessageToRabbitMQTask.main(mHostName, qName,  message, this@GameActivity as ToastMessage, Companion.resources)
        }
        writeToLog("GameActivity", "sendMessageToServerHost: $message queue: $qName")
    }

    // to update the game count:
    // http://ww2.guzzardo.com:8081/WillyShmoGrails/gamePlayer/updateGamesPlayed/?id=1
    fun showPrizeWon(prizeType: Int) {
        try {
            AlertDialog.Builder(this@GameActivity)
                .setTitle("Congratulations, you won a prize!")
                .setPositiveButton("Accept") { dialog, which ->
                    val i = Intent(this@GameActivity, PrizesAvailableActivity::class.java)
                    startActivity(i)
                }
                .setCancelable(true)
                .setIcon(R.drawable.willy_shmo_small_icon)
                .setNegativeButton("Reject") { dialog, which -> }
                .show()
        } catch (e: Exception) {
            //e.printStackTrace();
            sendToastMessage(e.message)
        }
    }

    companion object {
        var errorHandler: ErrorHandler? = null
        private const val packageName = "com.guzzardo.android.willyshmo.kotlintictacdoh"
        /* Start player. Must be 1 or 2. Default is 1.  */
        const val EXTRA_START_PLAYER = packageName + ".GameActivity.EXTRA_START_PLAYER"
        const val START_PLAYER_HUMAN = packageName + ".GameActivity.START_PLAYER_HUMAN"
        const val PLAYER1_NAME = packageName + ".GameActivity.PLAYER1_NAME"
        const val PLAYER2_NAME = packageName + ".GameActivity.PLAYER2_NAME"
        const val PLAYER1_SCORE = packageName + ".GameActivity.PLAYER1_SCORE"
        const val PLAYER2_SCORE = packageName + ".GameActivity.PLAYER2_SCORE"
        const val WILLY_SCORE = packageName + ".GameActivity.WILLY_SCORE"
        const val START_SERVER = packageName + ".GameActivity.START_SERVER"
        const val START_CLIENT = packageName + ".GameActivity.START_CLIENT"
        const val START_CLIENT_OPPONENT_ID = packageName + ".GameActivity.START_CLIENT_OPPONENT_ID"
        const val PLAYER1_ID = packageName + ".GameActivity.PLAYER1_ID"
        const val START_OVER_LAN = packageName + ".GameActivity.START_OVER_LAN"
        const val MOVE_MODE = packageName + ".GameActivity.MOVE_MODE"
        const val SOUND_MODE = packageName + ".GameActivity.SOUND_MODE"
        const val TOKEN_SIZE = packageName + ".GameActivity.TOKEN_SIZE"
        const val TOKEN_COLOR_1 = packageName + ".GameActivity.TOKEN_COLOR_1"
        const val TOKEN_COLOR_2 = packageName + ".GameActivity.TOKEN_COLOR_2"
        const val HAVE_OPPONENT = packageName + ".GameActivity.HAVE_OPPONENT"
        const val START_FROM_PLAYER_LIST = packageName + ".GameActivity.START_FROM_PLAYER_LIST"
        private const val MSG_COMPUTER_TURN = 1
        private const val NEW_GAME_FROM_CLIENT = 2
        private const val MSG_NETWORK_CLIENT_TURN = 3
        private const val MSG_NETWORK_SERVER_TURN = 4
        private const val MSG_NETWORK_SET_TOKEN_CHOICE = 5
        private const val DISMISS_WAIT_FOR_NEW_GAME_FROM_CLIENT = 6
        private const val DISMISS_WAIT_FOR_NEW_GAME_FROM_SERVER = 7
        private const val ACCEPT_INCOMING_GAME_REQUEST_FROM_CLIENT = 8
        private const val MSG_NETWORK_CLIENT_MAKE_FIRST_MOVE = 9
        private const val MSG_HOST_UNAVAILABLE = 10
        private const val MSG_NETWORK_SERVER_REFUSED_GAME = 11
        private const val MSG_NETWORK_SERVER_LEFT_GAME = 12
        private const val MSG_NETWORK_CLIENT_REFUSED_GAME = 13
        private const val MSG_NETWORK_CLIENT_LEFT_GAME = 14
        private const val COMPUTER_DELAY_MS: Long = 500
        private const val THREAD_SLEEP_INTERVAL = 300 //milliseconds
        private const val mRegularWin = 10
        private const val mSuperWin = 30
        private var mPlayer1Id = 0
        var player2Id: String? = null
        private var mGameView: GameView? = null
        private var mPlayer1Score = 0
        private var mPlayer2Score = 0
        private var mWillyScore = 0
        private var mPlayer1NetworkScore = 0
        private var mPlayer2NetworkScore = 0
        private var mPlayer1Name: String? = null
        private var mPlayer2Name: String? = null
        var moveModeTouch  = false //false = drag move mode; true = touch move mode
        var soundMode = false //false = no sound; true = sound
        private var HUMAN_VS_HUMAN = false
        private var HUMAN_VS_NETWORK = false
        private var mSavedCell = 0 //hack for saving cell selected when XO token is chosen as first move
        private var mButtonStartText: CharSequence? = null
        private var mServerRunning = false
        private var mClientRunning = false
        private var imServing = false
        private var mBallMoved = 0 //hack for correcting problem with resetting mBallId to -1 in mGameView.disableBall()
        private var resources: Resources? = null
        private var mHostWaitDialog: AlertDialog? = null
        private var mClientWaitDialog: AlertDialog? = null
        private var mChooseTokenDialog: AlertDialog? = null
        private var mNetworkOpponentPlayerName: String? = null
        private var mLastCellSelected = 0
        private var mHostName: String? = null
        private var mQueuePrefix: String? = null
        private const val ACCEPT_GAME = 1
        private const val REJECT_GAME = 2
        private var mGameStarted = false

        private fun writeToLog(filter: String, msg: String) {
            if ("true".equals(resources!!.getString(R.string.debug), ignoreCase = true)) {
                Log.d(filter, msg)
            }
        }
        @JvmStatic
        var isClientRunning: Boolean
            get() = mClientRunning
            set(clientRunning) {
                mClientRunning = clientRunning
            }

        @JvmStatic
        var isGameStarted: Boolean = false
            get() = mGameStarted
    }
}