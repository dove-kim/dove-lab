import type { Metadata } from "next";
import "@/styles/globals.css";
import ScrollWatcher from "@/components/ScrollWatcher";

export const metadata: Metadata = {
  title: "Dove Lab",
  description: "Dove Lab",
  icons: {
    icon: "data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🕊️</text></svg>",
  },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>
        <ScrollWatcher />
        {children}
      </body>
    </html>
  );
}
