use std::env;

use anyhow::{
    Context,
    Result,
};

#[derive(Clone)]
pub struct DiscordCredentials {
    #[allow(dead_code)]
    pub client_id: u64,
    #[allow(dead_code)]
    pub client_secret: String,
    pub token: String,
    pub guild_id: u64,
    pub channel_id: u64,
}

impl DiscordCredentials {
    pub fn new() -> Result<Self> {
        Ok(DiscordCredentials {
            client_id: env::var("DISCORD_CLIENT_ID")
                .context("DISCORD_CLIENT_ID is missing from environment")?
                .parse()
                .context("DISCORD_CLIENT_ID is not an integer type")?,
            client_secret: env::var("DISCORD_CLIENT_SECRET")
                .context("DISCORD_CLIENT_SECRET is missing from environment")?,
            token: env::var("DISCORD_TOKEN")
                .context("DISCORD_TOKEN is missing from environment")?,
            guild_id: env::var("DISCORD_GUILD_ID")
                .context("DISCORD_GUILD_ID is missing from environment")?
                .parse()
                .context("DISCORD_GUILD_ID is not an integer type")?,
            channel_id: env::var("DISCORD_CHANNEL_ID")
                .context("DISCORD_CHANNEL_ID is missing from environment")?
                .parse()
                .context("DISCORD_CHANNEL_ID is not an integer type")?,
        })
    }
}

pub struct RedisCredentials {
    pub redis_uri: String,
}

impl RedisCredentials {
    pub fn new() -> Result<Self> {
        Ok(RedisCredentials {
            redis_uri: env::var("REDIS_URI").context("REDIS_URI is missing from environment")?,
        })
    }
}
