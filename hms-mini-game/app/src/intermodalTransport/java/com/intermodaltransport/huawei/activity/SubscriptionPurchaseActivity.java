/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2022-2022. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *
 *  you may not use this file except in compliance with the License.
 *
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.intermodaltransport.huawei.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.huawei.gamecenter.minigame.huawei.R;
import com.huawei.gamecenter.minigame.huawei.Until.Constant;
import com.huawei.gamecenter.minigame.huawei.Until.HMSLogHelper;
import com.huawei.gamecenter.minigame.huawei.Until.TimeUtil;
import com.huawei.gamecenter.minigame.huawei.Until.UntilTool;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.IapApiException;
import com.huawei.hms.iap.entity.InAppPurchaseData;
import com.huawei.hms.iap.entity.OrderStatusCode;
import com.huawei.hms.iap.entity.ProductInfo;
import com.huawei.hms.iap.entity.ProductInfoReq;
import com.huawei.hms.iap.entity.ProductInfoResult;
import com.huawei.hms.iap.entity.PurchaseIntentReq;
import com.huawei.hms.iap.entity.PurchaseIntentResult;
import com.huawei.hms.iap.entity.PurchaseResultInfo;
import com.huawei.hms.jos.games.player.Player;
import com.huawei.hms.support.api.client.Status;
import com.intermodaltransport.huawei.SubscribeManager;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import static com.huawei.gamecenter.minigame.huawei.Until.Constant.OBTAIN_BONUS_POINTS_EVERY_DAY;
import static com.huawei.gamecenter.minigame.huawei.Until.Constant.START_RESOLUTION_REQUEST_CODE;

