import React from 'react';
import { Link, useLocation } from 'react-router-dom';

interface FriendSubNavProps {
  friendId: string;
  currentPage: string;
}

const FriendSubNav: React.FC<FriendSubNavProps> = ({
  friendId,
  currentPage,
}) => {
  const location = useLocation();

  const navItems = [
    { key: 'detail', label: '대시보드', path: `/friend/${friendId}` },
    { key: 'chart', label: '차트 디테일', path: `/friend/${friendId}/chart` },
    {
      key: 'cashflow',
      label: '정산 현황',
      path: `/friend/${friendId}/cashflow`,
    },
    {
      key: 'giftprice',
      label: '선물 가이드',
      path: `/friend/${friendId}/giftprice`,
    },
    {
      key: 'eventprice',
      label: '경조사비 가이드',
      path: `/friend/${friendId}/eventprice`,
    },
  ];

  return (
    <div className="bg-gray-900/70 border-b border-gray-800">
      <div className="px-8 py-2">
        <nav className="flex space-x-8">
          {navItems.map((item) => {
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.key}
                to={item.path}
                className={`px-4 py-2 rounded-lg font-medium transition-colors whitespace-nowrap ${
                  isActive
                    ? 'bg-[#1FFFA9] text-black'
                    : 'text-gray-300 hover:text-white hover:bg-gray-800/50'
                }`}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>
      </div>
    </div>
  );
};

export default FriendSubNav;
