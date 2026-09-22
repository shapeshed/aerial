package com.shapeshed.aerial.data

import com.shapeshed.aerial.BuildConfig
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class HttpUtilsTest {
    private lateinit var server: HttpServer
    private var lastMethod: String? = null
    private var lastUserAgent: String? = null
    private var lastCustomHeader: String? = null
    private var lastRequestBody: String? = null

    @Before
    fun startServer() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/ok") { exchange -> respond(exchange, 200, "hello") }
        server.createContext("/missing") { exchange -> respond(exchange, 404, "nope") }
        server.createContext("/boom") { exchange -> respond(exchange, 500, "server error") }
        server.createContext("/echo") { exchange ->
            lastMethod = exchange.requestMethod
            lastUserAgent = exchange.requestHeaders.getFirst("User-Agent")
            lastCustomHeader = exchange.requestHeaders.getFirst("X-Test")
            lastRequestBody = exchange.requestBody.bufferedReader().readText()
            respond(exchange, 200, "echoed")
        }
        server.start()
    }

    @After
    fun stopServer() {
        server.stop(0)
    }

    @Test
    fun getReturnsTheBodyOnSuccess() {
        assertEquals("hello", httpGetText(url("/ok")))
    }

    @Test
    fun getReturnsNullForErrorStatuses() {
        assertNull(httpGetText(url("/missing")))
        assertNull(httpGetText(url("/boom")))
    }

    @Test
    fun getReturnsNullWhenTheConnectionFails() {
        assertNull(httpGetText("http://127.0.0.1:1/unreachable"))
    }

    @Test
    fun getSendsTheAerialUserAgentAndExtraHeaders() {
        httpGetText(url("/echo"), extraHeaders = mapOf("X-Test" to "value"))

        assertEquals("Aerial/${BuildConfig.VERSION_NAME} (Android)", lastUserAgent)
        assertEquals("value", lastCustomHeader)
    }

    @Test
    fun postSendsTheJsonBodyAndReturnsTheResponse() {
        assertEquals("echoed", httpPostJson(url("/echo"), """{"station":"mango"}"""))

        assertEquals("POST", lastMethod)
        assertEquals("""{"station":"mango"}""", lastRequestBody)
        assertEquals("Aerial/${BuildConfig.VERSION_NAME} (Android)", lastUserAgent)
    }

    @Test
    fun postReturnsNullForErrorStatuses() {
        assertNull(httpPostJson(url("/boom"), "{}"))
    }

    @Test
    fun getJsonDelegatesToGetText() {
        assertEquals("hello", httpGetJson(url("/ok")))
    }

    private fun url(path: String) = "http://127.0.0.1:${server.address.port}$path"

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray()
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
