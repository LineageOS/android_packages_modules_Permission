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

package com.android.role.controller.behavior;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.nfc.cardemulation.CardEmulation;
import android.nfc.cardemulation.HostApduService;
import android.nfc.cardemulation.OffHostApduService;
import android.os.Build;
import android.os.UserHandle;
import android.permission.flags.Flags;
import android.service.quickaccesswallet.QuickAccessWalletService;
import android.util.ArraySet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;
import com.android.role.controller.model.Role;
import com.android.role.controller.model.RoleBehavior;
import com.android.role.controller.util.UserUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Handles the behavior of the wallet role.
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class WalletRoleBehavior implements RoleBehavior {

    private static final String LOG_TAG = WalletRoleBehavior.class.getSimpleName();

    @Override
    public boolean isAvailableAsUser(@NonNull Role role, @NonNull UserHandle user,
            @NonNull Context context) {
        return SdkLevel.isAtLeastV() && Flags.walletRoleEnabled()
                && !UserUtils.isProfile(user, context);
    }

    @Nullable
    @Override
    public List<String> getDefaultHoldersAsUser(@NonNull Role role, @NonNull UserHandle user,
            @NonNull Context context) {
        Context userContext = UserUtils.getUserContext(context, user);
        ComponentName preferredPaymentService =
                CardEmulation.getPreferredPaymentService(userContext);
        if (preferredPaymentService != null) {
            return Collections.singletonList(preferredPaymentService.getPackageName());
        }

        return null;
    }

    @Nullable
    @Override
    public Boolean isPackageQualifiedAsUser(@NonNull Role role, @NonNull String packageName,
            @NonNull UserHandle user, @NonNull Context context) {
        return !getQualifyingPackageNamesInternal(packageName, user, context).isEmpty();
    }

    @Nullable
    @Override
    public List<String> getQualifyingPackagesAsUser(@NonNull Role role, @NonNull UserHandle user,
            @NonNull Context context) {
        return new ArrayList<>(getQualifyingPackageNamesInternal(null, user, context));
    }

    @NonNull
    private static Set<String> getQualifyingPackageNamesInternal(@Nullable String packageName,
            @NonNull UserHandle user, @NonNull Context context) {
        Set<String> packageNames =
                resolvePackageNames(HostApduService.SERVICE_INTERFACE, packageName, user,
                        context);
        packageNames.addAll(
                resolvePackageNames(OffHostApduService.SERVICE_INTERFACE, packageName, user,
                        context));
        packageNames.addAll(
                resolvePackageNames(QuickAccessWalletService.SERVICE_INTERFACE, packageName, user,
                        context));
        return packageNames;
    }

    @NonNull
    private static Set<String> resolvePackageNames(@NonNull String action,
            @Nullable String packageName, @NonNull UserHandle user, @NonNull Context context) {
        Intent intent = new Intent(action).setPackage(packageName);
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager
                .queryIntentServicesAsUser(intent, PackageManager.MATCH_DIRECT_BOOT_AWARE
                        | PackageManager.MATCH_DIRECT_BOOT_UNAWARE, user);
        Set<String> packageNames = new ArraySet<>();
        int resolveInfosSize = resolveInfos.size();
        for (int i = 0; i < resolveInfosSize; i++) {
            packageNames.add(resolveInfos.get(i).serviceInfo.packageName);
        }
        return packageNames;
    }
}
