"use client";

import {
  Select as UiSelect,
  SelectContent,
  SelectGroup,
  SelectItem as UiSelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/utils/cn";

export type SelectItem =
  | { value: string; label: string; disabled?: boolean }
  | { group: string; options: { value: string; label: string; disabled?: boolean }[] };

interface Props {
  value: string | null | undefined;
  items: SelectItem[];
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string; // 트리거 너비 (예: "w-full", "w-44")
}

// Radix Select 는 빈 문자열 값을 허용하지 않아, "없음" 류 항목을 이 토큰으로 바꿔 쓴다.
const EMPTY = "__empty__";

const encode = (value: string) => (value === "" ? EMPTY : value);
const decode = (value: string) => (value === EMPTY ? "" : value);

export default function Select({ value, items, onChange, placeholder = "선택", disabled, className }: Props) {
  // 빈 문자열 항목이 실제로 있을 때만 토큰으로 바꾼다. 없으면 빈 값 = 미선택(플레이스홀더).
  const hasEmptyItem = items.some((item) =>
    "group" in item ? item.options.some((o) => o.value === "") : item.value === "",
  );

  return (
    <UiSelect
      value={value == null || (value === "" && !hasEmptyItem) ? "" : encode(value)}
      onValueChange={(v) => onChange(decode(v))}
      disabled={disabled}
    >
      <SelectTrigger
        className={cn(
          "h-auto w-full bg-popover px-3 py-2 text-base hover:bg-popover-hover sm:text-sm dark:bg-popover dark:hover:bg-popover-hover",
          className,
        )}
      >
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent className="max-h-70 border border-border p-1 shadow-2xl">
        {items.map((item, i) =>
          "group" in item ? (
            <SelectGroup key={`g${i}`} className="p-0">
              <SelectLabel className="px-3 pt-2 pb-1">{item.group}</SelectLabel>
              {item.options.map((opt) => (
                <UiSelectItem
                  key={opt.value}
                  value={encode(opt.value)}
                  disabled={opt.disabled}
                  className="py-2 pl-5"
                >
                  {opt.label}
                </UiSelectItem>
              ))}
            </SelectGroup>
          ) : (
            <UiSelectItem
              key={item.value}
              value={encode(item.value)}
              disabled={item.disabled}
              className="py-2 pl-3"
            >
              {item.label}
            </UiSelectItem>
          ),
        )}
      </SelectContent>
    </UiSelect>
  );
}
