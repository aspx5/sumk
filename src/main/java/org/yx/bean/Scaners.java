/**
 * Copyright (C) 2016 - 2030 youtongluan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.yx.bean;

import java.util.Objects;
import java.util.function.Supplier;

import org.yx.db.sql.PojoMetaListener;

public final class Scaners {

	private static Supplier<BeanEventListener[]> supplier = () -> new BeanEventListener[] { new BeanFactory(),
			new PojoMetaListener() };

	public static void setSupplier(Supplier<BeanEventListener[]> supplier) {
		Scaners.supplier = Objects.requireNonNull(supplier);
	}

	public static Supplier<BeanEventListener[]> supplier() {
		return supplier;
	}

}
