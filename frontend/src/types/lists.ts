export type TodoListShortResponse = {
  id: string;
  title: string;
  membersCount: number;
  isOwner: boolean;
};

export type CreateTodoListRequest = {
  title: string;
};

export type TodoListMemberResponse = {
  userId: string;
  login: string;
};

export type TodoListDetailsResponse = {
  id: string;
  title: string;
  ownerUserId: string;
  isOwner: boolean;
  members: TodoListMemberResponse[];
};

export type CreateInviteResponse = {
  token: string;
  expiresAt: string;
};

export type AcceptInviteRequest = {
  token: string;
};
