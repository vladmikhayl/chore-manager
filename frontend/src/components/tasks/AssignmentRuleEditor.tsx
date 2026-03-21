import { useMemo } from "react";
import type { TodoListMemberResponse } from "../../types/lists";
import type { AssignmentType } from "../../types/tasks";

type AssignmentRuleValue = {
  assignmentType: AssignmentType;
  fixedUserId: string;
  roundRobinUserIds: string[];
  weekdayAssignees: Record<number, string>;
};

type AssignmentRuleEditorProps = {
  members: TodoListMemberResponse[];
  value: AssignmentRuleValue;
  onChange: (value: AssignmentRuleValue) => void;
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

export function AssignmentRuleEditor({
  members,
  value,
  onChange,
}: AssignmentRuleEditorProps) {
  const sortedMembers = useMemo(() => [...members], [members]);

  const availableRoundRobinMembers = sortedMembers.filter(
    (member) => !value.roundRobinUserIds.includes(member.userId),
  );

  function patch(next: Partial<AssignmentRuleValue>) {
    onChange({
      ...value,
      ...next,
    });
  }

  function setAssignmentType(assignmentType: AssignmentType) {
    patch({ assignmentType });
  }

  function addRoundRobinUser(userId: string) {
    if (!userId) {
      return;
    }

    if (value.roundRobinUserIds.includes(userId)) {
      return;
    }

    patch({
      roundRobinUserIds: [...value.roundRobinUserIds, userId],
    });
  }

  function removeRoundRobinUser(userId: string) {
    patch({
      roundRobinUserIds: value.roundRobinUserIds.filter((x) => x !== userId),
    });
  }

  function moveRoundRobinUser(userId: string, direction: "up" | "down") {
    const index = value.roundRobinUserIds.indexOf(userId);

    if (index === -1) {
      return;
    }

    if (direction === "up" && index === 0) {
      return;
    }

    if (direction === "down" && index === value.roundRobinUserIds.length - 1) {
      return;
    }

    const next = [...value.roundRobinUserIds];
    const targetIndex = direction === "up" ? index - 1 : index + 1;

    [next[index], next[targetIndex]] = [next[targetIndex], next[index]];

    patch({ roundRobinUserIds: next });
  }

  function handleWeekdayAssigneeChange(day: number, userId: string) {
    patch({
      weekdayAssignees: {
        ...value.weekdayAssignees,
        [day]: userId,
      },
    });
  }

  return (
    <section className="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <div className="flex flex-col gap-1">
        <h3 className="text-lg font-semibold text-slate-900">
          Назначение ответственного
        </h3>
        <p className="text-sm text-slate-600">
          Выберите, по какому правилу система будет автоматически назначать
          исполнителя задачи
        </p>
      </div>

      <div className="grid gap-3 md:grid-cols-3">
        <label className="flex cursor-pointer flex-col gap-2 rounded-2xl border border-slate-200 bg-white p-4">
          <div className="flex items-center gap-3">
            <input
              type="radio"
              name="assignmentType"
              value="FixedUser"
              checked={value.assignmentType === "FixedUser"}
              onChange={() => setAssignmentType("FixedUser")}
            />
            <span className="font-semibold text-slate-900">
              Всегда один исполнитель
            </span>
          </div>
          <span className="text-sm text-slate-600">
            Один и тот же участник будет назначаться каждый раз
          </span>
        </label>

        <label className="flex cursor-pointer flex-col gap-2 rounded-2xl border border-slate-200 bg-white p-4">
          <div className="flex items-center gap-3">
            <input
              type="radio"
              name="assignmentType"
              value="RoundRobin"
              checked={value.assignmentType === "RoundRobin"}
              onChange={() => setAssignmentType("RoundRobin")}
            />
            <span className="font-semibold text-slate-900">По кругу</span>
          </div>
          <span className="text-sm text-slate-600">
            Участники будут назначаться по очереди
          </span>
        </label>

        <label className="flex cursor-pointer flex-col gap-2 rounded-2xl border border-slate-200 bg-white p-4">
          <div className="flex items-center gap-3">
            <input
              type="radio"
              name="assignmentType"
              value="ByWeekday"
              checked={value.assignmentType === "ByWeekday"}
              onChange={() => setAssignmentType("ByWeekday")}
            />
            <span className="font-semibold text-slate-900">По дням недели</span>
          </div>
          <span className="text-sm text-slate-600">
            Для каждого дня недели можно выбрать отдельного участника
          </span>
        </label>
      </div>

      {value.assignmentType === "FixedUser" && (
        <div className="flex max-w-md flex-col gap-2">
          <label className="text-sm font-medium text-slate-900">
            Исполнитель
          </label>
          <select
            value={value.fixedUserId}
            onChange={(e) => patch({ fixedUserId: e.target.value })}
            className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-indigo-400"
          >
            <option value="">Выберите участника</option>
            {sortedMembers.map((member) => (
              <option key={member.userId} value={member.userId}>
                {member.login}
              </option>
            ))}
          </select>
        </div>
      )}

      {value.assignmentType === "RoundRobin" && (
        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-1">
            <p className="text-sm font-medium text-slate-900">
              Порядок участников
            </p>
            <p className="text-sm text-slate-600">
              Система будет назначать участников сверху вниз и потом снова с
              начала списка.
            </p>
          </div>

          <div className="flex max-w-md flex-col gap-2">
            <label className="text-sm font-medium text-slate-900">
              Добавить участника
            </label>

            <select
              defaultValue=""
              onChange={(e) => {
                addRoundRobinUser(e.target.value);
                e.currentTarget.value = "";
              }}
              className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-indigo-400"
            >
              <option value="">Выберите участника</option>
              {availableRoundRobinMembers.map((member) => (
                <option key={member.userId} value={member.userId}>
                  {member.login}
                </option>
              ))}
            </select>
          </div>

          {value.roundRobinUserIds.length === 0 ? (
            <div className="rounded-xl border border-dashed border-slate-300 bg-white px-4 py-4 text-sm text-slate-600">
              Пока никто не добавлен.
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              {value.roundRobinUserIds.map((userId, index) => {
                const member = sortedMembers.find((x) => x.userId === userId);

                if (!member) {
                  return null;
                }

                return (
                  <div
                    key={userId}
                    className="flex items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3"
                  >
                    <div className="flex items-center gap-3">
                      <span className="flex h-7 w-7 items-center justify-center rounded-full bg-indigo-100 text-xs font-semibold text-indigo-700">
                        {index + 1}
                      </span>

                      <span className="text-sm font-medium text-slate-900">
                        {member.login}
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => moveRoundRobinUser(userId, "up")}
                        disabled={index === 0}
                        className="rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        ↑
                      </button>

                      <button
                        type="button"
                        onClick={() => moveRoundRobinUser(userId, "down")}
                        disabled={index === value.roundRobinUserIds.length - 1}
                        className="rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        ↓
                      </button>

                      <button
                        type="button"
                        onClick={() => removeRoundRobinUser(userId)}
                        className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-red-700 transition hover:bg-red-100"
                      >
                        Убрать
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {value.assignmentType === "ByWeekday" && (
        <div className="flex flex-col gap-3">
          <p className="text-sm font-medium text-slate-900">
            Исполнитель для каждого дня недели
          </p>

          <div className="grid gap-3">
            {weekdays.map((day) => (
              <div
                key={day.value}
                className="grid gap-2 rounded-xl border border-slate-200 bg-white p-3 md:grid-cols-[180px_1fr]"
              >
                <div className="flex items-center text-sm font-medium text-slate-900">
                  {day.fullLabel}
                </div>

                <select
                  value={value.weekdayAssignees[day.value] ?? ""}
                  onChange={(e) =>
                    handleWeekdayAssigneeChange(day.value, e.target.value)
                  }
                  className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-indigo-400"
                >
                  <option value="">Выберите участника</option>
                  {sortedMembers.map((member) => (
                    <option key={member.userId} value={member.userId}>
                      {member.login}
                    </option>
                  ))}
                </select>
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}
