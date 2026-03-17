import { useEffect, useMemo, useState } from "react";

type CreateInviteModalProps = {
  token: string | null;
  isLoading: boolean;
  errorMessage: string | null;
  onClose: () => void;
};

export function CreateInviteModal({
  token,
  isLoading,
  errorMessage,
  onClose,
}: CreateInviteModalProps) {
  const [isCopied, setIsCopied] = useState(false);

  const inviteLink = useMemo(() => {
    if (!token) {
      return "";
    }

    return `${window.location.origin}/invites/${token}`;
  }, [token]);

  useEffect(() => {
    setIsCopied(false);
  }, [inviteLink]);

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  async function handleCopyLink() {
    if (!inviteLink) {
      return;
    }

    await navigator.clipboard.writeText(inviteLink);
    setIsCopied(true);
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/35 px-4 py-6">
      <div className="w-full max-w-lg rounded-3xl bg-white shadow-xl ring-1 ring-slate-200">
        <div className="flex items-start justify-between gap-4 border-b border-slate-200 px-6 py-5">
          <h3 className="text-xl font-bold text-slate-900">
            Приглашение участников
          </h3>
        </div>

        <div className="flex flex-col gap-5 px-6 py-6">
          <div>
            <p className="text-sm leading-6 text-slate-600">
              Отправьте эту ссылку человеку, которого вы хотите добавить в этот
              список дел.
            </p>

            <p className="mt-2 text-sm leading-6 text-slate-600">
              Ссылка действует только 24 часа.
            </p>
          </div>

          <div>
            <label
              htmlFor="invite-link"
              className="mb-2 block text-sm font-medium text-slate-700"
            >
              Ссылка для приглашения
            </label>

            <input
              id="invite-link"
              type="text"
              readOnly
              value={isLoading ? "Создаём ссылку-приглашение..." : inviteLink}
              className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-800 outline-none"
            />
          </div>

          {errorMessage && (
            <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-4 text-sm text-red-700">
              {errorMessage}
            </div>
          )}

          <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-between">
            <button
              type="button"
              onClick={onClose}
              className="cursor-pointer rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
            >
              Закрыть
            </button>

            <button
              type="button"
              onClick={() => void handleCopyLink()}
              disabled={!inviteLink || isLoading}
              className="cursor-pointer rounded-xl border border-indigo-200 bg-indigo-50 px-4 py-2.5 text-sm font-semibold text-indigo-700 transition hover:bg-indigo-100 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isCopied ? "Ссылка скопирована" : "Скопировать ссылку"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
