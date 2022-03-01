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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * @author Carlos Sierra Andrés
 */
public class OSGiResultImpl implements OSGiResult {

    public OSGiResultImpl(Runnable close, Supplier<Boolean> onUpdate) {
        this.close = close;
        this.onUpdate = onUpdate;
    }

    @Override
    public void close() {
        if (_closed.compareAndSet(false, true)) {
            close.run();
        }
    }

    @Override
    public boolean update() {
        if (_closed.get()) {
            return false;
        }

        return onUpdate.get();
    }

    private final Runnable close;
    private Supplier<Boolean> onUpdate;
    private AtomicBoolean _closed = new AtomicBoolean();

}