public class SubscriptionPurchaseActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "SubscriptionPurchaseAct";
    private static int currentScore = 0;
    private static String openId;
    private TextView scoreText;
    private ConstraintLayout obtainScoreLayout;
    private List<ProductInfo> mProductList = new ArrayList<>();
    private TextView buyWeekText;
    private RelativeLayout buyWeekLayout;
    private TextView buyMonthText;
    private RelativeLayout buyMonthLayout;
    private TextView buyYearText;
    private RelativeLayout buyYearLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_subscription_purchase);
        Player currentPlayer = getIntent().getParcelableExtra(Constant.PLAYER_INFO_KEY);
        openId = currentPlayer.getOpenId();
        currentScore = UntilTool.getInfo(this, openId);
        initView();
    }

    @SuppressLint("SetTextI18n")
    private void initView() {
        scoreText = findViewById(R.id.text_score);
        scoreText.setText(currentScore + "");
        obtainScoreLayout = findViewById(R.id.obtain_score_layout);
        buyWeekText = findViewById(R.id.buy_week);
        buyWeekLayout = findViewById(R.id.week_card_layout);
        buyMonthText = findViewById(R.id.buy_month);
        buyMonthLayout = findViewById(R.id.month_card_layout);
        buyYearText = findViewById(R.id.buy_year);
        buyYearLayout = findViewById(R.id.year_card_layout);
        findViewById(R.id.btn_shopping_back).setOnClickListener(SubscriptionPurchaseActivity.this);
        findViewById(R.id.week_card_layout).setOnClickListener(SubscriptionPurchaseActivity.this);
        findViewById(R.id.month_card_layout).setOnClickListener(SubscriptionPurchaseActivity.this);
        findViewById(R.id.year_card_layout).setOnClickListener(SubscriptionPurchaseActivity.this);
        findViewById(R.id.obtain_score_layout).setOnClickListener(SubscriptionPurchaseActivity.this);
        buyWeekText.setText(getString(R.string.purchase));
        buyMonthText.setText(getString(R.string.purchase));
        buyYearText.setText(getString(R.string.purchase));
    }

    @Override
    protected void onResume() {
        super.onResume();
        showProductInfo();
    }

    @SuppressLint("DefaultLocale")
    private void showProductInfo() {
        List<String> productIdList = new ArrayList<>();
        // The product to be queried must be configured on the AppGallery Connect website.
        // 查询的商品必须是您在AppGallery Connect网站配置的商品
        productIdList.add("subscribe_001");
        productIdList.add("subscribe_002");
        productIdList.add("subscribe_003");
        ProductInfoReq req = new ProductInfoReq();
        // priceType: 0 :consumable product 1: non-consumable product; 2: subscription product
        // priceType: 0：消耗型商品; 1：非消耗型商品; 2：订阅型商品*
        req.setPriceType(Constant.PurchasesPriceType.PRICE_TYPE_SUBSCRIBING_OFFERING);
        req.setProductIds(productIdList);
        // Invoke the obtainProductInfo interface to obtain offering details configured on the AppGallery Connect website.
        // 调用obtainProductInfo接口获取AppGallery Connect网站配置的商品的详情信息
        Task<ProductInfoResult> task = Iap.getIapClient(SubscriptionPurchaseActivity.this).obtainProductInfo(req);
        task.addOnSuccessListener(result -> {
            // Obtain the product's details returned when the interface request is successful.
            // 获取接口请求成功时返回的商品详情信息
            mProductList = result.getProductInfoList();
            // After the offering list is obtained, it needs to be updated. Currently, only two offering display effects are set in this demo. Multiple offering lists can be displayed cyclically in the list view.
            // 商品列表查询成功获取之后，需要进行刷新,此demo当前只设置了两个商品展示效果，对于多个商品列表展示可以采用循环配合listView展示。
            setProductInfo(mProductList);
            SubscribeManager.getSingletonInstance().checkSubIsValidAndRefreshView(obtainScoreLayout, this, openId, callback);
        }).addOnFailureListener(e -> {
            if (e instanceof IapApiException) {
                IapApiException apiException = (IapApiException) e;
                int returnCode = apiException.getStatusCode();
                HMSLogHelper.getSingletonInstance().debug(TAG, "returnCode: " + returnCode);
            }
        });
    }

    private final SubscribeManager.Callback callback = result -> {
        if (result == null || result.getInAppPurchaseDataList() == null) {
            return;
        }
        if (result.getInAppPurchaseDataList().size() == 0) {
            obtainScoreLayout.setVisibility(View.GONE);
        }
        for (int i = 0; i < result.getInAppPurchaseDataList().size(); i++) {
            String inAppPurchaseData = result.getInAppPurchaseDataList().get(i);
            // You need to use your app's IAP public key to verify the signature of inAppPurchaseData.
            // If the verification is successful, check the payment status and subscription status.
            // 您需要使用您的应用的IAP公钥验证inAppPurchaseData的签名
            // 如果验签成功，请检查支付状态和订阅状态
            try {
                InAppPurchaseData inAppPurchaseDataBean = new InAppPurchaseData(inAppPurchaseData);
                boolean isSubValid = inAppPurchaseDataBean.isSubValid();
                if (!isSubValid) {
                    obtainScoreLayout.setVisibility(View.GONE);
                    continue;
                }
                if (inAppPurchaseDataBean.getProductId().equals(mProductList.size() > 2 ? mProductList.get(0).getProductId() : "")) {
                    buyWeekText.setText(getString(R.string.effective));
                    buyWeekLayout.setClickable(false);
                    buyMonthText.setText(getString(R.string.purchase));
                    buyMonthLayout.setClickable(true);
                    buyYearText.setText(getString(R.string.purchase));
                    buyYearLayout.setClickable(true);
                } else if (inAppPurchaseDataBean.getProductId().equals(mProductList.size() > 2 ? mProductList.get(1).getProductId() : "")) {
                    buyWeekText.setText(getString(R.string.purchase));
                    buyMonthText.setText(getString(R.string.effective));
                    buyYearText.setText(getString(R.string.purchase));
                    buyWeekLayout.setClickable(true);
                    buyMonthLayout.setClickable(false);
                    buyYearLayout.setClickable(true);
                } else if (inAppPurchaseDataBean.getProductId().equals(mProductList.size() > 2 ? mProductList.get(2).getProductId() : "")) {
                    buyWeekText.setText(getString(R.string.purchase));
                    buyMonthText.setText(getString(R.string.purchase));
                    buyYearText.setText(getString(R.string.effective));
                    buyWeekLayout.setClickable(true);
                    buyMonthLayout.setClickable(true);
                    buyYearLayout.setClickable(false);
                }
                obtainScoreLayout.setVisibility(View.VISIBLE);
                long lastUpdateTime = UntilTool.getLastScoreUpdateTime(this, openId);
                if (TimeUtil.isSameDayOfMillis(lastUpdateTime, System.currentTimeMillis())) {
                    obtainScoreLayout.setBackgroundResource(R.drawable.button_already_obtain_score);
                } else {
                    obtainScoreLayout.setBackgroundResource(R.drawable.button_obtain_score_selector);
                }
            } catch (JSONException e) {
                HMSLogHelper.getSingletonInstance().debug(TAG, e.getLocalizedMessage());
            }
        }

        List<String> placedInappPurchaseDataList = result.getPlacedInappPurchaseDataList();
        for (int i = 0; i < placedInappPurchaseDataList.size(); i++) {
            String inAppPurchaseData = placedInappPurchaseDataList.get(i);
            try {
                InAppPurchaseData inAppPurchaseDataBean = new InAppPurchaseData(inAppPurchaseData);
                boolean isSubValid = inAppPurchaseDataBean.isSubValid();
                if (isSubValid || inAppPurchaseDataBean.getPurchaseState() != -1) {
                    continue;
                }
                if (inAppPurchaseDataBean.getProductId().equals(mProductList.size() > 2 ? mProductList.get(0).getProductId() : "")) {
                    buyWeekText.setText(getString(R.string.to_be_effective));
                    buyWeekLayout.setClickable(false);
                } else if (inAppPurchaseDataBean.getProductId().equals(mProductList.size() > 2 ? mProductList.get(1).getProductId() : "")) {
                    buyMonthText.setText(getString(R.string.to_be_effective));
                    buyMonthLayout.setClickable(false);
                } else if (inAppPurchaseDataBean.getProductId().equals(mProductList.size() > 2 ? mProductList.get(2).getProductId() : "")) {
                    buyYearText.setText(getString(R.string.to_be_effective));
                    buyYearLayout.setClickable(false);
                }
            } catch (JSONException e) {
                HMSLogHelper.getSingletonInstance().debug(TAG, "json exception");
            }
        }
    };


    @SuppressLint("SetTextI18n")
    private void setProductInfo(List<ProductInfo> productList) {
        if (productList.size() > 2) {
            ((TextView) findViewById(R.id.week_card)).setText(productList.get(0).getProductName());
            ((TextView) findViewById(R.id.price)).setText(productList.get(0).getPrice());
            ((TextView) findViewById(R.id.month_card)).setText(productList.get(1).getProductName());
            ((TextView) findViewById(R.id.month_price)).setText(productList.get(1).getPrice());
            ((TextView) findViewById(R.id.year_card)).setText(productList.get(2).getProductName());
            ((TextView) findViewById(R.id.year_price)).setText(productList.get(2).getPrice());
        } else {
            HMSLogHelper.getSingletonInstance().debug(TAG, "productList is null, please check internet");
        }
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_shopping_back:
                onBackPressed();
                break;
            case R.id.week_card_layout:
                startPay((mProductList.size() > 2 ? mProductList.get(0).getProductId() : ""));
                break;
            case R.id.month_card_layout:
                startPay((mProductList.size() > 2 ? mProductList.get(1).getProductId() : ""));
                break;
            case R.id.year_card_layout:
                startPay((mProductList.size() > 2 ? mProductList.get(2).getProductId() : ""));
                break;
            case R.id.obtain_score_layout:
                updateScore();
                break;
            default:
                break;
        }
    }


    private void updateScore() {
        long lastUpdateTime = UntilTool.getLastScoreUpdateTime(this, openId);
        if (!TimeUtil.isSameDayOfMillis(lastUpdateTime, System.currentTimeMillis())) {
            UntilTool.updateScoreTime(this, openId);
            currentScore = currentScore + OBTAIN_BONUS_POINTS_EVERY_DAY;
            scoreText.setText(String.valueOf(currentScore));
            UntilTool.addInfo(this, openId, currentScore);
            HMSLogHelper.getSingletonInstance().debug(TAG, "updateScore currentScore:" + currentScore);
            obtainScoreLayout.setBackgroundResource(R.drawable.button_already_obtain_score);
        } else {
            Toast.makeText(this, getString(R.string.already_received_tips), Toast.LENGTH_SHORT).show();
        }
    }

    private void startPay(String productId) {
        // Construct a PurchaseIntentReq object.
        // 构造一个PurchaseIntentReq对象
        PurchaseIntentReq req = new PurchaseIntentReq();
        // The offerings to be purchased through the createPurchaseIntent interface must be those configured on the AppGallery Connect website.
        // 通过createPurchaseIntent接口购买的商品必须是您在AppGallery Connect网站配置的商品。
        req.setProductId(productId);
        // priceType: 0 :consumable product 1: non-consumable product; 2: subscription product
        // priceType: 0：消耗型商品; 1：非消耗型商品; 2：订阅型商品
        req.setPriceType(Constant.PurchasesPriceType.PRICE_TYPE_SUBSCRIBING_OFFERING);
        req.setDeveloperPayload("test");
        // Invoke the createPurchaseIntent interface to create a hosting offering order.
        // 调用createPurchaseIntent接口创建托管商品订单
        Task<PurchaseIntentResult> task = Iap.getIapClient(this).createPurchaseIntent(req);
        task.addOnSuccessListener(result -> {
            // Obtain the order creation result.
            // 获取创建订单的结果
            Status status = result.getStatus();
            if (status.hasResolution()) {
                try {
                    // 6666 is your custom constant
                    // Launch the cashier page returned by the IAP
                    // 6666是您自定义的常量
                    // 启动IAP返回的收银台页面
                    status.startResolutionForResult(SubscriptionPurchaseActivity.this, START_RESOLUTION_REQUEST_CODE);
                } catch (IntentSender.SendIntentException exp) {
                    HMSLogHelper.getSingletonInstance().debug(TAG, "exp: " + exp.getMessage());
                }
            }
        }).addOnFailureListener(e -> {
            if (e instanceof IapApiException) {
                IapApiException apiException = (IapApiException) e;
                int returnCode = apiException.getStatusCode();
                HMSLogHelper.getSingletonInstance().debug(TAG, "other error: " + returnCode);
            } else {
                // Other External Errors
                // 其他外部错误
                HMSLogHelper.getSingletonInstance().debug(TAG, "other error");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == START_RESOLUTION_REQUEST_CODE) {
            if (data == null) {
                HMSLogHelper.getSingletonInstance().error("onActivityResult", "data is null");
                return;
            }
            // Invoke the parseRespCodeFromIntent method to obtain the interface request result
            // 调用parsePurchaseResultInfoFromIntent方法解析支付结果数据
            PurchaseResultInfo purchaseResultInfo = Iap.getIapClient(this).parsePurchaseResultInfoFromIntent(data);
            switch (purchaseResultInfo.getReturnCode()) {
                case OrderStatusCode.ORDER_STATE_CANCEL:
                case OrderStatusCode.ORDER_STATE_FAILED:
                case OrderStatusCode.ORDER_STATE_DEFAULT_CODE:
                    HMSLogHelper.getSingletonInstance().debug(TAG, "parsePurchaseResultInfoFromIntent: " + purchaseResultInfo.getReturnCode());
                    break;
                case OrderStatusCode.ORDER_PRODUCT_OWNED:
                    Toast.makeText(SubscriptionPurchaseActivity.this, getString(R.string.the_service_has_been_purchased), Toast.LENGTH_SHORT).show();
                    break;
                case OrderStatusCode.ORDER_STATE_SUCCESS:
                    HMSLogHelper.getSingletonInstance().debug(TAG, "order success");
                    break;
                default:
                    break;
            }
        }
    }
}
