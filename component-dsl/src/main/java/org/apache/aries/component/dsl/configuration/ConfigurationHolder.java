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

package org.apache.aries.component.dsl.configuration;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

public interface ConfigurationHolder {

    Dictionary<String, ?> getProperties();

    long getChangeCount();

    long getUpdatedChangeCount();

    Dictionary<String, ?> getUpdatedProperties();

    public static ConfigurationHolder fromMap(Map<String, ?> map) {
        return new ConfigurationHolder() {

            final Hashtable<String, ?> properties = new Hashtable<>(map);

            @Override
            public Dictionary<String, ?> getProperties() {
                return new Hashtable<>(properties);
            }

            @Override
            public long getChangeCount() {
                return 0;
            }

            @Override
            public long getUpdatedChangeCount() {
                return getChangeCount();
            }

            @Override
            public Dictionary<String, ?> getUpdatedProperties() {
                return getProperties();
            }
        };
    }
}
