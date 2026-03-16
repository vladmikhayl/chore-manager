export type RecurrenceType = "WeeklyByDays" | "EveryNdays";

export type AssignmentType = "FixedUser" | "RoundRobin" | "ByWeekday";

export type CreateTaskRequest = {
  title: string;
  recurrenceType: RecurrenceType;
  intervalDays: number | null;
  weekdays: number[] | null;
  assignmentType: AssignmentType;
  fixedUserId: string | null;
  roundRobinUserIds: string[] | null;
  weekdayAssignees: Record<number, string> | null;
};

export type TaskResponse = {
  id: string;
  startDate: string;
  listId: string;
  listTitle: string;
  title: string;
  recurrenceType: string;
  intervalDays: number | null;
  weekdaysMask: number | null;
  weekdays: number[] | null;
  assignmentType: string;
  fixedUserId: string | null;
  roundRobinUsers: unknown[] | null;
  weekdayAssignees: Record<string, string> | null;
  completed: boolean;
};

export type TaskListItem = {
  id: string;
  title: string;
  listTitle: string;
  completed: boolean;
};
