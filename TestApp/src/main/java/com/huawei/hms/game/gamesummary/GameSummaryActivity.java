
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

package com.huawei.hms.game.gamesummary;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.R;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.game.common.BaseActivity;
import com.huawei.hms.jos.games.GameSummaryClient;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.gamesummary.GameSummary;

import android.os.Bundle;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class GameSummaryActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gamesummary);
        ButterKnife.bind(this);
    }

    /**
     * Get the current game information from the local cache asynchronously.
     * *
     * 以异步方式从本地缓存获取当前游戏信息。
     */
    @OnClick(R.id.btn_get_local_gamesummary)
    public void getLocalGameSummary() {
        GameSummaryClient client = Games.getGameSummaryClient(this);
        Task<GameSummary> task = client.getLocalGameSummary();
        task.addOnSuccessListener(new OnSuccessListener<GameSummary>() {
            @Override
            public void onSuccess(GameSummary gameSummary) {
                showLog("achievementCount:" + gameSummary.getAchievementCount());
                showLog("appId:" + gameSummary.getAppId());
                showLog("descInfo:" + gameSummary.getDescInfo());
                showLog("gameName:" + gameSummary.getGameName());
                showLog("gameHdImgUri:" + gameSummary.getGameHdImgUri());
                showLog("gameIconUri:" + gameSummary.getGameIconUri());
                showLog("rankingCount:" + gameSummary.getRankingCount());
                showLog("firstKind:" + gameSummary.getFirstKind());
                showLog("secondKind:" + gameSummary.getSecondKind());
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
     * Obtain the current game information from the game server asynchronously, and try to obtain it
     * from the local cache if the acquisition fails.
     * *
     * 以异步方式从游戏服务器获取当前游戏信息，获取失败时再尝试从本地缓存获取。
     */
    @OnClick(R.id.btn_get_gamesummary)
    public void getGameSummary() {
        GameSummaryClient client = Games.getGameSummaryClient(this);
        Task<GameSummary> task = client.getGameSummary();
        task.addOnSuccessListener(new OnSuccessListener<GameSummary>() {
            @Override
            public void onSuccess(GameSummary gameSummary) {
                showLog("achievementCount:" + gameSummary.getAchievementCount());
                showLog("appId:" + gameSummary.getAppId());
                showLog("descInfo:" + gameSummary.getDescInfo());
                showLog("gameName:" + gameSummary.getGameName());
                showLog("gameHdImgUri:" + gameSummary.getGameHdImgUri());
                showLog("gameIconUri:" + gameSummary.getGameIconUri());
                showLog("rankingCount:" + gameSummary.getRankingCount());
                showLog("firstKind:" + gameSummary.getFirstKind());
                showLog("secondKind:" + gameSummary.getSecondKind());
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
}
