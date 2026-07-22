use std::sync::{
    Arc,
    OnceLock,
};

use serenity::{
    Client,
    Error,
    all::{
        Cache,
        ChannelId,
        Colour,
        CreateEmbed,
        CreateEmbedFooter,
        CreateMessage,
        CreateThread,
        GatewayIntents,
        GuildId,
        Member,
        RoleId,
        UserId,
    },
    http::Http,
};

use crate::{
    discord::{
        credentials::DiscordCredentials,
        handlers::Handler,
    },
    redis,
    utils::latch::base::{
        CountdownLatch,
        Latch,
    },
};

const INTENTS: GatewayIntents = GatewayIntents::GUILD_MEMBERS.union(GatewayIntents::GUILD_MESSAGES);
const ROLE_ID: u64 = 1391944565409316944;

static HTTP: OnceLock<Arc<Http>> = OnceLock::new();
static CACHE: OnceLock<Arc<Cache>> = OnceLock::new();

pub async fn init_in_bg(discord_creds: &DiscordCredentials) -> Result<(), Error> {
    let latch = CountdownLatch::new(1);
    let mut client = Client::builder(&discord_creds.token, INTENTS)
        .event_handler(Handler {
            latch: latch.clone(),
        })
        .await?;

    let http = client.http.clone();
    let _ = HTTP
        .set(http)
        .inspect_err(|_| println!("Attempted to save Discord HTTP client more than once"));

    let cache = client.cache.clone();
    let _ = CACHE
        .set(cache)
        .inspect_err(|_| println!("Attempted to save Discord Cache more than once"));

    tokio::spawn(async move {
        if let Err(e) = client.start().await {
            println!("Client error: {e:?}");
        }
    });

    let _ = latch.wait_until(10_000).await;

    Ok(())
}

pub fn get_http() -> Arc<Http> {
    HTTP.get().expect("Discord not initialized").clone()
}

pub fn get_cache() -> Arc<Cache> {
    CACHE.get().expect("Discord not initialized").clone()
}

pub async fn send_standup_message(discord_creds: &DiscordCredentials) -> Result<(), Error> {
    println!("Sending standup message!");

    let embed = CreateEmbed::new()
        .title("Codebloom Standup")
        .description(
            "Standup time! Please leave an update about your latest progress inside of the thread.",
        )
        .footer(
            CreateEmbedFooter::new("Codebloom - Internal")
                .icon_url("https://codebloom.patinanetwork.org/favicon.ico"),
        )
        .colour(Colour::from_rgb(69, 129, 103));
    let create_msg = CreateMessage::new()
        .content(format!("<@&{ROLE_ID}>"))
        .embed(embed);
    let channel = ChannelId::new(discord_creds.channel_id);

    let msg = channel
        .send_message(get_http().as_ref(), create_msg)
        .await
        .inspect_err(|e| println!("Error sending message: {e:#?}"))?;

    let thread_builder = CreateThread::new("Daily Standup Thread");

    let thread = channel
        .create_thread_from_message(get_http().as_ref(), msg.id, thread_builder)
        .await
        .inspect_err(|e| println!("Error creating thread: {e:#?}"))?;

    if let Err(e) = redis::client::set_standup_thread_id(thread.id.get()).await {
        eprintln!("Failed to persist standup thread id: {e:#?}");
    }

    Ok(())
}

pub async fn get_standup_role_members(guild_id: u64) -> Result<Vec<Member>, Error> {
    let guild = GuildId::new(guild_id);
    let role = RoleId::new(ROLE_ID);

    let members = get_cache()
        .guild(guild)
        .map(|g| {
            g.members
                .values()
                .filter(|m| m.roles.contains(&role))
                .cloned()
                .collect()
        })
        .unwrap_or_default();

    Ok(members)
}

pub async fn send_eod_reminder_dm(user_id: u64) -> Result<(), Error> {
    let user = UserId::new(user_id).to_user(get_http().as_ref()).await?;

    let message = CreateMessage::new().content(
        "Reminder: you haven't posted your update in today's standup thread yet. Please add one before end of day!",
    );

    user.dm(get_http().as_ref(), message).await.map(|_| ())
}
