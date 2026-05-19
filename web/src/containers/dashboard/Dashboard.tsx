function MarketStatusCard() {
  return (
    <div className="bg-white/5 border border-white/10 rounded-xl p-5">
      <h3 className="text-slate-300 text-sm font-medium mb-4 flex items-center gap-2">
        <svg className="w-4 h-4 text-indigo-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
        </svg>
        시장 현황
      </h3>
      <div className="flex flex-col items-center justify-center py-8 gap-2">
        <div className="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center">
          <svg className="w-4 h-4 text-slate-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
          </svg>
        </div>
        <p className="text-slate-500 text-xs">시장 데이터 준비 중</p>
      </div>
    </div>
  );
}

function PortfolioCard({ tall }: { tall?: boolean }) {
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

function WatchlistCard() {
  return (
    <div className="bg-white/5 border border-white/10 rounded-xl p-5">
      <h3 className="text-slate-300 text-sm font-medium mb-4 flex items-center gap-2">
        <svg className="w-4 h-4 text-indigo-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M12 20h9" />
          <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
        </svg>
        관심 종목
      </h3>
      <div className="flex flex-col items-center justify-center py-8 gap-2">
        <div className="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center">
          <svg className="w-4 h-4 text-slate-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 20h9" />
            <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
          </svg>
        </div>
        <p className="text-slate-500 text-xs">관심 종목 기능 준비 중</p>
      </div>
    </div>
  );
}

function RecentActivityCard() {
  return (
    <div className="bg-white/5 border border-white/10 rounded-xl p-5">
      <h3 className="text-slate-300 text-sm font-medium mb-4 flex items-center gap-2">
        <svg className="w-4 h-4 text-indigo-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10" />
          <polyline points="12 6 12 12 16 14" />
        </svg>
        최근 활동
      </h3>
      <div className="flex flex-col items-center justify-center py-8 gap-2">
        <div className="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center">
          <svg className="w-4 h-4 text-slate-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 16 14" />
          </svg>
        </div>
        <p className="text-slate-500 text-xs">활동 내역 준비 중</p>
      </div>
    </div>
  );
}

export default function Dashboard() {
  return (
    <div className="flex-1 overflow-y-auto p-6">
      <h2 className="text-white text-lg font-semibold mb-6">대시보드</h2>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div className="md:col-span-2">
          <MarketStatusCard />
        </div>
        <div className="lg:row-span-2">
          <PortfolioCard tall />
        </div>
        <WatchlistCard />
        <div className="md:col-span-2 lg:col-span-1">
          <RecentActivityCard />
        </div>
      </div>
    </div>
  );
}

