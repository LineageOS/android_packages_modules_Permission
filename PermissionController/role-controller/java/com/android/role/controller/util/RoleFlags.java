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

package com.android.role.controller.util;

import android.os.Build;

import androidx.annotation.ChecksSdkIntAtLeast;

import java.util.Objects;

/** Util class for getting shared feature flag check logic. */
public final class RoleFlags {
    private RoleFlags() { /* cannot be instantiated */ }

    /**
     * Returns whether profile group exclusive roles are available. Profile exclusive roles are
     * available on B+
     */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.BAKLAVA)
    public static boolean isProfileGroupExclusivityAvailable() {
        // TODO(b/372743073): change to isAtLeastB once available
        return isAtLeastB() && com.android.permission.flags.Flags.crossUserRoleEnabled();
    }

    // TODO(b/372743073): remove once SdkLevel.isAtLeastB available
    @ChecksSdkIntAtLeast(api = 36 /* BUILD_VERSION_CODES.Baklava */)
    public static boolean isAtLeastB() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
                || Objects.equals(Build.VERSION.CODENAME, "Baklava");
    }
}
