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

package com.huawei.gamecenter.minigame.huawei;

import android.app.Application;

import com.huawei.hms.analytics.HiAnalytics;
import com.huawei.hms.analytics.HiAnalyticsInstance;
import com.huawei.hms.analytics.HiAnalyticsTools;
import com.huawei.hms.analytics.type.ReportPolicy;
import com.huawei.hms.api.HuaweiMobileServicesUtil;

import java.util.HashSet;
import java.util.Set;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // have to call this method here
        HuaweiMobileServicesUtil.setApplication(this);
        HiAnalyticsTools.enableLog();
        HiAnalyticsInstance instance = HiAnalytics.getInstance(this);
        ReportPolicy moveBackgroundPolicy = ReportPolicy.ON_MOVE_BACKGROUND_POLICY;
        Set<ReportPolicy> reportPolicies = new HashSet<>();
        reportPolicies.add(moveBackgroundPolicy);
        instance.setReportPolicies(reportPolicies);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
