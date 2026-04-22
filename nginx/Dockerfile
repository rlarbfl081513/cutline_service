FROM nginx:alpine

# 1. 기존 Nginx 기본 설정 삭제 (이게 없으면 충돌날 수 있음)
RUN rm /etc/nginx/conf.d/default.conf

# 2. 로컬에 있는 nginx.conf를 컨테이너 설정 경로로 복사
# [중요] nginx.conf 파일이 Dockerfile과 같은 폴더에 있어야 합니다.
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 3. 80번 포트 열기
EXPOSE 80

# 4. Nginx 실행
CMD ["nginx", "-g", "daemon off;"]