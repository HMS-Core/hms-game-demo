/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2022-2022. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.huawei.gamecenter.minigame.huawei.Until;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.text.TextUtils;

import static android.content.Context.MODE_PRIVATE;


public class UntilTool {
    private static final String TAG = "MiniGame_UntilTool";

    public static void addInfo(Context context, String playerId, int score) {
        SharedPreferences pref = context.getSharedPreferences("data", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(playerId, score);
        boolean isSuccess = editor.commit();
        if (isSuccess) {
            HMSLogHelper.getSingletonInstance().debug(TAG, "addInfo: success add !");
        }
    }

    public static int getInfo(Context context, String playerId) {
        if (TextUtils.isEmpty(playerId)) {
            int retCode = 0;
            HMSLogHelper.getSingletonInstance().debug(TAG, "playerId is empty !");
            return retCode;
        }
        SharedPreferences sp = context.getSharedPreferences("data", MODE_PRIVATE);
        return sp.getInt(playerId, 0);
    }

    public static int getScoreInt(String productId) {
        if (("20points").equals(productId)) {
            return 20;
        } else if (("100points").equals(productId)) {
            return 100;
        } else {
            return 0;
        }
    }

    public static boolean isNetSystemUsable(Context context) {
        boolean isNetUsable = false;
        ConnectivityManager manager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities networkCapabilities =
                manager.getNetworkCapabilities(manager.getActiveNetwork());
        if (networkCapabilities != null) {
            isNetUsable = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        }
        return isNetUsable;
    }

}
