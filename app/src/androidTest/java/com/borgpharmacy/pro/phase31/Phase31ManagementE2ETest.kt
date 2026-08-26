package com.borgpharmacy.pro.phase31

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.borgpharmacy.pro.AppContainer
import com.borgpharmacy.pro.domain.model.Company
import com.borgpharmacy.pro.domain.model.FacilityProfile
import com.borgpharmacy.pro.domain.model.CyclePolicy
import com.borgpharmacy.pro.domain.model.Shift
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase31ManagementE2ETest {
    @Test fun facilityAndCompanyUseRealRoomBackedMutationPath() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = AppContainer(context)
        val tenant = "phase31-tenant-a"
        val facility = FacilityProfile(arabicName="مستشفى الاختبار",englishName="Test Hospital",logoPath=null,policy=CyclePolicy.FOUR,tenantId=tenant)
        container.extendedMutationRepository.saveFacilityProfile(facility)
        val savedFacility = container.database.facilityDao().get()
        assertNotNull(savedFacility); assertEquals(tenant,savedFacility!!.tenantId); assertEquals("Test Hospital",savedFacility.englishName)
        val company = Company(name="Demo Pharma",baseDay=1,baseShift=Shift.MORNING)
        container.mutationRepository.saveCompany(tenant,company,"CREATE")
        val savedCompany = container.database.companyDao().page(tenant,10,0).firstOrNull{it.id==company.id}
        assertNotNull(savedCompany); assertEquals(tenant,savedCompany!!.tenantId); assertEquals("Demo Pharma",savedCompany.name)
        container.database.close()
    }
}
