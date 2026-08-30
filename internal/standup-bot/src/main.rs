use std::sync::Arc;

use anyhow::Result;
use chrono::Utc;
use chrono_tz::US;
use dotenvy::dotenv;
use tokio::time::{
    Duration,
    interval,
};
use tracing::error;

use crate::{
    common::standup::{
        is_time_to_send_eod_reminder,
        is_time_to_send_standup_message,
    },
    discord::DiscordClient,
    env::{
        DiscordCredentials,
        RedisCredentials,
    },
    redis::RedisClient,
};

mod common;
mod discord;
mod env;
mod redis;

fn init_tracing() {
    let is_production = std::env::var("ENVIRONMENT")
        .map(|v| v.eq_ignore_ascii_case("production"))
        .unwrap_or(false);

    let env_filter = tracing_subscriber::EnvFilter::try_from_default_env()
        .unwrap_or_else(|_| tracing_subscriber::EnvFilter::new("info"));

    if is_production {
        tracing_subscriber::fmt()
            .json()
            .with_env_filter(env_filter)
            .init();
    } else {
        tracing_subscriber::fmt()
            .pretty()
            .with_env_filter(env_filter)
            .init();
    }
}

#[tokio::main]
async fn main() -> Result<()> {
    if let Err(e) = dotenv() {
        eprintln!("Failed to load .env but continuing anyways...: {e:#?}\n\n");
    }

    init_tracing();
    std::panic::set_hook(Box::new(tracing_panic::panic_hook));

    let redis_creds = RedisCredentials::new()?;
    let redis_client = Arc::new(RedisClient::new(&redis_creds).await?);

    let discord_creds = DiscordCredentials::new()?;
    let discord_client = Arc::new(DiscordClient::new(&discord_creds, redis_client.clone()).await?);

    let mut interval = interval(Duration::from_mins(15));

    loop {
        interval.tick().await;

        let standup_redis = redis_client.clone();
        let standup_discord = discord_client.clone();
        tokio::spawn(async move {
            match standup_redis.get_last_standup().await {
                Ok(last_standup) if is_time_to_send_standup_message(last_standup) => {
                    let thread_id = match standup_discord.send_standup_message().await {
                        Ok(id) => id,
                        Err(e) => {
                            error!("Failed to send standup message: {e:#?}");
                            return;
                        }
                    };

                    if let Err(e) = standup_redis.set_standup_thread_id(thread_id).await {
                        error!("Failed to persist standup thread id: {e:#?}");
                    }

                    if let Err(e) = standup_redis.set_last_standup(Utc::now()).await {
                        error!("Failed to save standup to Redis: {e:#?}");
                    }
                }
                Err(e) => {
                    error!("Failed to get last standup from Redis: {e:#?}");
                }
                _ => (),
            }
        });

        let eod_redis = redis_client.clone();
        let eod_discord = discord_client.clone();
        tokio::spawn(async move {
            let today = Utc::now().with_timezone(&US::Eastern).date_naive();

            let already_sent = match eod_redis.get_eod_reminder_sent(today).await {
                Ok(v) => v,
                Err(e) => {
                    error!("Failed to get eod_reminder_sent from Redis: {e:#?}");
                    return;
                }
            };

            if !is_time_to_send_eod_reminder(already_sent) {
                return;
            }

            let members = match eod_discord.get_standup_role_members().await {
                Ok(m) => m,
                Err(e) => {
                    error!("Failed to fetch standup role members: {e:#?}");
                    return;
                }
            };

            let replied = match eod_redis.get_standup_replies(today).await {
                Ok(r) => r,
                Err(e) => {
                    error!("Failed to get standup replies from Redis: {e:#?}");
                    return;
                }
            };

            for member in members {
                let user_id = member.user.id.get();
                if replied.contains(&user_id) {
                    continue;
                }
                if let Err(e) = eod_discord.send_eod_reminder_dm(user_id).await {
                    error!("Failed to send EOD reminder DM to {user_id}: {e:#?}");
                }
            }

            if let Err(e) = eod_redis.set_eod_reminder_sent(today).await {
                error!("Failed to save eod_reminder_sent to Redis: {e:#?}");
            }
        });
    }
}
