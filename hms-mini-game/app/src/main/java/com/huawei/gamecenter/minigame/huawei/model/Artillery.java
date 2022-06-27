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

package com.huawei.gamecenter.minigame.huawei.model;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;

public class Artillery {
    private final Matrix matrix;
    private final Paint paint;
    private final Bitmap bitmap;
    private int centerX, centerY;

    public Artillery(Matrix matrix, Paint paint, Bitmap bitmap) {
        this.matrix = matrix;
        this.paint = paint;
        this.bitmap = bitmap;
    }

    public Matrix getMatrix() {
        return matrix;
    }

    public Paint getPaint() {
        return paint;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public int getCenterX() {
        return centerX;
    }

    public void setCenterX(int centerX) {
        this.centerX = centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public void setCenter(int centerX, int centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
    }
}
