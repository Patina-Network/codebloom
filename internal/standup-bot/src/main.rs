use std::error::Error;

use chrono::Utc;
use chrono_tz::US;
use dotenvy::dotenv;
use tokio::time::interval;

use crate::utils::standup::{
    is_time_to_send_eod_reminder,
    is_time_to_send_standup_message,
};

mod discord;
mod redis;
mod utils;

const FIFTEEN_MINUTES_AS_SECONDS: u64 = 15 * 60;

#[tokio::main]
async fn main() -> Result<(), Box<dyn Error>> {
    match dotenv() {
        Ok(_) => (),
        Err(e) => eprintln!("Failed to load .env but continuing anyways...: {e:#?}\n\n"),
    }

    let redis_creds = redis::credentials::get_redis_credentials();
    redis::client::init(&redis_creds).await?;

    let discord_creds = discord::credentials::get_discord_credentials();
    discord::client::init_in_bg(&discord_creds).await?;

    let mut interval = interval(tokio::time::Duration::from_secs(FIFTEEN_MINUTES_AS_SECONDS));

    loop {
        let cloned_discord_creds = discord_creds.clone();
        interval.tick().await;
        tokio::spawn(async move {
            match redis::client::get_last_standup().await {
                Ok(last_standup) => {
                    if !is_time_to_send_standup_message(last_standup) {
                        return;
                    }
                    if let Err(e) =
                        discord::client::send_standup_message(&cloned_discord_creds).await
                    {
                        eprintln!("Failed to send standup message: {e:#?}");
                        return;
                    }

                    if let Err(e) = redis::client::set_last_standup(Utc::now()).await {
                        eprintln!("Failed to save standup to Redis: {e:#?}");
                    }
                }
                Err(e) => {
                    eprintln!("Failed to get last standup from Redis: {e:#?}");
                }
            }
        });

        let cloned_discord_creds = discord_creds.clone();
        tokio::spawn(async move {
            let today = Utc::now().with_timezone(&US::Eastern).date_naive();

            let already_sent = match redis::client::get_eod_reminder_sent(today).await {
                Ok(v) => v,
                Err(e) => {
                    eprintln!("Failed to get eod_reminder_sent from Redis: {e:#?}");
                    return;
                }
            };

            if !is_time_to_send_eod_reminder(already_sent) {
                return;
            }

            let members =
                match discord::client::get_standup_role_members(cloned_discord_creds.guild_id)
                    .await
                {
                    Ok(m) => m,
                    Err(e) => {
                        eprintln!("Failed to fetch standup role members: {e:#?}");
                        return;
                    }
                };

            let replied = match redis::client::get_standup_replies(today).await {
                Ok(r) => r,
                Err(e) => {
                    eprintln!("Failed to get standup replies from Redis: {e:#?}");
                    return;
                }
            };

            for member in members {
                let user_id = member.user.id.get();
                if replied.contains(&user_id) {
                    continue;
                }
                if let Err(e) = discord::client::send_eod_reminder_dm(user_id).await {
                    eprintln!("Failed to send EOD reminder DM to {user_id}: {e:#?}");
                }
            }

            if let Err(e) = redis::client::set_eod_reminder_sent(today).await {
                eprintln!("Failed to save eod_reminder_sent to Redis: {e:#?}");
            }
        });
    }
}
