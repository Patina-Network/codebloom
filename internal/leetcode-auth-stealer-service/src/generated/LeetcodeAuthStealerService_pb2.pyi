from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from typing import ClassVar as _ClassVar, Optional as _Optional

DESCRIPTOR: _descriptor.FileDescriptor

class AuthenticateRequest(_message.Message):
    __slots__ = ("login", "password")
    LOGIN_FIELD_NUMBER: _ClassVar[int]
    PASSWORD_FIELD_NUMBER: _ClassVar[int]
    login: str
    password: str
    def __init__(self, login: _Optional[str] = ..., password: _Optional[str] = ...) -> None: ...

class AuthenticateResponse(_message.Message):
    __slots__ = ("leetcode_session", "csrf_token")
    LEETCODE_SESSION_FIELD_NUMBER: _ClassVar[int]
    CSRF_TOKEN_FIELD_NUMBER: _ClassVar[int]
    leetcode_session: str
    csrf_token: str
    def __init__(self, leetcode_session: _Optional[str] = ..., csrf_token: _Optional[str] = ...) -> None: ...
