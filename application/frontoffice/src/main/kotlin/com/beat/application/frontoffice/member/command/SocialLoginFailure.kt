package com.beat.application.frontoffice.member.command

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
        fun unsupportedSocialType(): SocialLoginFailure = SocialLoginFailure(Reason.UNSUPPORTED_SOCIAL_TYPE)

        fun authenticationFailed(cause: Throwable? = null): SocialLoginFailure =
            SocialLoginFailure(Reason.AUTHENTICATION_FAILED, cause)

        fun providerFailure(cause: Throwable? = null): SocialLoginFailure =
            SocialLoginFailure(Reason.PROVIDER_FAILURE, cause)

        fun providerUnavailable(cause: Throwable? = null): SocialLoginFailure =
            SocialLoginFailure(Reason.PROVIDER_UNAVAILABLE, cause)

        fun providerTimeout(cause: Throwable? = null): SocialLoginFailure =
            SocialLoginFailure(Reason.PROVIDER_TIMEOUT, cause)
    }
}
