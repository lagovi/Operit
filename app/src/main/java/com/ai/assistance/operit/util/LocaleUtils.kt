package com.ai.assistance.operit.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.ai.assistance.operit.data.preferences.UserPreferencesManager
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** 语言工具类，用于管理应用的国际化设置 */
object LocaleUtils {

    object LanguageCodes {
        const val ENGLISH = "en"
    }

    /**
     * 语言信息数据类
     * @param code 语言代码（如zh、en）
     * @param displayName 显示名称（英文）
     * @param nativeName 本地名称（语言自身的称呼）
     */
    data class Language(val code: String, val displayName: String, val nativeName: String)

    // Fork: this distribution ships English resources only, so English is both the
    // default and the only entry. The list, the resolver and DEFAULT_LANGUAGE are
    // all driven from here; adding a locale later means adding one Language entry,
    // one values-<code>/ directory and one line to res/xml/locales_config.xml.
    private val supportedLanguages =
            listOf(Language(LanguageCodes.ENGLISH, "English", "English"))

    private val supportedLanguageCodes = supportedLanguages.map { it.code }.toSet()

    /** 获取支持的语言列表 */
    fun getSupportedLanguages(): List<Language> {
        return supportedLanguages
    }

    fun getLocaleForLanguageCode(languageCode: String): Locale {
        return Locale.forLanguageTag(resolveSupportedLanguageCode(languageCode))
    }

    fun createLocaleOverrideConfiguration(locale: Locale): Configuration {
        // Keep this override sparse so window size and orientation continue to update.
        return Configuration().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setLocales(createPlatformLocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                setLocale(locale)
            }
        }
    }

    fun createCompatLocaleList(locale: Locale): LocaleListCompat {
        return LocaleListCompat.create(locale)
    }

    fun createPlatformLocaleList(locale: Locale): LocaleList {
        return LocaleList(locale)
    }

    fun setDefaultLocales(locale: Locale) {
        Locale.setDefault(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList.setDefault(createPlatformLocaleList(locale))
        }
    }

    /**
     * Whether the app's language is Chinese.
     *
     * This build ships English only, so the answer is always false. It is kept
     * because prompt builders are still bilingual and gate their Chinese variants
     * on it; removing it means deleting every Chinese prompt, which is a separate
     * change with a much larger merge surface.
     */
    fun usesChineseContent(context: Context): Boolean {
        return getCurrentLanguage(context)
                .lowercase(Locale.ROOT)
                .startsWith("zh")
    }

    /**
     * 获取包含当前应用语言设置的上下文。
     * 对于使用applicationContext的单例或服务，这非常有用，
     * 因为它可以确保获取到最新的本地化资源。
     *
     * @param context 基础上下文.
     * @return 带有更新后语言配置的新上下文.
     */
    fun getLocalizedContext(context: Context): Context {
        val locale = getLocaleForLanguageCode(getCurrentLanguage(context))
        return context.createConfigurationContext(createLocaleOverrideConfiguration(locale))
    }

    /**
     * 获取当前应用设置的语言
     * @param context 上下文
     * @return the active language code, e.g. en
     */
    fun getCurrentLanguage(context: Context): String {
        val savedLanguage =
                runCatching { UserPreferencesManager.getInstance(context).getCurrentLanguage() }
                        .onFailure { error ->
                            AppLogger.e("LocaleUtils", "读取应用语言设置失败", error)
                        }
                        .getOrDefault("")

        return resolveSupportedLanguageCode(savedLanguage)
    }

    /** The single shipped language code, for UI that has no context available. */
    fun currentLanguageCode(): String = supportedLanguages.first().code

    /**
     * 设置应用语言
     * @param context 上下文
     * @param languageCode language code, e.g. en
     */
    fun setAppLanguage(context: Context, languageCode: String) {
        val localeToSet = getLocaleForLanguageCode(languageCode)

        runBlocking(Dispatchers.IO) {
            UserPreferencesManager.getInstance(context).saveAppLanguage(localeToSet.toLanguageTag())
        }

        // 设置默认语言
        setDefaultLocales(localeToSet)

        // 根据Android版本应用语言设置
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 使用AppCompatDelegate API
            AppCompatDelegate.setApplicationLocales(createCompatLocaleList(localeToSet))
        } else {
            // Below Android 13 the app owns the locale itself: AppCompat cannot
            // reconfigure the process. Reconfigure the calling context, and let
            // every component that reads the Application context go through
            // getLocalizedContext, which rebuilds the configuration from the
            // preference that was just saved.
            val config = Configuration(context.resources.configuration)
            config.setLocales(createPlatformLocaleList(localeToSet))
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }

    private fun normalizeStoredLanguageCode(languageCode: String): String {
        return languageCode.replace("_", "-").replace("-r", "-")
    }

    /**
     * Fork: a language this build does not ship must not reach the resource
     * system. Returning it verbatim made Android fall back to the unqualified
     * bucket, which is why a fresh install on an unsupported system locale
     * started in the wrong language. English is the only shipped locale, so it
     * is the resolution for anything else.
     */
    private fun resolveSupportedLanguageCode(languageCode: String): String {
        val normalizedCode = normalizeStoredLanguageCode(languageCode)
        if (normalizedCode.isBlank()) {
            return LanguageCodes.ENGLISH
        }

        if (normalizedCode in supportedLanguageCodes) {
            return normalizedCode
        }

        val language =
                Locale.forLanguageTag(normalizedCode)
                        .takeIf { it.language.isNotBlank() }
                        ?.language
                        ?: return LanguageCodes.ENGLISH

        return supportedLanguageCodes.firstOrNull { it.equals(language, ignoreCase = true) }
                ?: LanguageCodes.ENGLISH
    }
}
