"""Provider boundary; implementations own login/browser behavior, not persistence."""

from dataclasses import dataclass


@dataclass(frozen=True, repr=False)
class Credentials:
    login: str
    password: str


@dataclass(frozen=True, repr=False)
class AuthCookies:
    leetcode_session: str
    csrf_token: str


class AuthenticationError(Exception): ...


class BadCredentials(AuthenticationError): ...


class ManualVerificationRequired(AuthenticationError): ...


class AuthenticationUnavailable(AuthenticationError): ...


class AuthenticationTimeout(AuthenticationError): ...
