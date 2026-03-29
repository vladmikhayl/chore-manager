import { useEffect, useState } from "react";
import { AppLayout } from "../components/AppLayout";
import { getProfile } from "../api/profileApi";
import type { ProfileResponse } from "../types/profile";
import { parseApiError } from "../utils/parseApiError";

export function ProfilePage() {
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    async function loadProfile() {
      try {
        setErrorMessage(null);
        const response = await getProfile();
        setProfile(response);
      } catch (error) {
        const parsedError = parseApiError(error);
        setErrorMessage(parsedError.message);
      } finally {
        setIsLoading(false);
      }
    }

    void loadProfile();
  }, []);

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
              <div className="flex flex-col gap-2">
                <h2 className="text-lg font-semibold text-slate-900">
                  Телеграм
                </h2>
                <p className="text-sm text-slate-600">
                  Здесь позже появится блок для привязки Telegram-аккаунта и
                  настройки напоминаний.
                </p>
              </div>
            </section>

            <section className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
              <div className="flex flex-col gap-2">
                <h2 className="text-lg font-semibold text-slate-900">
                  Яндекс Алиса
                </h2>
                <p className="text-sm text-slate-600">
                  Здесь позже появится переход в навык Алисы и привязка
                  аккаунта.
                </p>
              </div>
            </section>
          </>
        )}
      </div>
    </AppLayout>
  );
}
