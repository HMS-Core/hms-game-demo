
/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.huawei.hms.game.common;

import com.huawei.hms.api.ConnectionResult;
import com.huawei.hms.api.HuaweiApiClient;
import com.huawei.hms.support.api.game.HuaweiGame;

import android.app.Activity;

/**
 * ConnectClientSupport is a sample of supportor that helps to connect to HMS.
 */
public class ConnectClientSupport {
    private HuaweiApiClient mApiClient;

    private static final ConnectClientSupport INS = new ConnectClientSupport();

    public static ConnectClientSupport get() {
        return INS;
    }

    public interface IConnectCallBack {
        void onResult(HuaweiApiClient apiClient);
    }

    /**
     * connect to HMS and callback
     *
     * @param activity current activity
     * @param callback callback after connect successed
     */
    public void connect(Activity activity, final IConnectCallBack callback) {
        if (activity == null || callback == null) {
            return;
        }

        if (mApiClient != null && mApiClient.isConnected()) {
            callback.onResult(mApiClient);
            return;
        }

        mApiClient = new HuaweiApiClient.Builder(activity).addApi(HuaweiGame.GAME_API) // add api you would use
            // add callback after connect successful
            .addConnectionCallbacks(new HuaweiApiClient.ConnectionCallbacks() {
                // callback after connect completely
                @Override
                public void onConnected() {
                    callback.onResult(mApiClient);
                }

                // callback after connect suspended
                @Override
                public void onConnectionSuspended(int i) {
                    callback.onResult(null);
                }
            })
            // add callback after connect failed
            .addOnConnectionFailedListener(new HuaweiApiClient.OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult connectionResult) {
                    callback.onResult(null);
                }
            })
            .build();

        mApiClient.connect(activity);
    }
}
