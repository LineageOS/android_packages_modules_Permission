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

package com.android.ecm;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.UserIdInt;
import android.app.AppOpsManager;
import android.app.ecm.EnhancedConfirmationManager;
import android.app.ecm.IEnhancedConfirmationManager;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Binder;
import android.os.Build;
import android.os.UserHandle;
import android.permission.flags.Flags;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.android.internal.util.Preconditions;
import com.android.permission.util.UserUtils;
import com.android.server.SystemService;

import java.lang.annotation.Retention;


/**
 * Service for ECM (Enhanced Confirmation Mode).
 *
 * @see EnhancedConfirmationManager
 *
 * @hide
 */
@Keep
@FlaggedApi(Flags.FLAG_ENHANCED_CONFIRMATION_MODE_APIS_ENABLED)
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class EnhancedConfirmationService extends SystemService {
    private static final String LOG_TAG = EnhancedConfirmationService.class.getSimpleName();

    public EnhancedConfirmationService(@NonNull Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        publishBinderService(Context.ECM_ENHANCED_CONFIRMATION_SERVICE, new Stub());
    }

    private class Stub extends IEnhancedConfirmationManager.Stub {

        /** A map of ECM states to their corresponding app op states */
        @Retention(java.lang.annotation.RetentionPolicy.SOURCE)
        @IntDef(prefix = {"ECM_STATE_"}, value = {EcmState.ECM_STATE_NOT_GUARDED,
                EcmState.ECM_STATE_GUARDED, EcmState.ECM_STATE_GUARDED_AND_ACKNOWLEDGED,
                EcmState.ECM_STATE_IMPLICIT})
        private @interface EcmState {
            int ECM_STATE_NOT_GUARDED = AppOpsManager.MODE_ALLOWED;
            int ECM_STATE_GUARDED = AppOpsManager.MODE_ERRORED;
            int ECM_STATE_GUARDED_AND_ACKNOWLEDGED = AppOpsManager.MODE_IGNORED;
            int ECM_STATE_IMPLICIT = AppOpsManager.MODE_DEFAULT;
        }

        private static final ArraySet<String> PROTECTED_SETTINGS = new ArraySet<>();

        static {
            PROTECTED_SETTINGS.add(AppOpsManager.OPSTR_BIND_ACCESSIBILITY_SERVICE);
            // Default application roles.
            PROTECTED_SETTINGS.add(RoleManager.ROLE_ASSISTANT);
            PROTECTED_SETTINGS.add(RoleManager.ROLE_BROWSER);
            PROTECTED_SETTINGS.add(RoleManager.ROLE_CALL_REDIRECTION);
            PROTECTED_SETTINGS.add(RoleManager.ROLE_CALL_SCREENING);
            PROTECTED_SETTINGS.add(RoleManager.ROLE_DIALER);
            PROTECTED_SETTINGS.add(RoleManager.ROLE_HOME);
            PROTECTED_SETTINGS.add(RoleManager.ROLE_SMS);
            PROTECTED_SETTINGS.add(RoleManager.ROLE_WALLET);
            // TODO(b/310654015): Add other explicitly protected settings
        }

        private final @NonNull Context mContext;
        private final String mAttributionTag;
        private final AppOpsManager mAppOpsManager;
        private final PackageManager mPackageManager;

        Stub() {
            Context context = getContext();
            mContext = context;
            mAttributionTag = context.getAttributionTag();
            mAppOpsManager = context.getSystemService(AppOpsManager.class);
            mPackageManager = context.getPackageManager();
        }

        public boolean isRestricted(@NonNull String packageName, @NonNull String settingIdentifier,
                @UserIdInt int userId) {
            enforcePermissions("isRestricted", userId);
            if (!UserUtils.isUserExistent(userId, getContext())) {
                Log.e(LOG_TAG, "user " + userId + " does not exist");
                return false;
            }

            Preconditions.checkStringNotEmpty(packageName, "packageName cannot be null or empty");
            Preconditions.checkStringNotEmpty(settingIdentifier,
                    "settingIdentifier cannot be null or empty");

            try {
                return isSettingEcmProtected(settingIdentifier) && isPackageEcmGuarded(packageName,
                        userId);
            } catch (NameNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }

        public void clearRestriction(@NonNull String packageName, @UserIdInt int userId) {
            enforcePermissions("clearRestriction", userId);
            if (!UserUtils.isUserExistent(userId, getContext())) {
                return;
            }

            Preconditions.checkStringNotEmpty(packageName, "packageName cannot be null or empty");

            try {
                int state = getAppEcmState(packageName, userId);
                boolean isAllowed = state == EcmState.ECM_STATE_GUARDED_AND_ACKNOWLEDGED;
                if (!isAllowed) {
                    throw new IllegalStateException("Clear restriction attempted but not allowed");
                }
                setAppEcmState(packageName, EcmState.ECM_STATE_NOT_GUARDED, userId);
            } catch (NameNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }

        public boolean isClearRestrictionAllowed(@NonNull String packageName,
                @UserIdInt int userId) {
            enforcePermissions("isClearRestrictionAllowed", userId);
            if (!UserUtils.isUserExistent(userId, getContext())) {
                return false;
            }

            Preconditions.checkStringNotEmpty(packageName, "packageName cannot be null or empty");

            try {
                int state = getAppEcmState(packageName, userId);
                return state == EcmState.ECM_STATE_GUARDED_AND_ACKNOWLEDGED;
            } catch (NameNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }

        public void setClearRestrictionAllowed(@NonNull String packageName, @UserIdInt int userId) {
            enforcePermissions("setClearRestrictionAllowed", userId);
            if (!UserUtils.isUserExistent(userId, getContext())) {
                return;
            }

            Preconditions.checkStringNotEmpty(packageName, "packageName cannot be null or empty");

            try {
                if (isPackageEcmGuarded(packageName, userId)) {
                    setAppEcmState(packageName, EcmState.ECM_STATE_GUARDED_AND_ACKNOWLEDGED,
                            userId);
                }
            } catch (NameNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }

        private void enforcePermissions(@NonNull String methodName, @UserIdInt int userId) {
            UserUtils.enforceCrossUserPermission(userId, false, methodName, mContext);
            mContext.enforceCallingPermission(
                    android.Manifest.permission.MANAGE_ENHANCED_CONFIRMATION_STATES, methodName);
        }

        private boolean isPackageEcmGuarded(@NonNull String packageName, @UserIdInt int userId)
                throws NameNotFoundException {
            // If this is a trusted installer or a pre-installed app, it is not ECM guarded
            if (isAppTrustedInstaller(packageName, userId) || isPackagePreinstalled(packageName,
                    userId)) {
                return false;
            }

            // If the package already has an explicitly-set state, use that
            @EcmState int ecmState = getAppEcmState(packageName, userId);
            if (ecmState == EcmState.ECM_STATE_GUARDED
                    || ecmState == EcmState.ECM_STATE_GUARDED_AND_ACKNOWLEDGED) {
                return true;
            }
            if (ecmState == EcmState.ECM_STATE_NOT_GUARDED) {
                return false;
            }

            // Otherwise, lazily decide whether the app is considered guarded.
            InstallSourceInfo installSource;
            try {
                installSource = mPackageManager.getInstallSourceInfo(packageName);
            } catch (NameNotFoundException e) {
                Log.w(LOG_TAG, "Package not found: " + packageName);
                return false;
            }

            // These install sources are always considered dangerous.
            // PackageInstallers that are trusted can use these as a signal that the
            // packages they've installed aren't as trusted as themselves.
            int packageSource = installSource.getPackageSource();
            if (packageSource == PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE
                    || packageSource == PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE) {
                return true;
            }

            // ECM doesn't consider a transitive chain of trust for install sources.
            // If this package hasn't been explicitly handled by this point
            // then it is exempt from ECM if the immediate parent is a trusted installer
            boolean installedFromTrustedInstaller =
                    installSource.getInstallingPackageName() != null && isAppTrustedInstaller(
                            installSource.getInstallingPackageName(), userId);
            return !installedFromTrustedInstaller;
        }

        /**
         * A "trusted installer" is any app with the INSTALL_PACKAGES permission.
         */
        private boolean isAppTrustedInstaller(@NonNull String packageName, @UserIdInt int userId)
                throws NameNotFoundException {
            int uid = getPackageUid(packageName, userId);
            // TODO(b/310654834): Support allow-list for OEM installer exemptions
            return mContext.checkPermission(android.Manifest.permission.INSTALL_PACKAGES, 0, uid)
                    == PackageManager.PERMISSION_GRANTED;
        }

        private boolean isPackagePreinstalled(@NonNull String packageName, @UserIdInt int userId) {
            ApplicationInfo applicationInfo;
            try {
                applicationInfo = mPackageManager.getApplicationInfoAsUser(packageName, 0,
                        UserHandle.of(userId));
            } catch (NameNotFoundException e) {
                Log.w(LOG_TAG, "Package not found: " + packageName);
                return false;
            }
            return (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        }

        private void setAppEcmState(@NonNull String packageName, @EcmState int ecmState,
                @UserIdInt int userId) throws NameNotFoundException {
            int packageUid = getPackageUid(packageName, userId);
            final long identityToken = Binder.clearCallingIdentity();
            try {
                mAppOpsManager.setMode(AppOpsManager.OPSTR_ACCESS_RESTRICTED_SETTINGS, packageUid,
                        packageName, ecmState);
            } finally {
                Binder.restoreCallingIdentity(identityToken);
            }
        }

        private @EcmState int getAppEcmState(@NonNull String packageName, @UserIdInt int userId)
                throws NameNotFoundException {
            int packageUid = getPackageUid(packageName, userId);
            final long identityToken = Binder.clearCallingIdentity();
            try {
                return mAppOpsManager.noteOpNoThrow(AppOpsManager.OPSTR_ACCESS_RESTRICTED_SETTINGS,
                        packageUid, packageName, mAttributionTag, /* message */ null);
            } finally {
                Binder.restoreCallingIdentity(identityToken);
            }
        }

        private boolean isSettingEcmProtected(@NonNull String settingIdentifier) {
            if (PROTECTED_SETTINGS.contains(settingIdentifier)) {
                return true;
            }
            // TODO(b/310654818): If this is a permission, coerce it into a PermissionGroup.
            // TODO(b/310218979): Add role selections as protected settings
            return false;
        }

        private int getPackageUid(@NonNull String packageName, @UserIdInt int userId)
                throws NameNotFoundException {
            return mPackageManager.getApplicationInfoAsUser(packageName, /* flags */ 0,
                    UserHandle.of(userId)).uid;
        }
    }
}
