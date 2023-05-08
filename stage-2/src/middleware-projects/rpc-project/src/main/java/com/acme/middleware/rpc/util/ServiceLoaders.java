/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.acme.middleware.rpc.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ServiceLoader} 工具类
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public abstract class ServiceLoaders {

    private static final Map<String, List<?>> loadedServices = new ConcurrentHashMap<>();

    public static <T> T loadDefault(Class<T> serviceClass) {
        return ServiceLoader.load(serviceClass).iterator().next();
    }

    public static <T> List<T> loadAll(Class<T> serviceClass) {
        synchronized (loadedServices) {
            if (loadedServices.containsKey(serviceClass.getName()))
                return (List<T>) loadedServices.get(serviceClass.getName());
        }

        Iterator<T> serviceLoader = ServiceLoader.load(serviceClass).iterator();
        if (!serviceLoader.hasNext())
            return Collections.emptyList();

        List<T> services = new ArrayList<>();
        while (serviceLoader.hasNext())
            services.add(serviceLoader.next());

        synchronized (loadedServices) {
            loadedServices.put(serviceClass.getName(), services);
        }
        return services;
    }
}
