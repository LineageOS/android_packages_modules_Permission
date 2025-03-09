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
package android.app.rolemultiuser.cts

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.os.UserHandle
import androidx.test.filters.SdkSuppress
import com.android.bedstead.enterprise.annotations.EnsureHasWorkProfile
import com.android.bedstead.enterprise.annotations.RequireRunOnWorkProfile
import com.android.bedstead.enterprise.workProfile
import com.android.bedstead.flags.annotations.RequireFlagsEnabled
import com.android.bedstead.harrier.BedsteadJUnit4
import com.android.bedstead.harrier.DeviceState
import com.android.bedstead.multiuser.annotations.EnsureHasAdditionalUser
import com.android.bedstead.multiuser.annotations.EnsureHasPrivateProfile
import com.android.bedstead.multiuser.annotations.EnsureHasSecondaryUser
import com.android.bedstead.multiuser.annotations.RequireRunNotOnSecondaryUser
import com.android.bedstead.multiuser.annotations.RequireRunOnPrimaryUser
import com.android.bedstead.multiuser.privateProfile
import com.android.bedstead.multiuser.secondaryUser
import com.android.bedstead.nene.TestApis.context
import com.android.bedstead.nene.TestApis.permissions
import com.android.bedstead.nene.TestApis.users
import com.android.bedstead.nene.types.OptionalBoolean
import com.android.bedstead.permissions.CommonPermissions.INTERACT_ACROSS_USERS_FULL
import com.android.bedstead.permissions.CommonPermissions.MANAGE_DEFAULT_APPLICATIONS
import com.android.bedstead.permissions.CommonPermissions.MANAGE_ROLE_HOLDERS
import com.android.bedstead.permissions.annotations.EnsureDoesNotHavePermission
import com.android.bedstead.permissions.annotations.EnsureHasPermission
import com.android.compatibility.common.util.SystemUtil
import com.android.compatibility.common.util.SystemUtil.eventually
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.Objects
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
@RunWith(BedsteadJUnit4::class)
class RoleManagerMultiUserTest {
    @Before
    @Throws(java.lang.Exception::class)
    fun setUp() {
        installAppForAllUsers()
    }

