package com.guzzardo.android.willyshmo.kotlintictacdoh

import android.content.res.Resources
import android.os.AsyncTask
import android.util.Log
import com.guzzardo.android.willyshmo.kotlintictacdoh.WillyShmoApplication.Companion.getConfigMap
import com.rabbitmq.client.ConnectionFactory

class SendMessageToRabbitMQTask : AsyncTask<Any?, Void?, Void?>() {
    private var mCallingActivity: ToastMessage? = null
    protected override fun doInBackground(vararg values: Any?): Void? {
        try {

            //String hostName = (String)values[0];
            val qName = values[1] as String
            //val exchangeName = values[2] as String
            val message = values[3] as String
            mCallingActivity = values[4] as ToastMessage
            mResources = values[5] as Resources
            val connectionFactory = ConnectionFactory()
            connectionFactory.host = getConfigMap("RabbitMQIpAddress")
            connectionFactory.username = getConfigMap("RabbitMQUser")
            connectionFactory.password = getConfigMap("RabbitMQPassword")
            connectionFactory.virtualHost = getConfigMap("RabbitMQVirtualHost")
            val portNumber = Integer.valueOf(getConfigMap("RabbitMQPort"))
            connectionFactory.port = portNumber
            val connection = connectionFactory.newConnection()
            val channel = connection.createChannel()

//			channel.exchangeDeclare(EXCHANGE_NAME, "fanout", true);
            channel.queueDeclare(qName, false, false, false, null)

//			channel.basicPublish(EXCHANGE_NAME, QUEUE_NAME, null, tempstr.getBytes());
            channel.basicPublish("", qName, null, message.toByteArray())
            writeToLog("SendMessageToRabbitMQTask", "message: $message queue: $qName")
            channel.close()
            connection.close()
        } catch (e: Exception) {
            writeToLog("SendMessageToRabbitMQTask", "Exception: " + e.message)
            //Log.e("SendMessageToRabbitMQTask", e.getMessage());
            mCallingActivity!!.sendToastMessage(e.message)
        }
        return null
    }

    companion object {
        private var mResources: Resources? = null
        private fun writeToLog(filter: String, msg: String) {
            if ("true".equals(mResources!!.getString(R.string.debug), ignoreCase = true)) {
                Log.d(filter, msg)
            }
        }
    }
}