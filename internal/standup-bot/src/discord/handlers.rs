use std::sync::Arc;

use chrono::{
    NaiveDate,
    Utc,
};
use chrono_tz::US;
use serenity::{
    all::{
        ChunkGuildFilter,
        Command,
        Context,
        CreateInteractionResponse,
        CreateInteractionResponseMessage,
        EventHandler,
        GuildId,
        Interaction,
        Message,
        Ready,
    },
    async_trait,
};

use crate::{
    common::latch::CountdownLatch,
    discord::commands,
};

#[async_trait]
pub trait StandupStore: Send + Sync {
    async fn current_thread_id(&self) -> anyhow::Result<Option<u64>>;
    async fn record_reply(&self, date: NaiveDate, user_id: u64) -> anyhow::Result<()>;
}

pub struct Handler {
    pub latch: CountdownLatch,
    pub guild_id: u64,
    pub store: Arc<dyn StandupStore>,
}

impl Handler {
    async fn on_interaction(&self, ctx: Context, interaction: Interaction) -> anyhow::Result<()> {
        let Interaction::Command(command) = interaction else {
            return Ok(());
        };

        println!("Received command interaction: {command:#?}");

        let content = match command.data.name.as_str() {
            "hello" => Some(commands::hello::run(&command.data.options())),
            cmd => {
                eprintln!("Command {cmd} not supported");
                None
            }
        };

        if let Some(content) = content {
            let data = CreateInteractionResponseMessage::new().content(content);
            let builder = CreateInteractionResponse::Message(data);
            command.create_response(&ctx.http, builder).await?;
        }

        Ok(())
    }

    async fn on_message(&self, new_message: Message) -> anyhow::Result<()> {
        if new_message.author.bot {
            return Ok(());
        }

        let Some(thread_id) = self.store.current_thread_id().await? else {
            return Ok(());
        };

        if new_message.channel_id.get() != thread_id {
            return Ok(());
        }

        let today = Utc::now().with_timezone(&US::Eastern).date_naive();
        self.store
            .record_reply(today, new_message.author.id.get())
            .await?;

        Ok(())
    }

    async fn on_ready(&self, ctx: Context) -> anyhow::Result<()> {
        let commands = get_commands(self.guild_id, ctx).await?;
        println!("I now have the following guild slash commands: {commands:#?}");

        Ok(())
    }
}

#[async_trait]
impl EventHandler for Handler {
    async fn interaction_create(&self, ctx: Context, interaction: Interaction) {
        if let Err(e) = self.on_interaction(ctx, interaction).await {
            eprintln!("interaction handler failed: {e:#?}");
        }
    }

    async fn message(&self, _ctx: Context, new_message: Message) {
        if let Err(e) = self.on_message(new_message).await {
            eprintln!("message handler failed: {e:#?}");
        }
    }

    async fn ready(&self, ctx: Context, ready: Ready) {
        println!("{} is connected!", ready.user.name);

        if let Err(e) = self.on_ready(ctx).await {
            eprintln!("ready handler failed: {e:#?}");
        }

        self.latch.count_down();
    }

    async fn cache_ready(&self, ctx: Context, _guilds: Vec<GuildId>) {
        ctx.shard.chunk_guild(
            GuildId::new(self.guild_id),
            None,
            false,
            ChunkGuildFilter::None,
            None,
        );
    }
}

pub async fn get_commands(guild_id: u64, ctx: Context) -> Result<Vec<Command>, serenity::Error> {
    let guild = GuildId::new(guild_id);
    return guild
        .set_commands(&ctx.http, vec![commands::hello::register()])
        .await;
}
