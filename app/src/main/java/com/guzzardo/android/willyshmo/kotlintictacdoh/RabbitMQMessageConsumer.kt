package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.util.Log
//import com.guzzardo.android.willyshmo.kotlintictacdoh.ToastMessage.sendToastMessage
import com.rabbitmq.client.QueueingConsumer.Delivery
import com.rabbitmq.client.*
import java.io.IOException
import java.lang.Exception

// Consumes messages from a RabbitMQ broker
class RabbitMQMessageConsumer(private val mToastMessage: ToastMessage, private var resources: Resources?) {
    private val mExchange = "test"
    var channel: Channel? = null
    var connection: Connection? = null
    private val mExchangeType = "fanout"
    private var queue: String? = null //The Queue name for this consumer
    var consumer: QueueingConsumer? = null
    private var mConsumeRunning = false
    private var mResources = resources
    private lateinit var mLastMessage: ByteArray //last message to post back

    // An interface to be implemented by an object that is interested in messages(listener)
    // This is the hook to connect the MainActivity message handler response processing with the received message from RabbitMQ
    interface OnReceiveMessageHandler {
        fun onReceiveMessage(message: ByteArray?)
    }

    //a reference to the listener, we can only have one at a time (for now)
    private lateinit var mOnReceiveMessageHandler: OnReceiveMessageHandler

    /**
     * Set the callback for received messages
     * @param handler The callback
     */
    fun setOnReceiveMessageHandler(handler: OnReceiveMessageHandler) {
        mOnReceiveMessageHandler = handler
    }

    private val mMessageHandler = Handler(Looper.getMainLooper())

    // Create runnable for posting back to main thread
    val mReturnMessage = Runnable { mOnReceiveMessageHandler.onReceiveMessage(mLastMessage) }
    private val mConsumeHandler = Handler(Looper.getMainLooper())
    private val mConsumeRunner = Runnable { consume() }

    /**
     * Create Exchange and then start consuming. A binding needs to be added before any messages will be delivered
     */
    fun startConsuming(connection: Connection?, channel: Channel?, queue: String?, consumer: QueueingConsumer?): Boolean {
        this.channel = channel
        this.connection = connection
        this.queue = queue
        this.consumer = consumer
        //if (mExchangeType == "fanout")
        //  AddBinding("");//fanout has default binding
        mConsumeHandler.post(mConsumeRunner)
        mConsumeRunning = true
        return true
    }

    /**
     * Add a binding between this consumers Queue and the Exchange with routingKey
     * @param routingKey the binding key eg GOOG
     */
    fun addBinding(routingKey: String?) {
        try {
            channel!!.queueBind(queue, mExchange, routingKey)
        } catch (e: IOException) {
            //e.printStackTrace();
            mToastMessage.sendToastMessage("queuePurge " + e.message)
        }
    }

    /**
     * Remove binding between this consumers Queue and the Exchange with routingKey
     * @param routingKey the binding key eg GOOG
     */
    fun removeBinding(routingKey: String?) {
        try {
            channel!!.queueUnbind(queue, mExchange, routingKey)
        } catch (e: IOException) {
            //e.printStackTrace();
            mToastMessage.sendToastMessage("queuePurge " + e.message)
        }
    }

    private fun consume() {
        val thread: Thread = object : Thread() {
            override fun run() {
                var delivery: Delivery
                while (mConsumeRunning) {
                    //Log.d("RabbitMQMessageConsumer", "inside consume run loop");
                    try {
                        delivery = consumer!!.nextDelivery() //blocks until a message is received
                        mLastMessage = delivery.body
                        writeToLog("RabbitMQMessageConsumer", "last message: " + String(mLastMessage))
                        //Log.d("RabbitMQMessageConsumer", "last message: " + new String(mLastMessage));
                        mMessageHandler.post(mReturnMessage)
                    } catch (ie: InterruptedException) {
                        writeToLog("RabbitMQMessageConsumer", "InterruptedException: " + ie.message)
                        mToastMessage.sendToastMessage(ie.message)
                    } catch (sse: ShutdownSignalException) {

                    } catch (cce: ConsumerCancelledException) {

                    } catch (e: Exception) {

                    } finally {

                    }
                }
                writeToLog("RabbitMQMessageConsumer", "thread all done")
            }
        }
        thread.start()
    }

    fun dispose() {
        mConsumeRunning = false
        writeToLog("RabbitMQMessageConsumer", "dispose called")
        try {
            if (channel != null) channel!!.abort()
            if (connection != null) connection!!.close()
        } catch (e: IOException) {
            mToastMessage.sendToastMessage("queuePurge " + e.message)
        }
    }

    private fun writeToLog(filter: String, msg: String) {
        if ("true".equals(resources!!.getString(R.string.debug), ignoreCase = true)) {
            if ("true".equals(resources!!.getString(R.string.debug), ignoreCase = true)) {
                Log.d(filter, msg)
            }
        }
    }
}