
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

package com.huawei.hms.game.achievement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.R;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.jos.games.AchievementsClient;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.achievement.Achievement;
import com.huawei.hms.support.account.result.AuthAccount;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AchievementListActivity extends Activity implements AchievementListAdapter.OnBtnClickListener {

    @BindView(R.id.recycler_view)
    public RecyclerView recyclerView;

    private ArrayList<Achievement> achievements = new ArrayList<>();

    private AchievementsClient client;

    private AchievementListActivity mContext;

    AuthAccount authAccount = null;

    private boolean forceReload;

    private AchievementListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievement_list);
        mContext = this;
        ButterKnife.bind(mContext);
        initViews();
        initData();
        requestData();
    }

    private void initViews() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new AchievementListAdapter(mContext, achievements, mContext);
        recyclerView.setAdapter(adapter);
    }

    /**
     * Get a list of achievements on the server or the local client.
     * *
     * 获取服务器端或本地客户端的成就列表。
     */
    private void requestData() {
        Task<List<Achievement>> task = client.getAchievementList(forceReload);
        task.addOnSuccessListener(new OnSuccessListener<List<Achievement>>() {
            @Override
            public void onSuccess(List<Achievement> data) {
                if (data == null) {
                    showLog("achievementBuffer is null");
                    return;
                }
                Iterator<Achievement> iterator = data.iterator();
                achievements.clear();
                while (iterator.hasNext()) {
                    Achievement achievement = iterator.next();
                    achievements.add(achievement);
                }
                adapter.setData(achievements);
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

    private void initData() {
        Intent intent = getIntent();
        forceReload = intent.getBooleanExtra("forceReload", false);
        String mSignString = intent.getStringExtra("mSign");

        try {
            authAccount = authAccount.fromJson(mSignString);
        } catch (JSONException e) {
        }
        client = Games.getAchievementsClient(this);
    }

    @OnClick(R.id.iv_back)
    public void backHome() {
        finish();
    }

    private void showLog(String result) {
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
    }

    /**
     * Jump to the achievement details activity.
     * *
     * 跳转成就详情界面
     *
     * @param position Position
     */
    @Override
    public void onItemClick(int position) {
        Intent intent = new Intent(this, AchievementDetailActivity.class);
        Achievement achievement = achievements.get(position);
        intent.putExtra("achievementName", achievement.getDisplayName());
        intent.putExtra("achievementDes", achievement.getDescInfo());
        intent.putExtra("unlockedImageUri", achievement.getReachedThumbnailUri());
        intent.putExtra("rvealedImageUri", achievement.getVisualizedThumbnailUri());
        startActivity(intent);

    }

    /**
     * Unlock an achievement for the current player. This method needs to be called only
     * when the player completes the requirements specified by the achievement.
     * *
     * 为当前玩家解锁某个成就。只有当玩家完成成就指定的要求时才需要调用此方法。
     *
     * @param achievementId Achievement ID
     * @param isChecked Whether selected
     */
    @Override
    public void Unlock(String achievementId, boolean isChecked) {
        if (!isChecked) {
            client.reach(achievementId);
        } else {
            performUnlockImmediate(client, achievementId);
        }
    }

    /**
     * Reveal a hidden achievement of the game. This method needs to be called only when
     * the player enters the achievement preset scene. If this achievement is unlocked for the current
     * player, this method will not work.
     * *
     * 立即揭示游戏的某个隐藏的成就。只有当玩家进入到成就预设的场景时才需要调用此方法。如果此成就对于当前玩家已
     * 解锁，此方法将不起作用。
     *
     * @param achievementId Achievement ID
     * @param isChecked Whether selected
     */
    @Override
    public void reveal(String achievementId, boolean isChecked) {
        if (!isChecked) {
            client.visualize(achievementId);
        } else {
            performRevealImmediate(client, achievementId);
        }
    }

    /**
     * Increase the current step size of the current player for an achievement. For example, an
     * achievement needs 5 steps to complete. If the player completes from step 2 to step 3, you
     * need to call this method to increase the current step of the achievement by 1 step. The
     * achievement that needs to increase the step length must be a step achievement and has not been
     * unlocked. Once the player reaches the current maximum step size of the achievement, the
     * achievement will be automatically unlocked, and the request will be ignored when calling this
     * method to increase the step size.
     * *
     * 增加当前玩家对某个成就的当前步长，例如某个成就需5步完成，玩家从第2步完成到第3步，则需要调用此方法将
     * 成就的当前步长增加1步。需增加步长的成就必须是一个分步成就并且还未解锁。一旦玩家达到了成就当前的最大步长，
     * 此成就将自动解锁，再调用本方法增加步长时请求将被忽略。
     *
     * @param achievementId Achievement ID
     * @param isChecked Whether selected
     */
    @Override
    public void increment(String achievementId, boolean isChecked) {
        if (!isChecked) {
            client.grow(achievementId, 1);
        } else {
            performIncrementImmediate(client, achievementId, 1);
        }
    }

    /**
     * Set the step length of a certain achievement. For example, an achievement needs 5 steps to
     * complete. Players need to call this method to set the current step of the achievement to 3
     * from step 1 to step 3. Once the player reaches the maximum step size of the achievement, the
     * achievement will be automatically unlocked and the request will be ignored when this method is
     * called again.
     * *
     * 设置某个成就已完成的步长，例如某个成就需5步完成，玩家从第1步完成到第3步，则需要调用此方法将成就的当前
     * 步长设置为3。一旦玩家达到了成就的最大步长，此成就将自动解锁，再调用本方法时请求将被忽略。
     *
     * @param achievementId Achievement ID
     * @param isChecked Whether selected
     */
    @Override
    public void setStep(String achievementId, boolean isChecked) {
        if (!isChecked) {
            client.makeSteps(achievementId, 3);
        } else {
            performSetStepsImmediate(client, achievementId, 3);
        }
    }

    private void performSetStepsImmediate(AchievementsClient client, String achievementId, int stepsNum) {
        Task<Boolean> task = client.makeStepsWithResult(achievementId, stepsNum);
        task.addOnSuccessListener(new OnSuccessListener<Boolean>() {
            @Override
            public void onSuccess(Boolean isSucess) {
                if (isSucess) {
                    showLog("setAchievementSteps isSucess");
                } else {
                    showLog("achievement can not makeSteps");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    String result = "rtnCode:" + ((ApiException) e).getStatusCode();
                    showLog("step num is invalid" + result);
                }
            }
        });
    }

    private void performIncrementImmediate(AchievementsClient client, String achievementId, int stepsNum) {
        Task<Boolean> task = client.growWithResult(achievementId, stepsNum);

        task.addOnSuccessListener(new OnSuccessListener<Boolean>() {
            @Override
            public void onSuccess(Boolean isSucess) {
                if (isSucess) {
                    showLog("incrementAchievement isSucess");
                    requestData();
                } else {
                    showLog("achievement can not grow");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    String result = "rtnCode:" + ((ApiException) e).getStatusCode();
                    showLog("has bean already unlocked" + result);
                }
            }
        });
    }

    private void performRevealImmediate(AchievementsClient client, String achievementId) {
        Task<Void> task = client.visualizeWithResult(achievementId);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void v) {
                showLog("revealAchievemen isSucess");
                requestData();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    String result = "rtnCode:" + ((ApiException) e).getStatusCode();
                    showLog("achievement is not hidden" + result);
                }
            }
        });
    }

    private void performUnlockImmediate(AchievementsClient client, String achievementId) {
        Task<Void> task = client.reachWithResult(achievementId);

        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void v) {
                showLog("UnlockAchievemen isSucess");
                requestData();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    String result = "rtnCode:" + ((ApiException) e).getStatusCode();
                    showLog("achievement has been already unlocked" + result);
                }
            }
        });
    }
}
