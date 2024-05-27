/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
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

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttPingSender
import org.eclipse.paho.client.mqttv3.internal.ClientComms

/**
 * Default ping sender implementation on Android. It is based on AlarmManager.
 *
 *
 * This class implements the [MqttPingSender] pinger interface
 * allowing applications to send ping packet to server every keep alive interval.
 *
 *
 * @see MqttPingSender
 */
internal class AlarmPingSender(private val service: MqttService) : MqttPingSender {


    private var comms: ClientComms? = null
    private var alarmReceiver: BroadcastReceiver? = null
    private val that: AlarmPingSender = this
    private var pendingIntent: PendingIntent? = null
    @Volatile
    private var hasStarted = false

    override fun init(comms: ClientComms) {
        this.comms = comms
        this.alarmReceiver = AlarmReceiver()
    }

    override fun start() {
        val action = MqttServiceConstants.PING_SENDER + comms!!.client.clientId
        Log.d(TAG, "Register alarm receiver to MqttService$action")
        service.registerReceiver(alarmReceiver, IntentFilter(action))

        pendingIntent = PendingIntent.getBroadcast(service, 0, Intent(action), PendingIntent.FLAG_UPDATE_CURRENT)

        schedule(comms!!.keepAlive)
        hasStarted = true
    }

    override fun stop() {
        Log.d(TAG, "Unregister alarm receiver to MqttService" + comms!!.client.clientId)
        if (hasStarted) {
            if (pendingIntent != null) {
                // Cancel Alarm.
                val alarmManager = service.getSystemService(Service.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(pendingIntent)
            }

            hasStarted = false
            try {
                service.unregisterReceiver(alarmReceiver)
            } catch (e: IllegalArgumentException) {
                //Ignore unregister errors.
            }

        }
    }

    override fun schedule(delayInMilliseconds: Long) {
        val nextAlarmInMilliseconds = System.currentTimeMillis() + delayInMilliseconds
        Log.d(TAG, "Schedule next alarm at $nextAlarmInMilliseconds")
        val alarmManager = service.getSystemService(Service.ALARM_SERVICE) as AlarmManager

        when {
            Build.VERSION.SDK_INT >= 23 -> {
                // In SDK 23 and above, dosing will prevent setExact, setExactAndAllowWhileIdle will force
                // the device to run this task whilst dosing.
                Log.d(TAG, "Alarm schedule using setExactAndAllowWhileIdle, next: $delayInMilliseconds")
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
                        pendingIntent)
            }
            Build.VERSION.SDK_INT >= 19 -> {
                Log.d(TAG, "Alarm schedule using setExact, delay: $delayInMilliseconds")
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
                        pendingIntent)
            }
            else -> alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
                    pendingIntent)
        }
    }

    /*
	 * This class sends PingReq packet to MQTT broker
	 */
    internal inner class AlarmReceiver : BroadcastReceiver() {
        private var wakelock: WakeLock? = null
        private val wakeLockTag = MqttServiceConstants.PING_WAKELOCK + that.comms!!.client.clientId

        @SuppressLint("Wakelock", "WakelockTimeout")
        override fun onReceive(context: Context, intent: Intent) {
            // According to the docs, "Alarm Manager holds a CPU wake lock as
            // long as the alarm receiver's onReceive() method is executing.
            // This guarantees that the phone will not sleep until you have
            // finished handling the broadcast.", but this class still get
            // a wake lock to wait for ping finished.

            Log.d(TAG, "Sending Ping at:" + System.currentTimeMillis())

            val pm = service.getSystemService(Service.POWER_SERVICE) as PowerManager
            wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag)
            wakelock?.acquire()

            // Assign new callback to token to execute code after PingResp
            // arrives. Get another wakelock even receiver already has one,
            // release it until ping response returns.
            val token = comms!!.checkForActivity(object : IMqttActionListener {

                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d(TAG, "Success. Release lock(" + wakeLockTag + "):"
                            + System.currentTimeMillis())
                    //Release wakelock when it is done.
                    wakelock!!.release()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.d(TAG, "Failure. Release lock(" + wakeLockTag + "):" + System.currentTimeMillis())
                    //Release wakelock when it is done.
                    wakelock!!.release()
                }
            })


            if (token == null && wakelock!!.isHeld) {
                wakelock!!.release()
            }
        }
    }

    companion object {
        // Identifier for Intents, log messages, etc..
        private const val TAG = "AlarmPingSender"
    }
}
