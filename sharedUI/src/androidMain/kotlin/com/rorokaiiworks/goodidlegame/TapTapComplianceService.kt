package com.rorokaiiworks.goodidlegame

import android.app.Activity
import co.touchlab.kermit.Logger
import com.taptap.sdk.compliance.TapTapCompliance
import com.taptap.sdk.compliance.TapTapComplianceCallback
import com.taptap.sdk.compliance.constants.ComplianceMessage
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.coroutines.resume

class TapTapComplianceService(val activity: Activity) : ComplianceService, KoinComponent {
    private val logger: Logger by inject { parametersOf("TapTapComplianceService") }

    override suspend fun startCompliance(): Boolean = suspendCancellableCoroutine { continuation ->
        TapTapCompliance.registerComplianceCallback(
            callback = object : TapTapComplianceCallback {
                override fun onComplianceResult(code: Int, extra: Map<String, Any>?) {
                    when (code) {
                        ComplianceMessage.LOGIN_SUCCESS -> {
                            logger.i { "Compliance success, code=$code, extra=$extra" }
                            continuation.resume(true)
                        }
                        else -> {
                            logger.i { "Compliance failed, code=$code, extra=$extra" }
                            continuation.resume(false)
                        }
                    }
                }
            }
        )

        TapTapCompliance.startup(activity = activity, userId = "userIdentifier")
    }
}