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
package com.zhangjun.mqtt.android.service

import com.zhangjun.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage

/**
 *
 *
 * Implementation of the IMqttToken interface for use from within the
 * MqttAndroidClient implementation
 */

internal open class MqttTokenAndroid
/**
 * Constructor for use with subscribe operations
 *
 * @param client used to pass MqttAndroidClient object
 * @param userContext used to pass context
 * @param listener optional listener that will be notified when the action completes. Use null if not required.
 * @param topics topics to subscribe to, which can include wildcards.
 */
@JvmOverloads constructor(private val client: MqttAndroidClient,
                          private var userContext: Any?, private var listener: IMqttActionListener?, private val topics: Array<String>? = null) : IMqttToken {

    @Volatile
    private var isComplete: Boolean = false

    @Volatile
    private var lastException: MqttException? = null

    private val waitObject = Object()

    private var delegate: IMqttToken? = null // specifically for getMessageId

    private var pendingException: MqttException? = null

    /**
     * @see IMqttToken.waitForCompletion
     */
    @Throws(MqttException::class, MqttSecurityException::class)
    override fun waitForCompletion() {
        synchronized(waitObject) {
            try {
                waitObject.wait()
            } catch (e: InterruptedException) {
                // do nothing
            }

        }
        if (pendingException != null) {
            throw pendingException as MqttException
        }
    }

    /**
     * @see IMqttToken.waitForCompletion
     */
    @Throws(MqttException::class, MqttSecurityException::class)
    override fun waitForCompletion(timeout: Long) {
        synchronized(waitObject) {
            try {
                waitObject.wait(timeout)
            } catch (e: InterruptedException) {
                // do nothing
            }

            if (!isComplete) {
                throw MqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT.toInt())
            }
            if (pendingException != null) {
                throw pendingException as MqttException
            }
        }
    }

    /**
     * notify successful completion of the operation
     */
    fun notifyComplete() {
        synchronized(waitObject) {
            isComplete = true
            waitObject.notifyAll()
            if (listener != null) {
                listener!!.onSuccess(this)
            }
        }
    }

    /**
     * notify unsuccessful completion of the operation
     */
    fun notifyFailure(exception: Throwable) {
        synchronized(waitObject) {
            isComplete = true
            pendingException = if (exception is MqttException) {
                exception
            } else {
                MqttException(exception)
            }
            waitObject.notifyAll()
            if (exception is MqttException) {
                lastException = exception
            }
            if (listener != null) {
                listener!!.onFailure(this, exception)
            }
        }

    }

    /**
     * @see IMqttToken.isComplete
     */
    override fun isComplete(): Boolean {
        return isComplete
    }

    fun setComplete(complete: Boolean) {
        isComplete = complete
    }

    /**
     * @see IMqttToken.getException
     */
    override fun getException(): MqttException? {
        return lastException
    }

    fun setException(exception: MqttException) {
        lastException = exception
    }

    /**
     * @see IMqttToken.getClient
     */
    override fun getClient(): IMqttAsyncClient {
        return client
    }

    /**
     * @see IMqttToken.setActionCallback
     */
    override fun setActionCallback(listener: IMqttActionListener) {
        this.listener = listener
    }

    /**
     * @see IMqttToken.getActionCallback
     */
    override fun getActionCallback(): IMqttActionListener? {
        return listener
    }

    /**
     * @see IMqttToken.getTopics
     */
    override fun getTopics(): Array<String>? {
        return topics
    }

    /**
     * @see IMqttToken.setUserContext
     */
    override fun setUserContext(userContext: Any) {
        this.userContext = userContext

    }

    /**
     * @see IMqttToken.getUserContext
     */
    override fun getUserContext(): Any? {
        return userContext
    }

    fun setDelegate(delegate: IMqttDeliveryToken?) {
        this.delegate = delegate
    }

    /**
     * @see IMqttToken.getMessageId
     */
    override fun getMessageId(): Int {
        return if (delegate != null) delegate!!.messageId else 0
    }

    override fun getResponse(): MqttWireMessage {
        return delegate!!.response
    }

    override fun getSessionPresent(): Boolean {
        return delegate!!.sessionPresent
    }

    override fun getGrantedQos(): IntArray {
        return delegate!!.grantedQos
    }

}
