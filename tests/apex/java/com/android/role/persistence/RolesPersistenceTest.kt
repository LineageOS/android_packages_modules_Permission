/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.role.persistence

import android.content.ApexEnvironment
import android.content.Context
import android.os.Process
import android.os.UserHandle
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession
import com.android.permission.flags.Flags
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations.initMocks
import org.mockito.MockitoSession
import org.mockito.quality.Strictness

@RunWith(Parameterized::class)
class RolesPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    private lateinit var mockDataDirectory: File
    private lateinit var mockitoSession: MockitoSession
    @Mock lateinit var apexEnvironment: ApexEnvironment
    @Parameterized.Parameter(0) lateinit var stateVersion: StateVersion
    private lateinit var state: RolesState

    private val persistence = RolesPersistenceImpl {}
    private val defaultRoles = mapOf(ROLE_NAME to setOf(HOLDER_1, HOLDER_2))
    private val activeUserIds = mapOf(ROLE_NAME to USER_ID)
    private val stateVersionUndefined = RolesState(VERSION_UNDEFINED, PACKAGE_HASH, defaultRoles)
    private val stateVersionFallbackMigrated =
        RolesState(VERSION_FALLBACK_MIGRATED, PACKAGE_HASH, defaultRoles, setOf(ROLE_NAME))
    private val stateVersionActiveUserIds =
        RolesState(
            VERSION_ACTIVE_USER_IDS,
            PACKAGE_HASH,
            defaultRoles,
            setOf(ROLE_NAME),
            activeUserIds,
        )
    private val user = Process.myUserHandle()

    @get:Rule val flagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Before
    fun setUp() {
        createMockDataDirectory()
        mockApexEnvironment()
        state = getState()
    }

    private fun createMockDataDirectory() {
        mockDataDirectory = context.getDir("mock_data", Context.MODE_PRIVATE)
        mockDataDirectory.listFiles()!!.forEach { assertThat(it.deleteRecursively()).isTrue() }
    }

    private fun mockApexEnvironment() {
        initMocks(this)
        mockitoSession =
            mockitoSession()
                .mockStatic(ApexEnvironment::class.java)
                .strictness(Strictness.LENIENT)
                .startMocking()
        `when`(ApexEnvironment.getApexEnvironment(eq(APEX_MODULE_NAME))).thenReturn(apexEnvironment)
        `when`(apexEnvironment.getDeviceProtectedDataDirForUser(any(UserHandle::class.java))).then {
            File(mockDataDirectory, it.arguments[0].toString()).also { it.mkdirs() }
        }
    }

    @After
    fun finishMockingApexEnvironment() {
        mockitoSession.finishMocking()
    }

    @RequiresFlagsDisabled(Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @Test
    fun testWriteRead() {
        assumeFalse(stateVersion == StateVersion.VERSION_ACTIVE_USER_IDS)
        persistence.writeForUser(state, user)
        val persistedState = persistence.readForUser(user)

        assertThat(persistedState).isEqualTo(state)
    }

    @RequiresFlagsEnabled(Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @Test
    fun testWriteRead_supportsActiveUser() {
        persistence.writeForUser(state, user)
        val persistedState = persistence.readForUser(user)

        assertThat(persistedState).isEqualTo(state)
    }

    @RequiresFlagsDisabled(Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @Test
    fun testWriteCorruptReadFromReserveCopy() {
        assumeFalse(stateVersion == StateVersion.VERSION_ACTIVE_USER_IDS)
        persistence.writeForUser(state, user)
        // Corrupt the primary file.
        RolesPersistenceImpl.getFile(user)
            .writeText("<roles version=\"-1\"><role name=\"com.foo.bar\"><holder")
        val persistedState = persistence.readForUser(user)

        assertThat(persistedState).isEqualTo(state)
    }

    @RequiresFlagsEnabled(Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @Test
    fun testWriteCorruptReadFromReserveCopy_supportsActiveUser() {
        persistence.writeForUser(state, user)
        // Corrupt the primary file.
        RolesPersistenceImpl.getFile(user)
            .writeText("<roles version=\"-1\"><role name=\"com.foo.bar\"><holder")
        val persistedState = persistence.readForUser(user)

        assertThat(persistedState).isEqualTo(state)
    }

    @Test
    fun testDelete() {
        persistence.writeForUser(state, user)
        persistence.deleteForUser(user)
        val persistedState = persistence.readForUser(user)

        assertThat(persistedState).isNull()
    }

    private fun getState(): RolesState =
        when (stateVersion) {
            StateVersion.VERSION_UNDEFINED -> stateVersionUndefined
            StateVersion.VERSION_FALLBACK_MIGRATED -> stateVersionFallbackMigrated
            StateVersion.VERSION_ACTIVE_USER_IDS -> stateVersionActiveUserIds
        }

    enum class StateVersion {
        VERSION_UNDEFINED,
        VERSION_FALLBACK_MIGRATED,
        VERSION_ACTIVE_USER_IDS,
    }

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun data(): Array<StateVersion> = StateVersion.values()

        private const val VERSION_UNDEFINED = -1
        private const val VERSION_FALLBACK_MIGRATED = 1
        private const val VERSION_ACTIVE_USER_IDS = 2
        private const val APEX_MODULE_NAME = "com.android.permission"
        private const val PACKAGE_HASH = "packagesHash"
        private const val ROLE_NAME = "roleName"
        private const val HOLDER_1 = "holder1"
        private const val HOLDER_2 = "holder2"
        private const val USER_ID = 10
    }
}
