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

package com.huawei.hms.game;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import com.huawei.hms.R;

// 自定义协议弹窗，包含选中协议高亮文本效果以及点击事件，这里只做演示
public class ShowAgreementDialog extends AlertDialog {
    private final Context context;
    public static final int AGREE_TEXT_CLICK = 1;
    public static final int AGREE_BTN_CLICK = 3;
    public static final int NOT_AGREE_BTN_CLICK = 4;
    OnBtnClickListener listener;

    protected ShowAgreementDialog(Context context) {
        super(context);
        this.context = context;
    }

    public interface OnBtnClickListener {
        void onClick(int type);
    }

    public void setOnBtnClickListener(OnBtnClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_show_user_agreement);
        initView();
    }

    private void initView() {
        TextView agreeTv = findViewById(R.id.user_arg_tv);
        // 高亮选中的文本内容
        String serviceAgreement = context.getResources().getString(R.string.service_agreement);
        // 隐私协议文本，可替换，需要包含上面的高亮文本
        String agreementContent = context.getResources().getString(R.string.user_agr_content);
        int START_AG = agreementContent.indexOf(serviceAgreement);
        int END_AG = START_AG + serviceAgreement.length();
        SpannableString spannableString = new SpannableString(agreementContent);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(context.getResources().getColor(R.color.blue));
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // 高亮文本点击事件
                listener.onClick(AGREE_TEXT_CLICK);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(context.getResources().getColor(R.color.blue));
                ds.clearShadowLayer();
            }
        };

        // 同意和不同意按钮的选中效果，参见selector_btn.xml文件
        findViewById(R.id.notAgreeBtn).setOnClickListener(v -> listener.onClick(NOT_AGREE_BTN_CLICK));
        findViewById(R.id.agreeBtn).setOnClickListener(v -> listener.onClick(AGREE_BTN_CLICK));
        spannableString.setSpan(colorSpan, START_AG, END_AG, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        spannableString.setSpan(clickableSpan, START_AG, END_AG, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        agreeTv.setMovementMethod(LinkMovementMethod.getInstance());
        agreeTv.setText(spannableString);
        setCanceledOnTouchOutside(false);
        setCancelable(false);
    }

}
