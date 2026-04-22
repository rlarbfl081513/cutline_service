import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import Navigation from '../../../components/feature/Navigation';
import FriendSidebar from '../components/FriendSidebar';
import FriendSubNav from '../components/FriendSubNav';
import {
  getPersonChart,
  AutoStatsPayload,
  ManualStatsPayload,
  IssuePayload,
  PersonValuePayload,
} from '../../../lib/api/people';

// 커스텀 스크롤바 스타일
const customScrollbarStyle = `
  .custom-scrollbar::-webkit-scrollbar {
    width: 8px;
  }
  .custom-scrollbar::-webkit-scrollbar-track {
    background: rgba(55, 65, 81, 0.3);
    border-radius: 4px;
  }
  .custom-scrollbar::-webkit-scrollbar-thumb {
    background: rgba(31, 255, 169, 0.4);
    border-radius: 4px;
  }
  .custom-scrollbar::-webkit-scrollbar-thumb:hover {
    background: rgba(31, 255, 169, 0.6);
  }
`;

const ChartDetail: React.FC = () => {
  const { id } = useParams();
  const [hoveredPoint, setHoveredPoint] = useState<{
    x: number;
    y: number;
    value: number;
    date: string;
  } | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [autoStats, setAutoStats] = useState<AutoStatsPayload | null>(null);
  const [manualStats, setManualStats] = useState<ManualStatsPayload | null>(null);
  const [issues, setIssues] = useState<IssuePayload[]>([]);
  const [personValues, setPersonValues] = useState<PersonValuePayload[]>([]);

  // API 데이터 가져오기
  useEffect(() => {
    let mounted = true;
    async function fetchChartData() {
      if (!id) return;
      try {
        setLoading(true);
        setError(null);
        const res = await getPersonChart(id);
        if (mounted) {
          setAutoStats(res.data.latestAuto);
          setManualStats(res.data.latestManual);
          setIssues(res.data.issues);
          setPersonValues(res.data.personValuesLast12);
        }
      } catch (e: any) {
        if (mounted) setError(e?.message || '차트 데이터를 불러오지 못했습니다.');
      } finally {
        if (mounted) setLoading(false);
      }
    }
    fetchChartData();
    return () => {
      mounted = false;
    };
  }, [id]);

  // API 데이터를 차트 형식으로 변환
  const chartData = personValues
    .sort((a, b) => a.year - b.year || a.month - b.month)
    .map((pv) => ({
      value: pv.value,
      date: `${pv.year}.${String(pv.month).padStart(2, '0')}`,
    }));

  // 가장 최근 월의 feedback 가져오기
  const latestFeedback = personValues.length > 0 
    ? personValues
        .sort((a, b) => b.year - a.year || b.month - a.month)[0]?.feedback
    : null;

  // Issue content에서 타입 추출 및 포맷팅
  const formatIssueContent = (content: string) => {
    const types = ['OTHER', 'RECONCILIATION', 'CONFLICT', 'TOUCHING'];
    const foundType = types.find(type => content.startsWith(type));
    
    if (foundType) {
      let remainingContent = content.substring(foundType.length).trim();
      // 앞의 ': ' 제거
      if (remainingContent.startsWith(': ')) {
        remainingContent = remainingContent.substring(2);
      }
      return {
        type: foundType,
        content: remainingContent
      };
    }
    
    // 타입이 없는 경우에도 ': '로 시작하면 제거
    let cleanContent = content;
    if (cleanContent.startsWith(': ')) {
      cleanContent = cleanContent.substring(2);
    }
    
    return {
      type: null,
      content: cleanContent
    };
  };

  // Issue 타입별 색상 매핑
  const getIssueTypeColor = (type: string | null) => {
    switch (type) {
      case 'CONFLICT':
        return {
          bg: 'bg-red-500/20',
          border: 'border-red-500/30',
          text: 'text-red-400',
          dot: 'bg-red-500'
        };
      case 'RECONCILIATION':
        return {
          bg: 'bg-blue-500/20',
          border: 'border-blue-500/30',
          text: 'text-blue-400',
          dot: 'bg-blue-500'
        };
      case 'TOUCHING':
        return {
          bg: 'bg-green-500/20',
          border: 'border-green-500/30',
          text: 'text-green-400',
          dot: 'bg-green-500'
        };
      case 'OTHER':
        return {
          bg: 'bg-yellow-500/20',
          border: 'border-yellow-500/30',
          text: 'text-yellow-400',
          dot: 'bg-yellow-500'
        };
      default:
        return {
          bg: 'bg-gray-500/20',
          border: 'border-gray-500/30',
          text: 'text-gray-400',
          dot: 'bg-gray-500'
        };
    }
  };

  // 숫자 포맷팅 함수 (음수 지원)
  const formatValue = (value: number): string => {
    const absValue = Math.abs(value);
    const sign = value < 0 ? '-' : '';
    
    if (absValue >= 1000000) {
      return sign + (absValue / 1000000).toFixed(1) + 'M';
    } else if (absValue >= 1000) {
      return sign + (absValue / 1000).toFixed(1) + 'K';
    } else {
      return value.toLocaleString();
    }
  };

  // 차트 Y축 라벨 계산
  const maxValue = chartData.length > 0 ? Math.max(...chartData.map(d => d.value)) : 1000;
  const minValue = chartData.length > 0 ? Math.min(...chartData.map(d => d.value)) : 0;
  const valueRange = Math.max(1, maxValue - minValue);
  const yTicks = 6;
  const yLabels = Array.from({ length: yTicks }, (_, i) =>
    Math.round(minValue + ((yTicks - 1 - i) * valueRange) / (yTicks - 1))
  );

  // SVG 영역 내 좌표 변환 함수들 (실제 데이터값 반영)
  const svgWidth = 500;
  const svgHeight = 300;
  const paddingTop = 10;
  const paddingBottom = 20;
  const usableHeight = svgHeight - paddingTop - paddingBottom;
  const getX = (index: number): number => {
    if (chartData.length <= 1) {
      return svgWidth / 2;
    }
    const step = svgWidth / (chartData.length - 1);
    return step * index;
  };
  const getY = (value: number): number => {
    if (valueRange === 0) return svgHeight / 2;
    const normalized = (value - minValue) / valueRange; // 0~1
    return paddingTop + (1 - normalized) * usableHeight;
  };

  const handleMouseMove = (event: React.MouseEvent<SVGElement>) => {
    if (chartData.length === 0) return;
    const rect = event.currentTarget.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;

    // 가장 가까운 데이터 포인트 찾기
    const svgWidth = rect.width - 48; // left padding 제외
    const pointSpacing = svgWidth / (chartData.length - 1);
    const closestIndex = Math.round((x - 48) / pointSpacing);

    if (closestIndex >= 0 && closestIndex < chartData.length) {
      const dataPoint = chartData[closestIndex];
      setHoveredPoint({
        x: x,
        y: y,
        value: dataPoint.value,
        date: dataPoint.date,
      });
    }
  };

  const handleMouseLeave = () => {
    setHoveredPoint(null);
  };

  // 로딩 상태
  if (loading) {
    return (
      <div className="pt-16 min-h-screen bg-[#0A0D0C] text-white">
        <Navigation />
        <div className="flex items-center justify-center min-h-[calc(100vh-80px)] px-6">
          <div className="text-gray-400">차트 데이터를 불러오는 중...</div>
        </div>
      </div>
    );
  }

  // 에러 상태
  if (error) {
    return (
      <div className="pt-16 min-h-screen bg-[#0A0D0C] text-white">
        <Navigation />
        <div className="flex items-center justify-center min-h-[calc(100vh-80px)] px-6">
          <div className="text-center max-w-lg">
            <div className="mb-8">
              <i className="ri-error-warning-line text-6xl text-red-500 mb-4"></i>
              <h1 className="text-3xl font-bold mb-4">데이터를 불러올 수 없습니다</h1>
              <p className="text-gray-400 text-lg mb-8">{error}</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="pt-16 min-h-screen bg-[#0A0D0C] text-white">
      <style>{customScrollbarStyle}</style>
      <Navigation />

      <div className="flex">
        <FriendSidebar friendId={id} />

        <div className="flex-1 ml-80">
          <FriendSubNav friendId={id || ''} currentPage="chart" />

          <div className="p-8">
            <div className="max-w-7xl mx-auto">
              <div className="grid grid-cols-12 gap-8">
                {/* 왼쪽 메인 컨텐츠 */}
                <div className="col-span-9 space-y-6">
                  {/* 친구 관계 한줄 요약 */}
                  <div className="bg-[#1FFFA9]/10 rounded-xl p-6 border border-[#1FFFA9]/30">
                    <h3 className="text-lg font-bold text-[#1FFFA9] mb-3">
                      친구 관계 한줄 요약
                    </h3>
                    <p className="text-base text-white">
                      {latestFeedback || '아직 분석된 데이터가 없습니다. 대화 내역을 업로드해주세요.'}
                    </p>
                  </div>

                  {/* 친구 시가총액 그래프 */}
                  <div className="bg-gradient-to-br from-gray-900/90 to-gray-800/90 rounded-xl p-6 relative border border-gray-700/50 backdrop-blur-sm">
                    <div className="mb-4">
                      <div className="text-white text-xl font-bold">
                        관계 지수
                      </div>
                      <div className="text-3xl font-bold text-white mb-3">
                        {chartData.length > 0 ? chartData[chartData.length - 1].value.toLocaleString() : '0'}
                      </div>
                      <div className="text-gray-400 text-lg">
                        {chartData.length > 0 ? `${chartData[chartData.length - 1].date}` : '데이터 없음'}
                      </div>
                    </div>

                    {/* 그래프 영역 */}
                    <div className="h-80 relative mb-8">
                      {/* Y축 라벨 */}
                      <div className="absolute left-0 top-0 h-full flex flex-col justify-between text-gray-400 text-sm py-4">
                        {yLabels.map((label, index) => (
                          <span key={index}>₩{formatValue(label)}</span>
                        ))}
                      </div>

                      {/* X축 라벨 */}
                      <div className="absolute left-12 right-0 bottom-0 flex justify-between text-gray-400 text-sm px-2">
                        {chartData.map((data, index) => (
                          <span key={index}>{data.date}</span>
                        ))}
                      </div>

                      {/* 그리드 라인 */}
                      <div className="absolute left-12 right-0 top-0 h-full">
                        <div className="h-full flex flex-col justify-between py-4">
                          <div className="w-full h-px bg-gray-600/30"></div>
                          <div className="w-full h-px bg-gray-600/30"></div>
                          <div className="w-full h-px bg-gray-600/30"></div>
                          <div className="w-full h-px bg-gray-600/30"></div>
                          <div className="w-full h-px bg-gray-600/30"></div>
                          <div className="w-full h-px bg-gray-600/30"></div>
                        </div>
                      </div>

                      {/* 메인 그래프 라인 (실제 데이터값 기반) */}
                      <div className="absolute left-12 right-0 top-0 h-full py-4">
                        <svg
                          className="w-full h-full cursor-pointer"
                          viewBox="0 0 500 300"
                          preserveAspectRatio="none"
                          onMouseMove={handleMouseMove}
                          onMouseLeave={handleMouseLeave}
                        >
                          {/* 그라데이션 정의 */}
                          <defs>
                            <linearGradient
                              id="lineGradient"
                              x1="0%"
                              y1="0%"
                              x2="100%"
                              y2="0%"
                            >
                              <stop offset="0%" stopColor="#1FFFA9" />
                              <stop offset="100%" stopColor="#0A74FF" />
                            </linearGradient>
                            <linearGradient
                              id="areaGradient"
                              x1="0%"
                              y1="0%"
                              x2="0%"
                              y2="100%"
                            >
                              <stop
                                offset="0%"
                                stopColor="#1FFFA9"
                                stopOpacity="0.2"
                              />
                              <stop
                                offset="100%"
                                stopColor="#0A74FF"
                                stopOpacity="0.05"
                              />
                            </linearGradient>
                          </defs>

                          {/* 영역 채우기 (실제 포인트 기반) */}
                          {chartData.length > 0 && (
                            <path
                              d={`M ${getX(0)} ${getY(chartData[0].value)} ` +
                                chartData
                                  .slice(1)
                                  .map((d, i) => `L ${getX(i + 1)} ${getY(d.value)}`)
                                  .join(' ') +
                                ` L ${getX(chartData.length - 1)} ${svgHeight - paddingBottom}` +
                                ` L ${getX(0)} ${svgHeight - paddingBottom} Z`}
                              fill="url(#areaGradient)"
                            />
                          )}

                          {/* 메인 라인 (실제 포인트 연결) */}
                          {chartData.length > 0 && (
                            <path
                              d={`M ${getX(0)} ${getY(chartData[0].value)} ` +
                                chartData
                                  .slice(1)
                                  .map((d, i) => `L ${getX(i + 1)} ${getY(d.value)}`)
                                  .join(' ')}
                              stroke="url(#lineGradient)"
                              strokeWidth="3"
                              fill="none"
                            />
                          )}

                          {/* 데이터 포인트들 (실제 값 위치) */}
                          {chartData.map((d, index) => (
                            <circle
                              key={index}
                              cx={getX(index)}
                              cy={getY(d.value)}
                              r="6"
                              fill="#1FFFA9"
                              className="hover:r-8 transition-all"
                            />
                          ))}
                        </svg>
                      </div>
                    </div>

                    {/* 호버 툴팁 */}
                    {hoveredPoint && (
                      <div
                        className="absolute bg-black/90 text-white px-4 py-3 rounded-lg text-sm pointer-events-none z-10 min-w-48"
                        style={{
                          left: hoveredPoint.x,
                          top: hoveredPoint.y - 60,
                          transform: 'translateX(-50%)',
                        }}
                      >
                        <div className="space-y-1">
                          <div className="font-bold text-center border-b border-gray-600 pb-1">
                            {hoveredPoint.date}
                          </div>
                          <div className="flex justify-between">
                            <span>관계 가치:</span>
                            <span className="font-semibold text-[#1FFFA9]">
                              ₩{formatValue(hoveredPoint.value)}
                            </span>
                          </div>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* 통계 정보 */}
                  <div className="grid grid-cols-4 gap-6">
                    <div className="bg-gray-900/50 rounded-xl p-6 border border-gray-800 text-center">
                      <div className="text-[#1FFFA9] text-lg font-bold mb-1">
                        응답시간
                      </div>
                      <div className="text-2xl font-bold text-white">
                        {manualStats?.responseAverage?.toFixed(1) || 0}분
                      </div>
                    </div>
                    <div className="bg-gray-900/50 rounded-xl p-6 border border-gray-800 text-center">
                      <div className="text-[#0A74FF] text-lg font-bold mb-1">
                        월간 메시지 수
                      </div>
                      <div className="text-2xl font-bold text-white">
                        {manualStats?.monthVolume?.toLocaleString() || 0}개
                      </div>
                    </div>
                    <div className="bg-gray-900/50 rounded-xl p-6 border border-gray-800 text-center">
                      <div className="text-[#1FFFA9] text-lg font-bold mb-1">
                        도움받은 횟수
                      </div>
                      <div className="text-2xl font-bold text-white">
                        {autoStats?.getHelp || 0}회
                      </div>
                    </div>
                    <div className="bg-gray-900/50 rounded-xl p-6 border border-gray-800 text-center">
                      <div className="text-[#E43F42] text-lg font-bold mb-1">
                        공격 횟수
                      </div>
                      <div className="text-2xl font-bold text-white">
                        {autoStats?.attack || 0}회
                      </div>
                    </div>
                  </div>
                </div>

                {/* 오른쪽 이슈 바 */}
                <div className="col-span-3">
                  <div className="bg-gray-900/50 rounded-xl p-6 border border-gray-800 h-[600px] flex flex-col">
                    <h3 className="text-xl font-bold text-white mb-6">Issue</h3>

                    {/* 이슈 타임라인 */}
                    <div className="relative flex-1 overflow-hidden">
                      {/* 세로 선 */}
                      <div className="absolute left-3 top-0 bottom-0 w-0.5 bg-[#1FFFA9]"></div>

                      {/* 스크롤 가능한 영역 */}
                      <div 
                        className="h-full overflow-y-auto pr-3 custom-scrollbar"
                        style={{
                          scrollbarWidth: 'thin',
                          scrollbarColor: '#1FFFA940 #37415130'
                        }}
                      >
                        <div className="space-y-6 pb-6">
                          {issues.length > 0 ? (
                            issues
                              .sort((a, b) => {
                                // 년도 기준으로 내림차순 정렬 (최근년도가 위로)
                                if (a.year !== b.year) {
                                  return b.year - a.year;
                                }
                                // 같은 년도면 월 기준으로 내림차순 정렬 (최근월이 위로)
                                return b.month - a.month;
                              })
                              .map((issue, index) => {
                              const formattedIssue = formatIssueContent(issue.content);
                              const colors = getIssueTypeColor(formattedIssue.type);
                              return (
                                <div
                                  key={issue.id}
                                  className="relative flex items-start"
                                >
                                  {/* 타임라인 점 */}
                                  <div className={`w-6 h-6 rounded-full border-2 flex-shrink-0 ${colors.dot} border-white/20`}></div>

                                  {/* 이슈 정보 */}
                                  <div className="ml-4 flex-1">
                                    <div className={`${colors.bg} ${colors.border} rounded-lg p-3 border`}>
                                      <div className="text-xs text-gray-400 mb-1">
                                        {issue.year}.{issue.month.toString().padStart(2, '0')}
                                      </div>
                                      <div className="text-sm text-white font-medium">
                                        {formattedIssue.content}
                                      </div>
                                    </div>
                                  </div>
                                </div>
                              );
                            })
                          ) : (
                            <div className="text-center text-gray-400 py-8">
                              <i className="ri-inbox-line text-4xl mb-4"></i>
                              <p>등록된 이슈가 없습니다.</p>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ChartDetail;
