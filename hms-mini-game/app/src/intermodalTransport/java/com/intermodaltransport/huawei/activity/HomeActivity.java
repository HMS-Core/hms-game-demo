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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.huawei.gamecenter.minigame.huawei.GameActivity;
import com.huawei.gamecenter.minigame.huawei.R;
import com.huawei.gamecenter.minigame.huawei.Until.Constant;
import com.huawei.gamecenter.minigame.huawei.Until.HMSLogHelper;
import com.huawei.gamecenter.minigame.huawei.Until.TimeUtil;
import com.huawei.gamecenter.minigame.huawei.Until.UntilTool;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.IapApiException;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseReq;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseResult;
import com.huawei.hms.iap.entity.InAppPurchaseData;
import com.huawei.hms.iap.entity.OwnedPurchasesReq;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;
import com.huawei.hms.jos.AppUpdateClient;
import com.huawei.hms.jos.JosApps;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.player.Player;
import com.huawei.hms.jos.games.player.PlayersClientImpl;
import com.huawei.hms.support.account.AccountAuthManager;
import com.huawei.hms.support.account.request.AccountAuthParams;
import com.huawei.hms.support.account.request.AccountAuthParamsHelper;
import com.huawei.hms.support.account.result.AccountAuthResult;
import com.huawei.hms.support.account.result.AuthAccount;
import com.huawei.updatesdk.service.appmgr.bean.ApkUpgradeInfo;
import com.huawei.updatesdk.service.otaupdate.CheckUpdateCallBack;
import com.huawei.updatesdk.service.otaupdate.UpdateKey;
import com.intermodaltransport.huawei.ExitApplication;
import com.intermodaltransport.huawei.MainActivity;
import com.intermodaltransport.huawei.SubscribeManager;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.huawei.gamecenter.minigame.huawei.Until.Constant.OBTAIN_BONUS_POINTS_EVERY_DAY;
import static com.huawei.updatesdk.service.otaupdate.UpdateStatusCode.NO_UPGRADE_INFO;


