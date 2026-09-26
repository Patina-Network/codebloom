# `db/`

```mermaid
erDiagram
    Achievement {
        timestamp_with_time_zone createdAt "{NOT_NULL}"
        timestamp_with_time_zone deletedAt 
        text description 
        uuid id PK "{NOT_NULL}"
        boolean isActive 
        Tag leaderboard "<Patina,Hunter,Nyu,Baruch,Rpi,Gwc,Sbu,Ccny,Columbia,Cornell,Bmcc,MHCPlusPlus>"
        uuid leaderboardId FK "{NOT_NULL}"
        AchievementPlaceEnum place "<ONE,TWO,THREE>  {NOT_NULL}"
        character_varying title "{NOT_NULL}"
        uuid userId FK "{NOT_NULL}"
    }

    Announcement {
        timestamp_with_time_zone createdAt "{NOT_NULL}"
        timestamp_with_time_zone expiresAt "{NOT_NULL}"
        uuid id PK "{NOT_NULL}"
        text message "{NOT_NULL}"
        boolean showTimer 
    }

    ApiKey {
        text apiKeyHash UK "{NOT_NULL}"
        timestamp_with_time_zone createdAt "{NOT_NULL}"
        timestamp_with_time_zone expiresAt "{NOT_NULL}"
        uuid id PK "{NOT_NULL}"
        timestamp_with_time_zone updatedAt "{NOT_NULL}"
        uuid updatedBy FK "{NOT_NULL}"
    }

    ApiKeyAccess {
        ApiKeyAccessEnum access UK "<GWC_READ_BY_USER,TEST_VALUE>  {NOT_NULL}"
        uuid apiKeyId FK,UK "{NOT_NULL}"
        uuid id PK "{NOT_NULL}"
    }

    Auth {
        timestamp_with_time_zone createdAt "{NOT_NULL}"
        text csrf 
        uuid id PK "{NOT_NULL}"
        text token "{NOT_NULL}"
    }

    BackgroundTask {
        timestamp_with_time_zone completedAt "{NOT_NULL}"
        uuid id PK "{NOT_NULL}"
        BackgroundTaskEnum task "<LEETCODE_QUESTION_BANK,USER_METRICS>  {NOT_NULL}"
    }

    Club {
        text description 
        uuid id PK "{NOT_NULL}"
        character_varying name "{NOT_NULL}"
        character_varying password "{NOT_NULL}"
        character_varying slug UK "{NOT_NULL}"
        character_varying splashIconUrl 
        Tag tag "<Patina,Hunter,Nyu,Baruch,Rpi,Gwc,Sbu,Ccny,Columbia,Cornell,Bmcc,MHCPlusPlus>  {NOT_NULL}"
    }

    DiscordClub {
        timestamp_with_time_zone createdAt "{NOT_NULL}"
        timestamp_with_time_zone deletedAt 
        text description 
        uuid id PK "{NOT_NULL}"
        text name "{NOT_NULL}"
        Tag tag "<Patina,Hunter,Nyu,Baruch,Rpi,Gwc,Sbu,Ccny,Columbia,Cornell,Bmcc,MHCPlusPlus>  {NOT_NULL}"
    }

    DiscordClubMetadata {
        uuid discordClubId FK "{NOT_NULL}"
        text guildId 
        uuid id PK "{NOT_NULL}"
        text leaderboardChannelId 
    }

    Job {
        integer attempts "{NOT_NULL}"
        timestamp_with_time_zone completedAt 
        timestamp_with_time_zone createdAt "{NOT_NULL}"
        uuid id PK "{NOT_NULL}"
        timestamp_with_time_zone nextAttemptAt "{NOT_NULL}"
        timestamp_with_time_zone processedAt 
        character_varying questionId "{NOT_NULL}"
        JobStatus status "<COMPLETE,PROCESSING,INCOMPLETE>  {NOT_NULL}"
    }

    Leaderboard {
        timestamp_with_time_zone createdAt "{NOT_NULL}"
        timestamp_with_time_zone deletedAt 
        uuid id PK "{NOT_NULL}"
        character_varying name "{NOT_NULL}"
        timestamp_with_time_zone shouldExpireBy 
        text syntaxHighlightingLanguage 
    }

    Lobby {
        timestamp_with_time_zone createdAt "{NOT_NULL}"
        timestamp_with_time_zone expiresAt 
        uuid id PK "{NOT_NULL}"
        text joinCode "{NOT_NULL}"
        integer playerCount "{NOT_NULL}"
        LobbyStatusEnum status "<CLOSED,AVAILABLE,ACTIVE,COMPLETED>  {NOT_NULL}"
        boolean tie "{NOT_NULL}"
        uuid winnerId FK 
    }

    LobbyPlayer {
        uuid id PK "{NOT_NULL}"
        uuid lobbyId FK "{NOT_NULL}"
        uuid playerId FK "{NOT_NULL}"
        integer points "{NOT_NULL}"
    }

    LobbyPlayerQuestion {
        uuid id PK "{NOT_NULL}"
        uuid lobbyPlayerId FK,UK "{NOT_NULL}"
        integer points 
        uuid questionId FK,UK 
    }

    LobbyQuestion {
        timestamp_with_time_zone createdAt "{NOT_NULL}"
        uuid id PK "{NOT_NULL}"
        uuid lobbyId FK "{NOT_NULL}"
        uuid questionBankId FK "{NOT_NULL}"
        integer userSolvedCount "{NOT_NULL}"
    }

    Metadata {
        timestamp_with_time_zone createdAt "{NOT_NULL}"
        uuid id PK "{NOT_NULL}"
        uuid leaderboardId FK "{NOT_NULL}"
        integer totalScore "{NOT_NULL}"
        uuid userId FK "{NOT_NULL}"
    }

    POTD {
        timestamp_without_time_zone createdAt "{NOT_NULL}"
        uuid id PK "{NOT_NULL}"
        double_precision multiplier "{NOT_NULL}"
        character_varying slug "{NOT_NULL}"
        character_varying title "{NOT_NULL}"
    }

    Question {
        double_precision acceptanceRate "{NOT_NULL}"
        text code 
        timestamp_with_time_zone createdAt "{NOT_NULL}"
        text description 
        uuid id PK "{NOT_NULL}"
        text language 
        character_varying memory 
        integer pointsAwarded 
        question_difficulty questionDifficulty "<Easy,Medium,Hard>  {NOT_NULL}"
        text questionLink "{NOT_NULL}"
        smallint questionNumber "{NOT_NULL}"
        character_varying questionSlug "{NOT_NULL}"
        text questionTitle "{NOT_NULL}"
        character_varying runtime 
        text submissionId UK 
        timestamp_with_time_zone submittedAt "{NOT_NULL}"
        uuid userId FK "{NOT_NULL}"
    }

    QuestionBank {
        real acceptanceRate "{NOT_NULL}"
        timestamp_with_time_zone createdAt "{NOT_NULL}"
        text description 
        uuid id PK "{NOT_NULL}"
        question_difficulty questionDifficulty "<Easy,Medium,Hard>  {NOT_NULL}"
        text questionLink "{NOT_NULL}"
        smallint questionNumber "{NOT_NULL}"
        character_varying questionSlug "{NOT_NULL}"
        text questionTitle "{NOT_NULL}"
    }

    QuestionTopic {
        timestamp_with_time_zone createdAt "{NOT_NULL}"
        uuid id PK "{NOT_NULL}"
        uuid questionBankId FK,UK "Exactly one of questionBankId or questionId must be non-null. If questionBankId is non-null, this topic refers to a question bank question."
        uuid questionId FK,UK "Exactly one of questionId or questionBankId must be non-null. If questionId is non-null, this topic refers to a question submission."
        LeetcodeTopicEnum topic UK "<stack,data-stream,rejection-sampling,geometry,counting,design,probability-and-statistics,minimum-spanning-tree,line-sweep,number-theory,rolling-hash,segment-tree,biconnected-component,monotonic-stack,iterator,queue,radix-sort,bucket-sort,shell,memoization,string,prefix-sum,concurrency,database,shortest-path,sorting,linked-list,sliding-window,suffix-array,doubly-linked-list,simulation,ordered-set,graph,math,ordered-map,game-theory,dynamic-programming,recursion,monotonic-queue,matrix,reservoir-sampling,merge-sort,combinatorics,interactive,binary-tree,randomized,bitmask,breadth-first-search,string-matching,greedy,brainteaser,backtracking,bit-manipulation,union-find,binary-search-tree,two-pointers,array,depth-first-search,eulerian-circuit,tree,binary-search,strongly-connected-component,enumeration,heap-priority-queue,divide-and-conquer,hash-function,hash-table,trie,topological-sort,quickselect,binary-indexed-tree,counting-sort,unknown>  {NOT_NULL}"
        text topicSlug "{NOT_NULL}"
    }

    Report {
        timestamp_with_time_zone createdAt "{NOT_NULL}"
        text description "{NOT_NULL}"
        character_varying email 
        uuid id PK "{NOT_NULL}"
        character_varying title "{NOT_NULL}"
    }

    Session {
        timestamp_with_time_zone expiresAt "{NOT_NULL}"
        character_varying id PK "{NOT_NULL}"
        uuid userId FK "{NOT_NULL}"
    }

    User {
        boolean admin "{NOT_NULL}"
        character_varying discordId UK "{NOT_NULL}"
        character_varying discordName "{NOT_NULL}"
        uuid id PK "{NOT_NULL}"
        character_varying leetcodeUsername UK 
        character_varying nickname 
        character_varying profileUrl 
        character_varying schoolEmail 
        text verifyKey 
    }

    UserMetrics {
        timestamp_with_time_zone createdAt "{NOT_NULL}"
        timestamp_with_time_zone deletedAt 
        uuid id PK "{NOT_NULL}"
        integer points "{NOT_NULL}"
        uuid userId FK "{NOT_NULL}"
    }

    UserTag {
        timestamp_with_time_zone createdAt "{NOT_NULL}"
        uuid id PK "{NOT_NULL}"
        Tag tag UK "<Patina,Hunter,Nyu,Baruch,Rpi,Gwc,Sbu,Ccny,Columbia,Cornell,Bmcc,MHCPlusPlus>  {NOT_NULL}"
        uuid userId FK,UK "{NOT_NULL}"
    }

    WeeklyMessage {
        timestamp_with_time_zone createdAt "{NOT_NULL}"
        uuid id PK "{NOT_NULL}"
    }

    flyway_schema_history {
        integer checksum 
        character_varying description "{NOT_NULL}"
        integer execution_time "{NOT_NULL}"
        character_varying installed_by "{NOT_NULL}"
        timestamp_without_time_zone installed_on "{NOT_NULL}"
        integer installed_rank PK "{NOT_NULL}"
        character_varying script "{NOT_NULL}"
        boolean success "{NOT_NULL}"
        character_varying type "{NOT_NULL}"
        character_varying version 
    }

    Achievement }o--|| Leaderboard : "leaderboardId"
    Achievement }o--|| User : "userId"
    ApiKey }o--|| User : "updatedBy"
    ApiKeyAccess }o--|| ApiKey : "apiKeyId"
    DiscordClubMetadata }o--|| DiscordClub : "discordClubId"
    Metadata }o--|| Leaderboard : "leaderboardId"
    Lobby }o--|| User : "winnerId"
    LobbyPlayer }o--|| Lobby : "lobbyId"
    LobbyQuestion }o--|| Lobby : "lobbyId"
    LobbyPlayer }o--|| User : "playerId"
    LobbyPlayerQuestion }o--|| LobbyPlayer : "lobbyPlayerId"
    LobbyPlayerQuestion }o--|| Question : "questionId"
    LobbyQuestion }o--|| QuestionBank : "questionBankId"
    Metadata }o--|| User : "userId"
    Question }o--|| User : "userId"
    QuestionTopic }o--|| Question : "questionId"
    QuestionTopic }o--|| QuestionBank : "questionBankId"
    Session }o--|| User : "userId"
    UserMetrics }o--|| User : "userId"
    UserTag }o--|| User : "userId"
```

