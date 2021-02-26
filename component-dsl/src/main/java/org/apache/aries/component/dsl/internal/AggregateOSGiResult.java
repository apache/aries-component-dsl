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

import org.apache.aries.component.dsl.OSGiResult;
import org.apache.aries.component.dsl.update.UpdateSelector;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Carlos Sierra Andrés
 */
public class AggregateOSGiResult implements OSGiResult {

    private OSGiResult[] results;

    public AggregateOSGiResult(OSGiResult ... results) {
        this.results = results;
    }

    @Override
    public void close() {
        if (_closed.compareAndSet(false, true)) {
            for (OSGiResult result : results) {
                try {
                    result.close();
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    public boolean update(UpdateSelector updateSelector) {
        boolean bool = false;

        if (!_closed.get()) {
            for (OSGiResult result : results) {
                try {
                    bool |= result.update(updateSelector);
                } catch (Exception e) {
                }
            }
        }

        return bool;
    }

    private final AtomicBoolean _closed = new AtomicBoolean();

}
