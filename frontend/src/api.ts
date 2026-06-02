import { client } from "./api/generated/client.gen";

client.setConfig({
  baseUrl: window.location.origin,
  credentials: "same-origin"
});

type ApiResult<T> =
  | { data: T | undefined; error: undefined; response: Response }
  | { data: undefined; error: unknown; response: Response };

export class ApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

export function loginUrl() {
  return `/login/google?redirect=${encodeURIComponent(location.pathname + location.search + location.hash)}`;
}

export async function logout() {
  const response = await fetch("/logout", { method: "POST" });
  if (!response.ok) {
    throw new ApiError(await response.text(), response.status);
  }
}

export async function unwrap<T>(request: Promise<ApiResult<T>>): Promise<NonNullable<T>> {
  const result = await request;
  if (result.error === undefined) {
    if (result.data === undefined) {
      throw new ApiError("Response did not include data.", result.response.status);
    }
    return result.data as NonNullable<T>;
  }
  throw new ApiError(readErrorMessage(result.error), result.response.status);
}

export function readErrorMessage(error: unknown) {
  if (typeof error === "object" && error !== null && "message" in error) {
    return String(error.message);
  }
  if (typeof error === "string" && error.length > 0) {
    return error;
  }
  return "Request failed.";
}

export function accountRestrictionMessage(status: string) {
  if (status === "pending") {
    return "Your account is pending admin approval.";
  }
  if (status === "deactivated") {
    return "Your account has been deactivated.";
  }
  return "Your account is not approved to use app features.";
}