<p align="center">
    <i>
        Generated with <code>mermerd -c=postgresql://postgres:passwordnomas@localhost:5432/codebloom-dev-2 -s=public --outputMode=stdout --useAllTables --showDescriptions enumValues,columnComments,notNull</code>
    </i>
</p>
<p align="center">
    <i>
        Also view in <a href="https://mermaid.live/edit#pako:eNrtWntv2zgS_yqC_6aKptemabBYwHG0Ta7Oo7HT4hYBDFqiJa4pUstHHLWb775DyZIl25LlNJveAVc0bSzPDOfx4zxIfe_5IiC94x6RpxSHEsd33IE_fT-i5J7EhGvne_7I_tE0JkrjOJksqI4m9uPkm-DE8SXBmgR97dz1vl9ejSeXt8Ph412vA2tAGFmyrojJg4YvlC9poqngla-MoYEDf68_NS41FYIRzB2q-r6m96TCPcahA98FRE4FlgGI-OUaa8oxOjNcE4kuU4NOsDR-hG4Sij4ufDSaGjTweYoGgpl4SjH8IjlhDJ3Evo8uzgbXzCj782tVi0zRylrngfNbs84Vh18z7BOPm9hJ7G9Wx6tLD42_XqHx2Y3n_eo422X4EZbYBysm91imlIfgdM1I45qZhkYR2aza4x1fAoJzYbj_IoggDwmVRLWwdgBBBiFYQOGQ7ESKisRiDOtLZ93shH4iac1gKxdnj8-wipzbT_sb-NN90yTdJMEOxXLM5GQn6W7YZI7q-z4EourF6vMM6jgnybz5y8evg8mN1z-dnPxncjvybtDYG40nX_rD22bwZ4rlccngjFoCs8NFK-2Njp4L7BY2vpIzZ38UazEnvF3TE-zPQwk7NBhjNe-ms4iTMvM-HUv1lbNYaqsChHHoeePB1ak3-XwL8Tu_upyc9C8hMBDRyYU3vjkfjLaGs7RqwMx0Y-89uSxspkeOY7IHeYKVWoisanRlUcyEbSliCwckfRWd-4LfSrZWuTT8PHPFavX_KVU-2LsRhv-JLiDjbo3wT3bpBdEY8iiuujazK1jRtDcNmYmhocw2F_s7p9KcDCLMwZpSTKnvv0Ut9BT8E0KZxFqTONHqCbWvmnaeA1I_Uu84uKGfm_IU0CZS2Kq1Zsvmnv7TAD8A-Lw5dYCfRxproxyV_wegHFxdXA-9sYeub64G3mh0fvkRnV8WT9txNqw0uT9h6z5_Om5cHno3wwIv64ugI1nDuEq5xg9nNIwY_MAmD4eYhyZrCtc8JqbT9AVb27236x-C8gFMa41UxeaECpISOYCa3KxhZm2OuKxmV1A3vBp5p6j_pX8-7J8MPdQfjM-_eKjA3WljC1Z005rumDkWFJJNMXVsicJ1ZsBGXmz3UT5vWfb2pJnR5S5qJyzdCX5vyHTb1P683OxPVv96pVtbE7uuXl1SJeUsxWxReJuq_2wm3itIhQ0nmM-7hcoOsyPB7knQjP7SBdvq70tY3_VIoDBKC43ZCDqC55nkr6_Gp80mC6Of2-pAmCkjk0QSnyqLttgwTRNG7bi9Xyfdnbz55KN0xDb0b-hq59JEY-6TG_DEjvEuS87PNi3-WOfL6mVuu5diEgtZrZn1pNJfAEhJtbUstuQkoLMZ9SGQafnsdPUIyoiHVYouSEBNjM5ATGPVyLQtZAwpnzfapWLMGOhVUl9CJ74XiArGURuYagqNW0_QNleQkHZoTDbaEDONqVJ5Qi6zcXtfY1l0l9OYbvv-cyWbViEPYGSdYf6zIP1_3DUHdCwS6r9wEdsozXmn4j2A3Sx17GJitk4lZLUtiY3SzhRmMMFdbhh75ZxvcFBVfo0cHcFHnRkryYxI-8HBJYsztcAuPr1qVHm3unVVl6q0qruvqqt0UNNzSIi2RSSLaH6UlsnIT0UhpP4c2Y7FVRoiGSNJ_iC-FegqiDZANEQhETHRMkW-bYDsE9hzNOQIZtUpnlJGdepiHri24aegja9QTDmNTeyqBHMOLC5IJwjEEVctCEkQz_Du6ohAtUBS2N0QuhFWEVIktFcCOcuU-gI6e4B-4No5H_zKNYoFF1pw6ru5BRR2BtZCInCHIUjigD64SkiNpsafE53_riJ7uGILFP2GrY0IjLb2QG2eWQbY37Cab6Qk3E8zv0yxIsAI7OBnN8Ea9IMPlgs0noNWDExGitHAGgBjSCAWSJmZFYilxCDGNgCpWyMH57BcBQHlUMJzRTQKJU4iFNtFiscxTlAIQ2zhqSCFkRbsBt_bq7XYKgKdhZE28hW_5I4AUZI-AAHk83tB5SqoMZEhyd0Cbp1Sbt1nI2fLtU019N46n0O2yQMhIcIipt9IAI91jNUcTQExgY7cGZXgG0Ww9KOlS0Fv7UcZeoA5SIEWW8ngTYmmEDINa8zt9yAMiDlNCocYbtE3A0cW6-eSczX0QrhZJwE7AC39C2VmTQliGJEUc9en0jdUoyWWKuKsooKHEJhtACOATwspq1BEcAL-puAfAHru2IDe04BkqAd2eCaRxa47M9zPmewngA8DlSWFf0QimAipj1nu9T8N9eeKMFi5UAxMJg-gRqZssdlyasPnXCx4e-XJdnZjPSiT_A1JQORzXkLUanDnCkZiTNmPHvB0aMpHJEuLnQzefRe2qUKH2taxn7pV9SOK4vQDB7DP99Boedq77Av3ZbtsOzN7UpDYsghZ-7ITuVq7uuXUDrZGfnLXdnUixYwyUr_S2DLk-RF40VvDWgbbe8gRs9Rexm6JA4zyNh_-l550djhIegr47PXFi3V9xV1J3or809clm-5oOgkrPfKVkDlLL5YX_i_il3LtGUsXOJ0AeiFNTqD303aq3nJtAgRQR6Cva93ZHRJ0IRBKkG-ywSifOrvnQQ4-YYwEk2nacSPUj4ZWAjqouSKWtknfKx_lztj9CofJ3yDYoxqlyT4egwSkVnPr6h2ByptSj8J1__qrdvdyDCvUTvyKRbbwZRXFMuSoLynzd1A2iIqXMOp0y3ctltRLXktfvBtRkG-7iVxyVe99LWvtQrLgX2fqZHV-ybJuSnEfUKNaXgIU0jPGTG5-elyjLY_yOlHXJZdaFHcCW2i3yl-KKVe57s5ffrbMqyGy1aba-U2VLx9Qm8LSAKp16TvI8jOGbtq38uzQvmj_2rWqlvzdlLZ0NVP1UC-UNOgdawmjQg8GCeg-4GMvS993PRjlYnLXs0ww4pkHN8ByDnMEE9KyPwI_jM2_CxEXIqQwYdQ7nmEG0-hymy5friyfwswaLK_oesfv3rzPhPSOv_ceesdHB69eH3x4d_ivd4eHhwevj96iXto7Pnj_7tXBwdH7g7cfDg4PX797e_iIet-yZV-_Ojo6enN0ePD-w9Gbt2_h98e_Aa8Xti4"><code>mermaid.live</code></a>
    </i>
