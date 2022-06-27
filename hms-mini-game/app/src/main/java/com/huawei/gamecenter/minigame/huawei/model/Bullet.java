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
import android.graphics.Paint;

public class Bullet {
    private final Paint paint;
    private int radius;
    private final float moveStep;
    private final Bitmap bitmap;

    public Bullet(Paint paint, int radius, float moveStep, Bitmap bitmap) {
        this.paint = paint;
        this.radius = radius;
        this.moveStep = moveStep;
        this.bitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public Paint getPaint() {
        return paint;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public float getMoveStep() {
        return moveStep;
    }

}
