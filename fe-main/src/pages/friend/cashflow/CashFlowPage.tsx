import { useState, useEffect, useMemo, useCallback } from 'react';
import type { JSX } from 'react';
import { useParams } from 'react-router-dom';
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  BarElement,
} from 'chart.js';
import { Doughnut, Bar } from 'react-chartjs-2';
import Navigation from '../../../components/feature/Navigation';
import FriendSidebar from '../components/FriendSidebar';
import FriendSubNav from '../components/FriendSubNav';
import api from '../../../lib/api/client';
import type {
  CashflowResponse,
  CashflowDirection,
} from '../../../types/cashflow';

const CATEGORY_MAP: Record<number, string> = {
  1: '생일',
  2: '결혼식',
  3: '선물',
  4: '식사',
  5: '금전',
  6: '돌잔치',
  7: '장례식',
};

ChartJS.register(
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  BarElement,
);

const CashFlow: React.FC = () => {
  const { id } = useParams();
  const [showAddForm, setShowAddForm] = useState(false);
  const [formData, setFormData] = useState({
    payer: '',
    content: '',
    categoryId: '4',
    amount: '',
    date: '',
  });

  // 로그인 사용자 이름 (임시: localStorage에서 가져오고 없으면 '나')
  const loggedInUserName =
    (typeof window !== 'undefined' && localStorage.getItem('userName')) || '나';

  // 달력 관련 상태
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [datePickerMode, setDatePickerMode] = useState<
    'date' | 'month' | 'year'
  >('date');

  // API data state
  const [apiData, setApiData] = useState<CashflowResponse['data'] | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState<boolean>(false);

  // Fetch cashflows from API
  const fetchCashflows = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const personId = id || '';
      if (!personId) {
        setApiData({
          history: [],
          categoryTotals: [],
          totalGive: 0,
          totalTake: 0,
          net: 0,
        });
        setLoading(false);
        return;
      }
      const res = await api.get<CashflowResponse>(
        `/people/${personId}/cashflows`,
      );
      console.log('GET Response:', res.data);
      setApiData(res.data.data);
    } catch (e: any) {
      setError(e?.message || '불러오는 중 오류가 발생했습니다.');
      setApiData({
        history: [],
        categoryTotals: [],
        totalGive: 0,
        totalTake: 0,
        net: 0,
      });
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchCashflows();
  }, [fetchCashflows]);

  // Normalize transactions for UI. Category name is always shown as '식사'.
  const transactions = useMemo(
    () =>
      (apiData?.history || []).map((h) => ({
        id: h.id,
        payer: h.direction === 'GIVE' ? '내가 결제' : '친구가 결제',
        content: h.item,
        category: CATEGORY_MAP[h.categoryId] || '기타',
        amount: h.price,
        date: h.date,
        type: h.direction === 'GIVE' ? 'out' : 'in',
      })),
    [apiData?.history],
  );

  const totalOut = apiData?.totalGive ?? 0;
  const totalIn = apiData?.totalTake ?? 0;
  const total = (apiData?.totalGive ?? 0) + (apiData?.totalTake ?? 0);

  // 카테고리는 API의 categoryId와 무관하게 표시상 모두 '식사'
  const categories = ['식사'];

  // 카테고리별 데이터 계산
  const categoryTotals = apiData?.categoryTotals || [];
  const categoryData = useMemo(() => {
    return categoryTotals
      .map((c) => Math.max(0, (c.give || 0) + (c.take || 0)))
      .filter((v) => v > 0);
  }, [categoryTotals]);

  const categoryLabels = useMemo(() => {
    return categoryTotals
      .map((c) => ({ name: CATEGORY_MAP[c.categoryId] || `카테고리 ${c.categoryId}`, total: (c.give || 0) + (c.take || 0) }))
      .filter((x) => x.total > 0)
      .map((x) => x.name);
  }, [categoryTotals]);

  // 차트 데이터 설정
  const chartData = {
    labels: categoryLabels,
    datasets: [
      {
        data: categoryData,
        backgroundColor: [
          '#1FFFA9',
          '#0A74FF',
          '#E43F42',
          '#FFB800',
          '#8B5CF6',
          '#F59E0B',
          '#EF4444',
          '#10B981',
        ],
        borderColor: '#0A0D0C',
        borderWidth: 2,
        hoverOffset: 10,
      },
    ],
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'right' as const,
        labels: {
          color: '#ffffff',
          padding: 20,
          font: {
            size: 14,
          },
        },
      },
      tooltip: {
        backgroundColor: '#1f2937',
        titleColor: '#ffffff',
        bodyColor: '#ffffff',
        borderColor: '#1FFFA9',
        borderWidth: 1,
        callbacks: {
          label: function (context: any) {
            const value = context.parsed;
            const percentage = ((value / total) * 100).toFixed(1);
            return `${context.label}: ₩${value.toLocaleString()} (${percentage}%)`;
          },
        },
      },
    },
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (submitting) return;

    const personId = id || '';
    if (!personId) {
      alert('사용자 또는 친구 정보가 없습니다. 다시 시도해 주세요.');
      console.warn('Missing identifiers', { personIdFromRoute: id });
      return;
    }

    // Basic validation
    if (!formData.payer) {
      alert('누가 결제했는지 선택해 주세요.');
      return;
    }
    if (!formData.content.trim()) {
      alert('내용을 입력해 주세요.');
      return;
    }
    if (!formData.amount || Number(formData.amount) <= 0) {
      alert('0보다 큰 금액을 입력해 주세요.');
      return;
    }

    const direction: CashflowDirection =
      formData.payer === '내가 결제' ? 'GIVE' : 'TAKE';
    const payload = {
      categoryId: Number(formData.categoryId) || 4,
      price: Number(formData.amount || 0),
      item: formData.content,
      direction,
      date: formData.date || new Date().toISOString().slice(0, 10),
    };

    console.log('Final payload to send:', JSON.stringify(payload, null, 2));

    try {
      setSubmitting(true);
      console.log('POST /cashflows payload', payload, { personId });
      const res = await api.post(
        `/people/${personId}/cashflows`,
        payload,
      );
      console.log('POST Response status:', res.status);


      // console.log('Trying alternative endpoints...');
      // for (const endpoint of alternativeEndpoints) {
      //   try {
      //     const altRes = await fetch(endpoint, {
      //       method: 'POST',
      //       headers: { 'Content-Type': 'application/json' },
      //       body: JSON.stringify(payload),
      //     });
      //     console.log(`Alternative ${endpoint}:`, altRes.status, await altRes.text());
      //   } catch (e) {
      //     console.log(`Alternative ${endpoint} failed:`, e);
      //   }
      // }

      // 성공 시 목록 갱신
      await fetchCashflows();
      setShowAddForm(false);
      setFormData({
        payer: '',
        content: '',
        categoryId: '4',
        amount: '',
        date: '',
      });
    } catch (err) {
      console.error('POST /cashflows failed', err);
      alert(
        '저장 중 오류가 발생했습니다. 네트워크 또는 권한 문제일 수 있습니다.',
      );
    } finally {
      setSubmitting(false);
    }
  };

  // 달력 관련 함수들
  const handleDateSelect = (date: Date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const dateString = `${year}-${month}-${day}`;
    setFormData({ ...formData, date: dateString });
    setShowDatePicker(false);
    setDatePickerMode('date');
  };

  const formatDateForDisplay = (dateString: string) => {
    if (!dateString) return '거래 날짜를 선택하세요';
    const date = new Date(dateString);
    return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`;
  };

  const getDaysInMonth = (date: Date) => {
    return new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
  };

  const getFirstDayOfMonth = (date: Date) => {
    return new Date(date.getFullYear(), date.getMonth(), 1).getDay();
  };

  const changeMonth = (direction: number) => {
    const newDate = new Date(currentMonth);
    newDate.setMonth(newDate.getMonth() + direction);
    setCurrentMonth(newDate);
  };

  const changeYear = (direction: number) => {
    const newDate = new Date(currentMonth);
    newDate.setFullYear(newDate.getFullYear() + direction);
    setCurrentMonth(newDate);
  };

  const changeYearRange = (direction: number) => {
    const newDate = new Date(currentMonth);
    newDate.setFullYear(newDate.getFullYear() + direction * 12);
    setCurrentMonth(newDate);
  };

  const handleYearSelect = (year: number) => {
    const newDate = new Date(currentMonth);
    newDate.setFullYear(year);
    setCurrentMonth(newDate);
    setDatePickerMode('month');
  };

  const handleMonthSelect = (month: number) => {
    const newDate = new Date(currentMonth);
    newDate.setMonth(month);
    setCurrentMonth(newDate);
    setDatePickerMode('date');
  };

  const handleHeaderClick = () => {
    if (datePickerMode === 'date') {
      setDatePickerMode('year');
    }
  };

  const isToday = (date: Date) => {
    const today = new Date();
    return date.toDateString() === today.toDateString();
  };

  const isSelected = (date: Date) => {
    if (!formData.date) return false;
    const selected = new Date(formData.date);
    return date.toDateString() === selected.toDateString();
  };

  const isFutureDate = (date: Date) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return date > today;
  };

  const renderCalendar = () => {
    const daysInMonth = getDaysInMonth(currentMonth);
    const firstDay = getFirstDayOfMonth(currentMonth);
    const days = [];

    // 빈 셀들 (이전 달)
    for (let i = 0; i < firstDay; i++) {
      days.push(<div key={`empty-${i}`} className="h-8 w-8"></div>);
    }

    // 현재 달의 날짜들
    for (let day = 1; day <= daysInMonth; day++) {
      const date = new Date(
        currentMonth.getFullYear(),
        currentMonth.getMonth(),
        day,
      );
      const isCurrentDay = isToday(date);
      const isSelectedDay = isSelected(date);
      const isFuture = isFutureDate(date);

      days.push(
        <button
          key={day}
          onClick={() => !isFuture && handleDateSelect(date)}
          disabled={isFuture}
          className={`h-8 w-8 text-sm rounded transition-colors ${
            isFuture
              ? 'text-gray-600 cursor-not-allowed'
              : isSelectedDay
                ? 'bg-[#1FFFA9] text-black font-bold'
                : isCurrentDay
                  ? 'bg-blue-600 text-white font-bold'
                  : 'text-gray-300 hover.bg-gray-700 cursor-pointer'
          }`}
        >
          {day}
        </button>,
      );
    }

    return days;
  };

  const renderYearGrid = () => {
    const currentYear = currentMonth.getFullYear();
    const startYear = Math.floor(currentYear / 12) * 12;
    const years: JSX.Element[] = [];

    for (let i = 0; i < 12; i++) {
      const year = startYear + i;
      const isCurrentYear = year === new Date().getFullYear();
      const isSelectedYear =
        formData.date && year === new Date(formData.date).getFullYear();

      years.push(
        <button
          key={year}
          onClick={() => handleYearSelect(year)}
          className={`h-12 rounded transition-colors cursor-pointer ${
            isSelectedYear
              ? 'bg-[#1FFFA9] text-black font-bold'
              : isCurrentYear
                ? 'bg-blue-600 text-white font-bold'
                : 'text-gray-300 hover:bg-gray-700'
          }`}
        >
          {year}
        </button>,
      );
    }

    return years;
  };

  const renderMonthGrid = () => {
    const months = [
      '1월',
      '2월',
      '3월',
      '4월',
      '5월',
      '6월',
      '7월',
      '8월',
      '9월',
      '10월',
      '11월',
      '12월',
    ];
    const currentYear = currentMonth.getFullYear();
    const selectedMonth = formData.date
      ? new Date(formData.date).getMonth()
      : -1;
    const selectedYear = formData.date
      ? new Date(formData.date).getFullYear()
      : -1;
    const currentMonthIndex = new Date().getMonth();
    const currentYear2 = new Date().getFullYear();

    return months.map((month, index) => {
      const isCurrentMonth =
        index === currentMonthIndex && currentYear === currentYear2;
      const isSelectedMonth =
        index === selectedMonth && currentYear === selectedYear;

      return (
        <button
          key={index}
          onClick={() => handleMonthSelect(index)}
          className={`h-12 rounded transition-colors cursor-pointer ${
            isSelectedMonth
              ? 'bg-[#1FFFA9] text-black font-bold'
              : isCurrentMonth
                ? 'bg-blue-600 text-white font-bold'
                : 'text-gray-300 hover:bg-gray-700'
          }`}
        >
          {month}
        </button>
      );
    });
  };

  return (
    <div className="pt-16 min-h-screen bg-[#0A0D0C] text-white">
      <Navigation />

      <div className="flex">
        <FriendSidebar friendId={id} />

        <div className="flex-1 ml-80">
          <FriendSubNav friendId={id || ''} currentPage="cashflow" />

          <div className="p-8">
            <div className="max-w-6xl mx-auto space-y-8">
              {/* 총 지출 합계 - 그래프와 함께 */}
              {loading ? (
                <div className="p-8 text-gray-400">불러오는 중...</div>
              ) : transactions.length === 0 ? null : (
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                  {/* 총 지출 합계 */}
                  <div className="bg-gray-900/50 rounded-xl p-6 border border-gray-800">
                    <div className="space-y-6">
                      {/* 메인 금액들 */}
                      <div className="grid grid-cols-2 gap-4">
                        <div className="text-center bg-[#E43F42]/5 rounded-lg p-4 border border-[#E43F42]/20">
                          <div className="text-2xl font-bold text-[#E43F42] mb-2">
                            ₩{totalOut.toLocaleString()}
                          </div>
                          <div className="text-gray-400 text-sm">
                            내가 쓴 금액
                          </div>
                          <div className="text-xs text-[#E43F42] mt-1">
                            {total > 0
                              ? `전체의 ${((totalOut / total) * 100).toFixed(1)}%`
                              : '0%'}
                          </div>
                        </div>
                        <div className="text-center bg-[#0A74FF]/5 rounded-lg p-4 border border-[#0A74FF]/20">
                          <div className="text-2xl font-bold text-[#0A74FF] mb-2">
                            ₩{totalIn.toLocaleString()}
                          </div>
                          <div className="text-gray-400 text-sm">
                            상대방이 쓴 금액
                          </div>
                          <div className="text-xs text-[#0A74FF] mt-1">
                            {total > 0
                              ? `전체의 ${((totalIn / total) * 100).toFixed(1)}%`
                              : '0%'}
                          </div>
                        </div>
                      </div>

                      {/* 총합과 분석 */}
                      <div className="text-center border-t border-gray-700 pt-4">
                        <div className="text-3xl font-bold text-[#1FFFA9] mb-2">
                          ₩{total.toLocaleString()}
                        </div>
                        <div className="text-gray-400 mb-4">총 거래 금액</div>

                        {/* 거래 분석 정보 */}
                        <div className="grid grid-cols-3 gap-3 text-sm">
                          <div className="bg-gray-800/30 rounded-lg p-3">
                            <div className="text-gray-400 mb-1">총 거래 수</div>
                            <div className="text-white font-bold">
                              {transactions.length}건
                            </div>
                          </div>
                          <div className="bg-gray-800/30 rounded-lg p-3">
                            <div className="text-gray-400 mb-1">
                              평균 거래액
                            </div>
                            <div className="text-white font-bold">
                              ₩
                              {transactions.length > 0
                                ? Math.round(
                                    total / transactions.length,
                                  ).toLocaleString()
                                : '0'}
                            </div>
                          </div>
                          <div className="bg-gray-800/30 rounded-lg p-3">
                            <div className="text-gray-400 mb-1">차액</div>
                            <div
                              className={`font-bold ${totalOut > totalIn ? 'text-[#E43F42]' : totalIn > totalOut ? 'text-[#0A74FF]' : 'text-gray-400'}`}
                            >
                              ₩{Math.abs(totalOut - totalIn).toLocaleString()}
                            </div>
                          </div>
                        </div>

                        {/* 상태 표시 */}
                        <div className="mt-4 p-3 rounded-lg bg-gray-800/30">
                          <div className="text-sm text-gray-400 mb-1">
                            거래 상태
                          </div>
                          <div
                            className={`font-bold ${totalOut > totalIn ? 'text-[#E43F42]' : totalIn > totalOut ? 'text-[#1FFFA9]' : 'text-gray-400'}`}
                          >
                            {totalOut > totalIn
                              ? '내가 더 많이 지출'
                              : totalIn > totalOut
                                ? '내가 더 많이 받음'
                                : '균형 잡힌 거래'}
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* 나와 상대방의 지출 비교 막대 그래프 */}
                  <div className="bg-gray-900/50 rounded-xl p-6 border border-gray-800">
                    <h3 className="text-xl font-bold text-[#1FFFA9] mb-6">
                      나와 상대방의 지출 비교
                    </h3>
                    <div className="h-80">
                      <Bar
                        data={{
                          labels: ['내가 쓴 금액', '상대방이 쓴 금액'],
                          datasets: [
                            {
                              data: [totalOut, totalIn],
                              backgroundColor: ['#E43F42', '#0A74FF'],
                              borderColor: ['#E43F42', '#0A74FF'],
                              borderWidth: 2,
                              borderRadius: 12,
                              borderSkipped: false,
                              maxBarThickness: 80,
                            },
                          ],
                        }}
                        options={{
                          responsive: true,
                          maintainAspectRatio: false,
                          plugins: {
                            legend: {
                              display: false,
                            },
                            tooltip: {
                              backgroundColor: '#1f2937',
                              titleColor: '#ffffff',
                              bodyColor: '#ffffff',
                              borderColor: '#1FFFA9',
                              borderWidth: 1,
                              cornerRadius: 8,
                              displayColors: true,
                              callbacks: {
                                title: function (context: any) {
                                  return context[0].label;
                                },
                                label: function (context: any) {
                                  const value = context.parsed.y;
                                  const percentage =
                                    total > 0
                                      ? ((value / total) * 100).toFixed(1)
                                      : '0';
                                  return `지출 금액: ₩${value.toLocaleString()}`;
                                },
                                afterLabel: function (context: any) {
                                  const value = context.parsed.y;
                                  const percentage =
                                    total > 0
                                      ? ((value / total) * 100).toFixed(1)
                                      : '0';
                                  return `전체 대비: ${percentage}%`;
                                },
                              },
                            },
                          },
                          scales: {
                            x: {
                              grid: {
                                display: false,
                              },
                              ticks: {
                                color: '#9CA3AF',
                                font: {
                                  size: 14,
                                  weight: 'bold' as const,
                                },
                              },
                            },
                            y: {
                              grid: {
                                color: '#374151',
                              },
                              ticks: {
                                color: '#9CA3AF',
                                font: {
                                  size: 12,
                                },
                                callback: function (value: any) {
                                  return '₩' + Number(value).toLocaleString();
                                },
                              },
                              beginAtZero: true,
                            },
                          },
                          animation: {
                            duration: 1000,
                            easing: 'easeInOutQuart' as any,
                          },
                        }}
                      />
                    </div>
                    <div className="mt-4 grid grid-cols-2 gap-4 text-center">
                      <div className="bg-[#E43F42]/10 rounded-lg p-3 border border-[#E43F42]/30">
                        <div className="text-[#E43F42] font-bold">
                          지출 차액
                        </div>
                        <div className="text-sm text-gray-400">
                          ₩{Math.abs(totalOut - totalIn).toLocaleString()}
                        </div>
                      </div>
                      <div className="bg-[#0A74FF]/10 rounded-lg p-3 border border-[#0A74FF]/30">
                        <div className="text-[#0A74FF] font-bold">
                          {totalOut > totalIn
                            ? '내가 더 많이 지출'
                            : totalIn > totalOut
                              ? '상대방이 더 많이 지출'
                              : '동일한 지출'}
                        </div>
                        <div className="text-sm text-gray-400">
                          {total > 0
                            ? `${((Math.abs(totalOut - totalIn) / total) * 100).toFixed(1)}%`
                            : '0%'}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* (removed duplicate 위치 블록) */}

              {/* 사용 금액 입력 */}
              <div className="bg-gray-900/50 rounded-xl p-6 border border-gray-800">
                <div className="flex items-center justify-between mb-6">
                  <h3 className="text-xl font-bold">사용 금액 입력</h3>
                  <button
                    onClick={() => setShowAddForm(!showAddForm)}
                    className="bg-[#1FFFA9] text-black px-4 py-2 rounded-lg font-medium hover:bg-[#1FFFA9]/80 transition-colors whitespace-nowrap cursor-pointer"
                  >
                    {showAddForm ? '취소' : '+ 추가'}
                  </button>
                </div>

                {showAddForm && (
                  <form onSubmit={handleSubmit} className="space-y-4 mb-6">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium mb-2">
                          누가 결제
                        </label>
                        <select
                          value={formData.payer}
                          onChange={(e) =>
                            setFormData({ ...formData, payer: e.target.value })
                          }
                          className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[#1FFFA9] pr-8"
                          required
                        >
                          <option value="">선택하세요</option>
                          <option value="내가 결제">내가 결제</option>
                          <option value="친구가 결제">친구가 결제</option>
                        </select>
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-2">카테고리</label>
                        <select
                          value={formData.categoryId}
                          onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
                          className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[#1FFFA9] pr-8"
                          required
                        >
                          {Object.entries(CATEGORY_MAP).map(([value, label]) => (
                            <option key={value} value={value}>{label}</option>
                          ))}
                        </select>
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-2">
                          내용
                        </label>
                        <input
                          type="text"
                          value={formData.content}
                          onChange={(e) =>
                            setFormData({
                              ...formData,
                              content: e.target.value,
                            })
                          }
                          className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[#1FFFA9]"
                          placeholder="어디에 사용했나요?"
                          required
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium mb-2">
                          금액
                        </label>
                        <input
                          type="text"
                          inputMode="numeric"
                          value={formData.amount ? Number(formData.amount).toLocaleString() : ''}
                          onChange={(e) =>
                            setFormData({
                              ...formData,
                              amount: e.target.value.replace(/[^\d]/g, ''),
                            })
                          }
                          className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[#1FFFA9]"
                          placeholder="금액을 입력하세요"
                          required
                        />
                      </div>
                      <div className="md:col-span-2">
                        <label className="block text-sm font-medium mb-2">
                          거래 날짜
                        </label>
                        <div className="relative">
                          <div
                            onClick={() => setShowDatePicker(!showDatePicker)}
                            className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm cursor-pointer hover:border-[#1FFFA9] transition-colors flex items-center justify-between"
                          >
                            <span
                              className={
                                formData.date ? 'text-white' : 'text-gray-400'
                              }
                            >
                              {formatDateForDisplay(formData.date)}
                            </span>
                            <i className="ri-calendar-line text-gray-400"></i>
                          </div>

                          {showDatePicker && (
                            <div className="absolute top-full left-0 mt-2 bg-gray-800 border border-gray-600 rounded-lg p-4 z-50 shadow-xl w-80">
                              {/* 달력 헤더 */}
                              <div className="flex items-center justify-between mb-4">
                                <div className="flex items-center gap-2">
                                  {datePickerMode === 'year' ? (
                                    <button
                                      onClick={() => changeYearRange(-1)}
                                      className="w-8 h-8 flex items-center justify-center hover:bg-gray-700 rounded cursor-pointer"
                                      title="이전 12년"
                                    >
                                      <i className="ri-arrow-left-double-line"></i>
                                    </button>
                                  ) : datePickerMode === 'month' ? (
                                    <button
                                      onClick={() => changeYear(-1)}
                                      className="w-8 h-8 flex items-center justify-center hover:bg-gray-700 rounded cursor-pointer"
                                      title="이전 년도"
                                    >
                                      <i className="ri-arrow-left-double-line"></i>
                                    </button>
                                  ) : (
                                    <>
                                      <button
                                        onClick={() => changeYear(-1)}
                                        className="w-8 h-8 flex items-center justify-center hover:bg-gray-700 rounded cursor-pointer"
                                        title="이전 년도"
                                      >
                                        <i className="ri-arrow-left-double-line"></i>
                                      </button>
                                      <button
                                        onClick={() => changeMonth(-1)}
                                        className="w-8 h-8 flex items-center justify-center hover.bg-gray-700 rounded cursor-pointer"
                                        title="이전 달"
                                      >
                                        <i className="ri-arrow-left-s-line"></i>
                                      </button>
                                    </>
                                  )}
                                </div>

                                <button
                                  onClick={handleHeaderClick}
                                  className="text-lg font-medium hover:text-[#1FFFA9] transition-colors cursor-pointer"
                                >
                                  {datePickerMode === 'year'
                                    ? `${Math.floor(currentMonth.getFullYear() / 12) * 12} - ${Math.floor(currentMonth.getFullYear() / 12) * 12 + 11}`
                                    : datePickerMode === 'month'
                                      ? `${currentMonth.getFullYear()}년`
                                      : `${currentMonth.getFullYear()}년 ${currentMonth.getMonth() + 1}월`}
                                </button>

                                <div className="flex items-center gap-2">
                                  {datePickerMode === 'year' ? (
                                    <button
                                      onClick={() => changeYearRange(1)}
                                      className="w-8 h-8 flex items-center justify-center hover:bg-gray-700 rounded cursor-pointer"
                                      title="다음 12년"
                                    >
                                      <i className="ri-arrow-right-double-line"></i>
                                    </button>
                                  ) : datePickerMode === 'month' ? (
                                    <button
                                      onClick={() => changeYear(1)}
                                      className="w-8 h-8 flex items-center justify-center hover:bg-gray-700 rounded cursor-pointer"
                                      title="다음 년도"
                                    >
                                      <i className="ri-arrow-right-double-line"></i>
                                    </button>
                                  ) : (
                                    <>
                                      <button
                                        onClick={() => changeMonth(1)}
                                        className="w-8 h-8 flex items-center justify-center hover:bg-gray-700 rounded cursor-pointer"
                                        title="다음 달"
                                      >
                                        <i className="ri-arrow-right-s-line"></i>
                                      </button>
                                      <button
                                        onClick={() => changeYear(1)}
                                        className="w-8 h-8 flex items-center justify-center hover:bg-gray-700 rounded cursor-pointer"
                                        title="다음 년도"
                                      >
                                        <i className="ri-arrow-right-double-line"></i>
                                      </button>
                                    </>
                                  )}
                                </div>
                              </div>

                              {/* 콘텐츠 영역 */}
                              <div className="mb-4">
                                {datePickerMode === 'year' ? (
                                  <div className="grid grid-cols-3 gap-2">
                                    {renderYearGrid()}
                                  </div>
                                ) : datePickerMode === 'month' ? (
                                  <div className="grid grid-cols-3 gap-2">
                                    {renderMonthGrid()}
                                  </div>
                                ) : (
                                  <>
                                    {/* 요일 헤더 */}
                                    <div className="grid grid-cols-7 gap-1 mb-2">
                                      {[
                                        '일',
                                        '월',
                                        '화',
                                        '수',
                                        '목',
                                        '금',
                                        '토',
                                      ].map((day) => (
                                        <div
                                          key={day}
                                          className="h-8 flex items-center justify-center text-xs text-gray-400 font-medium"
                                        >
                                          {day}
                                        </div>
                                      ))}
                                    </div>

                                    {/* 달력 날짜 */}
                                    <div className="grid grid-cols-7 gap-1">
                                      {renderCalendar()}
                                    </div>
                                  </>
                                )}
                              </div>

                              {/* 닫기 버튼 */}
                              <div className="flex justify-center">
                                <button
                                  onClick={() => {
                                    setShowDatePicker(false);
                                    setDatePickerMode('date');
                                  }}
                                  className="bg-[#1FFFA9] text-black px-6 py-2 rounded text-sm font-medium hover:bg-[#1FFFA9]/90 transition-colors cursor-pointer"
                                >
                                  닫기
                                </button>
                              </div>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                    <button
                      type="submit"
                      disabled={submitting}
                      className={`w-full bg-[#1FFFA9] text-black py-2 rounded-lg font-medium transition-colors whitespace-nowrap ${submitting ? 'opacity-60 cursor-not-allowed' : 'hover:bg-[#1FFFA9]/80 cursor-pointer'}`}
                    >
                      {submitting ? '추가 중...' : '추가하기'}
                    </button>
                  </form>
                )}
              </div>

              {/* 카테고리별 금액 비교 (사용 금액 입력 아래로 이동) */}
              {transactions.length === 0 ? null : (
                <div className="bg-gray-900/50 rounded-xl p-6 border border-gray-800">
                  <h3 className="text-xl font-bold mb-6">
                    카테고리별 금액 비교
                  </h3>
                  <div className="space-y-6">
                    {categoryTotals.map((categoryData, index) => {
                      const categoryName = CATEGORY_MAP[categoryData.categoryId] || `카테고리 ${categoryData.categoryId}`;
                      const giveAmount = categoryData.give || 0;
                      const takeAmount = categoryData.take || 0;
                      const categoryTotal = giveAmount + takeAmount;
                      
                      if (categoryTotal === 0) return null;
                      
                      const percentage = total > 0 ? (categoryTotal / total) * 100 : 0;
                      const givePercentage = categoryTotal > 0 ? (giveAmount / categoryTotal) * 100 : 0;
                      const takePercentage = categoryTotal > 0 ? (takeAmount / categoryTotal) * 100 : 0;

                      return (
                        <div key={categoryData.categoryId} className="space-y-3">
                          {/* 카테고리 헤더 */}
                          <div className="flex items-center justify-between">
                            <div className="flex items-center space-x-4">
                              <div
                                className="w-4 h-4 rounded-full"
                                style={{
                                  backgroundColor: (chartData.datasets[0] as any)
                                    .backgroundColor[index],
                                }}
                              ></div>
                              <span className="font-medium text-lg">{categoryName}</span>
                              <span className="text-sm text-gray-400">
                                전체의 {percentage.toFixed(1)}%
                              </span>
                            </div>
                            <span className="text-lg font-bold">
                              ₩{categoryTotal.toLocaleString()}
                            </span>
                          </div>
                          
                          {/* Give/Take 세부 정보 */}
                          <div className="ml-8 space-y-2">
                            {/* 내가 쓴 금액 (Give) */}
                            {giveAmount > 0 && (
                              <div className="flex items-center justify-between bg-[#E43F42]/5 rounded-lg p-3 border border-[#E43F42]/20">
                                <div className="flex items-center space-x-3">
                                  <div className="w-3 h-3 bg-[#E43F42] rounded-full"></div>
                                  <span className="text-sm text-[#E43F42] font-medium">내가 쓴 금액</span>
                                  <div className="w-32 bg-gray-700 rounded-full h-1.5">
                                    <div
                                      className="h-1.5 rounded-full bg-[#E43F42]"
                                      style={{ width: `${givePercentage}%` }}
                                    ></div>
                                  </div>
                                  <span className="text-xs text-gray-400">
                                    {givePercentage.toFixed(1)}%
                                  </span>
                                </div>
                                <span className="text-sm font-medium text-[#E43F42]">
                                  ₩{giveAmount.toLocaleString()}
                                </span>
                              </div>
                            )}
                            
                            {/* 상대방이 쓴 금액 (Take) */}
                            {takeAmount > 0 && (
                              <div className="flex items-center justify-between bg-[#0A74FF]/5 rounded-lg p-3 border border-[#0A74FF]/20">
                                <div className="flex items-center space-x-3">
                                  <div className="w-3 h-3 bg-[#0A74FF] rounded-full"></div>
                                  <span className="text-sm text-[#0A74FF] font-medium">상대방이 쓴 금액</span>
                                  <div className="w-32 bg-gray-700 rounded-full h-1.5">
                                    <div
                                      className="h-1.5 rounded-full bg-[#0A74FF]"
                                      style={{ width: `${takePercentage}%` }}
                                    ></div>
                                  </div>
                                  <span className="text-xs text-gray-400">
                                    {takePercentage.toFixed(1)}%
                                  </span>
                                </div>
                                <span className="text-sm font-medium text-[#0A74FF]">
                                  ₩{takeAmount.toLocaleString()}
                                </span>
                              </div>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* 전체 히스토리 */}
              {transactions.length === 0 ? null : (
                <div className="bg-gray-900/50 rounded-xl p-6 border border-gray-800">
                  <h3 className="text-xl font-bold mb-6">전체 히스토리</h3>
                  <div className="space-y-3">
                    {transactions.map((transaction) => (
                      <div
                        key={transaction.id}
                        className="flex items-center justify-between p-4 bg-gray-800/50 rounded-lg"
                      >
                        <div className="flex-1">
                          <div className="flex items-center space-x-3">
                            <span
                              className={`px-2 py-1 text-xs rounded-full ${
                                transaction.type === 'out'
                                  ? 'bg-[#E43F42]/20 text-[#E43F42]'
                                  : 'bg-[#0A74FF]/20 text-[#0A74FF]'
                              }`}
                            >
                              {transaction.payer}
                            </span>
                            <span className="font-medium">
                              {transaction.content}
                            </span>
                            <span className="text-sm text-gray-400">
                              ({transaction.category})
                            </span>
                          </div>
                        </div>
                        <div className="text-right">
                          <div
                            className={`font-bold ${transaction.type === 'out' ? 'text-[#E43F42]' : 'text-[#0A74FF]'}`}
                          >
                            ₩{transaction.amount.toLocaleString()}
                          </div>
                          <div className="text-xs text-gray-400">
                            {transaction.date}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CashFlow;
