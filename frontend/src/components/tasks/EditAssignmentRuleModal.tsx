import { useEffect, useState } from "react";
import type { TodoListMemberResponse } from "../../types/lists";
import type {
  AssignmentType,
  TaskResponse,
  UpdateAssignmentRuleRequest,
} from "../../types/tasks";
import { PrimaryButton } from "../shared/PrimaryButton";
import { AssignmentRuleEditor } from "./AssignmentRuleEditor";

type EditAssignmentRuleModalProps = {
  task: TaskResponse;
  members: TodoListMemberResponse[];
  isLoading: boolean;
  errorMessage: string | null;
  onClose: () => void;
  onSubmit: (request: UpdateAssignmentRuleRequest) => Promise<void>;
};

function buildInitialState(task: TaskResponse) {
  return {
    assignmentType: task.assignmentType as AssignmentType,
    fixedUserId: task.fixedUserId ?? "",
    roundRobinUserIds: task.roundRobinUsers?.map((user) => user.userId) ?? [],
    weekdayAssignees: Object.fromEntries(
      Object.entries(task.weekdayAssignees ?? {}).map(([day, userId]) => [
        Number(day),
        userId,
      ]),
    ) as Record<number, string>,
  };
}

export function EditAssignmentRuleModal({
  task,
  members,
  isLoading,
  errorMessage,
  onClose,
  onSubmit,
}: EditAssignmentRuleModalProps) {
  const [form, setForm] = useState(() => buildInitialState(task));
  const [validationError, setValidationError] = useState<string | null>(null);

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  function validate(): string | null {
    if (form.assignmentType === "FixedUser" && !form.fixedUserId) {
      return "Выберите исполнителя.";
    }

    if (
      form.assignmentType === "RoundRobin" &&
      form.roundRobinUserIds.length === 0
    ) {
      return "Выберите хотя бы одного участника для назначения по кругу.";
    }

    if (form.assignmentType === "ByWeekday") {
      for (let day = 0; day <= 6; day += 1) {
        if (!form.weekdayAssignees[day]) {
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

    const request: UpdateAssignmentRuleRequest = {
      assignmentType: form.assignmentType,
      fixedUserId:
        form.assignmentType === "FixedUser" ? form.fixedUserId : null,
      roundRobinUserIds:
        form.assignmentType === "RoundRobin" ? form.roundRobinUserIds : null,
      weekdayAssignees:
        form.assignmentType === "ByWeekday" ? form.weekdayAssignees : null,
    };

    await onSubmit(request);
  }

  const visibleError = validationError ?? errorMessage;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 px-4 py-6">
      <div className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-3xl bg-white shadow-2xl">
        <div className="flex items-start justify-between gap-4 border-b border-slate-200 px-6 py-5">
          <div>
            <h2 className="text-2xl font-bold text-slate-900">
              Редактировать правило
            </h2>
            <p className="mt-1 text-sm text-slate-600">Задача: {task.title}</p>
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
          <AssignmentRuleEditor
            members={members}
            value={form}
            onChange={setForm}
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
              {isLoading ? "Сохраняем..." : "Сохранить правило"}
            </PrimaryButton>
          </div>
        </form>
      </div>
    </div>
  );
}
