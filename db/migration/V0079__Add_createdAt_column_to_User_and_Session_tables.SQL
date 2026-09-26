-- check only applies to new rows not being null, not old rows

ALTER TABLE IF EXISTS "User"
ADD COLUMN "createdAt" TIMESTAMP WITH TIME ZONE;

ALTER TABLE IF EXISTS "User"
ALTER COLUMN "createdAt" SET DEFAULT NOW();

ALTER TABLE IF EXISTS "User"
ADD CONSTRAINT "ck_user_createdAt_not_null" CHECK ("createdAt" IS NOT NULL) NOT VALID;

ALTER TABLE IF EXISTS "Session"
ADD COLUMN "createdAt" TIMESTAMP WITH TIME ZONE;

ALTER TABLE IF EXISTS "Session"
ALTER COLUMN "createdAt" SET DEFAULT NOW();

ALTER TABLE IF EXISTS "Session"
ADD CONSTRAINT "ck_session_createdAt_not_null" CHECK ("createdAt" IS NOT NULL) NOT VALID;
