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

package com.intermodaltransport.huawei;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.huawei.gamecenter.minigame.huawei.R;
import com.huawei.gamecenter.minigame.huawei.UI.LoadingDialog;
import com.huawei.gamecenter.minigame.huawei.Until.Constant;
import com.huawei.gamecenter.minigame.huawei.Until.HMSLogHelper;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.jos.AppParams;
import com.huawei.hms.jos.AppUpdateClient;
import com.huawei.hms.jos.JosApps;
import com.huawei.hms.jos.JosAppsClient;
import com.huawei.hms.jos.JosStatusCodes;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.player.Player;
import com.huawei.hms.jos.games.player.PlayersClientImpl;
import com.huawei.hms.support.account.AccountAuthManager;
import com.huawei.hms.support.account.request.AccountAuthParams;
import com.huawei.hms.support.account.request.AccountAuthParamsHelper;
import com.huawei.hms.support.account.result.AccountAuthResult;
import com.huawei.hms.support.account.result.AuthAccount;
import com.huawei.hms.utils.ResourceLoaderUtil;
import com.huawei.updatesdk.service.appmgr.bean.ApkUpgradeInfo;
import com.huawei.updatesdk.service.otaupdate.CheckUpdateCallBack;
import com.huawei.updatesdk.service.otaupdate.UpdateKey;
import com.intermodaltransport.huawei.activity.HomeActivity;
import com.intermodaltransport.huawei.activity.ShoppingActivity;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;

