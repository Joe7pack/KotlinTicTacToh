package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.res.Resources
import android.util.Log
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.getConfigMap
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import kotlinx.coroutines.*

class SetUpRabbitMQConnection {
    private var mCallingActivity: ToastMessage? = null
    //private lateinit var mRabbitMQConnection: RabbitMQConnection

    fun main(qName: String, callerActivity: ToastMessage, resources: Resources?): RabbitMQConnection { //runBlocking {
        var channel: Channel? = null
        var connection: Connection? = null
        try {
            mCallingActivity = callerActivity
            mResources = resources
            val connectionFactory = ConnectionFactory()
            connectionFactory.host = getConfigMap("RabbitMQIpAddress")
            connectionFactory.username = getConfigMap("RabbitMQUser")
            connectionFactory.password = getConfigMap("RabbitMQPassword")
            connectionFactory.virtualHost = getConfigMap("RabbitMQVirtualHost")
            val portNumber = Integer.valueOf(getConfigMap("RabbitMQPort"))
            connectionFactory.port = portNumber
            connection = connectionFactory.newConnection()
            //val channelOrig = connection.createChannel()
            channel = connection.createChannel()
//			channel.exchangeDeclare(EXCHANGE_NAME, "fanout", true);
            channel.queueDeclare(qName, false, false, false, null)
//			channel.basicPublish(EXCHANGE_NAME, QUEUE_NAME, null, tempstr.getBytes()); //pass message to be sent here!
            //channel.basicPublish("", qName, null, message.toByteArray())
            //writeToLog("SendMessageToRabbitMQTask", "message: $message to queue: $qName")
            //channel.close()
            //connection.close()
            //rabbitMQConnection.channel = channel
            //rabbitMQConnection.connection = connection

        } catch (e: Exception) {
            writeToLog("SendMessageToRabbitMQTask", "Exception: " + e.message)
            //Log.e("SendMessageToRabbitMQTask", e.getMessage());
            mCallingActivity!!.sendToastMessage(e.message)
        }
        finally {
            return RabbitMQConnection(connection, channel)
        }

    }

    companion object {
        private var mResources: Resources? = null
        private var rabbitMQConnection: RabbitMQConnection? = null
        private fun writeToLog(filter: String, msg: String) {
            if ("true".equals(mResources!!.getString(R.string.debug), ignoreCase = true)) {
                Log.d(filter, msg)
            }
        }
    }
}