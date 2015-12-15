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

/**
 * Implementation of {@link HystrixClusterMonitorFactory}  that returns instances of DefaultHystrixClusterMonitor
 * @see DefaultHystrixClusterMonitor
 */
public class DefaultHystrixClusterMonitorFactory implements HystrixClusterMonitorFactory {

    @Override
    public HystrixClusterMonitor createMonitor(String name, String streamUrl) {
        return new DefaultHystrixClusterMonitor(name, streamUrl);
    }
}
