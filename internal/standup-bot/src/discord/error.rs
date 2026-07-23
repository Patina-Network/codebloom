use crate::common::latch::error::CountdownLatchError;

#[derive(Debug, thiserror::Error)]
pub enum DiscordClientError {
    #[error("serenity error: {0}")]
    Serenity(#[from] serenity::Error),
    #[error("discord client did not become ready in time: {0}")]
    NotReady(#[from] CountdownLatchError),
}
