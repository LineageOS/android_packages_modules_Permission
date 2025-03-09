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
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.permission.flags.Flags;
import android.telecom.Call;
import android.telecom.InCallService;

import com.android.server.LocalManagerRegistry;

/**
 * @hide
 *
 * This InCallService tracks called (both incoming and outgoing), and sends their information to the
 * EnhancedConfirmationService
 *
 **/
@FlaggedApi(Flags.FLAG_ENHANCED_CONFIRMATION_IN_CALL_APIS_ENABLED)
@SystemApi(client = SystemApi.Client.SYSTEM_SERVER)
@TargetApi(Build.VERSION_CODES.BAKLAVA)
public class EnhancedConfirmationCallTrackerService extends InCallService {
    private EnhancedConfirmationManagerLocal mEnhancedConfirmationManagerLocal;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Flags.enhancedConfirmationInCallApisEnabled()) {
            mEnhancedConfirmationManagerLocal =
                    LocalManagerRegistry.getManager(EnhancedConfirmationManagerLocal.class);
        }
    }

    @Override
    public void onCallAdded(@Nullable Call call) {
        if (mEnhancedConfirmationManagerLocal == null || call == null) {
            return;
        }

        mEnhancedConfirmationManagerLocal.addOngoingCall(call);
    }

    @Override
    public void onCallRemoved(@Nullable Call call) {
        if (mEnhancedConfirmationManagerLocal == null || call == null) {
            return;
        }

        mEnhancedConfirmationManagerLocal.removeOngoingCall(call.getDetails().getId());
    }

    /**
     * When unbound, we should assume all calls have finished. Notify the system of such.
     */
    public boolean onUnbind(@Nullable Intent intent) {
        if (mEnhancedConfirmationManagerLocal != null) {
            mEnhancedConfirmationManagerLocal.clearOngoingCalls();
        }
        return super.onUnbind(intent);
    }
}
