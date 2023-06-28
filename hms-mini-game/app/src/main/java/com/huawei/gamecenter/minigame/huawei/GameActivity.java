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

package com.huawei.gamecenter.minigame.huawei;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.huawei.gamecenter.minigame.huawei.UI.MyCustomView;
import com.huawei.gamecenter.minigame.huawei.Until.Constant;
import com.huawei.gamecenter.minigame.huawei.Until.HMSLogHelper;
import com.huawei.gamecenter.minigame.huawei.Until.UntilTool;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.player.Player;
import com.huawei.hms.jos.games.player.PlayersClientImpl;
import com.huawei.hms.support.account.AccountAuthManager;
import com.huawei.hms.support.account.request.AccountAuthParams;
import com.huawei.hms.support.account.request.AccountAuthParamsHelper;
import com.huawei.hms.support.account.result.AccountAuthResult;
import com.huawei.hms.support.account.result.AuthAccount;
import com.intermodaltransport.huawei.ExitApplication;
import com.intermodaltransport.huawei.activity.HomeActivity;
import com.intermodaltransport.huawei.activity.ShoppingActivity;

import org.json.JSONException;

import androidx.appcompat.app.AppCompatActivity;

import de.hdodenhof.circleimageview.CircleImageView;

public class GameActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MiniGame_Game_Act";
    private static final int SIGN_IN_INTENT = 3000;
    private static String currentId;
    private static String photoUri = null;
    private int currentLevel = 0;
    private long timeSecond = 0;
    private int currentScore = 0;

    private Player mPlayer;
    private MyCustomView gameDrawView;
    private TextView tvScore;
    private TextView tvTime;
    private TextView tvUserName;
    private CircleImageView gamePhoto;
    private TextView gameTopLevel;
    private Button gameOnStart;
    private Button gameOnPause;
    private AlertDialog alertDialog;
    private CountDownTimer countDownTimer;
    private static int REQ_SHOPPING = 11111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set this parameter to Untitled (delete the Android title bar). (The full-screen function is irrelevant to this function.)
        // 设置为无标题(去掉Android自带的标题栏)，(全屏功能与此无关)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Set to full screen mode
        // 设置为全屏模式
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.game_view);
        mPlayer = getIntent().getParcelableExtra(Constant.PLAYER_INFO_KEY);
        currentId = mPlayer.getPlayerId();
        photoUri = getIntent().getStringExtra(Constant.PLAYER_ICON_URI);
        init();
        ExitApplication.getInstance().addActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentScore = UntilTool.getInfo(this, mPlayer.getOpenId());
        initData(mPlayer);
    }

    private void initData(Player mPlayer) {
        if (TextUtils.isEmpty(currentId)) {
            signIn();
        } else {
            tvUserName.setText(mPlayer.getDisplayName());
            setCircleImageView(photoUri, gamePhoto);
            gameLevelSetting(currentLevel);
            gameScoreSetting(currentScore);
        }
    }

    /**
     * MyCustomView.BeatEnemyListener   自定义view界面回调接口
     */
    private void init() {
        tvScore = findViewById(R.id.game_top_score);
        tvTime = findViewById(R.id.game_top_time);
        gameDrawView = findViewById(R.id.mini_game_view);
        gameOnStart = findViewById(R.id.game_onStart);
        gameOnStart.setOnClickListener(this);
        gameOnPause = findViewById(R.id.game_onPause);
        gameOnPause.setOnClickListener(this);
        tvUserName = findViewById(R.id.game_top_username);
        gamePhoto = findViewById(R.id.game_top_avatar);
        gamePhoto.setImageResource(R.mipmap.game_photo_man);
        gameTopLevel = findViewById(R.id.game_top_level);
        gameLevelSetting(currentLevel);
        gameScoreSetting(currentScore);

        gameDrawView.setBeatEnemyListener(new MyCustomView.BeatEnemyListener() {
            @Override
            public void onBeatEnemy(int showMode) {
                currentScore = currentScore + 3;
                gameScoreSetting(currentScore);
            }

            @Override
            public void onFire() {
                if (currentScore <= 1) {
                    cancelTimeCount();
                    gameDrawView.gameSwitch(true);
                    showAlertDialog(Constant.MODE_ONE);
                }
                currentScore--;
                gameScoreSetting(currentScore);
            }

            @Override
            public void gameEnd(int i) {
                cancelTimeCount();
                showAlertDialog(Constant.M0DE_TWO);
            }
        });
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.game_onStart) {
            if (currentScore <= 0) {
                gameDrawView.gameSwitch(true);
                gameOnStart.setVisibility(View.VISIBLE);
                gameOnPause.setVisibility(View.INVISIBLE);
                cancelTimeCount();
                showAlertDialog(Constant.MODE_ONE);
            } else {
                gameDrawView.gameSwitch(false);
                gameOnStart.setVisibility(View.GONE);
                gameOnPause.setVisibility(View.VISIBLE);
                initTimeCount(timeSecond);
            }
        }
        if (v.getId() == R.id.game_onPause) {
            if (!MyCustomView.isRefresh) {
                gameDrawView.gameSwitch(true);
                gameOnStart.setVisibility(View.VISIBLE);
                gameOnPause.setVisibility(View.INVISIBLE);
                updateScoreAndLevel();
                UntilTool.addInfo(this, currentId, currentScore);
                cancelTimeCount();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameDrawView.gameSwitch(true);
        gameOnStart.setVisibility(View.VISIBLE);
        gameOnPause.setVisibility(View.INVISIBLE);
        cancelTimeCount();
        updateScoreAndLevel();
        UntilTool.addInfo(this, currentId, currentScore);
    }

    /**
     * @param s  Pop-up prompt, long prompt!
     **
     * @param s 弹窗提示，长提示语！
     */
    private void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    /**
     * @param clickMode  Click the event distribution flag
     **
     * @param clickMode 点击事件分发标识
     */
    private void showAlertDialog(int clickMode) {
        if (clickMode == Constant.MODE_THREE) {
            alertDialog = new AlertDialog.Builder(this)
                    .setView(R.layout.game_continue)
                    .setCancelable(false)
                    .create();
            alertDialog.show();
            alertDialog.findViewById(R.id.btn_click_continue).setOnClickListener(v -> {
                currentLevel = ++currentLevel;
                gameLevelSetting(currentLevel);
                gameDrawView.setEmSpd(Constant.MODE_THREE, currentLevel);
                gameDrawView.gameSwitch(false);
                updateScoreAndLevel();
                // Start the timer.
                // 开启定时器
                initTimeCount(timeSecond);
                alertDialog.dismiss();
            });
        }
        // Bonus points are used up. Are you sure you want to purchase bonus points?
        // 积分消耗完毕 是否购买积分
        if (clickMode == Constant.MODE_ONE) {
            alertDialog = new AlertDialog.Builder(this, R.style.simpleDialogStyle)
                    .setCancelable(true)
                    .create();
            LayoutInflater inflater = LayoutInflater.from(this);
            @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.game_score_use_up, null);
            alertDialog.show();
            alertDialog.getWindow().setContentView(v);
            alertDialog.findViewById(R.id.btn_scoreUseUp).setOnClickListener(v14 -> {
//                currentScore = 30 + currentScore;
//                showToast(getString(R.string.GameToast_successfulBuySore));
//                gameScoreSetting(currentScore);
//                gameDrawView.gameSwitch(false);
//                updateScoreAndLevel();
                  // Start the timer.
//                // 开启定时器
//                initTimeCount(timeSecond);
                alertDialog.dismiss();
                // Switch to the payment page.
                // 跳转开启支付界面
                Intent intent = new Intent(GameActivity.this, ShoppingActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelable(Constant.PLAYER_INFO_KEY, mPlayer);
                bundle.putString(Constant.PLAYER_ICON_URI, photoUri);
                intent.putExtras(bundle);
                startActivity(intent);
            });
        }
        if (clickMode == Constant.M0DE_TWO) {
            alertDialog = new AlertDialog.Builder(this, R.style.simpleDialogStyle)
                    .setCancelable(false)
                    .create();
            LayoutInflater inflater = LayoutInflater.from(this);
            @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.game_failed, null);
            alertDialog.show();
            alertDialog.getWindow().setContentView(v);
            alertDialog.findViewById(R.id.btn_startAgain).setOnClickListener(v1 -> {
                gameDrawView.gameSwitch(false);
                gameDrawView.setEmSpd(Constant.M0DE_TWO, currentLevel);
                // Start the timer.
                // 开启定时器
                initTimeCount(timeSecond);
                // Cache data, start over
                // 缓存数据，重新开始
                updateScoreAndLevel();
                alertDialog.dismiss();
            });
        }
        // Game clearance all, choose to restart or exit the game.
        // 游戏通关全部，选择重新开始还是  退出游戏
        if (clickMode == Constant.MODE_FOUR) {
            alertDialog = new AlertDialog.Builder(this, R.style.simpleDialogStyle)
                    .setCancelable(false)
                    .create();
            LayoutInflater inflater = LayoutInflater.from(this);
            @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.game_clearance, null);
            alertDialog.show();
            alertDialog.getWindow().setContentView(v);
            alertDialog.findViewById(R.id.game_Again).setOnClickListener(v12 -> {
                currentLevel = 0;
                updateScoreAndLevel();
                gameLevelSetting(currentLevel);
                gameDrawView.setEmSpd(Constant.MODE_THREE, currentLevel);
                gameDrawView.gameSwitch(false);
                // Start the timer.
                // 开启定时器
                initTimeCount(timeSecond);
                alertDialog.dismiss();
            });
            // Game Exit
            // 游戏退出
            alertDialog.findViewById(R.id.game_exit).setOnClickListener(v13 -> {
                // Return to the upper-layer interface. Close the interface first.
                // 返回上层界面,此处先关闭界面
                GameActivity.this.finish();
                alertDialog.dismiss();
            });
        }
    }

    private void initTimeCount(long time) {
        long timeReset;

        if (time > 0) {
            timeReset = time;
        } else {
            timeReset = 45;
        }
        countDownTimer = new CountDownTimer(1000 * timeReset, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeSecond = millisUntilFinished / 1000;
                HMSLogHelper.getSingletonInstance().debug(TAG, millisUntilFinished + "time : " + timeSecond);
                gameTimeSetting((int) timeSecond);
            }

            @Override
            public void onFinish() {
                HMSLogHelper.getSingletonInstance().debug(TAG, "time finish : " + timeSecond);
                timeSecond = 0;
                showAlertDialog(Constant.MODE_THREE);
                gameDrawView.gameSwitch(true);
            }
        }.start();
    }

    private void cancelTimeCount() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    /**
     * @param levelNumber    Game level setting display
     **
     * @param levelNumber 游戏关卡设置显示
     */
    private void gameLevelSetting(int levelNumber) {
        String format = String.format(getString(R.string.GameToast_levelSetting), levelNumber + 1);
        if (levelNumber >= 9) {
            showAlertDialog(Constant.MODE_FOUR);
        } else {
            gameTopLevel.setText(format);
        }
    }

    /**
     *  @param score    Game Bonus Point Change Settings
     *                 The storage logic can be added in the following method.
     **
     * @param score 游戏积分变化设置
     *              后续此处方法可增加存储逻辑
     */
    @SuppressLint("SetTextI18n")
    private void gameScoreSetting(int score) {
        tvScore.setText("         " + score);
    }

    /**
     * @param timeSecond   Game Remaining Time Settings
     **
     * @param timeSecond 游戏剩余时间设置
     */
    @SuppressLint("SetTextI18n")
    private void gameTimeSetting(int timeSecond) {
        tvTime.setText(timeSecond + " s");
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
        Task<AuthAccount> authAccountTask = AccountAuthManager.getService(this, getAuthScopeParams()).silentSignIn();
        authAccountTask
                .addOnSuccessListener(
                        authAccount -> {
                            getCurrentPlayer();
                            photoUri = authAccount.getAvatarUriString();
                        })
                .addOnFailureListener(
                        e -> {
                            if (e instanceof ApiException) {
                                signInNewWay();
                            }
                        });
    }

    @SuppressWarnings("deprecation")
    public void signInNewWay() {
        Intent intent = AccountAuthManager.getService(this, getAuthScopeParams()).getSignInIntent();
        startActivityForResult(intent, SIGN_IN_INTENT);
    }

    public AccountAuthParams getAuthScopeParams() {
        return new AccountAuthParamsHelper(AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME).createParams();
    }

    /**
     * Get the currently logged in player object and get player information from the ‘Player’ object.
     * 获取当前登录的玩家对象，从Player对象中获取玩家信息。
     */
    public void getCurrentPlayer() {
        PlayersClientImpl client = (PlayersClientImpl) Games.getPlayersClient(this);
        Task<Player> task = client.getCurrentPlayer();
        task.addOnSuccessListener(
                player -> {
                    currentId = player.getPlayerId();
                    initData(player);
                })
                .addOnFailureListener(
                        e -> {
                            if (e instanceof ApiException) {
                                String result = "rtnCode:" + ((ApiException) e).getStatusCode();
                                if (((ApiException) e).getStatusCode() == 7400 || ((ApiException) e).getStatusCode() == 7018) {
                                    // 7400表示用户未签署联运协议，需要继续调用init接口
                                    // 7018表示初始化失败，需要继续调用init接口
                                    // error code 7400 indicates that the user has not agreed to the joint operations privacy agreement
                                    // error code 7018 indicates that the init API is not called.
                                    HMSLogHelper.getSingletonInstance().debug(TAG, "getCurrentPlayer failed result is :" + result);
                                }
                            }
                        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SIGN_IN_INTENT) {
            if (null == data) {
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
                    HMSLogHelper.getSingletonInstance().debug(TAG, "Sign in result: " + signInResult.toJson());
                    photoUri = signInResult.getAccount().getAvatarUriString();
                    getCurrentPlayer();
                } else {

                    HMSLogHelper.getSingletonInstance().debug(TAG, "Sign in failed: " + signInResult.getStatus().getStatusCode());
                    Toast.makeText(this, "Sign in failed: " + signInResult.getStatus().getStatusCode(), Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException var7) {

                HMSLogHelper.getSingletonInstance().debug(TAG, "Failed to convert json from signInResult.");
                Toast.makeText(this, "Failed to convert json from signInResult.", Toast.LENGTH_SHORT).show();
            }
        } else {
            HMSLogHelper.getSingletonInstance().debug(TAG, "unknown requestCode in onActivityResult");
            Toast.makeText(this, "unknown requestCode in onActivityResult", Toast.LENGTH_SHORT).show();
        }
    }

    private void setCircleImageView(String url, ImageView imageView) {
        if (!TextUtils.isEmpty(url)) {
            Glide.with(GameActivity.this)
                    .load(url)
                    .placeholder(R.mipmap.game_photo_man)
                    .fitCenter()
                    .into(imageView);
        }
    }

    private void updateScoreAndLevel() {
        tvUserName.setText(mPlayer.getDisplayName());
        setCircleImageView(photoUri, gamePhoto);
        gameLevelSetting(currentLevel);
        gameScoreSetting(currentScore);
    }

    @Override
    protected void onStop() {
        super.onStop();
        UntilTool.addInfo(this, currentId, currentScore);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UntilTool.addInfo(this, currentId, currentScore);
    }
}