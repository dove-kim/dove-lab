"""모델 채점 스코어러 — dove-lab 서버(Java)가 stdin/stdout JSON으로 호출.

서버가 STOCK_FEATURE_DAILY/STOCK_RANK_DAILY 저장값을 그대로 넘기므로 파생을 하지 않는다.
MODEL_CONTRACT.md predict 계약 준수: meta.features 순서의 이름 있는 DataFrame ->
model.predict(또는 predict_proba) -> calibrator 있으면 isotonic 적용 -> 0~1 점수.
임의 스케일링/클리핑 없음. 결측은 NaN 그대로(LightGBM 네이티브 처리).

입력(stdin JSON):
  {"model_id": 1, "model_path": "/models/entry_model.pkl",
   "rows": [{"ticker": "005930", "trade_date": "2026-06-26",
             "features": {"sma_5": 72500.0, "rank_turnover": 0.75, ...}}, ...]}

출력(stdout JSON):
  {"status": "ok", "scores": [{"ticker": "005930", "trade_date": "2026-06-26", "score": 0.62}, ...]}

에러: 비정상 종료코드 + stderr 메시지.
  1 MODEL_LOAD_FAILED   — 모델/보정기 로드 실패            (서버: system-event, 커서 미전진)
  2 FEATURE_MISMATCH    — meta.features != model.feature_name() (서버: 모델 INACTIVE 강제)
  3 PREDICT_ERROR       — 입력 파싱/예측 실패              (서버: system-event, 커서 미전진)

실행: <python> scripts/score.py        (stdin JSON -> stdout JSON)
자체 점검: <python> scripts/score.py --selftest
"""

import json
import sys

import numpy as np
import pandas as pd

EXIT_OK = 0
EXIT_MODEL_LOAD_FAILED = 1
EXIT_FEATURE_MISMATCH = 2
EXIT_PREDICT_ERROR = 3

CHUNK = 4000  # 행 묶음(일정 메모리 — 대량 행도 청크 단위로 예측)


def fail(code, error_code, message):
    """에러 코드를 stderr에 ASCII로 찍고 비정상 종료한다."""
    sys.stderr.write(f"{error_code}: {message}\n")
    sys.stderr.flush()
    sys.exit(code)


def load_artifact(model_path):
    """.pkl 로드 -> (model, calibrator, meta). 실패 시 MODEL_LOAD_FAILED."""
    try:
        import joblib

        art = joblib.load(model_path)
        return art["model"], art.get("calibrator"), art["meta"]
    except Exception as e:
        fail(EXIT_MODEL_LOAD_FAILED, "MODEL_LOAD_FAILED",
             f"cannot load artifact at {model_path}: {e}")


def model_feature_names(model):
    """추정기의 학습 피처 이름 리스트(LightGBM feature_name()) 또는 None."""
    fn = getattr(model, "feature_name", None)
    if callable(fn):
        try:
            return list(fn())
        except Exception:
            return None
    names = getattr(model, "feature_name_", None) or getattr(model, "feature_names_in_", None)
    return list(names) if names is not None else None


def verify_feature_alignment(model, features):
    """meta.features와 model.feature_name() 정합 검사(.pkl<->meta.json 진위)."""
    trained = model_feature_names(model)
    if trained is not None and list(trained) != list(features):
        fail(EXIT_FEATURE_MISMATCH, "FEATURE_MISMATCH",
             f"expected {features} but model trained on {trained}")


def predict_scores(model, calibrator, features, frame):
    """이름 있는 DataFrame -> [0,1] 점수(보정 적용). 임의 스케일링 없음."""
    x = frame[features]  # meta 순서로 정렬(이름 기준)
    if hasattr(model, "predict_proba"):
        raw = np.asarray(model.predict_proba(x))[:, 1]
    else:
        raw = np.asarray(model.predict(x), dtype=float)
    if calibrator is not None:
        raw = np.asarray(calibrator.predict(raw), dtype=float)
    return raw


def score_rows(model, calibrator, features, rows):
    """입력 행들을 청크 단위로 예측해 [{ticker, trade_date, score}] 생성."""
    out = []
    for start in range(0, len(rows), CHUNK):
        chunk = rows[start:start + CHUNK]
        frame = pd.DataFrame(
            [{name: r.get("features", {}).get(name, np.nan) for name in features} for r in chunk],
            columns=features,
        ).apply(pd.to_numeric, errors="coerce")
        scores = predict_scores(model, calibrator, features, frame)
        for r, s in zip(chunk, scores):
            val = None if s is None or (isinstance(s, float) and np.isnan(s)) else round(float(s), 6)
            out.append({"ticker": r.get("ticker"), "trade_date": r.get("trade_date"), "score": val})
    return out


def run(payload):
    """요청 dict -> 응답 dict. predict 단계 예외는 PREDICT_ERROR."""
    model_path = payload.get("model_path")
    if not model_path:
        fail(EXIT_PREDICT_ERROR, "PREDICT_ERROR", "missing model_path")
    model, calibrator, meta = load_artifact(model_path)
    features = meta.get("features")
    if not features:
        fail(EXIT_PREDICT_ERROR, "PREDICT_ERROR", "meta.features missing in artifact")
    verify_feature_alignment(model, features)
    try:
        scores = score_rows(model, calibrator, features, payload.get("rows", []))
    except Exception as e:
        fail(EXIT_PREDICT_ERROR, "PREDICT_ERROR", f"prediction failed: {e}")
    return {"status": "ok", "scores": scores}


def selftest():
    """소량 더미로 입출력 형식만 검증(추정기 stub — 모델 파일 불필요)."""
    class _Cal:
        def predict(self, a):
            return np.clip(np.asarray(a, dtype=float), 0.0, 1.0)

    class _Model:
        def __init__(self, feats):
            self._f = feats

        def feature_name(self):
            return self._f

        def predict(self, x):
            return x.sum(axis=1).to_numpy() / 10.0

    feats = ["sma_5", "rsi_14", "rank_turnover"]
    model, cal = _Model(feats), _Cal()
    rows = [
        {"ticker": "005930", "trade_date": "2026-06-26",
         "features": {"sma_5": 0.1, "rsi_14": 0.2, "rank_turnover": 0.7}},
        {"ticker": "000660", "trade_date": "2026-06-26",
         "features": {"sma_5": 0.0, "rsi_14": 0.0, "rank_turnover": 0.0}},
    ]
    verify_feature_alignment(model, feats)
    scores = score_rows(model, cal, feats, rows)
    resp = {"status": "ok", "scores": scores}
    assert len(scores) == 2, scores
    assert {s["ticker"] for s in scores} == {"005930", "000660"}, scores
    for s in scores:
        assert s["score"] is None or 0.0 <= s["score"] <= 1.0, s
        assert set(s) == {"ticker", "trade_date", "score"}, s
    sys.stdout.write(json.dumps(resp, ensure_ascii=True) + "\n")
    sys.stdout.write("SELFTEST_OK\n")


def main():
    if "--selftest" in sys.argv[1:]:
        selftest()
        return
    try:
        payload = json.load(sys.stdin)
    except Exception as e:
        fail(EXIT_PREDICT_ERROR, "PREDICT_ERROR", f"invalid input json: {e}")
    resp = run(payload)
    sys.stdout.write(json.dumps(resp, ensure_ascii=True))
    sys.stdout.flush()
    sys.exit(EXIT_OK)


if __name__ == "__main__":
    main()
