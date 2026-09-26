DO $$
BEGIN
    CASE current_database()
        WHEN 'codebloom-prod', 'codebloom-stg' THEN
            UPDATE "User" u
            SET "schoolEmail" = split_part(u."schoolEmail", '@', 1) || '@' || domains.new_domain
            FROM (VALUES
                ('myhunter.cuny.edu', 'stu-mail.hunter.cuny.edu'),
                ('baruchmail.cuny.edu', 'stu-mail.baruch.cuny.edu'),
                ('citymail.cuny.edu', 'stu-mail.ccny.cuny.edu'),
                ('stu.bmcc.cuny.edu', 'stu-mail.bmcc.cuny.edu')
            ) AS domains(old_domain, new_domain)
            WHERE lower(substring(u."schoolEmail" FROM '@(.*)$')) = domains.old_domain;
        ELSE
            RAISE NOTICE 'Skipping prod/staging only backfill: Current database is %', current_database();
    END CASE;
END $$;
