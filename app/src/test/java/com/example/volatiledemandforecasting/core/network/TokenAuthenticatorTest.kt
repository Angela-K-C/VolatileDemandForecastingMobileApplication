package com.example.volatiledemandforecasting.core.network

import com.example.volatiledemandforecasting.core.security.SessionManager
import com.example.volatiledemandforecasting.core.security.SessionState
import com.example.volatiledemandforecasting.core.security.SignOutReason
import com.example.volatiledemandforecasting.core.security.TokenStore
import com.example.volatiledemandforecasting.core.security.Tokens
import com.example.volatiledemandforecasting.data.auth.AuthApi
import com.example.volatiledemandforecasting.data.auth.RetrofitTokenRefresher
import com.example.volatiledemandforecasting.data.user.MeApi
import com.example.volatiledemandforecasting.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.create
import retrofit2.http.GET
import java.util.concurrent.atomic.AtomicInteger

private class InMemoryTokenStore(private var tokens: Tokens?) : TokenStore {
    override fun load() = tokens
    override fun save(tokens: Tokens) { this.tokens = tokens }
    override fun clear() { tokens = null }
}

private interface Probe {
    @GET("v1/probe")
    suspend fun probe()
}

private const val ME_JSON = """{"id":"u1","email":"a@b.co","displayName":"Ada","isResearch":false,"workspaces":[]}"""
private const val TOKENS_JSON = """{"accessToken":"new-access","refreshToken":"new-refresh","tokenType":"Bearer","expiresIn":900}"""

private fun error(code: String, status: Int, headers: Map<String, String> = emptyMap()) =
    MockResponse.Builder().code(status).apply {
        addHeader("Content-Type", "application/json")
        headers.forEach { (k, v) -> addHeader(k, v) }
        body("""{"error":{"code":"$code","message":"msg for $code","details":null,"requestId":"req_1"}}""")
    }.build()

private fun ok(body: String) =
    MockResponse.Builder().code(200).addHeader("Content-Type", "application/json").body(body).build()

class TokenAuthenticatorTest {

    private val server = MockWebServer()
    private val refreshCalls = AtomicInteger()
    private val refreshRequestBodies = mutableListOf<String>()

    /** What POST /v1/auth/refresh answers with. */
    @Volatile private var refreshResponse: () -> MockResponse = { ok(TOKENS_JSON) }

    private lateinit var store: InMemoryTokenStore
    private lateinit var session: SessionManager
    private lateinit var users: UserRepository
    private lateinit var probe: Probe

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.url.encodedPath) {
                "/v1/auth/refresh" -> {
                    refreshCalls.incrementAndGet()
                    synchronized(refreshRequestBodies) { refreshRequestBodies += request.body?.utf8().orEmpty() }
                    refreshResponse()
                }
                "/v1/me" ->
                    if (request.headers["Authorization"] == "Bearer new-access") ok(ME_JSON)
                    else error("UNAUTHORIZED", 401)
                "/v1/probe" -> error("INVALID_CREDENTIALS", 401)
                else -> MockResponse.Builder().code(404).build()
            }
        }
        server.start()

        store = InMemoryTokenStore(Tokens("old-access", "old-refresh"))
        session = SessionManager(store)

        val baseUrl = server.url("/").toString()
        val authApi = Network.retrofit(baseUrl, Network.publicClient()).create<AuthApi>()
        val authed = Network.retrofit(baseUrl, Network.authedClient(session, RetrofitTokenRefresher(authApi)))
        users = UserRepository(authed.create<MeApi>(), Network.json)
        probe = authed.create<Probe>()
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `an expired access token is refreshed once and the request is retried`() = runBlocking(Dispatchers.IO) {
        val me = users.me()

        assertEquals("Ada", me.displayName)
        assertEquals(1, refreshCalls.get())
        assertEquals(Tokens("new-access", "new-refresh"), store.load())
        assertEquals(SessionState.SignedIn, session.state.value)
    }

    @Test
    fun `the refresh sends the stored refresh token`() = runBlocking(Dispatchers.IO) {
        users.me()

        assertTrue(refreshRequestBodies.single().contains("old-refresh"))
    }

    @Test
    fun `requests carry a bearer token and a request id`() = runBlocking(Dispatchers.IO) {
        users.me()

        val first = server.takeRequest()
        assertEquals("Bearer old-access", first.headers["Authorization"])
        assertTrue(first.headers["X-Request-Id"].orEmpty().startsWith("app_"))
    }

    @Test
    fun `simultaneous failures cause a single refresh`() = runBlocking(Dispatchers.IO) {
        // A second refresh with the already-rotated token would end every session, so it must never happen.
        val results = (1..8).map { async { users.me() } }.awaitAll()

        assertEquals(8, results.size)
        assertEquals(1, refreshCalls.get())
    }

    @Test
    fun `a rejected refresh token signs the user out with a reason`() = runBlocking(Dispatchers.IO) {
        refreshResponse = { error("INVALID_REFRESH_TOKEN", 401) }

        val failure = runCatching { users.me() }.exceptionOrNull()

        assertTrue(failure is ApiException)
        assertEquals(ApiErrorCodes.UNAUTHORIZED, (failure as ApiException).code)
        assertNull(store.load())
        assertEquals(SessionState.SignedOut(SignOutReason.SessionExpired), session.state.value)
    }

    @Test
    fun `a refresh that fails on the server does not sign the user out`() = runBlocking(Dispatchers.IO) {
        refreshResponse = { MockResponse.Builder().code(503).build() }

        runCatching { users.me() }

        assertNotNull(store.load())
        assertEquals(SessionState.SignedIn, session.state.value)
    }

    @Test
    fun `a 401 that is not UNAUTHORIZED does not trigger a refresh`() = runBlocking(Dispatchers.IO) {
        // INVALID_CREDENTIALS means a wrong password, not an expired token.
        val failure = runCatching { apiCall(Network.json) { probe.probe() } }.exceptionOrNull()

        assertEquals(ApiErrorCodes.INVALID_CREDENTIALS, (failure as ApiException).code)
        assertEquals(0, refreshCalls.get())
        assertNotNull(store.load())
    }

    @Test
    fun `a signed-out user sends no token and nothing is refreshed`() = runBlocking(Dispatchers.IO) {
        session.signOut()

        val failure = runCatching { users.me() }.exceptionOrNull()

        assertTrue(failure is ApiException)
        assertEquals(0, refreshCalls.get())
        assertNull(server.takeRequest().headers["Authorization"])
    }
}
