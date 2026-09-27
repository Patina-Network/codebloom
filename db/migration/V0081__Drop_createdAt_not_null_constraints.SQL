SET LOCAL lock_timeout = '5s';

ALTER TABLE "User"
DROP CONSTRAINT "ck_user_createdAt_not_null";

ALTER TABLE "Session"
DROP CONSTRAINT "ck_session_createdAt_not_null";
