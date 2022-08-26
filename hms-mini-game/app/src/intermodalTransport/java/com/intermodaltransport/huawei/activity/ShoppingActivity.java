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

package com.intermodaltransport.huawei.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.huawei.gamecenter.minigame.huawei.R;
import com.huawei.gamecenter.minigame.huawei.Until.Constant;
import com.huawei.gamecenter.minigame.huawei.Until.HMSLogHelper;
import com.huawei.gamecenter.minigame.huawei.Until.TimeUtil;
import com.huawei.gamecenter.minigame.huawei.Until.UntilTool;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.IapApiException;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseReq;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseResult;
import com.huawei.hms.iap.entity.InAppPurchaseData;
import com.huawei.hms.iap.entity.IsEnvReadyResult;
import com.huawei.hms.iap.entity.OrderStatusCode;
import com.huawei.hms.iap.entity.OwnedPurchasesReq;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;
import com.huawei.hms.iap.entity.ProductInfo;
import com.huawei.hms.iap.entity.ProductInfoReq;
import com.huawei.hms.iap.entity.ProductInfoResult;
import com.huawei.hms.iap.entity.PurchaseIntentReq;
import com.huawei.hms.iap.entity.PurchaseIntentResult;
import com.huawei.hms.iap.entity.PurchaseResultInfo;
import com.huawei.hms.iap.util.IapClientHelper;
import com.huawei.hms.jos.games.player.Player;
import com.huawei.hms.support.api.client.Status;
import com.intermodaltransport.huawei.ExitApplication;
import com.intermodaltransport.huawei.SubscribeManager;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import static com.huawei.gamecenter.minigame.huawei.Until.Constant.OBTAIN_BONUS_POINTS_EVERY_DAY;

