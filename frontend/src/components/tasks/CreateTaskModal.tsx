import { useEffect, useState } from "react";
import type { TodoListMemberResponse } from "../../types/lists";
import type {
  AssignmentType,
  CreateTaskRequest,
  RecurrenceType,
} from "../../types/tasks";
import { PrimaryButton } from "../shared/PrimaryButton";
import { TextInput } from "../shared/TextInput";
import { AssignmentRuleEditor } from "./AssignmentRuleEditor";

type CreateTaskModalProps = {
  members: TodoListMemberResponse[];
  isLoading: boolean;
  errorMessage: string | null;
  onClose: () => void;
  onSubmit: (request: CreateTaskRequest) => Promise<void>;
};

const weekdays = [
  { value: 0, shortLabel: "Пн", fullLabel: "Понедельник" },
  { value: 1, shortLabel: "Вт", fullLabel: "Вторник" },
  { value: 2, shortLabel: "Ср", fullLabel: "Среда" },
  { value: 3, shortLabel: "Чт", fullLabel: "Четверг" },
  { value: 4, shortLabel: "Пт", fullLabel: "Пятница" },
  { value: 5, shortLabel: "Сб", fullLabel: "Суббота" },
  { value: 6, shortLabel: "Вс", fullLabel: "Воскресенье" },
] as const;

