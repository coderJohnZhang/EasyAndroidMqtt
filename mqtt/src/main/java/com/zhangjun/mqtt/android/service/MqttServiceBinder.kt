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

import android.os.Binder

/**
 * What the Service passes to the Activity on binding:-
 *
 *  * a reference to the Service
 *  * the activityToken provided when the Service was started
 *
 *
 */
internal class MqttServiceBinder(
        /**
         * @return a reference to the Service
         */
        val service: MqttService) : Binder() {
    /**
     * @return the activityToken provided when the Service was started
     */
    var activityToken: String? = null

}