public class ShoppingActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "ShoppingActivity";
    private static String currentId;
    private static int currentScore = 0;
    private TextView scoreText;
    private Player currentPlayer;
    private String currentProductId;
    private List<ProductInfo> mProductList = new ArrayList<>();
    private ConstraintLayout obtainScoreLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_shopping_beta);
        currentPlayer = getIntent().getParcelableExtra(Constant.PLAYER_INFO_KEY);
        currentId = currentPlayer.getPlayerId();

        initView();
        checkIapEnv();
        ExitApplication.getInstance().addActivity(this);
    }


    @SuppressLint("DefaultLocale")
    private void showProductInfo() {
        List<String> productIdList = new ArrayList<>();
        // 查询的商品必须是您在AppGallery Connect网站配置的商品
        productIdList.add("20points");
        productIdList.add("100points");
        ProductInfoReq req = new ProductInfoReq();
        // priceType: 0：消耗型商品; 1：非消耗型商品; 2：订阅型商品
        req.setPriceType(Constant.PurchasesPriceType.PRICE_TYPE_CONSUMABLE_GOODS);
        req.setProductIds(productIdList);
        // 获取调用接口的Activity对象
        final Activity activity = ShoppingActivity.this;
        // 调用obtainProductInfo接口获取AppGallery Connect网站配置的商品的详情信息
        Task<ProductInfoResult> task = Iap.getIapClient(activity).obtainProductInfo(req);
        task.addOnSuccessListener(result -> {
            // 获取接口请求成功时返回的商品详情信息
            mProductList = result.getProductInfoList();
            // 商品列表查询成功获取之后，需要进行刷新,此demo当前只设置了两个商品展示效果，对于多个商品列表展示可以采用循环配合listView展示。
            setProductInfo(mProductList);
        }).addOnFailureListener(e -> {
            if (e instanceof IapApiException) {
                IapApiException apiException = (IapApiException) e;
                int returnCode = apiException.getStatusCode();
                HMSLogHelper.getSingletonInstance().debug(TAG, "returnCode: " + returnCode);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void setProductInfo(List<ProductInfo> mProductList) {
        if (mProductList == null) {
            HMSLogHelper.getSingletonInstance().debug(TAG, "mProductList is null,please check internet");
        } else if (mProductList.size() > 1) {
            ((TextView) findViewById(R.id.text_product_name)).setText(mProductList.get(0).getProductName());
            ((TextView) findViewById(R.id.text_product_desc)).setText(mProductList.get(0).getProductDesc());
            ((TextView) findViewById(R.id.text_product_price)).setText(mProductList.get(0).getPrice());
            ((TextView) findViewById(R.id.text_product_name_two)).setText(mProductList.get(1).getProductName());
            ((TextView) findViewById(R.id.text_product_desc_two)).setText(mProductList.get(1).getProductDesc());
            ((TextView) findViewById(R.id.text_product_price_two)).setText(mProductList.get(1).getPrice());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentScore = UntilTool.getInfo(this, currentId);
        scoreText.setText(String.valueOf(currentScore));
        SubscribeManager.getSingletonInstance().checkSubIsValidAndRefreshView(obtainScoreLayout, this, currentId, null);
    }


    @SuppressLint("SetTextI18n")
    private void initView() {
        obtainScoreLayout = findViewById(R.id.obtain_score_layout);
        obtainScoreLayout.setOnClickListener(ShoppingActivity.this);
        findViewById(R.id.shop_layout_product_background_first).setOnClickListener(ShoppingActivity.this);
        findViewById(R.id.shop_layout_product_background_second).setOnClickListener(ShoppingActivity.this);
        findViewById(R.id.btn_shopping_back).setOnClickListener(ShoppingActivity.this);
        findViewById(R.id.purchase_monthly_card_layout).setOnClickListener(ShoppingActivity.this);
        scoreText = findViewById(R.id.shop_score_show_text);
        scoreText.setText(currentScore + "");
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.shop_layout_product_background_first:
                if (mProductList == null || mProductList.size() == 0) {
                    HMSLogHelper.getSingletonInstance().debug(TAG, "mProductList is null, please check !");
                    return;
                }
                startPay(mProductList.get(0).getProductId());
                break;
            case R.id.shop_layout_product_background_second:
                if (mProductList == null || mProductList.size() < 1) {
                    HMSLogHelper.getSingletonInstance().debug(TAG, "mProductList is null, please check !");
                    return;
                }
                startPay(mProductList.get(1).getProductId());
                break;
            case R.id.btn_shopping_back:
                onBackPressed();
                break;
            case R.id.purchase_monthly_card_layout:
                Intent intent = new Intent(ShoppingActivity.this, SubscriptionPurchaseActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelable(Constant.PLAYER_INFO_KEY, currentPlayer);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
            case R.id.obtain_score_layout:
                long lastUpdateTime = UntilTool.getLastScoreUpdateTime(this, currentId);
                if (TimeUtil.isSameDayOfMillis(lastUpdateTime, System.currentTimeMillis())) {
                    Toast.makeText(this, getString(R.string.already_received_tips), Toast.LENGTH_SHORT).show();
                    return;
                }
                UntilTool.updateScoreTime(this, currentId);
                currentScore = currentScore + OBTAIN_BONUS_POINTS_EVERY_DAY;
                scoreText.setText(String.valueOf(currentScore));
                UntilTool.addInfo(this, currentId, currentScore);
                HMSLogHelper.getSingletonInstance().debug(TAG, "updateScore currentScore:" + currentScore);
                obtainScoreLayout.setBackgroundResource(R.drawable.button_already_obtain_score);
                break;
            default:
                break;
        }
    }


    /**
     * 启动支付购买
     */
    private void startPay(String productId) {
        currentProductId = productId;
        // 构造一个PurchaseIntentReq对象
        PurchaseIntentReq req = new PurchaseIntentReq();
        // 通过createPurchaseIntent接口购买的商品必须是您在AppGallery Connect网站配置的商品。
        req.setProductId(productId);
        // priceType: 0：消耗型商品; 1：非消耗型商品; 2：订阅型商品
        req.setPriceType(Constant.PurchasesPriceType.PRICE_TYPE_CONSUMABLE_GOODS);
        // developerPayload:自定义字段，一般用于携带本次支付订单的备注信息，支付成功之后在支付回调中会原样返回该字段信息。
        req.setDeveloperPayload("test miniGame product pay.");
        // 获取调用接口的Activity对象
        final Activity activity = ShoppingActivity.this;
        // 调用createPurchaseIntent接口创建托管商品订单
        Task<PurchaseIntentResult> task = Iap.getIapClient(activity).createPurchaseIntent(req);
        task.addOnSuccessListener(result -> {
            // 获取创建订单的结果
            Status status = result.getStatus();
            if (status.hasResolution()) {
                try {
                    // 6666是您自定义的常量
                    // 启动IAP返回的收银台页面
                    HMSLogHelper.getSingletonInstance().debug(TAG, "onSuccess:" + result.getReturnCode());
                    status.startResolutionForResult(activity, Constant.START_RESOLUTION_REQUEST_CODE);
                } catch (IntentSender.SendIntentException ignored) {
                }
            }
        }).addOnFailureListener(e -> {
            if (e instanceof IapApiException) {
                IapApiException apiException = (IapApiException) e;
                int returnCode = apiException.getStatusCode();
                HMSLogHelper.getSingletonInstance().debug(TAG, "addOnFailureListener:" + returnCode);
            }
        });
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            HMSLogHelper.getSingletonInstance().debug(TAG, "onActivityResult intent data is null");
            return;
        }
        if (requestCode == Constant.START_RESOLUTION_REQUEST_CODE) {
            // 调用parsePurchaseResultInfoFromIntent方法解析支付结果数据
            PurchaseResultInfo purchaseResultInfo = Iap.getIapClient(this).parsePurchaseResultInfoFromIntent(data);
            switch (purchaseResultInfo.getReturnCode()) {
                case OrderStatusCode.ORDER_STATE_CANCEL:
                    // 用户取消

                    break;
                case OrderStatusCode.ORDER_STATE_FAILED:
                case OrderStatusCode.ORDER_STATE_DEFAULT_CODE:
                case OrderStatusCode.ORDER_PRODUCT_OWNED:
                    // 检查是否存在未发货商品
                    HMSLogHelper.getSingletonInstance().debug(TAG, "product is owned !");
                    obtainOwnedProduct();
                    break;
                case OrderStatusCode.ORDER_STATE_SUCCESS:
                    // 支付成功
                    String inAppPurchaseData = purchaseResultInfo.getInAppPurchaseData();
                    // 使用您应用的IAP公钥验证签名
                    // 若验签成功，则进行发货
                    // 若用户购买商品为消耗型商品，您需要在发货成功后调用consumeOwnedPurchase接口进行消耗
                    InAppPurchaseData inAppPurchaseDataBean = null;
                    try {
                        inAppPurchaseDataBean = new InAppPurchaseData(inAppPurchaseData);
                    } catch (JSONException e) {
                        HMSLogHelper.getSingletonInstance().debug(TAG, e.getMessage());
                    }
                    assert inAppPurchaseDataBean != null;
                    String purchaseToken = inAppPurchaseDataBean.getPurchaseToken();
                    updateScore(currentPlayer.getPlayerId(), UntilTool.getScoreInt(currentProductId));
                    consumeGood(purchaseToken);
                    break;
                default:
                    break;
            }
        }

        if (requestCode == Constant.START_IS_ENV_READY_REQUEST_CODE) {
            // isEnvReady 接口跳转回调
            // 使用parseRespCodeFromIntent方法获取接口请求结果
            int returnCode = IapClientHelper.parseRespCodeFromIntent(data);
            // 使用parseCarrierIdFromIntent方法获取接口返回的运营商ID
            String carrierId = IapClientHelper.parseCarrierIdFromIntent(data);
            HMSLogHelper.getSingletonInstance().debug(TAG, String.format("parseRespCodeFromIntent return code is :%d", returnCode));
            HMSLogHelper.getSingletonInstance().debug(TAG, "parseRespCodeFromIntent return carrierId is :" + carrierId);
        }
    }

    private void updateScore(String playerId, int scoreIncrement) {
        currentId = playerId;
        currentScore = currentScore + scoreIncrement;
        scoreText.setText(String.valueOf(currentScore));
        UntilTool.addInfo(this, currentId, currentScore);
        HMSLogHelper.getSingletonInstance().debug(TAG, "updateScore currentScore:" + currentScore);
    }

    private void obtainOwnedProduct() {
        // 构造一个OwnedPurchasesReq对象
        OwnedPurchasesReq ownedPurchasesReq = new OwnedPurchasesReq();
        // priceType: 0：消耗型商品; 1：非消耗型商品; 2：订阅型商品
        ownedPurchasesReq.setPriceType(Constant.PurchasesPriceType.PRICE_TYPE_CONSUMABLE_GOODS);
        // 获取调用接口的Activity对象
        final Activity activity = ShoppingActivity.this;
        // 调用obtainOwnedPurchases接口获取所有已购但未发货的消耗型商品的购买信息
        Task<OwnedPurchasesResult> task = Iap.getIapClient(activity).obtainOwnedPurchases(ownedPurchasesReq);
        task.addOnSuccessListener(result -> {
            // 获取接口请求成功的结果
            if (result != null && result.getInAppPurchaseDataList() != null) {
                for (int i = 0; i < result.getInAppPurchaseDataList().size(); i++) {
                    String inAppPurchaseData = result.getInAppPurchaseDataList().get(i);
                    // 使用应用的IAP公钥验证inAppPurchaseData的签名数据
                    // 如果验签成功，确认每个商品的购买状态。确认商品已支付后，检查此前是否已发过货，未发货则进行发货操作。发货成功后执行消耗操作
                    try {
                        InAppPurchaseData inAppPurchaseDataBean = new InAppPurchaseData(inAppPurchaseData);
                        updateScore(currentPlayer.getPlayerId(), UntilTool.getScoreInt(currentProductId));
                        consumeGood(inAppPurchaseDataBean.getPurchaseToken());
                    } catch (JSONException ignored) {
                        HMSLogHelper.getSingletonInstance().debug(TAG, "other error");
                    }
                }
            }
        }).addOnFailureListener(e -> {
            if (e instanceof IapApiException) {
                IapApiException apiException = (IapApiException) e;
                int returnCode = apiException.getStatusCode();
                HMSLogHelper.getSingletonInstance().debug(TAG, String.format("IAP API obtainOwnedPurchases return code : %d", returnCode));
            } else {
                // 其他外部错误
                HMSLogHelper.getSingletonInstance().debug(TAG, "IAP API obtainOwnedPurchases others error.");
            }
        });
    }


    private void consumeGood(String purchaseToken) {
        // 构造一个ConsumeOwnedPurchaseReq对象
        ConsumeOwnedPurchaseReq req = new ConsumeOwnedPurchaseReq();
        req.setPurchaseToken(purchaseToken);
        // 获取调用接口的Activity对象
        final Activity activity = ShoppingActivity.this;
        // 调用consumeOwnedPurchase接口
        Task<ConsumeOwnedPurchaseResult> task = Iap.getIapClient(activity).consumeOwnedPurchase(req);
        task.addOnSuccessListener(result -> {
            // 获取接口请求成功时的结果信息
            HMSLogHelper.getSingletonInstance().debug(TAG, "ConsumeOwnedPurchase onSuccess: " + result.getReturnCode());
        }).addOnFailureListener(e -> {
            if (e instanceof IapApiException) {
                IapApiException apiException = (IapApiException) e;
                int returnCode = apiException.getStatusCode();
                HMSLogHelper.getSingletonInstance().debug(TAG, "ConsumeOwnedPurchase onFailure: " + returnCode);
            }
        });
    }

    /**
     * checkIapEnv   检查当前支付环境是否满足要求，例如HMS Core后台华为账号是否是已登录状态
     */
    private void checkIapEnv() {
        // 获取调用接口的Activity对象
        final Activity activity = ShoppingActivity.this;
        Task<IsEnvReadyResult> task = Iap.getIapClient(activity).isEnvReady();
        task.addOnSuccessListener(result -> {
            // 展示商品信息
            showProductInfo();
        });
        task.addOnFailureListener(e -> {
            if (e instanceof IapApiException) {
                IapApiException apiException = (IapApiException) e;
                Status status = apiException.getStatus();
                if (status.getStatusCode() == OrderStatusCode.ORDER_HWID_NOT_LOGIN) {
                    // 未登录帐号
                    if (status.hasResolution()) {
                        try {
                            // 启动IAP返回的登录页面
                            status.startResolutionForResult(activity, Constant.START_IS_ENV_READY_REQUEST_CODE);
                        } catch (IntentSender.SendIntentException exp) {
                            HMSLogHelper.getSingletonInstance().debug(TAG, "other error");
                        }
                    }
                } else if (status.getStatusCode() == OrderStatusCode.ORDER_ACCOUNT_AREA_NOT_SUPPORTED) {
                    // 用户当前登录的华为帐号所在的服务地不在华为IAP支持结算的国家/地区中
                    HMSLogHelper.getSingletonInstance().debug(TAG, "Not in support area.");
                }
            } else {
                // 其他外部错误
                HMSLogHelper.getSingletonInstance().debug(TAG, "other error");
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}