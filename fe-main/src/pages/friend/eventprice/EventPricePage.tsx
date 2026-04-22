import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import Navigation from '../../../components/feature/Navigation';
import FriendSidebar from '../components/FriendSidebar';
import FriendSubNav from '../components/FriendSubNav';
import { 
  getEventPriceHistory, 
  createEvent, 
  FamilyEventPayload 
} from '../../../lib/api/people';

const EventPrice: React.FC = () => {
  const { id } = useParams();
  const [formData, setFormData] = useState({
    category: '',
    distance: 0, // 숫자로 변경
  });
  const [showHistory, setShowHistory] = useState(false);
  const [history, setHistory] = useState<FamilyEventPayload[]>([]);
  const [latestEvent, setLatestEvent] = useState<FamilyEventPayload | null>(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 카테고리 (한글 UI → 서버 categoryId)
  const categories = ['결혼식', '선물', '돌잔치', '장례식'];
  const categoryMap: Record<string, number> = {
    '결혼식': 2,    // WEDDING
    '선물': 3,      // PRESENT
    '돌잔치': 6,    // BABY_SHOWER
    '장례식': 7,    // FUNERAL
  };

  // 히스토리 로드
  useEffect(() => {
    const load = async () => {
      if (!id) return;
      setLoading(true);
      setError(null);
      try {
        const events = await getEventPriceHistory(id); // 배열 반환
        const sorted = [...events].sort((a, b) => {
          const ta = new Date(a.createdAt || '').getTime();
          const tb = new Date(b.createdAt || '').getTime();
          return tb - ta;
        });
        setHistory(sorted);
        setLatestEvent(sorted[0] || null);
      } catch (e) {
        console.error('Failed to fetch history', e);
        setError('히스토리를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [id]);

  // 새로운 추천 요청 (POST)
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || submitting) return;

    setSubmitting(true);
    setError(null);

    try {
      const payload = {
        cost: formData.distance, // 서버에선 이동거리(cost) 필드로 전송
        categoryId: categoryMap[formData.category] || 0,
        personId: Number(id),
      };
      const res = await createEvent(id, payload);

      // 새 이벤트 반영
      setHistory([res, ...history]);
      setLatestEvent(res);
      setFormData({ category: '', distance: 0 });
    } catch (err) {
      console.error('❌ Failed to create event', err);
      setError('새로운 이벤트를 생성하지 못했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="pt-16 min-h-screen bg-[#0A0D0C] text-white">
      <Navigation />
      <div className="flex">
        <FriendSidebar friendId={id} />
        <div className="flex-1 ml-80">
          <FriendSubNav friendId={id || ''} currentPage="eventprice" />
          <div className="p-8">
            <div className="max-w-6xl mx-auto space-y-8">

              {/* === 히스토리가 있을 때만 보여줌 === */}
              {history.length > 0 && (
                <>
                  {/* 추천 금액 */}
                  <div className="bg-gradient-to-r from-[#1FFFA9]/10 to-[#0A74FF]/10 rounded-xl p-6 border border-[#1FFFA9]/30">
                    <h3 className="text-xl font-bold text-[#1FFFA9] mb-4 flex items-center">
                      <i className="ri-money-dollar-circle-line mr-2"></i>
                      추천 금액
                    </h3>
                    <div className="text-center py-8">
                      {loading || submitting ? (
                        <div className="flex flex-col items-center justify-center py-8">
                          <div className="w-8 h-8 border-2 border-[#1FFFA9] border-t-transparent rounded-full animate-spin mb-4"></div>
                          <div className="text-lg text-gray-300 mb-2">
                            {submitting ? '분석 중...' : '불러오는 중...'}
                          </div>
                          <div className="text-sm text-gray-400">
                            잠시만 기다려주세요
                          </div>
                        </div>
                      ) : latestEvent ? (
                        <>
                          <div className="text-5xl font-bold text-white mb-4">
                            ₩{latestEvent.price.toLocaleString()}
                          </div>
                          <p className="text-lg text-gray-300">
                            {latestEvent.category?.title || '이벤트'} 기준 최근 추천 금액입니다.
                          </p>
                        </>
                      ) : (
                        <>
                          <div className="text-5xl font-bold text-white mb-4">₩100,000</div>
                          <p className="text-lg text-gray-300">추천 금액을 불러오지 못했습니다.</p>
                        </>
                      )}
                      {error && <div className="text-red-400 mt-3">{error}</div>}
                    </div>
                  </div>

                  {/* 참석 여부 추천 */}
                  <div className="bg-gradient-to-r from-green-500/10 to-emerald-500/10 rounded-xl p-6 border border-green-500/30">
                    <h3 className="text-xl font-bold text-green-400 mb-6 flex items-center">
                      <i className="ri-check-line mr-2"></i>
                      참석 여부 추천
                    </h3>
                    <div className="text-center py-6">
                      {latestEvent ? (
                        <div
                          className={`inline-flex items-center space-x-2 px-6 py-3 rounded-full ${
                            latestEvent.attendance ? 'bg-green-500/20' : 'bg-red-500/20'
                          }`}
                        >
                          <i
                            className={`text-2xl ${
                              latestEvent.attendance
                                ? 'ri-thumb-up-line text-green-400'
                                : 'ri-thumb-down-line text-red-400'
                            }`}
                          ></i>
                          <span
                            className={`text-2xl font-bold ${
                              latestEvent.attendance ? 'text-green-400' : 'text-red-400'
                            }`}
                          >
                            {latestEvent.attendance ? '참석' : '미참석'}
                          </span>
                        </div>
                      ) : (
                        <div className="inline-flex items-center space-x-2 bg-green-500/20 px-6 py-3 rounded-full">
                          <i className="ri-thumb-up-line text-2xl text-green-400"></i>
                          <span className="text-2xl font-bold text-green-400">참석 추천</span>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* 참석 여부 결정 근거 */}
                  <div className="bg-gray-900/50 rounded-xl p-6 border border-gray-800">
                    <h3 className="text-xl font-bold mb-6 flex items-center">
                      <i className="ri-lightbulb-line mr-2 text-yellow-400"></i>
                      참석 여부 결정 근거
                    </h3>
                    <div className="space-y-4">
                      {submitting ? (
                        <div className="flex flex-col items-center justify-center py-8">
                          <div className="w-6 h-6 border-2 border-yellow-400 border-t-transparent rounded-full animate-spin mb-4"></div>
                          <div className="text-sm text-gray-300">
                            참석 여부 근거를 분석하는 중...
                          </div>
                        </div>
                      ) : (
                        <p className="text-sm text-gray-300 leading-relaxed whitespace-pre-line">
                          {latestEvent?.content || '추천 근거를 불러오지 못했습니다.'}
                        </p>
                      )}
                    </div>
                  </div>
                </>
              )}

              {/* 새로운 추천 요청 (항상 보이게) */}
              <div className="bg-gray-900/50 rounded-xl p-6 border border-gray-800">
                <h3 className="text-xl font-bold mb-6 flex items-center">
                  <i className="ri-add-line mr-2 text-[#1FFFA9]"></i>
                  새로운 추천 요청
                </h3>
                <form onSubmit={handleSubmit} className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium mb-2">카테고리</label>
                      <select
                        value={formData.category}
                        onChange={(e) =>
                          setFormData({ ...formData, category: e.target.value })
                        }
                        className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[#1FFFA9] pr-8"
                        required
                      >
                        <option value="">선택하세요</option>
                        {categories.map((cat) => (
                          <option key={cat} value={cat}>
                            {cat}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-2">이동 거리(분)</label>
                      <input
                        type="text"
                        inputMode="numeric"
                        pattern="[0-9]*"
                        value={formData.distance}
                        onChange={(e) =>
                          setFormData({
                            ...formData,
                            distance: Number(e.target.value.replace(/\D/g, '')),
                          })
                        }
                        className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[#1FFFA9]"
                        required
                      />
                    </div>
                  </div>
                  <button
                    type="submit"
                    disabled={submitting}
                    className={`w-full py-3 rounded-lg font-medium transition-colors whitespace-nowrap ${
                      submitting
                        ? 'bg-gray-600 text-gray-400 cursor-not-allowed'
                        : 'bg-[#1FFFA9] text-black hover:bg-[#1FFFA9]/80 cursor-pointer'
                    }`}
                  >
                    {submitting ? (
                      <div className="flex items-center justify-center gap-2">
                        <div className="w-4 h-4 border-2 border-gray-400 border-t-transparent rounded-full animate-spin"></div>
                        분석 중...
                      </div>
                    ) : (
                      '추천 받기'
                    )}
                  </button>
                </form>
              </div>

              {/* 추천 히스토리 (히스토리가 있을 때만) */}
              {history.length > 0 && (
                <div className="bg-gray-900/50 rounded-xl p-6 border border-gray-800">
                  <div className="flex items-center justify-between mb-4">
                    <div>
                      <h3 className="text-xl font-bold flex items-center">
                        <i className="ri-history-line mr-2 text-gray-400"></i>
                        추천 히스토리
                      </h3>
                      {latestEvent && (
                        <p className="text-gray-400 text-sm mt-1">
                          마지막 추천: {new Date(latestEvent.createdAt || '').toLocaleString()}
                        </p>
                      )}
                    </div>
                    <button
                      onClick={() => setShowHistory(!showHistory)}
                      className="flex items-center space-x-2 text-[#1FFFA9] hover:text-[#1FFFA9]/80 transition-colors cursor-pointer"
                    >
                      <span>이전 추천 내용 보기</span>
                      <i className={`ri-arrow-${showHistory ? 'up' : 'down'}-line`}></i>
                    </button>
                  </div>

                  {showHistory && (
                    <div className="space-y-4">
                      {history.map((event) => (
                        <div key={event.id} className="bg-gray-800/30 rounded-lg p-4">
                          <div className="flex items-center justify-between mb-3">
                            <div className="flex items-center space-x-3">
                              <span className="font-medium">
                                {event.category?.title || '이벤트'}
                              </span>
                              <span className="text-sm text-gray-400">
                                {event.createdAt
                                  ? new Date(event.createdAt).toLocaleString()
                                  : '날짜 미상'}
                              </span>
                              <span className="text-xs bg-gray-700 px-2 py-1 rounded-full">
                                {event.cost}분
                              </span>
                            </div>
                            <div className="text-right">
                              <div className="text-[#1FFFA9] font-bold">
                                ₩{event.price.toLocaleString()}
                              </div>
                              <div
                                className={`text-xs ${
                                  event.attendance ? 'text-green-400' : 'text-red-400'
                                }`}
                              >
                                {event.attendance ? '참석' : '미참석'}
                              </div>
                            </div>
                          </div>
                          {event.content && (
                            <p className="text-sm text-gray-300 whitespace-pre-line">
                              {event.content}
                            </p>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EventPrice;
