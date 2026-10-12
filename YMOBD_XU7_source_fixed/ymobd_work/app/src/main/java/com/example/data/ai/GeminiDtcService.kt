package com.example.data.ai

import com.example.BuildConfig
import com.example.data.models.DtcCode
import com.example.data.obd.IranKhodroDtcDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiDtcService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val MODEL = "gemini-3.5-flash"
    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    suspend fun diagnoseDtc(
        dtcCode: String,
        vehicleContext: String = "پژو ۴۰۵ / پارس / سمند با موتور XU7 یا XU7P و ایسیو زیمنس / کروز CIX48 ایران‌خودرو (مدل‌های ۱۴۰۰ به بعد)"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        // If no API key provided or placeholder, provide high-quality localized expert mechanic response
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateExpertOfflineDiagnosis(dtcCode)
        }

        val prompt = """
            شما یک مهندس ارشد و کارشناس عیب‌یابی خودروهای ایران‌خودرو، متخصص سیستم‌های انژکتور زیمنس و کروز، و موتورهای پژو XU7 و XU7P هستید.
            کد خطای خوانده شده از خودرو: $dtcCode
            مشخصات خودرو: $vehicleContext
            
            لطفاً یک تحلیل جامع، دقیق، فنی و کاملاً کاربردی به زبان فارسی در قالب بخش‌های زیر ارائه دهید:
            ۱. نام و مفهوم دقیق خطا به زبان فارسی و انگلیسی
            ۲. علت ریشه‌ای این خطا دقیقاً در موتور XU7 و ایسیو زیمنس (مانند اثر بر واشر سرسیلندر، استپر، سنسور مپ، سنسور دمای آب، کوئل یا رله دوبل)
            ۳. درجه خطرساز بودن (آیا خودرو متوقف شود یا می‌توان تا تعمیرگاه ادامه مسیر داد؟)
            ۴. علائم ظاهری که راننده متوجه می‌شود (ریپ زدن، دود، بالا رفتن آمپر آب، مصرف بنزین)
            ۵. مراحل گام‌به‌گام تست و تعمیر برای راننده یا مکانیک (تست ولتاژ، تمیزکاری سوکت، تعویض قطعه)
            ۶. راهکار پس از تعمیر و نحوه ریست ایسیو
            
            لحن پاسخ حرفه‌ای، کاملاً تخصصی، واضح و بدون اضافه گویی باشد.
        """.trimIndent()

        try {
            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)
            }

            val request = Request.Builder()
                .url("$API_URL?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseString = response.body?.string() ?: ""
                val responseJson = JSONObject(responseString)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text")
                    }
                }
            }
            // If API fails or throttles, fallback gracefully to database
            generateExpertOfflineDiagnosis(dtcCode)
        } catch (e: Exception) {
            generateExpertOfflineDiagnosis(dtcCode)
        }
    }

    fun generateExpertOfflineDiagnosis(dtcCode: String): String {
        val known = IranKhodroDtcDatabase.findCode(dtcCode)
        return if (known != null) {
            """
                🔍 **تحلیل هوشمند کد خطای ${known.code} (پایگاه دانش زیمنس ایران‌خودرو)**
                
                📌 **عنوان خطا:** ${known.titleFa}
                🏷️ **عنوان بین‌المللی:** ${known.titleEn}
                
                ⚠️ **علت ریشه‌ای در موتور XU7 و ایسیو زیمنس:**
                ${known.xu7SpecificCause}
                
                🚨 **درجه اهمیت و خطر:** ${when (known.severity) {
                com.example.data.models.DtcSeverity.CRITICAL -> "🔴 بسیار بحرانی (خطر آسیب به سرسیلندر یا خاموش شدن ناگهانی خودرو)"
                com.example.data.models.DtcSeverity.HIGH -> "🟠 بالا (افت شدید شتاب، بد کار کردن موتور و لرزش)"
                com.example.data.models.DtcSeverity.MEDIUM -> "🟡 متوسط (افزایش مصرف سوخت و آلایندگی)"
                com.example.data.models.DtcSeverity.LOW -> "🟢 پایین (هشدار راندمان سیستم کاتالیزور)"
            }}
                
                🩺 **علائم ظاهری در خودرو:**
                ${known.symptoms.joinToString("\n") { "• $it" }}
                
                🔧 **مراحل عیب‌یابی و رفع نقص:**
                ${known.repairSteps.joinToString("\n") { "۱. $it" }}
                
                💡 **نکته تخصصی مکانیک:**
                پس از رفع عیب فیزیکی قطعه، حتماً از بخش «پیکربندی ECU» در این اپلیکیشن گزینه «فلش و ریست حافظه موقت ایسیو» را اجرا نمایید تا جدول‌های تصحیح سوخت (Fuel Trims) مجدداً صفر شوند.
            """.trimIndent()
        } else {
            """
                🔍 **تحلیل کد خطای $dtcCode برای ایسیو زیمنس موتور XU7**
                
                این کد خطا در دسته‌بندی استانداردهای OBD-II مربوط به مدیریت موتور و احتراق است.
                
                ⚠️ **بررسی پیشنهادی برای سیستم انژکتور زیمنس:**
                • وضعیت سیستم جرقه‌زنی (کوئل دوبل و شمع‌های موتور XU7)
                • سلامت سنسورهای کلیدی: مپ سنسور (فشار منیفولد)، استپر موتور، و سنسور اکسیژن بالا
                • فشار پمپ بنزین و سلامت رله دوبل
                • دمای آب رادیاتور و اطمینان از هواگیری سیستم خنک‌کننده
                
                🔧 **اقدام بعدی:**
                از بخش پاک‌کردن خطاها برای ریست کد استفاده کنید. در صورت ثبت مجدد پس از استارت خودرو، سیم‌کشی و مقاومت سنسور مربوطه را با مولتی‌متر اندازه‌گیری نمایید.
            """.trimIndent()
        }
    }
}
