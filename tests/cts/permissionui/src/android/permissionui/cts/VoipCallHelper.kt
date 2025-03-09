/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.permissionui.cts

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.permissionui.cts.VoipCallHelper.Companion.EXTRA_DISPLAY_NAME
import android.permissionui.cts.VoipCallHelper.Companion.awaitingCallStateLatch
import android.permissionui.cts.VoipCallHelper.Companion.currentActiveConnection
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert

/** A helper class which can register a phone account, and make/end VOIP phone calls */
class VoipCallHelper(val context: Context) {
    private val telecomManager by lazy { context.getSystemService(TelecomManager::class.java) }
    private lateinit var phoneAccount: PhoneAccount
    private val accountHandle =
        PhoneAccountHandle(
            ComponentName(context, VoipHelperTestConnectionService::class.java),
            "cts-voip-helper-test",
            Process.myUserHandle(),
        )

    init {
        registerPhoneAccount()
    }

    companion object {
        var currentActiveConnection: VoIPConnection? = null
        var awaitingCallStateLatch: CallPlacedLatch? = null

        const val EXTRA_DISPLAY_NAME = "display_name"
        const val CUSTOM_ADDRESS_SCHEMA = "custom_schema"
        const val CALL_STATE_WAIT_MS = 1000L
        const val CALL_TIMEOUT_MS = 10000L
    }

    fun registerPhoneAccount() {
        val phoneAccountBuilder = PhoneAccount.builder(accountHandle, "CTS VOIP HELPER")
        phoneAccountBuilder.setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
        // see b/343674176. Some OEMs expect the PhoneAccount.getExtras() to be non-null
        val defaultBundle = Bundle()
        phoneAccountBuilder.setExtras(defaultBundle)

        // build and register the PhoneAccount via the Platform API
        phoneAccount = phoneAccountBuilder.build()
        telecomManager.registerPhoneAccount(phoneAccount)
    }

    fun removePhoneAccount() {
        telecomManager.unregisterPhoneAccount(phoneAccount.accountHandle)
    }

    fun createCallAndWaitForActive(displayName: String?, phoneNumber: String?) {
        val extras = Bundle()

        val phoneUri =
            if (phoneNumber != null) {
                Uri.fromParts(PhoneAccount.SCHEME_TEL, phoneNumber, null)
            } else {
                // If we don't have a phone number, provide a custom address URI, like many VOIP
                // apps that aren't tied to a phone number do
                Uri.fromParts(CUSTOM_ADDRESS_SCHEMA, "custom_address", null)
            }
        if (displayName != null) {
            extras.putString(EXTRA_DISPLAY_NAME, displayName)
        }
        extras.putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, phoneUri)
        awaitingCallStateLatch = CallPlacedLatch(phoneUri, displayName)
        telecomManager.addNewIncomingCall(phoneAccount.accountHandle, extras)
        Assert.assertTrue(
            "Timed out waiting for call to start",
            awaitingCallStateLatch!!.await(CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS),
        )
        // TODO b/379941144: Replace wait with waiting until a test InCallService gets a callback
        Thread.sleep(CALL_STATE_WAIT_MS)
    }

    fun endCallAndWaitForInactive() {
        currentActiveConnection?.let { connection ->
            connection.setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
            connection.destroy()
            // TODO b/379941144: Replace wait with waiting until a test InCallService gets a
            //  callback
            Thread.sleep(CALL_STATE_WAIT_MS)
        }
        currentActiveConnection = null
    }
}

class CallPlacedLatch(val address: Uri?, val displayName: String?) : CountDownLatch(1) {
    fun nameAndNumberMatch(connection: Connection): Boolean {
        return connection.address == address && connection.callerDisplayName == displayName
    }
}

class VoIPConnection : Connection() {
    init {
        setConnectionProperties(PROPERTY_SELF_MANAGED)
        setAudioModeIsVoip(true)
        setActive()
    }

    override fun onShowIncomingCallUi() {
        super.onShowIncomingCallUi()
        setActive()
        currentActiveConnection = this
        if (awaitingCallStateLatch?.nameAndNumberMatch(this) == true) {
            awaitingCallStateLatch?.countDown()
        }
    }
}

class VoipHelperTestConnectionService : ConnectionService() {
    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle,
        request: ConnectionRequest,
    ): Connection {
        return createConnection(request)
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ): Connection {
        return createConnection(request)
    }

    private fun createConnection(request: ConnectionRequest?): Connection {
        val connection = VoIPConnection()
        if (request?.extras?.containsKey(EXTRA_DISPLAY_NAME) == true) {
            connection.setCallerDisplayName(
                request.extras.getString(EXTRA_DISPLAY_NAME),
                TelecomManager.PRESENTATION_ALLOWED,
            )
            connection.setAddress(
                request.extras.getParcelable(
                    TelecomManager.EXTRA_INCOMING_CALL_ADDRESS,
                    Uri::class.java,
                ),
                TelecomManager.PRESENTATION_ALLOWED,
            )
        }
        return connection
    }
}
