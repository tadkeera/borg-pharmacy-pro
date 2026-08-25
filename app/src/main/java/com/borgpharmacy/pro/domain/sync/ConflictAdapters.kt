package com.borgpharmacy.pro.domain.sync

import com.borgpharmacy.pro.core.database.entity.*

interface ConflictAdapter<T> { fun resolve(local:T, server:T):T }
class CompanyConflictResolver:ConflictAdapter<CompanyEntity>{ override fun resolve(local:CompanyEntity,server:CompanyEntity)=server }
class RepresentativeConflictResolver:ConflictAdapter<RepresentativeEntity>{ override fun resolve(local:RepresentativeEntity,server:RepresentativeEntity)=server }
class VisitConflictResolver:ConflictAdapter<VisitEntity>{ override fun resolve(local:VisitEntity,server:VisitEntity)=server }
