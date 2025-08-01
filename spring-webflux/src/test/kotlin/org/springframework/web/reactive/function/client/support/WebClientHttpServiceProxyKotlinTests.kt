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
package org.springframework.web.reactive.function.client.support

import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient
import org.springframework.web.util.DefaultUriBuilderFactory
import org.springframework.web.util.UriBuilderFactory
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.net.URI
import java.time.Duration

/**
 * Kotlin integration tests for [HTTP Service proxy][HttpServiceProxyFactory]
 * using [WebClient] and [MockWebServer].
 *
 * @author DongHyeon Kim
 * @author Sebastien Deleuze
 * @author Olga Maciaszek-Sharma
 */
class KotlinWebClientHttpServiceGroupAdapterServiceProxyTests {

	private lateinit var server: MockWebServer

	private lateinit var anotherServer: MockWebServer

	@BeforeEach
	fun setUp() {
		server = MockWebServer()
		server.start()
		anotherServer = anotherServer()
		anotherServer.start()
	}

	@AfterEach
	fun shutdown() {
		server.close()
		anotherServer.close()
	}

	@Test
	fun greetingSuspending() {
		prepareResponse { it.setHeader("Content-Type", "text/plain").body("Hello Spring!") }
		runBlocking {
			val greeting = initHttpService().getGreetingSuspending()
			assertThat(greeting).isEqualTo("Hello Spring!")
		}
	}

	@Test
	fun greetingMono() {
		prepareResponse { it.setHeader("Content-Type", "text/plain").body("Hello Spring!") }
		StepVerifier.create(initHttpService().getGreetingMono())
			.expectNext("Hello Spring!")
			.expectComplete()
			.verify(Duration.ofSeconds(5))
	}

	@Test
	fun greetingBlocking() {
		prepareResponse { it.setHeader("Content-Type", "text/plain").body("Hello Spring!") }
		val greeting = initHttpService().getGreetingBlocking()
		assertThat(greeting).isEqualTo("Hello Spring!")
	}

	@Test
	fun greetingSuspendingWithRequestAttribute() {
		val attributes: MutableMap<String, Any> = HashMap()
		val webClient = WebClient.builder()
			.baseUrl(server.url("/").toString())
			.filter { request: ClientRequest, next: ExchangeFunction ->
				attributes.putAll(request.attributes())
				next.exchange(request)
			}
			.build()
		prepareResponse { it.setHeader("Content-Type", "text/plain").body("Hello Spring!") }
		val service = initHttpService(webClient)
		runBlocking {
			val greeting = service.getGreetingSuspendingWithAttribute("myAttributeValue")
			assertThat(greeting).isEqualTo("Hello Spring!")
			assertThat(attributes).containsEntry("myAttribute", "myAttributeValue")
		}
	}

	@Test
	@Throws(InterruptedException::class)
	fun getWithFactoryPathVariableAndRequestParam() {
		prepareResponse { it.setHeader("Content-Type", "text/plain").body("Hello Spring!") }
		val factory: UriBuilderFactory = DefaultUriBuilderFactory(anotherServer.url("/").toString())

		val actualResponse: ResponseEntity<String> =
			initHttpService().getWithUriBuilderFactory(factory, "123", "test")

		val request = anotherServer.takeRequest()
		assertThat(actualResponse.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(actualResponse.body).isEqualTo("Hello Spring 2!")
		assertThat(request.method).isEqualTo("GET")
		assertThat(request.target).isEqualTo("/greeting/123?param=test")
		assertThat(server.requestCount).isEqualTo(0)
	}

	@Test
	@Throws(InterruptedException::class)
	fun getWithIgnoredUriBuilderFactory() {
		prepareResponse {
			it.setHeader("Content-Type", "text/plain").body("Hello Spring!") }
		val dynamicUri = server.url("/greeting/123").toUri()
		val factory: UriBuilderFactory = DefaultUriBuilderFactory(anotherServer.url("/").toString())

		val actualResponse: ResponseEntity<String> =
			initHttpService().getWithIgnoredUriBuilderFactory(dynamicUri, factory)

		val request = server.takeRequest()
		assertThat(actualResponse.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(actualResponse.body).isEqualTo("Hello Spring!")
		assertThat(request.method).isEqualTo("GET")
		assertThat(request.target).isEqualTo("/greeting/123")
		assertThat(anotherServer.requestCount).isEqualTo(0)
	}


	private fun initHttpService(): TestHttpService {
		val webClient = WebClient.builder().baseUrl(server.url("/").toString()).build()
		return initHttpService(webClient)
	}

	private fun initHttpService(webClient: WebClient): TestHttpService {
		val adapter = WebClientAdapter.create(webClient)
		return HttpServiceProxyFactory.builderFor(adapter).build().createClient()
	}

	private fun prepareResponse(f: (MockResponse.Builder) -> MockResponse.Builder) {
		val builder = MockResponse.Builder()
		server.enqueue(f.invoke(builder).build())
	}

	private fun anotherServer(): MockWebServer {
		val anotherServer = MockWebServer()
		val response = MockResponse.Builder()
			.setHeader("Content-Type", "text/plain")
			.body("Hello Spring 2!")
			.build()
		anotherServer.enqueue(response)
		return anotherServer
	}

	private interface TestHttpService {
		@GetExchange("/greeting")
		suspend fun getGreetingSuspending(): String

		@GetExchange("/greeting")
		fun getGreetingMono(): Mono<String>

		@GetExchange("/greeting")
		fun getGreetingBlocking(): String

		@GetExchange("/greeting")
		suspend fun getGreetingSuspendingWithAttribute(@RequestAttribute myAttribute: String): String

		@GetExchange("/greeting/{id}")
		fun getWithUriBuilderFactory(
			uriBuilderFactory: UriBuilderFactory?, @PathVariable id: String?, @RequestParam param: String?): ResponseEntity<String>

		@GetExchange("/greeting")
		fun getWithIgnoredUriBuilderFactory(uri: URI?, uriBuilderFactory: UriBuilderFactory?): ResponseEntity<String>
	}
}
