import { useNavigate, useSearchParams } from 'react-router-dom';
import { useEffect } from 'react';
import Navigation from '../../components/feature/Navigation';
import { getUser } from '../../lib/api/users';

export default function Login() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  // 쿠키에서 값 가져오는 함수
  const getCookie = (name: string): string | null => {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
      return parts.pop()?.split(';').shift() || null;
    }
    return null;
  };

  // OAuth 콜백 처리
  useEffect(() => {
    const handleLogin = async () => {
      try {
        // 사용자 정보 가져오기 시도
        const userResponse = await getUser();
        const userData = userResponse.data;

        // 사용자 정보가 있으면 로그인 성공
        if (userData && userData.id) {
          // 사용자 정보를 localStorage에 저장
          localStorage.setItem('userInfo', JSON.stringify(userData));
          localStorage.setItem('userId', userData.id.toString());

          console.log('로그인 성공:', userData);

          // Navigation에서 감지할 수 있도록 이벤트 트리거
          window.dispatchEvent(new Event("storage"));

          // 최초 로그인 여부: age 또는 birth 정보 부재 시 프로필 입력으로 이동
          const hasProfile = Boolean((userData as any).age ?? userData.birth);
          if (!hasProfile) {
            navigate('/signinpage');
            return;
          }

          // 프로필 정보가 있으면 메인으로 이동
          navigate('/main');
        }
      } catch (error) {
        console.error('사용자 정보 가져오기 실패:', error);
        // 사용자 정보를 가져올 수 없으면 리디렉션하지 않고 현재 페이지 유지
      }
    };

    handleLogin();
  }, [navigate]);

  const handleKakaoLogin = () => {
    // 백엔드(Railway)로 납치(?)해버리는 코드
    window.location.href = `${import.meta.env.VITE_API_BASE_URL}/oauth2/authorization/kakao`;
  };

  return (
    <div className="pt-16 min-h-screen bg-[#0A0D0C] text-white">
      <Navigation />

      <div className="flex items-center justify-center min-h-[calc(100vh-80px)] px-6">
        <div className="max-w-md w-full">
          <div className="bg-gray-900/50 p-8 rounded-xl border border-gray-800">
            <div className="text-center mb-8">
              <h1 className="text-3xl font-bold mb-4">로그인</h1>
              <p className="text-gray-400">
                FriendStock에 오신 것을 환영합니다
              </p>
            </div>

            <div className="space-y-4">
              <button
                onClick={handleKakaoLogin}
                className="w-full bg-[#FEE500] text-black py-3 px-4 rounded-lg font-medium hover:bg-[#FEE500]/90 transition-colors cursor-pointer whitespace-nowrap flex items-center justify-center gap-3"
              >
                <i className="ri-kakao-talk-fill text-xl"></i>
                카카오 로그인
              </button>
            </div>

            <div className="mt-8 text-center">
              <p className="text-gray-400 text-sm">
                로그인하시면 서비스 이용약관 및 개인정보처리방침에 동의하게 됩니다.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
