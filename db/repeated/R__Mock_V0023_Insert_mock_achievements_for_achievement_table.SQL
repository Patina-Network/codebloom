DO $$
BEGIN
    INSERT INTO "Achievement"
        (id, "userId", title, description, "isActive", place, leaderboard, "leaderboardId", "createdAt")
    VALUES
        (
            '265dd000-c310-11f0-8d3a-461b1b1abee9',
            '1717a2dc-ce77-4853-a620-82177799ad26',
            'Not expired leaderboard',
            'Achieved first place on the Patina Network leaderboard',
            true,
            'ONE',
            'Patina',
            '39bc2def-669f-4383-8ea3-7202efd613f2',
            '2025-11-11 00:00:00+00'
        );

    INSERT INTO "Achievement"
        (id, "userId", title, description, "isActive", place, leaderboard, "leaderboardId", "createdAt")
    VALUES
        (
            '365dd000-c310-11f0-8d3a-461b1b1abeea',
            '1717a2dc-ce77-4853-a620-82177799ad26',
            'Expired leaderboard',
            'Secured first place on the global leaderboard',
            true,
            'ONE',
            NULL,
            '8fc363b2-b5be-4f1a-9e0e-ea4844fc919c',
            '2025-12-06 00:00:00+00'
        );

END $$;
