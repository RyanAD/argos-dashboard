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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Date: 11/7/15
 * Time: 1:21 PM
 */
@RunWith(MockitoJUnitRunner.class)
public class HystrixClusterMetricsTest {
    @Mock
    HystrixCommandMetrics commandMetrics;

    @Test
    public void testAddCommandMetrics() {

        when(commandMetrics.getErrorPercentage()).thenReturn(10D);
        when(commandMetrics.getFailed()).thenReturn(4);
        when(commandMetrics.getName()).thenReturn("cmd1");
        when(commandMetrics.getRejected()).thenReturn(1);
        when(commandMetrics.getReportingHosts()).thenReturn(10);
        when(commandMetrics.getRequestRate()).thenReturn(10D);
        when(commandMetrics.getRequests()).thenReturn(100);
        when(commandMetrics.getShortCircuited()).thenReturn(2);
        when(commandMetrics.getSuccess()).thenReturn(50);
        when(commandMetrics.getTimedOut()).thenReturn(10);

        HystrixClusterMetrics.Builder builder = HystrixClusterMetrics.Builder.newBuilder("test", "testStream");

        builder.addCommandMetrics(commandMetrics);

        reset(commandMetrics);

        when(commandMetrics.getErrorPercentage()).thenReturn(4D);
        when(commandMetrics.getFailed()).thenReturn(2);
        when(commandMetrics.getName()).thenReturn("cmd2");
        when(commandMetrics.getRejected()).thenReturn(1);
        when(commandMetrics.getReportingHosts()).thenReturn(11);
        when(commandMetrics.getRequestRate()).thenReturn(20D);
        when(commandMetrics.getRequests()).thenReturn(200);
        when(commandMetrics.getShortCircuited()).thenReturn(3);
        when(commandMetrics.getSuccess()).thenReturn(190);
        when(commandMetrics.getTimedOut()).thenReturn(5);

        builder.addCommandMetrics(commandMetrics);

        HystrixClusterMetrics metrics = builder.build();
        assertEquals(6, metrics.getFailCount());
        assertEquals(2, metrics.getRejectedCount());
        assertEquals(11, metrics.getReportingHosts());
        assertEquals(30D, metrics.getRequestRate(), .05D);
        assertEquals(300, metrics.getRequestCount());
        assertEquals(5, metrics.getShortCircuitedCount());
        assertEquals(240, metrics.getSuccessCount());
        assertEquals(15, metrics.getTimeoutCount());
        assertEquals(7D, metrics.getErrorPercentage(), .05D);
    }

}