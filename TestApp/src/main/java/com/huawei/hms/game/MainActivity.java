
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

import java.io.Serializable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.R;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.game.common.BaseActivity;
import com.huawei.hms.game.common.SignInCenter;
import com.huawei.hms.jos.AppUpdateClient;
import com.huawei.hms.jos.JosApps;
import com.huawei.hms.jos.JosAppsClient;
import com.huawei.hms.jos.games.AppPlayerInfo;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.PlayersClient;
import com.huawei.hms.jos.games.player.Player;
import com.huawei.hms.jos.games.player.PlayerExtraInfo;
import com.huawei.hms.jos.games.player.PlayersClientImpl;
import com.huawei.hms.support.hwid.HuaweiIdAuthManager;
import com.huawei.hms.support.hwid.result.AuthHuaweiId;
import com.huawei.hms.support.hwid.result.HuaweiIdAuthResult;
import com.huawei.updatesdk.service.appmgr.bean.ApkUpgradeInfo;
import com.huawei.updatesdk.service.otaupdate.CheckUpdateCallBack;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends BaseActivity {
    public static final String TAG = "MainActivity";

    private final static int SIGN_IN_INTENT = 3000;

    private final static int HEARTBEAT_TIME = 15 * 60 * 1000;

    private String playerId;

    private String sessionId = null;

    private boolean hasInit = false;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // you can call these two methods orderly to check update when your app starting up
        // checkUpdate();
        // checkUpdatePop();
    }

    @Override
    protected void onStop() {
        super.onStop();
        gameEnd();
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
        // front
        gameBegin();
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
        JosAppsClient appsClient = JosApps.getJosAppsClient(this);
        appsClient.init();
        showLog("init success");
        hasInit = true;
        /**
         * Games released in the Chinese mainland: The update API provided by Huawei must be called upon game launch.
         * Games released outside the Chinese mainland: It is optional for calling the update API provided by Huawei
         * upon
         * game launch.
         * *
         * 检测应用新版本，中国大陆发布的应用：应用启动时必须使用华为升级接口进行应用升级。
         * 中国大陆以外发布的应用：不强制要求。
         */
        checkUpdate();
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
        Task<AuthHuaweiId> authHuaweiIdTask = HuaweiIdAuthManager.getService(this, getHuaweiIdParams()).silentSignIn();
        authHuaweiIdTask.addOnSuccessListener(new OnSuccessListener<AuthHuaweiId>() {
            @Override
            public void onSuccess(AuthHuaweiId authHuaweiId) {
                showLog("signIn success");
                showLog("display:" + authHuaweiId.getDisplayName());
                SignInCenter.get().updateAuthHuaweiId(authHuaweiId);
                getCurrentPlayer();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    ApiException apiException = (ApiException) e;
                    showLog("signIn failed:" + apiException.getStatusCode());
                    showLog("start getSignInIntent");
                    signInNewWay();
                }
            }
        });
    }

    /**
     * Obtain the Intent of the Huawei account login authorization page, and open the Huawei account
     * login authorization page by calling startActivityForResult(Intent, int).
     * *
     * 获取到华为帐号登录授权页面的Intent，并通过调用startActivityForResult(Intent, int)打开华为帐号登录授
     * 权页面。
     */
    public void signInNewWay() {
        Intent intent = HuaweiIdAuthManager.getService(MainActivity.this, getHuaweiIdParams()).getSignInIntent();
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
            showLog("signIn inetnt is null");
            return;
        }
        // HuaweiIdSignIn.getSignedInAccountFromIntent(data);
        String jsonSignInResult = data.getStringExtra("HUAWEIID_SIGNIN_RESULT");
        if (TextUtils.isEmpty(jsonSignInResult)) {
            showLog("SignIn result is empty");
            return;
        }
        try {
            HuaweiIdAuthResult signInResult = new HuaweiIdAuthResult().fromJson(jsonSignInResult);
            if (0 == signInResult.getStatus().getStatusCode()) {
                showLog("Sign in success.");
                showLog("Sign in result: " + signInResult.toJson());
                SignInCenter.get().updateAuthHuaweiId(signInResult.getHuaweiId());
                getCurrentPlayer();
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
    @OnClick(R.id.btn_get_player)
    public void getCurrentPlayer() {
        PlayersClientImpl client = (PlayersClientImpl) Games.getPlayersClient(this);

        Task<Player> task = client.getCurrentPlayer();
        task.addOnSuccessListener(new OnSuccessListener<Player>() {
            @Override
            public void onSuccess(Player player) {
                String result = "display:" + player.getDisplayName() + "\n" + "playerId:" + player.getPlayerId() + "\n"
                        + "playerLevel:" + player.getLevel() + "\n" + "timestamp:" + player.getSignTs() + "\n"
                        + "playerSign:" + player.getPlayerSign();
                showLog(result);
                playerId = player.getPlayerId();
                gameBegin();
                handler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        gamePlayExtra();
                    }
                };
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Message message = new Message();
                        handler.sendMessage(message);
                    }
                }, HEARTBEAT_TIME, HEARTBEAT_TIME);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    String result = "rtnCode:" + ((ApiException) e).getStatusCode();
                    showLog(result);
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
        if (TextUtils.isEmpty(playerId)) {
            showLog("GetCurrentPlayer first.");
            return;
        }
        PlayersClient client = Games.getPlayersClient(this);
        AppPlayerInfo appPlayerInfo = new AppPlayerInfo();
        appPlayerInfo.area = "20";
        appPlayerInfo.rank = "level 56";
        appPlayerInfo.role = "hunter";
        appPlayerInfo.sociaty = "Red Cliff II";
        appPlayerInfo.playerId = playerId;
        Task<Void> task = client.savePlayerInfo(appPlayerInfo);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void v) {
                showLog("save player info successfully ");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    String result = "rtnCode:" + ((ApiException) e).getStatusCode();
                    showLog(result);
                }
            }
        });
    }

    /**
     * Enter the game,report the player's behavior when entering the game.
     * *
     * 进入游戏，上报玩家进入游戏时的行为事件。
     */
    @OnClick(R.id.btn_game_begin)
    public void gameBegin() {
        if (TextUtils.isEmpty(playerId)) {
            showLog("GetCurrentPlayer first.");
            return;
        }
        String uid = UUID.randomUUID().toString();
        PlayersClient client = Games.getPlayersClient(this);
        Task<String> task = client.submitPlayerEvent(playerId, uid, "GAMEBEGIN");
        task.addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String jsonRequest) {
                if (jsonRequest == null) {
                    showLog("jsonRequest is null");
                    return;
                }
                try {
                    JSONObject data = new JSONObject(jsonRequest);
                    sessionId = data.getString("transactionId");
                } catch (JSONException e) {
                    showLog("parse jsonArray meet json exception");
                    return;
                }
                showLog("submitPlayerEvent traceId: " + jsonRequest);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    String result = "rtnCode:" + ((ApiException) e).getStatusCode();
                    showLog(result);
                }
            }
        });
    }

    /**
     * Quit the game, report the behavior event when the player quit the game.
     * *
     * 退出游戏，上报玩家退出游戏时的行为事件。
     */
    @OnClick(R.id.btn_game_end)
    public void gameEnd() {
        if (TextUtils.isEmpty(playerId)) {
            showLog("GetCurrentPlayer first.");
            return;
        }
        if (TextUtils.isEmpty(sessionId)) {
            showLog("SessionId is empty.");
            return;
        }
        PlayersClient client = Games.getPlayersClient(this);
        Task<String> task = client.submitPlayerEvent(playerId, sessionId, "GAMEEND");
        task.addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                showLog("submitPlayerEvent traceId: " + s);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    String result = "rtnCode:" + ((ApiException) e).getStatusCode();
                    showLog(result);
                }
            }
        });
    }

    /**
     * Get additional player information.
     * *
     * 获取玩家附加信息。
     */
    @OnClick(R.id.btn_play_extra)
    public void gamePlayExtra() {
        if (TextUtils.isEmpty(playerId)) {
            showLog("GetCurrentPlayer first.");
            return;
        }
        PlayersClient client = Games.getPlayersClient(this);
        Task<PlayerExtraInfo> task = client.getPlayerExtraInfo(sessionId);
        task.addOnSuccessListener(new OnSuccessListener<PlayerExtraInfo>() {
            @Override
            public void onSuccess(PlayerExtraInfo extra) {
                if (extra != null) {
                    showLog("IsRealName: " + extra.getIsRealName() + ", IsAdult: " + extra.getIsAdult() + ", PlayerId: "
                        + extra.getPlayerId() + ", PlayerDuration: " + extra.getPlayerDuration());
                } else {
                    showLog("Player extra info is empty.");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    String result = "rtnCode:" + ((ApiException) e).getStatusCode();
                    showLog(result);
                }
            }
        });
    }

    /**
     * Show the game buoy.
     * *
     * 显示游戏浮标。
     */
    private void showFloatWindowNewWay() {
        if (!hasInit) {
            init();
        }
        Games.getBuoyClient(this).showFloatWindow();
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
        client.checkAppUpdate(this, new UpdateCallBack(this));
    }

    private static class UpdateCallBack implements CheckUpdateCallBack {
        private MainActivity apiActivity;

        private UpdateCallBack(MainActivity apiActivity) {
            this.apiActivity = apiActivity;
        }

        /**
         * Get update info from appmarket
         * *
         * 从应用市场获取的更新状态信息
         *
         * @param intent see detail:
         *        https://developer.huawei.com/consumer/cn/doc/development/HMS-References/appupdateclient#intent
         */
        @Override
        public void onUpdateInfo(Intent intent) {
            if (intent != null) {
                Serializable info = intent.getSerializableExtra("updatesdk_update_info");
                if (info instanceof ApkUpgradeInfo) {
                    apiActivity.showLog("check update success");
                    AppUpdateClient client = JosApps.getAppUpdateClient(apiActivity);
                    /**
                     * show update dialog
                     * *
                     * 弹出升级提示框
                     */
                    client.showUpdateDialog(apiActivity, (ApkUpgradeInfo) info, false);
                } else {
                    apiActivity.showLog("check update failed");
                }
            }
        }

        // ignored
        // 预留, 无需处理
        @Override
        public void onMarketInstallInfo(Intent intent) {
            Log.w("AppUpdateManager", "info not instanceof ApkUpgradeInfo");
            apiActivity.showLog("check update failed");
        }

        // ignored
        // 预留, 无需处理
        @Override
        public void onMarketStoreError(int responseCode) {
            apiActivity.showLog("check update failed");
        }

        // ignored
        // 预留, 无需处理
        @Override
        public void onUpdateStoreError(int responseCode) {
            apiActivity.showLog("check update failed");
        }
    }
}
