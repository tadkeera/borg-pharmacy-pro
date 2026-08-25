package com.borgpharmacy.pro.domain.validation
import com.borgpharmacy.pro.domain.model.*
object BusinessValidators { fun company(c:Company,tenant:String){require(tenant.isNotBlank());require(c.name.trim().isNotEmpty());require(c.baseDay in 0..4)};fun representative(r:Representative,tenant:String){require(tenant.isNotBlank());require(r.companyId.isNotBlank());require(r.name.trim().isNotEmpty())};fun visit(v:Visit,tenant:String){require(tenant.isNotBlank());require(v.companyId.isNotBlank());require(v.week in 1..4);require(v.slotIndex>=0)} }
