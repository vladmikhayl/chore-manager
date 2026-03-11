import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { getAccessToken } from "../../utils/authStorage";

type PrivateRouteProps = {
  children: ReactNode;
};

export function PrivateRoute({ children }: PrivateRouteProps) {
  const accessToken = getAccessToken();

  if (!accessToken) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}
