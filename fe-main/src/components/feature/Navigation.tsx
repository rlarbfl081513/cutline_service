import { useNavigate, useLocation } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { getUser, logout } from '../../lib/api/users';

export default function Navigation() {
  const navigate = useNavigate();
  const location = useLocation();

  // 로그인 상태 관리
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  // 쿠키에서 값 가져오는 함수
  const getCookie = (name: string): string | null => {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
      return parts.pop()?.split(';').shift() || null;
    }
    return null;
  };

  // 컴포넌트 마운트 시 로그인 상태 확인
  useEffect(() => {
    const checkLoginStatus = async () => {
      try {
        // localStorage에서 사용자 정보 확인
        const userInfo = localStorage.getItem('userInfo');
        const userId = localStorage.getItem('userId');
        
        // localStorage에 사용자 정보가 있으면 로그인 상태로 간주
        if (userInfo && userId) {
          setIsLoggedIn(true);
        } else {
          // localStorage에 정보가 없으면 API로 직접 확인
          const userResponse = await getUser();
          const userData = userResponse.data;
          
          if (userData && userData.id) {
            // API에서 사용자 정보를 가져올 수 있으면 로그인 상태
            localStorage.setItem('userInfo', JSON.stringify(userData));
            localStorage.setItem('userId', userData.id.toString());
            setIsLoggedIn(true);
          } else {
            setIsLoggedIn(false);
          }
        }
      } catch (error) {
        // API 호출 실패 시 로그아웃 상태로 간주
        setIsLoggedIn(false);
      }
    };

    checkLoginStatus();

    // localStorage 변경 감지
    const handleStorageChange = () => {
      checkLoginStatus();
    };

    // 주기적으로 상태 확인
    const interval = setInterval(checkLoginStatus, 5000); // 5초마다 확인
    
    // 페이지 포커스 시에도 확인
    const handleFocus = () => checkLoginStatus();
    
    window.addEventListener('storage', handleStorageChange);
    window.addEventListener('focus', handleFocus);
    
    return () => {
      clearInterval(interval);
      window.removeEventListener('storage', handleStorageChange);
      window.removeEventListener('focus', handleFocus);
    };
  }, []);

  const isActive = (path: string) => location.pathname === path;

  // 로그아웃 함수
  const handleLogout = async () => {
    if (confirm('로그아웃 하시겠습니까?')) {
      try {
        // 로그아웃 API 호출
        await logout();
        console.log('로그아웃 성공');
      } catch (error) {
        console.error('로그아웃 API 호출 실패:', error);
        // API 호출이 실패해도 로컬 로그아웃은 진행
      } finally {
        // localStorage에서 사용자 정보 제거
        localStorage.removeItem('userInfo');
        localStorage.removeItem('userId');
        
        // 로그인 상태를 false로 변경
        setIsLoggedIn(false);
        
        // 랜딩 페이지로 이동
        navigate('/');
      }
    }
  };


  return (
    <nav className="fixed top-0 left-0 w-full bg-[#0A0D0C] h-16 px-6 py-4 z-50">
      <div className="flex items-center justify-between">
        {/* 로고 */}
        <div
          className="cursor-pointer flex items-center"
          onClick={() => navigate('/')}
        >
          <img
            src="/cutline_logo2.png"
            alt="logo"
            className="h-8 w-auto object-contain"
          />
        </div>

        {/* 메뉴 */}
        <div className="flex items-center space-x-6">
          {isLoggedIn ? (
            <>
              <button
                onClick={() => navigate('/main')}
                className={`cursor-pointer transition-colors ${
                  isActive('/main')
                    ? 'text-[#1FFFA9]'
                    : 'text-white hover:text-[#1FFFA9]'
                }`}
              >
                HOME
              </button>
              <button
                onClick={() => navigate('/mypage')}
                className={`cursor-pointer transition-colors ${
                  isActive('/mypage')
                    ? 'text-[#1FFFA9]'
                    : 'text-white hover:text-[#1FFFA9]'
                }`}
              >
                MYPAGE
              </button>
              <button
                onClick={handleLogout}
                className="text-gray-400 hover:text-white transition-colors cursor-pointer"
              >
                로그아웃
              </button>
            </>
          ) : (
            <button
              onClick={() => navigate('/login')}
              className="bg-[#1FFFA9] text-black px-4 py-2 rounded-lg whitespace-nowrap cursor-pointer hover:bg-[#1FFFA9]/90 transition-colors"
            >
              LOGIN
            </button>
          )}
        </div>
      </div>
    </nav>
  );
}
