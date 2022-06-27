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

package com.intermodaltransport.huawei;

import android.app.Activity;
import android.app.Application;

import java.util.LinkedList;
import java.util.List;

public class ExitApplication extends Application {
    private static ExitApplication instance;
    private List<Activity> activityList = new LinkedList();

    private ExitApplication() {
    }

    public static ExitApplication getInstance() {
        if (null == instance) {
            instance = new ExitApplication();
        }
        return instance;
    }

    public void addActivity(Activity activity) {
        activityList.add(activity);
    }

    public void exit() {

        for (Activity activity : activityList) {
            activity.finish();
        }
        System.exit(0);
    }
}
