import { useCallback, useEffect, useRef, useState } from "react";
import { AppLayout } from "../components/AppLayout";
import {
  createTelegramLinkToken,
  deleteTelegramLink,
  getProfile,
  getTelegramLink,
  updateNotificationSettings,
} from "../api/profileApi";
import type { ProfileResponse, TelegramLinkResponse } from "../types/profile";
import { parseApiError } from "../utils/parseApiError";
import toast from "react-hot-toast";

const TELEGRAM_BOT_USERNAME = "chore_manager_reminders_bot";

export function ProfilePage() {
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [telegramLink, setTelegramLink] = useState<TelegramLinkResponse | null>(
    null,
  );

  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [isTelegramConnecting, setIsTelegramConnecting] = useState(false);
  const [isTelegramDeleting, setIsTelegramDeleting] = useState(false);
  const [isReminderUpdating, setIsReminderUpdating] = useState(false);
  const [telegramErrorMessage, setTelegramErrorMessage] = useState<
    string | null
  >(null);

  const hasLoadedOnceRef = useRef(false);

  const loadProfileData = useCallback(async () => {
    try {
      setErrorMessage(null);

      const [profileResponse, telegramLinkResponse] = await Promise.all([
        getProfile(),
        getTelegramLink(),
      ]);

      setProfile(profileResponse);
      setTelegramLink(telegramLinkResponse);
      hasLoadedOnceRef.current = true;
    } catch (error) {
      const parsedError = parseApiError(error);
      setErrorMessage(parsedError.message);

      if (!hasLoadedOnceRef.current) {
        setProfile(null);
        setTelegramLink(null);
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadProfileData();
  }, [loadProfileData]);

  async function handleConnectTelegram() {
    try {
      setIsTelegramConnecting(true);
      setTelegramErrorMessage(null);

      const token = await createTelegramLinkToken();
      const telegramUrl = `https://t.me/${TELEGRAM_BOT_USERNAME}?start=${encodeURIComponent(token)}`;

      window.open(telegramUrl, "_blank", "noopener,noreferrer");
    } catch (error) {
      const parsedError = parseApiError(error);
      setTelegramErrorMessage(parsedError.message);
    } finally {
      setIsTelegramConnecting(false);
    }
  }

  function handleOpenTelegramBot() {
    const telegramUrl = `https://t.me/${TELEGRAM_BOT_USERNAME}`;
    window.open(telegramUrl, "_blank", "noopener,noreferrer");
  }

  async function handleToggleReminders() {
    if (!profile) {
      return;
    }

    const nextValue = !profile.dailyReminderEnabled;

    try {
      setIsReminderUpdating(true);
      setTelegramErrorMessage(null);

      await updateNotificationSettings({
        dailyReminderEnabled: nextValue,
      });

      setProfile({
        ...profile,
        dailyReminderEnabled: nextValue,
      });

      toast.success(
        nextValue
          ? "Напоминания в Telegram включены"
          : "Напоминания в Telegram выключены",
      );
    } catch (error) {
      const parsedError = parseApiError(error);
      setTelegramErrorMessage(parsedError.message);
    } finally {
      setIsReminderUpdating(false);
    }
  }

  async function handleDeleteTelegramLink() {
    try {
      setIsTelegramDeleting(true);
      setTelegramErrorMessage(null);

      await deleteTelegramLink();

      setTelegramLink({
        linked: false,
        chatId: null,
      });

      if (profile) {
        setProfile({
          ...profile,
          dailyReminderEnabled: false,
        });
      }

      toast.success("Привязка Telegram успешно удалена");
    } catch (error) {
      const parsedError = parseApiError(error);
      setTelegramErrorMessage(parsedError.message);
    } finally {
      setIsTelegramDeleting(false);
    }
  }

  const areRemindersEnabled = profile?.dailyReminderEnabled ?? false;
  const isTelegramLinked = telegramLink?.linked ?? false;
  const isTelegramActionInProgress =
    isTelegramConnecting || isTelegramDeleting || isReminderUpdating;
  const reminderTimeLabel = "8:00";
  const reminderStatusText = areRemindersEnabled ? "включены" : "выключены";
  const reminderStatusColor = areRemindersEnabled
    ? "text-emerald-700"
    : "text-slate-500";

  return (
    <AppLayout
      title="Профиль"
      description="Здесь отображается информация о вашем аккаунте и о внешних сервисах."
    >
      <div className="grid gap-6">
        {isLoading ? (
          <section className="rounded-3xl border border-slate-200 bg-slate-50 px-6 py-8 text-sm text-slate-600">
            Загрузка профиля...
          </section>
        ) : errorMessage ? (
          <section className="rounded-3xl border border-red-200 bg-red-50 px-6 py-8 text-sm text-red-700">
            {errorMessage}
          </section>
        ) : (
          <>
            <section className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
              <div className="flex flex-col gap-2">
                <p className="text-sm font-medium text-slate-500">Ваш логин</p>
                <p className="text-2xl font-semibold tracking-tight text-slate-900">
                  {profile?.login}
                </p>
              </div>
            </section>

            <section className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
              <div className="flex flex-col gap-5">
                <div className="flex flex-col gap-3">
                  <h2 className="text-lg font-semibold text-slate-900">
                    Telegram
                  </h2>

                  {!isTelegramLinked ? (
                    <>
                      <p className="text-sm text-slate-600">
                        Подключите Telegram, чтобы получать напоминания о своих
                        задачах через специальный бот.
                      </p>
                      <p className="text-sm text-slate-600">
                        Напоминания приходят{" "}
                        <span className="font-semibold text-slate-900">
                          каждый день в {reminderTimeLabel}
                        </span>
                        .
                      </p>
                    </>
                  ) : (
                    <>
                      <p className="text-sm text-slate-600">
                        Аккаунт подключён. Вы будете получать напоминания обо{" "}
                        <span className="font-semibold text-slate-900">
                          всех ваших задачах
                        </span>{" "}
                        на день.
                      </p>
                      <p className="text-sm text-slate-600">
                        Напоминания приходят{" "}
                        <span className="font-semibold text-slate-900">
                          каждый день в {reminderTimeLabel}
                        </span>
                        .
                      </p>
                      <p className="flex items-center gap-2 text-sm text-slate-600">
                        <span>
                          Сейчас напоминания{" "}
                          <span
                            className={`font-semibold ${reminderStatusColor}`}
                          >
                            {reminderStatusText}
                          </span>
                          .
                        </span>
                      </p>
                    </>
                  )}
                </div>

                {telegramErrorMessage && (
                  <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-4 text-sm text-red-700">
                    {telegramErrorMessage}
                  </div>
                )}

                {!isTelegramLinked ? (
                  <div>
                    <button
                      type="button"
                      onClick={() => void handleConnectTelegram()}
                      disabled={isTelegramConnecting}
                      className="cursor-pointer rounded-xl border border-indigo-200 bg-indigo-50 px-7 py-2.5 text-sm font-semibold text-indigo-700 transition hover:bg-indigo-100 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {isTelegramConnecting
                        ? "Открываем Telegram..."
                        : "Подключить Telegram"}
                    </button>
                  </div>
                ) : (
                  <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap">
                    <button
                      type="button"
                      onClick={handleOpenTelegramBot}
                      disabled={isTelegramActionInProgress}
                      className="cursor-pointer rounded-xl border border-slate-200 bg-slate-50 px-6 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      Перейти в бот
                    </button>

                    <button
                      type="button"
                      onClick={() => void handleToggleReminders()}
                      disabled={isTelegramActionInProgress}
                      className="cursor-pointer rounded-xl border border-indigo-200 bg-indigo-50 px-6 py-2.5 text-sm font-semibold text-indigo-700 transition hover:bg-indigo-100 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {isReminderUpdating
                        ? "Сохраняем..."
                        : areRemindersEnabled
                          ? "Отключить напоминания"
                          : "Включить напоминания"}
                    </button>

                    <button
                      type="button"
                      onClick={() => void handleDeleteTelegramLink()}
                      disabled={isTelegramActionInProgress}
                      className="cursor-pointer rounded-xl border border-red-200 bg-red-50 px-6 py-2.5 text-sm font-semibold text-red-700 transition hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {isTelegramDeleting
                        ? "Отключаем Telegram..."
                        : "Отвязать Telegram"}
                    </button>
                  </div>
                )}
              </div>
            </section>

            <section className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
              <div className="flex flex-col gap-5">
                <div className="flex flex-col gap-3">
                  <h2 className="text-lg font-semibold text-slate-900">
                    Яндекс Алиса
                  </h2>
                  <p className="text-sm text-slate-600">
                    Откройте навык в Яндекс Алисе, чтобы голосом получать
                    информацию о своих задачах.
                  </p>
                  <p className="text-sm text-slate-600">
                    Через навык можно, например, узнать{" "}
                    <span className="font-semibold text-slate-900">
                      свои задачи на сегодня или на завтра
                    </span>
                    .
                  </p>
                </div>

                <div>
                  <button
                    type="button"
                    onClick={() =>
                      window.open("#", "_blank", "noopener,noreferrer")
                    }
                    className="cursor-pointer rounded-xl border border-indigo-200 bg-indigo-50 px-7 py-2.5 text-sm font-semibold text-indigo-700 transition hover:bg-indigo-100"
                  >
                    Перейти в навык
                  </button>
                </div>
              </div>
            </section>
          </>
        )}
      </div>
    </AppLayout>
  );
}
