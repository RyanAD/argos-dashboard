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
import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

/**
 * Date: 11/8/15
 * Time: 5:58 PM
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterRegistryTest {
    @Mock
    HystrixClusterMonitorFactory monitorFactory;

    @Mock
    HystrixClusterMonitor clusterMonitor1;

    @Mock
    HystrixClusterMonitor clusterMonitor2;

    @Mock
    HystrixClusterMonitor clusterMonitor3;

    @Mock
    ClusterDiscovery clusterDiscovery;

    @Test
    public void testObserve() throws InterruptedException {
        when(clusterDiscovery.getClusters()).thenReturn(Observable.from(Lists.newArrayList(new Cluster("one", "one"), new Cluster("two", "two"))).concatWith(Observable.never()));
        //when(clusterDiscovery.getClusters()).thenReturn(obs);//.concatWith(Observable.never()));
        when(monitorFactory.createMonitor(anyString(), anyString())).thenReturn(clusterMonitor1, clusterMonitor2);

        Observable<HystrixClusterMetrics> metricsObservable1 = Observable.interval(2, TimeUnit.MILLISECONDS)
                .map(l -> HystrixClusterMetrics.Builder.newBuilder("one", "one")
                .addCommandMetrics(new HystrixCommandMetrics("cmd1", 10, 1, 1, 10, 0, 0, 0, 10000D))
                .addCommandMetrics(new HystrixCommandMetrics("cmd2", 11, 1, 1, 10, 0, 0, 0, 10000D)).build())
                .repeat();

        Observable<HystrixClusterMetrics> metricsObservable2 = Observable.interval(2, TimeUnit.MILLISECONDS)
                .map(l -> HystrixClusterMetrics.Builder.newBuilder("two", "two")
                .addCommandMetrics(new HystrixCommandMetrics("cmd1", 10, 1, 1, 10, 0, 0, 0, 10000D))
                .addCommandMetrics(new HystrixCommandMetrics("cmd2", 11, 1, 1, 10, 0, 0, 0, 10000D)).build())
                .repeat();

        when(clusterMonitor1.observe()).thenReturn(metricsObservable1);
        when(clusterMonitor2.observe()).thenReturn(metricsObservable2);

        ClusterRegistry registry = new ClusterRegistry(clusterDiscovery, monitorFactory);
        final Set<String> seenClusters = Sets.newConcurrentHashSet();
        Observable<HystrixClusterMetrics> mergedMetrics = registry.observe();
        mergedMetrics
                .take(100, TimeUnit.MILLISECONDS)
                .toBlocking()
                .forEach(m -> {
                    seenClusters.add(m.getClusterName());
                });
        assertEquals(Sets.newHashSet("one", "two"), seenClusters);
    }

    @Test
    public void testObserve_inactiveCluster() {

        Observable<Cluster> clusterObservable = Observable.interval(1, TimeUnit.MILLISECONDS)
                .flatMap(l -> {
                    if (l.equals(1L)) {
                        return Observable.just(new Cluster("one", "one"));
                    } else if (l.equals(2L)) {
                        return Observable.just(new Cluster("two", "two"));
                    } else if (l.equals(10L)) {
                        return Observable.just(new Cluster("one", "one", false));
                    } else {
                        return Observable.empty();
                    }
                }).concatWith(Observable.never());
        when(clusterDiscovery.getClusters()).thenReturn(clusterObservable);
        //when(clusterDiscovery.getClusters()).thenReturn(obs);//.concatWith(Observable.never()));
        when(monitorFactory.createMonitor(anyString(), anyString())).thenReturn(clusterMonitor1, clusterMonitor2);

        AtomicBoolean obs1Unsub = new AtomicBoolean();
        Observable<HystrixClusterMetrics> metricsObservable1 = Observable.interval(2, TimeUnit.MILLISECONDS)
                .map(l -> HystrixClusterMetrics.Builder.newBuilder("one", "one")
                .addCommandMetrics(new HystrixCommandMetrics("cmd1", 10, 1, 1, 10, 0, 0, 0, 10000D))
                .addCommandMetrics(new HystrixCommandMetrics("cmd2", 11, 1, 1, 10, 0, 0, 0, 10000D)).build())
                .repeat()
                .doOnUnsubscribe(() -> obs1Unsub.set(true));

        AtomicBoolean obs2Unsub = new AtomicBoolean();
        Observable<HystrixClusterMetrics> metricsObservable2 = Observable.interval(2, TimeUnit.MILLISECONDS)
                .map(l -> HystrixClusterMetrics.Builder.newBuilder("two", "two")
                .addCommandMetrics(new HystrixCommandMetrics("cmd1", 10, 1, 1, 10, 0, 0, 0, 10000D))
                .addCommandMetrics(new HystrixCommandMetrics("cmd2", 11, 1, 1, 10, 0, 0, 0, 10000D)).build())
                .repeat()
                .doOnUnsubscribe(() -> obs2Unsub.set(true));

        when(clusterMonitor1.observe()).thenReturn(metricsObservable1);
        when(clusterMonitor2.observe()).thenReturn(metricsObservable2);

        ClusterRegistry registry = new ClusterRegistry(clusterDiscovery, monitorFactory);
        Observable<HystrixClusterMetrics> mergedMetrics = registry.observe();
        mergedMetrics
                .take(100, TimeUnit.MILLISECONDS)
                .toBlocking()
                .forEach(m -> {
                    // noop
                });
        assertTrue(obs1Unsub.get());
        assertFalse(obs2Unsub.get());
    }
}