export function CreateTaskModal({
  members,
  isLoading,
  errorMessage,
  onClose,
  onSubmit,
}: CreateTaskModalProps) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  const [title, setTitle] = useState("");
  const [recurrenceType, setRecurrenceType] =
    useState<RecurrenceType>("WeeklyByDays");
  const [selectedWeekdays, setSelectedWeekdays] = useState<number[]>([]);
  const [intervalDays, setIntervalDays] = useState("1");

  const [assignmentType, setAssignmentType] =
    useState<AssignmentType>("FixedUser");
  const [fixedUserId, setFixedUserId] = useState("");
  const [roundRobinUserIds, setRoundRobinUserIds] = useState<string[]>([]);
  const [weekdayAssignees, setWeekdayAssignees] = useState<
    Record<number, string>
  >({});

  const [validationError, setValidationError] = useState<string | null>(null);

  function toggleWeekday(day: number) {
    setSelectedWeekdays((prev) =>
      prev.includes(day)
        ? prev.filter((x) => x !== day)
        : [...prev, day].sort(),
    );
  }

  function validate(): string | null {
    const normalizedTitle = title.trim();

    if (!normalizedTitle) {
      return "Введите название задачи.";
    }

    if (normalizedTitle.length > 255) {
      return "Название задачи не должно быть длиннее 255 символов.";
    }

    if (recurrenceType === "WeeklyByDays" && selectedWeekdays.length === 0) {
      return "Выберите хотя бы один день недели.";
    }

    if (recurrenceType === "EveryNdays") {
      const interval = Number(intervalDays);

      if (!Number.isInteger(interval) || interval < 1) {
        return "Интервал повторения должен быть целым числом не меньше 1.";
      }
    }

    if (assignmentType === "FixedUser" && !fixedUserId) {
      return "Выберите исполнителя.";
    }

    if (assignmentType === "RoundRobin" && roundRobinUserIds.length === 0) {
      return "Выберите хотя бы одного участника для назначения по кругу.";
    }

    if (assignmentType === "ByWeekday") {
      for (const day of weekdays) {
        if (!weekdayAssignees[day.value]) {
          return "Для назначения по дням недели нужно выбрать исполнителя на каждый день.";
        }
      }
    }

    return null;
  }

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();

    const validationMessage = validate();
    setValidationError(validationMessage);

    if (validationMessage) {
      return;
    }

    const request: CreateTaskRequest = {
      title: title.trim(),
      recurrenceType,
      intervalDays:
        recurrenceType === "EveryNdays" ? Number(intervalDays) : null,
      weekdays: recurrenceType === "WeeklyByDays" ? selectedWeekdays : null,
      assignmentType,
      fixedUserId: assignmentType === "FixedUser" ? fixedUserId : null,
      roundRobinUserIds:
        assignmentType === "RoundRobin" ? roundRobinUserIds : null,
      weekdayAssignees:
        assignmentType === "ByWeekday" ? weekdayAssignees : null,
    };

    await onSubmit(request);
  }

  const visibleError = validationError ?? errorMessage;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 px-4 py-6">
      <div className="max-h-[90vh] w-full max-w-4xl overflow-y-auto rounded-3xl bg-white shadow-2xl">
        <div className="flex items-start justify-between gap-4 border-b border-slate-200 px-6 py-5">
          <div>
            <h2 className="text-2xl font-bold text-slate-900">
              Создание задачи
            </h2>
          </div>

          <button
            type="button"
            onClick={onClose}
            disabled={isLoading}
            className="cursor-pointer rounded-xl border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
          >
            Закрыть
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-6 px-6 py-6">
          <section className="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <h3 className="text-lg font-semibold text-slate-900">
              Название задачи
            </h3>

            <TextInput
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Например, вынести мусор"
              maxLength={255}
            />
          </section>

          <section className="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <div className="flex flex-col gap-1">
              <h3 className="text-lg font-semibold text-slate-900">
                Повторение задачи
              </h3>
              <p className="text-sm text-slate-600">
                Выберите, как часто эта задача должна появляться в списке.
              </p>
            </div>

            <div className="grid gap-3 md:grid-cols-2">
              <label className="flex cursor-pointer flex-col gap-2 rounded-2xl border border-slate-200 bg-white p-4">
                <div className="flex items-center gap-3">
                  <input
                    type="radio"
                    name="recurrenceType"
                    value="WeeklyByDays"
                    checked={recurrenceType === "WeeklyByDays"}
                    onChange={() => setRecurrenceType("WeeklyByDays")}
                  />
                  <span className="font-semibold text-slate-900">
                    По дням недели
                  </span>
                </div>
                <span className="text-sm text-slate-600">
                  Подходит, если задача должна повторяться в конкретные дни,
                  например по понедельникам и четвергам
                </span>
              </label>

              <label className="flex cursor-pointer flex-col gap-2 rounded-2xl border border-slate-200 bg-white p-4">
                <div className="flex items-center gap-3">
                  <input
                    type="radio"
                    name="recurrenceType"
                    value="EveryNdays"
                    checked={recurrenceType === "EveryNdays"}
                    onChange={() => setRecurrenceType("EveryNdays")}
                  />
                  <span className="font-semibold text-slate-900">
                    Через фиксированный интервал
                  </span>
                </div>
                <span className="text-sm text-slate-600">
                  Подходит, если задача должна повторяться каждые N дней,
                  например каждые 3 дня
                </span>
              </label>
            </div>

            {recurrenceType === "WeeklyByDays" && (
              <div className="flex flex-col gap-3">
                <p className="text-sm font-medium text-slate-900">
                  Выберите дни недели
                </p>

                <div className="flex flex-wrap gap-2">
                  {weekdays.map((day) => {
                    const isSelected = selectedWeekdays.includes(day.value);

                    return (
                      <button
                        key={day.value}
                        type="button"
                        onClick={() => toggleWeekday(day.value)}
                        className={`cursor-pointer rounded-xl border px-4 py-2 text-sm font-medium transition ${
                          isSelected
                            ? "border-indigo-500 bg-indigo-600 text-white"
                            : "border-slate-200 bg-white text-slate-700 hover:border-indigo-200 hover:bg-indigo-50 hover:text-indigo-700"
                        }`}
                      >
                        {day.shortLabel}
                      </button>
                    );
                  })}
                </div>
              </div>
            )}

            {recurrenceType === "EveryNdays" && (
              <div className="flex max-w-xs flex-col gap-2">
                <label className="text-sm font-medium text-slate-900">
                  Интервал в днях
                </label>
                <input
                  type="number"
                  min={1}
                  step={1}
                  value={intervalDays}
                  onChange={(e) => setIntervalDays(e.target.value)}
                  className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-indigo-400"
                />
              </div>
            )}
          </section>

          <AssignmentRuleEditor
            members={members}
            value={{
              assignmentType,
              fixedUserId,
              roundRobinUserIds,
              weekdayAssignees,
            }}
            onChange={(next) => {
              setAssignmentType(next.assignmentType);
              setFixedUserId(next.fixedUserId);
              setRoundRobinUserIds(next.roundRobinUserIds);
              setWeekdayAssignees(next.weekdayAssignees);
            }}
          />

          {visibleError && (
            <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {visibleError}
            </div>
          )}

          <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
            <button
              type="button"
              onClick={onClose}
              disabled={isLoading}
              className="cursor-pointer rounded-xl border border-slate-200 px-4 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
            >
              Отмена
            </button>

            <PrimaryButton type="submit" disabled={isLoading}>
              {isLoading ? "Создаём задачу..." : "Создать задачу"}
            </PrimaryButton>
          </div>
        </form>
      </div>
    </div>
  );
}
