import { useMemo, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { confirmAliceLink } from "../api/aliceApi";
import { AppLayout } from "../components/AppLayout";
import { PageSection } from "../components/shared/PageSection";
import { getAccessToken } from "../utils/authStorage";
import { parseApiError } from "../utils/parseApiError";

type AliceLinkStatus = "idle" | "loading" | "success" | "error";

export function AliceLinkPage() {
  const location = useLocation();
  const accessToken = getAccessToken();

  const [status, setStatus] = useState<AliceLinkStatus>("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const searchParams = useMemo(
    () => new URLSearchParams(location.search),
    [location.search],
  );

  const redirectUri = searchParams.get("redirect_uri") ?? "";
  const state = searchParams.get("state") ?? undefined;

  const loginUrl = `/login`;

  async function handleConfirm() {
    if (!redirectUri) {
      setErrorMessage(
        "В ссылке отсутствует обязательный параметр redirect_uri.",
      );
      setStatus("error");
      return;
    }

    try {
      setStatus("loading");
      setErrorMessage(null);

      await confirmAliceLink({
        redirectUri,
        state,
      });

      setStatus("success");
    } catch (error) {
      const parsedError = parseApiError(error);
      setErrorMessage(parsedError.message);
      setStatus("error");
    }
  }

  return (
    <AppLayout>
      <div className="flex flex-col gap-6">
        <PageSection title="Привязка навыка Алисы">
          {!redirectUri ? (
            <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-5 text-sm text-red-700">
              В ссылке отсутствует обязательный параметр redirect_uri.
            </div>
          ) : !accessToken ? (
            <div className="flex flex-col gap-4">
              <p className="leading-10 text-slate-600">
                Чтобы привязать навык Алисы, сначала войдите в аккаунт. После
                входа заново запустите авторизацию в навыке Алисы.
              </p>

              <div>
                <Link
                  to={loginUrl}
                  className="inline-flex items-center rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-100"
                >
                  Перейти ко входу
                </Link>
              </div>
            </div>
          ) : status === "success" ? (
            <div className="flex flex-col gap-4">
              <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-5 text-sm text-emerald-700">
                Готово! Запрос на привязку успешно подтверждён.
              </div>
            </div>
          ) : (
            <div className="flex flex-col gap-4">
              <div className="py-5 text-slate-600">
                Навык Алисы запросил доступ к вашим задачам. После подтверждения
                он сможет работать с вашим аккаунтом.
              </div>

              <div>
                <button
                  type="button"
                  onClick={() => void handleConfirm()}
                  disabled={status === "loading"}
                  className="cursor-pointer inline-flex items-center rounded-xl border border-indigo-200 bg-indigo-50 px-4 py-2.5 text-sm font-semibold text-indigo-700 transition hover:bg-indigo-100 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {status === "loading"
                    ? "Подтверждаем..."
                    : "Подтвердить привязку"}
                </button>
              </div>

              {status === "error" && (
                <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-5 text-sm text-red-700 mt-6">
                  {errorMessage ?? "Не удалось подтвердить привязку."}
                </div>
              )}
            </div>
          )}
        </PageSection>
      </div>
    </AppLayout>
  );
}
