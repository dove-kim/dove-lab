import { perf } from "@/utils/perf";

/**
 * 클라이언트 측 fetch wrapper.
 *
 * <p>401 발생 시 한 번에 한해 `/api/auth/refresh` 로 access token 을 갱신하고
 * 원 요청을 재시도한다. 동시에 여러 요청이 401 을 받아도 single-flight 로
 * refresh 는 한 번만 호출된다.
 */
export async function clientFetch(
  input: RequestInfo | URL,
  init?: RequestInit
): Promise<Response | null> {
  const method = init?.method ?? "GET";
  const url    = typeof input === "string" ? input : input instanceof URL ? input.pathname : input.url;
  const label  = `${method} ${url}`;

  let res = await perf.measure("API", label, () => fetch(input, init));

  if (res.status === 401) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      res = await perf.measure("API", `${label} (retry)`, () => fetch(input, init));
      if (res.status !== 401) return res;
    }
    if (typeof window !== "undefined") {
      window.location.replace("/login");
    }
    return null;
  }
  return res;
}

let refreshPromise: Promise<boolean> | null = null;

/**
 * single-flight refresh. 진행 중인 refresh 가 있으면 그 결과를 공유한다.
 */
async function tryRefresh(): Promise<boolean> {
  if (refreshPromise) return refreshPromise;
  refreshPromise = fetch("/api/auth/refresh", { method: "POST", cache: "no-store" })
    .then((r) => r.ok)
    .catch(() => false)
    .finally(() => {
      refreshPromise = null;
    });
  return refreshPromise;
}
