import axios from 'axios';

// [핵심 수정] 
// Nginx가 '/parsing'으로 시작하는 요청만 파싱 서버로 보내므로,
// baseURL에 무조건 '/parsing'이 포함되도록 강제합니다.
// 이렇게 하면 실수로 .env에 도메인만 적어도 안전하게 동작합니다.
const getBaseUrl = () => {
  const envUrl = import.meta.env.VITE_API_BASE_URL;
  
  // 1. 환경 변수가 없으면 상대 경로 '/parsing' 사용 (가장 권장되는 방식)
  if (!envUrl) return '/parsing';

  // 2. 환경 변수가 있는데 '/parsing'으로 끝나지 않으면 붙여줌
  if (!envUrl.endsWith('/parsing')) {
    return `${envUrl.replace(/\/$/, '')}/parsing`;
  }

  // 3. 이미 올바르게 설정된 경우 그대로 반환
  return envUrl;
};

const api = axios.create({
  baseURL: getBaseUrl(),
  timeout: 3000000, // 대용량 파일 파싱을 위해 넉넉하게 설정
  withCredentials: true, // 쿠키(세션) 전송 필수
});

// =================================================================
// [Logging Interceptors] 디버깅을 위한 요청/응답 로그 (기존 유지)
// =================================================================
api.interceptors.request.use((config) => {
  const base = (config.baseURL || '').replace(/\/$/, '');
  const path = `${config.url || ''}`.startsWith('/') ? `${config.url}` : `/${config.url}`;
  const fullUrl = `${base}${path}`;
  const method = (config.method || 'get').toUpperCase();
  
  // eslint-disable-next-line no-console
  console.log(`➡️ [Parsing Request] ${method} ${fullUrl}`);
  
  if (config.params) {
    // eslint-disable-next-line no-console
    console.log('   ↳ params:', config.params);
  }
  // 파일 업로드(FormData)의 경우 body 로그가 너무 크거나 [object FormData]로 나올 수 있어 생략하거나 간략화 가능
  if (config.data && !(config.data instanceof FormData)) {
    // eslint-disable-next-line no-console
    console.log('   ↳ body:', config.data);
  }
  return config;
});

api.interceptors.response.use(
  (response) => {
    const base = (response.config.baseURL || '').replace(/\/$/, '');
    const path = `${response.config.url || ''}`.startsWith('/')
      ? `${response.config.url}`
      : `/${response.config.url}`;
    const fullUrl = `${base}${path}`;
    
    // eslint-disable-next-line no-console
    console.log(`✅ [Parsing Response] ${response.status} ${fullUrl}`);
    return response;
  },
  (error) => {
    const cfg = error.config || {};
    const base = (cfg.baseURL || '').replace(/\/$/, '');
    const path = `${cfg.url || ''}`.startsWith('/') ? `${cfg.url}` : `/${cfg.url}`;
    const fullUrl = `${base}${path}`;
    
    // eslint-disable-next-line no-console
    console.error(`❌ [Parsing Error] ${cfg.method ? cfg.method.toUpperCase() : ''} ${fullUrl}`, error);
    return Promise.reject(error);
  },
);

export default api;

// =================================================================
// [API Functions]
// =================================================================

export interface UploadParsingResponse {
  id?: string | number;
  [key: string]: any;
}

/**
 * 카카오톡 대화 내용 파싱 요청 (신규 업로드)
 * POST /parsing/{personId}
 */
export async function uploadParsing(personId: string | number, file: File) {
  const form = new FormData();
  form.append('file', file);

  // baseURL이 이미 '/parsing'을 포함하고 있으므로, 여기서는 ID만 붙입니다.
  // 최종 요청 URL: /parsing/{personId}
  const { data } = await api.post<UploadParsingResponse>(
    `/${personId}`,
    form,
  );
  return data;
}

/**
 * 카카오톡 대화 내용 파싱 업데이트 (기존 데이터에 추가)
 * PUT /parsing/{personId}
 */
export async function updateParsing(personId: string | number, file: File) {
  const form = new FormData();
  form.append('file', file);

  const { data } = await api.put<UploadParsingResponse>(
    `/${personId}`,
    form,
  );
  return data;
}