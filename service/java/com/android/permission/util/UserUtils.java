/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.permission.util;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Binder;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.permission.internal.compat.UserHandleCompat;

import com.android.internal.util.Preconditions;
import com.android.modules.utils.build.SdkLevel;
import com.android.permission.flags.Flags;

import java.util.ArrayList;
import java.util.List;

/** Utility class to deal with Android users. */
public final class UserUtils {

    private UserUtils() {}

    /** Enforces cross user permission for the calling UID and the given {@code userId}. */
    public static void enforceCrossUserPermission(
            @UserIdInt int userId,
            boolean allowAll,
            boolean enforceForProfileGroup,
            @NonNull String message,
            @NonNull Context context) {
        final int callingUid = Binder.getCallingUid();
        final int callingUserId = UserHandleCompat.getUserId(callingUid);
        if (userId == callingUserId && !enforceForProfileGroup) {
            return;
        }
        Preconditions.checkArgument(
                userId >= UserHandleCompat.USER_SYSTEM
                        || (allowAll && userId == UserHandleCompat.USER_ALL),
                "Invalid user " + userId);
        context.enforceCallingOrSelfPermission(
                android.Manifest.permission.INTERACT_ACROSS_USERS_FULL, message);
        if (callingUid != Process.SHELL_UID || userId == UserHandleCompat.USER_ALL) {
            return;
        }

        if (enforceForProfileGroup) {
            DevicePolicyManager devicePolicyManager =
                    context.getSystemService(DevicePolicyManager.class);
            if (!devicePolicyManager.isOrganizationOwnedDeviceWithManagedProfile()) {
                // For profileGroup exclusive roles users on BYOD are free to choose personal o
                // work profile app regardless of DISALLOW_DEBUGGING_FEATURES
                return;
            }

            Context userContext = UserUtils.getUserContext(userId, context);
            List<UserHandle> profiles = getUserProfiles(userContext, true);
            final int profilesSize = profiles.size();
            for (int i = 0; i < profilesSize; i++) {
                int profileId = profiles.get(i).getIdentifier();
                if (profileId == callingUserId) {
                    continue;
                }
                enforceShellRestriction(profileId, context);
            }
        } else {
            enforceShellRestriction(userId, context);
        }
    }

    private static void enforceShellRestriction(int userId, @NonNull Context context) {
        UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager.hasUserRestrictionForUser(
                UserManager.DISALLOW_DEBUGGING_FEATURES, UserHandle.of(userId))) {
            throw new SecurityException(
                    "Shell does not have permission to access user " + userId);
        }
    }

    /** Returns whether a given {@code userId} corresponds to an existing user. */
    public static boolean isUserExistent(@UserIdInt int userId, @NonNull Context context) {
        return getUserHandles(context).contains(UserHandle.of(userId));
    }

    /** Returns all the alive users on the device. */
    @NonNull
    public static List<UserHandle> getUserHandles(@NonNull Context context) {
        UserManager userManager = context.getSystemService(UserManager.class);
        // This call requires the MANAGE_USERS permission.
        final long identity = Binder.clearCallingIdentity();
        try {
            return userManager.getUserHandles(true);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /** Returns all the enabled user profiles on the device. */
    @NonNull
    public static List<UserHandle> getUserProfiles(@NonNull Context context) {
        return getUserProfiles(context, false);
    }

    /**
     * Returns all the enabled user profiles on the device
     *
     * @param context the {@link Context}
     * @param excludePrivate {@code true} to exclude private profiles from returned list of users
     */
    @NonNull
    public static List<UserHandle> getUserProfiles(@NonNull Context context,
            boolean excludePrivate) {
        UserManager userManager = context.getSystemService(UserManager.class);
        // This call requires the QUERY_USERS permission.
        final long identity = Binder.clearCallingIdentity();
        try {
            List<UserHandle> profiles = userManager.getUserProfiles();
            if (!excludePrivate) {
                return profiles;
            }
            List<UserHandle> filteredProfiles = new ArrayList<>();
            final int profilesSize = profiles.size();
            for (int i = 0; i < profilesSize; i++) {
                UserHandle user = profiles.get(i);
                if (!isPrivateProfile(user.getIdentifier(), context)) {
                    filteredProfiles.add(user);
                }
            }
            return filteredProfiles;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /**
     * Returns the parent of a given user, or userId if it has no parent (e.g. it is the primary
     * profile)
     */
    @UserIdInt
    public static int getProfileParentIdOrSelf(@UserIdInt int userId, @NonNull Context context) {
        UserHandle profileParent = getProfileParent(userId, context);
        // If profile parent userhandle is null, then original user id is the parent
        return profileParent != null ? profileParent.getIdentifier() : userId;
    }

    /** Returns the parent of a given user. */
    @Nullable
    private static UserHandle getProfileParent(@UserIdInt int userId, @NonNull Context context) {
        Context userContext = getUserContext(userId, context);
        UserManager userManager = userContext.getSystemService(UserManager.class);
        // This call requires the INTERACT_ACROSS_USERS permission.
        final long identity = Binder.clearCallingIdentity();
        try {
            return userManager.getProfileParent(UserHandle.of(userId));
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /** Returns whether a given {@code userId} corresponds to a managed profile. */
    public static boolean isManagedProfile(@UserIdInt int userId, @NonNull Context context) {
        UserManager userManager = context.getSystemService(UserManager.class);
        // This call requires the QUERY_USERS permission.
        final long identity = Binder.clearCallingIdentity();
        try {
            return userManager.isManagedProfile(userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /**
     * Returns whether the given {@code userId} is a private profile. Note that private profiles are
     * allowed from Android V+ only, so this method will return false on Sdk levels below that.
     */
    public static boolean isPrivateProfile(@UserIdInt int userId, @NonNull Context context) {
        if (!isPrivateProfileSupported()) {
            return false;
        }
        // It's needed to clear the calling identity because we are going to query the UserManager
        // for isPrivateProfile() and Context for createContextAsUser, which requires one of the
        // following permissions:
        // MANAGE_USERS, QUERY_USERS, or INTERACT_ACROSS_USERS.
        final long identity = Binder.clearCallingIdentity();
        try {
            Context userContext = getUserContext(userId, context);
            UserManager userManager = userContext.getSystemService(UserManager.class);
            return userManager != null && userManager.isPrivateProfile();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }


    /**
     * Returns whether private profile's allowed to exist.  This can be true iff the SdkLevel is at
     * least V AND the permission module's private profile feature flag is enabled.
     */
    public static boolean isPrivateProfileSupported() {
        //TODO(b/286539356) add the os feature flag protection when available.
        return SdkLevel.isAtLeastV() && Flags.privateProfileSupported();
    }

    /**
     * Returns whether a given {@code userId} corresponds to a running managed profile, i.e. the
     * user is running and the quiet mode is not enabled.
     */
    public static boolean isProfileRunning(@UserIdInt int userId, @NonNull Context context) {
        UserManager userManager = context.getSystemService(UserManager.class);
        // This call requires the QUERY_USERS permission
        final long identity = Binder.clearCallingIdentity();
        try {
            return userManager.isUserRunning(UserHandle.of(userId))
                    && !userManager.isQuietModeEnabled(UserHandle.of(userId));
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @NonNull
    public static Context getUserContext(@UserIdInt int userId, @NonNull Context context) {
        if (SdkLevel.isAtLeastS() && context.getUser().getIdentifier() == userId) {
            return context;
        } else {
            return context.createContextAsUser(UserHandle.of(userId), 0);
        }
    }
}
