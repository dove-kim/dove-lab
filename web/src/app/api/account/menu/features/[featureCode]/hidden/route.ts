import { NextRequest, NextResponse } from "next/server";
import { backendFetch, unauthorized } from "@/services/backend";

export async function PATCH(
  req: NextRequest,
  { params }: { params: Promise<{ featureCode: string }> }
) {
  const { featureCode } = await params;
  const body = await req.json();
  const res = await backendFetch(`/account/menu/features/${featureCode}/hidden`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res) return unauthorized();
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    return NextResponse.json(data, { status: res.status });
  }
  return new NextResponse(null, { status: 204 });
}
