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

package com.huawei.hms.game;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.huawei.hmf.tasks.Task;
import com.huawei.hms.R;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.game.common.BaseActivity;
import com.huawei.hms.game.common.SignInCenter;
import com.huawei.hms.jos.AppParams;
import com.huawei.hms.jos.AppUpdateClient;
import com.huawei.hms.jos.JosApps;
import com.huawei.hms.jos.JosAppsClient;
import com.huawei.hms.jos.JosStatusCodes;
import com.huawei.hms.jos.games.AppPlayerInfo;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.GamesStatusCodes;
import com.huawei.hms.jos.games.PlayersClient;
import com.huawei.hms.jos.games.player.Player;
import com.huawei.hms.jos.games.player.PlayersClientImpl;
import com.huawei.hms.support.account.AccountAuthManager;
import com.huawei.hms.support.account.request.AccountAuthParams;
import com.huawei.hms.support.account.result.AccountAuthResult;
import com.huawei.hms.support.account.result.AuthAccount;
import com.huawei.hms.utils.ResourceLoaderUtil;
import com.huawei.updatesdk.service.appmgr.bean.ApkUpgradeInfo;
import com.huawei.updatesdk.service.otaupdate.CheckUpdateCallBack;
import com.huawei.updatesdk.service.otaupdate.UpdateKey;

import butterknife.ButterKnife;
import butterknife.OnClick;

import org.json.JSONException;

import java.io.Serializable;
import java.lang.ref.WeakReference;

public class MainActivity extends BaseActivity {
    public static final String TAG = "MainActivity";

    private static final int SIGN_IN_INTENT = 3000;

    private String openId;

    private boolean hasInit = false;

    private Callback callback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        callback = new Callback(this);
        init();

