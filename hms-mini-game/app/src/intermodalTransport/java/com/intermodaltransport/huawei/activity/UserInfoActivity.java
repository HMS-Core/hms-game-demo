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
        getGamePlayerStatistics(currentAuthId);
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
    private void getGamePlayerStatistics(AuthHuaweiId authHuaweiId) {
        if (authHuaweiId == null) {
            HMSLogHelper.getSingletonInstance().debug(TAG, "authHuaWeiId is null ,please login  again");
            HuaweiIdAuthParams authParams = new HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME).createParams();
            HuaweiIdAuthService service = HuaweiIdAuthManager.getService(this, authParams);
            startActivityForResult(service.getSignInIntent(), Constant.SIGN_IN_REQUEST_CODE);
        } else {
            GamePlayerStatisticsClient playerStatsClient;
            //noinspection deprecation
            playerStatsClient = Games.getGamePlayerStatsClient(this, authHuaweiId);
            Task<GamePlayerStatistics> task = playerStatsClient.getGamePlayerStatistics(false);
            task.addOnSuccessListener(gamePlayerStatistics -> {
                if (gamePlayerStatistics != null) {
                    averageOnLineMinutes.setText("" + gamePlayerStatistics.getAverageOnLineMinutes() + getString(R.string.userinfo_minute));
                    lastGameTime.setText("" + gamePlayerStatistics.getDaysFromLastGame() + getString(R.string.userinfo_days));
                    paymentTimes.setText("" + gamePlayerStatistics.getPaymentTimes() + getString(R.string.userinfo_times));
                    onlineTimes.setText("" + gamePlayerStatistics.getOnlineTimes() + getString(R.string.userinfo_times));
                    payAmountRange.setText("" + gamePlayerStatistics.getTotalPurchasesAmountRange());
                } else {
                    // 当gamePlayerStatistics获取为null时，界面默认刷新所有数据为0。
                    int defaultNumber = 0;
                    averageOnLineMinutes.setText("" + defaultNumber + getString(R.string.userinfo_minute));
                    lastGameTime.setText("" + defaultNumber + getString(R.string.userinfo_days));
                    paymentTimes.setText("" + defaultNumber + getString(R.string.userinfo_times));
                    onlineTimes.setText("" + defaultNumber + getString(R.string.userinfo_times));
                    payAmountRange.setText("" + defaultNumber);
                }
            }).addOnFailureListener(e -> {
                if (e instanceof ApiException) {
                    String result = "rtnCode:" + ((ApiException) e).getStatusCode();
                    HMSLogHelper.getSingletonInstance().debug(TAG, "getGamePlayerStatistics API return code is : " + result);
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.SIGN_IN_REQUEST_CODE) {
            Task<AuthHuaweiId> authIdTask = HuaweiIdAuthManager.parseAuthResultFromIntent(data);
            authIdTask.addOnSuccessListener(authHuaWeiId -> {
                HMSLogHelper.getSingletonInstance().debug(TAG, "sign in success.");
                getGamePlayerStatistics(authHuaWeiId);
            }).addOnFailureListener(e -> HMSLogHelper.getSingletonInstance().debug(TAG, "parseAuthResultFromIntent failed"));
        }
    }

    private void initView() {
        averageOnLineMinutes = findViewById(R.id.text_average_onLine_minutes);
        lastGameTime = findViewById(R.id.text_last_game_time);
        paymentTimes = findViewById(R.id.text_payment_times);
        onlineTimes = findViewById(R.id.text_online_times);
        payAmountRange = findViewById(R.id.text_pay_amount_range);
        findViewById(R.id.user_press_back_button_id).setOnClickListener(v -> onBackPressed());
        displayName = findViewById(R.id.text_display_name);
        photoSnap = findViewById(R.id.image_photo_head);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}