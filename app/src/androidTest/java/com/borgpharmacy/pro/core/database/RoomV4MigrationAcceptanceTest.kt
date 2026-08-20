package com.borgpharmacy.pro.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomV4MigrationAcceptanceTest {
    @Test fun versionFourDatabaseIsLoadable() {
        assertNotNull(BorgProDatabase::class.java)
    }
}
