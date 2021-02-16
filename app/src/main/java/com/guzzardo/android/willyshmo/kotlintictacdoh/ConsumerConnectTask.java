package com.guzzardo.android.willyshmo.kotlintictacdoh;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

public class ConsumerConnectTask extends AsyncTask<Object, Void, Void> {
	
	private String mHostName;
	private RabbitMQMessageConsumer mMessageConsumer;
	private Connection mConnection;
	private String mQueueName;
	private Channel mChannel;
	private QueueingConsumer mConsumer;	
	private ToastMessage mToastMessage;
    private static Resources mResources;
    private String mSource;

	@Override
	protected Void doInBackground(Object... messageConsumer) {

		mHostName = (String)messageConsumer[0];
		mMessageConsumer = (RabbitMQMessageConsumer)messageConsumer[1];
		mQueueName = (String)messageConsumer[2];
		mToastMessage = (ToastMessage)messageConsumer[3];
	    mResources = (Resources)messageConsumer[4];
	    mSource = (String)messageConsumer[5];
		
		if (mMessageConsumer.getChannel() == null) {
			try { // Connect to broker
				String hostName = (String)WillyShmoApplication.getConfigMap("RabbitMQIpAddress");
				String userName = (String)WillyShmoApplication.getConfigMap("RabbitMQUser");
				String password = (String)WillyShmoApplication.getConfigMap("RabbitMQPassword");
				String virtualHost = (String)WillyShmoApplication.getConfigMap("RabbitMQVirtualHost");
				String port = (String)WillyShmoApplication.getConfigMap("RabbitMQPort");

				ConnectionFactory connectionFactory = new ConnectionFactory();
				connectionFactory.setHost(hostName);
				connectionFactory.setUsername(userName);
				connectionFactory.setPassword(password);
				connectionFactory.setVirtualHost(virtualHost);
				int portNumber = Integer.parseInt(port);
				connectionFactory.setPort(portNumber);
				//TODO - need to determine the default connection timeout
//				connectionFactory.setConnectionTimeout(5000);
				connectionFactory.setConnectionTimeout(ConnectionFactory.DEFAULT_CONNECTION_TIMEOUT); //wait forever

				mConnection = connectionFactory.newConnection();
				mChannel = mConnection.createChannel();
//				channel.exchangeDeclare("test", "fanout", true);
				AMQP.Queue.DeclareOk queueDeclare = mChannel.queueDeclare(mQueueName, false, false, false, null);
				queueDeclare.getQueue();
				mConsumer = new QueueingConsumer(mChannel);
//				mChannel.queuePurge(mQueueName); // get rid of any prior messages
				mChannel.basicConsume(mQueueName, true, mConsumer);
			} catch (Exception e) {
				mToastMessage.sendToastMessage(e.getMessage());
			}
		}
		return null;
	}

    protected void onPostExecute(Void res) {
		mMessageConsumer.setChannel(mChannel);
		mMessageConsumer.setConnection(mConnection);
		mMessageConsumer.startConsuming(mConnection, mChannel, mQueueName, mConsumer);
		}

    private static void writeToLog(String filter, String msg) {
	    if ("true".equalsIgnoreCase(mResources.getString(R.string.debug))) {
	        Log.d(filter, msg);
	    }
    }

}

