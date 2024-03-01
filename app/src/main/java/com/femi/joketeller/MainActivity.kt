package com.femi.joketeller

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.femi.joketeller.ui.theme.JokeTellerTheme
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.Locale

private lateinit var textToSpeech: TextToSpeech
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Toast.makeText(this, "Your language is either not downloaded or supported by text-to-speech. Consider changing your language in settings", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        setContent {
            JokeTellerTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Home()
                }
            }
        }
    }
}

@Composable
fun Home() {
    Scaffold(
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val context = LocalContext.current
                var joke by remember { mutableStateOf("") }
                val radioOptions = listOf("any","programming","misc","dark","pun","spooky","christmas")
                var selectedOption by remember { mutableStateOf(radioOptions[0]) }

                Column(
                    modifier = Modifier
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Start)
                            .padding(10.dp)
                    ) {
                        radioOptions.forEach { category ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (category == selectedOption),
                                    onClick = { selectedOption = category }
                                )
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }

                    Text(
                        modifier = Modifier.padding(20.dp),
                        text = joke,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {


                            val call = ApiClient.apiService.getJoke(selectedOption)
                            call.enqueue(object : Callback<Joke> {
                                override fun onResponse(call: Call<Joke>, response: Response<Joke>) {
                                    if (response.isSuccessful) {
                                        val responseBody = response.body()
                                        joke = if (responseBody?.setup == null || responseBody.delivery == null){
                                            if (responseBody?.joke == null)
                                                "Cannot get joke, so the joke is...Your love life"
                                            else
                                                responseBody.joke
                                        } else{
                                            "${responseBody.setup} ... ${responseBody.delivery}"
                                        }

                                        textToSpeech.speak(joke, TextToSpeech.QUEUE_ADD, null, null)
                                    } else {
                                        textToSpeech.speak("Wahala wahala ${response.errorBody()}", TextToSpeech.QUEUE_FLUSH, null, null)
                                        Toast.makeText(
                                            context,
                                            "Wahala wahala ${response.errorBody()}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                                override fun onFailure(call: Call<Joke>, t: Throwable) {
                                    textToSpeech.speak("Your network is bad dear", TextToSpeech.QUEUE_FLUSH, null, null)
                                    Toast.makeText(
                                        context,
                                        "Your network is bad o",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            })

                        },
                        shape = AbsoluteRoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Get a new Joke")
                    }
                }

            }
        }
    )
}

object RetrofitClient {
    private const val BASE_URL = "https://sv443.net/"

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                .also { client ->
                        val logging = HttpLoggingInterceptor()
                        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
                        client.addInterceptor(logging)
                }.build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

object ApiClient {
    val apiService: ApiService by lazy {
        RetrofitClient.retrofit.create(ApiService::class.java)
    }
}

interface ApiService {
    @GET("jokeapi/v2/joke/{category}?blacklistFlags=nsfw,racist,sexist")
    fun getJoke(@Path("category") category: String): Call<Joke>
}