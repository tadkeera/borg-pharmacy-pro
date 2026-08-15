package com.borgpharmacy.pro.core.security
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
object PasswordSecurity { private const val ITERATIONS=120_000; fun hash(password:CharArray,salt:ByteArray=ByteArray(16).also{SecureRandom().nextBytes(it)}):String { val key=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(password,salt,ITERATIONS,256)).encoded; return "$ITERATIONS:${salt.hex()}:${key.hex()}" }; fun verify(password:CharArray,encoded:String):Boolean=runCatching{val p=encoded.split(':'); val key=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(password,p[1].fromHex(),p[0].toInt(),256)).encoded; key.contentEquals(p[2].fromHex())}.getOrDefault(false); private fun ByteArray.hex()=joinToString(""){ "%02x".format(it)}; private fun String.fromHex()=chunked(2).map{it.toInt(16).toByte()}.toByteArray() }
