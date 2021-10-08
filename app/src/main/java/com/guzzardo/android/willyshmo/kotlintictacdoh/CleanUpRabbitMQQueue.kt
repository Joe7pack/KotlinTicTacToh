package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.Context
import android.util.Log
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.nio.charset.StandardCharsets

class CleanUpRabbitMQQueue(val player1Id: Int, val player1Name: String, val resources: Resources, val toastMessage: ToastMessage) : HandleRabbitMQMessage {

    fun main() {
        mResources = resources
        mMessageConsumer = RabbitMQMessageConsumer(toastMessage, mResources)
        mMessageConsumer!!.setUpMessageConsumer("client", player1Id, toastMessage, mResources, "CleanUpRabbitMQQueue")
        mMessageConsumer!!.setOnReceiveMessageHandler(object:
            RabbitMQMessageConsumer.OnReceiveMessageHandler {
            override fun onReceiveMessage(message: ByteArray?) {
                try {
                    val text = String(message!!, StandardCharsets.UTF_8)
                    writeToLog("CleanUpRabbitMQQueue", "OnReceiveMessageHandler has received message: $text")
                    handleRabbitMQMessage(text)
                } catch (e: Exception) {
                    writeToLog("CleanUpRabbitMQQueue", "exception in onReceiveMessage: $e")
                }
            } // end onReceiveMessage
        }) // end setOnReceiveMessageHandler

        mRabbitMQConnection = setUpRabbitMQConnection()
        mClearQueueThread = ClearQueueThread()
        mClearQueueThread!!.start()
        mClearQueueThreadRunning = true
    }

    inner class ClearQueueThread: Thread() {
        override fun run() {
            try {
                writeToLog("CleanUpRabbitMQQueue", "ClearQueueThread started")
                while (mClearQueueThreadRunning) {
                    if (mMessageRetrieved == null) {
                        mClearQueueThreadRunning = false
                    } else {
                        writeToLog("CleanUpRabbitMQQueue", "ClearQueueThread message retrieved: $mMessageRetrieved")
                        mMessageRetrieved = null
                    }
                    sleep(THREAD_SLEEP_INTERVAL.toLong())
                } // while end
                closeRabbitMQConnection(mRabbitMQConnection!!)
            } catch (e: Exception) {
                writeToLog("CleanUpRabbitMQQueue", "error in ClearQueueThread: " + e.message)
                sendToastMessage(e.message)
            } finally {
                writeToLog("CleanUpRabbitMQQueue", "ClearQueueThread finally done")
            }
        }
    }

    private fun setUpRabbitMQConnection(): RabbitMQConnection {
        val qName = WillyShmoApplication.getConfigMap("RabbitMQQueuePrefix") + "-" + "client" + "-" + player1Id
        return runBlocking {
            CoroutineScope(Dispatchers.Default).async {
                SetUpRabbitMQConnection().main(qName, toastMessage, mResources)
            }.await()
        }
    }

    fun closeRabbitMQConnection(mRabbitMQConnection: RabbitMQConnection) {
        writeToLog("CleanUpRabbitMQQueue", "at start of closeRabbitMQConnection()")
        return runBlocking {
            writeToLog("CleanUpRabbitMQQueue", "about to stop RabbitMQ consume thread")
            val messageToSelf = "finishConsuming,${player1Name},${player1Id}"
            CoroutineScope(Dispatchers.Default).async {
                val qName = WillyShmoApplication.getConfigMap("RabbitMQQueuePrefix") + "-" + "client" + "-" + player1Id
                val sendMessageToRabbitMQ = SendMessageToRabbitMQ()
                sendMessageToRabbitMQ.main(mRabbitMQConnection, qName, messageToSelf, toastMessage, mResources)
            }.await()
            writeToLog("CleanUpRabbitMQQueue", "about to close RabbitMQ connection")
            CoroutineScope(Dispatchers.Default).async {
                CloseRabbitMQConnection().main(mRabbitMQConnection, toastMessage, mResources)
            }.await()
            writeToLog("CleanUpRabbitMQQueue", "about to Dispose RabbitMQ consumer")
            CoroutineScope(Dispatchers.Default).async {
                val disposeRabbitMQTask = DisposeRabbitMQTask()
                disposeRabbitMQTask.main(mMessageConsumer, mResources, toastMessage)
            }.await()
        }
    }

    override fun handleRabbitMQMessage(message: String) {
        writeToLog("CleanUpRabbitMQQueue", "message retrieved: $message")
        mMessageRetrieved = message
    }

    fun sendToastMessage(message: String?) {
        val msg = mErrorHandler!!.obtainMessage()
        msg.obj = message
        mErrorHandler!!.sendMessage(msg)
    }

    class ErrorHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            Toast.makeText(mApplicationContext, msg.obj as String, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private lateinit var mResources: Resources
        private var mMessageConsumer: RabbitMQMessageConsumer? = null
        private var mErrorHandler: ErrorHandler? = null
        private var mApplicationContext: Context? = null
        private var mMessageRetrieved: String? = "seed message"
        private var mClearQueueThread: ClearQueueThread? = null
        private var mClearQueueThreadRunning = false
        private const val THREAD_SLEEP_INTERVAL = 300 //milliseconds
        private var mRabbitMQConnection: RabbitMQConnection? = null
        private fun writeToLog(filter: String, msg: String) {
            if ("true".equals(mResources.getString(R.string.debug), ignoreCase = true)) {
                Log.d(filter, msg)
            }
        }
    }
}