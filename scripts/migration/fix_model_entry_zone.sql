-- ML_MODEL(ID=2) 진입존 교정 — 등록 시점에 meta 의 entry_zone 이 유실된 것을 바로잡는다.
--
-- prod 등록본에는 유동성 조건(volume>=100000)이 빠져 있어 무거래 종목이 채점되고 있다.
-- 아티팩트(breakout_selector v1.0.3, feature_hash ca568079be43d1d8)의 meta.json 원문으로 교체한다.
-- 모델 ID·아티팩트는 그대로라 검색 필터(modelId=2)와 기존 점수는 영향받지 않는다.
-- 다음 채점 배치가 새 진입존으로 동작한다(sweep 이 매 실행 meta 를 다시 파싱).
--
-- 실행: mysql -h <host> -P <port> -u <writer> -p DOVE_LAB < fix_model_entry_zone.sql

SELECT 'before' AS phase, JSON_EXTRACT(META_JSON, '$.entry_zone.conditions') AS conditions
  FROM ML_MODEL WHERE ID = 2;

UPDATE ML_MODEL SET META_JSON = '{"name":"breakout_selector","version":"1.0.3","output_type":"probability","features":["rsi_14","macd_histogram","macd_line","adx_14","plus_di_14","minus_di_14","bb_width_20","bb_percent_b_20","volume_ma20_ratio","atr","cci","mfi","williams_r","high_52w_ratio","high_20d_ratio","ret_5d","ret_10d","body_ratio","upper_wick_ratio","volatility_20d","volatility_5d","gap_open","stochastic_k_14_7","close_pos"],"feature_count":24,"feature_hash":"ca568079be43d1d8","trained_at_data_range":["2010-01-04","2026-06-30"],"base_rate":0.2007113158619091,"best_iter":14,"trained_exchange":"KOSPI,KOSDAQ","trained_price_type":"ADJUSTED","entry_zone":{"desc":"신고가 근접(52주 고점비 0.70↑) + 스퀴즈(변동성20D 0.020↓) + 유동성(거래량 10만↑). 서빙 채점 후보존. 동시돌파 전환·시장 레짐은 검색 필터에서 결합.","conditions":["high_52w_ratio>=0.70","volatility_20d<=0.020","volume>=100000"]},"exit_policy":{"desc":"-10% 예약 스탑(시장가) + 종가<SMA120 이탈 전량. 매수=익일 시가.","stop":-0.1,"trend_exit":"SMA120"},"label":"실제 트레이드 수익(연속값): 익일시가 매수 → -10%스탑/SMA120이탈 청산.","score_meaning":"예측 트레이드수익의 후보 백분위(0~1, ×100=점). 90점=상위10%. 슬롯을 점수 상위부터 배분.","serving_note":"진입존은 coarse(위 조건)라 서빙 후보탐색(동시돌파 전환·유동성 OR)은 별도 로직. 점수는 후보 랭킹용. 레짐(시장 MA200)은 모델 밖 필터.","expected_oos":{"per_trade":"+8.1% (룰 신고가+스퀴즈 +5.1%, 워크포워드 OOS 2016~2026)","note":"CAGR·MDD·슬롯/사이징은 트레이딩 레이어(data/backtest/strategy_final.html). 승률은 ~30% 천장(팻테일)."}}' WHERE ID = 2;

SELECT 'after' AS phase, JSON_EXTRACT(META_JSON, '$.entry_zone.conditions') AS conditions
  FROM ML_MODEL WHERE ID = 2;
