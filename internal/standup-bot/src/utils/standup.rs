use chrono::{
    DateTime,
    Datelike as _,
    Timelike as _,
    Utc,
    Weekday,
};
use chrono_tz::US;

pub fn is_time_to_send_standup_message(last_standup_time: Option<DateTime<Utc>>) -> bool {
    let now = Utc::now().with_timezone(&US::Eastern);
    let weekday = now.weekday();
    let hour = now.hour();

    let is_standup_day = weekday == Weekday::Sat;
    let is_standup_time = hour >= 12;

    if !(is_standup_day && is_standup_time) {
        return false;
    }

    match last_standup_time {
        Some(dt) => {
            let last_standup_et = dt.with_timezone(&US::Eastern);

            let already_sent_today = last_standup_et.date_naive() == now.date_naive();

            !already_sent_today
        }
        None => true,
    }
}
