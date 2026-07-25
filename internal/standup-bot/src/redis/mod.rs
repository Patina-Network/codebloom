pub mod error;

use std::collections::HashSet;

use bb8_redis::{
    RedisConnectionManager,
    bb8::{
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
use serenity::async_trait;

use crate::{
    discord::handlers::StandupStore,
    env::RedisCredentials,
    redis::error::RedisClientError,
};

#[async_trait]
impl StandupStore for RedisClient {
    async fn current_thread_id(&self) -> anyhow::Result<Option<u64>> {
        Ok(self.get_standup_thread_id().await?)
    }

    async fn record_reply(&self, date: NaiveDate, user_id: u64) -> anyhow::Result<()> {
        self.add_standup_reply(date, user_id).await?;

        Ok(())
    }
}

/// Thin wrapper around a bb8 Redis connection pool that exposes the standup
/// operations the bot cares about.
pub struct RedisClient {
    pool: Pool<RedisConnectionManager>,
}

impl RedisClient {
    pub async fn new(creds: &RedisCredentials) -> Result<Self, RedisClientError> {
        let manager = RedisConnectionManager::new(creds.redis_uri.as_str())?;
        let pool = Pool::builder().build(manager).await?;

        Ok(Self { pool })
    }

    async fn conn(&self) -> Result<PooledConnection<'_, RedisConnectionManager>, RedisClientError> {
        Ok(self.pool.get().await?)
    }

    pub async fn set_last_standup(&self, time: DateTime<Utc>) -> Result<(), RedisClientError> {
        let mut conn = self.conn().await?;

        let () = conn.set("standup", time.to_string()).await?;

        Ok(())
    }

    pub async fn get_last_standup(&self) -> Result<Option<DateTime<Utc>>, RedisClientError> {
        let mut conn = self.conn().await?;

        let value: Option<String> = conn.get("standup").await?;

        value
            .map(|v| v.parse::<DateTime<Utc>>())
            .transpose()
            .map_err(Into::into)
    }

    pub async fn set_standup_thread_id(&self, thread_id: u64) -> Result<(), RedisClientError> {
        let mut conn = self.conn().await?;

        let () = conn
            .set("standup:current_thread_id", thread_id.to_string())
            .await?;

        Ok(())
    }

    pub async fn get_standup_thread_id(&self) -> Result<Option<u64>, RedisClientError> {
        let mut conn = self.conn().await?;

        let value: Option<String> = conn.get("standup:current_thread_id").await?;

        Ok(value.and_then(|v| v.parse::<u64>().ok()))
    }

    pub async fn add_standup_reply(
        &self,
        date: NaiveDate,
        user_id: u64,
    ) -> Result<(), RedisClientError> {
        let mut conn = self.conn().await?;

        let () = conn
            .sadd(format!("standup:{date}:replied"), user_id.to_string())
            .await?;

        Ok(())
    }

    pub async fn get_standup_replies(
        &self,
        date: NaiveDate,
    ) -> Result<HashSet<u64>, RedisClientError> {
        let mut conn = self.conn().await?;

        let members: Vec<String> = conn.smembers(format!("standup:{date}:replied")).await?;

        Ok(members
            .into_iter()
            .filter_map(|v| v.parse::<u64>().ok())
            .collect())
    }

    pub async fn set_eod_reminder_sent(&self, date: NaiveDate) -> Result<(), RedisClientError> {
        let mut conn = self.conn().await?;

        let () = conn
            .set(format!("standup:{date}:eod_reminder_sent"), "1")
            .await?;

        Ok(())
    }

    pub async fn get_eod_reminder_sent(&self, date: NaiveDate) -> Result<bool, RedisClientError> {
        let mut conn = self.conn().await?;

        let value: Option<String> = conn
            .get(format!("standup:{date}:eod_reminder_sent"))
            .await?;

        Ok(value.is_some())
    }
}
