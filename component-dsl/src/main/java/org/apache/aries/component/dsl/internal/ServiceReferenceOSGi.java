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
import org.apache.aries.component.dsl.CachingServiceReference;
import org.apache.aries.component.dsl.Publisher;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author Carlos Sierra Andrés
 */
public class ServiceReferenceOSGi<T>
    extends OSGiImpl<CachingServiceReference<T>> {

    public ServiceReferenceOSGi(Class<T> clazz, String filterString) {

        super((executionContext, op) -> {
            ServiceTracker<T, Tracked<T>>
                serviceTracker = new ServiceTracker<>(
                    executionContext.getBundleContext(),
                    buildFilter(executionContext, filterString, clazz),
                    new DefaultServiceTrackerCustomizer<>(op));

            serviceTracker.open();

            return new OSGiResultImpl(
                serviceTracker::close,
                () -> serviceTracker.getTracked().values().stream().map(
                    tracked -> tracked.runnable.update()
                ).reduce(
                    Boolean.FALSE, Boolean::logicalOr
                )
            );
        });
    }

    private static class DefaultServiceTrackerCustomizer<T>
        implements ServiceTrackerCustomizer<T, Tracked<T>> {

        public DefaultServiceTrackerCustomizer(
            Publisher<? super CachingServiceReference<T>> addedSource) {

            _addedSource = addedSource;
        }

        @Override
        public Tracked<T> addingService(ServiceReference<T> reference) {
            CachingServiceReference<T> cachingServiceReference =
                new CachingServiceReference<>(reference);

            return new Tracked<>(
                cachingServiceReference, _addedSource.apply(cachingServiceReference));
        }

        @Override
        public void modifiedService(
            ServiceReference<T> reference, Tracked<T> tracked) {

            if (UpdateSupport.sendUpdate(tracked.runnable)) {
                UpdateSupport.runUpdate(() -> {
                    tracked.runnable.run();
                    tracked.cachingServiceReference = new CachingServiceReference<>(
                        reference);
                    tracked.runnable =
                        _addedSource.apply(tracked.cachingServiceReference);
                });
            }
        }

        @Override
        public void removedService(
            ServiceReference<T> reference, Tracked<T> tracked) {

            tracked.runnable.run();
        }

        private final Publisher<? super CachingServiceReference<T>> _addedSource;

    }

    private static class Tracked<T> {

        public Tracked(
            CachingServiceReference<T> cachingServiceReference,
            OSGiResult osgiResult) {

            this.cachingServiceReference = cachingServiceReference;
            this.runnable = osgiResult;
        }

        volatile CachingServiceReference<T> cachingServiceReference;
        volatile OSGiResult runnable;

    }
}
