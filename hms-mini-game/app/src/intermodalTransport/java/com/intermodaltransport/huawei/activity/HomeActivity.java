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

import com.bumptech.glide.Glide;
import com.huawei.gamecenter.minigame.huawei.GameActivity;
import com.huawei.gamecenter.minigame.huawei.R;
import com.huawei.gamecenter.minigame.huawei.Until.Constant;
import com.huawei.gamecenter.minigame.huawei.Until.HMSLogHelper;
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


public class HomeActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "HomeActivity";
    private static final int SIGN_IN_INTENT = 3000;
    private static String currentId;
    private static int currentScore = 0;
    private TextView displayNameView;
    private Player currentPlayer;
    private TextView scoreTextView;
    private String photoUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_home_layout);
        currentPlayer = getIntent().getParcelableExtra(Constant.PLAYER_INFO_KEY);
        photoUri = getIntent().getStringExtra(Constant.PLAYER_ICON_URI);
        initView();
        currentId = currentPlayer.getPlayerId();
        displayNameView = findViewById(R.id.text_home_display_name);
        // 游戏登录成功之后，调用补单接口进行检查是否有掉单商品未发货。
        obtainProduct();
        ExitApplication.getInstance().addActivity(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentPlayer == null) {
            HMSLogHelper.getSingletonInstance().debug(TAG, "player obj is null,please login first !");
        } else {
            initData(currentPlayer);
        }
    }

    private void initView() {
        scoreTextView = findViewById(R.id.text_score_show_home);
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

        findViewById(R.id.btn_start_game_home).setOnClickListener(this);
        findViewById(R.id.btn_jump_into_rank_activity).setOnClickListener(v -> Toast.makeText(HomeActivity.this, getString(R.string.home_capabilities_not_deployed), Toast.LENGTH_LONG).show());
        findViewById(R.id.btn_jump_into_summary_activity).setOnClickListener(v -> Toast.makeText(HomeActivity.this, getString(R.string.home_capabilities_not_deployed), Toast.LENGTH_LONG).show());
        findViewById(R.id.btn_jump_into_archive_activity).setOnClickListener(v -> Toast.makeText(HomeActivity.this, getString(R.string.home_capabilities_not_deployed), Toast.LENGTH_LONG).show());
    }

    private void initData(Player player) {
        if (TextUtils.isEmpty(currentId)) {
            HMSLogHelper.getSingletonInstance().debug(TAG, "initData player is null ,please login  again");
            // 添加登录逻辑
            signIn();
        } else {
            setCircleImageView(photoUri);
            displayNameView.setText(player.getDisplayName());
            currentScore = UntilTool.getInfo(this, currentId);
            scoreTextView.setText(String.valueOf(currentScore));
        }

    }

    private void setCircleImageView(String url) {
        CircleImageView imageVie = findViewById(R.id.image_button_userInfo);
        if (!TextUtils.isEmpty(url)) {
            Glide.with(HomeActivity.this)
                    .load(url)
                    .placeholder(R.mipmap.game_photo_man)
                    .fitCenter()
                    .into(imageVie);
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

    /**
     * Log in ,and return the login information (or error message) of the Huawei account that has
     * logged in to this application. During this process, the authorization interface will not be
     * displayed to Huawei account users.
     * <p>
     * 登录，返回已登录此应用的华为帐号登录信息(或者错误信息)，在此过程中不会展现授权界面给华为帐号用户。
     */
    public void signIn() {
        // 一定要在init成功后，才可以调用登录接口
        // Be sure to call the login API after the init is successful
        Task<AuthAccount> authAccountTask = AccountAuthManager.getService(this, getScopeParams()).silentSignIn();
        authAccountTask
                .addOnSuccessListener(
                        authAccount -> {
                            HMSLogHelper.getSingletonInstance().debug(TAG, "signIn success");
                            photoUri = authAccount.getAvatarUriString();
                            getCurrentPlayer();
                        })
                .addOnFailureListener(
                        e -> {
                            if (e instanceof ApiException) {
                                ApiException apiException = (ApiException) e;
                                HMSLogHelper.getSingletonInstance().debug(TAG, "signIn failed:" + apiException.getStatusCode());
                                signInNewWay();
                            }
                        });
    }

    /**
     * Obtain the Intent of the Huawei account login authorization page, and open the Huawei account
     * login authorization page by calling startActivityForResult(Intent, int).
     * <p>
     * 获取到华为帐号登录授权页面的Intent，并通过调用startActivityForResult(Intent, int)打 开华为帐号登录授
     * 权页面。
     */
    public void signInNewWay() {
        Intent intent = AccountAuthManager.getService(HomeActivity.this, getScopeParams()).getSignInIntent();
        startActivityForResult(intent, SIGN_IN_INTENT);
    }

    public AccountAuthParams getScopeParams() {
        return new AccountAuthParamsHelper(AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME).createParams();
    }

    /**
     * Get the currently logged in player object and get player information from the ‘Player’ object.
     * <p>
     * 获取当前登录的玩家对象，从Player对象中获取玩家信息。
     */
    public void getCurrentPlayer() {
        PlayersClientImpl client = (PlayersClientImpl) Games.getPlayersClient(this);
        Task<Player> task = client.getCurrentPlayer();
        task.addOnSuccessListener(
                player -> {
                    String result = "display:" + player.getDisplayName();
                    if (!TextUtils.isEmpty(result)) {
                        HMSLogHelper.getSingletonInstance().debug(TAG, result);
                    }
                    checkSign(player);
                    initData(player);
                })
                .addOnFailureListener(
                        e -> {
                            if (e instanceof ApiException) {
                                String result = "rtnCode:" + ((ApiException) e).getStatusCode();
                                HMSLogHelper.getSingletonInstance().debug(TAG, result);
                                if (7400 == ((ApiException) e).getStatusCode() || 7018 == ((ApiException) e).getStatusCode()) {
                                    // 7400表示用户未签署联运协议，需要继续调用init接口
                                    // 7018表示初始化失败，需要继续调用init接口
                                    // error code 7400 indicates that the user has not agreed to the joint operations privacy agreement
                                    // error code 7018 indicates that the init API is not called.
                                    HMSLogHelper.getSingletonInstance().error(TAG, e.getMessage());
                                }
                            }
                        });
    }

    /**
     * 登录验签
     *
     * @param player 玩家对象
     */
    @SuppressWarnings("deprecation")
    private void checkSign(Player player) {
        if (player == null) {
            HMSLogHelper.getSingletonInstance().error(TAG, "Player obj is null, please signIn again!");
            return;
        }
        @SuppressLint("AllowAllHostnameVerifier") OkHttpClient client = new OkHttpClient().newBuilder()
                .hostnameVerifier(new AllowAllHostnameVerifier())
                .build();
        // 通过FormBody对象构建Builder来添加表单参数
        FormBody mFormBody = new FormBody.Builder()
                .add("method", "external.hms.gs.checkPlayerSign")
                .add("appId", "105174767")
                .add("cpId", "70086000159286487")
                .add("ts", player.getSignTs())
                .add("playerId", player.getPlayerId())
                .add("playerLevel", player.getLevel() + "")
                .add("playerSSign", player.getPlayerSign())
                .add("openId", player.getOpenId())
                .add("openIdSign", player.getOpenIdSign())
                .build();
        Request request = new Request.Builder()
                .url("https://jos-api.cloud.huawei.com/gameservice/api/gbClientApi")
                .post(mFormBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                HMSLogHelper.getSingletonInstance().error(TAG, e.getMessage());
                runOnUiThread(() -> Toast.makeText(HomeActivity.this, "check sign failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                HMSLogHelper.getSingletonInstance().debug(TAG, response.toString());
                runOnUiThread(() -> {
                    // 登录检验成功之后，进入游戏，把数据存入本地,设置全局的用户ID变量。
                    currentId = player.getPlayerId();
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (SIGN_IN_INTENT == requestCode) {

            if (null == data) {
                HMSLogHelper.getSingletonInstance().debug(TAG, "signIn intent is null");
                return;
            }
            String jsonSignInResult = data.getStringExtra("HUAWEIID_SIGNIN_RESULT");
            if (TextUtils.isEmpty(jsonSignInResult)) {
                HMSLogHelper.getSingletonInstance().debug(TAG, "SignIn result is empty");
                return;
            }
            try {
                AccountAuthResult signInResult = new AccountAuthResult().fromJson(jsonSignInResult);
                if (0 == signInResult.getStatus().getStatusCode()) {
                    HMSLogHelper.getSingletonInstance().debug(TAG, "Sign in success.");
                    photoUri = signInResult.getAccount().getAvatarUriString();
                    getCurrentPlayer();
                } else {

                    HMSLogHelper.getSingletonInstance().debug(TAG, "Sign in failed: " + signInResult.getStatus().getStatusCode());
                    Toast.makeText(HomeActivity.this, "Sign in failed: " + signInResult.getStatus().getStatusCode(), Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException var7) {
                HMSLogHelper.getSingletonInstance().debug(TAG, "Failed to convert json from signInResult.");
                Toast.makeText(HomeActivity.this, "Failed to convert json from signInResult.", Toast.LENGTH_SHORT).show();
            }
        } else {
            HMSLogHelper.getSingletonInstance().debug(TAG, "unknown requestCode in onActivityResult");
            Toast.makeText(HomeActivity.this, "unknown requestCode in onActivityResult", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_start_game_home) {
            if (!TextUtils.isEmpty(currentId)) {
                Intent intent = new Intent(this, GameActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelable(Constant.PLAYER_INFO_KEY, currentPlayer);
                bundle.putString(Constant.PLAYER_ICON_URI, photoUri);
                intent.putExtras(bundle);
                startActivity(intent);
            } else {
                // 重新登录
                signIn();
            }
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

    private void consumeGood(String productId, String purchaseToken) {
        // 注意：所有补单商品先发货再进行消耗
        updateSoreView(productId);
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

    @Override
    protected void onResume() {
        super.onResume();
        showFloatWindowNewWay();
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
                Toast.makeText(HomeActivity.this, getString(R.string.update_des) + status, Toast.LENGTH_LONG).show();
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