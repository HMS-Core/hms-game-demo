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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.R;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.jos.AntiAddictionCallback;
import com.huawei.hms.jos.AppParams;
import com.huawei.hms.jos.AppUpdateClient;
import com.huawei.hms.jos.ExitCallback;
import com.huawei.hms.jos.JosApps;
import com.huawei.hms.jos.JosAppsClient;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.PlayersClient;
import com.huawei.hms.jos.games.player.Player;
import com.huawei.hms.support.account.AccountAuthManager;
import com.huawei.hms.support.account.request.AccountAuthParams;
import com.huawei.hms.support.account.request.AccountAuthParamsHelper;
import com.huawei.hms.support.account.result.AccountAuthResult;
import com.huawei.hms.support.account.result.AuthAccount;
import com.huawei.hms.utils.ResourceLoaderUtil;
import com.huawei.updatesdk.service.appmgr.bean.ApkUpgradeInfo;
import com.huawei.updatesdk.service.otaupdate.CheckUpdateCallBack;
import com.huawei.updatesdk.service.otaupdate.UpdateKey;

import org.json.JSONException;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.huawei.hms.game.ShowAgreementDialog.AGREE_BTN_CLICK;
import static com.huawei.hms.game.ShowAgreementDialog.AGREE_TEXT_CLICK;
import static com.huawei.hms.game.ShowAgreementDialog.NOT_AGREE_BTN_CLICK;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    static StringBuffer sbLog = new StringBuffer();
    private static final int SIGN_IN_INTENT = 3000;
    private Callback callback;

    private static final String AGREEMENT_SP_NAME = "agreement";
    private static final String AGREEMENT_SP_KEY = "agreement_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        callback = new Callback(this);
        // You are advised to add a selection button or highlight the redirection link to the protocol pop-up box of the game. The following is an example. Design the pop-up box based on the actual service situation.
        // 游戏自己的协议弹框，智慧屏协议弹框建议增加选中按钮或跳转链接的高亮提示等效果，这里做个示例，请根据实际业务情况设计
        showAgreementDialog();
    }

    // Protocol pop-up box of the game, which is only used as an example.
    // 游戏自己的协议弹框，这里仅作示例
    private void showAgreementDialog() {
        SharedPreferences sp = getSharedPreferences(AGREEMENT_SP_NAME, MODE_PRIVATE);
        // The agreement is not agreed. The agreement dialog box needs to be displayed.
        // 未同意协议需要弹出协议弹窗
        if (!sp.getBoolean(AGREEMENT_SP_KEY, false)) {
            ShowAgreementDialog dialog = new ShowAgreementDialog(this);
            dialog.show();
            dialog.setOnBtnClickListener(type -> {
                switch (type) {
                    case AGREE_BTN_CLICK:
                        // Initialization can be performed only after the agreement is approved.
                        // 同意协议才可以初始化
                        // init();
                        if(dialog != null) {
                            dialog.dismiss();
                        }
                        sp.edit().putBoolean(AGREEMENT_SP_KEY, true).apply();
                        break;
                    case NOT_AGREE_BTN_CLICK:
                        // Reject the agreement. You can exit the game.
                        // 拒绝协议，可以退出游戏处理
                        if(dialog != null) {
                            dialog.dismiss();
                        }
                        break;
                    case AGREE_TEXT_CLICK:
                        // Game's own agreement page, click to jump to the interface.
                        // 游戏自己的协议页面点击了跳转链接
                        startActivity(new Intent(this, WebViewActivity.class));
                        break;
                    default:
                        break;
                }
            });
        }
    }

    /**
     * Initialization of SDK, this method should be called while the main page of your application starting.
     * Then you can use functions of Game Setvice SDK.
     * *
     * SDK初始化，需要在应用首页启动时调用, 调用后才能正常使用SDK其他功能。
     */
    @OnClick(R.id.btn_init)
    public void init() {
        AccountAuthParams params = AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME;
        AppParams appParams = new AppParams(params, new AntiAddictionCallback() {
            @Override
            public void onExit() {
                // The callback will return in two situations:
                // 1. When a no-adult, real name user logs in to the game during the day, Huawei will pop up a box to remind the player that the game is not allowed. The player clicks "OK" and Huawei will return to the callback
                // 2. The no-adult, real name user logs in the game at the time allowed by the state. At 9 p.m., Huawei will pop up a box to remind the player that it is time. The player clicks "I know" and Huawei will return to the callback
                // You can realize the anti addiction function of the game here, such as saving the game, calling the account to exit the interface or directly the game process
                // 该回调会在如下两种情况下返回:
                // 1.未成年人实名帐号在白天登录游戏，华为会弹框提示玩家不允许游戏，玩家点击“确定”，华为返回回调
                // 2.未成年实名帐号在国家允许的时间登录游戏，到晚上9点，华为会弹框提示玩家已到时间，玩家点击“知道了”，华为返回回调
                // 您可在此处实现游戏防沉迷功能，如保存游戏、调用帐号退出接口或直接游戏进程退出(如System.exit(0))
                finish();
                System.exit(0);
            }
        });
        // This callback must be implemented for the VIP members of TV games. During the game, when the member expires or the number of online devices exceeds the limit, the player clicks OK in the Huawei dialog box to return to the game.
        // 智慧屏会员游戏需要实现该回调，该回调会在游戏过程中，会员到期或同时在线设备数超限时，玩家点击华为弹框的“确定”按钮后返回。
        appParams.setExitCallback(new ExitCallback() {
            @Override
            public void onExit(int retCode) {
                // You can realize the function of exiting the game, such as saving the game or directly the game process.
                // 您需要在此方法中实现退出游戏的功能，例如保存玩家进度、退出游戏等(如System.exit(0))。
                finish();
                System.exit(0);
            }
        });

        JosAppsClient appsClient = JosApps.getJosAppsClient(this);
        Task<Void> initTask;
        // Set the anti-addiction prompt context, this line must be added.
        // 设置防沉迷提示语的Context，此行必须添加。
        ResourceLoaderUtil.setmContext(this);
        initTask = appsClient.init(appParams);
        initTask.addOnSuccessListener(aVoid -> {
            showLog("init success");
            // The login interface can be invoked only after the init operation is successful.
            // 必须在init成功后，才可以实现登录功能
            // checkUpdate();
            // signIn();
        }).addOnFailureListener(
                e -> {
                    if (e instanceof ApiException) {
                        ApiException apiException = (ApiException) e;
                        int statusCode = apiException.getStatusCode();
                        Log.e(TAG, "init failed and rtnCode is " + statusCode);
                        // If init failed, you can realize the function of exiting the game
                        // init失败，您可在此处实现游戏退出(如System.exit(0))
                        System.exit(0);
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
        // Be sure to call the signIn API after the init is successful
        // 必须在init成功后，才可以继续调用华为帐号静默授权接口
        Task<AuthAccount> authAccountTask = AccountAuthManager.getService(this, getHuaweiIdParams()).silentSignIn();
        authAccountTask
                .addOnSuccessListener(
                        authAccount -> {
                            showLog("display:" + authAccount.getDisplayName());
                            checkVip();
                        })
                .addOnFailureListener(
                        e -> {
                            if (e instanceof ApiException) {
                                ApiException apiException = (ApiException) e;
                                showLog("signIn failed:" + apiException.getStatusCode());
                                showLog("start getSignInIntent");
                                // Call the signInIntent API here
                                // 在此处实现华为帐号显式授权
                                signInNewWay(getHuaweiIdParams());
                            }
                        });
    }

    /**
     * Game login
     * 游戏登录
     *
     * @return
     */
    private AccountAuthParams getHuaweiIdParams() {
        return new AccountAuthParamsHelper(AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME).createParams();
    }

    /**
     * Obtain the Intent of the Huawei account login authorization page, and open the Huawei account
     * login authorization page by calling startActivityForResult(Intent, int).
     * *
     * 获取到华为帐号登录授权页面的Intent，并通过调用startActivityForResult(Intent, int)打开华为帐号登录授
     * 权页面。
     *
     * @param params 帐号授权参数
     */
    private void signInNewWay(AccountAuthParams params) {
        Intent intent = AccountAuthManager.getService(MainActivity.this, params).getSignInIntent();
        startActivityForResult(intent, SIGN_IN_INTENT);
    }

    private void checkVip() {
        PlayersClient client = Games.getPlayersClient(MainActivity.this);
        client.vipCheck().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                showLog("checkVip success, canPlay");
                Log.d(TAG, "checkVip success, canPlay");
                getGamePlayer();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    String result = "checkVip rtnCode:" + ((ApiException) e).getStatusCode();
                    // You can exit the game directly.
                    // 此处您可以直接游戏进程退出(如System.exit(0))
                    showLog(result);
                    Log.d(TAG, result);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (SIGN_IN_INTENT == requestCode) {
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
                    checkVip();
                    // Verify the rights and interests of Huawei TV VIP members.
                    // 智慧屏会员权益校验
                } else {
                    // You can exit the game directly.
                    // 在此处退出游戏
                    showLog("Sign in failed: " + signInResult.getStatus().getStatusCode());
                    System.exit(0);
                }
            } catch (JSONException var7) {
                // You can exit the game directly.
                // 在此处退出游戏
                showLog("Failed to convert json from signInResult.");
                System.exit(0);
            }
        } else {
            // You can exit the game directly.
            // 在此处退出游戏
            showLog("unknown requestCode in onActivityResult");
            System.exit(0);
        }
    }

    /**
     * Get the currently logged in player object and get player information from the ‘Player’ object.
     * *
     * 获取当前登录的玩家对象，从Player对象中获取玩家信息。
     */
    private void getGamePlayer() {
        // Call the getPlayersClient method
        // 调用getPlayersClient方法初始化
        PlayersClient client = Games.getPlayersClient(this);
        // Game Login
        // 执行游戏登录
        Task<Player> task = client.getGamePlayer();
        task.addOnSuccessListener(new OnSuccessListener<Player>() {
            @Override
            public void onSuccess(Player player) {
                String accessToken = player.getAccessToken();
                String displayName = player.getDisplayName();
                String unionId = player.getUnionId();
                String openId = player.getOpenId();
                // Player information is obtained successfully. If there is a server, you are advised to refer tohttps://developer.huawei.com/consumer/cn/doc/development/HMSCore-References/gettokeninfo-0000001343252157 to verifie the player information. After the verification is successful, the player is allowed to enter the game.
                // 获取玩家信息成功，有服务器的建议您参考https://developer.huawei.com/consumer/cn/doc/development/HMSCore-References/gettokeninfo-0000001343252157在服务器侧校验玩家信息，校验通过后允许进入游戏
                showLog("getGamePlayer success");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    String result = "rtnCode:" + ((ApiException) e).getStatusCode();
                    // If the player information fails to be obtained, you can exit the game directly.
                    // 获取玩家信息失败，此处您可以直接游戏进程退出(如System.exit(0))
                    showLog("getGamePlayer failed；" + result);
                }
            }
        });
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
        private WeakReference<MainActivity> mContextWeakReference;

        public Callback(MainActivity activity) {
            mContextWeakReference = new WeakReference<>(activity);
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
                // Update status information
                // 更新状态信息
                int status = intent.getIntExtra(UpdateKey.STATUS, -99);
                Log.i(TAG, "check update status is:" + status);
                MainActivity activity = mContextWeakReference.get();
                if (activity != null) {
                    activity.showLog("check update status is:" + status);
                }
                // Return the error code.
                // 返回错误码
                int rtnCode = intent.getIntExtra(UpdateKey.FAIL_CODE, -99);
                // Return failure information.
                // 返回失败信息
                String rtnMessage = intent.getStringExtra(UpdateKey.FAIL_REASON);
                // 强制更新应用时，弹出对话框后用户是否点击“退出应用”按钮
                // Check whether the user clicks the “exit” button after the dialog box is displayed when the application is forcibly updated.
                boolean isExit = intent.getBooleanExtra(UpdateKey.MUST_UPDATE, false);
                Log.i(TAG, "rtnCode = " + rtnCode + "rtnMessage = " + rtnMessage);

                Serializable info = intent.getSerializableExtra(UpdateKey.INFO);
                // If the info type is ApkUpgradeInfo, the update dialog box is displayed.
                // 如果info属于ApkUpgradeInfo类型，则拉起更新弹框
                if (info instanceof ApkUpgradeInfo) {
                    // If the info type is ApkUpgradeInfo, the update dialog box is displayed.
                    // 如果info属于ApkUpgradeInfo类型，则拉起更新弹框
                    if (activity != null) {
                        JosApps.getAppUpdateClient(activity).showUpdateDialog(activity, (ApkUpgradeInfo) info, false);
                    }
                    Log.i(TAG, "check update success and there is a new update");
                }
                Log.i(TAG, "check update isExit=" + isExit);
                if (isExit) {
                    // Forcibly update the application. If the user chooses to exit the application in the displayed upgrade dialog box, the processing logic is controlled by you. The following is only an example.
                    // 是强制更新应用，用户在弹出的升级提示框中选择了“退出应用”，处理逻辑由您自行控制，这里只是个例子
                    System.exit(0);
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


    private void showLog(String logLine) {
        show(logLine);
    }

    protected void show(String logLine) {
        DateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:SS", Locale.ENGLISH);
        String time = format.format(new Date());

        sbLog.append(time).append(":").append(logLine);
        sbLog.append('\n');
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                final TextView vText = (TextView) findViewById(R.id.tv_log);
                vText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        vText.setText("");
                        sbLog = new StringBuffer();
                    }
                });
                vText.setText(sbLog.toString());
                View vScroll = findViewById(R.id.sv_log);
                if (vScroll instanceof ScrollView) {
                    ScrollView svLog = (ScrollView) vScroll;
                    svLog.fullScroll(View.FOCUS_DOWN);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sbLog = null;
    }
}
