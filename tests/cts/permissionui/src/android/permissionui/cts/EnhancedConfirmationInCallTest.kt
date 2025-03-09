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

import android.app.AppOpsManager
import android.app.Instrumentation
import android.app.ecm.EnhancedConfirmationManager
import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.permission.flags.Flags
import android.platform.test.annotations.AppModeFull
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.android.compatibility.common.util.SystemUtil.callWithShellPermissionIdentity
import com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

/**
 * This test verifies the behavior of the Enhanced Confirmation Manager APIs that deal with unknown
 * callers
 */
@AppModeFull(reason = "Instant apps cannot install packages")
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
@RequiresFlagsEnabled(Flags.FLAG_ENHANCED_CONFIRMATION_IN_CALL_APIS_ENABLED)
// @CddTest(requirement = "TBD")
class EnhancedConfirmationInCallTest {
    private val ecm = context.getSystemService(EnhancedConfirmationManager::class.java)!!
    private val packageManager = context.packageManager
    private val addedContacts = mutableMapOf<String, List<Uri>>()

    @JvmField
    @Rule
    val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Before
    fun assumeNotAutoOrTv() {
        Assume.assumeFalse(packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK))
        Assume.assumeFalse(packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE))
    }

    companion object {
        private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
        private val context: Context = instrumentation.targetContext
        private lateinit var voipService: VoipCallHelper

        @JvmStatic
        @BeforeClass
        fun setupVoipService() {
            voipService = VoipCallHelper(context)
            voipService.registerPhoneAccount()
        }

        @JvmStatic
        @AfterClass
        fun tearDownVoipService() {
            voipService.removePhoneAccount()
        }

        const val CONTACT_DISPLAY_NAME = "Alice Bobson"
        const val NON_CONTACT_DISPLAY_NAME = "Eve McEve"
        const val CONTACT_PHONE_NUMBER = "8888888888"
        const val NON_CONTACT_PHONE_NUMBER = "1111111111"
    }

    private fun addContact(displayName: String, phoneNumber: String) {
        runWithShellPermissionIdentity {
            val ops: ArrayList<ContentProviderOperation> = ArrayList()
            ops.add(
                ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                    .withValue(RawContacts.ACCOUNT_TYPE, "test type")
                    .withValue(RawContacts.ACCOUNT_NAME, "test account")
                    .build()
            )
            ops.add(
                ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                    .build()
            )
            ops.add(
                ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Data.MIMETYPE, CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .build()
            )
            val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            val resultsForDisplayName = mutableListOf<Uri>()
            results.forEach { resultsForDisplayName.add(it.uri!!) }
            addedContacts[displayName] = resultsForDisplayName
        }
    }

    private fun removeContact(displayName: String) {
        runWithShellPermissionIdentity {
            var totalRowsRemoved = 0
            for (data in addedContacts[displayName] ?: emptyList()) {
                totalRowsRemoved += context.contentResolver.delete(data, null)
            }
            // There are multiple contacts tables, and removing from the raw_contacts table
            // can cause row removals from the data table, so we may get some uris that don't
            // report a delete, but we should get at least one, and not more than the number of uris
            Assert.assertNotEquals(
                "Expected at least one contact row to be removed",
                0,
                totalRowsRemoved,
            )
            Assert.assertTrue(
                "Unexpectedly large number of contact rows removed",
                totalRowsRemoved <= (addedContacts[displayName]?.size ?: 0),
            )
            addedContacts.remove(displayName)
        }
    }

    @After
    fun tearDown() {
        voipService.endCallAndWaitForInactive()
        addedContacts.keys.forEach { removeContact(it) }
    }

    private fun isSettingRestricted(): Boolean {
        return callWithShellPermissionIdentity {
            ecm.isRestricted(context.packageName, AppOpsManager.OPSTR_REQUEST_INSTALL_PACKAGES)
        }
    }

    @Test
    fun testIncomingCall_NonContact() {
        voipService.createCallAndWaitForActive(NON_CONTACT_DISPLAY_NAME, NON_CONTACT_PHONE_NUMBER)
        Assert.assertTrue(isSettingRestricted())
        voipService.endCallAndWaitForInactive()
        Assert.assertFalse(isSettingRestricted())
    }

    @Test
    fun testIncomingCall_Contact_DisplayNameMatches_PhoneNotGiven() {
        addContact(CONTACT_DISPLAY_NAME, CONTACT_PHONE_NUMBER)
        // If no phone number is given, the display name will be checked
        voipService.createCallAndWaitForActive(CONTACT_DISPLAY_NAME, CONTACT_PHONE_NUMBER)
        Assert.assertFalse(isSettingRestricted())
        voipService.endCallAndWaitForInactive()
        Assert.assertFalse(isSettingRestricted())
    }

    @Test
    fun testIncomingCall_Contact_PhoneNumberMatches() {
        addContact(CONTACT_DISPLAY_NAME, CONTACT_PHONE_NUMBER)
        // If the phone number matches, the display name is not checked
        voipService.createCallAndWaitForActive(NON_CONTACT_DISPLAY_NAME, CONTACT_PHONE_NUMBER)
        Assert.assertFalse(isSettingRestricted())
        voipService.endCallAndWaitForInactive()
        Assert.assertFalse(isSettingRestricted())
    }

    @Test
    fun testCall_DoesntBecomeTrustedIfCallerAddedDuringCall() {
        val tempContactDisplay = "TEMP CONTACT"
        val tempContactPhone = "999-999-9999"
        voipService.createCallAndWaitForActive(tempContactDisplay, tempContactPhone)
        addContact(tempContactDisplay, tempContactPhone)
        // State should not be recomputed just because the contact is newly added
        Assert.assertTrue(isSettingRestricted())
        voipService.endCallAndWaitForInactive()
        voipService.createCallAndWaitForActive(tempContactDisplay, tempContactPhone)
        // A new call should recognize our contact, and mark the call as trusted
        Assert.assertFalse(isSettingRestricted())
    }
}
