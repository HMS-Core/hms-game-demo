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

public class MyRect {
    public int x;
    public int y;
    public int width;
    public int height;

    public MyRect() {
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean intersects(MyRect bounds) {
        return this.x >= bounds.x && this.x < bounds.x + bounds.width && this.y >= bounds.y && this.y < bounds.y + bounds.height;
    }

    public boolean allIntersects(MyRect bounds) {
        return intersects(bounds)
                || (this.x + width >= bounds.x && this.x + width < bounds.x + bounds.width && this.y + height >= bounds.y && this.y + height < bounds.y + bounds.height)
                || (this.x >= bounds.x && this.x < bounds.x + bounds.width && this.y + height >= bounds.y && this.y + height < bounds.y + bounds.height)
                || (this.x + width >= bounds.x && this.x + width < bounds.x + bounds.width && this.y >= bounds.y && this.y < bounds.y + bounds.height);
    }


}
