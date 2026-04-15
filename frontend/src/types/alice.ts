export type ConfirmAliceLinkRequest = {
  redirectUri: string;
  state?: string;
};

export type ConfirmAliceLinkResponse = {
  redirectUrl: string;
};
