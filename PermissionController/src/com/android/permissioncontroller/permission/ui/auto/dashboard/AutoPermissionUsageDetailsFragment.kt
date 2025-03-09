/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.permissioncontroller.permission.ui.auto.dashboard

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.android.car.ui.preference.CarUiPreference
import com.android.permissioncontroller.Constants
import com.android.permissioncontroller.DumpableLog
import com.android.permissioncontroller.PermissionControllerApplication
import com.android.permissioncontroller.PermissionControllerStatsLog
import com.android.permissioncontroller.PermissionControllerStatsLog.PERMISSION_USAGE_FRAGMENT_INTERACTION
import com.android.permissioncontroller.PermissionControllerStatsLog.PERMISSION_USAGE_FRAGMENT_INTERACTION__ACTION__SHOW_SYSTEM_CLICKED
import com.android.permissioncontroller.R
import com.android.permissioncontroller.auto.AutoSettingsFrameFragment
import com.android.permissioncontroller.permission.ui.ManagePermissionsActivity
import com.android.permissioncontroller.permission.ui.auto.AutoDividerPreference
import com.android.permissioncontroller.permission.ui.model.v31.BasePermissionUsageDetailsViewModel
import com.android.permissioncontroller.permission.ui.model.v31.PermissionUsageDetailsViewModel
import com.android.permissioncontroller.permission.ui.model.v31.PermissionUsageDetailsViewModel.AppPermissionAccessUiInfo
import com.android.permissioncontroller.permission.utils.KotlinUtils.getPermGroupLabel
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicReference

@RequiresApi(Build.VERSION_CODES.S)
class AutoPermissionUsageDetailsFragment : AutoSettingsFrameFragment() {
    companion object {
        private const val LOG_TAG = "AutoPermissionUsageDetailsFragment"
        private const val KEY_SESSION_ID = "_session_id"
        private const val FILTER_24_HOURS = 2
        private val MIDNIGHT_TODAY =
            ZonedDateTime.now(ZoneId.systemDefault()).truncatedTo(ChronoUnit.DAYS).toEpochSecond() *
                1000L
        private val MIDNIGHT_YESTERDAY =
            ZonedDateTime.now(ZoneId.systemDefault())
                .minusDays(1)
                .truncatedTo(ChronoUnit.DAYS)
                .toEpochSecond() * 1000L

        /** Creates a new instance of [AutoPermissionUsageDetailsFragment]. */
        fun newInstance(
            groupName: String?,
            showSystem: Boolean,
            sessionId: Long,
        ): AutoPermissionUsageDetailsFragment {
            return AutoPermissionUsageDetailsFragment().apply {
                arguments =
                    Bundle().apply {
                        putString(Intent.EXTRA_PERMISSION_GROUP_NAME, groupName)
                        putLong(Constants.EXTRA_SESSION_ID, sessionId)
                        putBoolean(ManagePermissionsActivity.EXTRA_SHOW_SYSTEM, showSystem)
                    }
            }
        }
    }

    private val SESSION_ID_KEY = (AutoPermissionUsageFragment::class.java.name + KEY_SESSION_ID)

    private lateinit var usageViewModel: BasePermissionUsageDetailsViewModel
    private lateinit var filterGroup: String

    private var showSystem = false
    private var hasSystemApps = false

