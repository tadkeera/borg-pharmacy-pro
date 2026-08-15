package com.borgpharmacy.pro.core.printer
import android.graphics.Bitmap
object EscPosRasterHelper { fun toGsV0(bitmap:Bitmap):ByteArray { val w=(bitmap.width+7)/8; val out=java.io.ByteArrayOutputStream(); out.write(byteArrayOf(0x1d,0x76,0x30,0,w.toByte(),(w shr 8).toByte(),(bitmap.height and 255).toByte(),(bitmap.height shr 8).toByte())); for(y in 0 until bitmap.height) for(x in 0 until w){var b=0; for(bit in 0..7){val xx=x*8+bit;if(xx<bitmap.width && (bitmap.getPixel(xx,y) and 0xff)<128)b=b or (1 shl (7-bit))};out.write(b)};return out.toByteArray()} }
