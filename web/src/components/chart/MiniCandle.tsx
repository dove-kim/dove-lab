interface Props {
  open: number;
  high: number;
  low: number;
  close: number;
  width?: number;
  height?: number;
}

const UP = "#f87171";   // 양봉 (종가 ≥ 시가)
const DOWN = "#60a5fa"; // 음봉 (종가 < 시가)

/**
 * 하루치 시세를 축약한 미니 캔들 — 심지(고·저) + 몸통(시·종), 한국식 색상(양봉 빨강/음봉 파랑).
 */
export default function MiniCandle({ open, high, low, close, width = 14, height = 34 }: Props) {
  const range = high - low;
  const y = (p: number) => (range <= 0 ? height / 2 : ((high - p) / range) * height);

  const color = close >= open ? UP : DOWN;
  const cx = width / 2;
  const bodyTop = y(Math.max(open, close));
  const bodyBottom = y(Math.min(open, close));
  const bodyH = Math.max(1, bodyBottom - bodyTop);
  const bodyW = Math.max(3, width - 6);

  return (
    <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`} className="flex-shrink-0">
      <line x1={cx} x2={cx} y1={y(high)} y2={y(low)} stroke={color} strokeWidth={1} />
      <rect x={cx - bodyW / 2} y={bodyTop} width={bodyW} height={bodyH} fill={color} />
    </svg>
  );
}
