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

package com.huawei.gamecenter.minigame.huawei.UI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * triangles
 **
 * 三角形
 */
public class TriangleView extends View {

    private Paint paint;
    private Path path;
    private int color;
    private int mode;

    private final int DEFAULT_WIDTH = 48;
    private final int DEFAULT_HEIGHT = 24;

    private int width = 0;
    private int height = 0;

    /**
     * inverted triangle
     **
     * 倒三角
     */
    public static final int INVERTED = 0;
    /**
     * positive triangle
     **
     * 正三角
     */
    public static final int REGULAR = 1;

    @IntDef({INVERTED, REGULAR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ShapeMode {
    }

    public TriangleView(Context context) {
        this(context, null);
    }

    public TriangleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TriangleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        color = Color.GRAY;
        mode = 1;


        paint = new Paint();
        paint.setColor(color);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

        path = new Path();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = measureSize(widthMeasureSpec, DEFAULT_WIDTH);
        height = measureSize(heightMeasureSpec, DEFAULT_HEIGHT);
        setMeasuredDimension(width, height);
    }

    private int measureSize(int measureSpec, int defaultSize) {
        int newSize = 0;
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        switch (mode) {
            case MeasureSpec.AT_MOST:
                newSize = Math.min(size, defaultSize);
                break;
            case MeasureSpec.EXACTLY:
                newSize = size;
                break;
            case MeasureSpec.UNSPECIFIED:
                newSize = defaultSize;
                break;
        }
        return newSize;
    }

    public void setColor(int color) {
        this.color = color;
        paint.setColor(color);
        invalidate();
    }

    public void setMode(@ShapeMode int mode) {
        this.mode = mode;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTriangle(canvas);
    }

    private void drawTriangle(Canvas canvas) {
        if (mode == INVERTED) {
            path.moveTo(0f, 0f);
            path.lineTo(width, 0f);
            path.lineTo(width / 2.0f, height);
        } else {
            path.moveTo(width / 2.0f, 0f);
            path.lineTo(0, height);
            path.lineTo(width, height);
        }
        path.close();
        canvas.drawPath(path, paint);
    }
}