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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metrics for a single Hystrix command
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class HystrixCommandMetrics {
    private String name;
    private int success;
    private int rejected;
    private int failed;
    private int timedOut;
    private int requests;
    private int shortCircuited;
    private int reportingHosts;
    private double requestRate;
    private double errorPercentage;


    private HystrixCommandMetrics() {
    }

    @JsonCreator
    HystrixCommandMetrics(
            @JsonProperty("name") String name,
            @JsonProperty("reportingHosts") Integer reportingHosts,
            @JsonProperty("rollingCountTimeout") Integer timedOut,
            @JsonProperty("rollingCountFailure") Integer failed,
            @JsonProperty("rollingCountSuccess") Integer success,
            @JsonProperty("rollingCountShortCircuited") Integer shortCircuited,
            @JsonProperty("rollingCountThreadPoolRejected") Integer threadPoolRejected,
            @JsonProperty("rollingCountSemaphoreRejected") Integer semaphoreRejected,
            @JsonProperty("propertyValue_metricsRollingStatisticalWindowInMilliseconds") Double rollingWindowMs
    ) {

        this.name = name;
        this.reportingHosts = reportingHosts;
        this.timedOut = timedOut;
        this.failed = failed;
        this.success = success;
        this.shortCircuited = shortCircuited;
        this.rejected = threadPoolRejected;
        this.rejected += semaphoreRejected;

        // see https://github.com/Netflix/Hystrix/blob/master/hystrix-core/src/main/java/com/netflix/hystrix/HystrixCommandMetrics.java
        this.requests = this.failed + this.success + this.timedOut + this.rejected + this.shortCircuited;
        long errorCount = this.failed + this.timedOut + this.rejected + this.shortCircuited;

        if (this.requests > 0) {
            this.errorPercentage = (double) errorCount / this.requests * 100;
        }

        double numberSeconds = Math.floor(rollingWindowMs / this.reportingHosts) / 1000;

        if(numberSeconds > 0) {
            this.requestRate = this.requests / numberSeconds;
        }

        if(!Double.isFinite(this.requestRate)) {
            this.requestRate = 0D;
        }
        if(!Double.isFinite(this.errorPercentage)) {
            this.errorPercentage = 0D;
        }

    }

    public int getSuccess() {
        return success;
    }

    public int getRejected() {
        return rejected;
    }

    public int getFailed() {
        return failed;
    }

    public int getTimedOut() {
        return timedOut;
    }

    public int getRequests() {
        return requests;
    }

    public double getErrorPercentage() {
        return errorPercentage;
    }

    public int getShortCircuited() {
        return shortCircuited;
    }

    public double getRequestRate() {
        return requestRate;
    }

    public int getReportingHosts() {
        return reportingHosts;
    }

    public String getName() {
        return name;
    }
}
