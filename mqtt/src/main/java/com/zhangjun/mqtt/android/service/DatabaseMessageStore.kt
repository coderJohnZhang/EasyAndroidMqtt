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
 *
 * Contributors:
 * James Sutton - Removing SQL Injection vunerability (bug 467378)
 */
package com.zhangjun.mqtt.android.service

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import org.eclipse.paho.client.mqttv3.MqttMessage

/**
 * Implementation of the [MessageStore] interface, using a SQLite database
 *
 */
internal class DatabaseMessageStore(service: MqttService, context: Context) : MessageStore {

    // the database
    private var db: SQLiteDatabase? = null

    // a SQLiteOpenHelper specific for this database
    private var mqttDb: MQTTDatabaseHelper? = null

    // a place to send trace data
    private var traceHandler: MqttTraceHandler? = null

    /**
     * We need a SQLiteOpenHelper to handle database creation and updating
     *
     */
    private class MQTTDatabaseHelper(traceHandler: MqttTraceHandler, context: Context)
        : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        // a place to send trace data
        private var traceHandler: MqttTraceHandler? = null

        init {
            this.traceHandler = traceHandler
        }

        /**
         * When the database is (re)created, create our table
         *
         * @param database
         */
        override fun onCreate(database: SQLiteDatabase) {
            val createArrivedTableStatement = ("CREATE TABLE "
                    + ARRIVED_MESSAGE_TABLE_NAME + "("
                    + MqttServiceConstants.MESSAGE_ID + " TEXT PRIMARY KEY, "
                    + MqttServiceConstants.CLIENT_HANDLE + " TEXT, "
                    + MqttServiceConstants.DESTINATION_NAME + " TEXT, "
                    + MqttServiceConstants.PAYLOAD + " BLOB, "
                    + MqttServiceConstants.QOS + " INTEGER, "
                    + MqttServiceConstants.RETAINED + " TEXT, "
                    + MqttServiceConstants.DUPLICATE + " TEXT, " + MTIMESTAMP
                    + " INTEGER" + ");")
            traceHandler?.traceDebug(TAG, "onCreate {"
                    + createArrivedTableStatement + "}")
            try {
                database.execSQL(createArrivedTableStatement)
                traceHandler?.traceDebug(TAG, "created the table")
            } catch (e: SQLException) {
                traceHandler?.traceException(TAG, "onCreate", e)
                throw e
            }

        }

