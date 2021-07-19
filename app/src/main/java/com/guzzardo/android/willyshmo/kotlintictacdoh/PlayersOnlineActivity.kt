package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
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

class PlayersOnlineActivity : FragmentActivity(), ToastMessage {
    private var mUsersOnline: String? = null
    private var mMessageConsumer: RabbitMQMessageConsumer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSavedInstanceState = savedInstanceState
        mResources = resources
        errorHandler = ErrorHandler()
        mApplicationContext = applicationContext
        sharedPreferences
        mPlayersOnlineActivity = this
        mPlayer1Name = intent.getStringExtra(GameActivity.PLAYER1_NAME)
        playersOnline
        setNumberOfPlayersOnline()
        if (mUserNames.isEmpty()) {
            writeToLog("PlayersOnlineActivity", "starting server only")
            val i = Intent(mApplicationContext, GameActivity::class.java)

            val item = GameActivity.ParcelItems(123456789, "Cannabis")
            i.putExtra(GameActivity.PARCELABLE_VALUES, item)

            i.putExtra(GameActivity.START_SERVER, "true")
            i.putExtra(GameActivity.PLAYER1_ID, mPlayer1Id)
            i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name)
            startActivity(i)
            finish()
        } else {
            mMessageConsumer = RabbitMQMessageConsumer(this, mResources)
            setUpMessageConsumer(mMessageConsumer, "startGame", mRabbitMQPlayerResponseHandler)
            mRabbitMQPlayerResponseHandler = RabbitMQPlayerResponseHandler()
            setContentView(R.layout.players_online) //this starts up the list view
        }
        setContext(this)
        writeToLog("PlayersOnlineActivity", "onCreate taskId: $taskId")
    }

    private inner class RabbitMQPlayerResponseHandler : RabbitMQResponseHandler()

    public override fun onPause() {
        super.onPause()
        writeToLog("PlayersOnlineActivity", "onPause called from Main Activity")
        if (mSelectedPosition == -1) {
            val urlData = "/gamePlayer/update/?id=$mPlayer1Id&onlineNow=false&opponentId=0&userName="
            CoroutineScope(Dispatchers.Default).launch {
                val sendMessageToWillyShmoServer = SendMessageToWillyShmoServer()
                sendMessageToWillyShmoServer.main(urlData, mPlayer1Name, mPlayersOnlineActivity as ToastMessage, mResources, false)
            }
            writeToLog("PlayersOnlineActivity", "onPause from Main Activity called to set onlineNow to false")
        }
        GameActivity.isClientRunning = false
    }

    override fun onStop() {
        super.onStop()
        writeToLog("PlayersOnlineActivity", "onStop called from main class")
    }

    override fun onDestroy() {
        super.onDestroy()
        writeToLog("PlayersOnlineActivity", "onDestroy called from main class")
        if (mMessageConsumer != null) {
            CoroutineScope(Dispatchers.Default).launch {
                val disposeRabbitMQTask = DisposeRabbitMQTask()
                disposeRabbitMQTask.main(
                    mMessageConsumer,
                    mResources,
                    mPlayersOnlineActivity as ToastMessage
                )
            }
        }
    }

    private fun setUpMessageConsumer(rabbitMQMessageConsumer: RabbitMQMessageConsumer?, qNameQualifier: String, rabbitMQResponseHandler: RabbitMQResponseHandler?) {
        val qName = getConfigMap("RabbitMQQueuePrefix") + "-" + qNameQualifier + "-" + mPlayer1Id

        CoroutineScope(Dispatchers.Default).launch {
            val consumerConnectTask = ConsumerConnectTask()
            consumerConnectTask.main(
                getConfigMap("RabbitMQIpAddress"),
                rabbitMQMessageConsumer,
                qName,
                mPlayersOnlineActivity,
                mResources,
                "fromPlayersOnlineActivity"
            )
        }
        writeToLog("PlayersOnlineActivity", "$qNameQualifier message consumer listening on queue: $qName")

        // register for messages
        rabbitMQMessageConsumer!!.setOnReceiveMessageHandler(object: RabbitMQMessageConsumer.OnReceiveMessageHandler {
            override fun onReceiveMessage(message: ByteArray?) {
                try {
                    val text = String(message!!, StandardCharsets.UTF_8)
                    rabbitMQResponseHandler!!.rabbitMQResponse = text
                    writeToLog(
                        "PlayersOnlineActivity",
                        "$qNameQualifier OnReceiveMessageHandler received message: $text"
                    )
                    if (text.startsWith("letsPlay")) {
                        val playStringArray = text.split(",")
                        if (playStringArray.size >= 3) {
                            val opposingPlayerId = playStringArray[2]
                            val opposingPlayerName = playStringArray[1]
                            val i = Intent(mApplicationContext, GameActivity::class.java)

                            val item = GameActivity.ParcelItems(123456789, "Shakespeare")
                            i.putExtra(GameActivity.PARCELABLE_VALUES, item)

                            i.putExtra(GameActivity.START_SERVER, "true")
                            //i.putExtra(GameActivity.START_CLIENT, "true") //this will send the new game to the client
                            i.putExtra(GameActivity.PLAYER1_ID, mPlayer1Id)
                            i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name)
                            i.putExtra(GameActivity.START_CLIENT_OPPONENT_ID, opposingPlayerId)
                            i.putExtra(GameActivity.PLAYER2_NAME, opposingPlayerName)
                            i.putExtra(GameActivity.START_FROM_PLAYER_LIST, "true")
                            writeToLog(
                                "PlayersOnlineActivity",
                                "starting client and server from new player: $opposingPlayerName"
                            )
                            startActivity(appContext, i, null)
                        }
                        writeToLog(
                            "PlayersOnlineActivity",
                            "got LetsPlay response received message: $text at: " + SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss"
                            ).format(Date())
                        )
                    }
                } catch (e: Exception) {
                    writeToLog("PlayersOnlineActivity", "exception in onReceiveMessage: $e")
                }
            } // end onReceiveMessage
        })
    }

    /**
     * This is the "top-level" fragment, showing a list of items that the
     * user can pick.  Upon picking an item, it takes care of displaying the
     * data to the user as appropriate based on the currrent UI layout.
     */
    class PlayersOnlineFragment : ListFragment() {
        private val mDualPane = false
        private var mCurCheckPosition = 0

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            writeToLog("PlayersOnlineActivity", "onCreate called from PlayersOnlineFragment")

            // Populate list with our array of players.
            listAdapter = ArrayAdapter(mApplicationContext!!, android.R.layout.simple_list_item_activated_1, mUserNames)

            if (savedInstanceState != null) {
                // Restore last state for checked position.
                mCurCheckPosition = savedInstanceState.getInt("curChoice", 0)
            }
            if (mDualPane) {
                // In dual-pane mode, the list view highlights the selected item.
                listView.choiceMode = ListView.CHOICE_MODE_SINGLE
                // Make sure our UI is in the correct state.
                showDetails(mCurCheckPosition)
            }
        }

        override fun onResume() { //only called when at least one opponent is online to select 
            super.onResume()
            writeToLog("PlayersOnlineFragment", "onResume called from PlayersOnlineFragment")
            startGame()
        }

        private fun startGame() {
            mSelectedPosition = -1
            mRabbitMQPlayerResponseHandler!!.rabbitMQResponse = "null" // get rid of any old game requests
        }

        override fun onPause() { // pause the PlayersOnlineFragment
            super.onPause()
            writeToLog("PlayersOnlineFragment", "onPause called from PlayersOnlineFragment")
        }

        override fun onStop() {
            super.onStop()
            writeToLog("PlayersOnlineFragment", "onStop called from PlayersOnlineFragment")
        }

        override fun onDestroy() {
            super.onDestroy()
            writeToLog("PlayersOnlineFragment", "onDestroy called")
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            outState.putInt("curChoice", mCurCheckPosition)
        }

        override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
            setUpClientAndServer(position)
            val qName = getConfigMap("RabbitMQQueuePrefix") + "-" + "startGame" + "-" + mUserIds[position]

            //FIXME - clean up this code once fully debugged
            val rnds = (0..1000).random() // generated random from 0 to 1000 included
            val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())

            writeToLog("PlayersOnlineActivity", "============> onListItemClick called  at: $dateTime")
            //val messageToOpponent = "letsPlay," + mPlayer1Name + "," + mPlayer1Id //mUserIds[position];
            val messageToOpponent = "letsPlay," + mPlayer1Name + "," + mPlayer1Id + ", " + rnds + ", " + dateTime
            CoroutineScope(Dispatchers.Default).launch {
                val sendMessageToRabbitMQTask = SendMessageToRabbitMQTask()
                sendMessageToRabbitMQTask.main(getConfigMap("RabbitMQIpAddress"), qName, messageToOpponent, mPlayersOnlineActivity as ToastMessage, mResources)
            }
            mSelectedPosition = position
        }

        private fun setUpClientAndServer(which: Int) {
            val settings = mApplicationContext!!.getSharedPreferences(UserPreferences.PREFS_NAME, 0)
            val editor = settings.edit()
            editor.putString("ga_opponent_screenName", mUserNames[which])
            editor.apply()
            val i = Intent(mApplicationContext, GameActivity::class.java)
            i.putExtra(GameActivity.START_SERVER, "true")
            i.putExtra(GameActivity.START_CLIENT, "true") //this will send the new game to the client
            i.putExtra(GameActivity.PLAYER1_ID, mPlayer1Id)
            i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name)
            i.putExtra(GameActivity.START_CLIENT_OPPONENT_ID, mUserIds[which])
            i.putExtra(GameActivity.PLAYER2_NAME, mUserNames[which])
            i.putExtra(GameActivity.START_FROM_PLAYER_LIST, "true")
            writeToLog("PlayersOnlineActivity", "starting client and server")

            val item = GameActivity.ParcelItems(123456789, "Dr. Strangelove")
            i.putExtra(GameActivity.PARCELABLE_VALUES, item)

            startActivity(i)
        }

        private fun showDetails(index: Int) {
            mCurCheckPosition = index
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
            val settings = getSharedPreferences(UserPreferences.PREFS_NAME, MODE_PRIVATE)
            mUsersOnline = settings.getString("ga_users_online", null)
            mPlayer1Id = settings.getInt(GameActivity.PLAYER1_ID, 0)
        }

    private fun setNumberOfPlayersOnline() {
        val settings = getSharedPreferences(UserPreferences.PREFS_NAME, 0)
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
        val msg = errorHandler!!.obtainMessage()
        msg.obj = message
        errorHandler!!.sendMessage(msg)
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

        private lateinit var mUserNames: Array<String?>
        private lateinit var mUserIds: Array<String?>
        private var mPlayer1Id: Int? = null
        private var mPlayer1Name: String? = null
        private var mApplicationContext: Context? = null
        var errorHandler: ErrorHandler? = null
        private lateinit var mResources: Resources
        private var mPlayersOnlineActivity: PlayersOnlineActivity? = null
        private var mSelectedPosition = -1
        //private var mMessageConsumer: RabbitMQMessageConsumer? = null
        private var mRabbitMQPlayerResponseHandler: RabbitMQPlayerResponseHandler? = null

        private fun writeToLog(filter: String, msg: String) {
            if ("true".equals(mResources.getString(R.string.debug), ignoreCase = true)) {
                Log.d(filter, msg)
            }
        }
    }
}