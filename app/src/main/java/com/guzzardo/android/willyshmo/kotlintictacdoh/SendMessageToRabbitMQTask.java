package com.guzzardo.android.willyshmo.kotlintictacdoh;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class SendMessageToRabbitMQTask extends AsyncTask<Object, Void, Void> {
	private ToastMessage mCallingActivity;
	private static Resources mResources;

	@Override
	protected Void doInBackground(Object... values) {
		try {
			
			//String hostName = (String)values[0];
			String qName = (String)values[1];
			String exchangeName = (String)values[2];
			String message = (String)values[3];
			mCallingActivity = (ToastMessage)values[4];
			mResources = (Resources)values[5];

			ConnectionFactory connectionFactory = new ConnectionFactory();

			String hostName = (String)WillyShmoApplication.getConfigMap("RabbitMQIpAddress");
			String userName = (String)WillyShmoApplication.getConfigMap("RabbitMQUser");
			String password = (String)WillyShmoApplication.getConfigMap("RabbitMQPassword");
			String virtualHost = (String)WillyShmoApplication.getConfigMap("RabbitMQVirtualHost");
			String port = (String)WillyShmoApplication.getConfigMap("RabbitMQPort");

			connectionFactory.setHost(hostName);
			connectionFactory.setUsername(userName);
			connectionFactory.setPassword(password);
			connectionFactory.setVirtualHost(virtualHost);
			int portNumber = Integer.valueOf(port);
			connectionFactory.setPort(portNumber);
			Connection connection = connectionFactory.newConnection();
			Channel channel = connection.createChannel();
			
//			channel.exchangeDeclare(EXCHANGE_NAME, "fanout", true);
			channel.queueDeclare(qName, false, false, false, null);

//			channel.basicPublish(EXCHANGE_NAME, QUEUE_NAME, null, tempstr.getBytes());
			channel.basicPublish("", qName, null, message.getBytes());
			writeToLog("SendMessageToRabbitMQTask", "message: " + message + " queue: " + qName);
			channel.close();
			connection.close();
		} catch (Exception e) {
			writeToLog("SendMessageToRabbitMQTask", "Exception: "+e.getMessage()); 			
			//Log.e("SendMessageToRabbitMQTask", e.getMessage());
			mCallingActivity.sendToastMessage(e.getMessage());
		}
		return null;
	}
	
    private static void writeToLog(String filter, String msg) {
    	if ("true".equalsIgnoreCase(mResources.getString(R.string.debug))) {
    		Log.d(filter, msg);
    	}
    }
	

}
