#!/bin/bash

# CutLine Main Application Docker Compose 관리 스크립트
# 사용법: ./app.sh [start|stop|restart|logs|status|build|rebuild]

COMPOSE_FILE="docker-compose.main.yml"

case "$1" in
    start)
        echo "🚀 CutLine Main 애플리케이션을 시작합니다..."
        docker-compose -f $COMPOSE_FILE up -d
        echo "✅ 애플리케이션이 시작되었습니다."
        echo "📍 애플리케이션: http://localhost:8080/main"
        echo "📍 Swagger UI: http://localhost:8080/main/swagger-ui.html"
        echo "📍 Ping Test: http://localhost:8080/main/ping"
        ;;
    stop)
        echo "🛑 CutLine Main 애플리케이션을 중지합니다..."
        docker-compose -f $COMPOSE_FILE down
        echo "✅ 애플리케이션이 중지되었습니다."
        ;;
    restart)
        echo "🔄 CutLine Main 애플리케이션을 재시작합니다..."
        docker-compose -f $COMPOSE_FILE down
        docker-compose -f $COMPOSE_FILE up -d
        echo "✅ 애플리케이션이 재시작되었습니다."
        ;;
    logs)
        echo "📝 애플리케이션 로그를 확인합니다..."
        docker-compose -f $COMPOSE_FILE logs -f app
        ;;
    status)
        echo "📊 애플리케이션 상태를 확인합니다..."
        docker-compose -f $COMPOSE_FILE ps
        ;;
    build)
        echo "🔨 애플리케이션 이미지를 빌드합니다..."
        docker-compose -f $COMPOSE_FILE build app
        echo "✅ 빌드가 완료되었습니다."
        ;;
    rebuild)
        echo "🔨 애플리케이션을 완전히 재빌드하고 시작합니다..."
        docker-compose -f $COMPOSE_FILE down
        docker-compose -f $COMPOSE_FILE build --no-cache app
        docker-compose -f $COMPOSE_FILE up -d
        echo "✅ 재빌드 및 시작이 완료되었습니다."
        ;;
    shell)
        echo "🔗 애플리케이션 컨테이너에 접속합니다..."
        docker-compose -f $COMPOSE_FILE exec app bash
        ;;
    health)
        echo "🏥 애플리케이션 헬스체크를 수행합니다..."
        curl -f http://localhost:8080/main/ping || echo "❌ 헬스체크 실패"
        ;;
    *)
        echo "CutLine Main 애플리케이션 관리 스크립트"
        echo ""
        echo "사용법: $0 {start|stop|restart|logs|status|build|rebuild|shell|health}"
        echo ""
        echo "명령어:"
        echo "  start    - 애플리케이션 시작"
        echo "  stop     - 애플리케이션 중지"
        echo "  restart  - 애플리케이션 재시작"
        echo "  logs     - 애플리케이션 로그 실시간 보기"
        echo "  status   - 애플리케이션 상태 확인"
        echo "  build    - 애플리케이션 이미지 빌드"
        echo "  rebuild  - 캐시 없이 완전 재빌드 후 시작"
        echo "  shell    - 애플리케이션 컨테이너 접속"
        echo "  health   - 헬스체크 수행"
        echo ""
        exit 1
        ;;
esac
