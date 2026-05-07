export async function clientFetch(
  input: RequestInfo | URL,
  init?: RequestInit
): Promise<Response | null> {
  const res = await fetch(input, init);
  if (res.status === 401) {
    window.location.replace("/login");
    return null;
  }
  return res;
}
