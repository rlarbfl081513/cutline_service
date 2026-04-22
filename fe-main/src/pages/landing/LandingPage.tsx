import { useNavigate } from 'react-router-dom';
import Navigation from '../../components/feature/Navigation';
import Button from '../../components/base/Button';

export default function Landing() {
  const navigate = useNavigate();

  return (
    <div className="pt-16 min-h-screen bg-[#0A0D0C] text-white">
      <Navigation />

      {/* Hero Section */}
      <div className="relative min-h-screen flex items-center justify-center px-6">
        {/* 배경 이미지 */}
        <img
          src="/background.png"
          alt="logo"
          className="absolute inset-0 w-full h-full object-cover"
        />

        {/* Hero Content */}
        <div className="relative z-10 text-center max-w-4xl mx-auto">
          <h1 className="text-5xl md:text-7xl font-bold mb-6">
            당신의 관계,
            <br />
            <span className="text-[#1FFFA9] mt-2">포트폴리오처럼 관리</span>하기
          </h1>
          <p className="text-xl md:text-2xl mb-8 text-gray-300">
            AI가 분석하는 친구 관계의 가치와 투자 수익률
          </p>
          <p className="text-lg mb-12 text-gray-400 max-w-2xl mx-auto">
            대화 내역을 분석하여 친구와의 관계를 수치화하고,
            <br />
            적정 선물 금액과 경조사비를 추천받아보세요.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Button
              size="lg"
              onClick={() => navigate('/login')}
              className="text-lg px-8 py-4"
            >
              시작하기
            </Button>
          </div>
        </div>
      </div>

      {/* Features Section */}
      <div className="py-24 px-6">
        <div className="max-w-6xl mx-auto">
          <h2 className="text-4xl font-bold text-center mb-16">
            <span className="text-[#1FFFA9]">커트라인</span>의 특별한 기능
          </h2>

          <div className="grid md:grid-cols-3 gap-8">
            <div className="bg-gray-900/50 p-8 rounded-xl border border-gray-800">
              <div className="w-12 h-12 bg-[#1FFFA9] rounded-lg flex items-center justify-center mb-6">
                <i className="ri-line-chart-line text-2xl text-black"></i>
              </div>
              <h3 className="text-xl font-bold mb-4">관계 시가총액</h3>
              <p className="text-gray-400">
                대화 빈도, 도움 횟수, 감정 분석을 통해 친구와의 관계 가치를
                수치화합니다
              </p>
            </div>

            <div className="bg-gray-900/50 p-8 rounded-xl border border-gray-800">
              <div className="w-12 h-12 bg-[#0A74FF] rounded-lg flex items-center justify-center mb-6">
                <i className="ri-gift-line text-2xl text-white"></i>
              </div>
              <h3 className="text-xl font-bold mb-4">선물 금액 추천</h3>
              <p className="text-gray-400">
                관계의 깊이와 과거 거래 내역을 분석하여 적정 선물 금액을
                제안합니다
              </p>
            </div>

            <div className="bg-gray-900/50 p-8 rounded-xl border border-gray-800">
              <div className="w-12 h-12 bg-[#E43F42] rounded-lg flex items-center justify-center mb-6">
                <i className="ri-calendar-event-line text-2xl text-white"></i>
              </div>
              <h3 className="text-xl font-bold mb-4">경조사비 계산</h3>
              <p className="text-gray-400">
                나이, 관계, 지역을 고려하여 적절한 경조사비 금액을 추천합니다
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
