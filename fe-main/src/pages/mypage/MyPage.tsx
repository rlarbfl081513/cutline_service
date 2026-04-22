import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Navigation from '../../components/feature/Navigation';
import Button from '../../components/base/Button';
import { getUser, logout } from '../../lib/api/users';
import {
  getPeople,
  getPerson,
  updatePerson,
  CreatePersonPayload,
} from '../../lib/api/people';

type UIFriend = {
  id: number;
  name: string;
  status: '관심' | '유지' | '비관심';
  age: number;
  gender: '남성' | '여성' | '기타' | string;
  relationship: '친구' | '직장동료' | '애인' | string;
};

const statusToKo = (s: CreatePersonPayload['status']): UIFriend['status'] => {
  switch (s) {
    case 'INTEREST':
      return '관심';
    case 'MAINTAIN':
      return '유지';
    case 'UNINTEREST':
    default:
      return '비관심';
  }
};

const relationToKo = (
  r: CreatePersonPayload['relation'],
): UIFriend['relationship'] => {
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

const genderToKo = (g?: CreatePersonPayload['gender']): UIFriend['gender'] =>
  g === 'MALE' ? '남성' : g === 'FEMALE' ? '여성' : '기타';

const normalizeStatusKo = (s: string): UIFriend['status'] => {
  if (s === '관심') return '관심';
  if (s === '유지') return '유지';
  return '비관심';
};

export default function MyPage() {
  const navigate = useNavigate();
  const [user, setUser] = useState({
    email: '',
    name: '',
    birthDate: '',
    gender: 'male',
  });
  const [friends, setFriends] = useState<UIFriend[]>([]);
  const [editingFriend, setEditingFriend] = useState<number | null>(null);
  const [rawEditingPerson, setRawEditingPerson] =
    useState<CreatePersonPayload | null>(null);
  const [editForm, setEditForm] = useState({
    name: '',
    age: '',
    gender: '',
    relationship: '',
    status: '',
    period: '',
  });
  const [showEditModal, setShowEditModal] = useState(false);

  const getStatusColor = (status: string) => {
    switch (status) {
      case '관심':
        return 'text-[#1FFFA9]';
      case '유지':
        return 'text-[#0A74FF]';
      case '비관심':
        return 'text-[#E43F42]';
      default:
        return 'text-gray-400';
    }
  };

  const handleLogout = async () => {
    if (confirm('로그아웃 하시겠습니까?')) {
      try {
        await logout();
      } catch (error) {
        console.error('로그아웃 API 호출 실패:', error);
      } finally {
        localStorage.removeItem('userInfo');
        localStorage.removeItem('userId');
        navigate('/');
      }
    }
  };

  const formatDateForDisplay = (dateString: string) => {
    const date = new Date(dateString);
    return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`;
  };

  // ✅ 친구 리스트 불러오기
  useEffect(() => {
    let mounted = true;
    async function loadFriends() {
      try {
        const res = await getPeople();
        const list = res.data || [];
        const mapped: UIFriend[] = list.map((p) => {
          let age = p?.age || 0;
          if (!age && p?.birth) {
            const y = Number(p.birth.slice(0, 4));
            if (!Number.isNaN(y)) age = new Date().getFullYear() - y;
          }
          return {
            id: p.id,
            name: p.name,
            status: statusToKo(p.status),
            age,
            gender: genderToKo(p.gender),
            relationship: relationToKo(p.relation),
          };
        });
        if (mounted) setFriends(mapped);
      } catch (e) {
        console.error('Failed to load friends:', e);
      }
    }
    loadFriends();
    return () => {
      mounted = false;
    };
  }, []);

  // ✅ 내 프로필 불러오기
  useEffect(() => {
    let mounted = true;
    async function loadUser() {
      try {
        const res = await getUser();
        const u = res.data;
        const gender =
          u.gender === 'MALE'
            ? 'male'
            : u.gender === 'FEMALE'
            ? 'female'
            : 'male';
        const birthDate = u.birth ?? '';
        if (!mounted) return;
        setUser({
          email: u.email,
          name: u.name,
          birthDate,
          gender,
        });
      } catch (e) {
        console.error('Failed to load user profile:', e);
      }
    }
    loadUser();
    return () => {
      mounted = false;
    };
  }, []);

  return (
    <div className="pt-16 min-h-screen bg-[#0A0D0C] text-white">
      <Navigation />
      <div className="max-w-7xl mx-auto p-6">
        <h1 className="text-3xl font-bold mb-8">마이페이지</h1>

        <div className="flex justify-center">
          {/* 사용자 정보 */}
          <div className="bg-gray-900/50 p-8 rounded-xl border border-gray-800 max-w-md w-full">
            <h2 className="text-xl font-bold mb-6 text-center">프로필</h2>
            <div className="space-y-6">
              <div className="text-center">
                <label className="block text-sm font-medium mb-2 text-gray-400">
                  이메일
                </label>
                <div className="text-lg">{user.email}</div>
              </div>
              <div className="text-center">
                <label className="block text-sm font-medium mb-2 text-gray-400">
                  이름
                </label>
                <div className="text-lg">{user.name}</div>
              </div>
              <div className="text-center">
                <label className="block text-sm font-medium mb-2 text-gray-400">
                  생년월일
                </label>
                <div className="text-lg text-gray-300">
                  {user.birthDate ? formatDateForDisplay(user.birthDate) : '미설정'}
                </div>
              </div>
              <div className="text-center">
                <label className="block text-sm font-medium mb-2 text-gray-400">
                  성별
                </label>
                <div className="text-lg text-gray-300">
                  {user.gender === 'male'
                    ? '남성'
                    : user.gender === 'female'
                    ? '여성'
                    : '미설정'}
                </div>
              </div>
              <div className="pt-4">
                <Button
                  variant="danger"
                  className="w-full"
                  onClick={handleLogout}
                >
                  로그아웃
                </Button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