    /** Unique Id of a request */
    private var sessionId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments == null) {
            DumpableLog.e(LOG_TAG, "Missing arguments")
            activity?.finish()
            return
        }
        if (
            !requireArguments().containsKey(Intent.EXTRA_PERMISSION_GROUP_NAME) or
                (requireArguments().getString(Intent.EXTRA_PERMISSION_GROUP_NAME) == null)
        ) {
            DumpableLog.e(LOG_TAG, "Missing argument ${Intent.EXTRA_PERMISSION_GROUP_NAME}")
            activity?.finish()
            return
        }
        filterGroup = requireArguments().getString(Intent.EXTRA_PERMISSION_GROUP_NAME)!!
        showSystem =
            requireArguments().getBoolean(ManagePermissionsActivity.EXTRA_SHOW_SYSTEM, false)
        sessionId =
            savedInstanceState?.getLong(SESSION_ID_KEY)
                ?: (arguments?.getLong(Constants.EXTRA_SESSION_ID, Constants.INVALID_SESSION_ID)
                    ?: Constants.INVALID_SESSION_ID)
        headerLabel =
            resources.getString(
                R.string.permission_group_usage_title,
                getPermGroupLabel(requireContext(), filterGroup),
            )
        val factory =
            PermissionUsageDetailsViewModel.PermissionUsageDetailsViewModelFactory(
                PermissionControllerApplication.get(),
                this,
                filterGroup,
            )
        usageViewModel =
            ViewModelProvider(this, factory)[BasePermissionUsageDetailsViewModel::class.java]
        usageViewModel.getPermissionUsagesDetailsInfoUiLiveData().observe(this, this::updateUI)
    }

    override fun onCreatePreferences(bundlle: Bundle?, s: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
    }

    private fun setupHeaderPreferences() {
        addTimelineDescriptionPreference()
        preferenceScreen.addPreference(AutoDividerPreference(context))
        addManagePermissionPreference()
        preferenceScreen.addPreference(AutoDividerPreference(context))
    }

    private fun updateSystemToggle() {
        if (!showSystem) {
            PermissionControllerStatsLog.write(
                PERMISSION_USAGE_FRAGMENT_INTERACTION,
                sessionId,
                PERMISSION_USAGE_FRAGMENT_INTERACTION__ACTION__SHOW_SYSTEM_CLICKED,
            )
        }
        showSystem = !showSystem
        updateAction()
    }

    private fun updateAction() {
        if (!hasSystemApps) {
            setAction(null, null)
            return
        }
        val label =
            if (showSystem) {
                getString(R.string.menu_hide_system)
            } else {
                getString(R.string.menu_show_system)
            }
        setAction(label) {
            usageViewModel.updateShowSystemAppsToggle(!showSystem)
            updateSystemToggle()
        }
    }

    private fun updateUI(uiInfo: PermissionUsageDetailsViewModel.PermissionUsageDetailsUiState) {
        if (
            activity == null ||
                uiInfo is PermissionUsageDetailsViewModel.PermissionUsageDetailsUiState.Loading
        ) {
            return
        }
        preferenceScreen.removeAll()
        setupHeaderPreferences()
        val uiData = uiInfo as PermissionUsageDetailsViewModel.PermissionUsageDetailsUiState.Success
        if (hasSystemApps != uiData.containsSystemAppUsage) {
            hasSystemApps = uiData.containsSystemAppUsage
            updateAction()
        }
        val category = AtomicReference(PreferenceCategory(requireContext()))
        preferenceScreen.addPreference(category.get())

        renderHistoryPreferences(uiData.appPermissionAccessUiInfoList, category, preferenceScreen)

        setLoading(false)
    }

    fun createPermissionHistoryPreference(
        historyPreferenceData: AppPermissionAccessUiInfo
    ): Preference {
        return AutoPermissionHistoryPreference(requireContext(), historyPreferenceData)
    }

    private fun addTimelineDescriptionPreference() {
        val preference =
            CarUiPreference(context).apply {
                summary =
                    getString(
                        R.string.permission_group_usage_subtitle_24h,
                        getPermGroupLabel(requireContext(), filterGroup),
                    )
                isSelectable = false
            }
        preferenceScreen.addPreference(preference)
    }

    private fun addManagePermissionPreference() {
        val preference =
            CarUiPreference(context).apply {
                title = getString(R.string.manage_permission)
                summary =
                    getString(
                        R.string.manage_permission_summary,
                        getPermGroupLabel(requireContext(), filterGroup),
                    )
                onPreferenceClickListener =
                    Preference.OnPreferenceClickListener {
                        val intent =
                            Intent(Intent.ACTION_MANAGE_PERMISSION_APPS).apply {
                                putExtra(Intent.EXTRA_PERMISSION_NAME, filterGroup)
                            }
                        startActivity(intent)
                        true
                    }
            }
        preferenceScreen.addPreference(preference)
    }

    /** Render the provided [historyPreferenceDataList] into the [preferenceScreen] UI. */
    private fun renderHistoryPreferences(
        historyPreferenceDataList: List<AppPermissionAccessUiInfo>,
        category: AtomicReference<PreferenceCategory>,
        preferenceScreen: PreferenceScreen,
    ) {
        var previousDateMs = 0L
        historyPreferenceDataList.forEach {
            val usageTimestamp = it.accessEndTime
            val currentDateMs =
                ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(usageTimestamp),
                        Clock.system(ZoneId.systemDefault()).zone,
                    )
                    .truncatedTo(ChronoUnit.DAYS)
                    .toEpochSecond() * 1000L
            if (currentDateMs != previousDateMs) {
                if (previousDateMs != 0L) {
                    category.set(PreferenceCategory(requireContext()))
                    preferenceScreen.addPreference(category.get())
                }
                if (usageTimestamp > MIDNIGHT_TODAY) {
                    category.get().setTitle(R.string.permission_history_category_today)
                } else if (usageTimestamp > MIDNIGHT_YESTERDAY) {
                    category.get().setTitle(R.string.permission_history_category_yesterday)
                } else {
                    category.get().setTitle(DateFormat.getDateFormat(context).format(currentDateMs))
                }
                previousDateMs = currentDateMs
            }
            category.get().addPreference(createPermissionHistoryPreference(it))
        }
    }
}
