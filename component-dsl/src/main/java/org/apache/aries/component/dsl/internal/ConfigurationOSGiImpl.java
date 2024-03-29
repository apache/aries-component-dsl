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
import org.apache.aries.component.dsl.configuration.ConfigurationHolder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Carlos Sierra Andrés
 */
public class ConfigurationOSGiImpl extends OSGiImpl<ConfigurationHolder> {

    public ConfigurationOSGiImpl(String pid) {
        super((executionContext, op) -> {
            AtomicReference<Configuration> atomicReference =
                new AtomicReference<>(null);

            AtomicReference<OSGiResult>
                terminatorAtomicReference = new AtomicReference<>(
                    new OSGiResultImpl(NOOP, () -> false));

            AtomicBoolean closed = new AtomicBoolean();

            AtomicLong initialCounter = new AtomicLong();

            CountDownLatch countDownLatch = new CountDownLatch(1);

            BundleContext bundleContext = executionContext.getBundleContext();

            ServiceRegistration<?> serviceRegistration =
                bundleContext.registerService(
                    ConfigurationListener.class,
                    (ConfigurationEvent configurationEvent) -> {
                        if (configurationEvent.getFactoryPid() != null ||
                            !configurationEvent.getPid().equals(pid)) {

                            return;
                        }

                        try {
                            countDownLatch.await(1, TimeUnit.MINUTES);
                        }
                        catch (InterruptedException e) {
                            return;
                        }

                        Configuration configuration;

                        if (configurationEvent.getType() ==
                            ConfigurationEvent.CM_DELETED) {

                            atomicReference.set(null);

                            signalLeave(terminatorAtomicReference);
                        }
                        else {
                             configuration = getConfiguration(
                                 bundleContext, configurationEvent);

                            if (configuration == null ||
                                configuration.getChangeCount() == initialCounter.get()) {

                                return;
                            }

                            if (atomicReference.get() == null) {
                                atomicReference.set(configuration);
                            }
                            else {
                                if (!UpdateSupport.sendUpdate(terminatorAtomicReference.get())) {
                                    return;
                                }
                            }

                            UpdateSupport.runUpdate(() -> {
                                signalLeave(terminatorAtomicReference);

                                terminatorAtomicReference.set(
                                    op.apply(new ConfigurationHolderImpl(configuration)));

                            });

                            if (closed.get()) {
                                /*
                                if we have closed while executing the
                                effects we have to execute the terminator
                                directly instead of storing it
                                */
                                signalLeave(terminatorAtomicReference);
                            }
                        }
                    },
                    new Hashtable<>());

            ServiceReference<ConfigurationAdmin> serviceReference =
                bundleContext.getServiceReference(ConfigurationAdmin.class);

            if (serviceReference != null) {
                Configuration configuration = getConfiguration(
                    bundleContext, pid, serviceReference);

                if (configuration != null) {
                    atomicReference.set(configuration);

                    initialCounter.set(configuration.getChangeCount());

                    terminatorAtomicReference.set(
                        op.apply(new ConfigurationHolderImpl(configuration)));
                }
            }

            countDownLatch.countDown();

            return new OSGiResultImpl(
                () -> {
                    closed.set(true);

                    serviceRegistration.unregister();

                    signalLeave(terminatorAtomicReference);
                },
                () -> terminatorAtomicReference.get().update())
            ;
        });
    }

    private static Configuration getConfiguration(
        BundleContext bundleContext, ConfigurationEvent configurationEvent) {

        String pid = configurationEvent.getPid();

        ServiceReference<ConfigurationAdmin> reference =
            configurationEvent.getReference();

        return getConfiguration(bundleContext, pid, reference);
    }

    private static Configuration getConfiguration(
        BundleContext bundleContext, String pid,
        ServiceReference<ConfigurationAdmin> reference) {

        ConfigurationAdmin configurationAdmin = bundleContext.getService(
            reference);

        try {
            Configuration[] configurations =
                configurationAdmin.listConfigurations(
                    "(&(service.pid=" + pid + ")(!(service.factoryPid=*)))");

            if (configurations == null || configurations.length == 0) {
                return null;
            }

            return configurations[0];
        }
        catch (Exception e) {
            return null;
        }
        finally {
            bundleContext.ungetService(reference);
        }
    }

    private static void signalLeave(
        AtomicReference<OSGiResult> terminatorAtomicReference) {

        OSGiResult old = terminatorAtomicReference.getAndSet(null);

        if (old != null) {
            old.run();
        }
    }

}