        // you can call these method orderly to check update when your app starting up
        // checkUpdate();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideFloatWindowNewWay();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        showFloatWindowNewWay();
        Log.e(TAG, "onResume");
    }

    /**
     * Initialization of SDK, this method should be called while the main page of your application starting.
     * Then you can use functions of Game Setvice SDK and notice will show(if there is a notice).
     * *
     * SDK初始化，需要在应用首页启动时调用, 调用后才能正常使用SDK其他功能和展示公告。
     */
    @OnClick(R.id.btn_init)
    public void init() {
        AccountAuthParams params = AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME;
        JosAppsClient appsClient = JosApps.getJosAppsClient(this);
        Task<Void> initTask;
        // Set the anti-addiction prompt context, this line must be added
        // 设置防沉迷提示语的Context，此行必须添加
        ResourceLoaderUtil.setmContext(this);
        initTask = appsClient.init(
                new AppParams(params, () -> {
                    // System.exit(0);
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
            showLog("init success");
            // Games released in the Chinese mainland: The update API provided by Huawei must be called upon game launch.
            // Games released outside the Chinese mainland: It is optional for calling the update API provided by Huawei upon game launch.
            // 检测应用新版本，中国大陆发布的应用：应用启动时必须使用华为升级接口进行应用升级。
            // 中国大陆以外发布的应用：不强制要求。
            checkUpdate();
            hasInit = true;
            // Make sure that the interface of showFloatWindow() is successfully called once after the game has been initialized successfully
            // 游戏初始化成功后务必成功调用过一次浮标显示接口
            showFloatWindowNewWay();
            // 一定要在init成功后，才可以调用登录接口
            // signIn();
        }).addOnFailureListener(
                e -> {
                    if (e instanceof ApiException) {
                        ApiException apiException = (ApiException) e;
                        int statusCode = apiException.getStatusCode();
                        if (statusCode == JosStatusCodes.JOS_PRIVACY_PROTOCOL_REJECTED) {
                            // Error code 7401 indicates that the user did not agree to Huawei joint operations privacy agreement
                            // 错误码为7401时表示用户未同意华为联运隐私协议
                            showLog("has reject the protocol");
                            // You need to prohibit players from entering the game here.
                            // 此处您需禁止玩家进入游戏
                        } else if (statusCode == GamesStatusCodes.GAME_STATE_NETWORK_ERROR) {
                            // Error code 7002 indicates network error
                            // 错误码7002表示网络异常
                            showLog("network error");
                            // 此处您可提示玩家检查网络，请不要重复调用init接口，否则断网情况下可能会造成手机高耗电。
                            // You can ask the player to check the network. Do not invoke the init interface repeatedly. Otherwise, the phone may consume a lot of power if the network is disconnected.
                        } else if (statusCode == 907135003) {
                            // 907135003表示玩家取消HMS Core升级或组件升级
                            // 907135003 indicates that user rejected the installation or upgrade of HMS Core.
                            showLog("init statusCode=" + statusCode);
                            init();
                        } else {
                            // Handle other error codes
                            // 在此处实现其他错误码的处理
                        }
                    }
                });
    }

    /**
     * Log in ,and return the login information (or error message) of the Huawei account that has
     * logged in to this application. During this process, the authorization interface will not be
     * displayed to Huawei account users.
     * *
     * 登录，返回已登录此应用的华为帐号登录信息(或者错误信息)，在此过程中不会展现授权界面给华为帐号用户。
     */
    @OnClick(R.id.btn_sign_in)
    public void signIn() {
        showLog("begin login and current hasInit=" + hasInit);
        // 一定要在init成功后，才可以调用登录接口
        // Be sure to call the login API after the init is successful
        Task<AuthAccount> authAccountTask = AccountAuthManager.getService(this, getHuaweiIdParams()).silentSignIn();
        authAccountTask
                .addOnSuccessListener(
                        authAccount -> {
                            showLog("signIn success");
                            showLog("display:" + authAccount.getDisplayName());
                            SignInCenter.get().updateAuthAccount(authAccount);
                            getGamePlayer();
                        })
                .addOnFailureListener(
                        e -> {
                            if (e instanceof ApiException) {
                                ApiException apiException = (ApiException) e;
                                showLog("signIn failed:" + apiException.getStatusCode());
                                showLog("start getSignInIntent");
                                signInNewWay(getHuaweiIdParams());
                            }
                        });
    }

    /**
     * Get the currently logged in player object and get player information from the ‘Player’ object.
     * *
     * 快捷登录
     */
    @OnClick(R.id.btn_unauthorized_login)
    public void unauthorizedLogin() {
        // 必须在init成功后，才可以实现登录功能
        Task<AuthAccount> authAccountTask = AccountAuthManager.getService(this, getUnauthorizedAccountAuthParams()).silentSignIn();
        authAccountTask.addOnSuccessListener(
                authAccount -> {
                    showLog("signIn success");
                    getGamePlayer();   // 获取玩家信息
                })
                .addOnFailureListener(
                        e -> {
                            if (e instanceof ApiException) {
                                ApiException apiException = (ApiException) e;
                                showLog("signIn failed:" + apiException.getStatusCode());
                                showLog("start getSignInIntent");
                                // 在此处实现显式登录
                                signInNewWay(getUnauthorizedAccountAuthParams());
                            }
                        });
    }


    /**
     * Obtain the Intent of the Huawei account login authorization page, and open the Huawei account
     * login authorization page by calling startActivityForResult(Intent, int).
     * *
     * 获取到华为帐号登录授权页面的Intent，并通过调用startActivityForResult(Intent, int)打开华为帐号登录授
     * 权页面。
     *
     * @param params 账号授权参数
     */
    public void signInNewWay(AccountAuthParams params) {
        Intent intent = AccountAuthManager.getService(MainActivity.this, params).getSignInIntent();
        startActivityForResult(intent, SIGN_IN_INTENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (SIGN_IN_INTENT == requestCode) {
            handleSignInResult(data);
        } else {
            showLog("unknown requestCode in onActivityResult");
        }
    }

    /**
     * Login authorization result response processing method.
     * *
     * 登录授权的结果响应处理方法
     *
     * @param data Data
     */
    private void handleSignInResult(Intent data) {
        if (null == data) {
            showLog("signIn intent is null");
            return;
        }
        String jsonSignInResult = data.getStringExtra("HUAWEIID_SIGNIN_RESULT");
        if (TextUtils.isEmpty(jsonSignInResult)) {
            showLog("SignIn result is empty");
            return;
        }
        try {
            AccountAuthResult signInResult = new AccountAuthResult().fromJson(jsonSignInResult);
            if (0 == signInResult.getStatus().getStatusCode()) {
                showLog("Sign in success.");
                showLog("Sign in result: " + signInResult.toJson());
                SignInCenter.get().updateAuthAccount(signInResult.getAccount());
                getGamePlayer();
            } else {
                showLog("Sign in failed: " + signInResult.getStatus().getStatusCode());
            }
        } catch (JSONException var7) {
            showLog("Failed to convert json from signInResult.");
        }
    }

    /**
     * Get the currently logged in player object and get player information from the ‘Player’ object.
     * *
     * 获取当前登录的玩家对象，从Player对象中获取玩家信息。
     */
    public void getGamePlayer() {
        PlayersClientImpl client = (PlayersClientImpl) Games.getPlayersClient(this);
        Task<Player> task = client.getGamePlayer();
        task.addOnSuccessListener(
                player -> openId = player.getOpenId()
        ).addOnFailureListener(e -> {
            if (e instanceof ApiException) {
                String result = "rtnCode:" + ((ApiException) e).getStatusCode();
                showLog(result);
                if (7400 == ((ApiException) e).getStatusCode() || 7018 == ((ApiException) e).getStatusCode()) {
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
     * Save user's game character information to Huawei game server, such as district server, level,
     * character, etc.
     * *
     * 保存用户的游戏角色信息到华为游戏服务器，如区服、等级、角色等。
     */
    @OnClick(R.id.btn_save_player)
    public void savePlayerInfo() {
        if (TextUtils.isEmpty(openId)) {
            showLog("GetCurrentPlayer first.");
            return;
        }
        PlayersClient client = Games.getPlayersClient(this);
        AppPlayerInfo appPlayerInfo = new AppPlayerInfo();
        appPlayerInfo.area = "20";
        appPlayerInfo.rank = "level 56";
        appPlayerInfo.role = "hunter";
        appPlayerInfo.sociaty = "Red Cliff II";
        appPlayerInfo.openId = openId;
        Task<Void> task = client.savePlayerInfo(appPlayerInfo);
        task.addOnSuccessListener(
                v -> showLog("save player info successfully "))
                .addOnFailureListener(
                        e -> {
                            if (e instanceof ApiException) {
                                String result = "rtnCode:" + ((ApiException) e).getStatusCode();
                                showLog(result);
                            }
                        });
    }

    /**
     * Show the game buoy.
     * *
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
     * *
     * 隐藏已经显示的游戏浮标。
     */
    private void hideFloatWindowNewWay() {
        Games.getBuoyClient(this).hideFloatWindow();
    }

    /**
     * Games released in the Chinese mainland: The update API provided by Huawei must be called upon game launch.
     * Games released outside the Chinese mainland: It is optional for calling the update API provided by Huawei upon
     * game launch.
     * *
     * 检测应用新版本，中国大陆发布的应用：应用启动时必须使用华为升级接口进行应用升级。
     * 中国大陆以外发布的应用：不强制要求。
     */
    @OnClick(R.id.btn_check_update)
    public void checkUpdate() {
        AppUpdateClient client = JosApps.getAppUpdateClient(this);
        client.checkAppUpdate(this, callback);
    }

    private static class Callback implements CheckUpdateCallBack {
        private WeakReference<Context> mContextWeakReference;

        public Callback(Context context) {
            mContextWeakReference = new WeakReference<>(context);
        }

        /**
         * Get update info from appmarket
         * *
         * 从应用市场获取的更新状态信息
         *
         * @param intent see detail:
         *               https://developer.huawei.com/consumer/cn/doc/development/HMS-References/appupdateclient#intent
         */
        @Override
        public void onUpdateInfo(Intent intent) {
            if (intent != null) {
                // 更新状态信息
                int status = intent.getIntExtra(UpdateKey.STATUS, -99);
                Log.i(TAG, "check update status is:" + status);
                // 返回错误码
                int rtnCode = intent.getIntExtra(UpdateKey.FAIL_CODE, -99);
                // 返回失败信息
                String rtnMessage = intent.getStringExtra(UpdateKey.FAIL_REASON);
                // 强制更新应用时，弹出对话框后用户是否点击“退出应用”按钮
                // Check whether the user clicks the “exit” button after the dialog box is displayed when the application is forcibly updated.
                boolean isExit = intent.getBooleanExtra(UpdateKey.MUST_UPDATE, false);
                Log.i(TAG, "rtnCode = " + rtnCode + "rtnMessage = " + rtnMessage);

                Serializable info = intent.getSerializableExtra(UpdateKey.INFO);
                // 如果info属于ApkUpgradeInfo类型，则拉起更新弹框
                if (info instanceof ApkUpgradeInfo) {
                    // 如果info属于ApkUpgradeInfo类型，则拉起更新弹框
                    Context context = mContextWeakReference.get();
                    if (context != null) {
                        JosApps.getAppUpdateClient(context).showUpdateDialog(context, (ApkUpgradeInfo) info, false);
                    }
                    Log.i(TAG, "check update success and there is a new update");
                }
                Log.i(TAG, "check update isExit=" + isExit);
                if (isExit) {
                    // 是强制更新应用，用户在弹出的升级提示框中选择了“退出应用”，处理逻辑由您自行控制，这里只是个例子
                    // System.exit(0);
                }
            }
        }

        // ignored
        // 预留, 无需处理
        @Override
        public void onMarketInstallInfo(Intent intent) {
        }

        // ignored
        // 预留, 无需处理
        @Override
        public void onMarketStoreError(int responseCode) {
        }

        // ignored
        // 预留, 无需处理
        @Override
        public void onUpdateStoreError(int responseCode) {
        }
    }
}
