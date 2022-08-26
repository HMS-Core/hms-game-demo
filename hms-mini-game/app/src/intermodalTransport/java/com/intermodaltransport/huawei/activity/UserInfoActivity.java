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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.huawei.gamecenter.minigame.huawei.R;
import com.huawei.gamecenter.minigame.huawei.Until.Constant;
import com.huawei.gamecenter.minigame.huawei.Until.HMSLogHelper;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.IapClient;
import com.huawei.hms.iap.entity.StartIapActivityReq;
import com.huawei.hms.iap.entity.StartIapActivityResult;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.player.Player;
import com.huawei.hms.jos.games.playerstats.GamePlayerStatistics;
import com.huawei.hms.jos.games.playerstats.GamePlayerStatisticsClient;
import com.huawei.hms.support.hwid.HuaweiIdAuthManager;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper;
import com.huawei.hms.support.hwid.result.AuthHuaweiId;
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService;
import com.intermodaltransport.huawei.ExitApplication;

public class UserInfoActivity extends Activity {
    private static final String TAG = "UserInfoActivity";
    private final AuthHuaweiId currentAuthId = null;
    private TextView averageOnLineMinutes;
    private TextView lastGameTime;
    private TextView paymentTimes;
    private TextView onlineTimes;
    private TextView payAmountRange;
    private TextView displayName;
    private ImageView photoSnap;
    private String photoUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_userinfo);
        initView();
        getGamePlayerStatistics();
        Player player = getIntent().getParcelableExtra(Constant.PLAYER_INFO_KEY);
        photoUri = getIntent().getStringExtra(Constant.PLAYER_ICON_URI);
        initData(player);
        ExitApplication.getInstance().addActivity(this);
    }

    private void initData(Player player) {
        displayName.setText(player.getDisplayName());
        setCircleImageView(photoUri, photoSnap);
    }

    private void setCircleImageView(String url, ImageView imageView) {
        if (!TextUtils.isEmpty(url)) {
            Glide.with(UserInfoActivity.this)
                    .load(url)
                    .placeholder(R.mipmap.game_photo_man)
                    .fitCenter()
                    .into(imageView);
        }

    }

    @SuppressLint("SetTextI18n")
    private void getGamePlayerStatistics() {
        GamePlayerStatisticsClient playerStatsClient = Games.getGamePlayerStatsClient(this);
        Task<GamePlayerStatistics> task = playerStatsClient.getGamePlayerStatistics(false);
        task.addOnSuccessListener(gamePlayerStatistics -> {
            if (gamePlayerStatistics != null) {
                averageOnLineMinutes.setText(checkAndConversionNumbers(gamePlayerStatistics.getAverageOnLineMinutes()) + getString(R.string.userinfo_minute));
                lastGameTime.setText(checkAndConversionNumbers(gamePlayerStatistics.getDaysFromLastGame()) + getString(R.string.userinfo_days));
                paymentTimes.setText(checkAndConversionNumbers(gamePlayerStatistics.getPaymentTimes()) + getString(R.string.userinfo_times));
                onlineTimes.setText(checkAndConversionNumbers(gamePlayerStatistics.getOnlineTimes()) + getString(R.string.userinfo_times));
                payAmountRange.setText(convertTotalPaymentRange(gamePlayerStatistics.getTotalPurchasesAmountRange()));
            } else {
                // 当gamePlayerStatistics获取为null时，界面默认刷新所有数据为0。
                int defaultNumber = 0;
                averageOnLineMinutes.setText("" + defaultNumber + getString(R.string.userinfo_minute));
                lastGameTime.setText("" + defaultNumber + getString(R.string.userinfo_days));
                paymentTimes.setText("" + defaultNumber + getString(R.string.userinfo_times));
                onlineTimes.setText("" + defaultNumber + getString(R.string.userinfo_times));
                payAmountRange.setText(convertTotalPaymentRange(defaultNumber));
            }
        }).addOnFailureListener(e -> {
            if (e instanceof ApiException) {
                String result = "rtnCode:" + ((ApiException) e).getStatusCode();
                HMSLogHelper.getSingletonInstance().debug(TAG, "getGamePlayerStatistics API return code is : " + result);
            }
        });
    }

    // 当返回-1时，转换成0
    private String checkAndConversionNumbers(float data) {
        if (String.valueOf(data).equals("-1.0")) {
            return "0";
        }
        return String.valueOf(data);
    }

    // 当返回-1时，转换成0
    private String checkAndConversionNumbers(int data) {
        if (String.valueOf(data).equals("-1")) {
            return "0";
        }
        return String.valueOf(data);
    }

    /**
     * @param totalPurchasesAmountRange 使用1到6的数字表示。
     * @return 当前玩家12个月内总付费额度的区间，以美元为单位。
     */
    private String convertTotalPaymentRange(int totalPurchasesAmountRange) {
        switch (totalPurchasesAmountRange) {
            // 10美元以下
            case 2:
                return "<10$";
            // 10美元以上（含10美元），50美元以下。
            case 3:
                return ">=10$" + getString(R.string.and) + "<50$";
            // 50美元以上（含50美元），300美元以下。
            case 4:
                return ">=50$" + getString(R.string.and) + "<300$";
            // 300美元以上（含300美元），1000美元以下。
            case 5:
                return ">=300$" + getString(R.string.and) + "<1000$";
            // 1000美元以上。
            case 6:
                return ">=1000$";
            // 无消费。
            default:
                return getString(R.string.no_consumption);
        }
    }

    private void initView() {
        averageOnLineMinutes = findViewById(R.id.text_average_onLine_minutes);
        lastGameTime = findViewById(R.id.text_last_game_time);
        paymentTimes = findViewById(R.id.text_payment_times);
        onlineTimes = findViewById(R.id.text_online_times);
        payAmountRange = findViewById(R.id.text_pay_amount_range);
        findViewById(R.id.user_press_back_button_id).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.manager).setOnClickListener(v -> startIapActivity());
        displayName = findViewById(R.id.text_display_name);
        photoSnap = findViewById(R.id.image_photo_head);
    }

    private void startIapActivity() {
        // 创建一个StartIapActivityReq对象
        StartIapActivityReq req = new StartIapActivityReq();
        req.setType(StartIapActivityReq.TYPE_SUBSCRIBE_MANAGER_ACTIVITY);
        IapClient mClient = Iap.getIapClient(this);
        Task<StartIapActivityResult> task = mClient.startIapActivity(req);
        task.addOnSuccessListener(result -> {
            HMSLogHelper.getSingletonInstance().debug(TAG, "onSuccess");
            // 请求成功，需拉起IAP返回的页面
            if (result != null) {
                result.startActivity(UserInfoActivity.this);
            }
        }).addOnFailureListener(e -> HMSLogHelper.getSingletonInstance().debug(TAG, "onFailure"));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}