import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import Navigation from '../../../components/feature/Navigation';
import FriendSidebar from '../components/FriendSidebar';
import FriendSubNav from '../components/FriendSubNav';
import { getOffer, createOffer, OfferPayload } from '../../../lib/api/people';

const GiftPrice: React.FC = () => {
  const { id } = useParams();
  const [formData, setFormData] = useState({
    category: '',
    freeCash: '',
  });
  const [offer, setOffer] = useState<OfferPayload | null>(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const categories = ['생일', '결혼식', '선물', '돌잔치'];
  const categoryMap: Record<string, number> = {
    '생일': 1,
    '결혼식': 2,
    '선물': 3,
    '돌잔치': 6,
  };

  // GET: Offer 불러오기
  useEffect(() => {
    const loadOffer = async () => {
      if (!id) return;
      setLoading(true);
      try {
        const res = await getOffer(id);
        console.log("📥 API raw response (getOffer):", res);
        console.log("📥 Offer data:", res.data);
        console.log("📥 Gifts array:", res.data?.gifts);
        setOffer(res.data);
      } catch (err) {
        console.error('❌ Failed to fetch offer', err);
        setError('추천 선물을 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };
    loadOffer();
  }, [id]);

  // POST: Offer 생성
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || submitting) return;
    
    setSubmitting(true);
    setError(null);
    
    try {
      const payload = {
        freeCash: Number(formData.freeCash) || 0,
        categoryId: categoryMap[formData.category] || 0,
        personId: Number(id),
      };
      console.log("📤 createOffer payload:", payload);
      const res = await createOffer(id, payload);
      console.log("📥 API raw response (createOffer):", res);
      console.log("📥 Created offer gifts:", res?.gifts);
      setOffer(res);
      setFormData({ category: '', freeCash: '' });
    } catch (err) {
      console.error('❌ Failed to create offer', err);
      setError('새로운 선물 추천을 받지 못했습니다.');
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
          <FriendSubNav friendId={id || ''} currentPage="giftprice" />

          <div className="p-8">
            <div className="max-w-6xl mx-auto space-y-8">
              
              {/* 데이터 있으면 전체 표시 */}
              {offer && (
                <>
                  {/* 추천 선물 금액 */}
                  <div className="bg-gradient-to-br from-[#1FFFA9]/20 to-[#0A74FF]/20 rounded-2xl p-8 border border-[#1FFFA9]/30">
                    <div className="text-center mb-6">
                      <h2 className="text-3xl font-bold text-white mb-2">추천 선물 금액</h2>
                      <div className="text-5xl font-bold text-[#1FFFA9] mb-4">
                        ₩{offer?.price.toLocaleString()}
                      </div>
                      <p className="text-gray-300">관계 및 조건을 기반으로 한 맞춤 추천</p>
                    </div>

                    <div className="bg-white/10 backdrop-blur rounded-xl p-6 text-center">
                      {submitting ? (
                        <div className="flex flex-col items-center justify-center py-8">
                          <div className="w-8 h-8 border-2 border-[#1FFFA9] border-t-transparent rounded-full animate-spin mb-4"></div>
                          <div className="text-lg text-gray-300 mb-2">분석 중...</div>
                          <div className="text-sm text-gray-400">잠시만 기다려주세요</div>
                        </div>
                      ) : (
                        <p className="text-sm text-gray-300 whitespace-pre-line">
                          {offer.content || '추천 근거가 없습니다.'}
                        </p>
                      )}
                      {error && <div className="text-red-400 mt-2">{error}</div>}
                    </div>
                  </div>

                  {/* 추천 선물 리스트 */}
                  <div className="bg-gray-900/50 rounded-xl p-6 border border-gray-800">
                    <h3 className="text-xl font-bold text-white mb-6 flex items-center">
                      <i className="ri-gift-line mr-2"></i>
                      추천 선물 리스트
                    </h3>
                    {offer.gifts && offer.gifts.length > 0 ? (
                      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        {offer.gifts.slice(0, 3).map((gift) => {
                          console.log("🎁 Individual gift:", gift);
                          return (
                          <div
                            key={gift.id}
                            className="bg-gray-800/30 rounded-lg overflow-hidden hover:bg-gray-800/50 transition-colors"
                          >
                            {(gift.imageUrl || gift.image) && (
                              <img
                                src={gift.imageUrl || gift.image}
                                alt={gift.title || gift.name}
                                className="w-full h-48 object-cover object-top"
                              />
                            )}
                            <div className="p-4">
                              <div className="flex items-center justify-between mb-2">
                                <span className="text-lg font-bold text-white">
                                  ₩{gift.price.toLocaleString()}
                                </span>
                              </div>
                              <h4 className="font-bold text-white mb-2">{gift.title || gift.name}</h4>
                              {gift.link && (
                                <a
                                  href={gift.link}
                                  target="_blank"
                                  rel="noreferrer"
                                  className="w-full inline-block text-center bg-[#0A74FF] hover:bg-[#0A74FF]/80 text-white py-2 rounded-lg text-sm font-medium transition-colors cursor-pointer"
                                >
                                  선물 보기
                                </a>
                              )}
                            </div>
                          </div>
                        );
                        })}
                      </div>
                    ) : (
                      <div className="text-gray-400">추천 선물이 없습니다.</div>
                    )}
                  </div>
                </>
              )}

              {/* 맞춤 분석 요청 */}
              <div className="bg-gray-900/50 rounded-xl p-6 border border-gray-800">
                <h3 className="text-xl font-bold text-white mb-4 flex items-center">
                  <i className="ri-search-line mr-2"></i>
                  맞춤 분석 요청
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
                        className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm"
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
                      <label className="block text-sm font-medium mb-2">
                        여유 금액
                      </label>
                      <input
                        type="text"
                        inputMode="numeric"
                        value={formData.freeCash ? Number(formData.freeCash).toLocaleString() : ''}
                        onChange={(e) => {
                          const value = e.target.value.replace(/[^\d]/g, '');
                          setFormData({ ...formData, freeCash: value });
                        }}
                        className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm"
                        placeholder="금액을 입력하세요"
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

            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default GiftPrice;
