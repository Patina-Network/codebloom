> [!CAUTION]
> We apologize but this page is currently a work in progress
> Reason: Details are rapidly changing, which can lead to outdated information

# Discord Integrations

Codebloom has various Discord integrations that can be setup to work with any club/university/organization Discord server.

_Last updated: 02/16/2026_

As of right now, the Codebloom bot is currently integrated with:

- [Patina Network](https://www.patinanetwork.org/)
- [MHC++ - Hunter College](https://www.linkedin.com/company/mhcplusplus/)
- [Girls Who Code - Hunter College](https://girlswhocode.com/)

> [!NOTE]
> Would you like to register your club/university/organization? If so, please open an [issue](https://github.com/tahminator/codebloom/issues/new) here.

## Automatic messages

This is currently the main integration that Codebloom provides.

### Weekly Leaderboard Updates

This integration will send a weekly update every Saturday at noon (not very strict) about how the current users of the Discord server are performing.

Here is an example of what it may look like:

<div align="center">
    <img src="/screenshots/discord-leaderboard-weekly-update.png" alt="Discord weekly update" />
    <p style="margin-bottom: -5px;">
        <i>
            NOTE: The role mentions would point to actual users in production.
        </i>
    </p>
    <p>
        <i>
            NOTE: This screenshot is taken locally to avoid releasing private information from production.
        </i>
    </p>
</div>

### End of Leaderboard Winners

This integration will send an end of leaderboard, indicating the top 3 winners from this specific Discord server.

Here is an example of what it may look like:

<div align="center">
    <img src="/screenshots/discord-leaderboard-end-winners.png" alt="Discord /command" />
    <p style="margin-bottom: -5px;">
        <i>
            NOTE: The role mentions would point to actual users in production.
        </i>
    </p>
    <p>
        <i>
            NOTE: This screenshot is taken locally to avoid releasing private information from production.
        </i>
    </p>
</div>

## Slash commands

Codebloom is slowly rolling out slash command support for our Discord integrations!

### `/leaderboard` - Return leaderboard update

<img src="/screenshots/discord-leaderboard-cmd.png" alt="Discord /command" />

This slash command will return a refreshed leaderboard embed (similar to the automatic leaderboard embeds)

Current restrcitions:

- If the club requested a leaderboard update before 5 minutes have elapsed, the command will then fail & indicate how long to wait until you can request a new update.
  - **NOTE**: it will not show the fail response to every user, but only to the user who triggered the command.

Here is an example of what it may look like:

<img src="/screenshots/discord-slash-leaderboard.png" alt="Discord /command output" />