import androidx.appcompat.app.AppCompatActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int SIGN_IN_INTENT = 3000;
    private boolean hasInit = false;
    private LoadingDialog dialog;
    private String photoUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.simple_activity_main);
        initView();
        init();
        ExitApplication.getInstance().addActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 初始化所有控件。
     */
    private void initView() {
        findViewById(R.id.login_in_huawei).setOnClickListener(new MyClickListener());
        findViewById(R.id.visitors_login).setOnClickListener(new MyClickListener());
        dialog = new LoadingDialog(MainActivity.this);
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
            checkUpdate();
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

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
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

    /**
     * Hide the displayed game buoy.
     * <p>
     * 隐藏已经显示的游戏浮标。
     */
    private void hideFloatWindowNewWay() {
        Games.getBuoyClient(this).hideFloatWindow();
    }

    public AccountAuthParams getAuthParams() {
        return new AccountAuthParamsHelper(AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME).createParams();
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
        Task<AuthAccount> authAccountTask = AccountAuthManager.getService(this, getAuthParams()).silentSignIn();
        authAccountTask
                .addOnSuccessListener(
                        authAccount -> {
                            HMSLogHelper.getSingletonInstance().debug(TAG, "signIn success");
                            getCurrentPlayer();
                            photoUri = authAccount.getAvatarUriString();
                        })
                .addOnFailureListener(
                        e -> {
                            if (e instanceof ApiException) {
                                ApiException apiException = (ApiException) e;
                                HMSLogHelper.getSingletonInstance().debug(TAG, "signIn failed:" + apiException.getStatusCode());
                                dismissDialog();
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
    @SuppressWarnings("deprecation")
    public void signInNewWay() {
        Intent intent = AccountAuthManager.getService(MainActivity.this, getAuthParams()).getSignInIntent();
        startActivityForResult(intent, SIGN_IN_INTENT);
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
                    HMSLogHelper.getSingletonInstance().debug(TAG, player.getDisplayName());
                    // 初始化  打开数据库
                    checkSign(player);
                })
                .addOnFailureListener(
                        e -> {
                            HMSLogHelper.getSingletonInstance().debug(TAG, "get current player failed");
                            if (e instanceof ApiException) {
                                String result = "rtnCode:" + ((ApiException) e).getStatusCode();
                                dismissDialog();
                                // 登录检验成功之后，进入游戏，把数据存入本地,设置全局的用户ID变量。
                                HMSLogHelper.getSingletonInstance().debug(TAG, result);
                                if (((ApiException) e).getStatusCode() == 7400 || ((ApiException) e).getStatusCode() == 7018) {
                                    // 7400表示用户未签署联运协议，需要继续调用init接口
                                    // 7018表示初始化失败，需要继续调用init接口
                                    // error code 7400 indicates that the user has not agreed to the joint operations privacy agreement
                                    // error code 7018 indicates that the init API is not called.
                                    init();
                                }
                            }
                        });
    }

    /**
     * 登录验签
     *
     * @param player 玩家对象
     */
    private void checkSign(Player player) {
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
                HMSLogHelper.getSingletonInstance().debug(TAG, e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "check sign failed", Toast.LENGTH_SHORT).show();
                    dismissDialog();
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                HMSLogHelper.getSingletonInstance().debug(TAG, response.toString());
                runOnUiThread(() -> {
                    dismissDialog();
                    // 登录检验成功之后，进入游戏，把数据存入本地,设置全局的用户ID变量。
                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(Constant.PLAYER_INFO_KEY, player);
                    bundle.putString(Constant.PLAYER_ICON_URI, photoUri);
                    intent.putExtras(bundle);
                    startActivity(intent);
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SIGN_IN_INTENT) {
            if (dialog != null && !dialog.isShowing()) {
                dialog.show();
            }
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
                if (signInResult.getStatus().getStatusCode() == 0) {
                    HMSLogHelper.getSingletonInstance().debug(TAG, "Sign in success.");
                    photoUri = signInResult.getAccount().getAvatarUriString();
                    getCurrentPlayer();
                } else {
                    dismissDialog();
                    HMSLogHelper.getSingletonInstance().debug(TAG, "Sign in failed: " + signInResult.getStatus().getStatusCode());
                    Toast.makeText(MainActivity.this, "Sign in failed: " + signInResult.getStatus().getStatusCode(), Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException var7) {
                dismissDialog();
                HMSLogHelper.getSingletonInstance().debug(TAG, "Failed to convert json from signInResult.");
                Toast.makeText(MainActivity.this, "Failed to convert json from signInResult.", Toast.LENGTH_SHORT).show();
            }
        } else {
            dismissDialog();
            HMSLogHelper.getSingletonInstance().debug(TAG, "unknown requestCode in onActivityResult");
            Toast.makeText(MainActivity.this, "unknown requestCode in onActivityResult", Toast.LENGTH_SHORT).show();
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

    private class UpdateCallBack implements CheckUpdateCallBack {
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
                Toast.makeText(MainActivity.this, getString(R.string.update_des) + status, Toast.LENGTH_LONG).show();
                // 强制更新应用时，弹出对话框后用户是否点击“退出应用”按钮
                boolean isExit = intent.getBooleanExtra(UpdateKey.MUST_UPDATE, false);
                HMSLogHelper.getSingletonInstance().debug(TAG, "rtnCode = " + rtnCode + "rtnMessage = " + rtnMessage);

                Serializable info = intent.getSerializableExtra(UpdateKey.INFO);
                // 如果info属于ApkUpgradeInfo类型，则拉起更新弹框
                if (info instanceof ApkUpgradeInfo) {
                    Context context = mContextWeakReference.get();
                    if (context != null) {
                        // showUpdateDialog接口中最后一个字段传入不同取值会带来不同的用户体验，具体请参考本文档的场景描述，此处以false为例
                        JosApps.getAppUpdateClient(context).showUpdateDialog(context,(ApkUpgradeInfo) info,false);
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

    /**
     * 实现所有页面控件点击事件
     */
    private class MyClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.login_in_huawei) {
                // 仅点击外部不可取消
                dialog.setCanceledOnTouchOutside(false);
                // 点击返回键和外部都不可取消
                dialog.setCancelable(false);
                dialog.show();
                signIn();
            }
        }
    }
}