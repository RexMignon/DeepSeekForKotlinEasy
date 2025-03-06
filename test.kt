import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Scanner
import java.util.concurrent.TimeUnit

class OpenAI(private val apiKey: String, private val baseUrl: String = "https://api.deepseek.com",val initialPromptWord:String = "u are helpful assistant") {

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    // 维护对话上下文
    private val messages = mutableListOf(
        mapOf("role" to "system", "content" to "${initialPromptWord}")
    )

    fun chatCompletionsCreate(model: String, userInput: String, stream: Boolean = false): String {
        messages.add(mapOf("role" to "user", "content" to userInput))

        val requestBodyMap = mapOf(
            "model" to model,
            "messages" to messages,
            "stream" to stream
        )
        val requestBodyJson = gson.toJson(requestBodyMap)
        val requestBody = requestBodyJson.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return "请求失败，错误码：${response.code}"
            }
            val responseBody = response.body?.string()
            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            val reply = jsonResponse.getAsJsonArray("choices")
                ?.firstOrNull()
                ?.asJsonObject
                ?.getAsJsonObject("message")
                ?.get("content")
                ?.asString ?: "无返回内容"

            messages.add(mapOf("role" to "assistant", "content" to reply))
            return reply
        }
    }
}

// 交互式聊天
fun main() {
    val apiKey = "API KEY IN HERE"

    val client = OpenAI(apiKey, "https://api.deepseek.com")
    val scanner = Scanner(System.`in`)

    println("Deepseek R1 聊天开始，输入 'exit' 退出。")

    while (true) {
        print("\n你: ")
        val userInput = scanner.nextLine().trim()
        if (userInput.lowercase() == "exit") {
            println("对话结束。")
            break
        }

        val response = client.chatCompletionsCreate("deepseek-reasoner", userInput, stream = false)
        println("AI: $response")
    }
}
