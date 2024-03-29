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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * @author Carlos Sierra Andrés
 */
public class BundleOSGi extends OSGiImpl<Bundle> {

    public BundleOSGi(int stateMask) {
        super((executionContext, op) -> {

            BundleTracker<OSGiResult> bundleTracker =
                new BundleTracker<>(
                    executionContext.getBundleContext(), stateMask,
                    new BundleTrackerCustomizer<OSGiResult>() {

                        @Override
                        public OSGiResult addingBundle(
                            Bundle bundle, BundleEvent bundleEvent) {

                            return op.apply(bundle);
                        }

                        @Override
                        public void modifiedBundle(
                            Bundle bundle, BundleEvent bundleEvent,
                            OSGiResult osgiResult) {

                            osgiResult.update();
                        }

                        @Override
                        public void removedBundle(
                            Bundle bundle, BundleEvent bundleEvent,
                            OSGiResult osgiResult) {

                            osgiResult.run();
                        }
                    });

            bundleTracker.open();

            return new OSGiResultImpl(
                bundleTracker::close,
                () -> bundleTracker.getTracked().values().stream().map(
                    OSGiResult::update
                ).reduce(
                    Boolean.FALSE, Boolean::logicalOr
                )
            );
        });

    }

}
