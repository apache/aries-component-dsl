/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.component.dsl.internal;

import org.apache.aries.component.dsl.configuration.ConfigurationHolder;
import org.osgi.service.cm.Configuration;

import java.util.Dictionary;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class ConfigurationHolderImpl implements ConfigurationHolder {

    private Configuration configuration;
    private AtomicReference<Dictionary<String, ?>> properties;
    private AtomicLong changeCount = new AtomicLong(-1);

    public ConfigurationHolderImpl(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String getPid() {
        return configuration.getPid();
    }

    @Override
    public Dictionary<String, ?> getProperties() {
        properties.compareAndSet(null, configuration.getProperties());

        return properties.get();
    }

    @Override
    public String getFactoryPid() {
        return configuration.getFactoryPid();
    }

    @Override
    public long getChangeCount() {
        changeCount.compareAndSet(-1, configuration.getChangeCount());

        return changeCount.get();
    }

    @Override
    public void refresh() {
        changeCount.set(-1);

        properties.set(null);
    }

    @Override
    public long getUpdatedChangeCount() {
        return configuration.getChangeCount();
    }

    @Override
    public Dictionary<String, ?> getUpdatedProperties() {
        return configuration.getProperties();
    }

}
