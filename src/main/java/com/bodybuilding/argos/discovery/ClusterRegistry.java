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

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rx.Observable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry of all monitored Hystrix clusters, this will merge the metric observables from each cluster.
 */
@Component
public class ClusterRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterRegistry.class);
    private final ClusterDiscovery clusterDiscovery;
    private final Map<String, HystrixClusterMonitor> monitoredClusters = Maps.newConcurrentMap();
    private final Observable<HystrixClusterMetrics> mergedMetrics; // metrics from all known clusters
    private final HystrixClusterMonitorFactory clusterMonitorFactory;


    @Autowired
    public ClusterRegistry(ClusterDiscovery clusterDiscovery,
                           HystrixClusterMonitorFactory clusterMonitorFactory) {
        Objects.requireNonNull(clusterDiscovery);
        Objects.requireNonNull(clusterMonitorFactory);
        this.clusterDiscovery = clusterDiscovery;
        this.clusterMonitorFactory = clusterMonitorFactory;

        // inspired by com.netflix.turbine.Turbine
        // https://github.com/Netflix/Turbine/commit/10cd853c912442d5d62278cc98c0fac2f33b65b9#diff-6b51f2ba8d8fc42a4e669d2f34205684R105
        Observable<Cluster> clusters = clusterDiscovery.getClusters().share();

        Observable<Cluster> clusterAdds = clusters.filter(Cluster::isActive);

        Observable<Cluster> clusterRemoves = clusters
                .filter(c -> !c.isActive());

        Observable<Observable<HystrixClusterMetrics>> clusterObservables = clusterAdds
                .map(c -> {
                    if (monitoredClusters.get(c.getName()) != null) {
                        return monitoredClusters.get(c.getName()).observe();
                    } else {
                        HystrixClusterMonitor monitor = clusterMonitorFactory.createMonitor(c.getName(), c.getUrl());
                        monitoredClusters.put(c.getName(), monitor);
                        LOG.info("Started monitoring {} | {}", c.getName(), c.getUrl());
                        return monitor.observe().takeUntil(clusterRemoves.filter(c2 -> {
                            if(c2.getName().equals(c.getName())) {
                                LOG.info("Stopping monitoring for {} ", c.getName());
                                return true;
                            } else {
                                return false;
                            }
                        }));
                    }
                });

        mergedMetrics = Observable.mergeDelayError(clusterObservables.retry()).share();
    }

    public Observable<HystrixClusterMetrics> observe() {
        return mergedMetrics;
    }
    public Optional<HystrixClusterMonitor> getCluster(String clusterName) {
        return Optional.ofNullable(monitoredClusters.get(clusterName));
    }
}
