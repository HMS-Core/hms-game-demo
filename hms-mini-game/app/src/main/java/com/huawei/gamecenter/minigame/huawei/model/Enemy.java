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

public class Enemy {
    private final Paint paint;
    private float moveStep;
    private int radius;
    private final Bitmap bitmap;


    public Enemy(Paint paint, float moveStep, int radius, Bitmap bitmap) {
        this.paint = paint;
        this.moveStep = moveStep;
        this.radius = radius;
        this.bitmap = bitmap;
    }

    public Paint getPaint() {
        return paint;
    }

    public float getMoveStep() {
        return moveStep;
    }

    public void setMoveStep(float moveStep) {
        this.moveStep = moveStep;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

}