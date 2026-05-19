import { perf } from "@/utils/perf";

export async function clientFetch(
  input: RequestInfo | URL,
  init?: RequestInit
): Promise<Response | null> {
  const method = init?.method ?? "GET";
  const url    = typeof input === "string" ? input : input instanceof URL ? input.pathname : input.url;
  const label  = `${method} ${url}`;

  const res = await perf.measure("API", label, () => fetch(input, init));

  if (res.status === 401) {
    window.location.replace("/login");
    return null;
  }
  return res;
}
