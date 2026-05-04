import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "주식 딸깍이",
  description: "주식 딸깍이",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}
