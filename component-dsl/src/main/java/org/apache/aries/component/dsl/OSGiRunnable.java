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

package org.apache.aries.component.dsl;

import org.osgi.framework.BundleContext;

/**
 * @author Carlos Sierra Andrés
 */
public interface OSGiRunnable<T> {

	default OSGiResult run(ExecutionContext executionContext) {
		return run(executionContext, (__) -> () -> {});
	}

	default OSGiResult run(BundleContext bundleContext) {
		return run(new ExecutionContext(bundleContext), (__) -> () -> {});
	}

	OSGiResult run(ExecutionContext executionContext, Publisher<? super T> andThen);

	default OSGiResult run(BundleContext bundleContext, Publisher<? super T> andThen) {
		return run(new ExecutionContext(bundleContext), andThen);
	}

	public class ExecutionContext {
		private BundleContext bundleContext;

		public ExecutionContext(BundleContext bundleContext) {
			this.bundleContext = bundleContext;
		}

		public BundleContext getBundleContext() {
			return bundleContext;
		}
	}

}
