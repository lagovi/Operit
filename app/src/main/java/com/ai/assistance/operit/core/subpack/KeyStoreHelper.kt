package com.ai.assistance.operit.core.subpack

import android.content.Context
import com.ai.assistance.operit.util.AppLogger
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.Provider
import java.security.Security

/** 密钥库辅助类 统一处理密钥库加载、验证和Provider管理 */
class KeyStoreHelper {
    companion object {
        private const val TAG = "KeyStoreHelper"

        /**
         * Registers the bundled BouncyCastle first WITHOUT removing the
         * platform one (DoD-proven 10.10, task 13: removal kills the only
         * guaranteed PKCS12 KeyStore).
         *
         * Live-fire 10.10 correction: the platform BC alone canNOT unwrap
         * our PBES2-encrypted PKCS12 (SecretKeyFactory for
         * 1.2.840.113549.1.5.12 unavailable) — the working path is the
         * bundled BC inserted at position 1. Returns the insert outcome
         * for diagnostics; failure is non-fatal (iteration below still
         * tries every registered provider).
         */
        @JvmStatic
        fun registerBouncyCastleProvider(): Boolean {
            try {
                val existing = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
                if (existing is BouncyCastleProvider) {
                    AppLogger.d(TAG, "Bundled BC already registered")
                    return true
                }
                val position = Security.insertProviderAt(BouncyCastleProvider(), 1)
                AppLogger.d(TAG, "Bundled BC inserted at $position")
                return position > 0
            } catch (e: Exception) {
                AppLogger.e(TAG, "注册BouncyCastle提供程序失败: ${e.message}", e)
                return false
            }
        }

        /**
         * Loads and verifies a keystore, trying every registered provider
         * that offers the requested type in preference order.
         *
         * Why not a bare KeyStore.getInstance(type): JCA instantiates the
         * FIRST provider's service, but instantiation success does not mean
         * the load succeeds. Iterating to load+verify extends JCA's own
         * pattern past instantiation; total failure still returns null
         * (surfaced by the caller, never swallowed).
         */
        @JvmStatic
        fun loadVerifiedKeystore(file: File, type: String, password: String): KeyStore? {
            registerBouncyCastleProvider()
            val providers = Security.getProviders("KeyStore.$type")
            AppLogger.d(TAG, "KeyStore.$type offered by: ${providers?.joinToString { it.name }}")
            if (providers.isNullOrEmpty()) {
                AppLogger.e(TAG, "No provider offers KeyStore.$type")
                return null
            }
            for (provider in providers) {
                try {
                    val keyStore = KeyStore.getInstance(type, provider)
                    FileInputStream(file).use { input ->
                        keyStore.load(input, password.toCharArray())
                    }
                    if (!keyStore.aliases().hasMoreElements()) {
                        AppLogger.d(TAG, "$type via ${provider.name}: no aliases, trying next")
                        continue
                    }
                    AppLogger.d(TAG, "$type loaded via ${provider.name}")
                    return keyStore
                } catch (e: Exception) {
                    AppLogger.d(TAG, "$type via ${provider.name} failed: ${e.message}, trying next")
                }
            }
            AppLogger.e(TAG, "All providers failed to load $type keystore ${file.absolutePath}")
            return null
        }

        /**
         * 验证密钥库文件是否有效
         * @param file 密钥库文件
         * @param type 密钥库类型
         * @param password 密钥库密码
         * @return 是否有效
         */
        @JvmStatic
        fun validateKeystore(file: File, type: String, password: String): Boolean {
            return try {
                loadVerifiedKeystore(file, type, password) != null
            } catch (e: Exception) {
                AppLogger.e(TAG, "$type 密钥库验证失败: ${e.message}")
                false
            }
        }

        /**
         * 从应用assets中加载内置密钥库
         * @param context 应用上下文
         * @param assetName 资产文件名
         * @param outputFileName 输出文件名
         * @return 密钥库文件或null
         */
        @JvmStatic
        fun loadKeystoreFromAsset(
                context: Context,
                assetName: String,
                outputFileName: String
        ): File? {
            try {
                val keystoreFile = File(context.filesDir, outputFileName)

                // 如果文件已存在且大小合理，直接返回
                if (keystoreFile.exists() && keystoreFile.length() > 1000) {
                    return keystoreFile
                }

                // 如果已存在但可能损坏，先删除
                if (keystoreFile.exists()) {
                    keystoreFile.delete()
                }

                // 从assets复制密钥库文件
                context.assets.open(assetName).use { input ->
                    val bytes = input.readBytes()

                    if (bytes.size < 1000) {
                        AppLogger.e(TAG, "密钥库文件大小异常: ${bytes.size}字节")
                        return null
                    }

                    keystoreFile.outputStream().use { output ->
                        output.write(bytes)
                        output.flush()
                    }
                }

                return if (keystoreFile.exists() && keystoreFile.length() > 1000) {
                    keystoreFile
                } else {
                    null
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "加载内置密钥库失败: ${e.message}", e)
                return null
            }
        }

        /**
         * 获取或创建应用签名密钥库
         * @param context 应用上下文
         * @return 密钥库文件
         */
        @JvmStatic
        fun getOrCreateKeystore(context: Context): File {
            // 先尝试PKCS12格式
            val pkcs12KeyStoreFile = File(context.filesDir, "pkcs12.keystore")
            if (pkcs12KeyStoreFile.exists() && pkcs12KeyStoreFile.length() > 1000) {
                if (validateKeystore(pkcs12KeyStoreFile, "PKCS12", "android")) {
                    return pkcs12KeyStoreFile
                }
            }

            // 再尝试JKS格式
            val jksKeyStoreFile = File(context.filesDir, "jks.jks")
            if (jksKeyStoreFile.exists() && jksKeyStoreFile.length() > 1000) {
                if (validateKeystore(jksKeyStoreFile, "JKS", "android")) {
                    return jksKeyStoreFile
                }
            }

            // 尝试从assets加载
            val keystoreFiles = listOf(Pair("pkcs12.keystore", "PKCS12"), Pair("jks.jks", "JKS"))

            for ((assetName, type) in keystoreFiles) {
                try {
                    val keyStoreFile = loadKeystoreFromAsset(context, assetName, assetName)
                    if (keyStoreFile != null && validateKeystore(keyStoreFile, type, "android")) {
                        return keyStoreFile
                    }
                } catch (e: Exception) {
                    // 忽略单个格式错误，继续尝试下一个
                }
            }

            // 如果所有尝试都失败，返回默认文件路径
            return pkcs12KeyStoreFile
        }
    }
}
