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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Carlos Sierra Andrés
 */
public class ConfigurationsOSGiImpl extends OSGiImpl<ConfigurationHolder> {

    public ConfigurationsOSGiImpl(String factoryPid) {
        super((executionContext, op) -> {
            ConcurrentHashMap<String, Long> configurationCounters =
                new ConcurrentHashMap<>();

            ConcurrentHashMap<String, OSGiResult> terminators =
                new ConcurrentHashMap<>();

            AtomicBoolean closed = new AtomicBoolean();

            CountDownLatch countDownLatch = new CountDownLatch(1);

            final BundleContext bundleContext = executionContext.getBundleContext();

            ServiceRegistration<?> serviceRegistration =
                bundleContext.registerService(
                    ConfigurationListener.class,
                    (ConfigurationEvent configurationEvent) -> {
                        String incomingFactoryPid =
                            configurationEvent.getFactoryPid();

                        if (!factoryPid.equals(incomingFactoryPid)) {
                            return;
                        }

                        try {
                            countDownLatch.await(1, TimeUnit.MINUTES);
                        }
                        catch (InterruptedException e) {
                            return;
                        }

                        String pid = configurationEvent.getPid();

                        Configuration configuration;

                        if (configurationEvent.getType() ==
                            ConfigurationEvent.CM_DELETED) {

                            configurationCounters.remove(pid);

                            signalLeave(pid, terminators);
                        }
                        else {
                            configuration = getConfiguration(
                                bundleContext, configurationEvent);

                            Long oldChangeCount = configurationCounters.putIfAbsent(
                                pid, configuration.getChangeCount());

                            if (oldChangeCount != null) {
                                if (oldChangeCount == configuration.getChangeCount()) {
                                    return;
                                }

                                OSGiResult osgiResult = terminators.get(pid);

                                if (osgiResult != null && !UpdateSupport.sendUpdate(osgiResult)) {
                                    return;
                                }
                            }

                            UpdateSupport.runUpdate(() -> {
                                signalLeave(pid, terminators);

                                terminators.put(
                                    pid, op.apply(new ConfigurationHolderImpl(configuration)));
                            });

                            if (closed.get()) {
                            /*
                            if we have closed while executing the
                            effects we have to execute the terminator
                            directly instead of storing it
                            */
                                signalLeave(pid, terminators);
                            }
                        }
                    },
                    new Hashtable<>());

            ServiceReference<ConfigurationAdmin> serviceReference =
                bundleContext.getServiceReference(ConfigurationAdmin.class);

            if (serviceReference != null) {
                Configuration[] configurations = getConfigurations(
                    bundleContext, factoryPid, serviceReference);

                for (Configuration configuration : configurations) {
                    configurationCounters.put(
                        configuration.getPid(), configuration.getChangeCount());

                    terminators.put(
                        configuration.getPid(),
                        op.publish(new ConfigurationHolderImpl(configuration)));
                }
            }

            countDownLatch.countDown();

            return new OSGiResultImpl(
                () -> {
                    closed.set(true);

                    serviceRegistration.unregister();

                    for (Runnable runnable : terminators.values()) {
                        if (runnable != null) {
                            runnable.run();
                        }
                    }
                },
                () -> terminators.values().stream().map(
                    OSGiResult::update
                ).reduce(
                    Boolean.FALSE, Boolean::logicalOr
                ));
        });
    }

    private static Configuration getConfiguration(
        BundleContext bundleContext, ConfigurationEvent configurationEvent) {

        String pid = configurationEvent.getPid();
        String factoryPid = configurationEvent.getFactoryPid();

        ServiceReference<ConfigurationAdmin> reference =
            configurationEvent.getReference();

        return getConfiguration(bundleContext, pid, factoryPid, reference);
    }

    private static Configuration getConfiguration(
        BundleContext bundleContext, String pid, String factoryPid,
        ServiceReference<ConfigurationAdmin> reference) {

        ConfigurationAdmin configurationAdmin = bundleContext.getService(
            reference);

        try {
            Configuration[] configurations =
                configurationAdmin.listConfigurations(
                    "(&(service.pid=" + pid + ")" +
                        "(service.factoryPid="+ factoryPid + "))");

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

    private static Configuration[] getConfigurations(
        BundleContext bundleContext, String factoryPid,
        ServiceReference<ConfigurationAdmin> serviceReference) {

        ConfigurationAdmin configurationAdmin = bundleContext.getService(
            serviceReference);

        try {
            Configuration[] configurations =
                configurationAdmin.listConfigurations(
                    "(&(service.pid=*)(service.factoryPid="+ factoryPid +"))");

            if (configurations == null) {
                return new Configuration[0];
            }

            return configurations;
        }
        catch (Exception e) {
            return new Configuration[0];
        }
        finally {
            bundleContext.ungetService(serviceReference);
        }
    }

    private static void signalLeave(
        String factoryPid, ConcurrentHashMap<String, OSGiResult> terminators) {

        OSGiResult osgiResult = terminators.remove(factoryPid);

        if (osgiResult != null) {
            osgiResult.run();
        }
    }

}
