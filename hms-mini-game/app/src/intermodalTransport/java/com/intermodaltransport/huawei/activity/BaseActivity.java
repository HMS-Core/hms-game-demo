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

package com.intermodaltransport.huawei.activity;

import android.os.Bundle;

import com.huawei.gamecenter.minigame.huawei.Until.HMSLogHelper;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.jos.AppParams;
import com.huawei.hms.jos.JosApps;
import com.huawei.hms.jos.JosAppsClient;
import com.huawei.hms.jos.JosStatusCodes;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.support.account.request.AccountAuthParams;
import com.huawei.hms.utils.ResourceLoaderUtil;
import com.intermodaltransport.huawei.ExitApplication;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 此文件准备抽取出来，做基类Activity，提取所有公共逻辑以及初始化、浮标等代码块。
 */
public class BaseActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private boolean hasInit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Games.getBuoyClient(this).showFloatWindow();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Games.getBuoyClient(this).hideFloatWindow();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void init() {
        AccountAuthParams params = AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME;
        JosAppsClient appsClient = JosApps.getJosAppsClient(this);
        Task<Void> initTask;
        // Set the anti-addiction prompt context, this line must be added
        // 设置防沉迷提示语的Context，此行必须添加
        ResourceLoaderUtil.setmContext(this);
        initTask = appsClient.init(
                new AppParams(params, () -> {
                    ExitApplication.getInstance().exit();
                    // The callback will return in two situations:
                    // 1. When a no-adult, real name user logs in to the game during the day, Huawei will pop up a box to remind the player that the game is not allowed. The player clicks "OK" and Huawei will return to the callback
                    // 2. The no-adult, real name user logs in the game at the time allowed by the state. At 9 p.m., Huawei will pop up a box to remind the player that it is time. The player clicks "I know" and Huawei will return to the callback
                    // You can realize the anti addiction function of the game here, such as saving the game, calling the account to exit the interface or directly the game process
                    // 该回调会在如下两种情况下返回:
                    // 1.未成年人实名帐号在白天登录游戏，华为会弹框提示玩家不允许游戏，玩家点击“确定”，华为返回回调
                    // 2.未成年实名帐号在国家允许的时间登录游戏，到晚上9点，华为会弹框提示玩家已到时间，玩家点击“知道了”，华为返回回调
                    // 您可在此处实现游戏防沉迷功能，如保存游戏、调用帐号退出接口或直接游戏进程退出(如System.exit(0))
                }));
        initTask.addOnSuccessListener(aVoid -> {
            HMSLogHelper.getSingletonInstance().debug(TAG, "init success");
            hasInit = true;
            //  Make sure that the interface of showFloatWindow() is successfully called once after the game has been initialized successfully
            // 游戏初始化成功后务必成功调用过一次浮标显示接口
            showFloatWindowNewWay();
        }).addOnFailureListener(
                e -> {
                    if (e instanceof ApiException) {
                        ApiException apiException = (ApiException) e;
                        int statusCode = apiException.getStatusCode();
                        HMSLogHelper.getSingletonInstance().debug(TAG, "init failed statusCode:" + statusCode);
                        // Error code 7401 indicates that the user did not agree to Huawei joint operations privacy agreement
                        // 错误码为7401时表示用户未同意华为联运隐私协议
                        if (statusCode == JosStatusCodes.JOS_PRIVACY_PROTOCOL_REJECTED) {
                            HMSLogHelper.getSingletonInstance().debug(TAG, "has reject the protocol");
                            // You can exit the game or re-call the init interface.
                            // 在此处实现退出游戏或者重新调用初始化接口
                        }
                        // Handle other error codes.
                        // 在此处实现其他错误码的处理
                    }
                });
    }

    /**
     * Show the game buoy.
     * <p>
     * 显示游戏浮标。
     */
    private void showFloatWindowNewWay() {
        if (hasInit) {
            // 请务必在init成功后，调用浮标接口
            Games.getBuoyClient(this).showFloatWindow();
        }
    }
}