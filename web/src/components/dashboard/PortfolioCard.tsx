export default function PortfolioCard({ tall }: { tall?: boolean }) {
  return (
    <div className={`bg-white/5 border border-white/10 rounded-xl p-5 ${tall ? "h-full" : ""}`}>
      <h3 className="text-slate-300 text-sm font-medium mb-4 flex items-center gap-2">
        <svg className="w-4 h-4 text-indigo-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <rect x="2" y="7" width="20" height="14" rx="2" />
          <path d="M16 7V5a2 2 0 0 0-4 0v2" />
          <path d="M12 12v4" />
          <path d="M10 14h4" />
        </svg>
        포트폴리오
      </h3>
      <div className="flex flex-col items-center justify-center py-8 gap-2">
        <div className="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center">
          <svg className="w-4 h-4 text-slate-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <rect x="2" y="7" width="20" height="14" rx="2" />
          </svg>
        </div>
        <p className="text-slate-500 text-xs">포트폴리오 기능 준비 중</p>
      </div>
    </div>
  );
}
