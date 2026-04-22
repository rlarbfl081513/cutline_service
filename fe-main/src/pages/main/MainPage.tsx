import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Navigation from '../../components/feature/Navigation';
import Button from '../../components/base/Button';
import { getPeople, CreatePersonPayload, getPersonChart, PersonChartResponse } from '../../lib/api/people';
import { getUser } from '../../lib/api/users';

type UIFriend = {
  id: number;
  name: string;
  status: '관심' | '유지' | '비관심';
  gender?: '남성' | '여성';
  relationship: '친구' | '직장동료' | '애인';
  marketValue: number; // latestValue
  change: string; // latestChangeRate as "+x.x%" or "-x.x%"
  messages: number;
  chartData: number[]; // 12 months normalized 0~100
  latestYear?: number;
  latestMonth?: number;
};

const statusToKo = (s: CreatePersonPayload['status']): UIFriend['status'] => {
  switch (s) {
    case 'INTEREST':
      return '관심';
    case 'MAINTAIN':
      return '유지';
    case 'UNINTEREST':
    default:
      return '비관심';
  }
};

const relationToKo = (
  r: CreatePersonPayload['relation'],
): UIFriend['relationship'] => {
  switch (r) {
    case 'FRIEND':
      return '친구';
    case 'COWORKER':
      return '직장동료';
    case 'LOVER':
      return '애인';
    default:
      return '친구';
  }
};

const genderToKo = (g?: CreatePersonPayload['gender']): UIFriend['gender'] =>
  g === 'MALE' ? '남성' : g === 'FEMALE' ? '여성' : undefined;

