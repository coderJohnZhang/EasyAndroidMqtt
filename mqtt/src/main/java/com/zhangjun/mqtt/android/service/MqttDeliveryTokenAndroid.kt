/*******************************************************************************
 * Copyright (c) 1999, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.paho.android.service

import com.zhangjun.mqtt.android.service.MqttAndroidClient
import com.zhangjun.mqtt.android.service.MqttTokenAndroid
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage

/**
 *
 *
 * Implementation of the IMqttDeliveryToken interface for use from within the
 * MqttAndroidClient implementation
 */
internal class MqttDeliveryTokenAndroid(client: MqttAndroidClient,
                                        userContext: Any?, listener: IMqttActionListener?, // The message which is being tracked by this token
                                        private var message: MqttMessage?) : MqttTokenAndroid(client, userContext, listener), IMqttDeliveryToken {

    /**
     * @see IMqttDeliveryToken.getMessage
     */
    @Throws(MqttException::class)
    override fun getMessage(): MqttMessage? {
        return message
    }

    fun setMessage(message: MqttMessage) {
        this.message = message
    }

    fun notifyDelivery(delivered: MqttMessage) {
        message = delivered
        super.notifyComplete()
    }

}
