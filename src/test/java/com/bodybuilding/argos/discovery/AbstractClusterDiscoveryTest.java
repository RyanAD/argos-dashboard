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
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * Date: 12/2/15
 * Time: 2:38 PM
 */
public class AbstractClusterDiscoveryTest {

    @Test
    public void testGetCurrentClusters() throws Exception {
        final AtomicInteger updateCount = new AtomicInteger();
        AbstractClusterDiscovery discovery = new AbstractClusterDiscovery(50, TimeUnit.MILLISECONDS) {
            @Override
            protected Collection<Cluster> getCurrentClusters() {
                int numUpdates = updateCount.incrementAndGet();
                if(numUpdates < 4) {
                    return Lists.newArrayList(new Cluster("one", "one"), new Cluster("two", "two"), new Cluster("three", "three"));
                } else if(numUpdates < 7) {
                    return Lists.newArrayList(new Cluster("one", "one"), new Cluster("two", "two"), new Cluster("three", "three", false));
                } else {
                    return Lists.newArrayList(new Cluster("two", "two"));
                }
            }
        };

        ArrayList<Cluster> expected = Lists.newArrayList(
                new Cluster("one", "one"),
                new Cluster("two", "two"),
                new Cluster("three", "three"),
                new Cluster("three", "three", false),
                new Cluster("one", "one", false));


        ArrayList<Object> actual = discovery.getClusters()
                .take(500, TimeUnit.MILLISECONDS)
                .collect(Lists::newArrayList, ArrayList::add)
                .toBlocking()
                .first();

        assertEquals(expected, actual);
    }
}