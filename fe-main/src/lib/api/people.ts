import api from './client';

export type Gender = 'MALE' | 'FEMALE';
export type Status = 'INTEREST' | 'UNINTEREST' | 'MAINTAIN';
export type Relation = 'FRIEND' | 'COWORKER' | 'LOVER';

export interface CreatePersonRequest {
  name: string;
  birth?: string; // 나중에 age 변경
  gender: Gender;
  status: Status;
  relation: Relation;
  duration: number; // years
}

export type UpdatePersonRequest = CreatePersonRequest;

export interface CreatePersonPayload {
  id: number;
  name: string;
  birth?: string;
  gender: Gender;
  status: Status;
  interestStrategy: string;
  uninterestStrategy: string;
  maintainStrategy: string;
  relation: Relation;
  duration: number;
  totalGive?: number;
  totalTake?: number;
  age?: number;
  createdAt?: string;
  updatedAt?: string;
  // 값 관련 필드 (대시보드/리스트에서만 사용)
  latestValue?: number;
  latestYear?: number;
  latestMonth?: number;
  latestChangeRate?: number;
}

export interface PersonResponseWrapper {
  status: string;
  data: {
    person: CreatePersonPayload;
    latestManualStats?: ManualStatsPayload | null;
    topics?: TopicPayload[];
  };
}

// === List People (여기 수정됨) ===
export interface ListPeopleItem {
  person: CreatePersonPayload;
  latestValue: number;
  latestYear: number;
  latestMonth: number;
  latestChangeRate: number;
}

export interface ListPeopleResponseWrapper {
  status: string;
  data: ListPeopleItem[];
}

export async function createPerson(body: CreatePersonRequest) {
  const { data } = await api.post<PersonResponseWrapper>(
    `/people`,
    body,
    { headers: { 'Content-Type': 'application/json' } },
  );
  return data;
}

export async function getPeople() {
  const { data } = await api.get<ListPeopleResponseWrapper>(`/people`);
  return data;
}

export async function getPerson(id: string | number) {
  const { data } = await api.get<PersonResponseWrapper>(`/people/${id}`);
  return data;
}

export async function updatePerson(id: string | number, body: UpdatePersonRequest) {
  const { data } = await api.put<PersonResponseWrapper>(
    `/people/${id}`,
    body,
    { headers: { 'Content-Type': 'application/json' } },
  );
  return data;
}

// === Dashboard value response types ===
export interface PersonValuePayload {
  id: number;
  personId: number;
  value: number;
  year: number;
  month: number;
  feedback?: string;
  changeRate?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface FamilyEventCategoryPayload {
  id: number;
  title: string;
  type: string;
}

export interface FamilyEventPayload {
  id: number;
  personId: number;
  cost: number;
  attendance: boolean;
  price: number;
  content?: string;
  category: FamilyEventCategoryPayload;
  createdAt?: string;
  updatedAt?: string;
}

export interface OfferGiftPayload {
  id: number;
  name?: string;
  title?: string; // API에서 title 필드로 올 수 있음
  price: number;
  link?: string;
  imageUrl?: string;
  image?: string; // API에서 image 필드로 올 수 있음
  topic?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TopicPayload {
  id: number;
  topic: string;
  year: number;
  month: number;
  count: number;
  personId: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ManualStatsPayload {
  id: number;
  monthVolume: number;
  monthCount: number;
  responseAverage: number;
  chatDays: number;
  silentDays: number;
  personValueId: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface PersonDashboardValueResponse {
  status: string;
  data: {
    person: CreatePersonPayload;
    personValuesLast12: PersonValuePayload[];
    latestFamilyEvent?: FamilyEventPayload | null;
    latestOffer?: OfferGiftPayload | null;
    latestManualStats?: ManualStatsPayload | null;
    topics?: TopicPayload[];
  };
}

export async function getPersonDashboardValue(personId: string | number) {
  const { data } = await api.get<PersonDashboardValueResponse>(
    `/people/${personId}/value`,
  );
  return data;
}

// === Event price history ===
export async function getEventPriceHistory(personId: string | number) {
  const { data } = await api.get<FamilyEventPayload[]>(
    `/people/${personId}/family-events/All`,
  );
  return data;
}

export async function createEvent(
  personId: string | number,
  body: { cost: number; categoryId: number; personId: number }
) {
  const { data } = await api.post<{ status: string; data: FamilyEventPayload }>(
    `/people/${personId}/family-events`,
    body,
    { headers: { 'Content-Type': 'application/json' } }
  );
  return data.data;
}

// === Chart 관련 타입 ===
export interface AutoStatsPayload {
  id: number;
  startChat: number;
  question: number;
  privateStory: number;
  positiveReaction: number;
  getHelp: number;
  meetingSuccess: number;
  noResponse: number;
  giveHelp: number;
  attack: number;
  meetingRejection: number;
  personValueId: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface IssuePayload {
  id: number;
  content: string;
  year: number;
  month: number;
  personId: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface PersonChartResponse {
  status: string;
  data: {
    latestAuto: AutoStatsPayload;
    latestManual: ManualStatsPayload;
    issues: IssuePayload[];
    personValuesLast12: PersonValuePayload[];
  };
}

// === Gift / Offer 관련 타입 ===
export interface OfferCategoryPayload {
  id: number;
  title: string; // e.g., "BIRTH"
  type: string;  // e.g., "GIFT"
}

export interface OfferPayload {
  id: number;
  personId: number;
  freeCash: number;
  price: number;
  content?: string;
  category: OfferCategoryPayload;
  gifts: OfferGiftPayload[];
  createdAt?: string;
  updatedAt?: string;
}

export interface OfferResponse {
  status: string;
  data: OfferPayload;
}

// GET: Offer 가져오기
export async function getOffer(personId: string | number) {
  const { data } = await api.get<OfferResponse>(`/people/${personId}/offer`);
  return data; // OfferPayload 반환
}

// POST: Offer 생성
export async function createOffer(
  personId: string | number,
  body: { freeCash: number; categoryId: number; personId: number }
) {
  const { data } = await api.post<OfferResponse>(
    `/people/${personId}/offer`,
    body,
    { headers: { 'Content-Type': 'application/json' } }
  );
  return data.data; // OfferPayload 반환
}

// === Chart API ===
export async function getPersonChart(personId: string | number) {
  const { data } = await api.get<PersonChartResponse>(`/persons/${personId}/chart`);
  return data;
}

// === 파일 업로드 API ===
export async function uploadPersonFile(personId: string | number, file: File) {
  const formData = new FormData();
  formData.append('file', file);
  
  const { data } = await api.post(
    `/${personId}`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return data;
}
