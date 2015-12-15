/*
 * Copyright (C) 2015 Bodybuilding.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bodybuilding.argos.discovery;

import rx.Observable;

/**
 * An object that emits cluster metrics for a single Hystrix cluster. This is usually fed from a single Turbine stream.
 */
public interface HystrixClusterMonitor {
    /**
     * Returns an Observable of Metrics for this cluster
     * @return
     */
    Observable<HystrixClusterMetrics> observe();

    /**
     * Returns the raw Hystrix Metrics json for this cluster
     * @return
     */
    Observable<String> observeJson();
}
