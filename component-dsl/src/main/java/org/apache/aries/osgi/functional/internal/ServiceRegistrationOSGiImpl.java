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

package org.apache.aries.osgi.functional.internal;

import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Carlos Sierra Andrés
 */
public class ServiceRegistrationOSGiImpl<T>
	extends OSGiImpl<ServiceRegistration<T>> {

	public ServiceRegistrationOSGiImpl(
		Class<T> clazz, Supplier<T> service,
		Supplier<Map<String, ?>> properties) {

		super((bundleContext, op) -> {
			ServiceRegistration<?> serviceRegistration =
				bundleContext.registerService(
					clazz, service.get(), getProperties(properties.get()));

			return getServiceRegistrationOSGiResult(serviceRegistration, op);
		});
	}

	public ServiceRegistrationOSGiImpl(
		Class<T> clazz, ServiceFactory<T> serviceFactory,
		Supplier<Map<String, ?>> properties) {

		super((bundleContext, op) -> {
			ServiceRegistration<?> serviceRegistration =
				bundleContext.registerService(
					clazz, serviceFactory,
					getProperties(properties.get()));

			return getServiceRegistrationOSGiResult(serviceRegistration, op);
		});
	}

	public ServiceRegistrationOSGiImpl(
		String[] clazz, Supplier<Object> service,
		Supplier<Map<String, ?>> properties) {

		super((bundleContext, op) -> {
			ServiceRegistration<?> serviceRegistration =
				bundleContext.registerService(
					clazz, service, new Hashtable<>(properties.get()));

			return getServiceRegistrationOSGiResult(serviceRegistration, op);
		});
	}

	private static Hashtable<String, Object> getProperties(
		Map<String, ?> properties) {

		if (properties == null) {
			return new Hashtable<>();
		}

		return new Hashtable<>(properties);
	}

	private static <T> OSGiResultImpl
		getServiceRegistrationOSGiResult(
		ServiceRegistration<?> serviceRegistration,
		Function<ServiceRegistration<T>, Runnable> op) {

		AtomicReference<Runnable> reference = new AtomicReference<>();

		return new OSGiResultImpl(
            () -> reference.set(
            	(Runnable)((Function)op).apply(serviceRegistration)),
            () -> {
                try {
                    serviceRegistration.unregister();
                }
                catch (Exception e) {
                }
                finally {
                    reference.get().run();
                }
            });
	}

}
