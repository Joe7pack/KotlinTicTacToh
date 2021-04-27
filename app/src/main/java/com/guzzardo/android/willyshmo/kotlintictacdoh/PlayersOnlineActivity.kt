package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.app.Activity
import android.app.ListFragment
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.guzzardo.android.willyshmo.kotlintictacdoh.MainActivity.UserPreferences
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.getConfigMap
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.*


class PlayersOnlineActivity : Activity(), ToastMessage {
    private var mUsersOnline: String? = null
    private var mRabbitMQPlayerResponse: String? = null

    //TODO - consider saving mUserNames and mUserIds in savedInstanceState and changing AndroidManifest.PlayersOnlineActivity 
    // android:noHistory to false so that we can restore prior list when user presses back button in GameActivity
    // This will make replaying simpler since the user will see the prior list of users online when leaving a game instead of 
    // being sent back to the 2 Players screen.
    // The downside is that this list will become increasingly inaccurate as other players enter and leave the online list.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mResources = resources
        errorHandler = ErrorHandler()
        mApplicationContext = applicationContext
        sharedPreferences
        mPlayersOnlineActivity = this
        mPlayer1Name = intent.getStringExtra(GameActivity.PLAYER1_NAME)
        playersOnline
        resetPlayersOnline()
        if (mUserNames.size == 0) {
            writeToLog("PlayersOnlineActivity", "starting server only")
            val i = Intent(mApplicationContext, GameActivity::class.java)
            i.putExtra(GameActivity.START_SERVER, "true")
            i.putExtra(GameActivity.PLAYER1_ID, mPlayer1Id)
            i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name)
            startActivity(i)
            finish()
        } else {
            mMessageConsumer = RabbitMQMessageConsumer(this, mResources)
            mRabbitMQPlayerResponseHandler = RabbitMQPlayerResponseHandler()
            setContentView(R.layout.players_online) //this starts up the list view
        }
    }

    private inner class RabbitMQPlayerResponseHandler : RabbitMQResponseHandler() {
        /*
        override fun setRabbitMQResponse(rabbitMQResponse: String) {
            mRabbitMQPlayerResponse = rabbitMQResponse
        }

        override fun getRabbitMQResponse(): String {
            return mRabbitMQPlayerResponse!!
        }

         */
    }

    public override fun onPause() {
        super.onPause()
        if (mSelectedPosition == -1) {
            val urlData =
                "/gamePlayer/update/?id=" + mPlayer1Id + "&onlineNow=false&opponentId=0&userName="
            SendMessageToWillyShmoServer().execute(urlData, mPlayer1Name, this, mResources, false)
        }
    }

    /**
     * This is the "top-level" fragment, showing a list of items that the
     * user can pick.  Upon picking an item, it takes care of displaying the
     * data to the user as appropriate based on the currrent UI layout.
     */
    class PlayersOnlineFragment : ListFragment() {
        private val mDualPane = false
        private var mCurCheckPosition = 0
        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)

            // Populate list with our static array of titles.
            listAdapter = ArrayAdapter(
                activity,
                android.R.layout.simple_list_item_activated_1, mUserNames
            )

            // Check to see if we have a frame in which to embed the details
            // fragment directly in the containing UI.
            //View detailsFrame = getActivity().findViewById(R.id.details);
            //mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;
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

        private fun showAcceptDialog(opponentName: String, opponentId: String) {
            val manager = this.fragmentManager
            val ft = manager.beginTransaction()
            val myBundle = Bundle()
            myBundle.putString("opponentName", opponentName)
            myBundle.putString("opponentId", opponentId)
            myBundle.putString("playerName", mPlayer1Name)
            myBundle.putInt("player1Id", mPlayer1Id!!)
            val acceptGameDialog = AcceptGameDialog()
            //AcceptGameDialog acceptGameDialog = new AcceptGameDialog(opponentName, opponentId, mPlayer1Name, mPlayer1Id, mApplicationContext, mResources);
            acceptGameDialog.arguments = myBundle
            acceptGameDialog.context = mApplicationContext!!
            acceptGameDialog.resources = mResources
            acceptGameDialog.show(ft, "dialog")
        }

        override fun onResume() { //only called when at least one opponent is online to select 
            super.onResume()
            startGame()
        }

        override fun onPause() {
            super.onPause()
            DisposeRabbitMQTask().execute(mMessageConsumer, mResources, mPlayersOnlineActivity)
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            outState.putInt("curChoice", mCurCheckPosition)
        }

        override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
            setUpClientAndServer(position)
            val qName =
                getConfigMap("RabbitMQQueuePrefix") + "-" + "startGame" + "-" + mUserIds[position]
            val messageToOpponent =
                "letsPlay," + mPlayer1Name + "," + mPlayer1Id //mUserIds[position];
            SendMessageToRabbitMQTask().execute(
                getConfigMap("RabbitMQIpAddress"),
                qName,
                null,
                messageToOpponent,
                mPlayersOnlineActivity,
                mResources
            )
            mSelectedPosition = position
        }

        private fun setUpClientAndServer(which: Int) {
            val settings = mApplicationContext!!.getSharedPreferences(UserPreferences.PREFS_NAME, 0)
            val editor = settings.edit()
            editor.putString("ga_opponent_screenName", mUserNames[which])
            editor.apply()
            val i = Intent(mApplicationContext, GameActivity::class.java)
            i.putExtra(GameActivity.START_SERVER, "true")
            i.putExtra(
                GameActivity.START_CLIENT,
                "true"
            ) //this will send the new game to the client
            i.putExtra(GameActivity.PLAYER1_ID, mPlayer1Id)
            i.putExtra(GameActivity.PLAYER1_NAME, mPlayer1Name)
            i.putExtra(GameActivity.START_CLIENT_OPPONENT_ID, mUserIds[which])
            i.putExtra(GameActivity.PLAYER2_NAME, mUserNames[which])
            i.putExtra(GameActivity.START_FROM_PLAYER_LIST, "true")
            writeToLog("PlayersOnlineActivity", "starting client and server")
            startActivity(i)
        }

        fun showDetails(index: Int) {
            mCurCheckPosition = index
        }
    }//not going to play against myself on the network// this is where the keys (userNames) gets sorted

    //we're creating a clone because removing an entry from the original TreeMap causes a problem for the iterator
    private val playersOnline: Unit
        private get() {
            val users = parseUserList(mUsersOnline)
            val usersClone = users.clone() as TreeMap<String, HashMap<String, String>>
            //we're creating a clone because removing an entry from the original TreeMap causes a problem for the iterator
            val userKeySet: Set<String> =
                usersClone.keys // this is where the keys (userNames) gets sorted
            val keySetIterator = userKeySet.iterator()
            while (keySetIterator.hasNext()) {
                val key = keySetIterator.next()
                val userValues = users[key]!!
                val userId = userValues["userId"]
                if (userId == Integer.toString(mPlayer1Id!!)) //not going to play against myself on the network
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
        private get() {
            val settings = getSharedPreferences(UserPreferences.PREFS_NAME, MODE_PRIVATE)
            mUsersOnline = settings.getString("ga_users_online", null)
            mPlayer1Id = settings.getInt(GameActivity.PLAYER1_ID, 0)
        }

    private fun resetPlayersOnline() {
        val settings = getSharedPreferences(UserPreferences.PREFS_NAME, 0)
        val editor = settings.edit()
        editor.putString("ga_users_online", null)
        editor.apply()
    }

    /**
     * A simple utility Handler to display an error message as a Toast popup
     */
    inner class ErrorHandler : Handler() {
        override fun handleMessage(msg: Message) {
            Toast.makeText(applicationContext, msg.obj as String, Toast.LENGTH_LONG).show()
        }
    }

    override fun sendToastMessage(message: String?) {
        val msg = errorHandler!!.obtainMessage()
        msg.obj = message
        errorHandler!!.sendMessage(msg)
    }

    companion object {
        private lateinit var mUserNames: Array<String?>
        private lateinit var mUserIds: Array<String?>
        private var mPlayer1Id: Int? = null
        private var mPlayer1Name: String? = null
        private var mApplicationContext: Context? = null
        var errorHandler: ErrorHandler? = null
        //private var mResources: Resources? = null
        private lateinit var mResources: Resources
        private var mPlayersOnlineActivity: PlayersOnlineActivity? = null
        private var mSelectedPosition = -1
        private var mMessageConsumer: RabbitMQMessageConsumer? = null
        private var mRabbitMQPlayerResponseHandler: RabbitMQPlayerResponseHandler? = null
        private fun setUpMessageConsumer(
            rabbitMQMessageConsumer: RabbitMQMessageConsumer?,
            qNameQualifier: String,
            rabbitMQResponseHandler: RabbitMQResponseHandler?
        ) {
            //String hostName = mResources.getString(R.string.RabbitMQHostName);
            val qName =
                getConfigMap("RabbitMQQueuePrefix") + "-" + qNameQualifier + "-" + mPlayer1Id
            ConsumerConnectTask().execute(
                getConfigMap("RabbitMQIpAddress"),
                rabbitMQMessageConsumer,
                qName,
                mPlayersOnlineActivity,
                mResources,
                "fromPlayersOnlineActivity"
            )
            writeToLog(
                "PlayersOnlineActivity",
                "$qNameQualifier message consumer listening on queue: $qName"
            )

            // register for messages
            rabbitMQMessageConsumer!!.setOnReceiveMessageHandler(object : RabbitMQMessageConsumer.OnReceiveMessageHandler {
                override fun onReceiveMessage(message: ByteArray?) {
                    var text = ""
                    text = String(message!!, StandardCharsets.UTF_8)
                    rabbitMQResponseHandler!!.rabbitMQResponse = text
                    writeToLog("GameActivity", "$qNameQualifier OnReceiveMessageHandler received message: $text")
                }
            })
        }

        private fun startGame() {
            setUpMessageConsumer(mMessageConsumer, "startGame", mRabbitMQPlayerResponseHandler)
            mSelectedPosition = -1
            mRabbitMQPlayerResponseHandler!!.rabbitMQResponse =
                "null" // get rid of any old game requests
        }

        private fun writeToLog(filter: String, msg: String) {
            if ("true".equals(mResources!!.getString(R.string.debug), ignoreCase = true)) {
                Log.d(filter, msg)
            }
        }
    }
}