/**
 * 부여 대상 커스텀 지표 카탈로그 항목.
 *
 * @param id    지표 식별자
 * @param name  지표 이름
 * @param shape 지표 모양(형태) 라벨
 */
export interface CustomMetric {
  id: number;
  name: string;
  shape: string;
}
