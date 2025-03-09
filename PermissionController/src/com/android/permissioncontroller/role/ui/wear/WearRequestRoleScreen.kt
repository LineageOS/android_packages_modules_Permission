/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.permissioncontroller.role.ui.wear

import android.content.pm.ApplicationInfo
import android.util.Pair
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.permissioncontroller.R
import com.android.permissioncontroller.permission.ui.wear.elements.ScrollableScreen
import com.android.permissioncontroller.permission.ui.wear.elements.ToggleChipToggleControl
import com.android.permissioncontroller.permission.ui.wear.elements.material3.WearPermissionButton
import com.android.permissioncontroller.permission.ui.wear.elements.material3.WearPermissionButtonStyle
import com.android.permissioncontroller.permission.ui.wear.elements.material3.WearPermissionIconBuilder
import com.android.permissioncontroller.permission.ui.wear.elements.material3.WearPermissionListFooter
import com.android.permissioncontroller.permission.ui.wear.elements.material3.WearPermissionToggleControl
import com.android.permissioncontroller.permission.ui.wear.elements.material3.WearPermissionToggleControlStyle
import com.android.permissioncontroller.permission.ui.wear.theme.ResourceHelper
import com.android.permissioncontroller.permission.ui.wear.theme.WearPermissionMaterialUIVersion
import com.android.permissioncontroller.permission.ui.wear.theme.WearPermissionMaterialUIVersion.MATERIAL2_5
import com.android.permissioncontroller.permission.ui.wear.theme.WearPermissionMaterialUIVersion.MATERIAL3
import com.android.permissioncontroller.role.ui.ManageRoleHolderStateLiveData

@Composable
fun WearRequestRoleScreen(
    helper: WearRequestRoleHelper,
    onSetAsDefault: (Boolean, String?) -> Unit,
    onCanceled: () -> Unit,
) {
    val roleLiveData = helper.viewModel.roleLiveData.observeAsState(emptyList())
    val manageRoleHolderState =
        helper.viewModel.manageRoleHolderStateLiveData.observeAsState(
            ManageRoleHolderStateLiveData.STATE_WORKING
        )
    val dontAskAgain = helper.wearViewModel.dontAskAgain.observeAsState(false)
    val selectedPackageName = helper.wearViewModel.selectedPackageName.observeAsState(null)
    var isLoading by remember { mutableStateOf(true) }

    if (isLoading && roleLiveData.value.isNotEmpty()) {
        helper.initializeHolderPackageName(roleLiveData.value)
        helper.initializeSelectedPackageName()
    }

    val onCheckedChanged: (Boolean, String?, Boolean) -> Unit = { checked, packageName, isHolder ->
        if (checked) {
            helper.wearViewModel.selectedPackageName.value = packageName
            helper.wearViewModel.isHolderChecked = isHolder
        }
    }

    val onDontAskAgainCheckedChanged: (Boolean) -> Unit = { checked ->
        helper.wearViewModel.dontAskAgain.value = checked
        if (checked) {
            helper.initializeSelectedPackageName()
        }
    }
    val materialUIVersion =
        if (ResourceHelper.material3Enabled) {
            MATERIAL3
        } else {
            MATERIAL2_5
        }
    WearRequestRoleContent(
        materialUIVersion,
        isLoading,
        helper,
        roleLiveData.value,
        manageRoleHolderState.value == ManageRoleHolderStateLiveData.STATE_IDLE,
        dontAskAgain.value,
        selectedPackageName.value,
        onCheckedChanged,
        onDontAskAgainCheckedChanged,
        onSetAsDefault,
        onCanceled,
    )

    if (isLoading && roleLiveData.value.isNotEmpty()) {
        isLoading = false
    }
}

@Composable
internal fun WearRequestRoleContent(
    materialUIVersion: WearPermissionMaterialUIVersion,
    isLoading: Boolean,
    helper: WearRequestRoleHelper,
    qualifyingApplications: List<Pair<ApplicationInfo, Boolean>>,
    enabled: Boolean,
    dontAskAgain: Boolean,
    selectedPackageName: String?,
    onCheckedChanged: (Boolean, String?, Boolean) -> Unit,
    onDontAskAgainCheckedChanged: (Boolean) -> Unit,
    onSetAsDefault: (Boolean, String?) -> Unit,
    onCanceled: () -> Unit,
) {
    ScrollableScreen(
        materialUIVersion = materialUIVersion,
        image = helper.getIcon(),
        title = helper.getTitle(),
        showTimeText = false,
        isLoading = isLoading,
    ) {
        helper.getNonePreference(qualifyingApplications, selectedPackageName)?.let { pref ->
            item {
                WearPermissionToggleControl(
                    materialUIVersion = materialUIVersion,
                    label = pref.label,
                    iconBuilder = pref.icon?.let { WearPermissionIconBuilder.builder(it) },
                    enabled = enabled && pref.enabled,
                    checked = pref.checked,
                    onCheckedChanged = { checked ->
                        onCheckedChanged(checked, pref.packageName, pref.isHolder)
                    },
                    toggleControl = ToggleChipToggleControl.Radio,
                    labelMaxLines = Integer.MAX_VALUE,
                )
            }
            pref.subTitle?.let { subTitle ->
                item {
                    WearPermissionListFooter(
                        materialUIVersion = materialUIVersion,
                        label = subTitle,
                    )
                }
            }
        }

        for (pref in helper.getPreferences(qualifyingApplications, selectedPackageName)) {
            item {
                WearPermissionToggleControl(
                    materialUIVersion = materialUIVersion,
                    label = pref.label,
                    iconBuilder = pref.icon?.let { WearPermissionIconBuilder.builder(it) },
                    enabled = enabled && pref.enabled,
                    checked = pref.checked,
                    onCheckedChanged = { checked ->
                        onCheckedChanged(checked, pref.packageName, pref.isHolder)
                    },
                    toggleControl = ToggleChipToggleControl.Radio,
                )
            }
            pref.subTitle?.let { subTitle ->
                item {
                    WearPermissionListFooter(
                        materialUIVersion = materialUIVersion,
                        label = subTitle,
                    )
                }
            }
        }

        if (helper.showDontAskButton()) {
            item {
                WearPermissionToggleControl(
                    materialUIVersion = materialUIVersion,
                    checked = dontAskAgain,
                    enabled = enabled,
                    onCheckedChanged = { checked -> run { onDontAskAgainCheckedChanged(checked) } },
                    label = stringResource(R.string.request_role_dont_ask_again),
                    toggleControl = ToggleChipToggleControl.Checkbox,
                    style = WearPermissionToggleControlStyle.Transparent,
                    modifier =
                        Modifier.testTag("com.android.permissioncontroller:id/dont_ask_again"),
                )
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }

        item {
            WearPermissionButton(
                materialUIVersion = materialUIVersion,
                label = stringResource(R.string.request_role_set_as_default),
                style = WearPermissionButtonStyle.Primary,
                enabled = helper.shouldSetAsDefaultEnabled(enabled),
                onClick = { onSetAsDefault(dontAskAgain, selectedPackageName) },
                modifier = Modifier.testTag("android:id/button1"),
            )
        }
        item {
            WearPermissionButton(
                materialUIVersion = materialUIVersion,
                label = stringResource(R.string.cancel),
                enabled = enabled,
                onClick = { onCanceled() },
                modifier = Modifier.testTag("android:id/button2"),
            )
        }
    }
}
