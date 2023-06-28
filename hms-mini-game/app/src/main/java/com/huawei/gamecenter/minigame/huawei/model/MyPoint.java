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

public class MyPoint {
    private final double angle;
    private int x;
    private int y;

    public MyPoint(int x, int y, double angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double getAngle() {
        return angle;
    }

    public void set(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // Method of moving points
    // 点移动的方法
    /*
     * moveStep:步长
     * boundWidth:点所有在区域的宽度
     * boundHeight:点所在区域的高度
     * */
    public void move(float moveStep, boolean isEnemy) {
        double moveX, moveY;
        if (getAngle() >= 0) {
            moveX = moveStep * Math.cos(getAngle());
            moveY = moveStep * Math.sin(getAngle());
        } else {
            moveX = -moveStep * Math.cos(-getAngle());
            moveY = moveStep * Math.sin(-getAngle());
        }
        if (!isEnemy) set((int) (getX() + moveX), (int) (getY() - moveY));
        else set((int) (getX() + moveX), (int) (getY() + moveY));
    }

    // Whether to leave the area
    // 是否离开该区域
    public boolean isOutOfBounds(int boundWidth, int boundHeight) {
        if (getX() > boundWidth || getX() < 0) {
            return true;
        } else {
            return getY() > boundHeight || getY() < 0;
        }
    }

    // Whether to leave the area. Ignore the top
    // 是否离开该区域，忽略顶部
    public boolean isOutOfBoundsWithOutTop(int boundWidth, int boundHeight) {
        if (getX() < 0 || getX() > boundWidth) {
            return true;
        } else {
            return getY() > boundHeight;
        }
    }

}
