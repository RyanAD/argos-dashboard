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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Abstract class that calls {@link #getCurrentClusters()} on a schedule
 */
public abstract class AbstractClusterDiscovery implements ClusterDiscovery {
    private final long updateIntervalMs;
    private final Map<String, Cluster> trackedClusters = Maps.newConcurrentMap();
    private final Observable<Cluster> clusterObservable;


    /**
     * Construct AbstractClusterDiscovery that calls {@link #getCurrentClusters()} on the specified interval
     * @param interval
     * @param timeUnit
     */
    public AbstractClusterDiscovery(long interval, TimeUnit timeUnit) {
        this.updateIntervalMs = timeUnit.toMillis(interval);

        clusterObservable = observeUpdateClusters()
                .retry()
                .flatMap(clusterList -> {
                            List<Cluster> updates = Lists.newArrayList();
                            clusterList.stream().forEach(c -> {
                                // update our tracked clusters for any new or downed clusters
                                if (trackedClusters.containsKey(c.getName()) && !c.isActive()) {
                                    trackedClusters.remove(c.getName());
                                    updates.add(c);
                                } else if (c.isActive() && !trackedClusters.containsKey(c.getName())) {
                                    trackedClusters.put(c.getName(), c);
                                    updates.add(c);
                                }
                            });
                            // find clusters not returned from the latest update
                            Set<String> discoveredClusters = clusterList.stream()
                                    .map(Cluster::getName)
                                    .collect(Collectors.toSet());
                            Sets.SetView<String> missingClusterNames = Sets.difference(trackedClusters.keySet(), discoveredClusters);
                            missingClusterNames.stream()
                                    .forEach(name -> {
                                        Cluster c = trackedClusters.get(name);
                                        updates.add(new Cluster(c.getName(), c.getUrl(), false));
                                        trackedClusters.remove(c.getName());
                                    });

                            return Observable.from(updates);
                        }
                )
                // we want to clear the list when all subscribers are unsubscribed.
                .doOnUnsubscribe(trackedClusters::clear);
    }

    @Override
    public Observable<Cluster> getClusters() {
        return clusterObservable;
    }


    /**
     * Returns an observable that periodically calls {@link #getCurrentClusters()}. This is the same as fixedDelay in {@link java.util.concurrent.ScheduledExecutorService}
     * Any clusters that were returned from a previous call, but are not returned in the latest call, will be marked as down
     * @return Observable of clusters returned from a call to {@link #getCurrentClusters()}
     */
    // see https://github.com/ReactiveX/RxJava/issues/448
    // and https://github.com/ReactiveX/RxJava/issues/997
    private Observable<Collection<Cluster>> observeUpdateClusters() {
        return Observable.create((Observable.OnSubscribe<Collection<Cluster>>) s -> {
            Scheduler.Worker worker = Schedulers.newThread().createWorker();
            worker.schedule(
                    new Action0() {
                        @Override
                        public void call() {
                            s.onNext(getCurrentClusters());
                            worker.schedule(this, updateIntervalMs, TimeUnit.MILLISECONDS);
                        }
                    }
            );
            s.add(worker);
        });
    }


    /**
     * Returns the current list of clusters
     *
     * @return current list of {@link Cluster} objects
     */
    protected abstract Collection<Cluster> getCurrentClusters();
}
