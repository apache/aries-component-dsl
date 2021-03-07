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

import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;

import java.util.function.Predicate;

/**
 * @author Carlos Sierra Andrés
 */
public class RefreshWhenOSGi<T> extends OSGiImpl<T> {

    public RefreshWhenOSGi(OSGi<T> program, Predicate<T> refresher) {
        super((executionContext, op) -> {
            OSGiResult result = program.run(
                executionContext,
                op.pipe(
                    t -> {
                        OSGiResult osgiResult = op.publish(t);

                        return new OSGiResultImpl(
                            osgiResult::close,
                            () -> {
                                if (refresher.test(t)) {
                                    return true;
                                }

                                return osgiResult.update();
                            }
                        );
                    }
                ));

            return new OSGiResultImpl(result::close, () -> false);
        });
    }

}
