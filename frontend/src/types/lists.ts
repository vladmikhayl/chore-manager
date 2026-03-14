export type TodoListShortResponse = {
  id: string;
  title: string;
  membersCount: number;
  isOwner: boolean;
};

export type CreateTodoListRequest = {
  title: string;
};
