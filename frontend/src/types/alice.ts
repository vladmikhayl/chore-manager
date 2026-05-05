export type ConfirmAliceLinkRequest = {
  redirectUri: string;
  clientId: string;
  state?: string;
};

export type ConfirmAliceLinkResponse = {
  redirectUrl: string;
};
