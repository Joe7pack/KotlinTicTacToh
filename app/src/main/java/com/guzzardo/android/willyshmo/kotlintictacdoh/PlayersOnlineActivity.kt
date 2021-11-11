package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.ListFragment
import com.guzzardo.android.willyshmo.kotlintictacdoh.MainActivity.UserPreferences
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.getConfigMap
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

/*
We can start GameActivity from PlayersOnlineActivity 3 different ways:

1. In onCreate, if there are no other users online, starts server only - Cannabis.
   In this scenario, GameActivity will listen on queue test-client-xx.

2. When an opposing player is selected from the Fragment List, starts client and server - Dr. Strangelove.
   In this scenario PlayersOnlineActivity will send a letsPlay message to the opposing player's PlayersOnlineActivity.

3. When a letsPlay message is received from an opposing player, starts server only - Shakespeare.

Only PlayersOnlineActivity is primed to accept messages using the qName prefix of test-playerList-XX where XX is the PlayerID of the opposing player.

Note - letsPlay message is sent from one place only, PlayersOnlineFragment and is accepted only by PlayersOnlineActivity.
leftGame only comes from onPause in GameActivity.

Willy Shmo play over web messaging sequence:

1. Player selects name from list of users online - client sends letsPlay message to server from PlayersOnlineActivity
2. Server picks up this message in PlayersOnlineActivity
3. client sends tokenList to server:
   PlayersOnlineActivity sends intent to GameActivity: intent.putExtra(GameActivity.START_CLIENT, "true")
   This will send the new game to the client in a tokenList message.

If no one else is online, then starting a new game will start the server side, display the game board and wait for a tokenList message from the opponent.

D/SendMessageToRabbitMQTask: message: letsPlay,Evelyn,81, 738, 2021-08-08 22:54:22 to queue: test-startGame-90
D/GameActivity: client message consumer listening on queue: test-client-81
D/PlayersOnlineActivity: startGame message consumer listening on queue: test-startGame-81

Message sequence:
D/GameActivity: server OnReceiveMessageHandler received message: {"tokenList"... - received from opposing player
D/ServerThread: Server responded to client completed, queue: test-client-202, message: serverAccepted
GameActivity commences game play
*/

class PlayersOnlineActivity : FragmentActivity(), ToastMessage {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSavedInstanceState = savedInstanceState
        mResources = resources
        mErrorHandler = ErrorHandler()
        mApplicationContext = applicationContext
        mCallerActivity = this
        sharedPreferences
        mPlayersOnlineActivity = this
        mPlayer1Name = intent.getStringExtra(GameActivity.PLAYER1_NAME)

