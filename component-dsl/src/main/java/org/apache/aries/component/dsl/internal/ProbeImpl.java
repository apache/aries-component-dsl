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
import org.apache.aries.component.dsl.OSGiRunnable;
import org.apache.aries.component.dsl.Publisher;

/**
 * @author Carlos Sierra Andrés
 */
public class ProbeImpl<T> extends BaseOSGiImpl<T> {

    public ProbeImpl() {
        super(new ProbeOperationImpl<>());
    }

    public Publisher<? super T> getPublisher() {
        return ((ProbeOperationImpl<T>) _operation)._op;
    }

    private static class ProbeOperationImpl<T> implements OSGiRunnable<T> {

        private volatile OSGiResult _onClose = NOOP;

        @Override
        public OSGiResultImpl run(
            ExecutionContext executionContext, Publisher<? super T> op) {
            _op = op;

            return new OSGiResultImpl(
                () -> {_onClose.close(); _onClose = NOOP;},
                () -> _onClose.update()
            );
        }

        Publisher<? super T> _op;
    }

    public void onClose(OSGiResult onClose) {
        ((ProbeOperationImpl<T>) _operation)._onClose = onClose;
    }
}
