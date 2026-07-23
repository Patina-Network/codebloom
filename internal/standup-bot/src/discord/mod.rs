pub mod commands;
pub mod error;
pub mod handlers;

use std::sync::Arc;

use serenity::{
    Client,
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
    common::latch::CountdownLatch,
    discord::{
        error::DiscordClientError,
        handlers::{
            Handler,
            StandupStore,
        },
    },
    env::DiscordCredentials,
};

const INTENTS: GatewayIntents = GatewayIntents::GUILD_MEMBERS.union(GatewayIntents::GUILD_MESSAGES);
const ROLE_ID: u64 = 1391944565409316944;

pub struct DiscordClient {
    http: Arc<Http>,
    cache: Arc<Cache>,
    guild_id: u64,
    channel_id: u64,
}

impl DiscordClient {
    pub async fn new(
        creds: &DiscordCredentials,
        store: Arc<dyn StandupStore>,
    ) -> Result<Self, DiscordClientError> {
        let latch = CountdownLatch::new(1);
        let mut client = Client::builder(&creds.token, INTENTS)
            .event_handler(Handler {
                latch: latch.clone(),
                guild_id: creds.guild_id,
                store,
            })
            .await?;

        let http = client.http.clone();
        let cache = client.cache.clone();

        tokio::spawn(async move {
            if let Err(e) = client.start().await {
                eprintln!("Client error: {e:?}");
            }
        });

        latch.wait_until(10_000).await?;

        Ok(Self {
            http,
            cache,
            guild_id: creds.guild_id,
            channel_id: creds.channel_id,
        })
    }

    pub async fn send_standup_message(&self) -> Result<u64, DiscordClientError> {
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
        let channel = ChannelId::new(self.channel_id);

        let msg = channel
            .send_message(self.http.as_ref(), create_msg)
            .await
            .inspect_err(|e| eprintln!("Error sending message: {e:#?}"))?;

        let thread_builder = CreateThread::new("Daily Standup Thread");

        let thread = channel
            .create_thread_from_message(self.http.as_ref(), msg.id, thread_builder)
            .await
            .inspect_err(|e| eprintln!("Error creating thread: {e:#?}"))?;

        Ok(thread.id.get())
    }

    pub async fn get_standup_role_members(&self) -> Result<Vec<Member>, DiscordClientError> {
        let guild = GuildId::new(self.guild_id);
        let role = RoleId::new(ROLE_ID);

        let members = self
            .cache
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

    pub async fn send_eod_reminder_dm(&self, user_id: u64) -> Result<(), DiscordClientError> {
        let user = UserId::new(user_id).to_user(self.http.as_ref()).await?;

        let message = CreateMessage::new().content(
            "Reminder: you haven't posted your update in today's standup thread yet. Please add one before end of day!",
        );

        user.dm(self.http.as_ref(), message).await?;

        Ok(())
    }
}
