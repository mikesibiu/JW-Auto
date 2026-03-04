package org.jw.library.auto.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.jw.library.auto.data.api.JWOrgApiService
import org.jw.library.auto.data.api.MediatorApiService
import org.jw.library.auto.data.api.MediatorCategory
import org.jw.library.auto.data.api.MediatorCategoryResponse
import org.jw.library.auto.data.api.MediatorMediaFile
import org.jw.library.auto.data.api.MediatorMediaItem
import org.jw.library.auto.data.model.api.PublicationMediaResponse
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import okhttp3.MediaType.Companion.toMediaType

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class JWOrgRepositoryDramaTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private val fakeApiService = object : JWOrgApiService {
        override suspend fun getPublicationMedia(
            pub: String,
            issue: String?,
            track: Int?,
            booknum: Int?,
            fileformat: String,
            langwritten: String,
            output: String,
            alllangs: Int,
            txtCMSLang: String
        ): PublicationMediaResponse {
            return PublicationMediaResponse(emptyMap())
        }
    }

    private class FakeMediatorService : MediatorApiService {
        var response: MediatorCategoryResponse = MediatorCategoryResponse(null)

        override suspend fun getCategory(language: String, category: String, detailed: Int): MediatorCategoryResponse {
            return response
        }
    }

    private val fakeMediatorService = FakeMediatorService()

    @Test
    fun fetchBibleDramas_parsesNextDataPayload() = runTest {
        fakeMediatorService.response = MediatorCategoryResponse(null)
        val nextDataJson = """
            {
              "props": {
                "pageProps": {
                  "listData": {
                    "files": [
                      {
                        "title": "Faithful Drama",
                        "fileUrl": "https://media.example.org/drama1.mp3",
                        "description": "Sample description",
                        "duration": 1234
                      }
                    ]
                  }
                }
              }
            }
        """.trimIndent()
        val html = """
            <html>
              <head>
                <script id="__NEXT_DATA__" type="application/json">$nextDataJson</script>
              </head>
              <body></body>
            </html>
        """.trimIndent()

        val repo = JWOrgRepository(
            context = context,
            apiService = fakeApiService,
            mediatorService = fakeMediatorService,
            dramaPageUrl = "https://example.org/dramas",
            dramaHttpClient = htmlResponder(html)
        )

        val dramas = repo.getBibleDramas(forceRefresh = true)

        assertEquals(1, dramas.size)
        val drama = dramas.first()
        assertEquals("Faithful Drama", drama.title)
        assertEquals("https://media.example.org/drama1.mp3", drama.streamUrl)
        assertEquals(1234, drama.durationSeconds)
        assertTrue(drama.description?.contains("Sample") == true)
    }

    @Test
    fun getBibleDramas_prefersMediatorCategory() = runTest {
        fakeMediatorService.response = MediatorCategoryResponse(
            MediatorCategory(
                key = "VOXDramas",
                name = "Dramas",
                media = listOf(
                    MediatorMediaItem(
                        guid = "guid-1",
                        naturalKey = "natural-1",
                        title = "Mediator Drama",
                        firstPublished = null,
                        durationSeconds = 1800.0,
                        files = listOf(
                            MediatorMediaFile(
                                label = "audio",
                                mimeType = "audio/mpeg",
                                url = "https://media.example.org/mediator.mp3"
                            )
                        )
                    )
                )
            )
        )

        val repo = JWOrgRepository(
            context = context,
            apiService = fakeApiService,
            mediatorService = fakeMediatorService,
            dramaHttpClient = OkHttpClient()
        )

        val dramas = repo.getBibleDramas(forceRefresh = true)

        assertEquals(1, dramas.size)
        val drama = dramas.first()
        assertEquals("Mediator Drama", drama.title)
        assertEquals("https://media.example.org/mediator.mp3", drama.streamUrl)
        assertEquals(1800, drama.durationSeconds)
    }

    private fun htmlResponder(body: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                okhttp3.Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("text/html".toMediaType()))
                    .build()
            }
            .build()
    }
}
