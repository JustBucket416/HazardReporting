package com.example.hazardreporting.data.remote

import com.example.hazardreporting.domain.exception.InvalidCredentialsException
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.UnauthorizedException
import com.google.common.truth.Truth
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * @author Roman Pliskin on 29.10.2026
 */
class RemoteErrorMappingTest {

    @Test
    fun testMapRemoteErrors_Success() = runTest {
        val result = mapRemoteErrors { RESULT }

        Truth.assertThat(result).isEqualTo(RESULT)
    }

    @Test
    fun testMapRemoteErrors_IoFailure() {
        val ioException = IOException()

        val thrown = Assert.assertThrows(NoConnectionException::class.java) {
            runTest { mapRemoteErrors { throw ioException } }
        }

        Truth.assertThat(thrown).hasCauseThat().isSameInstanceAs(ioException)
    }

    @Test
    fun testMapRemoteErrors_Unauthorized() {
        val httpException = httpException(401)

        val thrown = Assert.assertThrows(UnauthorizedException::class.java) {
            runTest { mapRemoteErrors { throw httpException } }
        }

        Truth.assertThat(thrown).hasCauseThat().isSameInstanceAs(httpException)
    }

    @Test
    fun testMapRemoteErrors_UnauthorizedOnLogin() {
        val httpException = httpException(401)

        val thrown = Assert.assertThrows(InvalidCredentialsException::class.java) {
            runTest {
                mapRemoteErrors(onUnauthorized = { InvalidCredentialsException(it) }) { throw httpException }
            }
        }

        Truth.assertThat(thrown).hasCauseThat().isSameInstanceAs(httpException)
    }

    @Test
    fun testMapRemoteErrors_ServerError() {
        val httpException = httpException(500)

        val thrown = Assert.assertThrows(HttpException::class.java) {
            runTest { mapRemoteErrors { throw httpException } }
        }

        Truth.assertThat(thrown).isSameInstanceAs(httpException)
    }

    @Test
    fun testMapRemoteErrors_OtherException() {
        val exception = IllegalStateException()

        val thrown = Assert.assertThrows(IllegalStateException::class.java) {
            runTest { mapRemoteErrors { throw exception } }
        }

        Truth.assertThat(thrown).isSameInstanceAs(exception)
    }

    private fun httpException(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    private companion object {
        const val RESULT = "result"
    }
}