export default function Main() {
  const navigate = useNavigate();
  const [friends, setFriends] = useState<UIFriend[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedChartType, setSelectedChartType] = useState<'best' | 'worst'>(
    'best',
  );
  const [hoveredPoint, setHoveredPoint] = useState<{
    x: number;
    y: number;
    date: string;
  } | null>(null);
  const [chartData, setChartData] = useState<{
    highest: PersonChartResponse | null;
    lowest: PersonChartResponse | null;
  }>({ highest: null, lowest: null });
  const [chartLoading, setChartLoading] = useState(false);

  useEffect(() => {
    let mounted = true;
    async function run() {
      try {
        setLoading(true);
        setError(null);
        
        // 먼저 사용자 정보를 확인
        try {
          const userResponse = await getUser();
          const userData = userResponse.data;
          
          // email이나 birth 정보가 없으면 SignInPage로 리디렉션
          if (!userData.email || !userData.birth) {
            console.log('사용자 프로필 정보가 불완전합니다. SignInPage로 이동합니다.');
            navigate('/signinpage');
            return;
          }
        } catch (userError) {
          console.error('사용자 정보 조회 실패:', userError);
          // 사용자 정보를 가져올 수 없으면 로그인 페이지로
          navigate('/signinpage');
          return;
        }
        
        const res = await getPeople();
        const list = res.data || [];

        const ui: UIFriend[] = list.map((item) => {
          const person = item.person;
          const latestValue = item.latestValue ?? 0;
          const latestChangeRate = item.latestChangeRate ?? 0;

          const base = Math.max(
            10,
            Math.min(90, Math.round((Math.abs(latestValue) % 900000) / 10000)),
          );
          const chart = Array.from({ length: 12 }, (_, i) => {
            const noise = ((i * 7) % 15) - 7;
            return Math.max(10, Math.min(100, base + noise));
          });

          return {
            id: person.id,
            name: person.name,
            status: statusToKo(person.status),
            gender: genderToKo(person.gender),
            relationship: relationToKo(person.relation),
            marketValue: latestValue,
            change: `${latestChangeRate >= 0 ? '+' : ''}${latestChangeRate.toFixed(1)}%`,
            messages: 0,
            chartData: chart,
            latestYear: item.latestYear,
            latestMonth: item.latestMonth,
          };
        });

        if (mounted) setFriends(ui);
      } catch (e: any) {
        if (mounted) setError(e?.message || '친구 목록을 불러오지 못했습니다.');
      } finally {
        if (mounted) setLoading(false);
      }
    }
    run();
    return () => {
      mounted = false;
    };
  }, []);

  // 차트 데이터 가져오기
  useEffect(() => {
    if (friends.length === 0) return;

    async function fetchChartData() {
      setChartLoading(true);
      try {
        const sortedByValue = [...friends].sort((a, b) => b.marketValue - a.marketValue);
        const highestFriend = sortedByValue[0];
        const lowestFriend = sortedByValue[sortedByValue.length - 1];

        const [highestChart, lowestChart] = await Promise.all([
          getPersonChart(highestFriend.id),
          getPersonChart(lowestFriend.id)
        ]);

        setChartData({
          highest: highestChart,
          lowest: lowestChart
        });
      } catch (error) {
        console.error('차트 데이터 로딩 실패:', error);
      } finally {
        setChartLoading(false);
      }
    }

    fetchChartData();
  }, [friends]);

  const hasFriends = friends.length > 0;
  const { bestFriends, worstFriends, highestValueFriend, lowestValueFriend } = useMemo(() => {
    const sorted = [...friends].sort((a, b) => a.marketValue - b.marketValue); // asc by value
    const worst = sorted.slice(0, 5);
    const best = sorted.slice(-5).reverse();
    
    // value가 가장 큰 친구와 가장 작은 친구
    const highestValueFriend = friends.length > 0 ? friends.reduce((max, friend) => 
      friend.marketValue > max.marketValue ? friend : max
    ) : null;
    
    const lowestValueFriend = friends.length > 0 ? friends.reduce((min, friend) => 
      friend.marketValue < min.marketValue ? friend : min
    ) : null;
    
    return { bestFriends: best, worstFriends: worst, highestValueFriend, lowestValueFriend };
  }, [friends]);

  const getStatusColor = (status: string) => {
    switch (status) {
      case '관심':
        return 'text-[#1FFFA9]';
      case '유지':
        return 'text-[#0A74FF]';
      case '비관심':
        return 'text-[#E43F42]';
      default:
        return 'text-gray-400';
    }
  };

  // 숫자 포맷팅 함수 (양수/음수 모두 지원)
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

  const renderChart = () => {
    const highestFriend = highestValueFriend;
    const lowestFriend = lowestValueFriend;

    if (!highestFriend && !lowestFriend) return null;
    if (chartLoading) {
      return (
        <div className="relative h-full flex items-center justify-center">
          <div className="text-gray-400">차트 데이터 로딩 중...</div>
        </div>
      );
    }

    const highestColor = '#1FFFA9';
    const lowestColor = '#E43F42';

    // 실제 API 데이터에서 차트 데이터 추출
    const getChartDataFromAPI = (chartResponse: PersonChartResponse | null) => {
      if (!chartResponse?.data?.personValuesLast12) return [];
      
      // 12개월 데이터를 월순으로 정렬
      const values = chartResponse.data.personValuesLast12
        .sort((a, b) => {
          if (a.year !== b.year) return a.year - b.year;
          return a.month - b.month;
        })
        .map(item => item.value);

      return values;
    };

    const highestChartData = selectedChartType === 'best' ? getChartDataFromAPI(chartData.highest) : [];
    const lowestChartData = selectedChartType === 'worst' ? getChartDataFromAPI(chartData.lowest) : [];

    // 데이터가 없는 경우 메시지 표시
    const currentData = selectedChartType === 'best' ? highestChartData : lowestChartData;
    if (currentData.length === 0) {
      return (
        <div className="relative h-full flex items-center justify-center">
          <div className="text-center">
            <div className="text-gray-400">대화 내역을 등록하세요</div>
          </div>
        </div>
      );
    }

    // 차트 스케일링 (DashBoardPage와 동일한 방식)
    const maxValue = currentData.length > 0 ? Math.max(...currentData) : 1000;
    const minValue = currentData.length > 0 ? Math.min(...currentData) : 0;
    const valueRange = Math.max(1, maxValue - minValue);
    const yTicks = 6;
    const yLabels = Array.from({ length: yTicks }, (_, i) =>
      Math.round(minValue + ((yTicks - 1 - i) * valueRange) / (yTicks - 1))
    );

    // 좌표 변환 함수들
    const svgWidth = 500;
    const svgHeight = 300;
    const paddingTop = 10;
    const paddingBottom = 20;
    const usableHeight = svgHeight - paddingTop - paddingBottom;
    
    const getX = (index: number): number => {
      if (currentData.length <= 1) {
        return svgWidth / 2;
      }
      const step = svgWidth / (currentData.length - 1);
      return step * index;
    };
    
    const getY = (value: number): number => {
      if (valueRange === 0) return svgHeight / 2;
      const normalized = (value - minValue) / valueRange;
      return paddingTop + (1 - normalized) * usableHeight;
    };

    return (
      <div className="relative h-full">
        {/* Y축 라벨 */}
        <div className="absolute left-0 top-0 h-full flex flex-col justify-between text-gray-400 text-xs py-2">
          {yLabels.map((label, index) => (
            <span key={index}>₩{formatValue(label)}</span>
          ))}
        </div>

        {/* X축 라벨 */}
        <div className="absolute left-12 right-0 bottom-0 flex justify-between text-gray-400 text-xs px-2">
          {currentData.map((_, index) => {
            const chartItem = selectedChartType === 'best' 
              ? chartData.highest?.data?.personValuesLast12?.[index]
              : chartData.lowest?.data?.personValuesLast12?.[index];
            const date = chartItem ? `${chartItem.year}.${String(chartItem.month).padStart(2, '0')}` : `${index + 1}월`;
            return <span key={index}>{date}</span>;
          })}
        </div>

        {/* 그리드 라인 */}
        <div className="absolute left-12 right-0 top-0 h-full">
          <div className="h-full flex flex-col justify-between py-2">
            <div className="w-full h-px bg-gray-600/30"></div>
            <div className="w-full h-px bg-gray-600/30"></div>
            <div className="w-full h-px bg-gray-600/30"></div>
            <div className="w-full h-px bg-gray-600/30"></div>
            <div className="w-full h-px bg-gray-600/30"></div>
            <div className="w-full h-px bg-gray-600/30"></div>
          </div>
        </div>

        {/* 메인 그래프 라인 */}
        <div className="absolute left-12 right-0 top-0 h-full py-2">
          <svg
            className="w-full h-full cursor-pointer"
            viewBox="0 0 500 300"
            preserveAspectRatio="none"
            onMouseLeave={() => setHoveredPoint(null)}
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
                <stop offset="0%" stopColor={selectedChartType === 'best' ? highestColor : lowestColor} />
                <stop offset="100%" stopColor={selectedChartType === 'best' ? '#0A74FF' : '#FF6B6B'} />
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
                  stopColor={selectedChartType === 'best' ? highestColor : lowestColor}
                  stopOpacity="0.2"
                />
                <stop
                  offset="100%"
                  stopColor={selectedChartType === 'best' ? '#0A74FF' : '#FF6B6B'}
                  stopOpacity="0.05"
                />
              </linearGradient>
            </defs>

            {/* 영역 채우기 */}
            {currentData.length > 0 && (
              <path
                d={`M ${getX(0)} ${getY(currentData[0])} ` +
                  currentData
                    .slice(1)
                    .map((d, i) => `L ${getX(i + 1)} ${getY(d)}`)
                    .join(' ') +
                  ` L ${getX(currentData.length - 1)} ${svgHeight - paddingBottom}` +
                  ` L ${getX(0)} ${svgHeight - paddingBottom} Z`}
                fill="url(#areaGradient)"
              />
            )}

            {/* 메인 라인 */}
            {currentData.length > 0 && (
              <path
                d={`M ${getX(0)} ${getY(currentData[0])} ` +
                  currentData
                    .slice(1)
                    .map((d, i) => `L ${getX(i + 1)} ${getY(d)}`)
                    .join(' ')}
                stroke="url(#lineGradient)"
                strokeWidth="3"
                fill="none"
              />
            )}

            {/* 데이터 포인트들 */}
            {currentData.map((d, index) => (
              <circle
                key={index}
                cx={getX(index)}
                cy={getY(d)}
                r="6"
                fill={selectedChartType === 'best' ? highestColor : lowestColor}
                className="hover:r-8 transition-all"
                onMouseEnter={(e) => {
                  const chartItem = selectedChartType === 'best' 
                    ? chartData.highest?.data?.personValuesLast12?.[index]
                    : chartData.lowest?.data?.personValuesLast12?.[index];
                  const date = chartItem ? `${chartItem.year}.${String(chartItem.month).padStart(2, '0')}` : `${index + 1}월`;
                  const actualValue = `₩${formatValue(d)}`;
                  
                  // SVG 컨테이너의 상대적 위치 계산
                  const svgRect = e.currentTarget.closest('svg')?.getBoundingClientRect();
                  const containerRect = e.currentTarget.closest('.relative')?.getBoundingClientRect();
                  
                  if (svgRect && containerRect) {
                    const relativeX = e.clientX - containerRect.left;
                    const relativeY = e.clientY - containerRect.top;
                    setHoveredPoint({ x: relativeX, y: relativeY, date: `${date} ${actualValue}` });
                  }
                }}
              />
            ))}
          </svg>
        </div>


        {/* 호버 툴팁 */}
        {hoveredPoint && (
          <div
            className="absolute bg-black/90 text-white px-3 py-2 rounded-lg text-xs pointer-events-none z-10 min-w-40"
            style={{
              left: hoveredPoint.x > 300 ? hoveredPoint.x - 150 : hoveredPoint.x + 10,
              top: hoveredPoint.y - 10,
              transform: 'translateY(-100%)',
            }}
          >
            <div className="space-y-1">
              <div className="font-bold text-center border-b border-gray-600 pb-1">
                {hoveredPoint.date.split(' ')[0]}
              </div>
              <div className="flex justify-between">
                <span>관계 가치:</span>
                <span className="font-semibold text-[#1FFFA9]">
                  {hoveredPoint.date.split(' ')[1]}
                </span>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-[#0A0D0C] text-white">
        <Navigation />
        <div className="flex items-center justify-center min-h-[calc(100vh-80px)] px-6">
          <div className="text-gray-400">불러오는 중...</div>
        </div>
      </div>
    );
  }

  if (error || !hasFriends) {
    return (
      <div className="min-h-screen bg-[#0A0D0C] text-white">
        <Navigation />

        <div className="flex items-center justify-center min-h-[calc(100vh-80px)] px-6">
          <div className="text-center max-w-lg">
            <div className="mb-8">
              <i className="ri-user-add-line text-6xl text-gray-600 mb-4"></i>
              <h1 className="text-3xl font-bold mb-4">
                {error ? '목록을 불러오지 못했어요' : '친구가 없어요'}
              </h1>
              <p className="text-gray-400 text-lg mb-8">
                첫 친구를 등록하고 관계 분석을 시작해보세요
              </p>
            </div>

            <div className="space-y-4">
              <Button
                size="lg"
                onClick={() => navigate('/addfriend')}
                className="w-full"
              >
                친구 등록하러 가기
              </Button>

              <div className="bg-gray-900/50 p-6 rounded-xl border border-gray-800 text-left">
                <h3 className="font-bold mb-3 text-[#1FFFA9]">
                  친구 등록 가이드
                </h3>
                <ul className="space-y-2 text-sm text-gray-400">
                  <li className="flex items-start gap-2">
                    <span className="text-[#1FFFA9] mt-1">•</span>
                    카카오톡 대화 내역을 업로드하세요
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-[#1FFFA9] mt-1">•</span>
                    친구의 기본 정보를 입력하세요
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-[#1FFFA9] mt-1">•</span>
                    AI가 관계를 분석하여 투자 가치를 계산합니다
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="pt-16 min-h-screen bg-[#0A0D0C] text-white overflow-auto">
      <Navigation />

      <div className="flex">
        {/* 왼쪽 영역 - 관계 가치 그래프 & BEST/WORST */}
        <div className="flex-1 p-6 space-y-6">
          {/* 관계 가치 그래프 */}
          <div className="bg-gradient-to-br from-gray-900/90 to-gray-800/90 rounded-3xl p-6 border border-gray-700/50 backdrop-blur-sm">
            {/* 헤더와 버튼을 상단에 배치 */}
            <div className="flex justify-between items-center mb-4">
              <div className="flex items-center gap-4">
                <h2 className="text-white text-xl font-bold">
                  관계 지수
                </h2>
                {/* 범례를 헤더 옆에 배치 */}
                {selectedChartType === 'best' && highestValueFriend && (
                  <div className="flex items-center gap-2 text-xs">
                    <div
                      className="w-3 h-3 rounded-full"
                      style={{ backgroundColor: '#1FFFA9' }}
                    />
                    <span className="text-gray-300">HIGHEST: {highestValueFriend.name}</span>
                  </div>
                )}
                {selectedChartType === 'worst' && lowestValueFriend && (
                  <div className="flex items-center gap-2 text-xs">
                    <div
                      className="w-3 h-3 rounded-full"
                      style={{ backgroundColor: '#E43F42' }}
                    />
                    <span className="text-gray-300">LOWEST: {lowestValueFriend.name}</span>
                  </div>
                )}
              </div>
              {/* 차트 타입 선택 버튼 */}
              <div className="flex gap-2">
                <button
                  onClick={() => setSelectedChartType('best')}
                  className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors whitespace-nowrap ${
                    selectedChartType === 'best'
                      ? 'bg-[#1FFFA9] text-black'
                      : 'bg-gray-700/50 text-gray-300 hover:bg-gray-600/50 border border-gray-600/50'
                  }`}
                >
                  HIGHEST
                </button>
                <button
                  onClick={() => setSelectedChartType('worst')}
                  className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors whitespace-nowrap ${
                    selectedChartType === 'worst'
                      ? 'bg-[#E43F42] text-white'
                      : 'bg-gray-700/50 text-gray-300 hover:bg-gray-600/50 border border-gray-600/50'
                  }`}
                >
                  LOWEST
                </button>
              </div>
            </div>

            {/* 친구별 개별 차트 */}
            <div className="h-64">
              {renderChart()}
            </div>
          </div>

          {/* BEST/WORST 친구 섹션 */}
          <div className="flex-1 grid grid-cols-2 gap-6">
            {/* BEST 친구 */}
            <div className="bg-gradient-to-br from-gray-900/90 to-gray-800/90 rounded-2xl p-6 border border-gray-700/50 backdrop-blur-sm">
              <div className="flex items-center gap-3 mb-6">
                <div className="w-3 h-8 bg-[#1FFFA9] rounded-full"></div>
                <h3 className="text-xl font-bold text-[#1FFFA9]">
                  BEST 친구 TOP 5
                </h3>
              </div>
              <div className="space-y-4">
                {bestFriends.map((friend, index) => (
                  <div
                    key={friend.id}
                    onClick={() => navigate(`/friend/${friend.id}`)}
                    className="flex items-center justify-between p-4 bg-gray-800/30 rounded-xl border border-gray-700/30 hover:bg-gray-700/30 transition-colors cursor-pointer"
                  >
                    <div className="flex items-center gap-4">
                      <div className="flex items-center justify-center w-8 h-8 bg-[#1FFFA9] text-black font-bold text-sm rounded-full">
                        #{index + 1}
                      </div>
                      <div>
                        <div className="font-semibold text-white">
                          {friend.name}
                        </div>
                        <div className="text-xs text-gray-400">
                          {friend.relationship}
                        </div>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="font-bold text-white">
                        ₩{friend.marketValue.toLocaleString()}
                      </div>
                      <div className="text-xs text-[#1FFFA9]">
                        {friend.change}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* WORST 친구 */}
            <div className="bg-gradient-to-br from.gray-900/90 to-gray-800/90 rounded-2xl p-6 border border-gray-700/50 backdrop-blur-sm">
              <div className="flex items-center gap-3 mb-6">
                <div className="w-3 h-8 bg-[#E43F42] rounded-full"></div>
                <h3 className="text-xl font-bold text-[#E43F42]">
                  WORST 친구 TOP 5
                </h3>
              </div>
              <div className="space-y-4">
                {worstFriends.map((friend, index) => (
                  <div
                    key={friend.id}
                    onClick={() => navigate(`/friend/${friend.id}`)}
                    className="flex items-center justify-between p-4 bg-gray-800/30 rounded-xl border border-gray-700/30 hover:bg-gray-700/30 transition-colors cursor-pointer"
                  >
                    <div className="flex items-center gap-4">
                      <div className="flex items-center justify-center w-8 h-8 bg-[#E43F42] text-white font-bold text-sm rounded-full">
                        #{index + 1}
                      </div>
                      <div>
                        <div className="font-semibold text-white">
                          {friend.name}
                        </div>
                        <div className="text-xs text-gray-400">
                          {friend.relationship}
                        </div>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="font-bold text-white">
                        ₩{friend.marketValue.toLocaleString()}
                      </div>
                      <div className="text-xs text-[#E43F42]">
                        {friend.change}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* 오른쪽 영역 - 친구 리스트 */}
        <div className="w-80 bg-gradient-to-b from-gray-900/90 to-gray-800/90 border-l border-gray-700/50 p-6 backdrop-blur-sm">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-white text-lg font-bold">친구 리스트</h2>
            <Button
              size="sm"
              onClick={() => navigate('/addfriend')}
              className="text-xs"
            >
              + 친구추가
            </Button>
          </div>

          <div className="space-y-3">
            {friends.map((friend, index) => (
              <div
                key={friend.id}
                onClick={() => navigate(`/friend/${friend.id}`)}
                className="flex items-center justify-between p-4 hover:bg-gray-700/40 rounded-xl cursor-pointer transition-colors border border-gray-700/30 bg-gray-800/20"
              >
                <div className="flex items-center gap-4">
                  <div className="flex items-center justify-center w-8 h-8 bg-gradient-to-br from-[#1FFFA9] to-[#0A74FF] text-black font-bold text-sm rounded-full">
                    {index + 1}
                  </div>
                  <div>
                    <div className="font-semibold text-white text-sm">
                      {friend.name}
                    </div>
                    <div className="text-xs text-gray-400">
                      {friend.relationship}
                    </div>
                  </div>
                </div>

                <div className="text-right">
                  <div className="text-white text-sm font-semibold">
                    ₩{friend.marketValue.toLocaleString()}
                  </div>
                  <div
                    className={`text-xs ${friend.change.startsWith('+') ? 'text-[#1FFFA9]' : 'text-[#E43F42]'}`}
                  >
                    {friend.change}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
