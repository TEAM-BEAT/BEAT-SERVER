package com.beat.contracts.auth.social

class SocialLoginFailure private constructor(
    val reason: Reason,
    cause: Throwable? = null,
) : RuntimeException(reason.name, cause) {
    enum class Reason {
        UNSUPPORTED_SOCIAL_TYPE,
        AUTHENTICATION_FAILED,
        PROVIDER_FAILURE,
        PROVIDER_UNAVAILABLE,
        PROVIDER_TIMEOUT,
    }

    companion object {
        @JvmStatic
        fun unsupportedSocialType(): SocialLoginFailure = SocialLoginFailure(Reason.UNSUPPORTED_SOCIAL_TYPE)

        @JvmOverloads
        @JvmStatic
        fun authenticationFailed(cause: Throwable? = null): SocialLoginFailure =
            SocialLoginFailure(Reason.AUTHENTICATION_FAILED, cause)

        @JvmOverloads
        @JvmStatic
        fun providerFailure(cause: Throwable? = null): SocialLoginFailure =
            SocialLoginFailure(Reason.PROVIDER_FAILURE, cause)

        @JvmOverloads
        @JvmStatic
        fun providerUnavailable(cause: Throwable? = null): SocialLoginFailure =
            SocialLoginFailure(Reason.PROVIDER_UNAVAILABLE, cause)

        @JvmOverloads
        @JvmStatic
        fun providerTimeout(cause: Throwable? = null): SocialLoginFailure =
            SocialLoginFailure(Reason.PROVIDER_TIMEOUT, cause)
    }
}
