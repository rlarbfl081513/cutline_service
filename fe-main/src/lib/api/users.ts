import api from './client';

export type UserGender = 'MALE' | 'FEMALE';

export interface UserPayload {
  id: number;
  email: string;
  name: string;
  birth?: string; // YYYY-MM-DD
  gender?: UserGender;
  createdAt?: string;
  updatedAt?: string;
}

export interface UserResponseWrapper<T = UserPayload> {
  status: string;
  data: T;
}

export async function getUser() {
  const { data } = await api.get<UserResponseWrapper>(`/me`);
  return data;
}

export async function logout() {
  const { data } = await api.post<{ status: string; data: {} }>(`/auth/logout`);
  return data;
}

export interface CreateUserPayload {
  email: string;
  name: string;
  birth: string;
  gender: 'MALE' | 'FEMALE';
}

export async function createUser(payload: CreateUserPayload) {
  const { data } = await api.post<UserResponseWrapper>(`/me`, payload);
  return data;
}
