package com.borgpharmacy.pro.core.util
object ArabicTextNormalizer { fun normalize(text:String):String=text.trim().replace("ـ","").replace(Regex("\\s+")," ") }
