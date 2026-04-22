import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Navigation from '../../components/feature/Navigation';
import Button from '../../components/base/Button';
import { createUser, CreateUserPayload, getUser } from '../../lib/api/users';

export default function SignIn() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    email: '',
    name: '',
    birth: '',
    gender: '',
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // 기존 사용자 정보 불러오기
  useEffect(() => {
    const loadUserData = async () => {
      try {
        const response = await getUser();
        const userData = response.data;
        
        setFormData({
          email: userData.email || '',
          name: userData.name || '',
          birth: userData.birth || '',
          gender: userData.gender === 'MALE' ? 'male' : userData.gender === 'FEMALE' ? 'female' : '',
        });
      } catch (error) {
        console.error('사용자 정보 불러오기 실패:', error);
        // 실패해도 계속 진행 (빈 폼으로)
      } finally {
        setLoading(false);
      }
    };
    
    loadUserData();
  }, []);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget as HTMLFormElement;
    
    if (!form.checkValidity()) {
      const firstInvalid = form.querySelector(':invalid') as HTMLElement | null;
      if (firstInvalid) {
        firstInvalid.focus();
        // 스크롤이 필요한 경우 부드럽게 이동
        if ('scrollIntoView' in firstInvalid) {
          firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
      }
      // 브라우저 기본 검증 메시지 표시
      form.reportValidity();
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      // 성별을 API 형식으로 변환
      const gender = formData.gender === 'male' ? 'MALE' : 'FEMALE';
      
      const payload: CreateUserPayload = {
        email: formData.email,
        name: formData.name,
        birth: formData.birth,
        gender: gender,
      };

      const response = await createUser(payload);
      console.log('사용자 정보 등록 성공:', response);

      // 사용자 정보를 localStorage에 저장
      if (response.data) {
        localStorage.setItem('userInfo', JSON.stringify(response.data));
        localStorage.setItem('userId', response.data.id.toString());
        
        // Navigation에서 감지할 수 있도록 이벤트 트리거
        window.dispatchEvent(new Event("storage"));
      }

      // 홈페이지로 이동
    navigate('/main');
    } catch (error: any) {
      console.error('사용자 정보 등록 실패:', error);
      setError(error.response?.data?.message || '사용자 정보 등록에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) => {
    const { name, value } = e.target;
    
    // 생일 필드의 경우 날짜 형식 자동 포맷팅
    if (name === 'birth' && e.target instanceof HTMLInputElement) {
      const formattedValue = formatDateInput(value);
      setFormData({
        ...formData,
        [name]: formattedValue,
      });
    } else {
      setFormData({
        ...formData,
        [name]: value,
      });
    }
  };

  // 날짜 입력 자동 포맷팅 함수
  const formatDateInput = (value: string) => {
    // 숫자만 추출
    const numbers = value.replace(/\D/g, '');
    
    // 길이에 따라 포맷팅
    if (numbers.length <= 4) {
      return numbers;
    } else if (numbers.length <= 6) {
      return `${numbers.slice(0, 4)}-${numbers.slice(4)}`;
    } else {
      return `${numbers.slice(0, 4)}-${numbers.slice(4, 6)}-${numbers.slice(6, 8)}`;
    }
  };

  if (loading) {
    return (
      <div className="pt-16 min-h-screen bg-[#0A0D0C] text-white">
        <Navigation />
        <div className="flex items-center justify-center min-h-[calc(100vh-80px)] px-6">
          <div className="text-gray-400">사용자 정보를 불러오는 중...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="pt-16 min-h-screen bg-[#0A0D0C] text-white">
      <Navigation />

      <div className="flex items-center justify-center min-h-[calc(100vh-80px)] px-6 py-12">
        <div className="max-w-md w-full">
          <div className="bg-gray-900/50 p-8 rounded-xl border border-gray-800">
            <div className="text-center mb-8">
              <h1 className="text-3xl font-bold mb-4">정보 입력</h1>
              <p className="text-gray-400">추가 정보를 입력해주세요</p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
              <div>
                <label className="block text-sm font-medium mb-2">이메일</label>
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  className="w-full bg-gray-700 border border-gray-600 rounded-lg px-4 py-3 text-gray-300 cursor-not-allowed"
                  placeholder="이메일 정보"
                  readOnly
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-2">이름</label>
                <input
                  type="text"
                  name="name"
                  value={formData.name}
                  className="w-full bg-gray-700 border border-gray-600 rounded-lg px-4 py-3 text-gray-300 cursor-not-allowed"
                  placeholder="이름 정보"
                  readOnly
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-2">생일</label>
                <input
                  type="text"
                  name="birth"
                  value={formData.birth}
                  onChange={handleChange}
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-[#1FFFA9] transition-colors"
                  placeholder="YYYY-MM-DD (예: 1990-01-01)"
                  maxLength={10}
                  required
                />
              </div>

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
                    <option value="">성별을 선택하세요</option>
                    <option value="male">남성</option>
                    <option value="female">여성</option>
                  </select>
                  <div className="absolute right-3 top-1/2 transform -translate-y-1/2 pointer-events-none">
                    <i className="ri-arrow-down-s-line text-gray-400"></i>
                  </div>
                </div>
              </div>

              {error && (
                <div className="bg-red-900/50 border border-red-700 text-red-300 px-4 py-3 rounded-lg text-sm">
                  {error}
                </div>
              )}

              <Button
                type="submit"
                className="w-full"
                size="lg"
                disabled={isSubmitting}
              >
                {isSubmitting ? (
                  <div className="flex items-center justify-center gap-2">
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    저장 중...
                  </div>
                ) : (
                  '저장하기'
                )}
              </Button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
