import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import Navigation from '../../../components/feature/Navigation';
import FriendSidebar from '../components/FriendSidebar';
import FriendSubNav from '../components/FriendSubNav';
import {
  getPersonDashboardValue,
  CreatePersonPayload,
  PersonValuePayload,
  OfferGiftPayload,
  FamilyEventPayload,
} from '../../../lib/api/people';
import { uploadParsing } from '../../../lib/api/parsing';

const DashBoard: React.FC = () => {
  const { id } = useParams();
  const [showStrategy, setShowStrategy] = useState<string | null>(null);
  const [selectedStrategy, setSelectedStrategy] = useState<
    'INTEREST' | 'UNINTEREST' | 'MAINTAIN'
  >('INTEREST');

  // Status에 따른 기본 전략 설정
  const getDefaultStrategy = (
    status: CreatePersonPayload['status'],
  ): 'INTEREST' | 'UNINTEREST' | 'MAINTAIN' => {
    switch (status) {
      case 'INTEREST':
        return 'INTEREST';
      case 'MAINTAIN':
        return 'MAINTAIN';
      case 'UNINTEREST':
        return 'UNINTEREST';
      default:
        return 'INTEREST';
    }
  };
  const [hoveredPoint, setHoveredPoint] = useState<{
    x: number;
    y: number;
    value: number;
    date: string;
  } | null>(null);

  type SidebarFriend = {
    id: string | undefined;
    name: string;
    age: number;
    gender: string;
    status: string;
    relationship: string;
    period: string;
    messageCount: number; // API에서 받아온 실제 값
    avgMessageCount: number; // API에서 받아온 실제 값
    chatDays: number; // API에서 받아온 실제 값
    interests: string[]; // 하드코딩된 값
  };

  const [sidebarFriend, setSidebarFriend] = useState<SidebarFriend | null>(
    null,
  );
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [person, setPerson] = useState<CreatePersonPayload | null>(null);
  const [values, setValues] = useState<PersonValuePayload[]>([]);
  const [latestOffer, setLatestOffer] = useState<OfferGiftPayload | null>(null);
  const [latestFamilyEvent, setLatestFamilyEvent] =
    useState<FamilyEventPayload | null>(null);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const statusToKo = (s: CreatePersonPayload['status']): string => {
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

  const relationToKo = (r: CreatePersonPayload['relation']): string => {
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

  const genderToKo = (g?: CreatePersonPayload['gender']): string =>
    g === 'MALE' ? '남성' : g === 'FEMALE' ? '여성' : '미상';

  useEffect(() => {
    let mounted = true;
    async function fetchFriend() {
      if (!id) return;
      try {
        setLoading(true);
        setError(null);
        const res = await getPersonDashboardValue(id);
        const p = res.data.person;
        const pv = (res.data.personValuesLast12 || []).sort(
          (a, b) => a.year - b.year || a.month - b.month,
        );
        const lo = (res.data.latestOffer as OfferGiftPayload) || null;
        const lfe = (res.data.latestFamilyEvent as FamilyEventPayload) || null;
        // API에서 받은 age 필드 사용, 없으면 birth로 계산
        let age = p?.age || 0;
        if (!age && p?.birth) {
          const birthYear = Number(p.birth.slice(0, 4));
          if (!Number.isNaN(birthYear)) {
            const now = new Date();
            age = now.getFullYear() - birthYear;
          }
        }

        if (mounted) {
          setPerson(p);
          setValues(pv);
          setLatestOffer(lo);
          setLatestFamilyEvent(lfe);
          setSelectedStrategy(getDefaultStrategy(p.status));
          
          // 차트 데이터가 없으면 업로드 모달 표시
          if (pv.length === 0) {
            setShowUploadModal(true);
          }
        }
      } catch (e: any) {
        if (mounted) setError(e?.message || '친구 정보를 불러오지 못했습니다.');
      } finally {
        if (mounted) setLoading(false);
      }
    }
    fetchFriend();
    return () => {
      mounted = false;
    };
  }, [id]);

  const strategies = {
    INTEREST: {
      title: '관심 전략',
      content:
        person?.interestStrategy || '현재 관계에서 가장 효과적인 전략입니다.',
      color: 'border-[#1FFFA9] bg-[#1FFFA9]/10 text-[#1FFFA9]',
    },
    UNINTEREST: {
      title: '비관심 전략',
      content:
        person?.uninterestStrategy ||
        '이 관계가 더 이상 건설적이지 않다고 판단될 때 고려해볼 수 있는 전략입니다.',
      color: 'border-[#E43F42] bg-[#E43F42]/10 text-[#E43F42]',
    },
    MAINTAIN: {
      title: '유지 전략',
      content:
        person?.maintainStrategy ||
        '현재 관계 수준을 그대로 유지하는 것이 최선인 상황입니다.',
      color: 'border-[#0A74FF] bg-[#0A74FF]/10 text-[#0A74FF]',
    },
  };

  const chartData = values.map((v) => ({
    value: v.value,
    date: `${v.year}.${String(v.month).padStart(2, '0')}`,
  }));

  // chart helpers
  const viewW = 500;
  const viewH = 300;
  const padTop = 10;
  const padBottom = 30;
  const chartH = viewH - padTop - padBottom;
  const maxVal = chartData.length
    ? Math.max(...chartData.map((d) => d.value))
    : 600;
  const minVal = chartData.length
    ? Math.min(...chartData.map((d) => d.value))
    : 0;
  const valRange = Math.max(1, maxVal - minVal);
  const yScale = (v: number) => padTop + (1 - (v - minVal) / valRange) * chartH;
  const xScale = (i: number) =>
    chartData.length <= 1 ? 0 : (i / (chartData.length - 1)) * viewW;
  const pathD = chartData.length
    ? chartData
        .map((d, i) => `${i === 0 ? 'M' : 'L'} ${xScale(i)} ${yScale(d.value)}`)
        .join(' ')
    : '';
  const areaD = chartData.length
    ? `${pathD} L ${viewW} ${viewH} L 0 ${viewH} Z`
    : '';
  const yTicks = 6;
  const yLabels = Array.from({ length: yTicks }, (_, i) =>
    Math.round(
      minVal + ((yTicks - 1 - i) * valRange) / (yTicks - 1),
    ),
  );

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


  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !id) return;

    setUploading(true);
    setUploadError(null);

    try {
      const res = await uploadParsing(id, file);
      console.log('File uploaded successfully:', res);
      setShowUploadModal(false);
      // 데이터 새로고침
      window.location.reload();
    } catch (error: any) {
      console.error('Upload failed:', error);
      setUploadError(
        error?.response?.data?.data || 
        error?.response?.data?.message || 
        '파일 업로드에 실패했습니다.'
      );
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="pt-16 min-h-screen bg-[#0A0D0C] text-white">
      <Navigation />

      <div className="flex">
        <FriendSidebar friendId={id} />

        <div className="flex-1 ml-80">
          <FriendSubNav friendId={id || ''} currentPage="detail" />

          <div className="p-8">
            <div className="max-w-7xl mx-auto">
              {/* 전체 레이아웃 */}
              <div className="grid grid-cols-12 gap-8">
                {/* 왼쪽 영역 - 금액 현황과 차트 */}
                <div className="col-span-8 space-y-8">
                  {/* 금액 현황 */}
                  <Link
                    to={`/friend/${id}/cashflow`}
                    className="block bg-gray-900/50 rounded-xl p-6 border border-gray-800 hover:bg-gray-800/30 transition-colors cursor-pointer"
                  >
                    <div className="flex items-center justify-between mb-6">
                      <h3 className="text-xl font-bold text-white">
                        금액 현황
                      </h3>
                      <i className="ri-arrow-right-line text-gray-400"></i>
                    </div>
                    <div className="grid grid-cols-2 gap-6">
                      <div className="bg-[#E43F42]/10 rounded-lg p-6 border border-[#E43F42]/30">
                        <div className="text-sm text-gray-400 mb-2">
                          내가 쓴 금액
                        </div>
                        <div className="text-3xl font-bold text-[#E43F42]">
                          {loading
                            ? '로딩 중...'
                            : `₩${Number(person?.totalGive ?? 0).toLocaleString()}`}
                        </div>
                      </div>
                      <div className="bg-[#0A74FF]/10 rounded-lg p-6 border border-[#0A74FF]/30">
                        <div className="text-sm text-gray-400 mb-2">
                          내가 받은 금액
                        </div>
                        <div className="text-3xl font-bold text-[#0A74FF]">
                          {loading
                            ? '로딩 중...'
                            : `₩${Number(person?.totalTake ?? 0).toLocaleString()}`}
                        </div>
                      </div>
                    </div>
                  </Link>

                  {/* 친구 시가총액 그래프 */}
                  <Link
                    to={`/friend/${id}/chart`}
                    className="block bg-gradient-to-br from-gray-900/90 to-gray-800/90 rounded-xl p-8 relative border border-gray-700/50 backdrop-blur-sm hover:bg-gray-800/30 transition-colors cursor-pointer"
                  >
                    <div className="flex items-center justify-between mb-2">
                      <div className="text-white text-xl font-bold">
                        관계 지수
                      </div>
                      <i className="ri-arrow-right-line text-gray-400"></i>
                    </div>
                    <div className="text-3xl font-bold text-white mb-8">
                      {chartData.length
                        ? chartData[chartData.length - 1].value.toLocaleString()
                        : '0'}
                    </div>

                    {/* 그래프 영역 */}
                    <div className="h-80 relative mb-8">
                      {/* Y축 라벨 */}
                      <div className="absolute left-0 top-0 h-full flex flex-col justify-between text-gray-400 text-sm py-4">
                        {yLabels.map((l, i) => (
                          <span key={i}>₩{formatValue(l)}</span>
                        ))}
                      </div>

                      {/* 그리드 라인 */}
                      <div className="absolute left-12 right-0 top-0 h-full">
                        <div className="w-full h-px bg-gray-600/30"></div>
                        <div className="w-full h-px bg-gray-600/30"></div>
                        <div className="w-full h-px bg-gray-600/30"></div>
                        <div className="w-full h-px bg-gray-600/30"></div>
                        <div className="w-full h-px bg-gray-600/30"></div>
                        <div className="w-full h-px bg-gray-600/30"></div>
                      </div>

                      {/* 메인 그래프 라인 */}
                      <div className="absolute left-12 right-0 top-0 h-full py-4">
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

                          {/* 영역 채우기 */}
                          {chartData.length > 1 && (
                            <path d={areaD} fill="url(#areaGradient)" />
                          )}

                          {/* 메인 라인 */}
                          {chartData.length > 1 && (
                            <path
                              d={pathD}
                              stroke="url(#lineGradient)"
                              strokeWidth="3"
                              fill="none"
                            />
                          )}

                          {/* 데이터 포인트들 */}
                          {chartData.map((d, index) => {
                            const x = xScale(index);
                            const y = yScale(d.value);
                            return (
                              <circle
                                key={index}
                                cx={x}
                                cy={y}
                                r="6"
                                fill="#1FFFA9"
                                className="hover:r-8 transition-all"
                                onMouseEnter={(e) => {
                                  // SVG 컨테이너의 상대적 위치 계산
                                  const svgRect = e.currentTarget.closest('svg')?.getBoundingClientRect();
                                  const containerRect = e.currentTarget.closest('.relative')?.getBoundingClientRect();
                                  
                                  if (svgRect && containerRect) {
                                    const relativeX = e.clientX - containerRect.left;
                                    const relativeY = e.clientY - containerRect.top;
                                    setHoveredPoint({ x: relativeX, y: relativeY, value: d.value, date: d.date });
                                  }
                                }}
                              />
                            );
                          })}
                        </svg>
                      </div>

                      {/* X축 라벨 */}
                      <div className="absolute bottom-0 left-12 right-0 flex justify-between text-gray-400 text-sm">
                        {chartData.map((d, i) => (
                          <span key={i}>{d.date}</span>
                        ))}
                      </div>
                    </div>

                    {/* 호버 툴팁 */}
                    {hoveredPoint && (
                      <div
                        className="absolute bg-black/90 text-white px-4 py-3 rounded-lg text-sm pointer-events-none z-10 min-w-48"
                        style={{
                          left: hoveredPoint.x > 300 ? hoveredPoint.x - 150 : hoveredPoint.x + 10,
                          top: hoveredPoint.y - 6,
                          transform: 'translateY(-100%)',
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

                  </Link>
                </div>

                {/* 오른쪽 영역 */}
                <div className="col-span-4 space-y-8">
                  {/* 선물 금액 추천 */}
                  {latestOffer &&
                  latestOffer.gifts &&
                  latestOffer.gifts.length > 0 ? (
                    <Link
                      to={`/friend/${id}/giftprice`}
                      className="block bg-gray-900/50 rounded-xl p-6 border border-gray-800 hover:bg-gray-800/30 transition-colors cursor-pointer"
                    >
                      <div className="flex items-center justify-between mb-4">
                        <h3 className="text-lg font-bold text-white">
                          선물 금액 추천
                        </h3>
                        <i className="ri-arrow-right-line text-gray-400"></i>
                      </div>
                      <div className="bg-[#1FFFA9]/10 rounded-lg p-4 border border-[#1FFFA9]/30 mb-4">
                        <div className="flex justify-between items-center mb-2">
                          <span className="text-sm text-gray-400">
                            {latestOffer?.category?.title || '선물'} 기준
                          </span>
                        </div>
                        <div className="text-2xl font-bold text-[#1FFFA9]">
                          {(() => {
                            if (!latestOffer) return '₩0';
                            const giftPrices = (latestOffer.gifts || [])
                              .map((g) => g.price)
                              .filter(
                                (n) => typeof n === 'number' && !isNaN(n),
                              );
                            if (giftPrices.length >= 2) {
                              const min = Math.min(...giftPrices);
                              const max = Math.max(...giftPrices);
                              return `₩${min.toLocaleString()} - ₩${max.toLocaleString()}`;
                            }
                            const price =
                              latestOffer.price || giftPrices[0] || 0;
                            return `₩${Number(price).toLocaleString()}`;
                          })()}
                        </div>
                      </div>
                    </Link>
                  ) : (
                    <Link
                      to={`/friend/${id}/giftprice`}
                      className="block bg-gray-900/50 rounded-xl p-6 border border-gray-800 hover:bg-gray-800/30 transition-colors cursor-pointer"
                    >
                      <div className="flex items-center justify-between mb-4">
                        <h3 className="text-lg font-bold text-white">
                          선물 금액 분석
                        </h3>
                      </div>
                      <button className="bg-[#1FFFA9] text-black px-4 py-2 rounded-lg font-medium hover:bg-[#1FFFA9]/90 transition-colors">
                        + 분석하러 가기
                      </button>
                    </Link>
                  )}

                  {/* 경조사 금액 추천 */}
                  {latestFamilyEvent ? (
                    <Link
                      to={`/friend/${id}/eventprice`}
                      className="block bg-gray-900/50 rounded-xl p-6 border border-gray-800 hover:bg-gray-800/30 transition-colors cursor-pointer"
                    >
                      <div className="flex items-center justify-between mb-4">
                        <h3 className="text-lg font-bold text-white">
                          경조사 금액 추천
                        </h3>
                        <i className="ri-arrow-right-line text-gray-400"></i>
                      </div>
                      <div className="bg-[#0A74FF]/10 rounded-lg p-4 border border-[#0A74FF]/30 mb-4">
                        <div className="flex justify-between items-center mb-2">
                          <span className="text-sm text-gray-400">
                            {latestFamilyEvent?.category?.title || '경조사'}{' '}
                            기준
                          </span>
                        </div>
                        <div className="text-2xl font-bold text-[#0A74FF]">
                          {`₩${Number(latestFamilyEvent?.price ?? 0).toLocaleString()}`}
                        </div>
                      </div>
                    </Link>
                  ) : (
                    <Link
                      to={`/friend/${id}/eventprice`}
                      className="block bg-gray-900/50 rounded-xl p-6 border border-gray-800 hover:bg-gray-800/30 transition-colors cursor-pointer"
                    >
                      <div className="flex items-center justify-between mb-4">
                        <h3 className="text-lg font-bold text-white">
                          경조사 금액 분석
                        </h3>
                      </div>
                      <button className="bg-[#0A74FF] text-white px-4 py-2 rounded-lg font-medium hover:bg-[#0A74FF]/90 transition-colors">
                        + 분석하러 가기
                      </button>
                    </Link>
                  )}

                  {/* 관계 전략 시나리오 */}
                  <div className="bg-gray-900/50 rounded-xl p-6 border border-gray-800">
                    <h3 className="text-xl font-bold mb-6">
                      관계 전략 시나리오
                    </h3>
                    <div className="mb-6">
                      <div className="flex items-center justify-between gap-2 border border-[#1FFFA9]/60 rounded-full p-1">
                        {[
                          { key: 'INTEREST', label: '관심 전략' },
                          { key: 'MAINTAIN', label: '유지 전략' },
                          { key: 'UNINTEREST', label: '비관심 전략' },
                        ].map((tab) => (
                          <button
                            key={tab.key}
                            onClick={() =>
                              setSelectedStrategy(
                                tab.key as
                                  | 'INTEREST'
                                  | 'UNINTEREST'
                                  | 'MAINTAIN',
                              )
                            }
                            className={`flex-1 text-center py-2 rounded-full text-sm transition-colors ${
                              selectedStrategy === tab.key
                                ? 'bg-[#1FFFA9]/10 text-[#1FFFA9]'
                                : 'text-gray-300 hover:text-white'
                            }`}
                          >
                            {tab.label}
                          </button>
                        ))}
                      </div>
                    </div>

                    {/* 추천 전략 */}
                    <div className="mb-6">
                      <div
                        className={`${strategies[selectedStrategy].color} rounded-lg p-4 border-2`}
                      >
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center gap-2"></div>
                        </div>
                        <h4 className="text-lg font-bold mb-2">
                          {strategies[selectedStrategy].title}
                        </h4>
                        <p className="text-sm text-gray-300 leading-relaxed whitespace-pre-line">
                          {strategies[selectedStrategy].content}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 대화 내역 업로드 모달 */}
      {showUploadModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-gray-900 rounded-xl p-6 w-full max-w-lg mx-4 relative">
            <h3 className="text-xl font-bold text-white mb-6">
              대화 내역 업로드
            </h3>

            <div className="space-y-6">
              {/* 에러 메시지 */}
              {uploadError && (
                <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-3">
                  <p className="text-red-400 text-sm">{uploadError}</p>
                </div>
              )}

              {/* 안내 메시지 */}
              <div className="bg-blue-500/10 border border-blue-500/30 rounded-lg p-4">
                <div className="flex items-start gap-3">
                  <i className="ri-information-line text-blue-400 text-xl mt-0.5"></i>
                  <div>
                    <h4 className="text-blue-400 font-medium mb-1">차트 데이터가 없습니다</h4>
                    <p className="text-gray-300 text-sm">
                      관계 분석을 위해 카카오톡 대화 내역을 업로드해주세요.
                    </p>
                  </div>
                </div>
              </div>

              {/* 파일 업로드 영역 */}
              <div className={`border-2 border-dashed rounded-xl p-8 text-center transition-colors ${
                uploading 
                  ? 'border-gray-500 cursor-not-allowed opacity-50' 
                  : 'border-gray-600 hover:border-[#1FFFA9] cursor-pointer'
              }`}>
                <input
                  type="file"
                  id="chatFileUpload"
                  accept=".txt,.csv"
                  onChange={handleFileUpload}
                  disabled={uploading}
                  className="hidden"
                />
                <label
                  htmlFor="chatFileUpload"
                  className={`block ${uploading ? 'cursor-not-allowed' : 'cursor-pointer'}`}
                >
                  {uploading ? (
                    <>
                      <div className="h-10 w-10 mb-4 rounded-full border-4 border-[#1FFFA9]/30 border-t-[#1FFFA9] animate-spin mx-auto"></div>
                      <p className="text-lg font-medium mb-2">업로드 중...</p>
                      <p className="text-sm text-gray-400 mb-2">잠시만 기다려주세요</p>
                      <p className="text-xs text-yellow-400">
                        파일 업로드는 용량 및 네트워크 환경에 따라 최대 5분까지 소요될 수 있습니다.
                      </p>
                    </>
                  ) : (
                    <>
                      <i className="ri-file-upload-line text-4xl text-gray-400 mb-4"></i>
                      <p className="text-lg font-medium mb-2">
                        파일을 클릭하여 업로드 해주세요.
                      </p>
                      <p className="text-sm text-gray-400">
                        지원 형식: .txt
                      </p>
                    </>
                  )}
                </label>
              </div>

              {/* 가이드 섹션 */}
              <div className="bg-gray-800/50 p-4 rounded-xl border border-gray-700 text-left">
                <h4 className="font-bold mb-3 text-[#1FFFA9] text-sm">
                  카카오톡 대화 내역 내보내기 방법
                </h4>
                <div className="space-y-2 text-xs text-gray-300">
                  <div className="flex items-start gap-2">
                    <span className="bg-[#1FFFA9] text-black w-4 h-4 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 mt-0.5">
                      1
                    </span>
                    <p>카카오톡에서 해당 친구와의 채팅방에 들어갑니다</p>
                  </div>
                  <div className="flex items-start gap-2">
                    <span className="bg-[#1FFFA9] text-black w-4 h-4 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 mt-0.5">
                      2
                    </span>
                    <p>우상단 메뉴(≡) → 대화 내보내기를 선택합니다</p>
                  </div>
                  <div className="flex items-start gap-2">
                    <span className="bg-[#1FFFA9] text-black w-4 h-4 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 mt-0.5">
                      3
                    </span>
                    <p>텍스트 파일로 저장하여 업로드해주세요</p>
                  </div>
                </div>
              </div>

              {/* 버튼 */}
              <div className="flex space-x-3">
                <button
                  onClick={() => {
                    setShowUploadModal(false);
                    setUploadError(null);
                  }}
                  disabled={uploading}
                  className={`flex-1 py-2 px-4 rounded-lg transition-colors whitespace-nowrap ${
                    uploading 
                      ? 'bg-gray-600 text-gray-400 cursor-not-allowed' 
                      : 'bg-gray-700 hover:bg-gray-600 text-white cursor-pointer'
                  }`}
                >
                  나중에 하기
                </button>
              </div>
            </div>

            {uploading && (
              <div className="absolute inset-0 bg-black/60 rounded-xl flex items-center justify-center z-50">
                <div className="flex flex-col items-center gap-2 text-white">
                  <div className="h-10 w-10 mb-1 rounded-full border-4 border-[#1FFFA9]/30 border-t-[#1FFFA9] animate-spin mx-auto"></div>
                  <div className="text-lg font-medium">업로드 중입니다...</div>
                  <div className="text-sm text-yellow-300 text-center mt-1">
                    파일 업로드는 용량 및 네트워크 환경에 따라 최대 5분까지 소요될 수 있습니다.
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default DashBoard;
