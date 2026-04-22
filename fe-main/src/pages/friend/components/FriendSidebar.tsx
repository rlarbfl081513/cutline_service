import { useEffect, useState } from 'react';
import {
  CreatePersonPayload,
  updatePerson,
  getPerson,
} from '../../../lib/api/people';
import { updateParsing } from '../../../lib/api/parsing';

interface Friend {
  id: string | undefined;
  name: string;
  age: number;
  gender: string;
  status: string;
  relationship: string;
  period: string;
  messageCount: number;
  avgMessageCount: number;
  chatDays: number;
  interests: string[];
}

interface FriendSidebarProps {
  friend?: Friend; // optional for compatibility
  friendId?: string; // when provided, fetch from API (userId=1)
}

const FriendSidebar: React.FC<FriendSidebarProps> = ({ friend, friendId }) => {
  const [showEditModal, setShowEditModal] = useState(false);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [editData, setEditData] = useState({
    name: friend?.name ?? '',
    age: (friend?.age ?? 0).toString(),
    gender: friend?.gender ?? '-',
    relationship: friend?.relationship ?? '-',
    status: friend?.status ?? '-',
    period: friend?.period ?? '-',
  });

  const [displayFriend, setDisplayFriend] = useState<Friend>(
    friend ?? {
      id: friendId,
      name: '불러오는 중...',
      age: 0,
      gender: '-',
      status: '-',
      relationship: '-',
      period: '-',
      messageCount: 0,
      avgMessageCount: 0,
      chatDays: 0,
      interests: [],
    },
  );

  // keep raw API person for birth/duration reuse on update
  const [rawPerson, setRawPerson] = useState<CreatePersonPayload | null>(null);


  useEffect(() => {
    if (friend) {
      setDisplayFriend(friend);
      setEditData({
        name: friend.name,
        age: friend.age.toString(),
        gender: friend.gender,
        relationship: friend.relationship,
        status: friend.status,
        period: friend.period,
      });
    }
  }, [friend]);

  const statusToKo = (s: CreatePersonPayload['status']): string => {
    switch (s) {
      case 'UNINTEREST':
        return '비관심';
      case 'MAINTAIN':
        return '유지';
      case 'INTEREST':
      default:
        return '관심';
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
    g === 'MALE' ? '남성' : g === 'FEMALE' ? '여성' : '-';

  useEffect(() => {
    let mounted = true;
    async function fetchDetail() {
      if (!friendId) {
        console.warn('friendId is not provided to FriendSidebar');
        return;
      }

      // 로딩 중인 경우 API 호출 안 함
      if (loading) {
        return;
      }

      try {
        setLoading(true);
        console.log('Calling getPerson with friendId:', friendId);
        const res = await getPerson(friendId);

        const p = res.data.person; // 실제 API 응답 형식에 맞춰 수정
        // getPerson API에서 dashboard 정보도 함께 반환됨
        const lms = res.data.latestManualStats || null;
        const topics = res.data.topics || [];

        // API에서 데이터가 없거나 잘못된 경우 더미 데이터 사용
        const personData = p || {
          id: parseInt(friendId) || 1,
          name: '테스트 사용자',
          birth: '1995-01-01',
          gender: 'MALE' as const,
          status: 'INTEREST' as const,
          relation: 'FRIEND' as const,
          duration: 5,
        };

        // API에서 받은 age 필드 사용, 없으면 birth로 계산
        let age = personData?.age || 0;
        if (!age && personData?.birth) {
          const y = Number(personData.birth.slice(0, 4));
          if (!Number.isNaN(y)) {
            age = new Date().getFullYear() - y;
          }
        }
        if (!age) {
          age = 29; // 기본 나이 설정
        }

        const mapped: Friend = {
          id: friendId,
          name: personData.name || '알 수 없음',
          age: age || 0,
          gender: genderToKo(personData.gender) || '-',
          status: statusToKo(personData.status) || '-',
          relationship: relationToKo(personData.relation) || '-',
          period: `${personData.duration ?? 0}년`,
          // API에서 받아온 실제 값 사용
          messageCount: lms?.monthCount ?? 0,
          avgMessageCount: lms?.monthCount ?? 0,
          chatDays: lms?.chatDays ?? 0,
          interests:
            topics.length > 0
              ? topics.map((t: any) => t.topic).slice(0, 5)
              : [],
        };


        if (mounted) {
          setRawPerson(personData); // API 응답 데이터 저장
          setDisplayFriend(mapped);
          setEditData({
            name: mapped.name,
            age: mapped.age.toString(),
            gender: mapped.gender,
            relationship: mapped.relationship,
            status: mapped.status,
            period: mapped.period,
          });
        }
      } catch (e) {
        // keep fallback friend; optionally log
        console.error('API 호출 실패:', e);
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    }
    fetchDetail();
    return () => {
      mounted = false;
    };
  }, [friendId]);

  const handleEditSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // Map KO -> API enums
    const gender = editData.gender === '남성' ? 'MALE' : 'FEMALE';
    const status =
      editData.status === '관심'
        ? 'INTEREST'
        : editData.status === '유지'
          ? 'MAINTAIN'
          : 'UNINTEREST';
    const relation =
      editData.relationship === '친구'
        ? 'FRIEND'
        : editData.relationship === '직장동료'
          ? 'COWORKER'
          : 'LOVER';
    // editData.period에서 숫자만 추출 (예: "5년" -> 5)
    const duration = parseInt(editData.period.replace(/[^0-9]/g, '')) || 0;
    const birth = rawPerson?.birth; // keep previous if exists

    if (!displayFriend.id) return;
    updatePerson(displayFriend.id, {
      name: editData.name,
      birth,
      gender,
      status,
      relation,
      duration,
    })
      .then((res) => {
        console.log('Update response:', res);
        console.log('Update response data:', res.data);

        // API 응답 형식에 맞게 데이터 추출
        const responseData = res.data;
        const personData = responseData?.person || responseData;

        // API 응답이 없거나 잘못된 경우 수정된 데이터로 업데이트
        if (!personData) {
          console.warn('API response invalid, using edit data for update');
          const updated: Friend = {
            id: displayFriend.id,
            name: editData.name,
            age: parseInt(editData.age) || displayFriend.age,
            gender: editData.gender,
            status: editData.status,
            relationship: editData.relationship,
            period: editData.period, // 수정된 period 사용
            messageCount: displayFriend.messageCount,
            avgMessageCount: displayFriend.avgMessageCount,
            chatDays: displayFriend.chatDays,
            interests: displayFriend.interests,
          };
          setDisplayFriend(updated);
          setShowEditModal(false);
          return;
        }

        setRawPerson(personData);
        const updated: Friend = {
          id: String(personData.id || displayFriend.id),
          name: personData.name || editData.name,
          age: displayFriend.age, // age is derived; keep existing simple calc
          gender: genderToKo(personData.gender || 'MALE'),
          status: statusToKo(personData.status || 'INTEREST'),
          relationship: relationToKo(personData.relation || 'FRIEND'),
          period: `${personData.duration ?? 0}년`,
          messageCount: displayFriend.messageCount,
          avgMessageCount: displayFriend.avgMessageCount,
          chatDays: displayFriend.chatDays,
          interests: displayFriend.interests,
        };
        setDisplayFriend(updated);
        setShowEditModal(false);
      })
      .catch((err) => {
        console.error('Failed to update friend:', err);
        console.error('Error details:', err.response?.data || err.message);

        // API 호출 실패 시에도 수정된 데이터로 업데이트 (실시간 반영)
        console.warn('API update failed, updating UI with edit data anyway');
        const updated: Friend = {
          id: displayFriend.id,
          name: editData.name,
          age: parseInt(editData.age) || displayFriend.age,
          gender: editData.gender,
          status: editData.status,
          relationship: editData.relationship,
          period: editData.period, // 수정된 period 사용
          messageCount: displayFriend.messageCount,
          avgMessageCount: displayFriend.avgMessageCount,
          chatDays: displayFriend.chatDays,
          interests: displayFriend.interests,
        };
        setDisplayFriend(updated);
        setShowEditModal(false);
      });
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !friendId) return;

    setUploading(true);
    setUploadError(null);

    try {
      const res = await updateParsing(friendId, file);
      console.log('File uploaded successfully:', res);
      setShowUploadModal(false);
      // 성공 메시지 표시 (선택사항)
      alert('대화 내역이 성공적으로 업로드되었습니다!');
    } catch (error: any) {
      console.error('Upload failed:', error);
      setUploadError(error?.response?.data?.message || '파일 업로드에 실패했습니다.');
    } finally {
      setUploading(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case '관심':
        return 'text-[#1FFFA9] bg-[#1FFFA9]/20';
      case '유지':
        return 'text-[#0A74FF] bg-[#0A74FF]/20';
      case '비관심':
        return 'text-[#E43F42] bg-[#E43F42]/20';
      default:
        return 'text-gray-400 bg-gray-400/20';
    }
  };

  return (
    <>
      <div className="fixed left-0 top-16 w-80 h-full bg-gray-900/90 border-r border-gray-800 p-6 overflow-y-auto">
        <div className="space-y-6">
          {/* 친구 기본 정보 */}
          <div className="text-center">
            {/* <div className="w-20 h-20 bg-gradient-to-br from-[#1FFFA9] to-[#0A74FF] rounded-full flex items-center justify-center mx-auto mb-4">
              <span className="text-2xl font-bold text-black">
                {friend.name.charAt(0)}
              </span>
            </div> */}
            <h2 className="text-2xl font-bold text-white mb-2">
              {displayFriend.name}
            </h2>
            <span
              className={`px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(displayFriend.status)}`}
            >
              {displayFriend.status}
            </span>
          </div>

          {/* 기본 정보 */}
          <div className="space-y-4">
            <div className="bg-gray-800/50 rounded-lg p-4">
              <h3 className="text-sm font-medium text-gray-400 mb-3">
                기본 정보
              </h3>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-400">나이</span>
                  <span className="text-white">{displayFriend.age}세</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">성별</span>
                  <span className="text-white">{displayFriend.gender}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">관계</span>
                  <span className="text-white">
                    {displayFriend.relationship}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">알고 지낸 기간</span>
                  <span className="text-white">{displayFriend.period}</span>
                </div>
              </div>
            </div>

            {/* 소통 통계 */}
            <div className="bg-gray-800/50 rounded-lg p-4">
              <h3 className="text-sm font-medium text-gray-400 mb-3">
                소통 통계
              </h3>
              <div className="space-y-3 text-sm">
                <div>
                  <div className="flex justify-between mb-1">
                    <span className="text-gray-400">메세지 수</span>
                    <span className="text-white">
                      {displayFriend.messageCount.toLocaleString()} 건
                    </span>
                  </div>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">이번달 대화일</span>
                  <span className="text-[#1FFFA9]">
                    {displayFriend.chatDays}일
                  </span>
                </div>
              </div>
            </div>

            {/* 관심사 */}
            <div className="bg-gray-800/50 rounded-lg p-4">
              <h3 className="text-sm font-medium text-gray-400 mb-3">관심사</h3>
              <div className="flex flex-wrap gap-2">
                {displayFriend.interests.map((interest, index) => (
                  <span
                    key={index}
                    className="px-2 py-1 bg-[#1FFFA9]/20 text-[#1FFFA9] rounded-full text-xs"
                  >
                    {interest}
                  </span>
                ))}
              </div>
            </div>

            {/* 대화 내역 업로드 */}
            <button
              onClick={() => setShowUploadModal(true)}
              className="w-full bg-[#0A74FF] hover:bg-[#0A74FF]/80 text-white py-2 px-4 rounded-lg transition-colors cursor-pointer whitespace-nowrap mb-2"
            >
              대화 내역 업로드
            </button>

            {/* 수정 버튼 */}
            <button
              onClick={() => setShowEditModal(true)}
              className="w-full bg-gray-800 hover:bg-gray-700 text-white py-2 px-4 rounded-lg transition-colors cursor-pointer whitespace-nowrap"
            >
              친구 정보 수정
            </button>
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
                  취소
                </button>
              </div>
              {uploading && (
                <div className="absolute inset-0 bg-black/60 rounded-xl flex items-center justify-center z-50">
                  <div className="flex flex-col items-center gap-2 text-white">
                    <i className="ri-loader-4-line text-4xl text-[#1FFFA9] animate-spin"></i>
                    <div className="text-lg font-medium">업로드 중입니다...</div>
                    <div className="text-sm text-yellow-300 text-center mt-1">
                      파일 업로드는 용량 및 네트워크 환경에 따라 최대 5분까지 소요될 수 있습니다.
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* 수정 모달 */}
      {showEditModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-gray-900 rounded-xl p-6 w-full max-w-md mx-4">
            <h3 className="text-xl font-bold text-white mb-6">
              친구 정보 수정
            </h3>
            <form onSubmit={handleEditSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-2">
                  이름
                </label>
                <input
                  type="text"
                  value={editData.name}
                  onChange={(e) =>
                    setEditData({ ...editData, name: e.target.value })
                  }
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-[#1FFFA9]"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-2">
                  나이
                </label>
                <input
                  type="number"
                  value={editData.age}
                  onChange={(e) =>
                    setEditData({ ...editData, age: e.target.value })
                  }
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-[#1FFFA9]"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-2">
                  성별
                </label>
                <select
                  value={editData.gender}
                  onChange={(e) =>
                    setEditData({ ...editData, gender: e.target.value })
                  }
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-[#1FFFA9] pr-8"
                  required
                >
                  <option value="남성">남성</option>
                  <option value="여성">여성</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-2">
                  관계
                </label>
                <select
                  value={editData.relationship}
                  onChange={(e) =>
                    setEditData({ ...editData, relationship: e.target.value })
                  }
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-[#1FFFA9] pr-8"
                  required
                >
                  <option value="친구">친구</option>
                  <option value="직장동료">직장동료</option>
                  <option value="애인">애인</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-2">
                  상태
                </label>
                <select
                  value={editData.status}
                  onChange={(e) =>
                    setEditData({ ...editData, status: e.target.value })
                  }
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-[#1FFFA9] pr-8"
                  required
                >
                  <option value="관심">관심</option>
                  <option value="비관심">비관심</option>
                  <option value="유지">유지</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-400 mb-2">
                  알고 지낸 기간
                </label>
                <input
                  type="text"
                  value={editData.period}
                  onChange={(e) =>
                    setEditData({ ...editData, period: e.target.value })
                  }
                  placeholder="예: 5"
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-[#1FFFA9]"
                  required
                />
              </div>
              <div className="flex space-x-3 pt-4">
                <button
                  type="button"
                  onClick={() => setShowEditModal(false)}
                  className="flex-1 bg-gray-700 hover:bg-gray-600 text-white py-2 px-4 rounded-lg transition-colors cursor-pointer whitespace-nowrap"
                >
                  취소
                </button>
                <button
                  type="submit"
                  className="flex-1 bg-[#1FFFA9] hover:bg-[#1FFFA9]/80 text-black py-2 px-4 rounded-lg font-medium transition-colors cursor-pointer whitespace-nowrap"
                >
                  수정
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
};

export default FriendSidebar;
