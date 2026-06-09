package com.rorokaiiworks.goodidlegame

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import com.rorokaiiworks.goodidlegame.core.ILogin
import com.rorokaiiworks.goodidlegame.ui.ILoginUi
import com.taptap.sdk.kit.internal.callback.TapTapCallback
import com.taptap.sdk.kit.internal.exception.TapTapException
import com.taptap.sdk.login.Scopes
import com.taptap.sdk.login.TapTapAccount
import com.taptap.sdk.login.TapTapLogin
import goodidlegame.sharedui.generated.resources.Res
import goodidlegame.sharedui.generated.resources.taptap_login_btn
import kotlinx.coroutines.suspendCancellableCoroutine
import name.kropp.kotlinx.gettext.I18n
import org.jetbrains.compose.resources.painterResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.coroutines.resume

class TapTapLoginService(private val activity: Activity) : ILogin, KoinComponent {
    private val logger: Logger by inject { parametersOf("TapTapLoginService") }
    private val scopes = mutableSetOf(Scopes.SCOPE_PUBLIC_PROFILE)

    override suspend fun isLoggedIn(): ILogin.LoginResult {
        return when (TapTapLogin.getCurrentTapAccount() != null) {
            true -> ILogin.LoginResult.Success
            false -> ILogin.LoginResult.NotLoggedIn
        }
    }

    override suspend fun login(): ILogin.LoginResult = suspendCancellableCoroutine { continuation ->
        val callback = object : TapTapCallback<TapTapAccount> {
            override fun onSuccess(result: TapTapAccount) {
                logger.i { "TapTapLoginService onSuccess: $result" }
                continuation.resume(ILogin.LoginResult.Success)
            }

            override fun onCancel() {
                logger.i { "TapTapLoginService onCancel" }
                continuation.resume(ILogin.LoginResult.NotLoggedIn)
            }

            override fun onFail(exception: TapTapException) {
                logger.i { "TapTapLoginService onFail: $exception" }
                continuation.resume(ILogin.LoginResult.NotLoggedIn)
            }
        }

        TapTapLogin.loginWithScopes(
            activity,
            scopes.toTypedArray(),
            callback
        )
    }
}


class TapTapLoginUi : ILoginUi {
    @Composable
    override fun LoginButton(
        onClick: () -> Unit,
        i18n: I18n,
        isLoading: Boolean
    ) {
        Image(
            modifier = Modifier.clickable(onClick = onClick),
            painter = painterResource(Res.drawable.taptap_login_btn),
            contentDescription = "Login"
        )
    }
}