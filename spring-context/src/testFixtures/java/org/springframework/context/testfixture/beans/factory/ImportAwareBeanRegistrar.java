/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.testfixture.beans.factory;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

public class ImportAwareBeanRegistrar implements BeanRegistrar, ImportAware {

	@Nullable
	private AnnotationMetadata importMetadata;

	@Override
	public void register(BeanRegistry registry, Environment env) {
		registry.registerBean(ClassNameHolder.class, spec -> spec.supplier(context ->
				new ClassNameHolder(this.importMetadata == null ? null : this.importMetadata.getClassName())));
	}

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.importMetadata = importMetadata;
	}

	public @Nullable AnnotationMetadata getImportMetadata() {
		return this.importMetadata;
	}

	public record ClassNameHolder(@Nullable String className) {}
}
