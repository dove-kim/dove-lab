import { ImageResponse } from "next/og";

export const size = { width: 32, height: 32 };
export const contentType = "image/png";

export default function Icon() {
  return new ImageResponse(
    (
      <div
        style={{
          width: 32,
          height: 32,
          background: "#0f172a",
          borderRadius: 6,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <svg width="24" height="24" viewBox="0 0 24 24">
          {/* Candle 1 — bearish (red), left */}
          <line x1="5" y1="3" x2="5" y2="5" stroke="#f87171" strokeWidth="1.2" strokeLinecap="round" />
          <rect x="3" y="5" width="4" height="7" rx="0.5" fill="#f87171" />
          <line x1="5" y1="12" x2="5" y2="15" stroke="#f87171" strokeWidth="1.2" strokeLinecap="round" />

          {/* Candle 2 — bullish (green), center */}
          <line x1="12" y1="2" x2="12" y2="5" stroke="#34d399" strokeWidth="1.2" strokeLinecap="round" />
          <rect x="10" y="5" width="4" height="9" rx="0.5" fill="#34d399" />
          <line x1="12" y1="14" x2="12" y2="17" stroke="#34d399" strokeWidth="1.2" strokeLinecap="round" />

          {/* Candle 3 — bullish (indigo), right */}
          <line x1="19" y1="5" x2="19" y2="8" stroke="#818cf8" strokeWidth="1.2" strokeLinecap="round" />
          <rect x="17" y="8" width="4" height="8" rx="0.5" fill="#818cf8" />
          <line x1="19" y1="16" x2="19" y2="19" stroke="#818cf8" strokeWidth="1.2" strokeLinecap="round" />
        </svg>
      </div>
    ),
    { ...size }
  );
}
