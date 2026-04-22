import type { RouteObject } from 'react-router-dom';
import NotFound from '../pages/NotFound';
import Landing from '../pages/landing/LandingPage';
import Login from '../pages/login/LoginPage';
import SignIn from '../pages/register/SignInPage';
import Main from '../pages/main/MainPage';
import MyPage from '../pages/mypage/MyPage';
import AddFriend from '../pages/register/AddFreindPage';
import DashBoard from '../pages/friend/dashboard/DashBoardPage';
import ChartDetail from '../pages/friend/chart/ChartDetailPage';
import CashFlow from '../pages/friend/cashflow/CashFlowPage';
import GiftPrice from '../pages/friend/giftprice/GiftPricePage';
import EventPrice from '../pages/friend/eventprice/EventPricePage';
import Upload from '../pages/register/upload';

const routes: RouteObject[] = [
  {
    path: '/',
    element: <Landing />,
  },
  {
    path: '/login',
    element: <Login />,
  },
  {
    path: '/register',
    element: <SignIn />,
  },
  {
    path: '/signinpage',
    element: <SignIn />,
  },
  {
    path: '/main',
    element: <Main />,
  },
  {
    path: '/mypage',
    element: <MyPage />,
  },
  {
    path: '/addfriend',
    element: <AddFriend />,
  },
  {
    path: '/friend/:id/upload',
    element: <Upload />,
  },
  {
    path: '/friend/:id',
    element: <DashBoard />,
  },
  {
    path: '/friend/:id/chart',
    element: <ChartDetail />,
  },
  {
    path: '/friend/:id/cashflow',
    element: <CashFlow />,
  },
  {
    path: '/friend/:id/giftprice',
    element: <GiftPrice />,
  },
  {
    path: '/friend/:id/eventprice',
    element: <EventPrice />,
  },
  {
    path: '*',
    element: <NotFound />,
  },
];

export default routes;
