package com.borgpharmacy.pro.data.repository
import androidx.room.withTransaction
import com.borgpharmacy.pro.AppContainer
import com.borgpharmacy.pro.core.database.entity.UserEntity
import com.borgpharmacy.pro.core.security.PasswordSecurity
import com.borgpharmacy.pro.domain.security.AppRole
class UserMutationRepository(private val container:AppContainer){
 suspend fun create(tenant:String,actor:String,role:AppRole,username:String,password:CharArray,userId:String){require(username.isNotBlank());require(password.size>=8);container.database.withTransaction{container.database.userDao().upsert(UserEntity(userId,tenant,username,PasswordSecurity.hash(password),"",role==AppRole.SUPER_ADMIN||role==AppRole.FACILITY_ADMIN));container.userAuditService.created(tenant,actor,role.name,userId,username)}}
 suspend fun changePassword(tenant:String,actor:String,role:AppRole,userId:String,password:CharArray){require(password.size>=8);val found=container.database.userDao().findById(tenant,userId);require(found!=null);container.database.userDao().upsert(found.copy(passwordHash=PasswordSecurity.hash(password)));container.userAuditService.passwordChanged(tenant,actor,role.name,userId)}
 suspend fun changeRole(tenant:String,actor:String,role:AppRole,userId:String,newRole:AppRole){val found=container.database.userDao().findById(tenant,userId);require(found!=null);container.database.userDao().upsert(found.copy(isAdmin=newRole==AppRole.SUPER_ADMIN||newRole==AppRole.FACILITY_ADMIN));container.userAuditService.roleChanged(tenant,actor,role.name,userId,newRole.name)}
}
