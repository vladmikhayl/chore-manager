import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { getAccessToken } from "../../utils/authStorage";

type PublicRouteProps = {
  children: ReactNode;
};

export function PublicRoute({ children }: PublicRouteProps) {
  const accessToken = getAccessToken();

  if (accessToken) {
    return <Navigate to="/tasks" replace />;
  }

  return <>{children}</>;
}
