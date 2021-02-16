package com.guzzardo.android.willyshmo.kotlintictacdoh;

import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

import java.io.IOException;

/**
*Consumes messages from a RabbitMQ broker
*
*/
public class RabbitMQMessageConsumer {
	
    private String mExchange = "test";
    protected Channel mChannel;
    protected Connection mConnection;
    private String mExchangeType = "fanout";
	private String mQueue; //The Queue name for this consumer
	private QueueingConsumer mConsumer;
	private boolean mConsumeRunning;
	private byte[] mLastMessage; //last message to post back
	private ToastMessage mToastMessage;
	private static Resources mResources;
	
	public RabbitMQMessageConsumer(ToastMessage ToastMessage, Resources resources) {
    	mToastMessage = ToastMessage;
    	mResources = resources;
	}
	
	// An interface to be implemented by an object that is interested in messages(listener)
	// This is the hook to connect the MainActivity message handler response processing with the received message from RabbitMQ
	public interface OnReceiveMessageHandler {
		public void onReceiveMessage(byte[] message);
	};

	//a reference to the listener, we can only have one at a time (for now)
	private OnReceiveMessageHandler mOnReceiveMessageHandler;

	/**
	 *
	 * Set the callback for received messages
	 * @param handler The callback
	 */
	public void setOnReceiveMessageHandler(OnReceiveMessageHandler handler) {
		mOnReceiveMessageHandler = handler;
	};

	private Handler mMessageHandler = new Handler();

	// Create runnable for posting back to main thread
	final Runnable mReturnMessage = new Runnable() {
		public void run() {
			mOnReceiveMessageHandler.onReceiveMessage(mLastMessage);
		}
	};

	private Handler mConsumeHandler = new Handler();
	
	final Runnable mConsumeRunner = new Runnable() {
		public void run() {
			consume();
		}
	};

	/**
	 * Create Exchange and then start consuming. A binding needs to be added before any messages will be delivered
	 */
	public boolean startConsuming(Connection connection, Channel channel, String queue, QueueingConsumer consumer) {
		setChannel(channel);
		setConnection(connection);
		setQueue(queue);
		setConsumer(consumer);
		//if (mExchangeType == "fanout")
		//  AddBinding("");//fanout has default binding

		mConsumeHandler.post(mConsumeRunner);
		mConsumeRunning = true;		

		return true;
	}

	/**
	 * Add a binding between this consumers Queue and the Exchange with routingKey
	 * @param routingKey the binding key eg GOOG
	 */
	public void AddBinding(String routingKey) {
		try {
			mChannel.queueBind(mQueue, mExchange, routingKey);
		} catch (IOException e) {
			//e.printStackTrace();
			mToastMessage.sendToastMessage("queuePurge " + e.getMessage());			
		}
	}

	/**
	 * Remove binding between this consumers Queue and the Exchange with routingKey
	 * @param routingKey the binding key eg GOOG
	 */
	public void RemoveBinding(String routingKey) {
		try {
			mChannel.queueUnbind(mQueue, mExchange, routingKey);
		} catch (IOException e) {
			//e.printStackTrace();
			mToastMessage.sendToastMessage("queuePurge " + e.getMessage());			
		}
	}

	private void consume() {
		Thread thread = new Thread() {
			@Override
			public void run() {
				QueueingConsumer.Delivery delivery;
				while (mConsumeRunning) {
					//Log.d("RabbitMQMessageConsumer", "inside consume run loop");
					try {
						delivery = mConsumer.nextDelivery(); //blocks until a message is received
						mLastMessage = delivery.getBody();
						writeToLog("RabbitMQMessageConsumer", "last message: " + new String(mLastMessage));
						//Log.d("RabbitMQMessageConsumer", "last message: " + new String(mLastMessage));
						mMessageHandler.post(mReturnMessage);
//						try {
//							mChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
//						} catch (IOException eIO) {
//							Log.e("mLastMessage ", "IOException");							
//							eIO.printStackTrace();
//						} catch (Exception e) {
//							Log.e("mLastMessage ", "inner try error");
//							e.printStackTrace();
//						}
					} catch (InterruptedException ie) {
						writeToLog("RabbitMQMessageConsumer", "InterruptedException: " + ie.getMessage());
						mToastMessage.sendToastMessage(ie.getMessage());
					} catch (ShutdownSignalException sse) {
//						boolean hardError = sse.isHardError();
//						boolean initiatedByApplication = sse.isInitiatedByApplication();
//						Object reason = sse.getReason();
//						Object reference = sse.getReference();
//						Throwable cause = sse.getCause();
//						String localizedMessage = sse.getLocalizedMessage();
//						Log.e("ShutdownSignalException ", "hard error: " + hardError + " initiated by application: " + initiatedByApplication + " reason: " + reason);
//						Log.e("ShutdownSignalException ", "reference: " + reference + " cause: " + cause + " localized message: " + localizedMessage);
//						sse.printStackTrace();
//						mToastMessage.sendToastMessage(sse.getMessage());						
					} catch (ConsumerCancelledException cce) {
//						Log.e("mLastMessage ", "ConsumerCancelledException");
//						cce.printStackTrace();
//						mToastMessage.sendToastMessage(cce.getMessage());
					} catch (Exception e) {
//						Log.e("mLastMessage ", "outer try error");
//						e.printStackTrace();
//						mToastMessage.sendToastMessage(e.getMessage());
					} finally {
//						try {
//							mChannel.queuePurge(mQueue);
//						} catch (Exception e) {
//							mToastMessage.sendToastMessage("queuePurge " + e.getMessage());
//						}
					}
				}
				writeToLog("RabbitMQMessageConsumer", "thread all done");
			}
		};
		thread.start();
	}

	public void dispose() {
		mConsumeRunning = false;
		try {
			if (mChannel != null)
				mChannel.abort();
			if (mConnection!=null)
				mConnection.close();
		} catch (IOException e) {
			//e.printStackTrace();
			mToastMessage.sendToastMessage("queuePurge " + e.getMessage());			
		}
	}

	public void setChannel(Channel channel) {
		mChannel = channel;
	}
	
	public Channel getChannel() {
		return mChannel;
	}

	public void setConnection(Connection connection) {
		mConnection = connection;
	}
	
	public Connection getConnection() {
		return mConnection;
	}

	public void setQueue(String queue) {
		mQueue = queue;
	}
	
	public String getQueue() {
		return mQueue;
	}

	public void setConsumer(QueueingConsumer consumer) {
		mConsumer = consumer;
	}
	
	public QueueingConsumer getConsumer() {
		return mConsumer;
	}
	
    private static void writeToLog(String filter, String msg) {
    	if ("true".equalsIgnoreCase(mResources.getString(R.string.debug))) {
    		Log.d(filter, msg);
    	}
    }
	
}
