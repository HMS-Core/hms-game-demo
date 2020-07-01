
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

package com.huawei.hms.game.playerstats;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.R;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.game.common.BaseActivity;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.PlayersClient;
import com.huawei.hms.jos.games.playerstats.GamePlayerStatistics;
import com.huawei.hms.jos.games.playerstats.GamePlayerStatisticsClient;

import android.os.Bundle;
import android.widget.CheckBox;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class PlayerStatsActivity extends BaseActivity {

    /**
     * true : Retrieve data from the game server.
     * false : Retrieve data from the local cache. The local cache time is 5 minutes. If there is no
     * local cache or the cache times out, it is obtained from the game server.
     * *
     * true：表示从游戏服务器获取数据。
     * false：表示从本地缓存获取数据。本地缓存时间为5分钟，如果本地无缓存或缓存超时，则从游戏服务器获取。
     */
    private static boolean ISREALTIME = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_stats);
        ButterKnife.bind(this);
    }

    /**
     * Get statistics of current players, such as online duration, online ranking, etc. Support
     * obtaining from local cache or game server.
     * *
     * 获取当前玩家的统计信息，例如在线时长、在线名次等。支持从本地缓存或游戏服务器获取。
     */
    @OnClick(R.id.btn_get_player_stats)
    public void getCurrentPlayerStats() {
        initIsRealTime();
        GamePlayerStatisticsClient playerStatsClient = Games.getGamePlayerStatsClient(this);
        Task<GamePlayerStatistics> task = playerStatsClient.getGamePlayerStatistics(ISREALTIME);
        task.addOnSuccessListener(new OnSuccessListener<GamePlayerStatistics>() {
            @Override
            public void onSuccess(GamePlayerStatistics gamePlayerStatistics) {
                if (gamePlayerStatistics == null) {
                    showLog("playerStatsAnnotatedData is null, inner error");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("IsRealTime:" + ISREALTIME);
                sb.append("\n---AverageSessionLength: " + gamePlayerStatistics.getAverageOnLineMinutes() + "\n---");
                sb.append("DaysSinceLastPlayed: " + gamePlayerStatistics.getDaysFromLastGame() + "\n---");
                sb.append("NumberOfPurchases: " + gamePlayerStatistics.getPaymentTimes() + "\n---");
                sb.append("NumberOfSessions: " + gamePlayerStatistics.getOnlineTimes() + "\n---");
                sb.append("TotalPurchasesAmountRange: " + gamePlayerStatistics.getTotalPurchasesAmountRange());
                showLog(sb.toString());
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

    private void initIsRealTime() {
        CheckBox checkBox = findViewById(R.id.is_real_time_checkbox);
        ISREALTIME = checkBox.isChecked();
    }

    /**
     * Get the locally cached playerId of the currently logged in player.
     * *
     * 获取本地缓存的当前登录玩家的playerId。
     */
    @OnClick(R.id.btn_get_player_id)
    public void getPlayerId() {
        PlayersClient client = Games.getPlayersClient(this);
        Task<String> task = client.getCachePlayerId();
        task.addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String playerId) {
                showLog(playerId);
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