    @After
    @Throws(java.lang.Exception::class)
    fun tearDown() {
        uninstallAppForAllUsers()
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(INTERACT_ACROSS_USERS_FULL, MANAGE_ROLE_HOLDERS)
    @EnsureHasWorkProfile(installInstrumentedApp = OptionalBoolean.TRUE)
    @EnsureHasPrivateProfile(installInstrumentedApp = OptionalBoolean.TRUE)
    @RequireRunOnPrimaryUser
    @Test
    @Throws(Exception::class)
    fun isAvailableAsUserForProfileGroupExclusiveRole() {
        val workProfileRoleManager = getRoleManagerForUser(deviceState.workProfile().userHandle())
        val privateProfileRoleManager =
            getRoleManagerForUser(deviceState.privateProfile().userHandle())

        assertThat(roleManager.isRoleAvailable(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME)).isTrue()
        assertThat(workProfileRoleManager.isRoleAvailable(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
            .isTrue()
        assertThat(privateProfileRoleManager.isRoleAvailable(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
            .isFalse()
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(INTERACT_ACROSS_USERS_FULL, MANAGE_ROLE_HOLDERS)
    @Test
    @Throws(Exception::class)
    fun cannotGetActiveUserForNonCrossUserRole() {
        assertThrows(IllegalArgumentException::class.java) {
            roleManager.getActiveUserForRole(RoleManager.ROLE_SYSTEM_ACTIVITY_RECOGNIZER)
        }
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(MANAGE_ROLE_HOLDERS)
    @EnsureDoesNotHavePermission(INTERACT_ACROSS_USERS_FULL)
    @Test
    @Throws(Exception::class)
    fun cannotGetActiveUserForRoleWithoutInteractAcrossUserPermission() {
        assertThrows(SecurityException::class.java) {
            roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME)
        }
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(INTERACT_ACROSS_USERS_FULL)
    @EnsureDoesNotHavePermission(MANAGE_ROLE_HOLDERS, MANAGE_DEFAULT_APPLICATIONS)
    @Test
    @Throws(Exception::class)
    fun cannotGetActiveUserForRoleWithoutManageRoleAndManageDefaultApplicationsPermission() {
        assertThrows(SecurityException::class.java) {
            roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME)
        }
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(INTERACT_ACROSS_USERS_FULL, MANAGE_ROLE_HOLDERS)
    @Test
    @Throws(Exception::class)
    fun cannotSetActiveUserForNonCrossUserRole() {
        assertThrows(IllegalArgumentException::class.java) {
            roleManager.setActiveUserForRole(
                RoleManager.ROLE_SYSTEM_ACTIVITY_RECOGNIZER,
                Process.myUserHandle(),
                0,
            )
        }
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(MANAGE_ROLE_HOLDERS)
    @EnsureDoesNotHavePermission(INTERACT_ACROSS_USERS_FULL)
    @Test
    @Throws(Exception::class)
    fun cannotSetActiveUserForRoleWithoutInteractAcrossUserPermission() {
        assertThrows(SecurityException::class.java) {
            roleManager.setActiveUserForRole(
                PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME,
                Process.myUserHandle(),
                0,
            )
        }
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(INTERACT_ACROSS_USERS_FULL)
    @EnsureDoesNotHavePermission(MANAGE_ROLE_HOLDERS, MANAGE_DEFAULT_APPLICATIONS)
    @Test
    @Throws(Exception::class)
    fun cannotSetActiveUserForRoleWithoutManageRoleAndManageDefaultApplicationsPermission() {
        assertThrows(SecurityException::class.java) {
            roleManager.setActiveUserForRole(
                PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME,
                Process.myUserHandle(),
                0,
            )
        }
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(INTERACT_ACROSS_USERS_FULL, MANAGE_ROLE_HOLDERS)
    @Test
    @Throws(Exception::class)
    fun cannotSetActiveUserForRoleToNonExistentUser() {
        val targetActiveUser = users().nonExisting().userHandle()
        roleManager.setActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME, targetActiveUser, 0)

        assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
            .isNotEqualTo(targetActiveUser)
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(INTERACT_ACROSS_USERS_FULL, MANAGE_ROLE_HOLDERS)
    @EnsureHasPrivateProfile(installInstrumentedApp = OptionalBoolean.TRUE)
    @Test
    @Throws(Exception::class)
    fun cannotSetActiveUserForRoleToPrivateProfileUser() {
        val targetActiveUser = deviceState.privateProfile().userHandle()
        roleManager.setActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME, targetActiveUser, 0)

        assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
            .isNotEqualTo(targetActiveUser)
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(INTERACT_ACROSS_USERS_FULL, MANAGE_ROLE_HOLDERS)
    @EnsureHasAdditionalUser(installInstrumentedApp = OptionalBoolean.TRUE)
    @EnsureHasSecondaryUser
    @RequireRunNotOnSecondaryUser
    @Test
    @Throws(Exception::class)
    fun cannotSetActiveUserForRoleToUserNotInProfileGroup() {
        val targetActiveUser = deviceState.secondaryUser().userHandle()
        roleManager.setActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME, targetActiveUser, 0)

        assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
            .isNotEqualTo(targetActiveUser)
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(INTERACT_ACROSS_USERS_FULL, MANAGE_ROLE_HOLDERS)
    @EnsureHasWorkProfile
    @EnsureHasAdditionalUser(installInstrumentedApp = OptionalBoolean.TRUE)
    @EnsureHasSecondaryUser
    @RequireRunNotOnSecondaryUser
    @Test
    @Throws(java.lang.Exception::class)
    fun ensureRoleHasActiveUser() {
        val primaryUser = deviceState.initialUser().userHandle()
        val primaryUserRoleManager = getRoleManagerForUser(primaryUser)
        val secondaryUser = deviceState.secondaryUser().userHandle()
        val secondaryUserRoleManager = getRoleManagerForUser(secondaryUser)

        assertWithMessage(
                "Expected active user in profile group for user ${primaryUser.identifier}"
            )
            .that(primaryUserRoleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
            .isNotNull()
        assertWithMessage(
                "Expected active user in profile group for user ${secondaryUser.identifier}"
            )
            .that(
                secondaryUserRoleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME)
            )
            .isNotNull()
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(INTERACT_ACROSS_USERS_FULL, MANAGE_ROLE_HOLDERS)
    @EnsureHasWorkProfile
    @Test
    @Throws(java.lang.Exception::class)
    fun ensureOnlyActiveUserIsRoleHolder() {
        try {
            // Set test default role holder. Ensures fallbacks to a default holder
            roleManager.setDefaultHoldersForTest(
                PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME,
                listOf(APP_PACKAGE_NAME),
            )

            val activeUser = roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME)!!
            // Test app install might take a moment
            eventually { assertExpectedProfileHasRoleUsingGetRoleHoldersAsUser(activeUser) }
        } finally {
            // Clear test default role holder
            roleManager.setDefaultHoldersForTest(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME, null)
        }
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(INTERACT_ACROSS_USERS_FULL, MANAGE_ROLE_HOLDERS)
    @EnsureDoesNotHavePermission(MANAGE_DEFAULT_APPLICATIONS)
    @EnsureHasWorkProfile(installInstrumentedApp = OptionalBoolean.TRUE)
    @Test
    @Throws(Exception::class)
    fun setAndGetActiveUserForRoleSetCurrentUserWithManageRoleHoldersPermission() {
        assumeFalse(
            "setActiveUser not supported for private profile",
            users().current().type().name() == PRIVATE_PROFILE_TYPE_NAME,
        )

        val targetActiveUser = users().current().userHandle()
        roleManager.setActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME, targetActiveUser, 0)
        assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
            .isEqualTo(targetActiveUser)
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(INTERACT_ACROSS_USERS_FULL, MANAGE_DEFAULT_APPLICATIONS)
    @EnsureDoesNotHavePermission(MANAGE_ROLE_HOLDERS)
    @EnsureHasWorkProfile(installInstrumentedApp = OptionalBoolean.TRUE)
    @Test
    @Throws(Exception::class)
    fun setAndGetActiveUserForRoleSetCurrentUserWithManageDefaultApplicationPermission() {
        assumeFalse(
            "setActiveUser not supported for private profile",
            users().current().type().name() == PRIVATE_PROFILE_TYPE_NAME,
        )

        val targetActiveUser = users().current().userHandle()
        roleManager.setActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME, targetActiveUser, 0)
        assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
            .isEqualTo(targetActiveUser)
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(INTERACT_ACROSS_USERS_FULL, MANAGE_ROLE_HOLDERS)
    @EnsureHasWorkProfile(installInstrumentedApp = OptionalBoolean.TRUE)
    @Test
    @Throws(Exception::class)
    fun setAndGetActiveUserForRoleSetCurrentUserEnsureRoleNotHeldByInactiveUser() {
        assumeFalse(
            "setActiveUser not supported for private profile",
            users().current().type().name() == PRIVATE_PROFILE_TYPE_NAME,
        )
        // initialUser needs to be not the targetUser
        val targetActiveUser = users().current().userHandle()
        val initialUser =
            if (Objects.equals(targetActiveUser, deviceState.initialUser())) {
                deviceState.workProfile().userHandle()
            } else {
                deviceState.initialUser().userHandle()
            }
        roleManager.setActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME, initialUser, 0)

        try {
            // Set test default role holder. Ensures fallbacks to a default holder
            roleManager.setDefaultHoldersForTest(
                PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME,
                listOf(APP_PACKAGE_NAME),
            )

            roleManager.setActiveUserForRole(
                PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME,
                targetActiveUser,
                0,
            )
            assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
                .isEqualTo(targetActiveUser)
            // We can assume targetActiveUser is role holder since fallback is enabled
            eventually { assertExpectedProfileHasRoleUsingGetRoleHoldersAsUser(targetActiveUser) }
        } finally {
            // Clear test default role holder
            roleManager.setDefaultHoldersForTest(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME, null)
        }
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(INTERACT_ACROSS_USERS_FULL, MANAGE_ROLE_HOLDERS)
    @EnsureHasWorkProfile(installInstrumentedApp = OptionalBoolean.TRUE)
    @Test
    @Throws(Exception::class)
    fun setAndGetActiveUserForRoleSetWorkProfile() {
        try {
            // Set test default role holder. Ensures fallbacks to a default holder
            roleManager.setDefaultHoldersForTest(
                PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME,
                listOf(APP_PACKAGE_NAME),
            )

            val targetActiveUser = deviceState.workProfile().userHandle()
            roleManager.setActiveUserForRole(
                PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME,
                targetActiveUser,
                0,
            )

            assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
                .isEqualTo(targetActiveUser)
            // We can assume targetActiveUser is role holder since fallback is enabled
            eventually { assertExpectedProfileHasRoleUsingGetRoleHoldersAsUser(targetActiveUser) }
        } finally {
            // Clear test default role holder
            roleManager.setDefaultHoldersForTest(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME, null)
        }
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(MANAGE_ROLE_HOLDERS)
    @EnsureDoesNotHavePermission(INTERACT_ACROSS_USERS_FULL)
    @EnsureHasWorkProfile
    @RequireRunOnPrimaryUser
    @Test
    @Throws(Exception::class)
    fun cannotAddRoleHolderAsUserForProfileExclusiveRoleWithoutInteractAcrossUserPermission() {
        // Set other user as active
        val initialUser = deviceState.workProfile().userHandle()
        // setActiveUserForRole and getActiveUserForRole is used to ensure initial active users
        // state and requires INTERACT_ACROSS_USERS_FULL
        permissions().withPermission(INTERACT_ACROSS_USERS_FULL).use { _ ->
            roleManager.setActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME, initialUser, 0)
            assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
                .isEqualTo(initialUser)
        }

        val targetActiveUser = users().current().userHandle()
        val future = CallbackFuture()
        assertThrows(SecurityException::class.java) {
            roleManager.addRoleHolderAsUser(
                PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME,
                APP_PACKAGE_NAME,
                0,
                targetActiveUser,
                context.mainExecutor,
                future,
            )
        }
        assertThat(
                roleManager.getRoleHoldersAsUser(
                    PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME,
                    targetActiveUser,
                )
            )
            .isEmpty()

        // getActiveUserForRole is used to ensure addRoleHolderAsUser didn't set active user, and
        // requires INTERACT_ACROSS_USERS_FULL
        permissions().withPermission(INTERACT_ACROSS_USERS_FULL).use { _ ->
            assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
                .isEqualTo(initialUser)
        }
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(INTERACT_ACROSS_USERS_FULL, MANAGE_ROLE_HOLDERS)
    @EnsureHasWorkProfile
    @Test
    @Throws(java.lang.Exception::class)
    fun addRoleHolderAsUserSetsCurrentUserAsActive() {
        // Set other user as active
        val initialUser = deviceState.workProfile().userHandle()
        roleManager.setActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME, initialUser, 0)
        assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
            .isEqualTo(initialUser)

        val targetActiveUser = users().current().userHandle()
        val future = CallbackFuture()
        roleManager.addRoleHolderAsUser(
            PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME,
            APP_PACKAGE_NAME,
            0,
            targetActiveUser,
            context.mainExecutor,
            future,
        )
        assertThat(future.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
            .isEqualTo(targetActiveUser)
        assertExpectedProfileHasRoleUsingGetRoleHoldersAsUser(targetActiveUser)
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(INTERACT_ACROSS_USERS_FULL, MANAGE_ROLE_HOLDERS)
    @EnsureHasWorkProfile
    @RequireRunOnWorkProfile
    @Test
    @Throws(java.lang.Exception::class)
    fun addRoleHolderAsUserSetsWorkProfileAsActive() {
        // Set other user as active
        val initialUser = users().main()!!.userHandle()
        roleManager.setActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME, initialUser, 0)
        assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
            .isEqualTo(initialUser)

        val targetActiveUser = deviceState.workProfile().userHandle()

        assertThat(targetActiveUser).isNotEqualTo(initialUser)
        val future = CallbackFuture()
        roleManager.addRoleHolderAsUser(
            PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME,
            APP_PACKAGE_NAME,
            0,
            targetActiveUser,
            context.mainExecutor,
            future,
        )
        assertThat(future.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
            .isEqualTo(targetActiveUser)
        assertExpectedProfileHasRoleUsingGetRoleHoldersAsUser(targetActiveUser)
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(MANAGE_DEFAULT_APPLICATIONS)
    @EnsureDoesNotHavePermission(INTERACT_ACROSS_USERS_FULL)
    @EnsureHasWorkProfile
    @RequireRunOnPrimaryUser
    @Test
    @Throws(Exception::class)
    fun cannotSetDefaultApplicationForProfileExclusiveRoleWithoutInteractAcrossUserPermission() {
        // Set other user as active
        val initialUser = deviceState.workProfile().userHandle()
        // setActiveUserForRole and getActiveUserForRole is used to ensure initial active users
        // state and requires INTERACT_ACROSS_USERS_FULL
        permissions().withPermission(INTERACT_ACROSS_USERS_FULL).use { _ ->
            roleManager.setActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME, initialUser, 0)
            assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
                .isEqualTo(initialUser)
        }

        val future = CallbackFuture()
        assertThrows(SecurityException::class.java) {
            roleManager.setDefaultApplication(
                PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME,
                APP_PACKAGE_NAME,
                0,
                context.mainExecutor,
                future,
            )
        }
        assertThat(roleManager.getDefaultApplication(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME)).isNull()

        // getActiveUserForRole is used to ensure setDefaultApplication didn't set active user,
        // and requires INTERACT_ACROSS_USERS_FULL
        permissions().withPermission(INTERACT_ACROSS_USERS_FULL).use { _ ->
            assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
                .isEqualTo(initialUser)
        }
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(INTERACT_ACROSS_USERS_FULL, MANAGE_DEFAULT_APPLICATIONS)
    @EnsureHasWorkProfile
    @Test
    @Throws(java.lang.Exception::class)
    fun setDefaultApplicationSetsCurrentUserAsActive() {
        // Set other user as active
        val initialUser = deviceState.workProfile().userHandle()
        roleManager.setActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME, initialUser, 0)
        assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
            .isEqualTo(initialUser)

        val targetActiveUser = users().current().userHandle()
        val future = CallbackFuture()
        roleManager.setDefaultApplication(
            PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME,
            APP_PACKAGE_NAME,
            0,
            context.mainExecutor,
            future,
        )
        assertThat(future.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
            .isEqualTo(targetActiveUser)
        assertExpectedProfileHasRoleUsingGetDefaultApplication(targetActiveUser)
    }

    @RequireFlagsEnabled(com.android.permission.flags.Flags.FLAG_CROSS_USER_ROLE_ENABLED)
    @EnsureHasPermission(INTERACT_ACROSS_USERS_FULL, MANAGE_DEFAULT_APPLICATIONS)
    @EnsureHasWorkProfile
    @RequireRunOnWorkProfile
    @Test
    @Throws(java.lang.Exception::class)
    fun setDefaultApplicationSetsWorkProfileAsActive() {
        // Set other user as active
        val initialUser = users().main()!!.userHandle()
        roleManager.setActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME, initialUser, 0)
        assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
            .isEqualTo(initialUser)

        val targetActiveUser = deviceState.workProfile().userHandle()
        assertThat(targetActiveUser).isNotEqualTo(initialUser)
        val future = CallbackFuture()
        roleManager.setDefaultApplication(
            PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME,
            APP_PACKAGE_NAME,
            0,
            context.mainExecutor,
            future,
        )
        assertThat(future.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(roleManager.getActiveUserForRole(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME))
            .isEqualTo(targetActiveUser)
        assertExpectedProfileHasRoleUsingGetDefaultApplication(targetActiveUser)
    }

    @Throws(java.lang.Exception::class)
    private fun installAppForAllUsers() {
        SystemUtil.runShellCommandOrThrow("pm install -r --user all $APP_APK_PATH")
    }

    private fun uninstallAppForAllUsers() {
        SystemUtil.runShellCommand("pm uninstall $APP_PACKAGE_NAME")
    }

    private fun assertExpectedProfileHasRoleUsingGetRoleHoldersAsUser(
        expectedActiveUser: UserHandle
    ) {
        for (userReference in users().profileGroup()) {
            val user = userReference.userHandle()
            if (Objects.equals(user, expectedActiveUser)) {
                val roleHolders =
                    roleManager.getRoleHoldersAsUser(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME, user)
                assertWithMessage(
                        "Expected user ${user.identifier} to have a role holder for " +
                            " $PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME"
                    )
                    .that(roleHolders)
                    .isNotEmpty()
                assertWithMessage(
                        "Expected user ${user.identifier} to have $APP_PACKAGE_NAME as role " +
                            "holder for $PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME"
                    )
                    .that(roleHolders.first())
                    .isEqualTo(APP_PACKAGE_NAME)
            } else {
                // Verify the non-active user does not hold the role
                assertWithMessage(
                        "Expected user ${user.identifier} to not have a role holder for" +
                            " $PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME"
                    )
                    .that(
                        roleManager.getRoleHoldersAsUser(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME, user)
                    )
                    .isEmpty()
            }
        }
    }

    private fun assertExpectedProfileHasRoleUsingGetDefaultApplication(
        expectedActiveUser: UserHandle
    ) {
        for (userReference in users().profileGroup()) {
            val user = userReference.userHandle()
            val userRoleManager = getRoleManagerForUser(user)
            if (Objects.equals(user, expectedActiveUser)) {
                assertWithMessage("Expected default application for user ${user.identifier}")
                    .that(
                        userRoleManager.getDefaultApplication(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME)
                    )
                    .isEqualTo(APP_PACKAGE_NAME)
            } else {
                // Verify the non-active user does not hold the role
                assertWithMessage("Expected no default application for user ${user.identifier}")
                    .that(
                        userRoleManager.getDefaultApplication(PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME)
                    )
                    .isNull()
            }
        }
    }

    private fun getRoleManagerForUser(user: UserHandle): RoleManager {
        val userContext = context.createContextAsUser(user, 0)
        return userContext.getSystemService(RoleManager::class.java)
    }

    class CallbackFuture : CompletableFuture<Boolean?>(), Consumer<Boolean?> {
        override fun accept(successful: Boolean?) {
            complete(successful)
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS: Long = (15 * 1000).toLong()
        private const val PROFILE_GROUP_EXCLUSIVITY_ROLE_NAME =
            RoleManager.ROLE_RESERVED_FOR_TESTING_PROFILE_GROUP_EXCLUSIVITY
        private const val PRIVATE_PROFILE_TYPE_NAME = "android.os.usertype.profile.PRIVATE"
        private const val APP_APK_PATH: String =
            "/data/local/tmp/cts-role/CtsRoleMultiUserTestApp.apk"
        private const val APP_PACKAGE_NAME: String = "android.app.rolemultiuser.cts.app"
        private val context: Context = context().instrumentedContext()
        private val roleManager: RoleManager = context.getSystemService(RoleManager::class.java)

        @JvmField @ClassRule @Rule val deviceState = DeviceState()
    }
}
