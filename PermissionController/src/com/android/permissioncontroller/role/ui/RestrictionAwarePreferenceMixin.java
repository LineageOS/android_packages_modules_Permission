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

package com.android.permissioncontroller.role.ui;

import android.app.admin.DevicePolicyManager;
import android.app.ecm.EnhancedConfirmationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.modules.utils.build.SdkLevel;
import com.android.permissioncontroller.DeviceUtils;
import com.android.permissioncontroller.role.utils.UserUtils;

/**
 * Mixin for implementing {@link RestrictionAwarePreference}.
 */
public class RestrictionAwarePreferenceMixin {

    private static final String LOG_TAG = RestrictionAwarePreferenceMixin.class.getSimpleName();

    @NonNull
    private final Preference mPreference;

    @Nullable
    private String mUserRestriction;

    @Nullable
    private String mEnhancedConfirmationRestrictedPackageName;
    @Nullable
    private String mEnhancedConfirmationRestrictedSettingIdentifier;

    @Nullable
    private UserHandle mUser;

    public RestrictionAwarePreferenceMixin(@NonNull Preference preference) {
        mPreference = preference;
    }

    /**
     * Implementation for {@link RestrictionAwarePreference#setUserRestriction}.
     */
    public void setUserRestriction(@Nullable String userRestriction, @NonNull UserHandle user) {
        mUserRestriction = userRestriction;
        mUser = user;
        updateEnabled();
    }

    /**
     * Implementation for {@link RestrictionAwarePreference#setEnhancedConfirmationRestriction}.
     */
    public void setEnhancedConfirmationRestriction(@Nullable String packageName,
            @Nullable String settingIdentifier, @NonNull UserHandle user) {
        if (!isEnhancedConfirmationRestrictionEnabled()) {
            return;
        }
        mEnhancedConfirmationRestrictedPackageName = packageName;
        mEnhancedConfirmationRestrictedSettingIdentifier = settingIdentifier;
        mUser = user;
        updateEnabled();
    }

    private boolean isEnhancedConfirmationRestrictionEnabled() {
        // Enhanced confirmation restriction is currently applied only to handheld.
        Context context = mPreference.getContext();
        return !(DeviceUtils.isAuto(context) || DeviceUtils.isTelevision(context)
                || DeviceUtils.isWear(context));
    }

    private void updateEnabled() {
        mPreference.setEnabled(mUserRestriction == null
                && (mEnhancedConfirmationRestrictedPackageName == null
                        || mEnhancedConfirmationRestrictedSettingIdentifier == null));
    }

    /**
     * Call after {@link Preference#onBindViewHolder} to apply blocking effects.
     */
    public void onAfterBindViewHolder(@NonNull PreferenceViewHolder holder) {
        View.OnClickListener onClickListener = null;
        if (SdkLevel.isAtLeastV()
                && mEnhancedConfirmationRestrictedPackageName != null
                && mEnhancedConfirmationRestrictedSettingIdentifier != null
                && mUser != null) {
            Context context = UserUtils.getUserContext(holder.itemView.getContext(), mUser);
            onClickListener = (view) -> {
                EnhancedConfirmationManager enhancedConfirmationManager =
                        context.getSystemService(EnhancedConfirmationManager.class);
                try {
                    context.startActivity(
                            enhancedConfirmationManager.createRestrictedSettingDialogIntent(
                                    mEnhancedConfirmationRestrictedPackageName,
                                    mEnhancedConfirmationRestrictedSettingIdentifier));
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(LOG_TAG, "Package name is not found.", e);
                }
            };
        } else if (mUserRestriction != null) {
            Intent userRestrictionIntent = new Intent(Settings.ACTION_SHOW_ADMIN_SUPPORT_DETAILS)
                    .putExtra(DevicePolicyManager.EXTRA_RESTRICTION, mUserRestriction);
            onClickListener = (view) -> {
                holder.itemView.getContext().startActivity(userRestrictionIntent);
            };
        }

        // We set the item view to enabled to make the preference row clickable.
        // Normal disabled preferences have the whole view hierarchy disabled, so by making only
        // the top-level itemView enabled, we don't change the fact that the whole preference
        // still "looks" disabled (see Preference.onBindViewHolder).
        // Preference.onBindViewHolder sets the onClickListener as well on each preference, so
        // we don't need to unset the listener here (we wouldn't know the correct one anyway).
        // This approach is used already by com.android.settingslib.RestrictedPreferenceHelper.
        if (onClickListener != null) {
            holder.itemView.setEnabled(true);
            holder.itemView.setOnClickListener(onClickListener);
        }
    }
}
