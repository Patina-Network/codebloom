use bb8_redis::{
    RedisConnectionManager,
    bb8::{
        self,
        Pool,
        PooledConnection,
    },
    redis::AsyncCommands,
};
use chrono::{
    DateTime,
    NaiveDate,
    Utc,
};
use std::collections::HashSet;
use std::sync::OnceLock;

use crate::redis::{
    credentials::RedisCredentials,
    error::{
        RedisClientError,
        RedisSingletonEmptyError,
    },
};

static INSTANCE: OnceLock<Pool<RedisConnectionManager>> = OnceLock::new();

pub async fn init(creds: &RedisCredentials) -> Result<(), RedisClientError> {
    let manager = RedisConnectionManager::new(creds.redis_uri.as_str())?;
    let pool = bb8::Pool::builder().build(manager).await?;

    match INSTANCE.set(pool) {
        Ok(_) => (),
        Err(_) => println!("Attempted to save Redis INSTANCE more than once"),
    }

    Ok(())
}

async fn get_pool() -> Result<PooledConnection<'static, RedisConnectionManager>, RedisClientError> {
    match INSTANCE.get() {
        Some(pool) => pool.get().await.map_err(RedisClientError::from),
        None => Err(RedisSingletonEmptyError.into()),
    }
}

pub async fn set_last_standup(time: DateTime<Utc>) -> Result<(), RedisClientError> {
    let mut conn = get_pool().await?;

    Ok(conn.set("standup", time.to_string()).await?)
}

pub async fn get_last_standup() -> Result<Option<DateTime<Utc>>, RedisClientError> {
    let mut conn = get_pool().await?;

    let value: Option<String> = conn.get("standup").await?;

    value
        .map(|v| v.parse::<DateTime<Utc>>())
        .transpose()
        .map_err(RedisClientError::from)
}

pub async fn set_standup_thread_id(thread_id: u64) -> Result<(), RedisClientError> {
    let mut conn = get_pool().await?;

    Ok(conn
        .set("standup:current_thread_id", thread_id.to_string())
        .await?)
}

pub async fn get_standup_thread_id() -> Result<Option<u64>, RedisClientError> {
    let mut conn = get_pool().await?;

    let value: Option<String> = conn.get("standup:current_thread_id").await?;

    Ok(value.and_then(|v| v.parse::<u64>().ok()))
}

pub async fn add_standup_reply(date: NaiveDate, user_id: u64) -> Result<(), RedisClientError> {
    let mut conn = get_pool().await?;

    Ok(conn
        .sadd(format!("standup:{date}:replied"), user_id.to_string())
        .await?)
}

pub async fn get_standup_replies(date: NaiveDate) -> Result<HashSet<u64>, RedisClientError> {
    let mut conn = get_pool().await?;

    let members: Vec<String> = conn.smembers(format!("standup:{date}:replied")).await?;

    Ok(members
        .into_iter()
        .filter_map(|v| v.parse::<u64>().ok())
        .collect())
}

pub async fn set_eod_reminder_sent(date: NaiveDate) -> Result<(), RedisClientError> {
    let mut conn = get_pool().await?;

    Ok(conn
        .set(format!("standup:{date}:eod_reminder_sent"), "1")
        .await?)
}

pub async fn get_eod_reminder_sent(date: NaiveDate) -> Result<bool, RedisClientError> {
    let mut conn = get_pool().await?;

    let value: Option<String> = conn.get(format!("standup:{date}:eod_reminder_sent")).await?;

    Ok(value.is_some())
}
