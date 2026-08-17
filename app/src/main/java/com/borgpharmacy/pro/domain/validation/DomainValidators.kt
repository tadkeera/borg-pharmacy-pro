package com.borgpharmacy.pro.domain.validation

import com.borgpharmacy.pro.domain.model.Company
import com.borgpharmacy.pro.domain.model.CyclePolicy

object DomainValidators {
    fun tenantId(value: String): String = value.trim().also {
        require(it.isNotEmpty()) { "tenantId is required" }
        require(it.length <= 128) { "tenantId is too long" }
    }

    fun company(company: Company, policy: CyclePolicy) {
        require(company.id.isNotBlank()) { "company id is required" }
        require(company.name.trim().isNotEmpty()) { "company name is required" }
        require(company.name.length <= 200) { "company name is too long" }
        require(company.baseDay in 0..4) { "company baseDay must be between 0 and 4" }
        require(policy.visits in 1..4) { "cycle policy must contain between 1 and 4 visits" }
    }
}
