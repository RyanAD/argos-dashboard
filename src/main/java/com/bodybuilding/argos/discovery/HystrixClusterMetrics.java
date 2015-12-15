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

import java.util.Objects;

/**
 * Aggregate metrics for an entire Hystrix cluster.
 */
public final class HystrixClusterMetrics {
    public enum Type {
        CLUSTER
    }
    private final String clusterName;
    private final String streamUrl;
    private final String dashboardUrl;
    private long requestCount;
    private long successCount;
    private long failCount;
    private long timeoutCount;
    private long shortCircuitedCount;
    private long rejectedCount;
    private int reportingHosts;
    private int commandCount;
    private double requestRate;
    private double errorPercentage;
    private Type type;

    private HystrixClusterMetrics(String clusterName, String streamUrl) {
        Objects.requireNonNull(clusterName);
        this.clusterName = clusterName;
        this.streamUrl = streamUrl;
        this.dashboardUrl = "hystrix/monitor.html?stream=../turbine-stream/" + clusterName;
    }

    public HystrixClusterMetrics(HystrixClusterMetrics other) {
        this.clusterName = other.clusterName;
        this.streamUrl = other.streamUrl;
        this.dashboardUrl = other.dashboardUrl;
        this.requestCount = other.requestCount;
        this.successCount = other.successCount;
        this.failCount = other.failCount;
        this.timeoutCount = other.timeoutCount;
        this.shortCircuitedCount = other.shortCircuitedCount;
        this.rejectedCount = other.rejectedCount;
        this.requestRate = other.requestRate;
        this.type = other.type;
        this.reportingHosts = other.reportingHosts;
        this.errorPercentage = other.errorPercentage;
        this.commandCount = other.commandCount;
    }


    public static class Builder {
        private final HystrixClusterMetrics metrics;

        public Builder(String clusterName, String streamUrl) {
            metrics = new HystrixClusterMetrics(clusterName, streamUrl);
            metrics.type = Type.CLUSTER;
        }

        public static Builder newBuilder(String clusterName, String streamUrl) {
            return new Builder(clusterName, streamUrl);
        }

        public final Builder addCommandMetrics(HystrixCommandMetrics commandMetrics) {
            metrics.failCount += commandMetrics.getFailed();
            metrics.successCount += commandMetrics.getSuccess();
            metrics.rejectedCount += commandMetrics.getRejected();
            metrics.requestCount += commandMetrics.getRequests();
            metrics.timeoutCount += commandMetrics.getTimedOut();
            metrics.requestRate += commandMetrics.getRequestRate();
            metrics.shortCircuitedCount += commandMetrics.getShortCircuited();
            metrics.reportingHosts = Math.max(commandMetrics.getReportingHosts(), metrics.reportingHosts);

            // errorPercentage is the average errorPercentage of all the commands
            metrics.errorPercentage *= metrics.commandCount;
            metrics.errorPercentage += commandMetrics.getErrorPercentage();
            metrics.errorPercentage = metrics.errorPercentage / (metrics.commandCount + 1);

            metrics.commandCount++;
            return this;
        }

        public HystrixClusterMetrics build() {
            return new HystrixClusterMetrics(metrics);
        }
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public String getDashboardUrl() {
        return dashboardUrl;
    }

    public long getRequestCount() {
        return requestCount;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public long getFailCount() {
        return failCount;
    }

    public long getTimeoutCount() {
        return timeoutCount;
    }

    public long getShortCircuitedCount() {
        return shortCircuitedCount;
    }

    public long getRejectedCount() {
        return rejectedCount;
    }

    public int getReportingHosts() {
        return reportingHosts;
    }

    public int getCommandCount() {
        return commandCount;
    }

    public double getRequestRate() {
        return requestRate;
    }

    public double getErrorPercentage() {
        return errorPercentage;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "HystrixClusterMetrics{" +
                "clusterName='" + clusterName + '\'' +
                ", streamUrl='" + streamUrl + '\'' +
                ", dashboardUrl='" + dashboardUrl + '\'' +
                ", requestCount=" + requestCount +
                ", successCount=" + successCount +
                ", failCount=" + failCount +
                ", timeoutCount=" + timeoutCount +
                ", shortCircuitedCount=" + shortCircuitedCount +
                ", rejectedCount=" + rejectedCount +
                ", reportingHosts=" + reportingHosts +
                ", commandCount=" + commandCount +
                ", requestRate=" + requestRate +
                ", errorPercentage=" + errorPercentage +
                ", type=" + type +
                '}';
    }
}
