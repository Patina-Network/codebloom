use chrono::Utc;
use chrono_tz::US;
use serenity::{
    all::{
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
    discord::{
        commands,
        credentials,
    },
    redis,
    utils::latch::base::{
        CountdownLatch,
        Latch as _,
    },
};

pub struct Handler {
    pub latch: CountdownLatch,
}

#[async_trait]
impl EventHandler for Handler {
    async fn interaction_create(&self, ctx: Context, interaction: Interaction) {
        if let Interaction::Command(command) = interaction {
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
                if let Err(why) = command.create_response(&ctx.http, builder).await {
                    eprintln!("Cannot respond to slash command: {why}");
                }
            }
        }
    }

    async fn message(&self, _ctx: Context, new_message: Message) {
        if new_message.author.bot {
            return;
        }

        let thread_id = match redis::client::get_standup_thread_id().await {
            Ok(Some(id)) => id,
            Ok(None) => return,
            Err(e) => {
                eprintln!("Failed to get standup thread id: {e:#?}");
                return;
            }
        };

        if new_message.channel_id.get() != thread_id {
            return;
        }

        let today = Utc::now().with_timezone(&US::Eastern).date_naive();
        if let Err(e) = redis::client::add_standup_reply(today, new_message.author.id.get()).await
        {
            eprintln!("Failed to record standup reply: {e:#?}");
        }
    }

    async fn ready(&self, ctx: Context, ready: Ready) {
        println!("{} is connected!", ready.user.name);

        let creds = credentials::get_discord_credentials();

        let commands = get_commands(creds.guild_id, ctx).await;
        println!("I now have the following guild slash commands: {commands:#?}");

        self.latch.count_down();
    }
}

pub async fn get_commands(guild_id: u64, ctx: Context) -> Result<Vec<Command>, serenity::Error> {
    let guild = GuildId::new(guild_id);
    return guild
        .set_commands(&ctx.http, vec![commands::hello::register()])
        .await;
}
