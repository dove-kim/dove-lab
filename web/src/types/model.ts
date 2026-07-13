/**
 * 부여 대상 모델 카탈로그 항목.
 *
 * @param id         모델 식별자
 * @param name       모델 이름
 * @param version    모델 버전
 * @param outputType 모델 출력 유형
 */
export interface Model {
  id: number;
  name: string;
  version: string;
  outputType: string;
}
