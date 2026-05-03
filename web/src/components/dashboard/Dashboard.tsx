import MarketStatusCard from "./MarketStatusCard";
import PortfolioCard from "./PortfolioCard";
import WatchlistCard from "./WatchlistCard";
import RecentActivityCard from "./RecentActivityCard";

export default function Dashboard() {
  return (
    <div className="flex-1 overflow-y-auto p-6">
      <h2 className="text-white text-lg font-semibold mb-6">대시보드</h2>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {/* 시장 현황: 태블릿↑ 2열, 데스크탑 2열 */}
        <div className="md:col-span-2">
          <MarketStatusCard />
        </div>
        {/* 포트폴리오: 데스크탑에서 2행 차지 */}
        <div className="lg:row-span-2">
          <PortfolioCard tall />
        </div>
        {/* 관심 종목 */}
        <WatchlistCard />
        {/* 최근 활동: 태블릿↑ 2열 */}
        <div className="md:col-span-2 lg:col-span-1">
          <RecentActivityCard />
        </div>
      </div>
    </div>
  );
}
