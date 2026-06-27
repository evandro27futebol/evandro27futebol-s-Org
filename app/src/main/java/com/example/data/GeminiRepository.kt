package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// --- JSON Data Structures for Moshi ---
@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

class GeminiRepository {
    private val tag = "GeminiRepository"
    private val modelName = "gemini-3.5-flash"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isEmpty() || key == "MY_GEMINI_API_KEY" || key == "GEMINI_API_KEY") "" else key
        } catch (e: Exception) {
            ""
        }
    }

    fun isApiKeyConfigured(): Boolean {
        return getApiKey().isNotEmpty()
    }

    /**
     * Splits narration text into scenes.
     * Uses real Gemini API if key is available, else returns offline generated scenes.
     */
    suspend fun divideNarrationIntoScenes(narrationText: String, styleTemplate: String): List<SceneData> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            Log.w(tag, "Gemini API key not found, using offline fallback for scene division.")
            return@withContext getOfflineScenes(narrationText, styleTemplate)
        }

        val prompt = """
            Você é um assistente especialista em automação de vídeos faceless para YouTube/TikTok.
            Analise e divida o seguinte texto de narração em cenas sequenciais curtas (de 4 a 7 segundos de duração cada).
            
            Texto de narração: "$narrationText"
            Estilo de vídeo desejado: "$styleTemplate"
            
            Retorne obrigatoriamente um JSON Array de objetos, onde cada objeto representa uma cena e possui exatamente estes campos:
            - sequenceNumber (Int, iniciando em 1)
            - narrationText (String, o fragmento exato ou adaptado do texto que é falado nesta cena)
            - durationSeconds (Int, de 4 a 7 segundos)
            - overlayText (String, texto de impacto para a tela com NO MÁXIMO 5 palavras, super direto, ex: "O SEGREDO!", "MUDANÇA TOTAL!")
            - motionStyle (String, uma destas opções exatas: "Zoom In", "Zoom Out", "Pan Left", "Pan Right", "Static")
            - emotion (String, a emoção da cena, ex: "misterioso", "empolgado", "noticioso", "tenso", "informativo")
            - keywords (String, palavras-chave para buscar imagens adequadas no banco, separadas por vírgula)
            
            Garantias importantes:
            1. As cenas devem cobrir sequencialmente TODO o texto de narração fornecido.
            2. Não adicione nenhuma formatação markdown (como ```json ou ```). Retorne estritamente o JSON puro.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        try {
            val jsonRequest = requestAdapter.toJson(request)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            
            val httpRequest = Request.Builder()
                .url(url)
                .post(jsonRequest.toRequestBody(mediaType))
                .build()

            httpClient.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("API call failed with code ${response.code}: ${response.body?.string()}")
                }
                val bodyString = response.body?.string() ?: throw Exception("Empty response body")
                val responseJson = JSONObject(bodyString)
                val textResponse = responseJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                return@withContext parseScenesJson(textResponse)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error dividing scenes with Gemini: ${e.message}", e)
            return@withContext getOfflineScenes(narrationText, styleTemplate)
        }
    }

    /**
     * Helper to parse JSON output into scenes list
     */
    private fun parseScenesJson(jsonString: String): List<SceneData> {
        val list = mutableListOf<SceneData>()
        try {
            // Clean up backticks in case model ignored responseMimeType
            var cleanJson = jsonString.trim()
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substringAfter("```json").substringBeforeLast("```")
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substringAfter("```").substringBeforeLast("```")
            }
            cleanJson = cleanJson.trim()

            val array = JSONArray(cleanJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SceneData(
                        sequenceNumber = obj.optInt("sequenceNumber", i + 1),
                        narrationText = obj.optString("narrationText", ""),
                        durationSeconds = obj.optInt("durationSeconds", 5),
                        overlayText = obj.optString("overlayText", ""),
                        motionStyle = obj.optString("motionStyle", "Zoom In"),
                        emotion = obj.optString("emotion", "neutral"),
                        keywords = obj.optString("keywords", "")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse scenes JSON: ${e.message}. Raw: $jsonString")
        }
        return list
    }

    /**
     * Tags an image using Gemini.
     * Extracts visual context, tags, category, and a friendly description.
     */
    suspend fun analyzeImageWithIA(bitmap: Bitmap): ImageAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext getOfflineImageAnalysis()
        }

        val prompt = """
            Analise esta imagem que o usuário salvou em sua biblioteca para automação de vídeos.
            Identifique o tema visual principal, o clima emocional, e sugira tags úteis para busca.
            
            Retorne obrigatoriamente um objeto JSON com exatamente estes campos:
            - name (String, um nome curto descritivo para a imagem de 2 a 4 palavras)
            - category (String, uma categoria genérica, ex: "Tecnologia", "Natureza", "Pessoas", "Finanças", "Espaço")
            - tags (String, de 5 a 8 tags separadas por vírgula em português)
            - visualDescription (String, uma breve descrição da cena da imagem)
            
            Retorne apenas o JSON puro, sem marcações markdown.
        """.trimIndent()

        val base64Image = bitmapToBase64(bitmap)

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt),
                        GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.4f
            )
        )

        try {
            val jsonRequest = requestAdapter.toJson(request)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            
            val httpRequest = Request.Builder()
                .url(url)
                .post(jsonRequest.toRequestBody(mediaType))
                .build()

            httpClient.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("API call failed with code ${response.code}")
                }
                val bodyString = response.body?.string() ?: throw Exception("Empty response body")
                val responseJson = JSONObject(bodyString)
                val textResponse = responseJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val parsed = JSONObject(textResponse.trim())
                return@withContext ImageAnalysisResult(
                    name = parsed.optString("name", "Imagem de Usuário"),
                    category = parsed.optString("category", "Geral"),
                    tags = parsed.optString("tags", "imagem, fotos"),
                    visualDescription = parsed.optString("visualDescription", "Imagem carregada pelo usuário.")
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error analyzing image: ${e.message}", e)
            return@withContext getOfflineImageAnalysis()
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    // --- GRACEFUL FALLBACK DATA GENERATORS ---

    private fun getOfflineScenes(text: String, style: String): List<SceneData> {
        val cleanText = text.trim()
        val sentences = if (cleanText.isEmpty()) {
            listOf(
                "O universo guarda segredos fascinantes que a ciência ainda tenta desvendar.",
                "Bilhões de galáxias flutuam no vazio infinito do espaço sideral.",
                "Buracos negros devoram estrelas inteiras e distorcem a própria gravidade.",
                "E nós, deste pequeno planeta azul, observamos as estrelas maravilhados."
            )
        } else {
            // split by periods or commas
            cleanText.split(Regex("[.\\n]+")).map { it.trim() }.filter { it.length > 5 }
        }

        val stylesMap = mapOf(
            "documentary" to Pair("documentário", "misterioso"),
            "suspense" to Pair("suspense", "tenso"),
            "curiosities" to Pair("curiosidades", "curioso"),
            "sports" to Pair("esportivo", "empolgado"),
            "news" to Pair("notícias", "noticioso")
        )
        val currentStyle = stylesMap[style] ?: Pair("geral", "neutro")

        val list = mutableListOf<SceneData>()
        val motions = listOf("Zoom In", "Zoom Out", "Pan Left", "Pan Right", "Static")

        sentences.forEachIndexed { index, sentence ->
            val num = index + 1
            val words = sentence.split(" ")
            val overlay = if (words.size > 2) {
                "${words[0].uppercase()} ${words[1].uppercase()}!"
            } else {
                "CENA HISTÓRICA!"
            }
            
            // Suggest keywords
            val keywords = when {
                sentence.contains("universo", true) || sentence.contains("espaço", true) -> "espaço, galáxia, estrelas"
                sentence.contains("buraco", true) -> "buraco negro, gravidade, astrofísica"
                sentence.contains("planeta", true) || sentence.contains("azul", true) -> "terra, planeta azul, órbita"
                else -> "tecnologia, abstrato, futuro"
            }

            list.add(
                SceneData(
                    sequenceNumber = num,
                    narrationText = sentence,
                    durationSeconds = 5,
                    overlayText = overlay,
                    motionStyle = motions[index % motions.size],
                    emotion = currentStyle.second,
                    keywords = keywords
                )
            )
        }
        return list
    }

    private fun getOfflineImageAnalysis(): ImageAnalysisResult {
        val rand = (1..100).random()
        return ImageAnalysisResult(
            name = "Imagem Importada #$rand",
            category = "Importados",
            tags = "user, upload, slideshow, video",
            visualDescription = "Imagem carregada localmente pelo usuário para o projeto."
        )
    }

    /**
     * Generates a template design using Gemini.
     */
    suspend fun generateTemplateWithIA(prompt: String): TemplateSuggestion? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext getOfflineTemplateSuggestion(prompt)
        }

        val systemPrompt = """
            Você é um designer de vídeo de IA que cria templates visuais otimizados para vídeos faceless (TikTok/YouTube Reels).
            Analise a descrição do usuário: "$prompt"
            Crie um design de template completo em formato JSON de acordo com o seguinte esquema exato:
            
            - name (String, nome curto e criativo para o template em português)
            - primaryColorHex (String, cor primária vibrante em hex ex: "#FF00FF")
            - backgroundColorHex (String, cor de fundo escura em hex ex: "#0F0F1A")
            - textColorHex (String, cor de texto de alta legibilidade em hex ex: "#FFFFFF")
            - fontStyle (String, uma destas exatas opções: "Sans Serif", "Serif", "Monospace", "Space Grotesk")
            - motionStyle (String, uma destas exatas opções: "Zoom In", "Zoom Out", "Pan Left", "Pan Right", "Static", "Random")
            - defaultSceneDurationSeconds (Int, de 4 a 7 segundos)
            - overlayTextType (String, uma destas exatas opções: "Uppercase Bold", "Italic Highlight", "Subtitle Center", "Large Splash")
            - bgMusicName (String, uma destas exatas opções: "Silent", "Cosmic Ambient", "Synth Pulse", "News Flash", "Cinematic Tension")
            - bgMusicVolume (Float, de 0.1 a 0.5)
            
            Retorne apenas o JSON puro, sem marcações markdown ou blocos ```json.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.5f
            )
        )

        try {
            val jsonRequest = requestAdapter.toJson(request)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            
            val httpRequest = Request.Builder()
                .url(url)
                .post(jsonRequest.toRequestBody(mediaType))
                .build()

            httpClient.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("API call failed with code ${response.code}")
                }
                val bodyString = response.body?.string() ?: throw Exception("Empty response body")
                val responseJson = JSONObject(bodyString)
                val textResponse = responseJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                var cleanJson = textResponse.trim()
                if (cleanJson.startsWith("```json")) {
                    cleanJson = cleanJson.substringAfter("```json").substringBeforeLast("```")
                } else if (cleanJson.startsWith("```")) {
                    cleanJson = cleanJson.substringAfter("```").substringBeforeLast("```")
                }
                cleanJson = cleanJson.trim()

                val parsed = JSONObject(cleanJson)
                return@withContext TemplateSuggestion(
                    name = parsed.optString("name", "Template Personalizado"),
                    primaryColorHex = parsed.optString("primaryColorHex", "#FF5E97"),
                    backgroundColorHex = parsed.optString("backgroundColorHex", "#121212"),
                    textColorHex = parsed.optString("textColorHex", "#FFFFFF"),
                    fontStyle = parsed.optString("fontStyle", "Space Grotesk"),
                    motionStyle = parsed.optString("motionStyle", "Zoom In"),
                    defaultSceneDurationSeconds = parsed.optInt("defaultSceneDurationSeconds", 5),
                    overlayTextType = parsed.optString("overlayTextType", "Uppercase Bold"),
                    bgMusicName = parsed.optString("bgMusicName", "Silent"),
                    bgMusicVolume = parsed.optDouble("bgMusicVolume", 0.15).toFloat()
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error generating template with Gemini: ${e.message}", e)
            return@withContext getOfflineTemplateSuggestion(prompt)
        }
    }

    private fun getOfflineTemplateSuggestion(prompt: String): TemplateSuggestion {
        val lower = prompt.lowercase()
        return when {
            lower.contains("cyberpunk") || lower.contains("futurista") || lower.contains("neon") -> {
                TemplateSuggestion(
                    name = "Neon Cyberpunk IA",
                    primaryColorHex = "#FF007F",
                    backgroundColorHex = "#05050C",
                    textColorHex = "#00FFFF",
                    fontStyle = "Space Grotesk",
                    motionStyle = "Zoom Out",
                    defaultSceneDurationSeconds = 4,
                    overlayTextType = "Uppercase Bold",
                    bgMusicName = "Synth Pulse",
                    bgMusicVolume = 0.20f
                )
            }
            lower.contains("terror") || lower.contains("mistério") || lower.contains("suspense") || lower.contains("escuro") -> {
                TemplateSuggestion(
                    name = "Mistério Sombrio IA",
                    primaryColorHex = "#FF3333",
                    backgroundColorHex = "#0D0B0E",
                    textColorHex = "#E2E2E6",
                    fontStyle = "Serif",
                    motionStyle = "Pan Left",
                    defaultSceneDurationSeconds = 6,
                    overlayTextType = "Italic Highlight",
                    bgMusicName = "Cinematic Tension",
                    bgMusicVolume = 0.25f
                )
            }
            lower.contains("notícia") || lower.contains("jornal") || lower.contains("sério") || lower.contains("oficial") -> {
                TemplateSuggestion(
                    name = "Noticiário Premium IA",
                    primaryColorHex = "#1F75FE",
                    backgroundColorHex = "#0A1128",
                    textColorHex = "#FFFFFF",
                    fontStyle = "Sans Serif",
                    motionStyle = "Static",
                    defaultSceneDurationSeconds = 5,
                    overlayTextType = "Subtitle Center",
                    bgMusicName = "News Flash",
                    bgMusicVolume = 0.15f
                )
            }
            else -> {
                TemplateSuggestion(
                    name = "Criativo Dinâmico IA",
                    primaryColorHex = "#FF5E97",
                    backgroundColorHex = "#1D1612",
                    textColorHex = "#FFFDF9",
                    fontStyle = "Space Grotesk",
                    motionStyle = "Zoom In",
                    defaultSceneDurationSeconds = 5,
                    overlayTextType = "Large Splash",
                    bgMusicName = "Cosmic Ambient",
                    bgMusicVolume = 0.18f
                )
            }
        }
    }
}

// --- Auxiliary Classes ---
data class SceneData(
    val sequenceNumber: Int,
    val narrationText: String,
    val durationSeconds: Int,
    val overlayText: String,
    val motionStyle: String,
    val emotion: String,
    val keywords: String
)

data class ImageAnalysisResult(
    val name: String,
    val category: String,
    val tags: String,
    val visualDescription: String
)

data class TemplateSuggestion(
    val name: String,
    val primaryColorHex: String,
    val backgroundColorHex: String,
    val textColorHex: String,
    val fontStyle: String,
    val motionStyle: String,
    val defaultSceneDurationSeconds: Int,
    val overlayTextType: String,
    val bgMusicName: String,
    val bgMusicVolume: Float
)

