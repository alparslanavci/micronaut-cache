/*
 * Copyright 2017-2019 original authors
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
package io.micronaut.cache.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import io.micronaut.cache.DefaultCacheManager;
import io.micronaut.cache.SyncCache;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.convert.ConversionService;

import javax.annotation.Nonnull;
import javax.inject.Named;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * A {@link io.micronaut.cache.CacheManager} implementation for Hazelcast.
 *
 * @author Nirav Assar
 * @since 1.0.0
 */
@Replaces(DefaultCacheManager.class)
@Primary
public class HazelcastManager implements io.micronaut.cache.CacheManager<IMap<Object, Object>>, Closeable {

    private Map<String, HazelcastSyncCache> cacheMap;
    private final ConversionService<?> conversionService;
    private final ExecutorService executorService;
    private final HazelcastInstance hazelcastClientInstance;

    /**
     * Constructor.
     *
     * @param conversionService convert values that are returned
     * @param hazelcastClientInstance the client instance of hazelcast client
     * @param executorService managers the pool of executors
     */
    public HazelcastManager(ConversionService<?> conversionService,
                            HazelcastInstance hazelcastClientInstance,
                            @Named("io") ExecutorService executorService) {
        this.conversionService = conversionService;
        this.executorService = executorService;
        this.cacheMap = new HashMap<>();
        this.hazelcastClientInstance = hazelcastClientInstance;
    }

    @Nonnull
    @Override
    public Set<String> getCacheNames() {
        Set<String> names = new HashSet<String>();
        names.add(this.hazelcastClientInstance.getName());
        return names;
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public SyncCache<IMap<Object, Object>> getCache(String name) {
        HazelcastSyncCache syncCache = this.cacheMap.get(name);
        if (syncCache == null) {
            IMap<Object, Object> nativeCache = hazelcastClientInstance.getMap(name);
            syncCache = new HazelcastSyncCache(conversionService, nativeCache, executorService);
            this.cacheMap.put(name, syncCache);
        }
        return syncCache;
    }

    @Override
    public void close() throws IOException {
        hazelcastClientInstance.shutdown();
    }
}
