package com.guzzardo.android.willyshmo.kotlintictacdoh

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection

class RabbitMQConnection(var connection: Connection?, var channel: Channel?) {

}