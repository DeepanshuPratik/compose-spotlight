/**
 * Copyright (c) 2026 DAIA Tech Pvt Ltd. All rights reserved.
 *
 * This software is confidential and proprietary information of DAIA Tech Pvt Ltd
 * and its licensors. You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement.
 *
 * Unauthorized reproduction, modification, distribution, or disclosure of this
 * software is strictly prohibited.
 */

package com.daiatech.composespotlight

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.daiatech.composespotlight.test", appContext.packageName)
    }
}
