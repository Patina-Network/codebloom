-- keep in sync with e2e/src/const.ts

DO $$
BEGIN
INSERT INTO "User"
    (id, "discordId", "discordName", "leetcodeUsername", "nickname", admin, "verifyKey")
VALUES
    ('e17e7905-0871-4e15-bf1c-e653d8ce6412', '900000000000000001', 'e2e-school', NULL, NULL, false, 'iamagoodbot'),
    ('b96dd61c-0e6f-49c6-8753-7458519f4ae8', '900000000000000002', 'e2e-leetcode', NULL, NULL, false, 'iamagoodbot');

INSERT INTO "Session"
    (id, "expiresAt", "userId")
VALUES
    ('5fc6518f5da44d82a89ee3548b65f52e', '2099-01-01 23:59:50', 'e17e7905-0871-4e15-bf1c-e653d8ce6412'),
    ('cd0ebcd1274e4eafaa365331b6c26857', '2099-01-01 23:59:50', 'b96dd61c-0e6f-49c6-8753-7458519f4ae8');

-- leaderboard filter users
INSERT INTO "Leaderboard"
    (id, name, "createdAt", "deletedAt")
VALUES
    ('a0e2e000-0000-4000-8000-000000000001', 'E2E leaderboard', NOW(), NULL);

INSERT INTO "User"
    (id, "discordId", "discordName", "leetcodeUsername", "nickname", admin)
VALUES
    ('a0e2e000-0000-4000-8000-000000000003', '900000000000000003', 'e2e-leader', 'e2e_leader', NULL, false),
    ('a0e2e000-0000-4000-8000-000000000004', '900000000000000004', 'e2e-gwc', 'e2e_gwc', NULL, false);

INSERT INTO "UserTag"
    (id, "createdAt", "userId", tag)
VALUES
    ('a0e2e000-0000-4000-8000-000000000014', NOW(), 'a0e2e000-0000-4000-8000-000000000004', 'Gwc');

INSERT INTO "Metadata"
    (id, "userId", "leaderboardId", "createdAt", "totalScore")
VALUES
    ('a0e2e000-0000-4000-8000-000000000023', 'a0e2e000-0000-4000-8000-000000000003', 'a0e2e000-0000-4000-8000-000000000001', NOW(), 9000),
    ('a0e2e000-0000-4000-8000-000000000024', 'a0e2e000-0000-4000-8000-000000000004', 'a0e2e000-0000-4000-8000-000000000001', NOW(), 1000);
END $$
