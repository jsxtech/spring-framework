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

package org.springframework.web.method.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ui.ModelMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link ModelAndViewContainer}.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
class ModelAndViewContainerTests {

	private ModelAndViewContainer mavContainer;


	@BeforeEach
	void setup() {
		this.mavContainer = new ModelAndViewContainer();
	}


	@Test
	void getModel() {
		this.mavContainer.addAttribute("name", "value");
		assertThat(this.mavContainer.getModel()).hasSize(1);
		assertThat(this.mavContainer.getModel().get("name")).isEqualTo("value");
	}

	@Test
	void redirectScenarioWithRedirectModel() {
		this.mavContainer.addAttribute("name1", "value1");
		this.mavContainer.setRedirectModel(new ModelMap("name2", "value2"));
		this.mavContainer.setRedirectModelScenario(true);

		assertThat(this.mavContainer.getModel()).hasSize(1);
		assertThat(this.mavContainer.getModel().get("name2")).isEqualTo("value2");
	}

	@Test
	void ignoreDefaultModel() {
		this.mavContainer.addAttribute("name", "value");
		this.mavContainer.setRedirectModelScenario(true);

		assertThat(this.mavContainer.getModel()).isEmpty();
	}

	@Test  // SPR-14045
	public void ignoreDefaultModelAndWithoutRedirectModel() {
		this.mavContainer.setRedirectModelScenario(true);
		this.mavContainer.addAttribute("name", "value");

		assertThat(this.mavContainer.getModel()).hasSize(1);
		assertThat(this.mavContainer.getModel().get("name")).isEqualTo("value");
	}


}
