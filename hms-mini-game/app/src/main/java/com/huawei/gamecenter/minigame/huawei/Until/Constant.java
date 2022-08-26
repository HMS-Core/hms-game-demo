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

package com.huawei.gamecenter.minigame.huawei.Until;

public class Constant {
    // 触碰警戒线
    public static final int MODE_ONE = 10001;
    // 积分消耗为0
    public static final int M0DE_TWO = 10002;
    // 游戏进入下一关事件标识
    public static final int MODE_THREE = 10003;
    // 游戏通关事件标识
    public static final int MODE_FOUR = 10004;
    public static final int START_RESOLUTION_REQUEST_CODE = 6666;
    public static final int START_IS_ENV_READY_REQUEST_CODE = 7777;
    public static final int SIGN_IN_REQUEST_CODE = 4444;
    public static String PLAYER_INFO_KEY = "player_info";
    public static String PLAYER_ICON_URI = "player_icon";
    public static final int OBTAIN_BONUS_POINTS_EVERY_DAY = 20;

    public static class PurchasesPriceType {
        public static final int PRICE_TYPE_CONSUMABLE_GOODS = 0;
        public static final int PRICE_TYPE_NON_EXPENDABLE_GOODS = 1;
        public static final int PRICE_TYPE_SUBSCRIBING_OFFERING = 2;
    }
}
