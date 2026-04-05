export type ProfileResponse = {
  login: string;
  dailyReminderEnabled: boolean;
};

export type TelegramLinkResponse = {
  linked: boolean;
  chatId: number | null;
};

export type NotificationSettingsRequest = {
  dailyReminderEnabled?: boolean;
};
