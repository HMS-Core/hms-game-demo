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

package com.huawei.hms.game.player;

import android.os.Bundle;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.R;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.framework.common.NetworkUtil;
import com.huawei.hms.game.common.BaseActivity;
import com.huawei.hms.jos.games.Games;
import com.huawei.hms.jos.games.PlayersClient;
import com.huawei.hms.jos.games.player.Player;
import com.huawei.hms.jos.games.player.PlayerExtraInfo;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class PlayerActivity extends BaseActivity {
    private String sessionId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        ButterKnife.bind(this);
    }

    /**
     * Obtains the currently logged-in player object.
     * *
     * 获取当前登录的玩家对象。
     */
    @OnClick(R.id.btn_get_game_player)
    public void getGamePlayer() {
        PlayersClient client = Games.getPlayersClient(this);
        Task<Player> task = client.getGamePlayer();
        task.addOnSuccessListener(new OnSuccessListener<Player>() {
            @Override
            public void onSuccess(Player player) {
                printPlayerInfo(player);
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
     * Obtains the current logged-in player object and requests the PlayerId information.
     * *
     * 获取当前登录的玩家对象并获取PlayerId信息。
     */
    @OnClick(R.id.btn_get_game_player_false)
    public void getGamePlayerNotRequestPlayerId() {
        PlayersClient client = Games.getPlayersClient(this);

        Task<Player> task = client.getGamePlayer(false);
        task.addOnSuccessListener(new OnSuccessListener<Player>() {
            @Override
            public void onSuccess(Player player) {
                printPlayerInfo(player);
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
     * The system does not obtain the PlayerId information when obtaining the player object that is currently logged in.
     * *
     * 获取当前登录的玩家对象不获取PlayerId信息。
     */
    @OnClick(R.id.btn_get_game_player_true)
    public void getGamePlayerAndRequestPlayerId() {
        PlayersClient client = Games.getPlayersClient(this);

        Task<Player> task = client.getGamePlayer(true);
        task.addOnSuccessListener(new OnSuccessListener<Player>() {
            @Override
            public void onSuccess(Player player) {
                printPlayerInfo(player);
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
     * Obtains additional information about a player.
     * *
     * 获取玩家附加信息。
     */
    @OnClick(R.id.btn_play_extra)
    public void gamePlayExtra() {
        PlayersClient client = Games.getPlayersClient(this);
        Task<PlayerExtraInfo> task = client.getPlayerExtraInfo(sessionId);
        task.addOnSuccessListener(new OnSuccessListener<PlayerExtraInfo>() {
            @Override
            public void onSuccess(PlayerExtraInfo extra) {
                if (extra != null) {
                    showLog("IsRealName: " + extra.getIsRealName() + ", IsAdult: " + extra.getIsAdult() + ", PlayerId: "
                        + extra.getPlayerId() +  ", OpenId: "
                            + extra.getOpenId() +", PlayerDuration: " + extra.getPlayerDuration());
                } else {
                    showLog("Player extra info is empty.");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    int rtnCode = ((ApiException) e).getStatusCode();
                    String result = "rtnCode:" + rtnCode;
                    showLog(result);
                    if (rtnCode == 7023) {
                        showLog("It is recommended to check every 15 minutes.");
                        return;
                    }
                    if ((rtnCode == 7002 && NetworkUtil.isNetworkAvailable(PlayerActivity.this)) || rtnCode == 7006) {
                        showLog("No additional user information was found and allow the player to enter the game");
                    }
                }
            }
        });
    }

    private void printPlayerInfo(final Player player) {
        if (player != null) {
            String result = "playerInfo:" + "\n"
                    + "displayName:" + player.getDisplayName() + "\n"
                    + "playerId:" + player.getPlayerId() + "\n"
                    + "playerLevel:" + player.getLevel() + "\n"
                    + "timestamp:" + player.getSignTs() + "\n"
                    + "playerSign:" + player.getPlayerSign() + "\n"
                    + ":----------------------------分割线----------------------------:" + "\n"
                    + "openId:" + player.getOpenId() + "\n"
                    + "unionId:" + player.getUnionId() + "\n"
                    + "accessToken:" + player.getAccessToken()+ "\n"
                    + "openIdSign:" + player.getOpenIdSign()+ "\n"
                    + "hiResImage:" + player.getHiResImageUri() + "\n"
                    + "iconImage:" + player.getIconImageUri();
            showLog(result);
        }
    }
}
