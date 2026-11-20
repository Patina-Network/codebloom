import { ApiURL } from "@/lib/api/common/apiURL";
import { http, HttpResponse } from "msw";

const announcementUrl = ApiURL.create("/api/announcement", {
  method: "GET",
});

export const MOCK_ANNOUNCEMENT = {
  id: "announcement-1",
  createdAt: "2026-11-19T06:40:24.917Z",
  expiresAt: "2026-11-19T06:40:24.917Z",
  showTimer: true,
  message: "Announcement Message",
};

export const announcementHandler = http.get(
  announcementUrl.url.toString(),
  () => {
    return HttpResponse.json({
      success: true,
      payload: MOCK_ANNOUNCEMENT,
      message: "Announcement fetched!",
    });
  },
);
