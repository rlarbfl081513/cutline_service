import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Navigation from '../../components/feature/Navigation';
import { uploadPersonFile } from '../../lib/api/people';

export default function Upload() {
  const navigate = useNavigate();
  const { personId } = useParams<{ personId: string }>();
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadStatus, setUploadStatus] = useState<'idle' | 'uploading' | 'processing'>('idle');
  const [error, setError] = useState<string | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] || null;
    setUploadedFile(file);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadedFile || !personId) return;

    setIsUploading(true);
    setUploadStatus('uploading');
    setError(null);

    try {
      // 파일 업로드 시작
      setUploadStatus('uploading');
      const res = await uploadPersonFile(personId, uploadedFile);
      
      // 응답 처리 중
      setUploadStatus('processing');
      console.log('[uploadPersonFile response]', res);
      
      // 잠시 대기 후 성공 메시지와 함께 이동
      setTimeout(() => {
        navigate('/main');
      }, 1500);
      
    } catch (error: any) {
      console.error('Upload failed:', error);
      setError(error.response?.data?.message || '파일 업로드에 실패했습니다.');
      // 에러가 발생해도 3초 후 홈으로 이동
      setTimeout(() => navigate('/main'), 3000);
    } finally {
      // 상태 초기화는 navigate 직전에
      setTimeout(() => {
        setIsUploading(false);
        setUploadStatus('idle');
      }, 1500);
    }
  };

  const handleSkip = () => {
    // 홈 또는 친구 대시보드로 이동
    navigate(`/main`);
  };

  // personId가 없어도 업로드 화면을 그대로 노출 (제출 시에는 personId 필요)

  return (
    <div className="min-h-screen bg-[#0A0D0C] text-white">
      <Navigation />

      <div className="flex items-center justify-center min-h-[calc(100vh-80px)] px-6">
        <div className="text-center max-w-2xl">
          <div className="mb-8">
            <i className="ri-chat-upload-line text-6xl text-[#1FFFA9] mb-6"></i>
            <h1 className="text-3xl font-bold mb-4">
              대화 내용을 업로드해주세요
            </h1>
            <p className="text-gray-400 text-lg mb-8">
              카카오톡 대화 내역을 업로드하면 AI가 관계를 분석하여
              <br />
              투자 가치를 정확하게 계산해드립니다.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            {/* 파일 업로드 영역 */}
            <div className="border-2 border-dashed border-gray-600 rounded-xl p-12 hover:border-[#1FFFA9] transition-colors">
              <input
                type="file"
                id="chatFile"
                accept=".txt,.csv"
                onChange={handleFileChange}
                className="hidden"
                disabled={isUploading}
              />
              <label
                htmlFor="chatFile"
                className={`cursor-pointer block ${isUploading ? 'pointer-events-none opacity-50' : ''}`}
              >
                {isUploading ? (
                  <>
                    <i className="ri-loader-4-line text-4xl text-[#1FFFA9] mb-4 animate-spin"></i>
                    <p className="text-lg font-medium mb-2">
                      {uploadStatus === 'uploading' && '파일 업로드 중...'}
                      {uploadStatus === 'processing' && '분석 중...'}
                    </p>
                    <p className="text-sm text-gray-400">
                      {uploadStatus === 'uploading' && '파일을 서버로 전송하고 있습니다'}
                      {uploadStatus === 'processing' && '대화 내용을 분석하고 있습니다'}
                    </p>
                    <p className="text-xs text-gray-500 mt-2">
                      최대 5분 정도 시간이 소요됩니다.
                    </p>
                  </>
                ) : (
                  <>
                    <i className="ri-file-upload-line text-4xl text-gray-400 mb-4"></i>
                    <p className="text-lg font-medium mb-2">
                      파일을 드래그하거나 클릭하여 업로드
                    </p>
                    <p className="text-sm text-gray-400">
                      지원 형식: .txt, .csv (최대 10MB)
                    </p>
                  </>
                )}
              </label>
              {uploadedFile && !isUploading && (
                <p className="mt-4 text-[#1FFFA9] text-sm">
                  선택된 파일: {uploadedFile.name}
                </p>
              )}
            </div>

            {/* 가이드 섹션 */}
            <div className="bg-gray-900/50 p-6 rounded-xl border border-gray-800 text-left">
              <h3 className="font-bold mb-4 text-[#1FFFA9]">
                카카오톡 대화 내역 내보내기 방법
              </h3>
              <div className="space-y-3 text-sm text-gray-300">
                <div className="flex items-start gap-3">
                  <span className="bg-[#1FFFA9] text-black w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 mt-0.5">
                    1
                  </span>
                  <p>카카오톡에서 해당 친구와의 채팅방에 들어갑니다</p>
                </div>
                <div className="flex items-start gap-3">
                  <span className="bg-[#1FFFA9] text-black w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 mt-0.5">
                    2
                  </span>
                  <p>우상단 메뉴(≡) → 대화 내보내기를 선택합니다</p>
                </div>
                <div className="flex items-start gap-3">
                  <span className="bg-[#1FFFA9] text-black w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 mt-0.5">
                    3
                  </span>
                  <p>텍스트 파일로 저장하여 업로드해주세요</p>
                </div>
              </div>
            </div>

            {/* 에러 메시지 */}
            {error && (
              <div className="bg-red-900/50 border border-red-700 text-red-300 px-4 py-3 rounded-lg text-sm text-center">
                {error}
                <p className="text-xs text-red-400 mt-1">3초 후 메인 페이지로 이동합니다.</p>
              </div>
            )}

            {/* 버튼 */}
            <div className="flex gap-4 justify-center">
              <button
                type="submit"
                disabled={!uploadedFile || isUploading}
                className={`px-8 py-3 rounded-lg font-medium transition-colors cursor-pointer whitespace-nowrap ${
                  uploadedFile && !isUploading
                    ? 'bg-[#1FFFA9] text-black hover:bg-[#1FFFA9]/90'
                    : 'bg-gray-600 text-gray-400 cursor-not-allowed'
                }`}
              >
                {isUploading ? (
                  uploadStatus === 'uploading' ? '업로드 중...' : '분석 중...'
                ) : '등록하기'}
              </button>
              <button
                type="button"
                onClick={handleSkip}
                disabled={isUploading}
                className="bg-gray-700 text-white px-8 py-3 rounded-lg font-medium hover:bg-gray-600 transition-colors cursor-pointer whitespace-nowrap disabled:opacity-50 disabled:cursor-not-allowed"
              >
                나중에 하기
              </button>
            </div>
          </form>
        </div>
      </div>
      {isUploading && (
        <div className="fixed inset-0 z-50 bg-black/70 flex items-center justify-center">
          <div className="bg-gray-900/90 p-8 rounded-2xl border border-gray-700 text-center max-w-md">
            <i className="ri-loader-4-line text-5xl text-[#1FFFA9] animate-spin mb-4"></i>
            <h3 className="text-xl font-bold text-white mb-2">
              {uploadStatus === 'uploading' && '파일 업로드 중'}
              {uploadStatus === 'processing' && '대화 분석 중'}
            </h3>
            <p className="text-gray-400 text-sm">
              {uploadStatus === 'uploading' && '파일을 서버로 전송하고 있습니다...'}
              {uploadStatus === 'processing' && 'AI가 대화 내용을 분석하고 있습니다...'}
            </p>
            <p className="text-gray-500 text-xs mt-2">
              최대 5분 정도 시간이 소요됩니다.
            </p>
            <div className="mt-4 flex justify-center">
              <div className="flex space-x-1">
                <div className="w-2 h-2 bg-[#1FFFA9] rounded-full animate-pulse"></div>
                <div className="w-2 h-2 bg-[#1FFFA9] rounded-full animate-pulse" style={{animationDelay: '0.2s'}}></div>
                <div className="w-2 h-2 bg-[#1FFFA9] rounded-full animate-pulse" style={{animationDelay: '0.4s'}}></div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
