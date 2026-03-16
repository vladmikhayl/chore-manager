import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { acceptInvite } from "../api/listsApi";
import { AppLayout } from "../components/AppLayout";
import { PageSection } from "../components/shared/PageSection";
import { parseApiError } from "../utils/parseApiError";

type AcceptInviteStatus = "loading" | "success" | "error";

export function AcceptInvitePage() {
  const { token } = useParams<{ token: string }>();

  const [status, setStatus] = useState<AcceptInviteStatus>("loading");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const hasRequestedRef = useRef(false);

  useEffect(() => {
    if (!token || hasRequestedRef.current) {
      return;
    }

    hasRequestedRef.current = true;
    const inviteToken = token;

    async function runAcceptInvite() {
      try {
        setStatus("loading");
        setErrorMessage(null);

        await acceptInvite({ token: inviteToken });

        setStatus("success");
      } catch (error) {
        const parsedError = parseApiError(error);
        setErrorMessage(parsedError.message);
        setStatus("error");
      }
    }

    void runAcceptInvite();
  }, [token]);

  return (
    <AppLayout>
      <div className="flex flex-col gap-6">
        <PageSection title="Приглашение в список дел">
          {status === "loading" && (
            <div className="rounded-2xl bg-slate-50 px-4 py-5 text-sm text-slate-600">
              Подключаем вас к списку дел...
            </div>
          )}

          {status === "success" && (
            <div className="flex flex-col gap-4">
              <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-5 text-sm text-emerald-700">
                Готово! Вы успешно подключились к списку дел.
              </div>

              <div>
                <Link
                  to="/lists"
                  className="inline-flex items-center rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-100"
                >
                  Перейти к спискам дел
                </Link>
              </div>
            </div>
          )}

          {status === "error" && (
            <div className="flex flex-col gap-4">
              <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-5 text-sm text-red-700">
                {errorMessage ?? "Не удалось принять приглашение."}
              </div>
            </div>
          )}
        </PageSection>
      </div>
    </AppLayout>
  );
}
