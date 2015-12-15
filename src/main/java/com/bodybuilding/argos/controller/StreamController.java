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

package com.bodybuilding.argos.controller;

import com.bodybuilding.argos.discovery.ClusterRegistry;
import com.bodybuilding.argos.discovery.HystrixClusterMetrics;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import rx.Observable;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Emits Server Sent Events for the Argos dashboard.
 */
@RestController
public class StreamController {
    private final Observable<String> streamObservable;

    @Autowired
    public StreamController(ClusterRegistry registry, Observable<Boolean> shutdown) {
        Objects.requireNonNull(registry);
        Objects.requireNonNull(shutdown);
        ObjectMapper om = new ObjectMapper();
        om.enable(MapperFeature.AUTO_DETECT_FIELDS);
        om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);


        Observable<HystrixClusterMetrics> metricsObs = registry.observe();

        streamObservable = metricsObs
                .takeUntil(shutdown)
                .map(d -> {
                    try {
                        return om.writeValueAsString(d);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .share();
    }

    @RequestMapping("/cluster.stream")
    public SseEmitter streamMetrics() {
        final SseEmitter emitter = new SseEmitter(TimeUnit.DAYS.toMillis(45));
        SseEmitterUtil.bindObservable(emitter, streamObservable);
        return emitter;
    }
}
