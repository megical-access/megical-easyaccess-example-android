package com.megical.easyaccess.sdk

sealed class MegicalException(message: String, cause: Throwable? = null) : Exception(message, cause)
class CouldNotCreateKeyError(cause: Throwable) :
    MegicalException("Could not create key for client", cause)

class AuthenticationError(cause: Throwable) : MegicalException("Authentication failed", cause)
class IssuerError(cause: Throwable) : MegicalException("Issuer failed", cause)
class ClientError(cause: Throwable) : MegicalException("Error creating client", cause)
class DeleteClientError(cause: Throwable) : MegicalException("Error deleting client", cause)
class VerifyError(cause: Throwable) : MegicalException("Verify failed", cause)
class TokenError(cause: Throwable) : MegicalException("Token failed", cause)
class SessionIdNullError : MegicalException("Session id was not set")
class UnknownError(cause: Throwable) : MegicalException("Unknown exception", cause)
class MetadataError(cause: Throwable) : MegicalException("Metadata error", cause)
class StateError(cause: Throwable) : MegicalException("State error", cause)
class IssuerNullError : MegicalException("Issuer configuration was null")
class IdTokenValidationError(message: String) : MegicalException(message)
class InvalidResponse : MegicalException("Invalid response from server")
class JwksError(cause: Throwable) : MegicalException("Error fetching jwks from server", cause)