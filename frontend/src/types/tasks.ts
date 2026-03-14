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
