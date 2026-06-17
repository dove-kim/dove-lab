import { redirect } from "next/navigation";

// "모든 종목"은 종목 검색(전체 보기)으로 통합됨 — 기존 링크·북마크 호환용 리다이렉트.
export default function AllStocksPage() {
  redirect("/stock-search");
}
