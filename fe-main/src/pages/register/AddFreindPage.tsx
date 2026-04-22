import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Navigation from '../../components/feature/Navigation';
import Button from '../../components/base/Button';
import { createPerson } from '../../lib/api/people';

export default function AddFriend() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    name: '',
    gender: '',
    age: '',
    status: '',
    relationship: '',
    period: '',
    chatFile: null as File | null,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const mapGender = (g: string) => (g === 'male' ? 'MALE' : 'FEMALE');
      const mapStatus = (s: string) =>
        s === '관심' ? 'INTEREST' : s === '비관심' ? 'UNINTEREST' : 'MAINTAIN';
      const mapRelation = (r: string) =>
        r === '친구' ? 'FRIEND' : r === '직장동료' ? 'COWORKER' : 'LOVER';

      const payload = {
        name: formData.name,
        // birth는 이 페이지에서 수집하지 않으므로 생략합니다. 필요 시 추가 가능
        gender: mapGender(formData.gender) as 'MALE' | 'FEMALE',
        age: Number(formData.age || 0),
        status: mapStatus(formData.status) as
          | 'INTEREST'
          | 'UNINTEREST'
          | 'MAINTAIN',
        relation: mapRelation(formData.relationship) as
          | 'FRIEND'
          | 'COWORKER'
          | 'LOVER',
        duration: Number(formData.period || 0),
      };

      const result = await createPerson(payload);
      // 성공 응답 검증: result.data.id가 존재할 때만 다음 페이지로 이동
      const newId = result?.data?.id;
      if (newId) {
        // 이후 업로드 단계에서 사용할 수 있도록 이름 저장(선택)
        try {
          localStorage.setItem('personName', formData.name);
        } catch {}
        navigate(`/friend/${newId}`);
      } else {
        setError('등록 응답이 올바르지 않습니다. 다시 시도해 주세요.');
      }
    } catch (err: any) {
      setError(
        err?.response?.data?.message || '친구 등록 중 오류가 발생했습니다.',
      );
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] || null;
    setFormData({
      ...formData,
      chatFile: file,
    });
  };

  return (
    <div className="pt-16 min-h-screen bg-[#0A0D0C] text-white">
      <Navigation />

      <div className="max-w-2xl mx-auto p-6">
        <div className="mb-8">
          <h1 className="text-3xl font-bold mb-4">친구 등록</h1>
          <p className="text-gray-400">
            새로운 친구를 등록하고 관계를 분석해보세요
          </p>
        </div>

        <div className="bg-gray-900/50 p-8 rounded-xl border border-gray-800">
          <form onSubmit={handleSubmit} className="space-y-6">
            {error && (
              <p className="text-red-500 text-sm" role="alert">
                {error}
              </p>
            )}
            <div className="grid md:grid-cols-2 gap-6">
              {/* 친구 이름 */}
              <div>
                <label className="block text-sm font-medium mb-2">
                  친구 이름
                </label>
                <input
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleChange}
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-[#1FFFA9] transition-colors"
                  placeholder="이름을 입력하세요"
                  required
                />
                <p className="text-sm text-gray-400 mt-2">카카오톡 이름과 동일하게 작성해주세요</p>
              </div>

              {/* 성별 */}
              <div>
                <label className="block text-sm font-medium mb-2">성별</label>
                <div className="relative">
                  <select
                    name="gender"
                    value={formData.gender}
                    onChange={handleChange}
                    className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 pr-8 text-white focus:outline-none focus:border-[#1FFFA9] transition-colors appearance-none cursor-pointer"
                    required
                  >
                    <option value="">선택하세요</option>
                    <option value="male">남성</option>
                    <option value="female">여성</option>
                  </select>
                  <div className="absolute right-3 top-1/2 transform -translate-y-1/2 pointer-events-none">
                    <i className="ri-arrow-down-s-line text-gray-400"></i>
                  </div>
                </div>
              </div>

              {/* 나이 */}
              <div>
                <label className="block text-sm font-medium mb-2">나이</label>
                <input
                  type="number"
                  name="age"
                  value={formData.age}
                  onChange={handleChange}
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-[#1FFFA9] transition-colors"
                  placeholder="나이를 입력하세요"
                  min="1"
                  max="100"
                  required
                />
              </div>

              {/* 상태 */}
              <div>
                <label className="block text-sm font-medium mb-2">상태</label>
                <div className="relative">
                  <select
                    name="status"
                    value={formData.status}
                    onChange={handleChange}
                    className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 pr-8 text-white focus:outline-none focus:border-[#1FFFA9] transition-colors appearance-none cursor-pointer"
                    required
                  >
                    <option value="">선택하세요</option>
                    <option value="관심">관심</option>
                    <option value="비관심">비관심</option>
                    <option value="유지">유지</option>
                  </select>
                  <div className="absolute right-3 top-1/2 transform -translate-y-1/2 pointer-events-none">
                    <i className="ri-arrow-down-s-line text-gray-400"></i>
                  </div>
                </div>
              </div>

              {/* 관계 */}
              <div>
                <label className="block text-sm font-medium mb-2">관계</label>
                <div className="relative">
                  <select
                    name="relationship"
                    value={formData.relationship}
                    onChange={handleChange}
                    className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 pr-8 text-white focus:outline-none focus:border-[#1FFFA9] transition-colors appearance-none cursor-pointer"
                    required
                  >
                    <option value="">선택하세요</option>
                    <option value="친구">친구</option>
                    <option value="직장동료">직장동료</option>
                    <option value="애인">애인</option>
                  </select>
                  <div className="absolute right-3 top-1/2 transform -translate-y-1/2 pointer-events-none">
                    <i className="ri-arrow-down-s-line text-gray-400"></i>
                  </div>
                </div>
              </div>

              {/* 만남기간 */}
              <div>
                <label className="block text-sm font-medium mb-2">
                  만남기간 (년)
                </label>
                <input
                  type="number"
                  name="period"
                  value={formData.period}
                  onChange={handleChange}
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-[#1FFFA9] transition-colors"
                  placeholder="알고 지낸 기간을 입력하세요"
                  min="0"
                  step="1"
                  required
                />
              </div>
            </div>

            {/* 버튼 */}
            <div className="flex gap-4 pt-6">
              <Button
                type="submit"
                className="flex-1"
                size="lg"
                disabled={loading}
              >
                {loading ? '등록 중...' : '등록하기'}
              </Button>
              <Button
                variant="secondary"
                onClick={() => navigate('/main')}
                className="flex-1"
                size="lg"
              >
                취소
              </Button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
