package com.borgpharmacy.pro.core.printer
object PrinterRetryPolicy { fun <T> run(attempts:Int=3,action:()->T):T{var last:Throwable?=null;repeat(attempts.coerceAtLeast(1)){try{return action()}catch(t:Throwable){last=t}};throw last?:IllegalStateException("print failed")} }
