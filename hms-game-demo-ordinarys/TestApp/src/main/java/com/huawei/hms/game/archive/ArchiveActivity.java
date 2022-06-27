
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

package com.huawei.hms.game.archive;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.R;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.game.common.BaseActivity;
import com.huawei.hms.game.common.SignInCenter;
import com.huawei.hms.jos.AntiAddictionCallback;
import com.huawei.hms.jos.AppParams;
import com.huawei.hms.jos.JosApps;
import com.huawei.hms.jos.JosAppsClient;
import com.huawei.hms.jos.JosStatusCodes;
import com.huawei.hms.jos.games.ArchivesClient;
import com.huawei.hms.jos.games.GameScopes;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.GamesStatusCodes;
import com.huawei.hms.jos.games.archive.ArchiveConstants;
import com.huawei.hms.jos.games.archive.ArchiveSummary;
import com.huawei.hms.support.account.AccountAuthManager;
import com.huawei.hms.support.account.request.AccountAuthParams;
import com.huawei.hms.support.account.request.AccountAuthParamsHelper;
import com.huawei.hms.support.account.result.AccountAuthResult;
import com.huawei.hms.support.account.result.AuthAccount;
import com.huawei.hms.support.api.entity.auth.Scope;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ArchiveActivity extends BaseActivity {
    ArchivesClient client;

    @BindView(R.id.cb_add)
    public CheckBox checkBoxAdd;

    @BindView(R.id.cb_delete)
    public CheckBox checkBoxDelete;

    @BindView(R.id.et_max_size)
    public EditText etMaxSize;

    private ArchivesClient getArchivesClient() {
        if (client == null) {
            client = Games.getArchiveClient(this);
        }
        return client;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);
        ButterKnife.bind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public AccountAuthParams getHuaweiIdParams() {
        List<Scope> scopes = new ArrayList<>();
        scopes.add(GameScopes.DRIVE_APP_DATA);
        return new AccountAuthParamsHelper(AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME).setScopeList(scopes)
            .createParams();
    }

    /**
     * Initialization of SDK，and log in.
     * *
     * SDK初始化并登录
     */
    @OnClick(R.id.btn_signin_drive)
    public void init() {
        AccountAuthParams params = AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME;
        JosAppsClient appsClient = JosApps.getJosAppsClient(this);
        Task<Void> initTask;
        initTask = appsClient.init(new AppParams(params, new AntiAddictionCallback() {
            @Override
            public void onExit() {
                //在此处实现游戏防沉迷功能，如保存游戏、调用帐号退出接口
            }
        }));

        initTask.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                showLog("init success");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    ApiException apiException = (ApiException) e;
                    int statusCode = apiException.getStatusCode();
                    //错误码为7401时表示用户未同意华为联运隐私协议
                    if (statusCode == JosStatusCodes.JOS_PRIVACY_PROTOCOL_REJECTED) {
                        showLog("has reject the protocol");
                        //在此处实现退出游戏或者重新调用初始化接口
                    }
                    //在此处实现其他错误码的处理
                }
            }
        });

        Task<AuthAccount> authAccountTask = AccountAuthManager.getService(this,getHuaweiIdParams()).silentSignIn();
        authAccountTask.addOnSuccessListener(new OnSuccessListener<AuthAccount>() {
            @Override
            public void onSuccess(AuthAccount authAccount) {
                showLog("signIn success");
                showLog("display:" + authAccount.getDisplayName());
                SignInCenter.get().updateAuthAccount(authAccount);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    ApiException apiException = (ApiException) e;
                    showLog("signIn failed:" + apiException.getStatusCode());
                    signInNewWay();
                }
            }
        });
    }

    private final static int SIGN_IN_INTENT = 3000;

    /**
     * Obtain the Intent of the Huawei account login authorization page, and open the Huawei account
     * login authorization page by calling startActivityForResult(Intent, int).
     * *
     * 获取到华为帐号登录授权页面的Intent，并通过调用startActivityForResult(Intent, int)打开华为帐号登录授
     * 权页面。
     */
    public void signInNewWay() {
        Intent intent = AccountAuthManager.getService(ArchiveActivity.this, getHuaweiIdParams()).getSignInIntent();
        startActivityForResult(intent, SIGN_IN_INTENT);
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
            } else {
                showLog("Sign in failed: " + signInResult.getStatus().getStatusCode());
            }
        } catch (JSONException var7) {
            showLog("Failed to convert json from signInResult.");
        }
    }

    /**
     * Guide to agree to drive protocol.
     * *
     * 引导同意云空间协议
     */
    @OnClick(R.id.btn_guide_drive_protocol)
    public void agreeDriveProtocol() {
        guideToAgreeDriveProtocol();
    }

    /**
     * Open the archive committing activity.
     * *
     * 跳转添加档案界面
     */
    @OnClick(R.id.btn_archive_add)
    public void addArchive() {
        Intent intent = new Intent(this, CommitArchiveActivity.class);
        startActivityForResult(intent, 1000);
    }

    /**
     * Get the maximum size of the cover file allowed by the server.
     * *
     * 获取服务器允许的封面文件的最大大小。
     */
    @OnClick(R.id.btn_archive_get_max_image_size)
    public void getMaxImageSize() {
        Task<Integer> task = getArchivesClient().getLimitThumbnailSize();
        task.addOnSuccessListener(new OnSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                show("MaxImageSize:" + integer);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                String result = "MaxImageSize rtnCode:" + ((ApiException) e).getStatusCode();
                showLog(result);
            }
        });
    }

    /**
     * Get the maximum size of the archive file allowed by the server.
     * *
     * 获取服务器允许的存档文件的最大大小。
     */
    @OnClick(R.id.btn_archive_get_max_content_size)
    public void getMaxFileSize() {
        Task<Integer> task = getArchivesClient().getLimitDetailsSize();
        task.addOnSuccessListener(new OnSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                show("MaxData:" + integer);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                String result = "MaxData rtnCode:" + ((ApiException) e).getStatusCode();
                showLog(result);
            }
        });
    }

    /**
     * Get all the archived metadata of the current player from the game server.
     * *
     * 从游戏服务器获取当前玩家的所有存档元数据。
     */
    @OnClick(R.id.btn_archive_load)
    public void loadArchive() {
        Intent intent = new Intent(this, ArchiveListActivity.class);
        intent.putExtra("isRealTime", true);
        startActivityForResult(intent, 1000);
    }

    /**
     * Get all the archive metadata of the current player from the local cache. The local cache
     * time is 5 minutes. If there is no local cache or the cache times out, it will be obtained
     * from the game server.
     * *
     * 从本地缓存获取当前玩家的所有存档元数据，本地缓存时间为5分钟，如果本地无缓存或缓存超时，则从游戏服务器获取。
     */
    @OnClick(R.id.btn_archive_load_cache)
    public void loadArchiveCache() {
        Intent intent = new Intent(this, ArchiveListActivity.class);
        intent.putExtra("isRealTime", false);
        startActivityForResult(intent, 1000);
    }

    private int getMaxSize() {
        try {
            return Integer.valueOf(etMaxSize.getText().toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Get the intent of the archive record selection page.
     * *
     * 获取指向存档记录选择页面的Intent.
     */
    @OnClick(R.id.btn_archive_get_select_intent)
    public void selectArchiveFromAppAssistant() {
        getArchiveIntent(getResources().getString(R.string.app_name), checkBoxAdd.isChecked(),
            checkBoxDelete.isChecked(), getMaxSize());
    }

    public void getArchiveIntent(String title, boolean allowAddBtn, boolean allowDeleteBtn, int maxArchive) {

        Task<Intent> task =
            getArchivesClient().getShowArchiveListIntent(title, allowAddBtn, allowDeleteBtn, maxArchive);
        task.addOnSuccessListener(new OnSuccessListener<Intent>() {
            @Override
            public void onSuccess(Intent intent) {
                if (intent == null) {
                    showLog("intent == null");
                } else {
                    try {
                        startActivityForResult(intent, 5000);
                    } catch (Exception e) {
                        showLog("Archive Activity is Invalid");
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    String result = "rtnCode:" + ((ApiException) e).getStatusCode();
                    showLog(result);
                    if (((ApiException) e).getStatusCode() == GamesStatusCodes.GAME_STATE_ARCHIVE_NO_DRIVE) {
                        guideToAgreeDriveProtocol();
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (SIGN_IN_INTENT == requestCode) {
                handleSignInResult(data);
            } else if (requestCode == 5000) {
                if (data == null) {
                    return;
                }

                if (data.hasExtra(ArchiveConstants.ARCHIVE_SELECT)) {
                    Bundle bundle = data.getParcelableExtra(ArchiveConstants.ARCHIVE_SELECT);
                    /*
                     * Get archive metadata from the Bundle object.
                     * *
                     * 从Bundle对象中获取存档元数据。
                     */
                    Task<ArchiveSummary> task = getArchivesClient().parseSummary(bundle);
                    task.addOnSuccessListener(new OnSuccessListener<ArchiveSummary>() {
                        @Override
                        public void onSuccess(ArchiveSummary archiveSummary) {
                            if (archiveSummary != null) {
                                showLog("UniqueName:" + archiveSummary.getFileName());
                                showLog("ArchiveId:" + archiveSummary.getId());
                                showLog("Description:" + archiveSummary.getDescInfo());
                                showLog("ModifyTime:" + archiveSummary.getRecentUpdateTime());
                                showLog("PlayedTime:" + archiveSummary.getActiveTime());
                                showLog("ImageAspectRatio:" + archiveSummary.getThumbnailRatio());
                                showLog("progressValue:" + archiveSummary.getThumbnailRatio());
                                showLog("hasThumbnail:" + archiveSummary.hasThumbnail());

                                showPlayerAndGame(archiveSummary);
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
                } else if (data.hasExtra(ArchiveConstants.ARCHIVE_ADD)) {
                    addArchive();
                }
            }
        }
    }

}
