"use client";

import { InputHTMLAttributes } from "react";

/**
 * 천단위 콤마를 자동 표시하는 숫자 입력 — 숫자(소수점 옵션) 외 입력은 차단한다.
 * 상태에는 콤마 없는 원시 문자열을 저장하고, 화면에만 콤마를 붙인다.
 *
 * @param value    원시 값(콤마 없음, 예: "1234.5")
 * @param onChange 원시 값 콜백
 * @param decimal  소수점 허용 여부(수량·단가·환율 등)
 */
interface Props extends Omit<InputHTMLAttributes<HTMLInputElement>, "value" | "onChange"> {
  value: string;
  onChange: (raw: string) => void;
  decimal?: boolean;
}

function group(raw: string, decimal?: boolean): string {
  if (!raw) return "";
  const dot = raw.indexOf(".");
  const intPart = dot === -1 ? raw : raw.slice(0, dot);
  const grouped = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  if (decimal && dot !== -1) return grouped + "." + raw.slice(dot + 1);
  return grouped;
}

export default function CommaInput({ value, onChange, decimal, ...rest }: Props) {
  function handle(e: React.ChangeEvent<HTMLInputElement>) {
    let raw = e.target.value.replace(/,/g, "");
    if (decimal) {
      raw = raw.replace(/[^\d.]/g, "");
      const first = raw.indexOf(".");
      if (first !== -1) {
        raw = raw.slice(0, first + 1) + raw.slice(first + 1).replace(/\./g, "");
      }
    } else {
      raw = raw.replace(/\D/g, "");
    }
    onChange(raw);
  }

  return (
    <input
      {...rest}
      value={group(value, decimal)}
      onChange={handle}
      inputMode={decimal ? "decimal" : "numeric"}
    />
  );
}
