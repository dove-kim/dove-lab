/**
 * 확인 다이얼로그 내용.
 *
 * @param title       제목
 * @param description 본문 설명(선택)
 * @param confirmText 확인 버튼 문구(기본 "확인")
 * @param cancelText  취소 버튼 문구(기본 "취소")
 * @param danger      되돌릴 수 없는 작업이면 true — 확인 버튼이 빨간색
 */
export interface ConfirmOptions {
  title: string;
  description?: string;
  confirmText?: string;
  cancelText?: string;
  danger?: boolean;
}