        /**
         * To upgrade the database, drop and recreate our table
         *
         * @param db
         * the database
         * @param oldVersion
         * ignored
         * @param newVersion
         * ignored
         */
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            traceHandler?.traceDebug(TAG, "onUpgrade")
            try {
                db.execSQL("DROP TABLE IF EXISTS $ARRIVED_MESSAGE_TABLE_NAME")
            } catch (e: SQLException) {
                traceHandler?.traceException(TAG, "onUpgrade", e)
                throw e
            }
            onCreate(db)
            traceHandler?.traceDebug(TAG, "onUpgrade complete")
        }

        companion object {
            // TAG used for indentify trace data etc.
            private const val TAG = "MQTTDatabaseHelper"

            private const val DATABASE_NAME = "mqttAndroidService.db"

            // database version, used to recognise when we need to upgrade,delete and recreate
            private const val DATABASE_VERSION = 1
        }
    }

    init {
        this.traceHandler = service

        // Open message database
        mqttDb = MQTTDatabaseHelper(traceHandler as MqttService, context)

        // Android documentation suggests that this perhaps
        // could/should be done in another thread, but as the
        // database is only one table, I doubt it matters...

        traceHandler?.traceDebug(TAG, "DatabaseMessageStore<init> complete")
    }

    /**
     * Store an MQTT message
     *
     * @param clientHandle
     * identifier for the client storing the message
     * @param topic
     * The topic on which the message was published
     * @param message
     * the arrived MQTT message
     * @return an identifier for the message, so that it can be removed when appropriate
     */
    override fun storeArrived(clientHandle: String?, topic: String,
                              message: MqttMessage): String {

        db = mqttDb!!.writableDatabase

        traceHandler!!.traceDebug(TAG, "storeArrived{" + clientHandle + "}, {"
                + message.toString() + "}")

        val payload = message.payload
        val qos = message.qos
        val retained = message.isRetained
        val duplicate = message.isDuplicate

        val values = ContentValues()
        val id = java.util.UUID.randomUUID().toString()
        values.put(MqttServiceConstants.MESSAGE_ID, id)
        values.put(MqttServiceConstants.CLIENT_HANDLE, clientHandle)
        values.put(MqttServiceConstants.DESTINATION_NAME, topic)
        values.put(MqttServiceConstants.PAYLOAD, payload)
        values.put(MqttServiceConstants.QOS, qos)
        values.put(MqttServiceConstants.RETAINED, retained)
        values.put(MqttServiceConstants.DUPLICATE, duplicate)
        values.put(MTIMESTAMP, System.currentTimeMillis())
        try {
            db!!.insertOrThrow(ARRIVED_MESSAGE_TABLE_NAME,
                    null, values)
        } catch (e: SQLException) {
            traceHandler?.traceException(TAG, "onUpgrade", e)
            throw e
        }

        val count = getArrivedRowCount(clientHandle)
        traceHandler?.traceDebug(TAG, "storeArrived: inserted message with id of {"
                + id
                + "} - Number of messages in database for this clientHandle = "
                + count)
        return id
    }

    private fun getArrivedRowCount(clientHandle: String?): Int {
        var count = 0
        val projection = arrayOf(MqttServiceConstants.MESSAGE_ID)
        val selection = MqttServiceConstants.CLIENT_HANDLE + "=?"
        val selectionArgs = arrayOfNulls<String>(1)
        selectionArgs[0] = clientHandle
        val c = db!!.query(
                ARRIVED_MESSAGE_TABLE_NAME, // Table Name
                projection, // The columns to return;
                selection, // Columns for WHERE Clause
                selectionArgs, // The sort order
                null, // The values for the WHERE Cause
                null, // Don't group the rows
                null // Don't filter by row groups
        )

        if (c.moveToFirst()) {
            count = c.getInt(0)
        }
        c.close()
        return count
    }

    /**
     * Delete an MQTT message.
     *
     * @param clientHandle
     * identifier for the client which stored the message
     * @param id
     * the identifying string returned when the message was stored
     *
     * @return true if the message was found and deleted
     */
    override fun discardArrived(clientHandle: String?, id: String): Boolean {

        db = mqttDb!!.writableDatabase

        traceHandler!!.traceDebug(TAG, "discardArrived{" + clientHandle + "}, {"
                + id + "}")
        val rows: Int
        val selectionArgs = arrayOfNulls<String>(2)
        selectionArgs[0] = id
        selectionArgs[1] = clientHandle

        try {
            rows = db!!.delete(ARRIVED_MESSAGE_TABLE_NAME,
                    MqttServiceConstants.MESSAGE_ID + "=? AND "
                            + MqttServiceConstants.CLIENT_HANDLE + "=?",
                    selectionArgs)
        } catch (e: SQLException) {
            traceHandler?.traceException(TAG, "discardArrived", e)
            throw e
        }

        if (rows != 1) {
            traceHandler?.traceError(TAG,
                    "discardArrived - Error deleting message {" + id
                            + "} from database: Rows affected = " + rows)
            return false
        }
        val count = getArrivedRowCount(clientHandle)
        traceHandler?.traceDebug(TAG, "discardArrived - Message deleted successfully. - messages in db for this clientHandle $count")
        return true
    }

    /**
     * Get an iterator over all messages stored (optionally for a specific client)
     *
     * @param clientHandle
     * identifier for the client.<br></br>
     * If null, all messages are retrieved
     * @return iterator of all the arrived MQTT messages
     */
    override fun getAllArrivedMessages(clientHandle: String?): Iterator<MessageStore.StoredMessage> {
        return object : Iterator<MessageStore.StoredMessage> {
            private var c: Cursor? = null
            private var hasNext: Boolean = false
            private val selectionArgs = arrayOf(clientHandle)

            init {
                db = mqttDb!!.writableDatabase
                // anonymous initialiser to start a suitable query
                // and position at the first row, if one exists
                if (clientHandle == null) {
                    c = db!!.query(ARRIVED_MESSAGE_TABLE_NAME, null, null, null, null, null,
                            "mtimestamp ASC")
                } else {
                    c = db!!.query(ARRIVED_MESSAGE_TABLE_NAME, null,
                            MqttServiceConstants.CLIENT_HANDLE + "=?",
                            selectionArgs, null, null,
                            "mtimestamp ASC")
                }
                hasNext = c!!.moveToFirst()
            }

            override fun hasNext(): Boolean {
                if (!hasNext) {
                    c!!.close()
                }
                return hasNext
            }

            override fun next(): MessageStore.StoredMessage {
                val messageId = c!!.getString(c!!
                        .getColumnIndex(MqttServiceConstants.MESSAGE_ID))
                val clientHandle0 = c!!.getString(c!!
                        .getColumnIndex(MqttServiceConstants.CLIENT_HANDLE))
                val topic = c!!.getString(c!!
                        .getColumnIndex(MqttServiceConstants.DESTINATION_NAME))
                val payload = c!!.getBlob(c!!
                        .getColumnIndex(MqttServiceConstants.PAYLOAD))
                val qos = c!!.getInt(c!!.getColumnIndex(MqttServiceConstants.QOS))
                val retained = java.lang.Boolean.parseBoolean(c!!.getString(c!!
                        .getColumnIndex(MqttServiceConstants.RETAINED)))
                val dup = java.lang.Boolean.parseBoolean(c!!.getString(c!!
                        .getColumnIndex(MqttServiceConstants.DUPLICATE)))

                // build the result
                val message = MqttMessageHack(payload)
                message.qos = qos
                message.isRetained = retained
                message.isDuplicate = dup

                // move on
                hasNext = c!!.moveToNext()
                return DbStoredData(messageId, clientHandle0, topic, message)
            }
        }
    }

    /**
     * Delete all messages (optionally for a specific client)
     *
     * @param clientHandle
     * identifier for the client.<br></br>
     * If null, all messages are deleted
     */
    override fun clearArrivedMessages(clientHandle: String?) {

        db = mqttDb!!.writableDatabase
        val selectionArgs = arrayOfNulls<String>(1)
        selectionArgs[0] = clientHandle

        val rows: Int
        rows = if (clientHandle == null) {
            traceHandler!!.traceDebug(TAG,
                    "clearArrivedMessages: clearing the table")
            db!!.delete(ARRIVED_MESSAGE_TABLE_NAME, null, null)
        } else {
            traceHandler!!.traceDebug(TAG,
                    "clearArrivedMessages: clearing the table of "
                            + clientHandle + " messages")
            db!!.delete(ARRIVED_MESSAGE_TABLE_NAME,
                    MqttServiceConstants.CLIENT_HANDLE + "=?",
                    selectionArgs)

        }
        traceHandler?.traceDebug(TAG, "clearArrivedMessages: rows affected = $rows")
    }

    private inner class DbStoredData internal constructor(override val messageId: String,
                                                          override val clientHandle: String,
                                                          override val topic: String,
                                                          override val message: MqttMessage) : MessageStore.StoredMessage

    /**
     * A way to get at the "setDuplicate" method of MqttMessage
     */
    private inner class MqttMessageHack(payload: ByteArray) : MqttMessage(payload) {

        public override fun setDuplicate(dup: Boolean) {
            super.setDuplicate(dup)
        }
    }

    override fun close() {
        if (this.db != null)
            this.db!!.close()

    }

    companion object {

        // TAG used for identify trace data etc.
        private const val TAG = "DatabaseMessageStore"

        // One "private" database column name
        // The other database column names are defined in MqttServiceConstants
        private const val MTIMESTAMP = "mtimestamp"

        // the name of the table in the database to which we will save messages
        private const val ARRIVED_MESSAGE_TABLE_NAME = "MqttArrivedMessageTable"
    }

}