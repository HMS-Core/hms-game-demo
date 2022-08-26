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

package com.intermodaltransport.huawei;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;

import com.huawei.gamecenter.minigame.huawei.R;
import com.huawei.gamecenter.minigame.huawei.Until.Constant;
import com.huawei.gamecenter.minigame.huawei.Until.HMSLogHelper;
import com.huawei.gamecenter.minigame.huawei.Until.TimeUtil;
import com.huawei.gamecenter.minigame.huawei.Until.UntilTool;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.IapApiException;
import com.huawei.hms.iap.entity.InAppPurchaseData;
import com.huawei.hms.iap.entity.OwnedPurchasesReq;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;

import org.json.JSONException;

public class SubscribeManager {
    private static final String TAG = "SubscribeManager";
    private static SubscribeManager singletonInstance;

    private SubscribeManager() {
    }

    public static SubscribeManager getSingletonInstance() {
        if (singletonInstance == null) {
            singletonInstance = new SubscribeManager();
        }
        return singletonInstance;
    }

    /**
     * 检查订阅是否生效
     *
     * @param obtainScoreLayout 获取积分布局
     * @param activity          activity对象
     * @param currentId         当前的玩家id
     * @param callback         购买数据回调，可以自定义处理数据
     */
    public void checkSubIsValidAndRefreshView(View obtainScoreLayout, Activity activity, String currentId, Callback callback) {
        // 构造一个OwnedPurchasesReq对象
        OwnedPurchasesReq ownedPurchasesReq = new OwnedPurchasesReq();
        // priceType: 2：订阅型商品
        ownedPurchasesReq.setPriceType(Constant.PurchasesPriceType.PRICE_TYPE_SUBSCRIBING_OFFERING);
        // 调用obtainOwnedPurchases接口
        Task<OwnedPurchasesResult> task = Iap.getIapClient(activity).obtainOwnedPurchases(ownedPurchasesReq);
        task.addOnSuccessListener(result -> {
            // 获取接口请求结果
            if (result == null
                    || result.getInAppPurchaseDataList() == null
                    || activity == null
                    || obtainScoreLayout == null
                    || TextUtils.isEmpty(currentId)) {
                HMSLogHelper.getSingletonInstance().debug(TAG, "owned purchases is empty or params is empty");
                return;
            }
            if (callback != null) {
                callback.onResult(result);
            }
            refreshView(obtainScoreLayout, activity, currentId, result);
        }).addOnFailureListener(e -> {
            if (e instanceof IapApiException) {
                IapApiException apiException = (IapApiException) e;
                int returnCode = apiException.getStatusCode();
                HMSLogHelper.getSingletonInstance().debug(TAG, "returnCode: " + returnCode);
            } else {
                // 其他外部错误
                HMSLogHelper.getSingletonInstance().debug(TAG, "other error");
            }
        });
    }

    /**
     * 根据获取的生效的订阅结果刷新获取积分按钮
     *
     * @param obtainScoreLayout   获取积分布局
     * @param activity            activity
     * @param currentId           玩家id
     * @param result              生效的订阅对象
     */
    private void refreshView(View obtainScoreLayout, Activity activity, String currentId, OwnedPurchasesResult result) {
        if (result.getInAppPurchaseDataList().size() == 0) {
            obtainScoreLayout.setVisibility(View.GONE);
        }
        for (int i = 0; i < result.getInAppPurchaseDataList().size(); i++) {
            String inAppPurchaseData = result.getInAppPurchaseDataList().get(i);
            // 您需要使用您的应用的IAP公钥验证inAppPurchaseData的签名
            // 如果验签成功，请检查支付状态和订阅状态
            try {
                InAppPurchaseData inAppPurchaseDataBean = new InAppPurchaseData(inAppPurchaseData);
                boolean isSubValid = inAppPurchaseDataBean.isSubValid();
                if (!isSubValid) {
                    obtainScoreLayout.setVisibility(View.GONE);
                    continue;
                }
                obtainScoreLayout.setVisibility(View.VISIBLE);
                long lastUpdateTime = UntilTool.getLastScoreUpdateTime(activity, currentId);
                if (TimeUtil.isSameDayOfMillis(lastUpdateTime, System.currentTimeMillis())) {
                    obtainScoreLayout.setBackgroundResource(R.drawable.button_already_obtain_score);
                } else {
                    obtainScoreLayout.setBackgroundResource(R.drawable.button_obtain_score_selector);
                }
            } catch (JSONException e) {
                HMSLogHelper.getSingletonInstance().debug(TAG, "other error");
            }
        }
    }

    /**
     *
     * 回调获取的购买数据
     */
    public interface Callback {
        void onResult(OwnedPurchasesResult result);
    }

}
