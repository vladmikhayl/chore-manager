import { useEffect, useMemo, useState } from "react";
import { getTaskCompletion } from "../../api/tasksApi";
import type { TaskCompletionStatusResponse } from "../../types/tasks";
import { parseApiError } from "../../utils/parseApiError";
import { TaskDatePicker } from "./TaskDatePicker";

type TaskCompletionStatusModalProps = {
  taskId: string;
  taskTitle: string;
  onClose: () => void;
};

function getTodayDateString(): string {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function formatDateTime(value: string | null): string {
  if (!value) {
    return "Не указано";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString("ru-RU", {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

export function TaskCompletionStatusModal({
  taskId,
  taskTitle,
  onClose,
}: TaskCompletionStatusModalProps) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  const todayDate = useMemo(() => getTodayDateString(), []);
  const [selectedDate, setSelectedDate] = useState(todayDate);
  const [status, setStatus] = useState<TaskCompletionStatusResponse | null>(
    null,
  );
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!selectedDate) {
      setStatus(null);
      setErrorMessage(null);
      setIsLoading(false);
      return;
    }

    async function loadCompletion() {
      try {
        setIsLoading(true);
        setErrorMessage(null);

        const response = await getTaskCompletion(taskId, selectedDate);
        setStatus(response);
      } catch (error) {
        const parsedError = parseApiError(error);
        setErrorMessage(parsedError.message);
        setStatus(null);
      } finally {
        setIsLoading(false);
      }
    }

    void loadCompletion();
  }, [taskId, selectedDate]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 px-4 py-6">
      <div className="w-full max-w-lg rounded-3xl bg-white p-6 shadow-2xl">
        <div className="flex items-start justify-between gap-4">
          <div className="flex flex-col gap-1">
            <h2 className="text-xl font-bold text-slate-900">
              Выполнение задачи
            </h2>
            <p className="break-words text-sm text-slate-600">{taskTitle}</p>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="cursor-pointer rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-sm font-medium text-slate-600 transition hover:bg-slate-50 hover:text-slate-900"
          >
            Закрыть
          </button>
        </div>

        <div className="mt-5">
          <TaskDatePicker
            id={`task-completion-date-${taskId}`}
            label="Дата выполнения"
            value={selectedDate}
            max={todayDate}
            onChange={setSelectedDate}
          />
        </div>

        <div className="mt-5">
          {!selectedDate ? (
            <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-600">
              Выберите дату, чтобы посмотреть выполнение задачи.
            </div>
          ) : isLoading ? (
            <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-600">
              Загружаем статус выполнения...
            </div>
          ) : errorMessage ? (
            <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-5 text-sm text-red-700">
              {errorMessage}
            </div>
          ) : !status ? (
            <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-600">
              Не удалось получить данные по выполнению.
            </div>
          ) : status.completed ? (
            <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-5">
              <div className="flex flex-col gap-2 text-sm text-slate-700">
                <p className="font-semibold text-emerald-700">
                  Задача выполнена ✅
                </p>

                <p>
                  <span className="font-medium text-slate-900">
                    Кто отметил:
                  </span>{" "}
                  {status.completedByLogin ?? "Неизвестный пользователь"}
                </p>

                <p>
                  <span className="font-medium text-slate-900">
                    Когда отметил:
                  </span>{" "}
                  {formatDateTime(status.completedAt)}
                </p>
              </div>
            </div>
          ) : (
            <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-5">
              <p className="text-sm font-medium text-slate-900">
                За выбранную дату выполнение не отмечено.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
