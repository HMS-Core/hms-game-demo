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

package com.huawei.hms.game.appservice;

import android.os.Bundle;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.R;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.game.common.BaseActivity;
import com.huawei.hms.jos.JosApps;
import com.huawei.hms.jos.product.ProductClient;
import com.huawei.hms.jos.product.ProductOrderInfo;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class AppServiceActivity extends BaseActivity {
    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_service);
        ButterKnife.bind(this);
    }

    /**
     * This interface is used to query the information about orders that have not been shipped to the current Huawei ID.
     * *
     * 查询当前登录的华为帐号未发货的订单信息。
     */
    @OnClick(R.id.btn_get_miss_product)
    public void getMissProductList() {
        ProductClient appsClient = JosApps.getProductClient(this);
        Task<List<ProductOrderInfo>> task = appsClient.getMissProductOrder(this);
        task.addOnSuccessListener(new OnSuccessListener<List<ProductOrderInfo>>() {
            @Override
            public void onSuccess(List<ProductOrderInfo> productOrderInfos) {
                if (productOrderInfos != null) {
                    for (ProductOrderInfo productOrderInfo : productOrderInfos) {
                        String productNo = productOrderInfo.getProductNo();
                        String tradeId = productOrderInfo.getTradeId();
                        showLog("productNo：" + productNo + ",tradeId：" + tradeId);
                    }
                } else {
                    showLog("product list is null");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof ApiException) {
                    ApiException apiException = (ApiException) e;
                    showLog("get miss product info failed:" + apiException.getStatusCode());
                }
            }
        });
    }
}
