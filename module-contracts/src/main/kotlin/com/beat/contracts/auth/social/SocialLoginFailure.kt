package com.beat.contracts.auth.social

class SocialLoginFailure private constructor(
    val reason: Reason,
    cause: Throwable? = null,
) : RuntimeException(reason.name, cause) {
    enum class Reason {
        UNSUPPORTED_SOCIAL_TYPE,
        AUTHENTICATION_FAILED,
    }

    companion object {
        @JvmStatic
        fun unsupportedSocialType(): SocialLoginFailure = SocialLoginFailure(Reason.UNSUPPORTED_SOCIAL_TYPE)

        @JvmStatic
        fun authenticationFailed(): SocialLoginFailure = authenticationFailed(null)

        @JvmStatic
        fun authenticationFailed(cause: Throwable?): SocialLoginFailure =
            SocialLoginFailure(Reason.AUTHENTICATION_FAILED, cause)
    }
}