public class HomeActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "HomeActivity";
    private static final int SIGN_IN_INTENT = 3000;
    private static String currentId;
    private static int currentScore = 0;
    private TextView displayNameView;
    private Player currentPlayer;
    private TextView scoreTextView;
    private String photoUri = null;
    private ConstraintLayout obtainScoreLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_home_layout);
        currentPlayer = getIntent().getParcelableExtra(Constant.PLAYER_INFO_KEY);
        photoUri = getIntent().getStringExtra(Constant.PLAYER_ICON_URI);
        initView();
        currentId = currentPlayer.getOpenId();
        displayNameView = findViewById(R.id.text_home_display_name);
        // 游戏登录成功之后，调用补单接口进行检查是否有掉单商品未发货。
        obtainProduct();
        ExitApplication.getInstance().addActivity(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initData(currentPlayer);
    }

    private void initView() {
        scoreTextView = findViewById(R.id.text_score_show_home);
        obtainScoreLayout = findViewById(R.id.obtain_score_layout);
        findViewById(R.id.image_button_userInfo).setOnClickListener(v -> {
            // 跳转个人游戏信息界面
            Intent intent = new Intent(HomeActivity.this, UserInfoActivity.class);
            Bundle bundle = new Bundle();
            bundle.putParcelable(Constant.PLAYER_INFO_KEY, currentPlayer);
            bundle.putString(Constant.PLAYER_ICON_URI, photoUri);
            intent.putExtras(bundle);
            startActivity(intent);
        });

        findViewById(R.id.btn_jump_shop).setOnClickListener(v -> {
            // 跳转开启支付界面
            Intent intent = new Intent(HomeActivity.this, ShoppingActivity.class);
            Bundle bundle = new Bundle();
            bundle.putParcelable(Constant.PLAYER_INFO_KEY, currentPlayer);
            bundle.putString(Constant.PLAYER_ICON_URI, photoUri);
            intent.putExtras(bundle);
            startActivity(intent);
        });

        findViewById(R.id.btn_image_check_update).setOnClickListener(v -> {
            // 检测更新接口调用
            checkUpdate();
        });

        obtainScoreLayout.setOnClickListener(this);
        findViewById(R.id.btn_start_game_home).setOnClickListener(this);
        findViewById(R.id.btn_jump_into_rank_activity).setOnClickListener(v -> Toast.makeText(HomeActivity.this, getString(R.string.home_capabilities_not_deployed), Toast.LENGTH_LONG).show());
        findViewById(R.id.btn_jump_into_summary_activity).setOnClickListener(v -> Toast.makeText(HomeActivity.this, getString(R.string.home_capabilities_not_deployed), Toast.LENGTH_LONG).show());
        findViewById(R.id.btn_jump_into_archive_activity).setOnClickListener(v -> Toast.makeText(HomeActivity.this, getString(R.string.home_capabilities_not_deployed), Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        showFloatWindowNewWay();
        SubscribeManager.getSingletonInstance().checkSubIsValidAndRefreshView(obtainScoreLayout, this, currentId, null);
    }


    /**
     * @param player 登录页面获取的登录对象信息，用于刷新大厅页面：用户头像、用户昵称、积分值。
     */
    private void initData(Player player) {
        if (TextUtils.isEmpty(currentId)) {
            HMSLogHelper.getSingletonInstance().debug(TAG, "initData player is null ,please login  again");
        } else {
            setCircleImageView(photoUri);
            displayNameView.setText(player.getDisplayName());
            currentScore = UntilTool.getInfo(this, currentId);
            scoreTextView.setText(String.valueOf(currentScore));
        }

    }

    private void setCircleImageView(String url) {
        CircleImageView imageView= findViewById(R.id.image_button_userInfo);
        if (!TextUtils.isEmpty(url)) {
            Glide.with(HomeActivity.this)
                    .load(url)
                    .placeholder(R.mipmap.game_photo_man)
                    .fitCenter()
                    .into(imageView);
        }

    }

    /**
     * Games released in the Chinese mainland: The update API provided by Huawei must be called upon game launch.
     * Games released outside the Chinese mainland: It is optional for calling the update API provided by Huawei upon
     * game launch.
     * <p>
     * 检测应用新版本，中国大陆发布的应用：应用启动时必须使用华为升级接口进行应用升级。
     * 中国大陆以外发布的应用：不强制要求。
     */
    public void checkUpdate() {
        AppUpdateClient client = JosApps.getAppUpdateClient(this);
        client.checkAppUpdate(this, new UpdateCallBack(this));
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start_game_home:
                Intent intent = new Intent(this, GameActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelable(Constant.PLAYER_INFO_KEY, currentPlayer);
                bundle.putString(Constant.PLAYER_ICON_URI, photoUri);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
            case R.id.obtain_score_layout:
                long lastUpdateTime = UntilTool.getLastScoreUpdateTime(this, currentId);
                if (TimeUtil.isSameDayOfMillis(lastUpdateTime, System.currentTimeMillis())) {
                    Toast.makeText(this, getString(R.string.already_received_tips), Toast.LENGTH_SHORT).show();
                    break;
                }
                UntilTool.updateScoreTime(this, currentId);
                // 会员每日领取20积分
                currentScore = currentScore + OBTAIN_BONUS_POINTS_EVERY_DAY;
                scoreTextView.setText(String.valueOf(currentScore));
                UntilTool.addInfo(this, currentId, currentScore);
                HMSLogHelper.getSingletonInstance().debug(TAG, "updateScore currentScore:" + currentScore);
                obtainScoreLayout.setBackgroundResource(R.drawable.button_already_obtain_score);
                break;
            default:
                break;
        }
    }

    /**
     * （本案例放在Activity_onCreate生命周期中执行本段代码逻辑）游戏启动登录之后需要调用补单接口，进行查询当前登录账号所有的已支付未发货状态订单进行补发商品。
     */
    private void obtainProduct() {
        HMSLogHelper.getSingletonInstance().debug(TAG, "obtainProduct start.");
        // 构造一个OwnedPurchasesReq对象
        OwnedPurchasesReq ownedPurchasesReq = new OwnedPurchasesReq();
        // priceType: 0：消耗型商品; 1：非消耗型商品; 2：订阅型商品
        ownedPurchasesReq.setPriceType(Constant.PurchasesPriceType.PRICE_TYPE_CONSUMABLE_GOODS);
        // 获取调用接口的Activity对象
        final Activity activity = HomeActivity.this;
        // 调用obtainOwnedPurchases接口获取所有已购但未发货的消耗型商品的购买信息
        Task<OwnedPurchasesResult> task = Iap.getIapClient(activity).obtainOwnedPurchases(ownedPurchasesReq);
        task.addOnSuccessListener(result -> {
            HMSLogHelper.getSingletonInstance().debug(TAG, "OwnedPurchasesResult onSuccess");

            // 获取接口请求成功的结果
            if (result != null && result.getInAppPurchaseDataList() != null) {
                for (int i = 0; i < result.getInAppPurchaseDataList().size(); i++) {
                    String inAppPurchaseData = result.getInAppPurchaseDataList().get(i);
                    // 使用应用的IAP公钥验证inAppPurchaseData的签名数据
                    // 如果验签成功，确认每个商品的购买状态。确认商品已支付后，检查此前是否已发过货，未发货则进行发货操作。发货成功后执行消耗操作
                    try {
                        InAppPurchaseData inAppPurchaseDataBean = new InAppPurchaseData(inAppPurchaseData);
                        int purchaseState = inAppPurchaseDataBean.getPurchaseState();
                        HMSLogHelper.getSingletonInstance().debug(TAG, "OwnedPurchasesResult purchaseState code is :" + purchaseState);
                        consumeGood(inAppPurchaseDataBean.getProductId(), inAppPurchaseDataBean.getPurchaseToken());
                    } catch (JSONException e) {
                        HMSLogHelper.getSingletonInstance().error(TAG, e.getMessage());
                    }
                }
            }
        }).addOnFailureListener(e -> {
            if (e instanceof IapApiException) {
                IapApiException apiException = (IapApiException) e;
                int returnCode = apiException.getStatusCode();
                HMSLogHelper.getSingletonInstance().debug(TAG, "OwnedPurchasesResult onFailure:" + returnCode);
            } else {
                // 其他外部错误
                HMSLogHelper.getSingletonInstance().debug(TAG, "OwnedPurchasesResult onFailure");
            }
        });
    }

    /**
     * @param productId     购买支付后未发货的商品ID
     * @param purchaseToken 掉单订单购买凭据
     */
    private void consumeGood(String productId, String purchaseToken) {
        // 构造一个ConsumeOwnedPurchaseReq对象
        ConsumeOwnedPurchaseReq req = new ConsumeOwnedPurchaseReq();
        req.setPurchaseToken(purchaseToken);
        // 调用consumeOwnedPurchase接口
        Task<ConsumeOwnedPurchaseResult> task = Iap.getIapClient(HomeActivity.this).consumeOwnedPurchase(req);
        task.addOnSuccessListener(result -> {
            // 获取接口请求成功时的结果信息
            HMSLogHelper.getSingletonInstance().debug(TAG, "ConsumeOwnedPurchase onSuccess: " + result.getReturnCode());
        }).addOnFailureListener(e -> {
            if (e instanceof IapApiException) {
                IapApiException apiException = (IapApiException) e;
                int returnCode = apiException.getStatusCode();
                HMSLogHelper.getSingletonInstance().debug(TAG, "ConsumeOwnedPurchase onFailure: " + returnCode);
            } else {
                // 其他外部错误
                HMSLogHelper.getSingletonInstance().debug(TAG, "ConsumeOwnedPurchase onFailure.");
            }
        });
    }

    /**
     * @param productId 补单消耗成功后，根据商品ID进行发放对应的虚拟商品,刷新积分显示控件,并保存至本地share文件存储中。
     */
    @SuppressLint("SetTextI18n")
    private void updateSoreView(String productId) {
        currentScore = currentScore + UntilTool.getScoreInt(productId);
        scoreTextView.setText(String.valueOf(currentScore));
        UntilTool.addInfo(this, currentId, currentScore);
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideFloatWindowNewWay();
    }


    /**
     * Show the game buoy.
     * <p>
     * 显示游戏浮标。
     */
    private void showFloatWindowNewWay() {
        // 请务必在init成功后，调用浮标接口
        Games.getBuoyClient(this).showFloatWindow();
    }

    /**
     * Hide the displayed game buoy.
     * <p>
     * 隐藏已经显示的游戏浮标。
     */
    private void hideFloatWindowNewWay() {
        Games.getBuoyClient(this).hideFloatWindow();
    }

    private class UpdateCallBack implements CheckUpdateCallBack {
        private static final String TAG = "CheckUpdateCallBack_";
        private final WeakReference<Context> mContextWeakReference;

        private UpdateCallBack(Context context) {
            mContextWeakReference = new WeakReference<>(context);
        }

        @Override
        public void onUpdateInfo(Intent intent) {
            if (intent != null) {
                // 更新状态信息
                int status = intent.getIntExtra(UpdateKey.STATUS, -99);
                HMSLogHelper.getSingletonInstance().debug(TAG, "check update status is:" + status);
                // 返回错误码
                int rtnCode = intent.getIntExtra(UpdateKey.FAIL_CODE, -99);
                // 返回失败信息
                String rtnMessage = intent.getStringExtra(UpdateKey.FAIL_REASON);
                if (status == NO_UPGRADE_INFO) {
                    Toast.makeText(HomeActivity.this, getString(R.string.update_no_gradeinfo), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(HomeActivity.this, getString(R.string.update_des) + status, Toast.LENGTH_LONG).show();
                }
                // 强制更新应用时，弹出对话框后用户是否点击“退出应用”按钮
                boolean isExit = intent.getBooleanExtra(UpdateKey.MUST_UPDATE, false);
                HMSLogHelper.getSingletonInstance().debug(TAG, "rtnCode = " + rtnCode + "rtnMessage = " + rtnMessage);

                Serializable info = intent.getSerializableExtra(UpdateKey.INFO);
                // 如果info属于ApkUpgradeInfo类型，则拉起更新弹框
                if (info instanceof ApkUpgradeInfo) {
                    Context context = mContextWeakReference.get();
                    if (context != null) {
                        // showUpdateDialog接口中最后一个字段传入不同取值会带来不同的用户体验，具体请参考本文档的场景描述，此处以false为例
                        JosApps.getAppUpdateClient(context).showUpdateDialog(context, (ApkUpgradeInfo) info, false);
                    }
                    HMSLogHelper.getSingletonInstance().debug(TAG, "check update success and there is a new update");
                }
                HMSLogHelper.getSingletonInstance().debug(TAG, "check update isExit=" + isExit);
                if (isExit) {
                    // 是强制更新应用，用户在弹出的升级提示框中选择了“退出应用”，处理逻辑由您自行控制，这里只是个例子
                    System.exit(0);
                }
            }
        }

        // ignored
        // 预留, 无需处理
        @Override
        public void onMarketInstallInfo(Intent intent) {
            HMSLogHelper.getSingletonInstance().debug(TAG, "check update failed: info not instance of ApkUpgradeInfo");
        }

        // ignored
        // 预留, 无需处理
        @Override
        public void onMarketStoreError(int responseCode) {
            HMSLogHelper.getSingletonInstance().debug(TAG, "check update failed");
        }

        // ignored
        // 预留, 无需处理
        @Override
        public void onUpdateStoreError(int responseCode) {
            HMSLogHelper.getSingletonInstance().debug(TAG, "check update failed");
        }
    }
}