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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Connects to a turbine stream and aggregates data from multiple hystrix commands within a single hystrix cluster.
 */
public final class DefaultHystrixClusterMonitor implements HystrixClusterMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultHystrixClusterMonitor.class);
    private static final ObjectMapper om = new ObjectMapper();
    private final String clusterName;
    private final URI uri;

    private final Cache<String, HystrixCommandMetrics> commandCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build();

    private Observable<String> jsonObservable;

    private volatile Observable<HystrixClusterMetrics> observable = null;

    public DefaultHystrixClusterMonitor(String clusterName, String streamUrl) {
        this.clusterName = clusterName;
        this.uri = URI.create(streamUrl);
    }

    @Override
    public Observable<HystrixClusterMetrics> observe() {
        if(observable != null) {
            return observable;
        }

        observable = observeJson()
                .filter(s -> s.contains("HystrixCommand") && s.contains("latencyExecute"))// we get multiple event types, make sure this is a HystrixCommand event
                .map(this::jsonToMetrics)
                .filter(Objects::nonNull)
                .doOnEach(n -> {
                    HystrixCommandMetrics metrics = (HystrixCommandMetrics) n.getValue();
                    if(n.getThrowable() != null) {
                        LOG.warn("Error processing metrics", n.getThrowable());
                    } else if(metrics != null) {
                        commandCache.put(metrics.getName(), metrics);
                    }
                })
                .sample(1, TimeUnit.SECONDS) // generate metrics once per second
                .map(f -> this.generateMetrics())
                .takeUntil(f -> observable == null)
                .retry((i, t) -> {
                    LOG.error("Error streaming from server", t);
                    return true;
                });

        return observable;
    }

    private HystrixClusterMetrics generateMetrics() {
        HystrixClusterMetrics.Builder metricsBuilder = new HystrixClusterMetrics.Builder(clusterName, uri.toASCIIString());

        commandCache.asMap().values()
                .stream()
                .forEach(metricsBuilder::addCommandMetrics);

        return metricsBuilder.build();
    }

    private HystrixCommandMetrics jsonToMetrics(String json) {
        HystrixCommandMetrics metrics = null;
        try {
            metrics = om.readValue(json, HystrixCommandMetrics.class);
        } catch (Exception e) {
            LOG.warn("Exception parsing json", e);
        }
        return metrics;
    }

    @Override
    public Observable<String> observeJson() {
        if(jsonObservable != null) {
            return jsonObservable;
        }

        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet(uri.getPath() + "?" + uri.getQuery());
        // TODO, do we ever need to call shutdown on this client?
        HttpClient<ByteBuf, ServerSentEvent> client = RxNetty.<ByteBuf, ServerSentEvent>newHttpClientBuilder(uri.getHost(), uri.getPort())
                .withNoConnectionPooling()
                .pipelineConfigurator(PipelineConfigurators.<ByteBuf>clientSseConfigurator())
                .build();


        jsonObservable = client.submit(request)
                .doOnError(t -> LOG.error("Error connecting to " + uri, t))
                .flatMap(response -> {
                            if (response.getStatus().code() != 200) {
                                return Observable.error(new RuntimeException("Failed to connect: " + response.getStatus()));
                            }

                            return response.getContent()
                                    .doOnSubscribe(() -> LOG.info("Turbine => Aggregate Stream from URI: " + uri))
                                    .doOnUnsubscribe(() -> LOG.info("Turbine => Unsubscribing Stream: " + uri))
                                    .map(ServerSentEvent::contentAsString);
                        }
                )
                .timeout(120, TimeUnit.SECONDS)
                .retryWhen(attempts -> attempts.zipWith(Observable.range(1, Integer.MAX_VALUE), (k, i) -> i)
                        .flatMap(n -> {
                            int waitTimeSeconds = Math.min(6, n) * 10; // wait in 10 second increments up to a max of 1 minute
                            LOG.info("Turbine => Retrying connection to: " + this.uri + " in {} seconds", waitTimeSeconds);
                            return Observable.timer(waitTimeSeconds, TimeUnit.SECONDS);
                        })
                )
                .repeat()
                .share();

        return jsonObservable;
    }

}