</p>
<p align="center">
    <i>
        Last updated: 09/26/2026
    </i>
</p>

This directory contains the migrations that are applied to our [PostgreSQL](https://www.postgresql.org/) databases.

## Commands

To migrate your local database using your root `.env`, you can simply run:

```bash
just migrate
```

If you need to drop it quickly, you can simply run:

```bash
just drop
```

## Versioned Migrations

Versioned migrations are under `db/migration/`

> [!NOTE]
> Versioned migrations are applied to all databases

### Explanation

- Versioned migrations will only run once.
- You can use these migrations to define tables, schemas, columns (otherwise known as DDL) OR define insertions/updates/deletes of certain columns (otherwise known as DML).
    > If you need to generate mock data **that does not need to be in production**, please look at the docs on [repeatable migrations](#repeatable-migrations).

### Naming Scheme

#### Requirements

`V00{number}__{description}.SQL`

1. Name must be prefixed with a `V`.
1. Version numbers must be sequential and unique.
    - Version numbers must be 4 digits wide. You may pad the left-side with 0s until you reach that goal.
1. Double underscores (\_\_) separate the version from the description
1. Use underscores (\_) instead of spaces in descriptions
1. Files must have `.sql` (or `.SQL`) extension

#### Examples

```txt
V0005__Add_user_table.SQL
V0642__Insert_new_tag_enums.SQL
V9999__Delete_user_table.SQL
```

## Repeatable Migrations

Versioned migrations are under `db/repeated/`

> [!NOTE]
> Repeatable migrations are only applied to local & CI databases. <br />
> Repeatable migrations are **NOT** applied to the production & staging database.

### Explanation

1. Instead of being run just once, repeatable migrations are (re-)applied to a database on [migrate](https://documentation.red-gate.com/fd/migrate-277578887.html) every time their checksum changes.
1. Our main use for repeatable migrations are to generate mock data to use locally and in our CI database, but **is not needed for our production or staging database**.

#### Requirements

1. Name must be prefixed with an `R__Mock`
1. Version numbers must be sequential and unique
1. Use underscores (\_) instead of spaces in descriptions
1. Files must have `.sql` (or `.SQL`) extension

#### Examples

```txt
R__Mock_V0005_Insert_mock_users.SQL
R__Mock_V0011_Add_old_leaderboards.SQL
R__Mock_V9999_Delete_old_mock_users_and_insert_new_users.SQL
```