        if (mUsersOnline.isNullOrEmpty()) {
            writeToLog("PlayersOnlineActivity", "onCreate() mUsersOnline == null")
            sendToastMessage("Sorry unable to retrieve Users online, please try again")
            finish()
        } else {
            playersOnline
            setNumberOfPlayersOnline()
            if (mUserNames.isEmpty()) {
                writeToLog("PlayersOnlineActivity", "starting server only")
                val intent = Intent(mApplicationContext, GameActivity::class.java)
                val item = GameActivity.ParcelItems(123456789, "Cannabis")
                intent.putExtra(GameActivity.PARCELABLE_VALUES, item)
                intent.putExtra(GameActivity.START_SERVER, "true")
                intent.putExtra(GameActivity.PLAYER1_ID, mPlayer1Id)
                intent.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name)
                startActivity(intent)
                finish()
            } else {
                mMessageConsumer = RabbitMQMessageConsumer(this, mResources)
                mMessageConsumer!!.setUpMessageConsumer("playerList", mPlayer1Id, this, mResources, "PlayersOnlineActivity")
                mMessageConsumer!!.setOnReceiveMessageHandler(object :
                    RabbitMQMessageConsumer.OnReceiveMessageHandler {
                    override fun onReceiveMessage(message: ByteArray?) {
                        try {
                            val text = String(message!!, StandardCharsets.UTF_8)
                            writeToLog("PlayersOnlineActivity", "OnReceiveMessageHandler has received message: $text")
                            handleRabbitMQMessage(text)
                        } catch (e: Exception) {
                            writeToLog("PlayersOnlineActivity", "exception in onReceiveMessage: $e")
                        }
                    } // end onReceiveMessage
                }) // end setOnReceiveMessageHandler
                setContentView(R.layout.players_online_fragment) //this starts up the list view
            }
            setContext(this)
            writeToLog("PlayersOnlineActivity", "onCreate taskId: $taskId")
        }
    }

    private fun handleRabbitMQMessage(message: String) {
        if (mRabbitMQResponse.equals(message)) {
            writeToLog("PlayersOnlineActivity", "================> handleRabbitMQMessage() returning due to duplicate message: $message \n\n")
            return
        }
        mRabbitMQResponse = message
        if (message.startsWith("letsPlay")) {
            val playStringArray = message.split(",")
            if (playStringArray.size >= 3) {
                val opposingPlayerId = playStringArray[2]
                val opposingPlayerName = playStringArray[1]
                val i = Intent(mApplicationContext, GameActivity::class.java)

                val item = GameActivity.ParcelItems(123456789, "Shakespeare")
                i.putExtra(GameActivity.PARCELABLE_VALUES, item)

                i.putExtra(GameActivity.START_SERVER, "true")
                i.putExtra(GameActivity.PLAYER1_ID, mPlayer1Id)
                i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name)
                i.putExtra(GameActivity.START_CLIENT_OPPONENT_ID, opposingPlayerId)
                i.putExtra(GameActivity.PLAYER2_NAME, opposingPlayerName)
                i.putExtra(GameActivity.START_FROM_PLAYER_LIST, "true")
                writeToLog("PlayersOnlineActivity", "starting client and server from new player: $opposingPlayerName")
                startActivity(appContext, i, null)
            }
            writeToLog("PlayersOnlineActivity", "got LetsPlay response received message: $message at: " +
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
            )
        }
        finish()
    }

    public override fun onPause() {
        super.onPause()
        writeToLog("PlayersOnlineActivity", "onPause called from Main Activity")
        if (mSelectedPosition == -1) {
            val urlData = "/gamePlayer/update/?id=$mPlayer1Id&onlineNow=false&opponentId=0&userName=$mPlayer1Name"
            val messageResponse = CoroutineScope(Dispatchers.Default).async {
                SendMessageToAppServer.main(urlData, mPlayersOnlineActivity as ToastMessage, mResources, false)
            }
            writeToLog("PlayersOnlineActivity", "onPause from Main Activity called to set onlineNow to false $messageResponse")
        }
        writeToLog("PlayersOnlineActivity", "onPause all done")
    }

    override fun onResume() { //only called when at least one opponent is online to select
        super.onResume()
        writeToLog("PlayersOnlineActivity", "onResume called from PlayersOnlineActivity")
    }

    override fun onStop() {
        super.onStop()
        writeToLog("PlayersOnlineActivity", "onStop called from main class")
    }

    override fun onDestroy() {
        super.onDestroy()
        writeToLog("PlayersOnlineActivity", "onDestroy called from main class")
    }

    /**
     * This is the "top-level" fragment, showing a list of items that the
     * user can pick.  Upon picking an item, it takes care of displaying the
     * data to the user as appropriate based on the current UI layout.
     */
    class PlayersOnlineFragment : ListFragment(), ToastMessage {
        private var mCurCheckPosition = 0
        private var mAlreadySelected = false

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            val view: View = inflater.inflate(R.layout.player_list_content, container, false)
            val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
                inflater.context,
                android.R.layout.simple_list_item_1,
                mUserNames
            )
            listAdapter = adapter
            return view
        }

        override fun onResume() { //only called when at least one opponent is online to select
            super.onResume()
            writeToLog("PlayersOnlineFragment", "onResume called from PlayersOnlineFragment")
            mSelectedPosition = -1
        }

        override fun onPause() { // pause the PlayersOnlineFragment
            super.onPause()
            writeToLog("PlayersOnlineFragment", "onPause completed from PlayersOnlineFragment mSelectedPosition = $mSelectedPosition")
        }

        override fun onStop() {
            super.onStop()
            writeToLog("PlayersOnlineFragment", "onStop called from PlayersOnlineFragment")
        }

        override fun onDestroy() {
            super.onDestroy()
            writeToLog("PlayersOnlineFragment", "onDestroy called")
        }

        override fun onDestroyView() {
            super.onDestroyView()
            writeToLog("PlayersOnlineFragment", "onDestroyView() called")
        }

        override fun onDestroyOptionsMenu() {
            super.onDestroyOptionsMenu()
        }

        override fun onDetach() {
            super.onDetach()
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            outState.putInt("curChoice", mCurCheckPosition)
        }

        override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
            super.onListItemClick(l, v, position, id)
            writeToLog("PlayersOnlineActivity", ">>>>>>>>>>>> onListItemClick position: $position")
            if (mAlreadySelected) {
                // not sure if this is really needed
                // may want to reconsider making this class a distinct Rabbit MQ queue
                writeToLog("PlayersOnlineActivity", "<<<<<<<<<<<<<< onListItemClick already selected, I'm returning")
                //showCancelDialog()
                sendToastMessage("Please press the Back button to search for other cool players!")
                return
            }
            mAlreadySelected = true
            setUpClientAndServer(position)
            val qName = getConfigMap("RabbitMQQueuePrefix") + "-" + "playerList" + "-" + mUserIds[position]
            mRabbitMQConnection = setUpRabbitMQConnection(qName)
            mSelectedPosition = position
            //FIXME - clean up this code once fully debugged
            val rnds = (0..10000000).random() // generated random from 0 to 10,000,000 included
            val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
            writeToLog("PlayersOnlineActivity", "============> onListItemClick called  at: $dateTime")
            val messageToOpponent = "letsPlay,$mPlayer1Name,$mPlayer1Id,$rnds, $dateTime"
            GlobalScope.launch {
                    SendMessageToRabbitMQ().main(
                        mRabbitMQConnection,
                        qName,
                        messageToOpponent,
                        mPlayersOnlineActivity as ToastMessage,
                        mResources
                    )
            }
            if (mRabbitMQConnection != null) {
                closeRabbitMQConnection(mRabbitMQConnection!!)
                writeToLog("PlayersOnlineActivity", "closeRabbitMQConnection() all done")
            }
            writeToLog("PlayersOnlineActivity", "onListItemClick() completed selected opponent:  $position")
        }

        private fun showCancelDialog() {
            if (mCancelDialog == null) {
                mCancelDialog = createCancelDialog()
                mCancelDialog!!.show()
            } else {
                mCancelDialog!!.show()
            }
        }

        private fun createCancelDialog(): AlertDialog {
            if (mCancelDialog != null) {
                mCancelDialog!!.dismiss()
            }

            return AlertDialog.Builder(mApplicationContext!!)
                .setIcon(R.drawable.willy_shmo_small_icon)
                .setTitle("Trouble connecting?")
                .setMessage("Please press the Back button to search again")
                .setCancelable(false)
                .setNegativeButton("Cancel") { _, _ -> createCancelDialog() }
                .create()
        }

        private fun setUpRabbitMQConnection(qName: String): RabbitMQConnection {
            return runBlocking {
                withContext(CoroutineScope(Dispatchers.Default).coroutineContext) {
                    SetUpRabbitMQConnection().main(
                        qName,
                        mPlayersOnlineActivity as ToastMessage,
                        resources
                    )
                }
            }
        }

        private fun closeRabbitMQConnection(mRabbitMQConnection: RabbitMQConnection) {
            // terminate run loop in RabbitMQMessageConsumer
            writeToLog("PlayersOnlineActivity", "at start of closeRabbitMQConnection()")
            return runBlocking {
                writeToLog("PlayersOnlineActivity", "about to stop RabbitMQ consume thread")
                val messageToSelf = "finishConsuming,$mPlayer1Name,$mPlayer1Id"
                val myQName = getConfigMap("RabbitMQQueuePrefix") + "-" + "playerList" + "-" + mPlayer1Id
                withContext(CoroutineScope(Dispatchers.Default).coroutineContext) {
                    val sendMessageToRabbitMQ = SendMessageToRabbitMQ()
                    sendMessageToRabbitMQ.main(Companion.mRabbitMQConnection, myQName, messageToSelf, mPlayersOnlineActivity as ToastMessage, mResources)
                }
                writeToLog("PlayersOnlineActivity", "about to close RabbitMQ connection")
                withContext(CoroutineScope(Dispatchers.Default).coroutineContext) {
                    CloseRabbitMQConnection().main(mRabbitMQConnection, mPlayersOnlineActivity as ToastMessage, resources)
                }
                writeToLog("PlayersOnlineActivity", "about to Dispose RabbitMQ consumer")
                withContext(CoroutineScope(Dispatchers.Default).coroutineContext) {
                    val disposeRabbitMQTask = DisposeRabbitMQTask()
                    disposeRabbitMQTask.main(mMessageConsumer, mResources, mPlayersOnlineActivity as ToastMessage)
                }
            }
        }

        private fun setUpClientAndServer(which: Int) {
            val settings = mApplicationContext!!.getSharedPreferences(UserPreferences.PREFS_NAME, Context.MODE_PRIVATE)
            val editor = settings.edit()
            editor.putString("ga_opponent_screenName", mUserNames[which])
            editor.apply()

            val opponentUserId = mUserIds[which]
            CoroutineScope( Dispatchers.Default).launch {
                StartGameTask().main(mCallerActivity, mPlayer1Id!!, mPlayer1Name!!, opponentUserId!!, mUserNames[which]!!, resources)
            }
        }

        private fun showDetails(index: Int) {
            mCurCheckPosition = index
        }

        override fun sendToastMessage(message: String?) {
            val msg = SplashScreen.mErrorHandler!!.obtainMessage()
            msg.obj = message
            SplashScreen.mErrorHandler!!.sendMessage(msg)
        }
    }

    //we're creating a clone because removing an entry from the original TreeMap causes a problem for the iterator
    private val playersOnline: Unit
        get() {
            val users = parseUserList(mUsersOnline)
            val usersClone = users.clone() as TreeMap<*, *>
            //we're creating a clone because removing an entry from the original TreeMap causes a problem for the iterator
            val userKeySet: MutableSet<out Any> = usersClone.keys // this is where the keys (userNames) gets sorted
            val keySetIterator = userKeySet.iterator()
            while (keySetIterator.hasNext()) {
                val key = keySetIterator.next()
                val userValues = users[key]!!
                val userId = userValues["userId"]
                if (userId == (mPlayer1Id!!).toString()) //not going to play against myself on the network
                    users.remove(key)
            }
            val objectArray: Array<Any> = users.keys.toTypedArray()
            mUserNames = arrayOfNulls(objectArray.size)
            mUserIds = arrayOfNulls(objectArray.size)
            for (x in objectArray.indices) {
                mUserNames[x] = objectArray[x] as String
                val userValues = users[objectArray[x]]!!
                mUserIds[x] = userValues["userId"]
            }
        }

    private fun parseUserList(usersLoggedOn: String?): TreeMap<String, HashMap<String, String>> {
        val userTreeMap = TreeMap<String, HashMap<String, String>>()
        try {
            val jsonObject = JSONObject(usersLoggedOn)
            val userArray = jsonObject.getJSONArray("UserList")
            for (y in 0 until userArray.length()) {
                val userMapValues = HashMap<String, String>()
                val userValues = userArray.getJSONObject(y)
                val userId = userValues.getString("id")
                val userName = userValues.getString("userName")
                userMapValues["userId"] = userId
                userTreeMap[userName] = userMapValues
            }
        } catch (e: JSONException) {
            writeToLog("PlayersOnlineActivity", "parseUserList: " + e.message)
            sendToastMessage(e.message)
        }
        return userTreeMap
    }

    private val sharedPreferences: Unit
        get() {
            //TODO - Think about moving ga_users_online and ga_users_online_number to WillyShmoApplication
            val settings = getSharedPreferences(UserPreferences.PREFS_NAME, MODE_PRIVATE)
            mUsersOnline = settings.getString("ga_users_online", null)
            mPlayer1Id = settings.getInt(GameActivity.PLAYER1_ID, 0)
        }

    private fun setNumberOfPlayersOnline() {
        val settings = getSharedPreferences(UserPreferences.PREFS_NAME, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putInt("ga_users_online_number",  mUserNames.size)
        editor.apply()
    }

    /**
     * A simple utility Handler to display an error message as a Toast popup
     */
    class ErrorHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            Toast.makeText(mApplicationContext, msg.obj as String, Toast.LENGTH_LONG).show()
        }
    }

    override fun sendToastMessage(message: String?) {
        val msg = mErrorHandler!!.obtainMessage()
        msg.obj = message
        mErrorHandler!!.sendMessage(msg)
    }

    companion object {
        private var mSavedInstanceState: Bundle? = null
        private lateinit  var appContext: Context
        fun setContext(context: Context) {
            appContext = context
        }
        fun getContext(): Context {
            return appContext
        }
        private lateinit var mCallerActivity: PlayersOnlineActivity
        private lateinit var mUserNames: Array<String?>
        private lateinit var mUserIds: Array<String?>
        private var mPlayer1Id: Int? = null
        private var mPlayer1Name: String? = null
        private var mApplicationContext: Context? = null
        var mErrorHandler: ErrorHandler? = null
        private lateinit var mResources: Resources
        private var mPlayersOnlineActivity: PlayersOnlineActivity? = null
        private var mSelectedPosition = -1
        private var mRabbitMQConnection: RabbitMQConnection? = null
        private var mRabbitMQResponse: String? = null
        private var mMessageConsumer: RabbitMQMessageConsumer? = null
        private var mUsersOnline: String? = null
        private var mCancelDialog: AlertDialog? = null

        private fun writeToLog(filter: String, msg: String) {
            if ("true".equals(mResources.getString(R.string.debug), ignoreCase = true)) {
                Log.d(filter, msg)
            }
        }
    }
}