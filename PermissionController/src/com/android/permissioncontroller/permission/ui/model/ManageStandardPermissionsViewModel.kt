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

package com.android.permissioncontroller.permission.ui.model

import android.Manifest
import android.app.Application
import android.content.Intent
import android.health.connect.HealthPermissions.HEALTH_PERMISSION_GROUP
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.navigation.fragment.findNavController
import com.android.permission.flags.Flags
import com.android.permissioncontroller.R
import com.android.permissioncontroller.permission.data.PermGroupsPackagesLiveData
import com.android.permissioncontroller.permission.data.PermGroupsPackagesUiInfoLiveData
import com.android.permissioncontroller.permission.data.SmartUpdateMediatorLiveData
import com.android.permissioncontroller.permission.data.StandardPermGroupNamesLiveData
import com.android.permissioncontroller.permission.data.unusedAutoRevokePackagesLiveData
import com.android.permissioncontroller.permission.utils.Utils
import com.android.permissioncontroller.permission.utils.navigateSafe

/**
 * A ViewModel for the ManageStandardPermissionsFragment. Provides a LiveData which watches over all
 * platform permission groups, and sends async updates when these groups have changes. It also
 * provides a liveData which watches the custom permission groups of the system, and provides a list
 * of group names.
 *
 * @param app The current application of the fragment
 */
class ManageStandardPermissionsViewModel(private val app: Application) : AndroidViewModel(app) {

    val uiDataLiveData = PermGroupsPackagesUiInfoLiveData(app, StandardPermGroupNamesLiveData)
    val usedStandardPermGroupsUiInfo =
        PermGroupsPackagesUiInfoLiveData(
            app,
            if (Flags.declutteredPermissionManagerEnabled()) UsedStandardPermGroupNamesLiveData(app)
            else MutableLiveData<List<String>>(),
        )
    val numCustomPermGroups = NumCustomPermGroupsWithPackagesLiveData()
    val numUnusedStandardPermGroups =
        MediatorLiveData<Int>().apply {
            if (Flags.declutteredPermissionManagerEnabled()) {
                addSource(UnusedStandardPermGroupNamesLiveData(app)) { groupNames ->
                    value = groupNames.size
                }
            }
        }
    val numAutoRevoked = unusedAutoRevokePackagesLiveData.map { it?.size ?: 0 }

    /**
     * Navigate to the Custom Permissions screen
     *
     * @param fragment The fragment we are navigating from
     * @param args The args to pass to the new fragment
     */
    fun showCustomPermissions(fragment: Fragment, args: Bundle) {
        fragment.findNavController().navigateSafe(R.id.standard_to_custom, args)
    }

    /**
     * Navigate to a Permission Apps fragment
     *
     * @param fragment The fragment we are navigating from
     * @param args The args to pass to the new fragment
     */
    fun showPermissionApps(fragment: Fragment, args: Bundle) {
        val groupName = args.getString(Intent.EXTRA_PERMISSION_GROUP_NAME)
        if (groupName == Manifest.permission_group.NOTIFICATIONS) {
            Utils.navigateToNotificationSettings(fragment.context!!)
            return
        }
        if (Utils.isHealthPermissionUiEnabled() && groupName == HEALTH_PERMISSION_GROUP) {
            Utils.navigateToHealthConnectSettings(fragment.context!!)
            return
        }
        fragment.findNavController().navigateSafe(R.id.manage_to_perm_apps, args)
    }

    fun showAutoRevoke(fragment: Fragment, args: Bundle) {
        fragment.findNavController().navigateSafe(R.id.manage_to_auto_revoke, args)
    }
}

/**
 * A LiveData which tracks the number of custom permission groups that are used by at least one
 * package
 */
class NumCustomPermGroupsWithPackagesLiveData() : SmartUpdateMediatorLiveData<Int>() {

    private val customPermGroupPackages = PermGroupsPackagesLiveData.get(customGroups = true)

    init {
        addSource(customPermGroupPackages) { update() }
    }

    override fun onUpdate() {
        value = customPermGroupPackages.value?.size ?: 0
    }
}

/**
 * A LiveData that tracks the names of the platform-defined permission groups, such that at least
 * one of the permissions in the group has been requested at runtime by at least one non-system
 * application.
 *
 * @param app The current application of the fragment
 */
class UsedStandardPermGroupNamesLiveData(private val app: Application) :
    SmartUpdateMediatorLiveData<List<String>>() {
    init {
        addSource(PermGroupsPackagesUiInfoLiveData(app, StandardPermGroupNamesLiveData)) {
            permGroups ->
            if (permGroups.values.any { it != null }) {
                value =
                    permGroups.filterValues { it != null && it.nonSystemTotal > 0 }.keys.toList()
            }
        }
    }

    override fun onUpdate() {
        /* No op override */
    }
}

/**
 * A LiveData that tracks the names of the platform-defined permission groups, such that none of the
 * the permissions in the group has been requested at runtime by any non-system application.
 *
 * @param app The current application of the fragment
 */
class UnusedStandardPermGroupNamesLiveData(private val app: Application) :
    SmartUpdateMediatorLiveData<List<String>>() {
    init {
        addSource(PermGroupsPackagesUiInfoLiveData(app, StandardPermGroupNamesLiveData)) {
            permGroups ->
            value = permGroups.filterValues { it != null && it.nonSystemTotal == 0 }.keys.toList()
        }
    }

    override fun onUpdate() {
        /* No op override */
    }
}
