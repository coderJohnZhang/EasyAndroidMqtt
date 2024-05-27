package com.zhangjun.easy.android.mqtt

import android.util.Base64
import android.util.Log
import com.zhangjun.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


/**
 * Created by ZhangJun on 2019/1/3.
 */
class MqttManager private constructor() {

    private var mqttAndroidClient: MqttAndroidClient? = null
    private lateinit var mqttConnectOptions: MqttConnectOptions

    private object MqttManagerHolder {
        val INSTANCE = MqttManager()
    }

    fun init(node: String, port: Int, clientId: String) {
        try {
            if (mqttAndroidClient == null) {
                mqttAndroidClient = MqttAndroidClient(MyApplication.instance, "ssl://$node:$port", clientId)
            } else {
                mqttAndroidClient!!.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        Log.d(TAG, "mqtt connectComplete reconnect = $reconnect")
                    }

                    override fun connectionLost(cause: Throwable?) {
                        if (cause != null) {
                            Log.d(TAG, "mqtt connectionLost cause = " + cause.message)
                        }
                        connect()
                    }

                    @Throws(Exception::class)
                    override fun messageArrived(topic: String, message: MqttMessage) {
                        val str = String(message.payload)
                        Log.d(TAG, "messageArrived str = $str")
                        val jsonObject = JSONObject(str)
                        val event = jsonObject.optJSONObject("event")
                        val header = event.optJSONObject("header")
                        val namespace = header.optString("namespace")
                        val name = header.optString("name")
                        val payload = event.optJSONObject("payload")
                        val message1 = MqttMessageBean()
                        message1.messageId = namespace.plus(name)
                        message1.messageContent = payload
                        EventBus.getDefault().post(MessageEvent.MqttMessageEvent(message1))
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken) {
                        //do nothing
                    }
                })

                mqttConnectOptions = MqttConnectOptions()
                mqttConnectOptions.socketFactory = sslSocketFactory

                mqttConnectOptions.isAutomaticReconnect = true
                mqttConnectOptions.isCleanSession = false
                // 设置超时时间，单位：秒
                mqttConnectOptions.connectionTimeout = 10
                // 心跳包发送间隔，单位：秒
                mqttConnectOptions.keepAliveInterval = 20
                // 用户名
                mqttConnectOptions.userName = decryptToken()
                // 密码
                mqttConnectOptions.password = MyApplication.instance.packageName.toCharArray()
                connect()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /**
     * 解密token
     *
     * @return decodeToken
     */
    fun decryptToken(): String? {
        var decodeToken = SharedPreferencesUtil.instance.getString("token", "")
        try {
            decodeToken = String(Base64.decode(decodeToken, Base64.NO_WRAP), Charsets.UTF_8)
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        return decodeToken
    }

    private fun connect() {
        if (mqttAndroidClient != null && !mqttAndroidClient!!.isConnected) {
            mqttAndroidClient!!.connect(mqttConnectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    val disconnectedBufferOptions = DisconnectedBufferOptions()
                    disconnectedBufferOptions.bufferSize = 100
                    disconnectedBufferOptions.isBufferEnabled = true
                    disconnectedBufferOptions.isPersistBuffer = false
                    disconnectedBufferOptions.isDeleteOldestMessages = false
                    mqttAndroidClient!!.setBufferOpts(disconnectedBufferOptions)
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.d(TAG, " mqtt connect fail exception = " + exception.message)
                }
            })
        }
    }

    private val sslSocketFactory: SSLSocketFactory
        get() {
            try {
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustManager, SecureRandom())
                return sslContext.socketFactory
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

        }

    private val trustManager: Array<TrustManager>
        get() = arrayOf(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                //do nothing
            }

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                //do nothing
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        })

    /**
     * 订阅消息
     */
    fun subscribeTopic(subTopic: String, qos: Int) {
        try {
            if (mqttAndroidClient != null && mqttAndroidClient!!.isConnected) {
                mqttAndroidClient!!.subscribe(subTopic, qos)
            }
        } catch (ex: MqttException) {
            System.err.println("Exception while subscribing")
            ex.printStackTrace()
        }

    }

    /**
     * 发布消息
     */
    fun publishMessage(pubTopic: String, qos: Int, content: String) {
        try {
            if (mqttAndroidClient != null && mqttAndroidClient!!.isConnected) {
                mqttAndroidClient!!.publish(pubTopic, content.toByteArray(), qos, false)
            }
        } catch (e: MqttException) {
            System.err.println("Error Publishing: " + e.message)
            e.printStackTrace()
        }

    }

    fun release() {
        try {
            if (mqttAndroidClient != null) {
                mqttAndroidClient!!.unregisterResources()
                if (mqttAndroidClient!!.isConnected) {
                    mqttAndroidClient!!.disconnect()
                }
                mqttAndroidClient!!.close()
                mqttAndroidClient = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private val TAG = MqttManager::class.java.simpleName

        val instance: MqttManager
            get() = MqttManagerHolder.INSTANCE
    }
}
